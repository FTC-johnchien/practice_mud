package com.example.htmlmud.town;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import com.example.htmlmud.town.FacilityDto;
import com.example.htmlmud.town.TownHubDto;
import com.example.htmlmud.town.PerceptionTier;
import com.example.htmlmud.template.model.FacilityTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 營地機能設施中樞服務 (Town Hub Service)
 * 核心職責：加載 facilities.json，透過 CognitiveProxyService 進行表層仙俠化投影，管理休整調息與設施互動。
 * 對齊 Level 2《M1_TOWN_HUB_AND_FACILITIES.md》
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TownHubService {

  private final CognitiveProxyService cognitiveProxyService;
  private final ObjectMapper objectMapper;

  private final Map<String, List<FacilityTemplate>> zoneFacilitiesCache = new ConcurrentHashMap<>();

  public record CampRestResult(
      boolean success,
      String message,
      int healedMembersCount
  ) {}

  @PostConstruct
  public void init() {
    loadZoneFacilities("newbie_village", "data/zones/newbie_village/facilities.json");
  }

  public synchronized void loadZoneFacilities(String zoneId, String resourcePath) {
    try {
      ClassPathResource resource = new ClassPathResource(resourcePath);
      if (resource.exists()) {
        try (InputStream is = resource.getInputStream()) {
          List<FacilityTemplate> templates = objectMapper.readValue(is, new TypeReference<List<FacilityTemplate>>() {});
          zoneFacilitiesCache.put(zoneId, templates);
          log.info("成功加載營地設施範本 [{}]，共計 {} 項設施", zoneId, templates.size());
        }
      } else {
        log.warn("營地設施資料檔案不存在: {}", resourcePath);
      }
    } catch (Exception e) {
      log.error("加載營地設施範本失敗 [{}]: {}", zoneId, e.getMessage(), e);
    }
  }

  public List<FacilityTemplate> getRawFacilities(String zoneId) {
    return zoneFacilitiesCache.getOrDefault(zoneId, Collections.emptyList());
  }

  /**
   * 獲取指定營地的投影聚合 DTO（經 CognitiveProxyService 轉譯）
   */
  public TownHubDto getTownHub(String zoneId, int chipRate) {
    String actualZoneId = (zoneId != null && !zoneId.isBlank()) ? zoneId : "newbie_village";
    List<FacilityTemplate> templates = zoneFacilitiesCache.getOrDefault(actualZoneId, Collections.emptyList());

    List<FacilityDto> facilityDtos = new ArrayList<>();
    for (FacilityTemplate template : templates) {
      var resolved = cognitiveProxyService.resolve(
          template.name(),
          template.description(),
          template.lenses(),
          chipRate
      );

      FacilityDto dto = FacilityDto.builder()
          .id(template.id())
          .name(resolved.name())
          .facilityType(template.facilityType())
          .icon(template.icon())
          .description(resolved.description())
          .npcId(template.npcId())
          .npcName(template.npcName())
          .targetDungeons(template.targetDungeons())
          .actions(template.actions())
          .isOpen(template.isOpen())
          .glitch(resolved.glitch())
          .build();

      facilityDtos.add(dto);
    }

    PerceptionTier tier = cognitiveProxyService.resolveTier(chipRate);

    return TownHubDto.builder()
        .zoneId(actualZoneId)
        .zoneName("黑煞宗採掘通鋪營地")
        .description("萬年母神卵鞘偽地殼邊緣的避煞避難所，散修與採礦雜役苟延殘喘之安全據點。")
        .chipRate(chipRate)
        .perceptionTier(tier)
        .facilities(facilityDtos)
        .build();
  }

  public TownHubDto getTownHub(int chipRate) {
    return getTownHub("newbie_village", chipRate);
  }

  /**
   * 執行營地全隊休整調息 (Rest & Full Recovery)
   * 氣血 (HP)、真元 (MP) 與精神防護罩 (SAN) 回滿，清除【精神萎靡】弱化狀態
   */
  public CampRestResult performCampRest(Party party) {
    if (party == null || party.getMembers() == null || party.getMembers().isEmpty()) {
      return new CampRestResult(false, "小隊為空，無法進行休整！", 0);
    }

    int count = 0;
    for (PartyMember member : party.getMembers()) {
      if (member == null) continue;

      // 1. 回滿氣血與真元
      if (member.getStats() != null) {
        member.getStats().setHp(member.getStats().getMaxHp());
        member.getStats().setMp(member.getStats().getMaxMp());
      }

      // 2. 回滿精神防護罩
      member.setCurrentSan(member.getMaxSan());

      // 3. 恢復神智清明，解除精神萎靡
      member.setMadnessState(PartyMember.MadnessState.SANE);
      member.setAlive(true);

      count++;
    }

    return new CampRestResult(
        true,
        "🛌 小隊在回春客棧燃香調息，吐納天地靈氣，全員氣血真元與精神防護罩完全恢復，精神萎靡狀態已消散！",
        count
    );
  }
}
