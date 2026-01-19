package com.example.htmlmud.infra.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "item_templates")
@Data
public class ItemTemplateEntity {

  @Id
  private Integer id;

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
