package com.example.htmlmud.infra.persistence.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemTemplateEntity {

  public ItemTemplateEntity() {}

  private String id;
  private String name;
  private String description;
  private String slot;
  private Integer maxDurability;
  private Integer baseAttack;
  private Integer baseDefense;
  private Double chance;
}
