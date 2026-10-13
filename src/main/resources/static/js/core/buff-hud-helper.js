import { escapeHtml } from './ui-utils.js';

/**
 * 屬性圖示聚合 HUD 輔助模組 (Element-Icon Aggregated HUD Helper)
 * 遵循《太陰地宮》Buff/Debuff 系統規範 (Phase 3.2 & 3.3)
 * 防破版、防 XSS 轉義、同屬性 DoT/Buff 聚合、懸停明細展開。
 */

// 屬性與效果對應圖示映射表
const ELEMENT_ICONS = {
  FIRE: { icon: '🔥', label: '烈火', color: '#f87171', border: '#ef4444' },
  WATER: { icon: '❄️', label: '寒冰', color: '#67e8f9', border: '#06b6d4' },
  ICE: { icon: '❄️', label: '寒冰', color: '#67e8f9', border: '#06b6d4' },
  WOOD: { icon: '🪵', label: '青木', color: '#4ade80', border: '#22c55e' },
  EARTH: { icon: '🪨', label: '崇巖', color: '#fbbf24', border: '#d97706' },
  LIGHTNING: { icon: '⚡', label: '驚雷', color: '#fde047', border: '#eab308' },
  THUNDER: { icon: '⚡', label: '驚雷', color: '#fde047', border: '#eab308' },
  WIND: { icon: '💨', label: '疾風', color: '#38bdf8', border: '#0284c7' },
  POISON: { icon: '🧪', label: '蝕毒', color: '#a3e635', border: '#84cc16' },
  BLEED: { icon: '🩸', label: '流血', color: '#f87171', border: '#dc2626' },
  DARK: { icon: '👻', label: '陰煞', color: '#c084fc', border: '#9333ea' },
  YIN: { icon: '👻', label: '陰煞', color: '#c084fc', border: '#9333ea' },
  HOLY: { icon: '☀️', label: '純陽', color: '#fde047', border: '#f59e0b' },
  YANG: { icon: '☀️', label: '純陽', color: '#fde047', border: '#f59e0b' },
  PHYSICAL: { icon: '⚔️', label: '外功', color: '#cbd5e1', border: '#64748b' },
  SLASH: { icon: '⚔️', label: '外功', color: '#cbd5e1', border: '#64748b' }
};

const STAT_ICONS = {
  ATK: { icon: '🗡️', label: '攻擊', color: '#fca5a5', border: '#ef4444' },
  DEF: { icon: '🛡️', label: '防禦', color: '#93c5fd', border: '#3b82f6' },
  SPEED: { icon: '🏃', label: '身法', color: '#86efac', border: '#22c55e' },
  MAX_HP: { icon: '❤️', label: '生命', color: '#fca5a5', border: '#ef4444' }
};

const CC_ICONS = {
  STUN: { icon: '💫', label: '眩暈', color: '#fde047', border: '#eab308' },
  SILENCE: { icon: '🔇', label: '沉默', color: '#d8b4fe', border: '#a855f7' },
  ROOT: { icon: '⛓️', label: '定身', color: '#cbd5e1', border: '#94a3b8' },
  SLEEP: { icon: '💤', label: '昏睡', color: '#c7d2fe', border: '#6366f1' }
};

/**
 * 聚合處理 Buff 清單
 * @param {Array} activeBuffs 原始 Buff 陣列
 * @returns {Array} 聚合後的項目清單
 */
