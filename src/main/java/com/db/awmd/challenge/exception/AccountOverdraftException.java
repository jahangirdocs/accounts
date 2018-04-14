package com.db.awmd.challenge.exception;

public class AccountOverdraftException extends RuntimeException {

  public AccountOverdraftException(String message) {
    super(message);
  }
}
