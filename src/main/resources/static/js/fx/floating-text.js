/**
 * 戰鬥浮動數字生成器 (Floating Combat Text)
 * 遵循正交色彩與結果標籤規範，以目標 HP 變化為核心
 */

const DMG_COLORS = {
  PHYSICAL: 'dmg-phys',
  SLASH: 'dmg-phys',
  PIERCE: 'dmg-phys',
  BLUNT: 'dmg-phys',
  FIRE: 'dmg-fire',
  ICE: 'dmg-ice',
  LIGHTNING: 'dmg-lightning',
  POISON: 'dmg-poison',
  HOLY: 'dmg-holy',
  DARK: 'dmg-dark',
  MAGIC: 'dmg-magic',
  SONIC: 'dmg-sonic',
  TRUE: 'dmg-true'
};

const MAX_ALIVE_TEXTS = 24;

/**
 * 在特效層生成浮動文字
 * @param {HTMLElement} layer 特效層容器 (#battle-fx-layer)
 * @param {{x: number, y: number, w: number, h: number}} rect 目標幾何位置
 * @param {Object} hit 單一命中結算 { target, outcome, amount, absorbed, killed }
 * @param {Object} ev 戰鬥事件 { type, damageType, ... }
 */
export function spawnFloatingText(layer, rect, hit, ev) {
  if (!layer || !rect) return;

  // 限制同時存在的節點上限，維護效能防卡頓
  const aliveNodes = layer.querySelectorAll('.ftext');
  if (aliveNodes.length >= MAX_ALIVE_TEXTS) {
    aliveNodes[0]?.remove();
  }

  const el = document.createElement('div');
  const cls = ['ftext'];
  let text = '';

  const outcome = hit.outcome || 'HIT';
  if (ev.skillId === 'phase_transition') {
    cls.push('ft-interrupt');
    text = ev.fxKey === 'boss_enraged' ? '⚡⚡ 超載狂暴！' : '⚡ 氣浪震退！';
  } else if (ev.damageType === 'TAUNT' || ev.fxKey === 'taunt_roar' || outcome === 'TAUNT') {
    cls.push('ft-taunt');
    text = '💢 仇恨鎖定';
  } else {
    switch (outcome) {
      case 'MISS':
        cls.push('ft-miss');
        text = '未命中';
        break;
      case 'DODGED':
        cls.push('ft-dodge');
        text = '身法閃避';
        break;
      case 'BLOCKED':
        cls.push('ft-block');
        text = hit.amount > 0 ? `🛡️ 格擋 -${hit.amount}` : '🛡️ 完全格擋';
        break;
      case 'PARRIED':
        cls.push('ft-parry');
        text = hit.amount > 0 ? `⚔️ 招架 -${hit.amount}` : '⚔️ 招架';
        break;
      case 'ABSORBED':
        cls.push('ft-block');
        text = '🛡️ 護盾吸收';
        break;
      case 'INTERRUPT':
      case 'INTERRUPTED':
        cls.push('ft-interrupt');
        text = hit.amount > 0 ? `⚡ 打斷！ -${hit.amount}` : '⚡ 打斷！';
        break;
      default:
        if (ev.type === 'SHIELD') {
          cls.push('ft-heal');
          text = `🛡️ +${hit.amount || '玄罡護盾'}`;
        } else if (ev.type === 'HEAL' || ev.type === 'HOT_TICK') {
          cls.push('ft-heal');
          text = `+${hit.amount}`;
        } else {
          const dmgType = (ev.damageType || 'PHYSICAL').toUpperCase();
          cls.push(DMG_COLORS[dmgType] || 'dmg-phys');

          if (hit.reaction || ev.reaction) {
            const rx = (hit.reaction || ev.reaction).toUpperCase();
            if (rx.includes('VAPORIZE') || rx.includes('氣化')) {
              cls.push('ft-crit', 'ft-vaporize');
              text = `💥 氣化爆裂! -${hit.amount}`;
            } else if (rx.includes('ELECTRO') || rx.includes('感電')) {
              cls.push('ft-crit', 'ft-electro');
              text = `⚡ 感電傳導! -${hit.amount}`;
            } else if (rx.includes('CHAOS') || rx.includes('混沌')) {
              cls.push('ft-crit', 'ft-chaos');
              text = `☯️ 混沌湮滅! -${hit.amount}`;
            } else if (rx.includes('COMBUSTION') || rx.includes('燎原')) {
              cls.push('ft-crit', 'ft-combustion');
              text = `🔥 三昧燎原! -${hit.amount}`;
            } else if (rx.includes('DETONAT') || rx.includes('引爆')) {
              cls.push('ft-crit', 'ft-detonation');
              text = `🩸 裂魂引爆! -${hit.amount}`;
            }
          } else if (hit.absorbed > 0 && hit.amount > 0) {
            text = `🛡️ 破盾 -${hit.amount}`;
          } else if (outcome === 'CRIT') {
            cls.push('ft-crit', 'ft-stagger-crit');
            text = `💥 破防重創! -${hit.amount}`;
          } else {
            text = `-${hit.amount}`;
          }

          if (ev.type === 'DOT_TICK') {
            cls.push('ft-small');
            const elemIcon = dmgType === 'POISON' ? '🧪 ' : (dmgType === 'FIRE' ? '🔥 ' : '');
            text = `${elemIcon}-${hit.amount}`;
          }
        }
    }
  }

  // 我方隊友受創時，外加紅色警戒描邊
  if (hit.target && hit.target.side === 'PARTY' && outcome !== 'MISS' && outcome !== 'DODGED') {
    cls.push('ft-on-ally');
  }

  el.className = cls.join(' ');
  el.textContent = text;

  // 水平微隨機偏移 (±14px)，防止連擊數字完全重疊
  const offsetX = (Math.random() * 28 - 14);
  const posX = rect.x + (rect.w / 2) + offsetX;
  const posY = rect.y + (rect.h * 0.25);

  el.style.left = `${posX}px`;
  el.style.top = `${posY}px`;

  el.addEventListener('animationend', () => el.remove(), { once: true });
  layer.appendChild(el);
}

