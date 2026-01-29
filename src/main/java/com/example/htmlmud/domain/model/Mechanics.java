package com.example.htmlmud.domain.model;

public record Mechanics(

    // A. 傷害部分 (標準 100% 傷害)
    int baseValue,

    Attributes scaleStat, // 吃「屬性 STR, INT, DEX, CON」加成的

    double scaleFactor, // scaleStat的倍數 1.0 (有多少力打多少痛)

    // B. 戰鬥機制
    double hitRateMod, // 額外命中

    double parryMod, // 額外招架

    // C. 附加狀態 (通常為空，或僅有微量擊退)
    // TODO effects

    // D. 連擊 (基礎技能通常是用來「堆疊連擊點」的)
    int comboPointGen // 產生 n 點氣/連擊點，供進階技能消耗

) {

}
