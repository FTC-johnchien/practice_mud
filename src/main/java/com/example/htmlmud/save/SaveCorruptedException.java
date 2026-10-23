package com.example.htmlmud.save;

public class SaveCorruptedException extends RuntimeException {
  public SaveCorruptedException(String message) {
    super(message);
  }

  public SaveCorruptedException(String message, Throwable cause) {
    super(message, cause);
  }
}

