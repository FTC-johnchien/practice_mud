package com.example.htmlmud.template;

import java.util.Map;
import com.example.htmlmud.template.model.ClassTemplate;
import com.example.htmlmud.template.model.CompanionTemplate;
import com.example.htmlmud.template.model.ItemTemplate;
import com.example.htmlmud.template.model.MobTemplate;
import com.example.htmlmud.template.model.RaceTemplate;
import com.example.htmlmud.template.model.RoomTemplate;
import com.example.htmlmud.template.model.ShopTemplate;
import com.example.htmlmud.template.model.SkillTemplate;
import com.example.htmlmud.template.model.ZoneTemplate;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.template.TemplateReader;

/**
 * 模板資料註冊與存儲埠 (Template Registry Port)
 * 遵循整潔架構 (Clean Architecture)，定義世界模板的載入、註冊與完整性校驗合約。
 */
public interface TemplateRegistryPort extends TemplateReader {

  /**
   * 校驗所有載入的模板關聯完整性 (如出口房間是否存在、預設技能是否存在)
   */
  void validateData();

  void addItem(ItemTemplate item);

  void addClass(ClassTemplate classTemplate);

  void addPartySkill(PartyMemberSkill skill);

  void addCompanion(CompanionTemplate companion);

  void addFormation(FormationTemplate formation);

  void addRace(RaceTemplate race);

  void addSkill(SkillTemplate skill);

  void addZone(ZoneTemplate zoneTemplate);

  void addMob(MobTemplate mob);

  void addRoom(RoomTemplate room);

  void addShop(ShopTemplate shop);

  Map<String, RoomTemplate> getRoomTemplateMap();
}
