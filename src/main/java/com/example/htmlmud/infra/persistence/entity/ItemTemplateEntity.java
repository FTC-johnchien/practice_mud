package com.example.htmlmud.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "item_templates")
@Getter
@Setter
public class ItemTemplateEntity {

  public ItemTemplateEntity() {}

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String name;

  private String description;

  private String slot;

  @Column(name = "max_durability")
  private Integer maxDurability;

  @Column(name = "base_attack")
  private Integer baseAttack;

  @Column(name = "base_defense")
  private Integer baseDefense;

  private Double chance;

}
