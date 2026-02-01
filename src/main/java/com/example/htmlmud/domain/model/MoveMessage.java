package com.example.htmlmud.domain.model;

public record MoveMessage(

    String cast,

    String hit,

    String crit,

    String miss

) {
  public MoveMessage {
    if (cast == null) {
      cast = "";
    }
    if (hit == null) {
      hit = "";
    }
    if (crit == null) {
      crit = "";
    }
    if (miss == null) {
      miss = "";
    }
  }
}
