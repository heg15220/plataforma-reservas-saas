package com.reserly.platform.demand.waitlist.service;

/** Rechazo opaco que impide distinguir token inexistente, caducidad, revocación o capacidad. */
public class WaitlistOfferUnavailableException extends RuntimeException {

  public WaitlistOfferUnavailableException() {
    super("Waitlist offer is unavailable");
  }
}
