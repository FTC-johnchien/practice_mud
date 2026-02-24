package com.example.htmlmud.protocol.util;

/**
 * ANSI 顏色碼列舉 包含標準色、高亮色、背景色以及 MUD 常用的 256 色擴展
 */
public enum AnsiColor {

  // --- 控制碼 ---
  RESET("\u001B[0m"), BOLD("\u001B[1m"), // 粗體
  DIM("\u001B[2m"), // 弱體
  ITALIC("\u001B[3m"), // 斜體
  UNDERLINE("\u001B[4m"), // 下劃線
  BLINK("\u001B[5m"), // 閃爍
  INVERSE("\u001B[7m"), // 反白
  HIDDEN("\u001B[8m"), // 隱藏
  STRIKETHROUGH("\u001B[9m"), // 刪除線

  // --- 標準前景色 (Standard Foreground) ---
  BLACK("\u001B[30m"), // 常用於深灰
  RED("\u001B[31m"), // 常用於紅色
  GREEN("\u001B[32m"), // 常用於綠色
  YELLOW("\u001B[33m"), // 常用於黃色
  BLUE("\u001B[34m"), // 常用於藍色
  MAGENTA("\u001B[35m"), // 常用於紫紅色
  CYAN("\u001B[36m"), // 常用於青色
  WHITE("\u001B[37m"), // 常用於白色

  BLACK_BOLD("\u001B[30;1m"), // 常用於深灰
  RED_BOLD("\u001B[31;1m"), // 常用於紅色
  GREEN_BOLD("\u001B[32;1m"), // 常用於綠色
  YELLOW_BOLD("\u001B[33;1m"), // 常用於黃色
  BLUE_BOLD("\u001B[34;1m"), // 常用於藍色
  MAGENTA_BOLD("\u001B[35;1m"), // 常用於紫紅色
  CYAN_BOLD("\u001B[36;1m"), // 常用於青色
  WHITE_BOLD("\u001B[37;1m"), // 常用於白色

  // --- 高亮前景色 (Bright Foreground) ---
  BRIGHT_BLACK("\u001B[90m"), // 常用於高亮深灰
  BRIGHT_RED("\u001B[91m"), // 常用於高亮紅色
  BRIGHT_GREEN("\u001B[92m"), // 常用於高亮綠色
  BRIGHT_YELLOW("\u001B[93m"), // 常用於高亮黃色
  BRIGHT_BLUE("\u001B[94m"), // 常用於高亮藍色
  BRIGHT_MAGENTA("\u001B[95m"), // 常用於高亮紫紅色
  BRIGHT_CYAN("\u001B[96m"), // 常用於高亮青色
  BRIGHT_WHITE("\u001B[97m"), // 常用於高亮白色

  // --- MUD 特選 256 色 (Extended Colors) ---
  // 裝備與物品常用色
  ORANGE("\u001B[38;5;208m"), // 傳說物品
  GOLD("\u001B[38;5;220m"), // 金幣、神器
  SILVER("\u001B[38;5;250m"), // 銀幣、金屬
  BRONZE("\u001B[38;5;136m"), // 銅幣
  PINK("\u001B[38;5;205m"), // 特殊/女性角色
  PURPLE("\u001B[38;5;129m"), // 史詩物品

  // 元素與環境常用色
  LAVA_RED("\u001B[38;5;196m"), // 熔岩、致命傷害
  ICE_BLUE("\u001B[38;5;51m"), // 冰凍、魔法
  NATURE_GREEN("\u001B[38;5;46m"), // 劇毒、治療
  DARK_GREY("\u001B[38;5;236m"), // 陰影、潛形
  LIGHT_GREY("\u001B[38;5;244m"), // 石頭、牆壁
  BROWN("\u001B[38;5;94m"), // 木頭、泥土

  // --- 背景色 (Background) ---
  BG_BLACK("\u001B[40m"), // 常用於背景色深灰
  BG_RED("\u001B[41m"), // 常用於背景色紅色
  BG_GREEN("\u001B[42m"), // 常用於背景色綠色
  BG_YELLOW("\u001B[43m"), // 常用於背景色黃色
  BG_BLUE("\u001B[44m"), // 常用於背景色藍色
  BG_MAGENTA("\u001B[45m"), // 常用於背景色紫紅色
  BG_CYAN("\u001B[46m"), // 常用於背景色青色
  BG_WHITE("\u001B[47m"); // 常用於背景色白色

  private final String code;

  AnsiColor(String code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return code;
  }

  public String getCode() {
    return code;
  }
}
