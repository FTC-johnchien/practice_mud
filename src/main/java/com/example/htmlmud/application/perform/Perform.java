package com.example.htmlmud.application.perform;

import com.example.htmlmud.domain.actor.impl.LivingActor;

public interface Perform {

  String getId(); // "taichi_hit"

  void execute(LivingActor self, LivingActor target);

}
