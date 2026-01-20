package com.example.htmlmud.infra.util;

public class ColorText {

  // 1. 基礎包裝方法
  public static String wrap(AnsiColor color, String text) {
    return color + text + AnsiColor.RESET;
  }

  // 2. 複合樣式 (例如：粗體紅色)
  public static String wrap(AnsiColor color, AnsiColor style, String text) {
    return style + "" + color + text + AnsiColor.RESET;
  }

  // --- 語義化 Helper Methods (MUD 專用) ---

  // 系統訊息 (青色)
  public static String system(String text) {
    return wrap(AnsiColor.CYAN, text);
  }

  // 錯誤訊息 (紅色)
  public static String error(String text) {
    return wrap(AnsiColor.RED, text);
  }

  // 警告訊息 (黃色)
  public static String warn(String text) {
    return wrap(AnsiColor.YELLOW, text);
  }

  // NPC 名稱 (綠色)
  public static String npc(String name) {
    return wrap(AnsiColor.GREEN, name);
  }

  // 怪物名稱 (紅色粗體)
  public static String mob(String name) {
    return wrap(AnsiColor.RED, AnsiColor.BOLD, name);
  }

  // 玩家名稱 (亮藍色)
  public static String player(String name) {
    return wrap(AnsiColor.BRIGHT_BLUE, name);
  }

  // 物品 (金色)
  public static String item(String name) {
    return wrap(AnsiColor.GOLD, name);
  }

  // 傷害數值 (熔岩紅)
  public static String damage(int dmg) {
    return wrap(AnsiColor.LAVA_RED, String.valueOf(dmg));
  }

  // 治療數值 (自然綠)
  public static String heal(int amount) {
    return wrap(AnsiColor.NATURE_GREEN, String.valueOf(amount));
  }

  // 房間名稱 (亮白色)
  public static String room(String name) {
    return wrap(AnsiColor.BRIGHT_WHITE, name);
  }

  // 房間描述 (預設色/灰色)
  public static String roomDesc(String desc) {
    return wrap(AnsiColor.LIGHT_GREY, desc);
  }

  // 出口名稱 (黃色)
  public static String exit(String name) {
    return wrap(AnsiColor.YELLOW, name);
  }

  // 物品描述 (白色)
  public static String itemDesc(String desc) {
    return wrap(AnsiColor.WHITE, desc);
  }

  // 物品稀有度 (紫色)
  public static String rarity(String rarity) {
    return wrap(AnsiColor.PURPLE, rarity);
  }
}
