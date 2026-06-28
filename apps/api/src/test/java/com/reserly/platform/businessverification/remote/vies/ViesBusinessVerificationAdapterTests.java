package com.reserly.platform.businessverification.remote.vies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.reserly.platform.businessverification.matching.BusinessIdentityMatchingProperties;
import com.reserly.platform.businessverification.matching.BusinessIdentityMatchingServiceImpl;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationException;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationRequest;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationResult;
import com.reserly.platform.businessverification.remote.RemoteVerificationAttemptContext;
import com.reserly.platform.businessverification.remote.RemoteVerificationErrorCode;
import com.reserly.platform.businessverification.remote.RemoteVerificationStatus;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Verifica el contrato SOAP VIES contra un servidor HTTP local y determinista. */
class ViesBusinessVerificationAdapterTests {

  private HttpServer server;
  private AtomicReference<String> receivedRequest;
  private AtomicReference<ResponseFixture> responseFixture;
  private ViesBusinessVerificationAdapter adapter;

  @BeforeEach
  void startServer() throws IOException {
    receivedRequest = new AtomicReference<>();
    responseFixture = new AtomicReference<>(new ResponseFixture(200, validResponse()));
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/vies", this::respond);
    server.start();
    URI endpoint = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/vies");
    adapter =
        new ViesBusinessVerificationAdapter(
            new ViesProperties(endpoint, 4096),
            new BusinessIdentityMatchingServiceImpl(
                new BusinessIdentityMatchingProperties(0.85, 0.75)));
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void sendsOnlyVatIdentityAndMapsValidMinimalResponse() throws Exception {
    RemoteBusinessVerificationResult result = adapter.verify(spanishRequest(), context());

    assertThat(receivedRequest.get())
        .contains("<urn:countryCode>ES</urn:countryCode>")
        .contains("<urn:vatNumber>B12345674</urn:vatNumber>")
        .doesNotContain("Empresa de Prueba")
        .doesNotContain("businessAccountId");
    assertThat(result.status()).isEqualTo(RemoteVerificationStatus.VERIFIED);
    assertThat(result.matchedLegalName()).isTrue();
    assertThat(result.matchedAddress()).isTrue();
    assertThat(result.remoteReference()).isNull();
    assertThat(result.rawResponseHash()).matches("[0-9a-f]{64}");
  }

  @Test
  void mapsNegativeViesAnswerWithoutTreatingMissingNameAsMismatch() throws Exception {
    responseFixture.set(new ResponseFixture(200, invalidResponse()));

    RemoteBusinessVerificationResult result = adapter.verify(spanishRequest(), context());

    assertThat(result.status()).isEqualTo(RemoteVerificationStatus.INVALID);
    assertThat(result.matchedLegalName()).isNull();
    assertThat(result.matchedAddress()).isNull();
  }

  @Test
  void mapsOfficialTransientSoapFaultToRetryableError() {
    responseFixture.set(new ResponseFixture(500, soapFault("MS_UNAVAILABLE")));

    assertThatThrownBy(() -> adapter.verify(spanishRequest(), context()))
        .isInstanceOfSatisfying(
            RemoteBusinessVerificationException.class,
            exception -> {
              assertThat(exception.getErrorCode())
                  .isEqualTo(RemoteVerificationErrorCode.PROVIDER_UNAVAILABLE);
              assertThat(exception.getErrorCode().retryable()).isTrue();
            });
  }

  @Test
  void rejectsOversizedOrUnsafeXmlResponse() {
    responseFixture.set(new ResponseFixture(200, "x".repeat(5000)));
    assertThatThrownBy(() -> adapter.verify(spanishRequest(), context()))
        .isInstanceOfSatisfying(
            RemoteBusinessVerificationException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(RemoteVerificationErrorCode.INVALID_PROVIDER_RESPONSE));

    responseFixture.set(
        new ResponseFixture(
            200,
            """
            <!DOCTYPE data [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <data>&xxe;</data>
            """));
    assertThatThrownBy(() -> adapter.verify(spanishRequest(), context()))
        .isInstanceOfSatisfying(
            RemoteBusinessVerificationException.class,
            exception ->
                assertThat(exception.getErrorCode())
                    .isEqualTo(RemoteVerificationErrorCode.INVALID_PROVIDER_RESPONSE));
  }

  @Test
  void mapsGreekIsoCountryToViesElCodeAndStripsPrefix() throws Exception {
    responseFixture.set(
        new ResponseFixture(
            200,
            validResponse()
                .replace("<countryCode>ES</countryCode>", "<countryCode>EL</countryCode>")
                .replace("<vatNumber>B12345674</vatNumber>", "<vatNumber>123456789</vatNumber>")));
    RemoteBusinessVerificationRequest request =
        new RemoteBusinessVerificationRequest(
            UUID.randomUUID(), UUID.randomUUID(), "GR", "EL123456789", "Empresa", null, true);

    adapter.verify(request, context());

    assertThat(receivedRequest.get())
        .contains("<urn:countryCode>EL</urn:countryCode>")
        .contains("<urn:vatNumber>123456789</urn:vatNumber>");
  }

  private void respond(HttpExchange exchange) throws IOException {
    receivedRequest.set(
        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    ResponseFixture fixture = responseFixture.get();
    byte[] body = fixture.body().getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().add("Content-Type", "text/xml; charset=UTF-8");
    exchange.sendResponseHeaders(fixture.status(), body.length);
    exchange.getResponseBody().write(body);
    exchange.close();
  }

  private RemoteBusinessVerificationRequest spanishRequest() {
    return new RemoteBusinessVerificationRequest(
        UUID.randomUUID(),
        UUID.randomUUID(),
        "ES",
        "B12345674",
        "Empresa de Prueba SL",
        "Calle Alcalá 10, Madrid",
        true);
  }

  private RemoteVerificationAttemptContext context() {
    return new RemoteVerificationAttemptContext(
        UUID.randomUUID(),
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        1,
        Duration.ofSeconds(1),
        Duration.ofSeconds(2));
  }

  private static String validResponse() {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <checkVatResponse xmlns="urn:ec.europa.eu:taxud:vies:services:checkVat:types">
              <countryCode>ES</countryCode>
              <vatNumber>B12345674</vatNumber>
              <requestDate>2026-06-28+02:00</requestDate>
              <valid>true</valid>
              <name>EMPRESA DE PRUEBA S.L.</name>
              <address>CALLE ALCALA 10 MADRID</address>
            </checkVatResponse>
          </soap:Body>
        </soap:Envelope>
        """;
  }

  private static String invalidResponse() {
    return validResponse()
        .replace("<valid>true</valid>", "<valid>false</valid>")
        .replace("<name>EMPRESA DE PRUEBA S.L.</name>", "<name>---</name>")
        .replace("<address>CALLE ALCALA 10 MADRID</address>", "<address>---</address>");
  }

  private static String soapFault(String fault) {
    return """
        <?xml version="1.0" encoding="UTF-8"?>
        <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <soap:Fault>
              <faultcode>soap:Server</faultcode>
              <faultstring>%s</faultstring>
            </soap:Fault>
          </soap:Body>
        </soap:Envelope>
        """
        .formatted(fault);
  }

  private record ResponseFixture(int status, String body) {}
}
