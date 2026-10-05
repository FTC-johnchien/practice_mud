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
      text = '護盾吸收';
      break;
    default:
      if (ev.type === 'HEAL' || ev.type === 'HOT_TICK') {
        cls.push('ft-heal');
        text = `+${hit.amount}`;
      } else {
        const dmgType = (ev.damageType || 'PHYSICAL').toUpperCase();
        cls.push(DMG_COLORS[dmgType] || 'dmg-phys');

        if (outcome === 'CRIT') {
          cls.push('ft-crit');
          text = `暴擊！-${hit.amount}`;
        } else {
          text = `-${hit.amount}`;
        }

        if (ev.type === 'DOT_TICK') {
          cls.push('ft-small');
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
