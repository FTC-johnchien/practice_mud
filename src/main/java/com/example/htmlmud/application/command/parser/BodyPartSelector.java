package com.example.htmlmud.application.command.parser;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class BodyPartSelector {

  public static String getRandomBodyPart() {
    String[] parts = {"頭部", "胸口", "左臂", "右臂", "小腹", "大腿"};
    return parts[ThreadLocalRandom.current().nextInt(parts.length)];
  }

}
