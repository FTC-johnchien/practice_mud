package com.example.htmlmud.domain.actor.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageFragment {
  private String type;
  private Object payload;
  private long timestamp;
}
