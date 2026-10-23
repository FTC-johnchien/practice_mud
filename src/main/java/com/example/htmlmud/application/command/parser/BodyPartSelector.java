package com.example.htmlmud.application.command.parser;

/**
 * @deprecated 請改用 {@link com.example.htmlmud.combat.BodyPartSelector}
 */
@Deprecated
public class BodyPartSelector {

  public static String getRandomBodyPart() {
    return com.example.htmlmud.combat.BodyPartSelector.getRandomBodyPart();
  }
}
