package com.example.htmlmud.domain.model.entity;

import java.util.List;

public record RoomStateRecord(

    String roomId,

    String zoneId,

    List<GameItem> droppedItems

) {

}
