package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
public class ClassSkillAndSynergyTest {

  @Autowired
  private PartyService partyService;

  @Test
  @DisplayName("驗證職業特徵技能已作為標準 SkillTemplate 成功載入")
  void testClassSkillsLoadedAsCanonicalTemplates() {
    Optional<SkillTemplate> taunt = TemplateRepository.findSkill("class_warrior_taunt");
    assertTrue(taunt.isPresent(), "金剛怒目 (class_warrior_taunt) 應作為標準技能載入");
    assertEquals("金剛怒目", taunt.get().getName());
    assertTrue(taunt.get().getTags().contains("TAUNT"));

    Optional<SkillTemplate> heal = TemplateRepository.findSkill("class_cleric_heal");
    assertTrue(heal.isPresent(), "九轉回春 (class_cleric_heal) 應作為標準技能載入");
    assertTrue(heal.get().getTags().contains("HEAL"));

    Optional<SkillTemplate> stealth = TemplateRepository.findSkill("class_rogue_stealth");
    assertTrue(stealth.isPresent(), "斂息匿形 (class_rogue_stealth) 應作為標準技能載入");

    Optional<SkillTemplate> ironBody = TemplateRepository.findSkill("class_monk_iron_body");
    assertTrue(ironBody.isPresent(), "不滅金身 (class_monk_iron_body) 應作為標準技能載入");
  }

  @Test
  @DisplayName("驗證 SkillTemplate 能自動適配為 DRPG PartyMemberSkill 且不限武器")
  void testClassSkillAdaptationToDrpgPartySkill() {
    Optional<PartyMemberSkill> opt = TemplateRepository.findPartySkill("class_warrior_taunt");
    assertTrue(opt.isPresent(), "findPartySkill 應能查詢並自動適配 class_warrior_taunt");
    PartyMemberSkill skill = opt.get();
    assertTrue(skill.isTaunt(), "嘲諷特徵應為 true");
    assertTrue(skill.getAllowedWeapons().isEmpty(), "職業技能應不限武器 (allowedWeapons 為空)");
    assertTrue(skill.isWeaponAllowed("SWORD"), "赤手或任何武器均應可施展嘲諷");
    assertTrue(skill.isWeaponAllowed("DAGGER"), "赤手或任何武器均應可施展嘲諷");
    assertTrue(skill.isWeaponAllowed(null), "無武器時亦應可施展嘲諷");
  }

  @Test
  @DisplayName("驗證舊技能 ID 與別名相容性")
  void testLegacySkillAliasCompatibility() {
    Optional<PartyMemberSkill> legacyTaunt = TemplateRepository.findPartySkill("tank_taunt");
    assertTrue(legacyTaunt.isPresent(), "tank_taunt 應自動兼容");
    assertTrue(legacyTaunt.get().isTaunt());

    Optional<PartyMemberSkill> legacyHeal = TemplateRepository.findPartySkill("heal_single");
    assertTrue(legacyHeal.isPresent(), "heal_single 應自動兼容");
    assertTrue(legacyHeal.get().isHeal());
  }

  @Test
  @DisplayName("驗證同伴初始化時自動掛載所屬職業之特徵技能")
  void testCompanionAutoAttachesClassSkills() {
    PartyMember tieNiu = partyService.createCompanion("tie_niu");
    assertNotNull(tieNiu, "鐵牛應能成功實例化");

    boolean hasTaunt = tieNiu.getSkills().stream()
        .anyMatch(s -> s.getId().contains("taunt") || s.getName().contains("金剛怒目"));
    assertTrue(hasTaunt, "鐵牛作為戰士 (WARRIOR)，應自動掛載金剛怒目嘲諷技能");

    boolean hasIronWall = tieNiu.getSkills().stream()
        .anyMatch(s -> s.getId().equals("class_warrior_iron_wall"));
    assertTrue(hasIronWall, "鐵牛應自動掛載不動明王盾牆技能");

    PartyMember lingShuang = partyService.createCompanion("ling_shuang");
    assertNotNull(lingShuang, "凌霜應能成功實例化");

    boolean hasHeal = lingShuang.getSkills().stream()
        .anyMatch(s -> s.isHeal() || s.getName().contains("九轉回春"));
    assertTrue(hasHeal, "凌霜作為醫修 (CLERIC)，應自動掛載治療技能");
  }

  @Test
  @DisplayName("驗證小隊合擊技能 (Party Combo Skills) 資料驅動正確解析")
  void testPartyComboSkillsLoading() {
    Optional<PartyMemberSkill> comboBlade = TemplateRepository.findPartySkill("combo_blade_and_shadow");
    assertTrue(comboBlade.isPresent(), "刀劍合璧・斷空破合擊技能應載入");
    PartyMemberSkill skill = comboBlade.get();
    assertTrue(skill.isSynergy(), "應標記為合擊技能 (synergy=true)");
    assertEquals(2, skill.getMinPartyAlive(), "最少需 2 人存活");
    assertTrue(skill.getRequiredClasses().contains("WARRIOR"), "需求戰士職業參與");
    assertTrue(skill.getRequiredClasses().contains("ROGUE"), "需求刺客職業參與");

    Optional<PartyMemberSkill> comboFourSymbols = TemplateRepository.findPartySkill("combo_four_symbols_seal");
    assertTrue(comboFourSymbols.isPresent(), "四象降魔大陣合擊技能應載入");
    assertEquals("formation_four_symbols", comboFourSymbols.get().getRequiredFormation(), "需求四象陣法");
    assertEquals(100, comboFourSymbols.get().getFormationEnergyCost(), "需求 100 靈威");
  }
}
