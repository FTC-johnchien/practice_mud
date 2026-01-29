package com.example.htmlmud.domain.model;

public record Timing(

    int castTime,

    int cooldown,

    int gcd, // 0 = 使用全域設定，填數值則覆蓋 (如盜賊技能可能只需 1000ms)

    boolean triggerGcd, // 施放後是否觸發 GCD？(預設 true)

    boolean ignoreGcd, // 是否可在 GCD 轉圈時施放？(預設 false)

    boolean interruptible // 可被打斷

) {

}
