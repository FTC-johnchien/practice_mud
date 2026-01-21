package com.example.htmlmud.application.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.PlayerActor;
import com.example.htmlmud.domain.model.PlayerRecord;
import com.example.htmlmud.infra.mapper.PlayerMapper;
import com.example.htmlmud.infra.persistence.entity.CharacterEntity;
import com.example.htmlmud.infra.persistence.repository.CharacterRepository;
import com.example.htmlmud.infra.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerService {

  private final UserRepository userRepository;

  private final CharacterRepository characterRepository;

  private final PlayerMapper mapper; // 注入 MapStruct

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

  public PlayerRecord loadRecord(String uid, String username) {
    // 1. DB -> Entity
    CharacterEntity entity = characterRepository.findByUidAndName(uid, username)
        .orElseThrow(() -> new IllegalArgumentException("角色不存在 uid:" + uid + ", name:" + username));

    // 2. Entity -> Record (MapStruct 自動轉)
    // 注意：這裡得到的 Record 內含的 State 是 Entity 裡解序列化出來的
    return mapper.toRecord(entity);
  }
}
