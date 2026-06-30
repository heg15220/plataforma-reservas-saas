package com.reserly.platform.businessverification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.reserly.platform.businessverification.persistence.BusinessAccountDao;
import com.reserly.platform.businessverification.persistence.BusinessAccountEntity;
import com.reserly.platform.businessverification.persistence.BusinessVerificationCheckDao;
import com.reserly.platform.businessverification.remote.RemoteBusinessVerificationGatewayService;
import com.reserly.platform.infrastructure.ratelimit.RateLimitExceededException;
import com.reserly.platform.infrastructure.ratelimit.RateLimitScope;
import com.reserly.platform.infrastructure.ratelimit.RateLimitService;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Verifica que una cuota agotada impida transición de estado y llamada al proveedor. */
class RemoteBusinessVerificationRateLimitTests {

  @Test
  void stopsNewRemoteCheckBeforeStateMutationOrProviderInvocation() {
    UUID accountId = UUID.randomUUID();
    UUID requestId = UUID.randomUUID();
    BusinessAccountDao accountDao = mock(BusinessAccountDao.class);
    BusinessVerificationCheckDao checkDao = mock(BusinessVerificationCheckDao.class);
    RemoteBusinessVerificationGatewayService gateway =
        mock(RemoteBusinessVerificationGatewayService.class);
    EuropeanVatIdentifierPolicy vatPolicy = mock(EuropeanVatIdentifierPolicy.class);
    BusinessVerificationStateService stateService = mock(BusinessVerificationStateService.class);
    RateLimitService rateLimitService = mock(RateLimitService.class);
    BusinessAccountEntity account = new BusinessAccountEntity();
    account.setId(accountId);
    when(checkDao.findByRequestId(requestId)).thenReturn(Optional.empty());
    when(accountDao.findById(accountId)).thenReturn(Optional.of(account));
    doThrow(new RateLimitExceededException(Duration.ofMinutes(10)))
        .when(rateLimitService)
        .check(RateLimitScope.BUSINESS_VERIFICATION, accountId.toString());
    RemoteBusinessVerificationService service =
        new RemoteBusinessVerificationServiceImpl(
            accountDao, checkDao, gateway, vatPolicy, stateService, rateLimitService);

    assertThatThrownBy(
            () -> service.verify(new RemoteBusinessVerificationCommand(requestId, accountId, null)))
        .isInstanceOf(RateLimitExceededException.class);

    verifyNoInteractions(gateway, stateService);
  }
}
