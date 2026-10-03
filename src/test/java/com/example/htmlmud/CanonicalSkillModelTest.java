package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.model.config.Costs;
import com.example.htmlmud.domain.model.config.Mechanics;
import com.example.htmlmud.domain.model.definition.DrpgSkillRules;
import com.example.htmlmud.domain.model.definition.MudSkillRules;
import com.example.htmlmud.domain.model.definition.SkillBridgeRule;
import com.example.htmlmud.domain.model.definition.SkillDefinition;
import com.example.htmlmud.domain.model.enums.DamageType;
import com.example.htmlmud.domain.model.enums.ResourceType;
import com.example.htmlmud.domain.model.enums.SkillType;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.service.SkillBridgeService;
import com.example.htmlmud.domain.service.TemplateCatalog;

public class CanonicalSkillModelTest {

  @Test
  @DisplayName("CANON-02: SkillDefinition parses MUD template with MUD & DRPG facets")
  void testSkillDefinitionFromMudTemplate() {
    SkillTemplate tpl = new SkillTemplate();
    tpl.setId("test_taiji_sword");
    tpl.setName("太極神劍");
    tpl.setDescription("以柔克剛之太極劍意");
    tpl.setType(SkillType.ACTIVE);
    tpl.setSchool("SWORD");
    tpl.setTags(Set.of("WEAPON", "SWORD", "COMBO"));

    Costs costs = new Costs(0, 0, 15, 2, 0);
    tpl.setCosts(costs);

    Mechanics mechanics = Mechanics.builder()
        .damage(25)
        .critRate(0.1)
        .hitRate(0.9)
        .scaleFactor(1.8)
        .scaleStat(ResourceType.STR)
        .damageType(DamageType.SLASH)
        .build();
    tpl.setMechanics(mechanics);

    SkillDefinition def = tpl.toDefinition();
    assertNotNull(def);
    assertEquals("test_taiji_sword", def.id());
    assertEquals("太極神劍", def.name());
    assertEquals(SkillType.ACTIVE, def.type());
    assertEquals("SWORD", def.school());
    assertTrue(def.hasMudRules());
    assertTrue(def.hasDrpgRules());
    assertEquals(15, def.mudRules().costs().sp());

    // DRPG Facet validation
    DrpgSkillRules drpg = def.drpgRules();
    assertNotNull(drpg);
    assertEquals(CombatResourceType.COMBO, drpg.costType());
    assertEquals(2, drpg.costValue());
    assertEquals(1.8, drpg.damageMultiplier());
  }

  @Test
  @DisplayName("CANON-02: SkillDefinition parses PartyMemberSkill into canonical DRPG rules")
  void testSkillDefinitionFromPartySkill() {
    PartyMemberSkill pms = PartyMemberSkill.builder()
        .id("cleric_radiance")
        .name("太陰普照")
        .icon("✨")
        .description("恢復全員氣血與道心")
        .costType(CombatResourceType.MP)
        .costValue(30)
        .cooldownMs(12000L)
        .damageMultiplier(1.0)
        .aoe(true)
        .heal(true)
        .healAmount(60)
        .sanRestore(15)
        .build();

    SkillDefinition def = SkillDefinition.fromPartyMemberSkill(pms);
    assertNotNull(def);
    assertEquals("cleric_radiance", def.id());
    assertEquals("太陰普照", def.name());
    assertEquals("✨", def.icon());
    assertTrue(def.hasDrpgRules());
    assertFalse(def.hasMudRules());

    DrpgSkillRules drpg = def.drpgRules();
    assertTrue(drpg.aoe());
    assertTrue(drpg.heal());
    assertEquals(60, drpg.healAmount());
    assertEquals(15, drpg.sanRestore());

    PartyMemberSkill backPms = def.toPartyMemberSkill();
    assertEquals("cleric_radiance", backPms.getId());
    assertEquals(60, backPms.getHealAmount());
    assertEquals(15, backPms.getSanRestore());
  }

  @Test
  @DisplayName("CANON-02: SkillTemplate with custom bridges overrides default bridge rules")
  void testCustomSkillBridges() {
    SkillTemplate customTpl = new SkillTemplate();
    customTpl.setId("custom_martial_art");
    customTpl.setName("乾坤伏魔拳");
    customTpl.setBridges(List.of(
        SkillBridgeRule.of("tank_taunt", 1),
        SkillBridgeRule.of("tank_smash", 4)
    ));

    SkillDefinition def = customTpl.toDefinition();
    assertNotNull(def);
    assertTrue(def.hasBridgeRules());
    assertEquals(2, def.bridgeRules().size());
    assertEquals("tank_taunt", def.bridgeRules().get(0).targetDrpgSkillId());
    assertEquals(1, def.bridgeRules().get(0).requiredLevel());
    assertEquals("tank_smash", def.bridgeRules().get(1).targetDrpgSkillId());
    assertEquals(4, def.bridgeRules().get(1).requiredLevel());
  }

  @Test
  @DisplayName("CANON-02: SkillBridgeService maps skills dynamically via SkillDefinition")
  void testSkillBridgeServiceDynamicMapping() {
    SkillBridgeService bridgeService = new SkillBridgeService(new TemplateCatalog());

    // Basic sword lvl 1 -> sword_pierce; lvl 5 -> sword_pierce + sword_storm
    List<String> lowLvl = bridgeService.mapMudSkillToDrpgSkills("basic_sword", 1);
    assertEquals(List.of("sword_pierce"), lowLvl);

    List<String> highLvl = bridgeService.mapMudSkillToDrpgSkills("basic_sword", 5);
    assertEquals(List.of("sword_pierce", "sword_storm"), highLvl);
  }
}

