package com.example.htmlmud.domain.model;

import java.util.List;

public record RoomStateRecord(String roomId, String zoneId, List<GameItem> droppedItems) {

}
