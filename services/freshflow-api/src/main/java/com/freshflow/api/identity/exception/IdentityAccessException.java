package com.freshflow.api.identity.exception;

public class IdentityAccessException extends RuntimeException {
  public IdentityAccessException() {
    super("Actor does not have active access in this scope");
  }
}
