package com.example.htmlmud.domain.model;

public enum RoomFlag {
  // Environment
  INDOORS, OUTDOORS, DARK, UNDERWATER,
  // Combat
  SAFE_ZONE, NO_PVP, ARENA,
  // Movement
  NO_RECALL, NO_TELEPORT, PRIVATE,
  // Special
  NO_MAGIC, BANK, SHOP
}
