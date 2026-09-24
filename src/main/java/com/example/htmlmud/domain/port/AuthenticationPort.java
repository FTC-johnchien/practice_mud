package com.example.htmlmud.domain.port;

import com.example.htmlmud.domain.model.entity.PlayerRecord;

/**
 * 玩家身分驗證輸出埠口 (Authentication Output Port)
 * 遵循依賴反轉原則，由 Application 層的 AuthService 實作。
 */
public interface AuthenticationPort {

  /**
   * 註冊帳號
   */
  Object register(String username, String password);

  /**
   * 帳號登入並返回玩家存檔實體
   */
  PlayerRecord login(String username, String password);
}
