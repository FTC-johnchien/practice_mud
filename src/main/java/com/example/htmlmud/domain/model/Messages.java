package com.example.htmlmud.domain.model;

import java.util.List;

public record Messages(

    List<String> prepare,

    List<String> cast,

    List<String> hit,

    List<String> crit,

    List<String> dodge,

    List<String> parried,

    List<String> miss,

    List<String> counter,

    List<String> tick,

    List<String> finish,

    List<String> interrupt,

    List<String> fail,

    List<String> meditate,

    List<String> combat_tick

) {

}