/**
 * 專屬打斷反制 FCT 浮動大字生成
 * @param {HTMLElement} layer 特效層容器 (#battle-fx-layer)
 * @param {{x: number, y: number, w: number, h: number}} rect 目標敵方卡片幾何位置
 */
export function spawnInterruptFloatingText(layer, rect) {
  if (!layer || !rect) return;
  const el = document.createElement('div');
  el.className = 'ftext ft-interrupt';
  el.textContent = '⚡ 打斷！';

  const posX = rect.x + (rect.w / 2);
  const posY = rect.y + (rect.h * 0.2);

  el.style.left = `${posX}px`;
  el.style.top = `${posY}px`;

  el.addEventListener('animationend', () => el.remove(), { once: true });
  layer.appendChild(el);
}

/**
 * 專屬元素複合反應與裂魂印爆 FCT 浮動大字生成 (Milestone F2-05)
 */
export function spawnElementalReactionFloatingText(layer, rect, reactionType, amount) {
  if (!layer || !rect) return;
  const el = document.createElement('div');
  const cls = ['ftext', 'ft-crit'];
  let text = '';

  const rx = (reactionType || '').toUpperCase();
  if (rx.includes('VAPORIZE') || rx.includes('氣化')) {
    cls.push('ft-vaporize');
    text = amount > 0 ? `💥 氣化爆裂! -${amount}` : '💥 氣化爆裂!';
  } else if (rx.includes('ELECTRO') || rx.includes('感電')) {
    cls.push('ft-electro');
    text = amount > 0 ? `⚡ 感電傳導! -${amount}` : '⚡ 感電傳導!';
  } else if (rx.includes('CHAOS') || rx.includes('混沌')) {
    cls.push('ft-chaos');
    text = amount > 0 ? `☯️ 混沌湮滅! -${amount}` : '☯️ 混沌湮滅!';
  } else if (rx.includes('COMBUSTION') || rx.includes('燎原')) {
    cls.push('ft-combustion');
    text = amount > 0 ? `🔥 三昧燎原! -${amount}` : '🔥 三昧燎原!';
  } else if (rx.includes('DETONAT') || rx.includes('引爆')) {
    cls.push('ft-detonation');
    text = amount > 0 ? `🩸 裂魂引爆! -${amount}` : '🩸 裂魂引爆!';
  } else {
    cls.push('ft-crit');
    text = amount > 0 ? `💥 靈氣質變! -${amount}` : '💥 靈氣質變!';
  }

  el.className = cls.join(' ');
  el.textContent = text;

  const posX = rect.x + (rect.w / 2);
  const posY = rect.y + (rect.h * 0.15);

  el.style.left = `${posX}px`;
  el.style.top = `${posY}px`;

  el.addEventListener('animationend', () => el.remove(), { once: true });
  layer.appendChild(el);
}