export function aggregateBuffs(activeBuffs) {
  if (!activeBuffs || !Array.isArray(activeBuffs) || activeBuffs.length === 0) {
    return [];
  }

  const dotGroups = new Map();
  const statGroups = new Map();
  const ccGroups = new Map();
  const hotItems = [];
  const shieldItems = [];
  const otherItems = [];

  activeBuffs.forEach(b => {
    const cat = String(b.category || '').toUpperCase();
    const eff = String(b.effectType || '').toUpperCase();
    const elem = String(b.element || '').toUpperCase();

    if (cat === 'DOT') {
      const key = elem || eff || 'POISON';
      if (!dotGroups.has(key)) {
        dotGroups.set(key, { key, element: elem, effectType: eff, totalDps: 0, totalStacks: 0, items: [] });
      }
      const group = dotGroups.get(key);
      const stacks = Math.max(1, Number(b.stacks) || 1);
      const dps = (Number(b.value) || 0) * stacks;
      group.totalDps += dps;
      group.totalStacks += stacks;
      group.items.push(b);
    } else if (cat === 'SHIELD' || eff === 'SHIELD') {
      shieldItems.push(b);
    } else if (cat === 'STAT_MOD') {
      const statKey = eff || 'ATK';
      if (!statGroups.has(statKey)) {
        statGroups.set(statKey, { statKey, totalVal: 0, isPercent: true, items: [] });
      }
      const group = statGroups.get(statKey);
      group.totalVal += Number(b.value) || 0;
      group.items.push(b);
    } else if (cat === 'CC') {
      const ccKey = eff || 'STUN';
      if (!ccGroups.has(ccKey)) {
        ccGroups.set(ccKey, { ccKey, items: [] });
      }
      ccGroups.get(ccKey).items.push(b);
    } else if (cat === 'HOT') {
      hotItems.push(b);
    } else {
      otherItems.push(b);
    }
  });

  const results = [];

  // 1. DoT 聚合
  dotGroups.forEach((group, key) => {
    const meta = ELEMENT_ICONS[key] || ELEMENT_ICONS[group.element] || { icon: '🧪', label: key, color: '#a3e635', border: '#84cc16' };
    const stackText = group.totalStacks > 1 ? ` x${group.totalStacks}` : '';
    const label = `${meta.icon} ${group.totalDps}/s${stackText}`;

    const tooltip = group.items.map(b => {
      const rem = b.isPermanent || b.remainingSeconds === -1 ? '♾️常駐' : `${b.remainingSeconds}s`;
      return `• 【${b.name || 'DoT'}】: ${b.value}/跳 (${rem}${b.stacks > 1 ? ', x' + b.stacks : ''})`;
    }).join('\n');

    results.push({
      type: 'DOT',
      htmlText: label,
      tooltip: `【${meta.label}持續傷害】\n${tooltip}`,
      color: meta.color,
      border: meta.border,
      bg: 'rgba(24, 24, 27, 0.85)'
    });
  });

  // 2. 護盾聚合
  if (shieldItems.length > 0) {
    const totalShield = shieldItems.reduce((acc, cur) => acc + (Number(cur.value) || 0), 0);
    const tooltip = shieldItems.map(b => {
      const rem = b.isPermanent || b.remainingSeconds === -1 ? '♾️常駐' : `${b.remainingSeconds}s`;
      return `• 【${b.name || '護盾'}】: 容量 ${b.value} (${rem})`;
    }).join('\n');

    results.push({
      type: 'SHIELD',
      htmlText: `🛡️ ${totalShield}`,
      tooltip: `【玄罡護盾】\n${tooltip}`,
      color: '#93c5fd',
      border: '#3b82f6',
      bg: 'rgba(23, 37, 84, 0.85)'
    });
  }

  // 3. 屬性修正聚合
  statGroups.forEach((group, key) => {
    const meta = STAT_ICONS[key] || { icon: '✨', label: key, color: '#fde047', border: '#eab308' };
    const sign = group.totalVal >= 0 ? '+' : '';
    const label = `${meta.icon} ${sign}${group.totalVal}%`;

    const tooltip = group.items.map(b => {
      const rem = b.isPermanent || b.remainingSeconds === -1 ? '♾️常駐' : `${b.remainingSeconds}s`;
      const valSign = b.value >= 0 ? '+' : '';
      return `• 【${b.name || '增益'}】: ${meta.label} ${valSign}${b.value}% (${rem})`;
    }).join('\n');

    results.push({
      type: 'STAT',
      htmlText: label,
      tooltip: `【${meta.label}修持加成】\n${tooltip}`,
      color: meta.color,
      border: meta.border,
      bg: 'rgba(15, 23, 42, 0.85)'
    });
  });

  // 4. 控場聚合
  ccGroups.forEach((group, key) => {
    const meta = CC_ICONS[key] || { icon: '💫', label: key, color: '#fde047', border: '#eab308' };
    const label = `${meta.icon} ${meta.label}`;

    const tooltip = group.items.map(b => {
      const rem = b.isPermanent || b.remainingSeconds === -1 ? '♾️常駐' : `${b.remainingSeconds}s`;
      return `• 【${b.name || '控場'}】: 剩餘 ${rem}`;
    }).join('\n');

    results.push({
      type: 'CC',
      htmlText: label,
      tooltip: `【控場狀態】\n${tooltip}`,
      color: meta.color,
      border: meta.border,
      bg: 'rgba(66, 32, 6, 0.85)'
    });
  });

  // 5. HoT 聚合
  if (hotItems.length > 0) {
    const totalHot = hotItems.reduce((acc, cur) => acc + ((Number(cur.value) || 0) * Math.max(1, Number(cur.stacks) || 1)), 0);
    const tooltip = hotItems.map(b => {
      const rem = b.isPermanent || b.remainingSeconds === -1 ? '♾️常駐' : `${b.remainingSeconds}s`;
      return `• 【${b.name || '滋養'}】: +${b.value}/秒 (${rem})`;
    }).join('\n');

    results.push({
      type: 'HOT',
      htmlText: `✨ +${totalHot}/s`,
      tooltip: `【生機滋養】\n${tooltip}`,
      color: '#86efac',
      border: '#22c55e',
      bg: 'rgba(6, 78, 59, 0.85)'
    });
  }

  // 6. 其他單獨狀態
  otherItems.forEach(b => {
    const icon = b.icon || '✨';
    const rem = b.isPermanent || b.remainingSeconds === -1 ? '♾️' : `${b.remainingSeconds}s`;
    const label = `${icon} ${b.name ? b.name.slice(0, 4) : ''}`;
    results.push({
      type: 'OTHER',
      htmlText: label,
      tooltip: `• 【${b.name || '加持'}】 (${b.category || ''}): 剩餘 ${rem}`,
      color: '#cbd5e1',
      border: '#64748b',
      bg: 'rgba(30, 41, 59, 0.85)'
    });
  });

  return results;
}

