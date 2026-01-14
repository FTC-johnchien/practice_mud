package com.example.htmlmud.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PlayerService {

  // 模擬 DB: Username -> Password
  private final Map<String, String> userDb = new ConcurrentHashMap<>();

  public PlayerService() {
    // 預設一個測試帳號 (帳號: admin, 密碼: 123)
    userDb.put("admin", "123");
  }

  public boolean exists(String username) {
    return userDb.containsKey(username.toLowerCase());
  }

  public void register(String username, String password) {
    userDb.put(username.toLowerCase(), password);
  }

  public boolean verifyPassword(String username, String password) {
    String stored = userDb.get(username.toLowerCase());
    return stored != null && stored.equals(password);
  }

  // 檢查輸入是否為系統保留字 (防止玩家取名叫做 "new" 或 "quit")
  public boolean isReservedWord(String input) {
    return switch (input.trim().toLowerCase()) {
      case "new", "quit", "exit", "login", "look", "kill", "move", "help" -> true;
      default -> false;
    };
  }
}
