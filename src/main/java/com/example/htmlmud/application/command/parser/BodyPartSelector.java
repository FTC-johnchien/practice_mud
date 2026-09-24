package com.example.htmlmud.application.command.parser;

/**
 * @deprecated 請改用 {@link com.example.htmlmud.domain.service.BodyPartSelector}
 */
@Deprecated
public class BodyPartSelector {

  public static String getRandomBodyPart() {
    return com.example.htmlmud.domain.service.BodyPartSelector.getRandomBodyPart();
  }
}
