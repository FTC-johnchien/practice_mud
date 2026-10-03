package com.example.htmlmud.domain.port;

import java.util.Map;
import com.example.htmlmud.domain.model.template.ClassTemplate;
import com.example.htmlmud.domain.model.template.CompanionTemplate;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.RaceTemplate;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.ShopTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.repository.TemplateReader;

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
