package com.example.htmlmud.domain.model.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MoveMessage(

    String cast,

    String hit,

    String crit,

    String miss,

    String success

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
    if (success == null) {
      success = "";
    }
  }
}
