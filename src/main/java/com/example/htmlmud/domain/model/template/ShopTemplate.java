package com.example.htmlmud.domain.model.template;

import java.util.List;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.TemplateCatalog;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShopTemplate(
    String id,
    String name,
    String npcId,
    String roomId,
    List<ShopItemTemplate> goods
) {
  public ShopTemplate {
    if (goods == null) goods = List.of();
  }

  @Builder(toBuilder = true)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record ShopItemTemplate(
      int index,
      String id,
      String templateId,
      String name,
      Integer price,
      Double priceMultiplier,
      Integer stock,
      String description
  ) {
    public int getEffectivePrice() {
      return getEffectivePrice(new TemplateCatalog());
    }

    public int getEffectivePrice(TemplateReader templateReader) {
      if (price != null && price > 0) return price;
      ItemTemplate tpl = (templateId != null) ? templateReader.findItem(templateId).orElse(null) : null;
      int baseValue = (tpl != null && tpl.value() > 0) ? tpl.value() : 1;
      if (priceMultiplier != null && priceMultiplier > 0) {
        return Math.max(1, (int) Math.round(baseValue * priceMultiplier));
      }
      return baseValue;
    }

    public String getEffectiveName() {
      return getEffectiveName(new TemplateCatalog());
    }

    public String getEffectiveName(TemplateReader templateReader) {
      if (name != null && !name.isBlank()) return name;
      if (templateId != null) {
        return templateReader.findItem(templateId).map(ItemTemplate::name).orElse(id != null ? id : "未知商品");
      }
      return id != null ? id : "未知商品";
    }

    public String getEffectiveDescription() {
      return getEffectiveDescription(new TemplateCatalog());
    }

    public String getEffectiveDescription(TemplateReader templateReader) {
      if (description != null && !description.isBlank()) return description;
      if (templateId != null) {
        return templateReader.findItem(templateId).map(ItemTemplate::description).orElse("");
      }
      return "";
    }

    public int getEffectiveStock() {
      return (stock != null) ? stock : -1;
    }
  }
}
