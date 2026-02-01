package com.example.htmlmud.application.perform;

import com.example.htmlmud.domain.actor.impl.Living;

public interface Perform {

  String getId(); // "taichi_hit"

  void execute(Living self, Living target);

}
