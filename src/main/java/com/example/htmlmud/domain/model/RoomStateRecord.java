package com.example.htmlmud.domain.model;

import java.util.List;

public record RoomStateRecord(long id, List<GameItem> droppedItems) {

}
