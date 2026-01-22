package com.example.htmlmud.domain.model.map;

import lombok.Builder;

@Builder(toBuilder = true)
public record Equipment(

    String head, // 頭部

    String face, // 臉部

    String neck, // 頸部

    String shoulders, // 肩部

    String arms, // 手臂

    String wrists, // 腕部

    String hands, // 手部

    String body, // 身體

    String chest, // 胸部

    String back, // 背部

    String waist, // 腰部

    String legs, // 腿部

    String feet, // 脚部

    String finger_1, // 戒指1

    String finger_2, // 戒指2

    String mainHand, // 主手

    String offHand // 副手
) {
}
