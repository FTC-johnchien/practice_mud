package com.example.htmlmud.protocol;

public record ActorMessage(String traceId, GameCommand command) {

}
