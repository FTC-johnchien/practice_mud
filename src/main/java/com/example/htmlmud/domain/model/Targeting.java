package com.example.htmlmud.domain.model;

public record Targeting(

    String type,

    int range,

    boolean mustHaveTarget

) {
}
