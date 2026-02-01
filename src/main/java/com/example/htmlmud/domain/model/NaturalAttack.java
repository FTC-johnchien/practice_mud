package com.example.htmlmud.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NaturalAttack(

    @JsonProperty("id") String id, // 技能或攻擊的 ID
    @JsonProperty("weight") int weight // 權重，用於計算隨機觸發機率

) implements Weighted {

  // 實作 Weighted 介面要求的方法
  @Override
  public int getWeight() {
    return weight;
  }

  // 為了相容 SkillService 中的 .getId() 呼叫
  // 雖然 record 自帶 id()，但保留 getId() 可以讓舊程式碼不用修改
  public String getId() {
    return id;
  }
}
