import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';

/**
 * 隊伍公共行囊抽屜 (Bag Drawer Component)
 * 負責渲染隊伍公共背包 30 格、物品分類、使用丹藥及裝備穿戴
 */

/**
 * 切換隊伍行囊抽屜
 * @param {boolean} [show] 強制設定開關狀態
 */
export function toggleBagDrawer(show) {
  const drawer = document.getElementById('bag-drawer');
  if (!drawer) return;

  const current = store.get('isBagDrawerOpen');
  const target = show !== undefined ? Boolean(show) : !current;
  store.setState({ isBagDrawerOpen: target });

  if (target) {
    drawer.classList.remove('hidden');
    renderBagDrawer();
  } else {
    drawer.classList.add('hidden');
  }
}

/**
 * 渲染隊伍行囊內容
 */
export function renderBagDrawer() {
  const listEl = document.getElementById('bag-items-list');
  const countEl = document.getElementById('bag-slot-count');
  const lastParty = store.get('lastParty');
  if (!listEl || !lastParty) return;

  const inv = lastParty.inventory;
  const members = lastParty.members || [];
  const slots = (inv && inv.slots) ? inv.slots : [];
  const capacity = inv ? inv.capacity : 30;

  if (countEl) {
    countEl.innerText = `${slots.length}/${capacity}`;
  }

  listEl.innerHTML = '';
  if (slots.length === 0) {
    listEl.innerHTML = '<div class="bag-empty-notice">🎒 行囊空空如也，探索墓塚或斬除妖邪可獲取法寶與靈丹！</div>';
    return;
  }

  slots.forEach((item, slotIdx) => {
    const card = document.createElement('div');
    const qualityCls = `quality-${(item.quality || 'COMMON').toLowerCase()}`;
    card.className = `bag-item-card ${qualityCls}`;

    let effectDesc = '';
    if (item.weapon) {
      effectDesc = `🗡️ 攻擊力 +${item.bonusMinDamage}~${item.bonusMaxDamage}`;
    } else if (item.armor) {
      effectDesc = `🥋 防禦 +${item.bonusDefense}, 氣血 +${item.bonusHp}`;
    } else if (item.effectType === 'HEAL_HP') {
      effectDesc = `🌿 服用回復 ${item.effectValue} HP`;
    } else if (item.effectType === 'RESTORE_SAN') {
      effectDesc = `📜 服用回復 ${item.effectValue} SAN (解走火入魔)`;
    } else if (item.effectType === 'LEARN_SKILL') {
      effectDesc = `🧬 煉化領悟絕學【${item.grantedSkillName || '道種'}】`;
    }

    // 構造快捷操作按鍵 (對 6 位隊員)
    let actionButtons = '';
    if (item.consumable) {
      const btns = members.map((m, mIdx) => {
        const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
        return `<button class="item-act-mini-btn btn-use" ${isAlive ? '' : 'disabled'}
          onclick="window.send('use ${item.slotId} ${mIdx}')" title="為 #${mIdx + 1} ${m.name} 使用">
          #${mIdx + 1} ${m.name.slice(0, 3)}
        </button>`;
      }).join('');
      actionButtons += `
        <div class="item-action-row">
          <span class="action-type-label">使用/服用:</span>
          <div class="action-targets-grid">${btns}</div>
        </div>
      `;
    }

    if (item.weapon || item.armor || item.equipment || item.equipSlot || item.itemType === 'WEAPON' || item.itemType === 'ARMOR' || item.itemType === 'SHIELD' || item.itemType === 'ACCESSORY') {
      const btns = members.map((m, mIdx) => {
        const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
        return `<button class="item-act-mini-btn btn-equip" ${isAlive ? '' : 'disabled'}
          onclick="window.send('equip ${item.slotId} ${mIdx}')" title="為 #${mIdx + 1} ${m.name} 穿戴">
          #${mIdx + 1} ${m.name.slice(0, 3)}
        </button>`;
      }).join('');
      actionButtons += `
        <div class="item-action-row">
          <span class="action-type-label">穿戴裝備:</span>
          <div class="action-targets-grid">${btns}</div>
        </div>
      `;
    }

    card.innerHTML = `
      <div class="bag-item-top">
        <span class="bag-item-icon">${item.icon || '📦'}</span>
        <div class="bag-item-meta">
          <div class="bag-item-name-line">
            <span class="bag-item-title">${item.name}</span>
            <span class="bag-item-qty">x${item.count}</span>
          </div>
          <div class="bag-item-effect">${effectDesc}</div>
        </div>
      </div>
      <div class="bag-item-desc">${item.description || ''}</div>
      ${actionButtons ? `<div class="bag-item-actions">${actionButtons}</div>` : ''}
    `;

    listEl.appendChild(card);
  });
}

// 相容掛載至 window
if (typeof window !== 'undefined') {
  window.toggleBagDrawer = toggleBagDrawer;
  window.renderBagDrawer = renderBagDrawer;
}
