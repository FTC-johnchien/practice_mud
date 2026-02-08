package com.example.htmlmud.domain.model.enums;

import lombok.Getter;

@Getter
public enum Gender {
  MALE("男性", "你", "他"), // He, You
  FEMALE("女性", "妳", "她"), // She, You
  NEUTRAL("無性", "你", "它"), // It, You (物品、魔像)
  ANIMAL("動物", "你", "牠"); // It, You (野獸)

  private final String displayName;
  private final String you; // 你/妳 (You)
  private final String he; // 他/她 (He)

  Gender(String displayName, String you, String he) {
    this.displayName = displayName;
    this.you = you;
    this.he = he;
  }
}
