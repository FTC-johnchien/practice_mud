package com.example.htmlmud.application.service;

import java.time.LocalDateTime;
import java.util.Set;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.entity.PlayerRecord;
import com.example.htmlmud.infra.mapper.PlayerMapper;
import com.example.htmlmud.infra.persistence.entity.CharacterEntity;
import com.example.htmlmud.infra.persistence.entity.UserEntity;
import com.example.htmlmud.infra.persistence.repository.CharacterRepository;
import com.example.htmlmud.infra.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

  // 基礎保留字（非指令類的關鍵字）
  private static final Set<String> RESERVED_WORDS =
      Set.of("new", "quit", "exit", "wizard", "admin", "system", "root", "guest", "player");

  // 使用 BCrypt，Spring Security 內建，或者自己 new 一個
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  private final PlayerMapper mapper; // 注入 MapStruct

  private final UserRepository userRepository;

  private final CharacterRepository playerRepository;

  private final CharacterRepository characterRepository;



  public boolean exists(String username) {
    return userRepository.existsByUsername(username);
  }

  /**
   * 註冊新帳號
   */
  @Transactional
  public CharacterEntity register(String username, String rawPassword) {
    if (userRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("帳號已存在");
    }

    LocalDateTime now = LocalDateTime.now();
    UserEntity newUser = UserEntity.builder().username(username)
        .passwordHash(passwordEncoder.encode(rawPassword)).createdAt(now).lastLoginAt(now).build();
    userRepository.save(newUser);


    // TODO 待處理創角的設定
    CharacterEntity characterEntity = new CharacterEntity();
    characterEntity.setUid(newUser.getId());
    characterEntity.setName(username);
    characterEntity.setNickname(username);
    characterEntity.setCurrentRoomId(null);
    characterEntity.setState(new LivingStats());
    characterEntity.setCreatedAt(now);
    characterEntity.setModifyAt(now);

    return playerRepository.save(characterEntity);
  }

  /**
   * 登入驗證
   */
  @Transactional
  public PlayerRecord login(String username, String rawPassword) {
    // log.info("login username:{} password: {}", username, rawPassword);
    UserEntity user = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("帳號不存在"));

    if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
      throw new IllegalArgumentException("密碼錯誤");
    }

    // 更新最後登入時間
    user.setLastLoginAt(LocalDateTime.now());
    userRepository.save(user);
    log.info("玩家登入成功: {}", username);

    // -----------------------------------------------------------------------------
    // 進入角色設定流程
    // -----------------------------------------------------------------------------

    // 取出角色 player 資料
    return loadRecord(user.getId(), username);
  }


  /**
   * 驗證用戶名是否合法
   *
   * @return 錯誤訊息，若合法則回傳 null
   */
  public String validateUsername(String input) {
    if (input == null || input.isEmpty()) {
      return "帳號名稱不能為空。";
    }
    if (!input.matches("^[a-zA-Z]+$")) {
      return "帳號名稱只能包含英文字母（不分大小寫），不允許數字、空格或特殊符號。";
    }
    if (input.length() < 4 || input.length() > 20) {
      return "帳號名稱長度必須在 4 到 20 個字元之間。";
    }
    if (RESERVED_WORDS.contains(input.toLowerCase())) {
      return "「" + input + "」是系統保留字或指令，請選擇其他名稱。";
    }
    return null;
  }

  /**
   * 驗證密碼是否合法
   *
   * @param input 輸入的密碼
   * @return 錯誤訊息，若合法則回傳 null
   */
  public String validatePassword(String input) {
    if (input == null || input.isEmpty()) {
      return "密碼不能為空。";
    }
    if (!input.matches("^[a-zA-Z0-9]+$")) {
      return "密碼只能包含英文字母與數字。";
    }
    if (input.length() < 4 || input.length() > 32) {
      return "密碼長度必須在 4 到 32 個字元之間。";
    }
    return null;
  }


  // private boolean verifyPassword(String username, String password) {
  // String stored = userDb.get(username.toLowerCase());
  // return stored != null && stored.equals(password);
  // }

  private PlayerRecord loadRecord(String uid, String username) {
    // 1. DB -> Entity
    CharacterEntity entity = characterRepository.findByUidAndName(uid, username)
        .orElseThrow(() -> new IllegalArgumentException("角色不存在 uid:" + uid + ", name:" + username));

    // 2. Entity -> Record (MapStruct 自動轉)
    // 注意：這裡得到的 Record 內含的 State 是 Entity 裡解序列化出來的
    return mapper.toRecord(entity);
  }
}
