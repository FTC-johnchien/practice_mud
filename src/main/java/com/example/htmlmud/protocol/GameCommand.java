package com.example.htmlmud.protocol;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// 定義外部傳來的 JSON 指令
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = GameCommand.Login.class, name = "LOGIN"),
    // @JsonSubTypes.Type(value = GameCommand.Move.class, name = "MOVE"),
    // @JsonSubTypes.Type(value = GameCommand.Chat.class, name = "CHAT"),
    @JsonSubTypes.Type(value = GameCommand.Input.class, name = "INPUT") // <--- 您的核心需求
})
public sealed interface GameCommand {
  // 1. 登入指令 (維持結構化，因為安全性)
  record Login(String username, String password) implements GameCommand {
  }
  // 2. 通用字串指令 (您的 MVP 核心)
  // 玩家輸入 "kill goblin", "look", "north" 全部都包在這裡面
  record Input(String text) implements GameCommand {
  }
  // record Move(String direction) implements GameCommand {}
  // record Chat(String content) implements GameCommand {}
}
