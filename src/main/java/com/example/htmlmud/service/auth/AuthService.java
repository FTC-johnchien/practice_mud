package com.example.htmlmud.service.auth;

import java.time.LocalDateTime;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.htmlmud.infra.persistence.entity.PlayerEntity;
import com.example.htmlmud.infra.persistence.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final PlayerRepository playerRepository;
  // 使用 BCrypt，Spring Security 內建，或者自己 new 一個
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  /**
   * 註冊新帳號
   */
  @Transactional
  public PlayerEntity register(String username, String rawPassword) {
    if (playerRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("帳號已存在");
    }

    PlayerEntity newPlayer = PlayerEntity.builder().username(username)
        .passwordHash(passwordEncoder.encode(rawPassword)).currentRoomId(1001) // 初始房間
        .build();

    return playerRepository.save(newPlayer);
  }

  /**
   * 登入驗證
   */
  @Transactional
  public PlayerEntity login(String username, String rawPassword) {
    PlayerEntity player = playerRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("帳號不存在"));

    if (!passwordEncoder.matches(rawPassword, player.getPasswordHash())) {
      throw new IllegalArgumentException("密碼錯誤");
    }

    // 更新最後登入時間
    player.setLastLoginAt(LocalDateTime.now());
    return playerRepository.save(player);
  }
}
