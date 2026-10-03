package com.example.htmlmud.domain.save.exception;

public class SaveCorruptedException extends RuntimeException {
  public SaveCorruptedException(String message) {
    super(message);
  }

  public SaveCorruptedException(String message, Throwable cause) {
    super(message, cause);
  }
}

