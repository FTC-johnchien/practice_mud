/**
 * 戰鬥特效預設查表與解析器 (FX Presets)
 * 依據事件類型、武器類型、傷害屬性、攻擊形狀映射視覺特效 CSS class
 */

const WEAPON_IMPACTS = {
  SWORD: 'fx-slash',
  BLADE: 'fx-heavy-slash',
  AXE: 'fx-heavy-slash',
  POLEAXE: 'fx-heavy-slash',
  KATANA: 'fx-slash',
  SABER: 'fx-heavy-slash',
  SCIMITAR: 'fx-heavy-slash',
  SPEAR: 'fx-thrust',
  POLEARM: 'fx-thrust',
  HALBERD: 'fx-thrust',
  JAVELIN: 'fx-thrust',
  BLUNT: 'fx-blunt',
  HAMMER: 'fx-blunt',
  MACE: 'fx-blunt',
  MAUL: 'fx-blunt',
  CLUB: 'fx-blunt',
  FLAIL: 'fx-blunt',
  DAGGER: 'fx-dagger',
  DIRK: 'fx-dagger',
  KNIFE: 'fx-dagger',
  STILETTO: 'fx-dagger',
  UNARMED: 'fx-fist',
  STAFF: 'fx-holy',
  WAND: 'fx-holy'
};

const ELEMENT_IMPACTS = {
  FIRE: 'fx-fire',
  ICE: 'fx-ice',
  LIGHTNING: 'fx-lightning',
  POISON: 'fx-poison',
  HOLY: 'fx-holy',
  DARK: 'fx-dark',
  SLASH: 'fx-slash',
  PIERCE: 'fx-thrust',
  BLUNT: 'fx-blunt'
};

export function resolvePreset(ev) {
  if (!ev) {
    return { impactFx: 'fx-slash', areaFx: null, isAoe: false };
  }

  const dmgType = (ev.damageType || '').toUpperCase();
  const weaponType = (ev.weaponType || '').toUpperCase();
  const isAoe = ev.shape === 'ALL' || ev.shape === 'ROW' || (ev.hits && ev.hits.length > 1);

  // 1. 特殊絕招 / 奧義優先解析 (Ultimate & Taunt)
  if (ev.fxKey === 'ult_holy') {
    return {
      impactFx: 'fx-holy',
      areaFx: 'fx-area-ultimate-holy',
      isAoe: true,
      showBanner: true,
      skillName: ev.skillName || '陣法奧義・四象辟邪聖光'
    };
  }
  if (ev.fxKey === 'ult_abyss') {
    return {
      impactFx: 'fx-dark',
      areaFx: 'fx-area-ultimate-abyss',
      isAoe: true,
      showBanner: true,
      skillName: ev.skillName || '陣法奧義・不可名狀星蝕'
    };
  }
  if (ev.fxKey === 'taunt_roar' || dmgType === 'TAUNT') {
    return {
      impactFx: null, // 嘲諷非傷害，不播武器受擊切口
      areaFx: 'fx-area-taunt-roar',
      isAoe: true,
      showBanner: true,
      skillName: ev.skillName || '金剛怒目'
    };
  }

  // 2. 單體命中特效 (impactFx)
  let impactFx = 'fx-slash';
  if (ev.type === 'HEAL') {
    impactFx = 'fx-holy';
  } else if (ELEMENT_IMPACTS[dmgType]) {
    impactFx = ELEMENT_IMPACTS[dmgType];
  } else if (WEAPON_IMPACTS[weaponType]) {
    impactFx = WEAPON_IMPACTS[weaponType];
  }

  // 3. 全體或範圍特效 (areaFx)
  let areaFx = null;
  if (isAoe) {
    if (ev.type === 'HEAL') {
      areaFx = 'fx-area-heal';
    } else if (dmgType === 'LIGHTNING') {
      areaFx = 'fx-area-lightning';
    } else if (dmgType === 'FIRE') {
      areaFx = 'fx-area-fire';
    } else {
      areaFx = 'fx-area-sweep';
    }
  }

  // 4. 技能宣告橫幅 (skillBanner)
  const showBanner = Boolean(
    ev.skillName &&
    (ev.type === 'SKILL' || isAoe || (ev.fxKey && ev.fxKey.startsWith('combo_')) || (ev.fxKey && ev.fxKey.startsWith('ult_')))
  );

  return {
    impactFx,
    areaFx,
    isAoe,
    showBanner,
    skillName: ev.skillName
  };
}
