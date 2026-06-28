package com.reserly.platform.businessverification.remote.vies;

import com.reserly.platform.businessverification.matching.BusinessIdentityMatchingService;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationAdapter;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationException;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationRequest;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationResult;
import com.reserly.platform.businessverification.remote.RemoteVerificationAttemptContext;
import com.reserly.platform.businessverification.remote.RemoteVerificationErrorCode;
import com.reserly.platform.businessverification.remote.RemoteVerificationStatus;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Adaptador oficial gratuito VIES mediante el contrato SOAP público de la Comisión Europea.
 *
 * <p>Solo envía país y número VAT. No envía razón social, dirección, ID interno ni credenciales.
 * Nombre y dirección devueltos se usan en memoria para calcular coincidencias y nunca se persisten.
 */
@Component
public class ViesBusinessVerificationAdapter implements RemoteBusinessVerificationAdapter {

  private static final String PROVIDER_CODE = "vies";
  private static final String SOAP_NAMESPACE =
      "urn:ec.europa.eu:taxud:vies:services:checkVat:types";
  private static final Set<String> SUPPORTED_COUNTRIES =
      Set.of(
          "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI", "FR", "GR", "HR", "HU", "IE",
          "IT", "LT", "LU", "LV", "MT", "NL", "PL", "PT", "RO", "SE", "SI", "SK", "XI");

  private final ViesProperties properties;
  private final BusinessIdentityMatchingService identityMatchingService;

  public ViesBusinessVerificationAdapter(
      ViesProperties properties, BusinessIdentityMatchingService identityMatchingService) {
    this.properties = properties;
    this.identityMatchingService = identityMatchingService;
  }

  @Override
  public String providerCode() {
    return PROVIDER_CODE;
  }

  @Override
  public Set<String> supportedCountries() {
    return SUPPORTED_COUNTRIES;
  }

  @Override
  public int priority() {
    return 0;
  }

  @Override
  public boolean supports(RemoteBusinessVerificationRequest request) {
    return request.euVatIdentifier() && SUPPORTED_COUNTRIES.contains(request.taxCountry());
  }

  @Override
  public RemoteBusinessVerificationResult verify(
      RemoteBusinessVerificationRequest request, RemoteVerificationAttemptContext context)
      throws RemoteBusinessVerificationException {
    ViesVatNumber vatNumber = ViesVatNumber.from(request);
    byte[] response = invoke(vatNumber, context);
    ViesSoapResponse parsed = parse(response);
    if (!parsed.countryCode().equals(vatNumber.countryCode())
        || !parsed.vatNumber().equals(vatNumber.number())) {
      throw new RemoteBusinessVerificationException(
          RemoteVerificationErrorCode.INVALID_PROVIDER_RESPONSE);
    }
    return new RemoteBusinessVerificationResult(
        parsed.valid() ? RemoteVerificationStatus.VERIFIED : RemoteVerificationStatus.INVALID,
        parsed.valid()
            ? identityMatchingService.matchesLegalName(request.legalName(), parsed.name())
            : null,
        parsed.valid()
            ? identityMatchingService.matchesAddress(request.address(), parsed.address())
            : null,
        null,
        Instant.now(),
        sha256(response));
  }

