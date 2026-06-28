package com.reserly.platform.businessverification.remote;

/** Trabajo remoto aislado por el watchdog de timeout total del gateway. */
@FunctionalInterface
public interface RemoteVerificationInvocation {

  RemoteBusinessVerificationResult invoke() throws RemoteBusinessVerificationException;
}
