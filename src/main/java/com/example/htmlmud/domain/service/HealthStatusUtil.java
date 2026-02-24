package com.example.htmlmud.domain.service;

public class HealthStatusUtil {

  // 定義一個簡單的狀態判斷
  public static String getLookHealthStatus(int hp, int maxHp) {
    double pct = (double) hp / maxHp;
    if (pct >= 0.9)
      return "[狀態佳]";
    if (pct > 0.75)
      return "[受輕傷]";
    if (pct > 0.5)
      return "[受　傷]";
    if (pct > 0.2)
      return "[受重傷]";

    return "[瀕　死]";
  }

  public static String getHealthStatus(int hp, int maxHp) {
    double pct = (double) hp / maxHp;

    if (pct >= 1)
      return "$N看起來氣血充盈﹐並沒有受傷。";
    if (pct >= 0.9)
      return "$N似乎有些疲憊﹐但是仍然十分有活力。";
    if (pct >= 0.8)
      return "$N看起來可能有些累了。";
    if (pct >= 0.7)
      return "$N動作似乎開始有點不太靈光﹐但是仍然有條不紊。";
    if (pct >= 0.6)
      return "$N氣喘噓噓﹐看起來狀況並不太好。";
    if (pct >= 0.5)
      return "$N似乎十分疲憊﹐看來需要好好休息了。";
    if (pct >= 0.4)
      return "$N看起來已經力不從心了。";
    if (pct >= 0.3)
      return "$N已經一副頭重腳輕的模樣﹐正在勉力支撐著不倒下去。";
    if (pct >= 0.2)
      return "$N搖頭晃腦、歪歪斜斜地站都站不穩﹐眼看就要倒在地上。";

    return "$N已經陷入半昏迷狀態﹐隨時都可能摔倒暈去。";
  }

}
