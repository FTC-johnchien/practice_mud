package com.example.htmlmud.domain.service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 戰鬥命中部位隨機判定器 (純領域機制)
 */
public class BodyPartSelector {

  private static final String[] DEFAULT_PARTS = {"頭部", "胸口", "左臂", "右臂", "小腹", "大腿"};

  public static String getRandomBodyPart() {
    return DEFAULT_PARTS[ThreadLocalRandom.current().nextInt(DEFAULT_PARTS.length)];
  }
}
