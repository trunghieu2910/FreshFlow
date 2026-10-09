package com.freshflow.api.identity.exception;

public class IdentityGrantConflictException extends RuntimeException {
  public IdentityGrantConflictException(Throwable cause) {
    super("Role assignment conflicts with identity data", cause);
  }
}
