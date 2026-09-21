package com.example.htmlmud.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.service.DrpgTemplateAdapter;
import com.example.htmlmud.domain.service.MudTemplateAdapter;
import com.example.htmlmud.domain.service.TemplateCatalog;

@SpringBootTest
@ActiveProfiles("test")
class SharedTemplateRegistryTest {

  @Autowired
  private TemplateCatalog templateCatalog;

  @Autowired
  private MudTemplateAdapter mudTemplateAdapter;

  @Autowired
  private DrpgTemplateAdapter drpgTemplateAdapter;

  @Test
  @DisplayName("共享模板 registry 應可透過 canonical entrypoint 解析真實物品與技能，且 MUD/DRPG adapter 皆指向同一份資料")
  void resolvesRealItemAndSkillThroughSharedRegistry() {
    ItemTemplate item = templateCatalog.resolveItem("iron_sword");
    assertThat(item).isNotNull();
    assertThat(item.id()).isEqualTo("iron_sword");
    assertThat(item.name()).isEqualTo("精鋼劍");
    assertThat(item.subType()).isEqualTo("SWORD");

    SkillTemplate skill = templateCatalog.resolveSkill("basic_sword");
    assertThat(skill).isNotNull();
    assertThat(skill.getId()).isEqualTo("basic_sword");
    assertThat(skill.getName()).isEqualTo("基本劍法");

    assertThat(mudTemplateAdapter.findItem("iron_sword").orElseThrow().id()).isEqualTo(item.id());
    assertThat(drpgTemplateAdapter.findSkill("basic_sword").orElseThrow().getId()).isEqualTo(skill.getId());
    assertThat(mudTemplateAdapter.findSkill("basic_sword").orElseThrow().getId()).isEqualTo(skill.getId());
    assertThat(drpgTemplateAdapter.findItem("iron_sword").orElseThrow().id()).isEqualTo(item.id());
  }
}
