package com.example.htmlmud.domain.model.map;

import java.util.List;
import java.util.Set;
import com.example.htmlmud.domain.model.Costs;
import com.example.htmlmud.domain.model.DefaultConfig;
import com.example.htmlmud.domain.model.LearningConfig;
import com.example.htmlmud.domain.model.Mechanics;
import com.example.htmlmud.domain.model.MoveAction;
import com.example.htmlmud.domain.model.ScalingConfig;
import com.example.htmlmud.domain.model.SkillType;
import com.example.htmlmud.domain.model.SynergiesConfig;
import com.example.htmlmud.domain.model.UsageConfig;
import lombok.Data;

@Data
public class SkillTemplate {

  // 基礎資訊
  String id;

  String name;

  String description;

  SkillType type; // UNARMED, WEAPON, MAGIC, PASSIVE

  String school;

  Set<String> tags;

  boolean isHidden;

  // 學習條件
  LearningConfig learning;
  // 施展條件
  UsageConfig usage;
  // 消耗與成本
  Costs costs;
  // 技能成長
  ScalingConfig scaling;
  // 效果與數值
  Mechanics mechanics;

  List<SynergiesConfig> synergies;

  List<MoveAction> moves;

  DefaultConfig comboDefaults;

  List<MoveAction> combo;

  DefaultConfig counterDefaults;

  List<MoveAction> counter;

}
