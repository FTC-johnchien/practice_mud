package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
class MozhuMinesIntegrationTest {

  @Autowired
  private WorldManager worldManager;

  @Test
  void testMozhuMinesZoneLoaded() {
    Optional<ZoneTemplate> zoneOpt = TemplateRepository.findZone("mozhu_mines");
    assertTrue(zoneOpt.isPresent(), "Zone mozhu_mines 應該成功載入");
    assertEquals("墨竹山廢棄靈石礦坑", zoneOpt.get().name());
  }

  @Test
  void testMozhuMinesRoomsLoadedAndConnected() {
    Optional<RoomTemplate> entrance = TemplateRepository.findRoom("mozhu_mines:mine_entrance");
    assertTrue(entrance.isPresent(), "mine_entrance 房間應存在");
    assertNotNull(entrance.get().exits());
    assertTrue(entrance.get().exits().containsKey("down"), "mine_entrance 應有 down 出口");
    assertTrue(entrance.get().exits().containsKey("east"), "mine_entrance 應有連回新手村的正東出口");

    Optional<RoomTemplate> altar = TemplateRepository.findRoom("mozhu_mines:void_moon_altar");
    assertTrue(altar.isPresent(), "void_moon_altar (Boss 房) 應存在");
    assertTrue(altar.get().exits().containsKey("up"), "altar 應有通往 elder_vent_shaft 的向上出口");
  }

  @Test
  void testCorruptedSkillsLoaded() {
    Optional<SkillTemplate> fleshRend = TemplateRepository.findSkill("flesh_rend");
    assertTrue(fleshRend.isPresent(), "血肉撕裂 (flesh_rend) 技能應成功註冊");
    assertEquals("血肉撕裂", fleshRend.get().getName());

    Optional<SkillTemplate> gorging = TemplateRepository.findSkill("gorging_corruption");
    assertTrue(gorging.isPresent(), "煞氣暴食 (gorging_corruption) 技能應成功註冊");

    Optional<SkillTemplate> terminalStrike = TemplateRepository.findSkill("terminal_strike");
    assertTrue(terminalStrike.isPresent(), "終焉解構 (terminal_strike) 技能應成功註冊");
    assertEquals("終焉解構", terminalStrike.get().getName());
  }

  @Test
  void testMobsAndBossLoaded() {
    Optional<MobTemplate> boss = TemplateRepository.findMob("mozhu_mines:boss_song_tianheng");
    assertTrue(boss.isPresent(), "畸變大師兄·宋天衡 Boss 模板應存在");
    assertEquals(450, boss.get().maxHp());

    Optional<MobTemplate> servant = TemplateRepository.findMob("mozhu_mines:corrupted_servant");
    assertTrue(servant.isPresent(), "煞氣異化雜役 模板應存在");

    Optional<MobTemplate> beast = TemplateRepository.findMob("mozhu_mines:shao_crystal_beast");
    assertTrue(beast.isPresent(), "煞氣結晶獸 模板應存在");
  }

  @Test
  void testItemsLoaded() {
    Optional<ItemTemplate> scythe = TemplateRepository.findItem("mozhu_mines:black_obsidian_scythe");
    assertTrue(scythe.isPresent(), "黑曜骨鐮 武器應存在");

    Optional<ItemTemplate> token = TemplateRepository.findItem("mozhu_mines:elder_token");
    assertTrue(token.isPresent(), "長老黑話玉牌 應存在");
  }
}
