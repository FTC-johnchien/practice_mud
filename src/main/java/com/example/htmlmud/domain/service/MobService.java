package com.example.htmlmud.domain.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@Getter
@RequiredArgsConstructor
public class MobService {

  private final ObjectProvider<LivingService> livingServiceProvider;

  public Optional<Living> getHighestAggroTarget(Mob mob) {

    // 優化：一次取得房間內所有生物，避免多次跨執行緒通訊
    List<Living> roomLivings = mob.getCurrentRoom().getLivings();
    if (roomLivings.isEmpty()) {
      return Optional.empty();
    }

    return mob.getAggroTable().entrySet().stream()
        .flatMap(entry -> roomLivings.stream()
            .filter(l -> l.getId().equals(entry.getKey()) && l.isValid())
            .map(l -> Map.entry(entry.getValue(), l)))
        .max(Map.Entry.comparingByKey()).map(Map.Entry::getValue);
  }
}