  private byte[] invoke(ViesVatNumber vatNumber, RemoteVerificationAttemptContext context)
      throws RemoteBusinessVerificationException {
    HttpClient client =
        HttpClient.newBuilder()
            .connectTimeout(context.connectTimeout())
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    HttpRequest httpRequest =
        HttpRequest.newBuilder(properties.endpoint())
            .timeout(context.connectTimeout().plus(context.readTimeout()))
            .header("Content-Type", "text/xml; charset=UTF-8")
            .header("SOAPAction", "\"\"")
            .header("User-Agent", "Reserly-Business-Verification/1.0")
            .POST(
                HttpRequest.BodyPublishers.ofString(soapRequest(vatNumber), StandardCharsets.UTF_8))
            .build();
    try {
      HttpResponse<InputStream> response =
          client.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream());
      byte[] body = readBounded(response.body());
      if (response.statusCode() == 429) {
        throw new RemoteBusinessVerificationException(
            RemoteVerificationErrorCode.PROVIDER_RATE_LIMITED);
      }
      if (response.statusCode() >= 500) {
        RemoteVerificationErrorCode fault = parseSoapFault(body);
        throw new RemoteBusinessVerificationException(
            fault == null ? RemoteVerificationErrorCode.PROVIDER_UNAVAILABLE : fault);
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new RemoteBusinessVerificationException(
            RemoteVerificationErrorCode.PROVIDER_PROTOCOL_ERROR);
      }
      return body;
    } catch (HttpTimeoutException exception) {
      throw new RemoteBusinessVerificationException(RemoteVerificationErrorCode.PROVIDER_TIMEOUT);
    } catch (IOException exception) {
      throw new RemoteBusinessVerificationException(
          RemoteVerificationErrorCode.PROVIDER_UNAVAILABLE);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RemoteBusinessVerificationException(
          RemoteVerificationErrorCode.PROVIDER_UNAVAILABLE);
    }
  }

  private byte[] readBounded(InputStream input)
      throws IOException, RemoteBusinessVerificationException {
    try (input) {
      byte[] body = input.readNBytes(properties.maxResponseBytes() + 1);
      if (body.length > properties.maxResponseBytes()) {
        throw new RemoteBusinessVerificationException(
            RemoteVerificationErrorCode.INVALID_PROVIDER_RESPONSE);
      }
      return body;
    }
  }

  private ViesSoapResponse parse(byte[] body) throws RemoteBusinessVerificationException {
    Document document = parseXml(body);
    if (firstText(document, "Fault") != null) {
      throw new RemoteBusinessVerificationException(
          RemoteVerificationErrorCode.PROVIDER_PROTOCOL_ERROR);
    }
    String countryCode = requiredText(document, "countryCode");
    String vatNumber = requiredText(document, "vatNumber");
    String validValue = requiredText(document, "valid");
    if (!"true".equals(validValue) && !"false".equals(validValue)) {
      throw new RemoteBusinessVerificationException(
          RemoteVerificationErrorCode.INVALID_PROVIDER_RESPONSE);
    }
    return new ViesSoapResponse(
        countryCode,
        vatNumber,
        Boolean.parseBoolean(validValue),
        optionalText(document, "name"),
        optionalText(document, "address"));
  }

  private RemoteVerificationErrorCode parseSoapFault(byte[] body) {
    try {
      String fault = optionalText(parseXml(body), "faultstring");
      if (fault == null) {
        return null;
      }
      return switch (fault) {
        case "MS_UNAVAILABLE", "SERVICE_UNAVAILABLE" ->
            RemoteVerificationErrorCode.PROVIDER_UNAVAILABLE;
        case "TIMEOUT" -> RemoteVerificationErrorCode.PROVIDER_TIMEOUT;
        case "GLOBAL_MAX_CONCURRENT_REQ", "MS_MAX_CONCURRENT_REQ" ->
            RemoteVerificationErrorCode.PROVIDER_RATE_LIMITED;
        case "INVALID_INPUT" -> RemoteVerificationErrorCode.PROVIDER_PROTOCOL_ERROR;
        default -> RemoteVerificationErrorCode.INVALID_PROVIDER_RESPONSE;
      };
    } catch (RemoteBusinessVerificationException exception) {
      return null;
    }
  }

  private Document parseXml(byte[] body) throws RemoteBusinessVerificationException {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      var builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new DefaultHandler());
      return builder.parse(new java.io.ByteArrayInputStream(body));
    } catch (ParserConfigurationException | SAXException | IOException exception) {
      throw new RemoteBusinessVerificationException(
          RemoteVerificationErrorCode.INVALID_PROVIDER_RESPONSE);
    }
  }

  private String requiredText(Document document, String localName)
      throws RemoteBusinessVerificationException {
    String value = optionalText(document, localName);
    if (value == null) {
      throw new RemoteBusinessVerificationException(
          RemoteVerificationErrorCode.INVALID_PROVIDER_RESPONSE);
    }
    return value;
  }

  private String optionalText(Document document, String localName) {
    NodeList elements = document.getElementsByTagNameNS("*", localName);
    if (elements.getLength() == 0) {
      return null;
    }
    String value = elements.item(0).getTextContent();
    return value == null || value.isBlank() ? null : value.strip();
  }

  private String firstText(Document document, String localName) {
    return optionalText(document, localName);
  }

  private String soapRequest(ViesVatNumber vatNumber) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                          xmlns:urn="%s">
          <soapenv:Header/>
          <soapenv:Body>
            <urn:checkVat>
              <urn:countryCode>%s</urn:countryCode>
              <urn:vatNumber>%s</urn:vatNumber>
            </urn:checkVat>
          </soapenv:Body>
        </soapenv:Envelope>
        """
        .formatted(SOAP_NAMESPACE, vatNumber.countryCode(), vatNumber.number());
  }

  private String sha256(byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Required SHA-256 algorithm is unavailable", exception);
    }
  }

  private record ViesSoapResponse(
      String countryCode, String vatNumber, boolean valid, String name, String address) {}

  private record ViesVatNumber(String countryCode, String number) {

    private static ViesVatNumber from(RemoteBusinessVerificationRequest request)
        throws RemoteBusinessVerificationException {
      String countryCode = "GR".equals(request.taxCountry()) ? "EL" : request.taxCountry();
      String number = request.taxIdentifier();
      if (number.startsWith(request.taxCountry()) && number.length() > 2) {
        number = number.substring(2);
      } else if (number.startsWith(countryCode) && number.length() > 2) {
        number = number.substring(2);
      }
      if (!number.matches("[A-Z0-9]{2,14}")) {
        throw new RemoteBusinessVerificationException(
            RemoteVerificationErrorCode.PROVIDER_PROTOCOL_ERROR);
      }
      return new ViesVatNumber(countryCode, number);
    }
  }
}
