package com.example.htmlmud;

import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.WeaponType;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class WeaponSkillBindingTest {

    private PartyMember member;

    @BeforeEach
    void setUp() {
        member = new PartyMember();
        member.setId("test_warrior");
        member.setName("測試戰士");
    }

    @Test
    void testUnarmedDefaultsToBasicFist() {
        assertEquals(WeaponType.UNARMED, member.getMainHandWeaponType());
        assertEquals("basic_fist", member.getBasicSkillId());

        var skill = member.getEnabledBasicSkill();
        assertNotNull(skill);
        assertEquals("basic_fist", skill.getId());

        var move = member.getRandomBasicMove();
        assertNotNull(move);
        assertNotNull(move.name());
    }

    @Test
    void testEquipSwordBindsBasicSword() {
        PartyItemSlot sword = PartyItemSlot.builder()
                .itemId("iron_sword")
                .name("鐵劍")
                .equipSlot(EquipmentSlot.MAIN_HAND)
                .subType("SWORD")
                .bonusMaxDamage(5)
                .build();
        member.setEquipment(new java.util.EnumMap<>(Map.of(EquipmentSlot.MAIN_HAND, sword)));

        assertEquals(WeaponType.SWORD, member.getMainHandWeaponType());
        assertEquals("basic_sword", member.getBasicSkillId());

        var skill = member.getEnabledBasicSkill();
        assertNotNull(skill);
        assertEquals("basic_sword", skill.getId());
    }

    @Test
    void testEquipBluntBindsBasicBlunt() {
        PartyItemSlot mace = PartyItemSlot.builder()
                .itemId("oak_mace")
                .name("橡木重錘")
                .equipSlot(EquipmentSlot.MAIN_HAND)
                .subType("BLUNT")
                .bonusMaxDamage(6)
                .build();
        member.setEquipment(new java.util.EnumMap<>(Map.of(EquipmentSlot.MAIN_HAND, mace)));

        assertEquals(WeaponType.BLUNT, member.getMainHandWeaponType());
        assertEquals("basic_blunt", member.getBasicSkillId());

        var skill = member.getEnabledBasicSkill();
        assertNotNull(skill);
        assertEquals("basic_blunt", skill.getId());
    }

    @Test
    void testActiveSkillWeaponRestriction() {
        PartyMemberSkill swordSkill = PartyMemberSkill.builder()
                .id("sword_pierce")
                .name("破空刺")
                .allowedWeapons(List.of("SWORD", "BLADE"))
                .build();

        // 徒手時不能使用破空刺
        assertFalse(member.isSkillUsable(swordSkill));

        // 裝備鐵劍後可以使用
        PartyItemSlot sword = PartyItemSlot.builder()
                .itemId("iron_sword")
                .name("鐵劍")
                .equipSlot(EquipmentSlot.MAIN_HAND)
                .subType("SWORD")
                .build();
        member.getEquipment().put(EquipmentSlot.MAIN_HAND, sword);
        assertTrue(member.isSkillUsable(swordSkill));

        // 換成斧頭/錘子不能使用
        PartyItemSlot mace = PartyItemSlot.builder()
                .itemId("oak_mace")
                .name("橡木重錘")
                .equipSlot(EquipmentSlot.MAIN_HAND)
                .subType("BLUNT")
                .build();
        member.getEquipment().put(EquipmentSlot.MAIN_HAND, mace);
        assertFalse(member.isSkillUsable(swordSkill));
    }
}