package com.example.htmlmud.protocol;

public enum ConnectionState {
  CONNECTED, // 剛連線：等待輸入帳號 或 'new'
  ENTERING_PASSWORD, // 已輸入帳號，等待密碼
  CREATING_USERNAME, // 正在輸入新帳號名稱
  CREATING_PASSWORD, // 正在設定新密碼
  ENTERING_CHAR_NAME, // 正在輸入新角色名稱
  ENTERING_CHAR_GENDER, // 正在選擇性別
  ENTERING_CHAR_RACE, // 正在選擇種族
  ENTERING_CHAR_CLASS, // 正在選擇職業
  ENTERING_CHAR_ATTRIBUTES, // 正在選擇等級
  LINK_DEAD, // 玩家已斷線
  DISCONNECTED, // 關閉連線
  IN_GAME // 遊戲中：正常遊玩
}
