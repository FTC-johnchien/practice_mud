package com.example.htmlmud.infra.util.formula;

/**
 * 這就是那個 Wrapper 用途：讓 SpEL 可以用 object.level 這種語法來呼叫 getLevel()
 */
public class SkillContext {
  private final int level;

  public SkillContext(int level) {
    this.level = level;
  }

  // SpEL 看到 .level 會自動呼叫這個 getter
  public int getLevel() {
    return level;
  }
}
