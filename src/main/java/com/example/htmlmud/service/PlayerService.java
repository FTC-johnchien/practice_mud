package com.example.htmlmud.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.catalina.User;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.PlayerActor;
import com.example.htmlmud.domain.model.PlayerRecord;
import com.example.htmlmud.infra.persistence.entity.CharacterEntity;
import com.example.htmlmud.infra.persistence.repository.CharacterRepository;
import com.example.htmlmud.infra.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerService {

  private final UserRepository userRepository;

  private final CharacterRepository playerRepository;

  // 模擬 DB: Username -> Password
  private final Map<String, String> userDb = new ConcurrentHashMap<>();

  // public PlayerService() {
  // // 預設一個測試帳號 (帳號: admin, 密碼: 123)
  // userDb.put("admin", "123");
  // }



  // public void register(PlayerActor actor) {

  // userDb.put(username.toLowerCase(), password);
  // }

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

  public PlayerRecord loadCharacter(Integer accountId, String charName) {
    CharacterEntity charEntity = playerRepository.findByAccountIdAndName(accountId, charName)
        .orElseThrow(() -> new IllegalArgumentException("角色不存在"));
    return new PlayerRecord(charEntity.getId(), charEntity.getName(), charEntity.getDisplayName(),
        charEntity.getCurrentRoomId(), charEntity.getState());
  }
}
