package com.example.htmlmud.protocol;

public enum ConnectionState {
  CONNECTED, // 剛連線：等待輸入帳號 或 'new'
  REGISTER_USERNAME, // 註冊中：等待輸入新帳號
  REGISTER_PASSWORD, // 註冊中：等待輸入新密碼
  LOGIN_PASSWORD, // 登入中：等待輸入密碼
  PLAYING // 遊戲中：正常遊玩
}
