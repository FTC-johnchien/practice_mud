package com.example.htmlmud.config;

import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.impl.Player;

@Component
public class GuiBridge {
  private Player currentPlayer;

  public void setPlayer(Player player) {
    this.currentPlayer = player;
  }

  public Player getPlayer() {
    return currentPlayer;
  }
}
