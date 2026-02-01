package com.example.htmlmud.domain.model;

public record Mechanics(

    ResourceType scaleStat, // 吃「屬性 STR, INT, DEX, CON」加成的

    double scaleFactor // scaleStat的倍數 1.0 (有多少力打多少痛)

) {

}
