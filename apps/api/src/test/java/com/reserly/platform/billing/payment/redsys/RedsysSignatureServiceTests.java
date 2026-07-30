package com.reserly.platform.billing.payment.redsys;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Contrasta HMAC_SHA512_V2 con el vector oficial publicado por RedSys. */
class RedsysSignatureServiceTests {

  private static final String TEST_KEY = "sq7HjrUOBfKmC576ILgskD5srU870gJ7";
  private static final String ORDER = "1234567890";
  private static final String PARAMETERS =
      "eyJEU19NRVJDSEFOVF9BTU9VTlQiOiI5OTkiLCJEU19NRVJDSEFOVF9PUkRFUiI6"
          + "IjEyMzQ1Njc4OTAiLCJEU19NRVJDSEFOVF9NRVJDSEFOVENPREUiOiI5OTkwMDg4"
          + "ODEiLCJEU19NRVJDSEFOVF9DVVJSRU5DWSI6Ijk3OCIsIkRTX01FUkNIQU5UX1RS"
          + "QU5TQUNUSU9OVFlQRSI6IjAiLCJEU19NRVJDSEFOVF9URVJNSU5BTCI6IjEiLCJE"
          + "U19NRVJDSEFOVF9NRVJDSEFOVFVSTCI6Imh0dHA6XC9cL3d3dy5wcnVlYmEuY29t"
          + "XC91cmxOb3RpZmljYWNpb24ucGhwIiwiRFNfTUVSQ0hBTlRfVVJMT0siOiJodHRw"
          + "OlwvXC93d3cucHJ1ZWJhLmNvbVwvdXJsT0sucGhwIiwiRFNfTUVSQ0hBTlRfVVJM"
          + "S08iOiJodHRwOlwvXC93d3cucHJ1ZWJhLmNvbVwvdXJsS08ucGhwIn0";
  private static final String EXPECTED_SIGNATURE =
      "Vjo02eSWq249IeZZp3R-ArFnGLhKY0OuzDDlx1BuVtZDC2yhczA7_11uZhsYzLZBCMFAz8u8uzGDX3AErHKmmw";

  private final RedsysSignatureService service = new RedsysSignatureServiceImpl();

  @Test
  void matchesOfficialAesAndHmacSha512VectorExactly() {
    assertThat(service.sign(PARAMETERS, ORDER, TEST_KEY)).isEqualTo(EXPECTED_SIGNATURE);
    assertThat(service.verify(PARAMETERS, ORDER, TEST_KEY, EXPECTED_SIGNATURE)).isTrue();
  }

  @Test
  void rejectsTamperingAndMalformedSignaturesWithoutThrowing() {
    assertThat(service.verify(PARAMETERS + "A", ORDER, TEST_KEY, EXPECTED_SIGNATURE)).isFalse();
    assertThat(service.verify(PARAMETERS, ORDER, TEST_KEY, "not-base64!")).isFalse();
  }
}