/**
 * 渲染聚合 Buff 徽章 HTML
 * @param {Array} activeBuffs 原始 Buff 陣列
 * @returns {string} 安全 HTML
 */
export function renderAggregatedBuffBadges(activeBuffs) {
  const aggregated = aggregateBuffs(activeBuffs);
  if (aggregated.length === 0) return '';

  return `
    <div class="buff-badges-row aggregated-buff-row" style="display:flex; flex-wrap:wrap; gap:3px; align-items:center;">
      ${aggregated.map(item => {
        const safeTooltip = escapeHtml(item.tooltip);
        const safeLabel = escapeHtml(item.htmlText);
        return `
          <span class="buff-aggregated-chip"
                style="color:${item.color}; border:1px solid ${item.border}; background:${item.bg};"
                title="${safeTooltip}">
            ${safeLabel}
          </span>
        `;
      }).join('')}
    </div>
  `;
}

/**
 * 渲染護盾半透明疊層 (Shield Overlay)
 * @param {number} hp 當前 HP
 * @param {number} maxHp 最大 HP
 * @param {number} shield 當前護盾值
 * @returns {string} 護盾疊層 HTML
 */
export function renderShieldOverlayHtml(hp, maxHp, shield) {
  const safeShield = Number(shield) || 0;
  if (safeShield <= 0 || !maxHp) return '';
  const shieldPct = Math.min(100, Math.max(0, (safeShield / maxHp) * 100));
  return `
    <div class="enemy-shield-overlay" style="width:${shieldPct}%;" title="玄罡護盾: ${safeShield}"></div>
  `;
}

/**
 * 渲染 Poise 架勢條 (Poise Bar)
 * @param {number} poise 當前 Poise 值
 * @param {number} maxPoise 最大 Poise 值
 * @param {boolean} isBroken 是否已破防
 * @returns {string} 架勢條 HTML
 */
export function renderPoiseBarHtml(poise, maxPoise, isBroken) {
  const cur = poise !== undefined ? Number(poise) : 100;
  const max = maxPoise ? Number(maxPoise) : 100;
  const pct = Math.min(100, Math.max(0, (cur / max) * 100));

  if (isBroken) {
    return `
      <div class="poise-bar-wrap poise-broken" style="height:4px; margin-top:2px;" title="【力竭破防】架勢已崩解！受傷加深 +35%，霸體失效！">
        <div class="poise-bar-fill" style="width:100%; background:#ef4444;"></div>
      </div>
    `;
  }

  return `
    <div class="poise-bar-wrap" style="height:4px; margin-top:2px;" title="架勢值 (Poise): ${cur}/${max}">
      <div class="poise-bar-fill" style="width:${pct}%; background:linear-gradient(90deg, #f59e0b, #fbbf24);"></div>
    </div>
  `;
}
