import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';
import { escapeHtml } from '../core/ui-utils.js';

/**
 * 裝備與道具子模組 (Party Equipment & Items Tab)
 * 負責渲染成員 5+2 裝備欄位、屬性詳細道基、行囊道具清單、裝備 Diff 挑選與全隊裝備總覽
 */

const drpgState = store.getState();
const send = (cmd, silent) => sendCmd(cmd, silent);

function getRenderPartyModal() {
  return window.renderPartyModal;
}

function getRenderPaginationBar() {
  return window.renderPaginationBar || ((cur, tot, cnt, fn) => '');
}

/**
 * 裝備部位定義 (7 個部位)
 */
export const EQUIP_SLOT_DEFS = [
  { key: 'MAIN_HAND', alias: 'weapon', label: '主手武器', icon: '🗡️' },
  { key: 'OFF_HAND', alias: 'shield', label: '副手防具', icon: '🛡️' },
  { key: 'HEAD', alias: 'head', label: '頭部盔甲', icon: '👑' },
  { key: 'BODY', alias: 'armor', label: '身軀道袍', icon: '🥋' },
  { key: 'FEET', alias: 'feet', label: '靴履護具', icon: '👢' },
  { key: 'ACCESSORY_1', alias: 'acc1', label: '本命法寶', icon: '💍' },
  { key: 'ACCESSORY_2', alias: 'acc2', label: '輔佐靈寶', icon: '📿' }
];

export function getMemberSlotItem(m, slotKey) {
  if (m.equipment && m.equipment[slotKey]) return m.equipment[slotKey];
  if (slotKey === 'MAIN_HAND' && m.equippedWeapon) return m.equippedWeapon;
  if (slotKey === 'BODY' && m.equippedArmor) return m.equippedArmor;
  return null;
}

export function isItemMatchingSlot(item, slotKey) {
  if (!item) return false;
  const eqSlot = (item.equipSlot || '').toUpperCase();
  const itType = (item.itemType || '').toUpperCase();
  const subType = (item.subType || '').toUpperCase();
  const name = item.name || '';

  switch (slotKey) {
    case 'MAIN_HAND':
      return item.weapon || itType === 'WEAPON' || eqSlot === 'MAIN_HAND' || subType === 'WEAPON';
    case 'OFF_HAND':
      return item.shield || itType === 'SHIELD' || eqSlot === 'OFF_HAND' || subType === 'SHIELD';
    case 'HEAD':
      return eqSlot === 'HEAD' || itType === 'HEAD' || subType === 'HEAD' || name.includes('冠') || name.includes('盔') || name.includes('帽');
    case 'BODY':
      return item.armor || itType === 'ARMOR' || eqSlot === 'BODY' || subType === 'ARMOR' || name.includes('甲') || name.includes('袍') || name.includes('衣');
    case 'FEET':
      return eqSlot === 'FEET' || itType === 'FEET' || subType === 'FEET' || name.includes('靴') || name.includes('履');
    case 'ACCESSORY_1':
    case 'ACCESSORY_2':
      return eqSlot === 'ACCESSORY_1' || eqSlot === 'ACCESSORY_2' || itType === 'ACCESSORY' || subType === 'ACCESSORY' || name.includes('戒') || name.includes('佩') || name.includes('鐲') || name.includes('珠');
    default:
      return false;
  }
}

export function getPassiveSkillDisplayName(m, category) {
  if (!m || !m.passiveSlots || !m.passiveSlots[category]) return null;
  const skillId = m.passiveSlots[category];
  if (!skillId || skillId.startsWith('basic_')) return null;
  if (m.availablePassives) {
    const found = m.availablePassives.find(p => p.skillId === skillId);
    if (found && found.skillName) return found.skillName;
  }
  return skillId;
}

export function setItemsSecondaryFilter(filter) {
  drpgState.itemsSecondaryFilter = filter;
  drpgState.itemsPage = 1;
  const render = getRenderPartyModal();
  if (render) render();
}

export function changeItemsPage(delta) {
  drpgState.itemsPage = Math.max(1, (drpgState.itemsPage || 1) + delta);
  const render = getRenderPartyModal();
  if (render) render();
}

/**
 * 渲染道具畫面 (Items View - 單頁上限 20 筆)
 */
export function renderItemsListHtml(itemTab, secFilter, party) {
  const inv = (party && party.inventory) ? party.inventory : null;
  const allSlots = (inv && inv.slots) ? inv.slots : [];
  const currentSec = secFilter || 'ALL';

  // 1. 依據 itemTab 與 secFilter 進行多維度篩選
  const filtered = allSlots.filter(item => {
    if (!item) return false;
    if (itemTab === 'USABLE') {
      const isUsable = Boolean(item.consumable || item.effectType);
      if (!isUsable) return false;
      if (currentSec === 'HEAL') {
        return item.effectType === 'HEAL_HP' || item.effectType === 'RESTORE_SAN';
      }
      if (currentSec === 'BUFF') {
        return item.effectType === 'BUFF' || item.effectType === 'LEARN_SKILL' || (item.effectType !== 'HEAL_HP' && item.effectType !== 'RESTORE_SAN');
      }
      return true;
    } else if (itemTab === 'QUEST') {
      return item.itemType === 'QUEST' || item.subType === 'QUEST' || item.quality === 'QUEST';
    } else {
      // ALL
      if (currentSec === 'EQUIP') return Boolean(item.weapon || item.armor || item.equipment || item.itemType === 'WEAPON' || item.itemType === 'ARMOR' || item.itemType === 'SHIELD' || item.itemType === 'ACCESSORY');
      if (currentSec === 'CONSUMABLE') return Boolean(item.consumable);
      if (currentSec === 'QUEST') return item.itemType === 'QUEST' || item.subType === 'QUEST' || item.quality === 'QUEST';
      if (currentSec === 'MISC') return !item.weapon && !item.armor && !item.equipment && !item.consumable && item.itemType !== 'QUEST';
      return true;
    }
  });

  // 2. 20 筆單頁分頁計算
  const PAGE_SIZE = 20;
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const curPage = Math.min(Math.max(1, drpgState.itemsPage || 1), totalPages);
  drpgState.itemsPage = curPage;

  const startIdx = (curPage - 1) * PAGE_SIZE;
  const pagedItems = filtered.slice(startIdx, startIdx + PAGE_SIZE);

  // 3. 次級分類標籤列 (Chips)
  let chipsHtml = '';
  if (itemTab === 'USABLE') {
    chipsHtml = `
      <div style="display:flex;align-items:center;gap:8px;padding-bottom:10px;border-bottom:1px solid #1e293b;margin-bottom:12px;flex-wrap:wrap;">
        <span style="font-size:13px;color:#94a3b8;">子分類篩選：</span>
        <button class="act-btn btn-sm ${currentSec === 'ALL' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('ALL')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'ALL' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'ALL' ? '#0f172a' : '#cbd5e1'};">全部可使用</button>
        <button class="act-btn btn-sm ${currentSec === 'HEAL' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('HEAL')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'HEAL' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'HEAL' ? '#0f172a' : '#cbd5e1'};">🌿 氣血/道心回復</button>
        <button class="act-btn btn-sm ${currentSec === 'BUFF' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('BUFF')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'BUFF' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'BUFF' ? '#0f172a' : '#cbd5e1'};">⚡ 增益/丹道Buff</button>
      </div>
    `;
  } else if (itemTab === 'ALL') {
    chipsHtml = `
      <div style="display:flex;align-items:center;gap:8px;padding-bottom:10px;border-bottom:1px solid #1e293b;margin-bottom:12px;flex-wrap:wrap;">
        <span style="font-size:13px;color:#94a3b8;">品項篩選：</span>
        <button class="act-btn btn-sm ${currentSec === 'ALL' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('ALL')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'ALL' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'ALL' ? '#0f172a' : '#cbd5e1'};">全部 (${allSlots.length})</button>
        <button class="act-btn btn-sm ${currentSec === 'EQUIP' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('EQUIP')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'EQUIP' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'EQUIP' ? '#0f172a' : '#cbd5e1'};">🛡️ 裝備法寶</button>
        <button class="act-btn btn-sm ${currentSec === 'CONSUMABLE' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('CONSUMABLE')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'CONSUMABLE' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'CONSUMABLE' ? '#0f172a' : '#cbd5e1'};">🧪 消耗靈丹</button>
        <button class="act-btn btn-sm ${currentSec === 'QUEST' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('QUEST')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'QUEST' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'QUEST' ? '#0f172a' : '#cbd5e1'};">📜 任務信物</button>
        <button class="act-btn btn-sm ${currentSec === 'MISC' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('MISC')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'MISC' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'MISC' ? '#0f172a' : '#cbd5e1'};">📦 靈材雜項</button>
      </div>
    `;
  }

  // 4. 物品卡片渲染
  let itemsGridHtml = '';
  if (pagedItems.length === 0) {
    itemsGridHtml = `
      <div style="flex:1;display:flex;align-items:center;justify-content:center;color:#94a3b8;font-size:14px;padding:40px;border:1px dashed #334155;border-radius:6px;grid-column:1/-1;">
        🎒 此分類下尚無符合條件之靈物或道具。
      </div>
    `;
  } else {
    itemsGridHtml = pagedItems.map(item => {
      const q = (item.quality || 'COMMON').toUpperCase();
      let qColor = '#cbd5e1';
      let qBorder = '#334155';
      if (q === 'UNCOMMON') { qColor = '#34d399'; qBorder = '#059669'; }
      else if (q === 'RARE') { qColor = '#60a5fa'; qBorder = '#2563eb'; }
      else if (q === 'EPIC') { qColor = '#c084fc'; qBorder = '#7c3aed'; }
      else if (q === 'LEGENDARY') { qColor = '#fbbf24'; qBorder = '#d97706'; }
      else if (q === 'QUEST') { qColor = '#fde047'; qBorder = '#ca8a04'; }

      let effectDesc = '';
      if (item.weapon) {
        effectDesc = `<span style="color:#f87171;">🗡️ 攻 +${item.bonusMinDamage}~${item.bonusMaxDamage}</span>`;
      } else if (item.armor) {
        effectDesc = `<span style="color:#60a5fa;">🥋 防 +${item.bonusDefense}, 血 +${item.bonusHp}</span>`;
      } else if (item.effectType === 'HEAL_HP') {
        effectDesc = `<span style="color:#34d399;">🌿 服用回復 ${item.effectValue} HP</span>`;
      } else if (item.effectType === 'RESTORE_SAN') {
        effectDesc = `<span style="color:#c084fc;">📜 服用回復 ${item.effectValue} SAN (定神)</span>`;
      } else if (item.effectType === 'LEARN_SKILL') {
        effectDesc = `<span style="color:#fde047;">🧬 煉化領悟絕學【${item.grantedSkillName || '道種'}】</span>`;
      } else if (item.effectType === 'BUFF') {
        effectDesc = `<span style="color:#38bdf8;">⚡ 服用賦予專屬靈效加持</span>`;
      }

      // 操作按鍵 (支援點擊直接指定隊員)
      let actionButtons = '';
      const members = (party && party.members) ? party.members : [];
      if (item.consumable) {
        actionButtons = `
          <div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap;justify-content:flex-end;">
            <span style="font-size:var(--font-xs);color:#94a3b8;margin-right:2px;">服用給：</span>
            ${members.map((mem, mIdx) => {
              const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
              return `
                <button class="act-btn btn-sm btn-green" ${isAlive ? '' : 'disabled style="opacity:0.35;cursor:not-allowed;"'}
                  onclick="send('use ${item.slotId} ${mIdx}')" title="為 #${mIdx + 1} ${mem.name} 服用" style="font-size:var(--font-xs);padding:3px 7px;">
                  #${mIdx + 1} ${mem.name}
                </button>
              `;
            }).join('')}
          </div>
        `;
      } else if (item.weapon || item.armor || item.equipment || item.itemType === 'WEAPON' || item.itemType === 'ARMOR' || item.itemType === 'SHIELD' || item.itemType === 'ACCESSORY') {
        actionButtons = `
          <div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap;justify-content:flex-end;">
            <span style="font-size:var(--font-xs);color:#94a3b8;margin-right:2px;">穿戴給：</span>
            ${members.map((mem, mIdx) => {
              const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
              return `
                <button class="act-btn btn-sm btn-blue" ${isAlive ? '' : 'disabled style="opacity:0.35;cursor:not-allowed;"'}
                  onclick="send('equip ${item.slotId} ${mIdx}')" title="為 #${mIdx + 1} ${mem.name} 穿戴" style="font-size:var(--font-xs);padding:3px 7px;">
                  #${mIdx + 1}
                </button>
              `;
            }).join('')}
          </div>
        `;
      } else if (item.itemType === 'QUEST' || item.subType === 'QUEST' || item.quality === 'QUEST') {
        actionButtons = `<span style="font-size:var(--font-xs);color:#fde047;background:rgba(253,224,71,0.15);border:1px solid #ca8a04;padding:2px 8px;border-radius:4px;">📜 機緣信物</span>`;
      }

      return `
        <div style="background:#0f172a;border:1px solid ${qBorder};border-radius:8px;padding:12px 14px;display:flex;gap:14px;align-items:center;box-shadow:0 2px 8px rgba(0,0,0,0.35);">
          <!-- 左側物品圖標 -->
          <div style="font-size:24px;width:44px;height:44px;background:#1e293b;border:1px solid #475569;border-radius:6px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
            ${item.icon || '📦'}
          </div>
          <!-- 中間文字資訊 -->
          <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:3px;">
            <div style="display:flex;align-items:center;gap:8px;">
              <span style="font-size:15px;font-weight:bold;color:${qColor};overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${item.name}</span>
              ${item.count > 1 ? `<span style="font-size:var(--font-xs);color:#cbd5e1;background:#1e293b;padding:1px 6px;border-radius:4px;font-weight:bold;flex-shrink:0;">×${item.count}</span>` : ''}
              <span style="font-size:var(--font-xs);color:#94a3b8;border:1px solid #334155;padding:1px 6px;border-radius:3px;flex-shrink:0;">${item.itemType || '道具'}</span>
            </div>
            ${effectDesc ? `<div style="font-size:13px;font-weight:500;">${effectDesc}</div>` : ''}
            <div style="font-size:var(--font-xs);color:#94a3b8;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${item.description || ''}">
              ${item.description || '無描述'}
            </div>
          </div>
          <!-- 右側操作按鈕 -->
          <div style="flex-shrink:0;">
            ${actionButtons}
          </div>
        </div>
      `;
    }).join('');
  }

  const paginationBar = getRenderPaginationBar()(curPage, totalPages, filtered.length, 'window.changeItemsPage');

  return `
    <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;flex:1;display:flex;flex-direction:column;">
      ${chipsHtml}
      <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(460px, 1fr));gap:12px;flex:1;align-content:start;">
        ${itemsGridHtml}
      </div>
      ${paginationBar}
    </div>
  `;
}

/**
 * 渲染角色詳細屬性道基面板 (Characters View)
 */
export function renderMemberStatusDetailHtml(m, selIdx, party) {
  const isLeader = (m.id && (m.id === 'm-leader' || m.id.includes('leader'))) || selIdx === 0;
  const level = m.level || 1;
  const exp = m.exp || 0;
  const nextExp = m.nextLevelExp || 180;
  const expPct = Math.min(100, Math.max(0, Math.floor((exp / nextExp) * 100)));
  const freePoints = m.freeStatPoints || 0;

  const strVal = m.str || 5;
  const conVal = m.con || 5;
  const dexVal = m.dex || 5;
  const intVal = m.intStat !== undefined ? m.intStat : (m.intelligence || 5);
  const wisVal = m.wis || 5;

  const resType = m.resourceType || 'MP';
  const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
  const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;
  let resLabel = `MP: ${curRes}/${maxRes}`;
  if (resType === 'SP' || resType === 'RAGE' || resType === 'COMBO' || resType === 'STAMINA' || resType === 'FORCE' || resType === 'ENERGY') {
    resLabel = `戰氣: ${curRes}/${maxRes}`;
  }

  const renderStatAddBtn = (statKey, label) => {
    if (!isLeader) return '';
    if (freePoints > 0) {
      return `<button class="stat-add-btn" onclick="send('party stat add ${statKey} 1')" title="點擊投入 1 點自由修為點數提升【${label}】">+1</button>`;
    } else {
      return `<button class="stat-add-btn disabled" disabled title="無可用自由修為點數">+1</button>`;
    }
  };

  let freePointsBannerHtml = '';
  if (isLeader) {
    if (freePoints > 0) {
      freePointsBannerHtml = `
        <div class="free-points-banner" style="font-size:14px;">
          <div style="display:flex;align-items:center;gap:8px;">
            <span>⭐</span>
            <span><strong>道胎未定・造化充盈</strong>：尚有 <strong style="font-size:16px;color:#fde047;">${freePoints}</strong> 點自由修為點數！</span>
          </div>
          <span style="font-size:13px;color:#fef3c7;">(點擊下方屬性右側 [+1] 按鈕即刻分配)</span>
        </div>
      `;
    } else {
      freePointsBannerHtml = `
        <div style="font-size:13px;color:#94a3b8;display:flex;justify-content:space-between;padding:4px 6px;">
          <span>⭐ 自由修為點數：0 點</span>
          <span style="color:#64748b;">(主角每升一級額外獲贈 2 點自由分配點數)</span>
        </div>
      `;
    }
  }

  return `
    <div class="character-detail-container" style="display:grid;grid-template-columns:300px 1fr;gap:16px;">
      <!-- 左欄：核心氣血、真元、歷練進度與陣容定位 -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;display:flex;flex-direction:column;gap:14px;">
        <div style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;padding-bottom:12px;">
          <div>
            <div style="font-size:18px;font-weight:bold;color:${isLeader ? '#fde047' : '#38bdf8'};display:flex;align-items:center;gap:6px;">
              ${isLeader ? '👑' : '👤'} #${selIdx + 1} ${m.name}
            </div>
            <div style="font-size:13px;color:#94a3b8;margin-top:2px;">
              門派/職能：<strong style="color:#cbd5e1;">${m.className || '道友'}</strong> ‧ ${m.roleTitle || '成員'}
            </div>
          </div>
          <div style="text-align:right;">
            <div style="font-size:14px;color:#fbbf24;font-weight:bold;">Lv.${level}</div>
            <div style="font-size:11px;color:#64748b;">境階層級</div>
          </div>
        </div>

        <!-- 歷練進度條 (EXP) -->
        <div style="background:#0f172a;border:1px solid #334155;border-radius:6px;padding:10px;">
          <div style="display:flex;justify-content:space-between;font-size:12px;color:#cbd5e1;margin-bottom:4px;">
            <span>✨ 修為歷練 (EXP)</span>
            <span style="font-weight:bold;color:#fde047;">${exp} / ${nextExp} (${expPct}%)</span>
          </div>
          <div style="width:100%;height:6px;background:#1e293b;border-radius:3px;overflow:hidden;">
            <div style="width:${expPct}%;height:100%;background:linear-gradient(90deg, #f59e0b, #fde047);border-radius:3px;"></div>
          </div>
        </div>

        <!-- 氣血與真元 -->
        <div style="display:flex;flex-direction:column;gap:10px;">
          <div style="background:#0f172a;border:1px solid #334155;border-radius:6px;padding:10px;">
            <div style="display:flex;justify-content:space-between;font-size:12px;color:#cbd5e1;margin-bottom:4px;">
              <span>❤️ 氣血生命 (HP)</span>
              <span style="font-weight:bold;color:#f87171;">${m.hp} / ${m.maxHp}</span>
            </div>
            <div style="width:100%;height:6px;background:#1e293b;border-radius:3px;overflow:hidden;">
              <div style="width:${Math.min(100, Math.max(0, Math.floor((m.hp / m.maxHp) * 100)))}%;height:100%;background:linear-gradient(90deg, #ef4444, #f87171);border-radius:3px;"></div>
            </div>
          </div>

          <div style="background:#0f172a;border:1px solid #334155;border-radius:6px;padding:10px;">
            <div style="display:flex;justify-content:space-between;font-size:12px;color:#cbd5e1;margin-bottom:4px;">
              <span>⚡ 真元/戰氣 (${resType})</span>
              <span style="font-weight:bold;color:#60a5fa;">${resLabel}</span>
            </div>
            <div style="width:100%;height:6px;background:#1e293b;border-radius:3px;overflow:hidden;">
              <div style="width:${Math.min(100, Math.max(0, Math.floor((curRes / maxRes) * 100)))}%;height:100%;background:linear-gradient(90deg, #3b82f6, #60a5fa);border-radius:3px;"></div>
            </div>
          </div>
        </div>

        <div style="background:rgba(15,23,42,0.6);border:1px dashed #334155;border-radius:6px;padding:10px;font-size:13px;color:#94a3b8;line-height:1.4;">
          💡 提示：主角每提升一個境界，即可獲贈 2 點自由分配之先天道基點數。同伴之五維道基由其先天資質與門派專精自動成長。
        </div>
      </div>

      <!-- 右欄：先天五維、攻防抗性與道心 SAN -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;display:flex;flex-direction:column;gap:14px;">
        ${freePointsBannerHtml}

        <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(200px, 1fr));gap:12px;">
          <!-- 先天五維分配 -->
          <div style="background:#0f172a;border:1px solid #334155;border-radius:6px;padding:12px;">
            <div style="font-size:14px;color:#38bdf8;font-weight:bold;margin-bottom:10px;border-bottom:1px solid #1e293b;padding-bottom:6px;">
              ☯️ 先天五維道基
            </div>
            <div style="display:flex;flex-direction:column;gap:8px;">
              <div class="stat-row" style="font-size:13px;">
                <span>💪 膂力 (STR)：<strong style="color:#f1f5f9;">${strVal}</strong></span>
                ${renderStatAddBtn('str', '膂力')}
              </div>
              <div class="stat-row" style="font-size:13px;">
                <span>🛡️ 根骨 (CON)：<strong style="color:#f1f5f9;">${conVal}</strong></span>
                ${renderStatAddBtn('con', '根骨')}
              </div>
              <div class="stat-row" style="font-size:13px;">
                <span>💨 身法 (DEX)：<strong style="color:#f1f5f9;">${dexVal}</strong></span>
                ${renderStatAddBtn('dex', '身法')}
              </div>
              <div class="stat-row" style="font-size:13px;">
                <span>🧠 悟性 (INT)：<strong style="color:#f1f5f9;">${intVal}</strong></span>
                ${renderStatAddBtn('int', '悟性')}
              </div>
              <div class="stat-row" style="font-size:13px;">
                <span>🧘 定力 (WIS)：<strong style="color:#f1f5f9;">${wisVal}</strong></span>
                ${renderStatAddBtn('wis', '定力')}
              </div>
            </div>
          </div>

          <!-- 攻防綜合戰力 -->
          <div style="background:#0f172a;border:1px solid #334155;border-radius:6px;padding:12px;">
            <div style="font-size:14px;color:#fde047;font-weight:bold;margin-bottom:10px;border-bottom:1px solid #1e293b;padding-bottom:6px;">
              ⚔️ 攻防修為實力
            </div>
            <div style="display:flex;flex-direction:column;gap:8px;font-size:13px;color:#cbd5e1;">
              <div>物理殺傷：<strong style="color:#f87171;">${m.minDamage || 5} ~ ${m.maxDamage || 10}</strong></div>
              <div>道術護甲：<strong style="color:#60a5fa;">${m.defense || 0}</strong></div>
              <div>暴擊概率：<strong style="color:#fbbf24;">${m.critRate || 5}%</strong></div>
              <div>身法閃避：<strong style="color:#34d399;">${m.dodgeRate || 5}%</strong></div>
              <div>格擋招架：<strong style="color:#a78bfa;">${m.parryRate || 0}%</strong></div>
            </div>
          </div>
        </div>

        <div style="font-size:14px;color:#34d399;font-weight:bold;">🧘 道心 SAN: ${m.san}/${m.maxSan} (${m.sanityStatus || '心境平穩'})</div>
      </div>
    </div>
  `;
}

/**
 * 渲染成員裝備與被動功法畫面 (Equipment View)
 */
export function renderMemberEquipHtml(m, selIdx, party) {
  let equipSlotsHtml = '';
  for (const slot of EQUIP_SLOT_DEFS) {
    let item = m.equipment ? m.equipment[slot.key] : null;
    if (!item && slot.key === 'MAIN_HAND' && m.equippedWeapon) item = m.equippedWeapon;
    if (!item && slot.key === 'BODY' && m.equippedArmor) item = m.equippedArmor;

    if (item) {
      let statsParts = [];
      if (item.bonusMinDamage || item.bonusMaxDamage) statsParts.push(`攻 ${item.bonusMinDamage}~${item.bonusMaxDamage}`);
      if (item.bonusDefense) statsParts.push(`防 +${item.bonusDefense}`);
      if (item.bonusHp) statsParts.push(`血 +${item.bonusHp}`);
      if (item.bonusSan) statsParts.push(`心 +${item.bonusSan}`);
      const statsStr = statsParts.length > 0 ? statsParts.join(' ') : '基礎裝備';

      equipSlotsHtml += `
        <div class="equip-slot-box has-item" title="${item.description || ''}" style="font-size:13px;">
          <div class="equip-slot-title">
            <span style="font-size:13px;">${slot.icon} ${slot.label}</span>
            <button class="unequip-mini-btn" onclick="send('item unequip ${slot.alias} ${selIdx}')" title="卸下放回行囊" style="font-size:var(--font-xs);">✕ 卸下</button>
          </div>
          <div class="equip-slot-name" style="font-size:14px;font-weight:bold;">${item.icon || '📦'} ${item.name}</div>
          <div class="equip-slot-stats" style="font-size:var(--font-xs);">${statsStr}</div>
        </div>
      `;
    } else {
      equipSlotsHtml += `
        <div class="equip-slot-box empty" style="font-size:13px;">
          <div class="equip-slot-title">
            <span style="font-size:13px;">${slot.icon} ${slot.label}</span>
          </div>
          <div class="equip-slot-name" style="color:#64748b;font-weight:normal;font-size:13px;">(未穿戴)</div>
          <div class="equip-slot-act">
            <button class="item-act-mini-btn" onclick="toggleBagDrawer(true)" title="開啟行囊挑選裝備穿戴" style="font-size:var(--font-xs);">🎒 挑選</button>
          </div>
        </div>
      `;
    }
  }

  // 被動功法欄位 (Stance, Parry, Dodge, Force)
  const passiveCategories = [
    { key: 'STANCE', label: '兵刃套路', icon: '⚔️', val: m.basicSkillName || null },
    { key: 'PARRY', label: '護身招架', icon: '🛡️', val: getPassiveSkillDisplayName(m, 'PARRY') },
    { key: 'DODGE', label: '靈動身法', icon: '💨', val: getPassiveSkillDisplayName(m, 'DODGE') },
    { key: 'FORCE', label: '玄門心法', icon: '🧘', val: getPassiveSkillDisplayName(m, 'FORCE') }
  ];

  let passivesHtml = passiveCategories.map(p => {
    if (p.val) {
      return `
        <div class="equip-slot-item-row" onclick="window.openSkillPicker(${selIdx}, '${p.key}', '${p.label}')" title="點擊更換功法" style="background:#0f172a;border:1px solid #334155;border-radius:6px;padding:8px 12px;display:flex;justify-content:space-between;align-items:center;cursor:pointer;">
          <span style="font-size:13px;color:#94a3b8;">${p.icon} ${p.label}</span>
          <div style="display:flex;align-items:center;gap:8px;">
            <span style="font-size:13px;color:#38bdf8;font-weight:bold;">${p.val}</span>
            <button class="act-btn btn-sm" onclick="event.stopPropagation();window.confirmUnequipSkill(${selIdx}, '${p.key}');" style="font-size:var(--font-xs);padding:2px 6px;background:rgba(239,68,68,0.2);color:#fca5a5;border:1px solid #ef4444;" title="卸下還原為預設">✕</button>
          </div>
        </div>
      `;
    } else {
      return `
        <div class="equip-slot-item-row is-empty" onclick="window.openSkillPicker(${selIdx}, '${p.key}', '${p.label}')" title="點擊挑選功法" style="background:#0f172a;border:1px dashed #334155;border-radius:6px;padding:8px 12px;display:flex;justify-content:space-between;align-items:center;cursor:pointer;">
          <span style="font-size:13px;color:#64748b;">${p.icon} ${p.label}</span>
          <span style="font-size:13px;color:#64748b;">(無) <span style="color:#94a3b8;">+ 選擇功法</span></span>
        </div>
      `;
    }
  }).join('');

  return `
    <div style="display:flex;flex-direction:column;gap:14px;">
      <!-- 1. 角色摘要條 -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:12px 18px;display:flex;justify-content:space-between;align-items:center;">
        <div style="display:flex;align-items:center;gap:12px;">
          <span style="font-size:18px;font-weight:bold;color:#38bdf8;">#${selIdx + 1} ${m.name}</span>
          <span style="font-size:13px;color:#cbd5e1;background:#0f172a;padding:2px 8px;border-radius:4px;">Lv.${m.level || 1} ${m.className || '道友'}</span>
          <span style="font-size:13px;color:#94a3b8;">氣血: ${m.hp}/${m.maxHp}</span>
        </div>
      </div>

      <!-- 2. 7 部位裝備槽位 (5 基礎 + 2 飾品) -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;">
        <div style="font-size:14px;color:#cbd5e1;font-weight:bold;margin-bottom:10px;">🛡️ 穿戴裝備欄 (7 槽位)：</div>
        <div class="party-detail-equip-grid">
          ${equipSlotsHtml}
        </div>
      </div>

      <!-- 3. 被動四槽位功法 -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;">
        <div style="font-size:14px;color:#cbd5e1;font-weight:bold;margin-bottom:10px;">⚡ 主修與被動套路功法 (4 部位)：</div>
        <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(220px, 1fr));gap:10px;">
          ${passivesHtml}
        </div>
      </div>
    </div>
  `;
}

export function openEquipPicker(memberIdx, slotKey, slotAlias, slotLabel) {
  drpgState.equipPicker = {
    memberIdx,
    slotKey,
    slotAlias,
    slotLabel,
    selectedSlotId: null,
    page: 1
  };
  const render = getRenderPartyModal();
  if (render) render();
}

export function closeEquipPicker() {
  drpgState.equipPicker = null;
  const render = getRenderPartyModal();
  if (render) render();
}

export function selectEquipPickerItem(slotId) {
  if (drpgState.equipPicker) {
    drpgState.equipPicker.selectedSlotId = slotId;
    const render = getRenderPartyModal();
    if (render) render();
  }
}

export function changeEquipPickerPage(delta) {
  if (drpgState.equipPicker) {
    drpgState.equipPicker.page = Math.max(1, (drpgState.equipPicker.page || 1) + delta);
    const render = getRenderPartyModal();
    if (render) render();
  }
}

export function confirmEquipItem(slotId, memberIdx) {
  if (!slotId) return;
  send(`item equip ${slotId} ${memberIdx}`);
  drpgState.equipPicker = null;
}

export function confirmUnequipItem(slotAlias, memberIdx) {
  if (!slotAlias) return;
  send(`item unequip ${slotAlias} ${memberIdx}`);
  drpgState.equipPicker = null;
}

export function renderMemberEquipmentView(m, selIdx, party) {
  if (!m) return '<div style="color:#94a3b8;padding:30px;text-align:center;font-size:14px;">查無此成員資料。</div>';

  const isLeader = (selIdx === 0);
  const isAlive = (m.alive !== undefined ? m.alive : m.hp > 0);

  // 1. 裝備累計加成計算
  let bonusMin = 0;
  let bonusMax = 0;
  let bonusDef = 0;
  let bonusHp = 0;
  let bonusSan = 0;
  const eqMap = m.equipment || {};
  Object.values(eqMap).forEach(it => {
    if (it) {
      bonusMin += (it.bonusMinDamage || 0);
      bonusMax += (it.bonusMaxDamage || 0);
      bonusDef += (it.bonusDefense || 0);
      bonusHp += (it.bonusHp || 0);
      bonusSan += (it.bonusSan || 0);
    }
  });
  if (bonusMin === 0 && bonusMax === 0 && m.equippedWeapon) {
    bonusMin += (m.equippedWeapon.bonusMinDamage || 0);
    bonusMax += (m.equippedWeapon.bonusMaxDamage || 0);
  }
  if (bonusDef === 0 && m.equippedArmor) {
    bonusDef += (m.equippedArmor.bonusDefense || 0);
  }

  // 資源標籤
  const resType = m.resourceType || 'MP';
  const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
  const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;
  const isSp = (resType === 'SP' || resType === 'RAGE' || resType === 'COMBO' || resType === 'STAMINA' || resType === 'FORCE' || resType === 'ENERGY');
  const resName = isSp ? '戰氣' : '真元';
  const resColor = isSp ? '#60a5fa' : '#38bdf8';
  const hpPct = Math.min(100, Math.max(0, Math.round((m.hp / (m.maxHp || 1)) * 100)));
  const resPct = Math.min(100, Math.max(0, Math.round((curRes / (maxRes || 1)) * 100)));

  // 2. 7 個裝備槽位生成
  let equipCount = 0;
  const equipRowsHtml = EQUIP_SLOT_DEFS.map(slot => {
    const item = getMemberSlotItem(m, slot.key);
    if (item) {
      equipCount++;
      let statsParts = [];
      if (item.bonusMinDamage || item.bonusMaxDamage) statsParts.push(`攻 +${item.bonusMinDamage}~${item.bonusMaxDamage}`);
      if (item.bonusDefense) statsParts.push(`防 +${item.bonusDefense}`);
      if (item.bonusHp) statsParts.push(`血 +${item.bonusHp}`);
      if (item.bonusSan) statsParts.push(`心 +${item.bonusSan}`);
      const statStr = statsParts.length > 0 ? statsParts.join(' ') : '裝備中';

      return `
        <div class="equip-slot-item-row" onclick="window.openEquipPicker(${selIdx}, '${slot.key}', '${slot.alias}', '${slot.label}')" title="點擊更換法寶裝備或比較屬性" style="height:38px;box-sizing:border-box;">
          <div style="display:flex;align-items:center;gap:8px;min-width:0;flex:1;">
            <span style="font-size:13px;color:#94a3b8;flex-shrink:0;">${slot.icon} ${slot.label}</span>
            <span style="font-size:13px;font-weight:bold;color:#f1f5f9;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${item.name}</span>
            <span style="font-size:12px;color:#38bdf8;flex-shrink:0;margin-left:auto;margin-right:8px;">${statStr}</span>
          </div>
          <button class="act-btn btn-sm" onclick="event.stopPropagation();send('item unequip ${slot.alias} ${selIdx}');" style="font-size:12px;padding:2px 7px;background:rgba(239,68,68,0.2);color:#fca5a5;border:1px solid #ef4444;border-radius:3px;" title="卸下裝備">
            ✕
          </button>
        </div>
      `;
    } else {
      return `
        <div class="equip-slot-item-row is-empty" onclick="window.openEquipPicker(${selIdx}, '${slot.key}', '${slot.alias}', '${slot.label}')" title="點擊挑選裝備穿戴" style="height:38px;box-sizing:border-box;">
          <span style="font-size:13px;color:#64748b;">${slot.icon} ${slot.label}</span>
          <span style="font-size:12px;color:#94a3b8;">+ 挑選法寶</span>
        </div>
      `;
    }
  }).join('');

  // 3. 4 個被動功法槽位生成
  const passives = [
    { key: 'STANCE', icon: '⚔️', label: '兵刃套路', typeDesc: '連攜普攻', val: m.basicSkillName || null },
    { key: 'PARRY', icon: '🛡️', label: '護身招架', typeDesc: '受擊減傷', val: getPassiveSkillDisplayName(m, 'PARRY') },
    { key: 'DODGE', icon: '💨', label: '靈動身法', typeDesc: '身法閃避', val: getPassiveSkillDisplayName(m, 'DODGE') },
    { key: 'FORCE', icon: '🟣', label: '玄門心法', typeDesc: '內功修為', val: getPassiveSkillDisplayName(m, 'FORCE') }
  ];

  let passiveCount = 0;
  const passivesHtml = passives.map(p => {
    if (p.val) {
      passiveCount++;
      return `
        <div class="equip-slot-item-row" onclick="window.openSkillPicker(${selIdx}, '${p.key}', '${p.label}')" title="點擊更換功法" style="height:44px;box-sizing:border-box;">
          <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:1px;">
            <div style="display:flex;align-items:center;gap:6px;">
              <span style="font-size:13px;color:#94a3b8;flex-shrink:0;">${p.icon} ${p.label}</span>
              <span style="font-size:13px;font-weight:bold;color:#c084fc;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">【${p.val}】</span>
            </div>
            <span style="font-size:11px;color:#64748b;">${p.typeDesc}常駐加持中</span>
          </div>
          <button class="act-btn btn-sm" onclick="event.stopPropagation();window.confirmUnequipSkill(${selIdx}, '${p.key}');" style="font-size:12px;padding:2px 7px;margin-left:6px;background:rgba(239,68,68,0.2);color:#fca5a5;border:1px solid #ef4444;border-radius:3px;" title="卸下還原為預設招式">
            ✕
          </button>
        </div>
      `;
    } else {
      return `
        <div class="equip-slot-item-row is-empty" onclick="window.openSkillPicker(${selIdx}, '${p.key}', '${p.label}')" title="點擊挑選功法" style="height:44px;box-sizing:border-box;">
          <div style="display:flex;align-items:center;gap:6px;">
            <span style="font-size:13px;color:#64748b;">${p.icon} ${p.label}</span>
            <span style="font-size:12px;color:#64748b;">(門派基礎)</span>
          </div>
          <span style="font-size:12px;color:#94a3b8;">+ 配置心法</span>
        </div>
      `;
    }
  }).join('');

  // 4. 組合三欄全資訊頁面 (無捲軸設計)
  return `
    <div class="member-equip-view-container" style="display:grid;grid-template-columns: 280px 1.25fr 1fr;gap:14px;align-items:stretch;height:100%;box-sizing:border-box;">
      <!-- 第 1 欄：角色道基與戰力屬性卡 -->
      <div style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:14px;display:flex;flex-direction:column;gap:10px;box-shadow:0 4px 12px rgba(0,0,0,0.35);">
        <!-- 標頭：頭銜、等級、職業與站位 -->
        <div style="border-bottom:1px solid #334155;padding-bottom:10px;display:flex;flex-direction:column;gap:4px;">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span style="font-size:17px;font-weight:bold;color:${isLeader ? '#fde047' : '#38bdf8'};display:flex;align-items:center;gap:4px;">
              ${isLeader ? '👑' : '👤'} #${selIdx + 1} ${m.name}
            </span>
          </div>
          <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:#cbd5e1;">
            <span>Lv.${m.level || 1}</span>
            <span>🏷️ ${m.className || '道友'}</span>
            ${!isAlive ? '<span style="color:#ef4444;font-weight:bold;">(💀 陣亡)</span>' : ''}
          </div>
        </div>

        <!-- 氣血與資源條 -->
        <div style="display:flex;flex-direction:column;gap:6px;background:#1e293b;border:1px solid #334155;border-radius:6px;padding:10px;">
          <div>
            <div style="display:flex;justify-content:space-between;font-size:12px;color:#cbd5e1;margin-bottom:3px;">
              <span>❤️ 氣血</span>
              <span style="font-weight:bold;color:#f87171;">${m.hp} / ${m.maxHp}</span>
            </div>
            <div style="width:100%;height:6px;background:#0f172a;border-radius:3px;overflow:hidden;">
              <div style="width:${hpPct}%;height:100%;background:linear-gradient(90deg, #ef4444, #f87171);border-radius:3px;"></div>
            </div>
          </div>
          <div>
            <div style="display:flex;justify-content:space-between;font-size:12px;color:#cbd5e1;margin-bottom:3px;">
              <span>⚡ ${resName}</span>
              <span style="font-weight:bold;color:${resColor};">${curRes} / ${maxRes}</span>
            </div>
            <div style="width:100%;height:6px;background:#0f172a;border-radius:3px;overflow:hidden;">
              <div style="width:${resPct}%;height:100%;background:linear-gradient(90deg, #3b82f6, #60a5fa);border-radius:3px;"></div>
            </div>
          </div>
        </div>

        <!-- 攻防綜合戰力加成 -->
        <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:8px 10px;display:flex;flex-direction:column;gap:5px;font-size:13px;">
          <div style="display:flex;justify-content:space-between;">
            <span style="color:#fde047;">⚔️ 裝備攻加成:</span>
            <span style="font-weight:bold;color:#fde047;">+${bonusMin} ~ +${bonusMax}</span>
          </div>
          <div style="display:flex;justify-content:space-between;border-top:1px solid #334155;padding-top:4px;">
            <span style="color:#34d399;">🛡️ 裝備防加成:</span>
            <span style="font-weight:bold;color:#34d399;">+${bonusDef}</span>
          </div>
        </div>

        <!-- 五維先天道基 -->
        <div style="display:flex;flex-direction:column;gap:6px;background:#1e293b;border:1px solid #334155;border-radius:6px;padding:8px 10px;">
          <div style="font-size:12px;color:#94a3b8;font-weight:bold;">☯️ 先天五維道基</div>
          <div style="display:grid;grid-template-columns:repeat(5, 1fr);gap:4px;text-align:center;">
            <div style="background:#0f172a;padding:3px 2px;border-radius:4px;font-size:12px;color:#cbd5e1;">力<strong style="color:#f1f5f9;display:block;">${m.str || 5}</strong></div>
            <div style="background:#0f172a;padding:3px 2px;border-radius:4px;font-size:12px;color:#cbd5e1;">骨<strong style="color:#f1f5f9;display:block;">${m.con || 5}</strong></div>
            <div style="background:#0f172a;padding:3px 2px;border-radius:4px;font-size:12px;color:#cbd5e1;">巧<strong style="color:#f1f5f9;display:block;">${m.dex || 5}</strong></div>
            <div style="background:#0f172a;padding:3px 2px;border-radius:4px;font-size:12px;color:#cbd5e1;">悟<strong style="color:#f1f5f9;display:block;">${m.intStat !== undefined ? m.intStat : (m.intelligence || 5)}</strong></div>
            <div style="background:#0f172a;padding:3px 2px;border-radius:4px;font-size:12px;color:#cbd5e1;">定<strong style="color:#f1f5f9;display:block;">${m.wis || 5}</strong></div>
          </div>
        </div>

        <!-- 陣法效果 -->
        ${m.formationSlotBonus ? `
          <div style="font-size:12px;color:#6ee7b7;background:rgba(16,185,129,0.12);border:1px solid rgba(16,185,129,0.3);padding:6px 10px;border-radius:6px;line-height:1.3;">
            ☯️ 陣法契合: ${m.formationSlotBonus}
          </div>
        ` : ''}

        <!-- 底部統計摘要 -->
        <div style="margin-top:auto;font-size:12px;color:#64748b;display:flex;justify-content:space-between;border-top:1px solid #334155;padding-top:6px;">
          <span>法寶穿戴: <strong style="color:#38bdf8;">${equipCount}/7</strong></span>
          <span>功法啟用: <strong style="color:#c084fc;">${passiveCount}/4</strong></span>
        </div>
      </div>

      <!-- 第 2 欄：🛡️ 穿戴法寶裝備 (7 槽位) -->
      <div style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:12px 14px;display:flex;flex-direction:column;gap:8px;box-shadow:0 4px 12px rgba(0,0,0,0.35);">
        <div style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;padding-bottom:8px;">
          <div style="font-size:14px;font-weight:bold;color:#f1f5f9;display:flex;align-items:center;gap:6px;">
            <span>🛡️ 穿戴法寶裝備 (7 槽位)</span>
          </div>
          <span style="font-size:12px;color:#64748b;">點擊槽位換裝</span>
        </div>
        <div style="display:flex;flex-direction:column;gap:6px;">
          ${equipRowsHtml}
        </div>
      </div>

      <!-- 第 3 欄：🧘 主修功法套路 (4 部位) -->
      <div style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:12px 14px;display:flex;flex-direction:column;gap:8px;box-shadow:0 4px 12px rgba(0,0,0,0.35);">
        <div style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;padding-bottom:8px;">
          <div style="font-size:14px;font-weight:bold;color:#f1f5f9;display:flex;align-items:center;gap:6px;">
            <span>🧘 主修功法套路 (4 部位)</span>
          </div>
          <span style="font-size:12px;color:#64748b;">點擊更換心法</span>
        </div>
        <div style="display:flex;flex-direction:column;gap:8px;">
          ${passivesHtml}
        </div>
        <!-- 功法運行心法提示 -->
        <div style="margin-top:auto;background:rgba(30,41,59,0.5);border:1px solid #1e293b;border-radius:6px;padding:10px;font-size:12px;color:#94a3b8;line-height:1.4;">
          <div style="color:#cbd5e1;font-weight:bold;margin-bottom:3px;">📜 功法運轉秘訣：</div>
          兵刃套路隨主手武器普攻發動；招架、身法與內功心法於戰鬥中常駐生效。未配置進階套路時，自動運轉門派基礎心法。
        </div>
      </div>
    </div>
  `;
}

/**
 * 5 隊員直排全隊裝備總覽 (5-column vertical overview)
 */
export function renderTeamEquipmentOverview(party) {
  const members = party.members || [];
  if (members.length === 0) {
    return '<div style="color:#94a3b8;padding:30px;text-align:center;font-size:14px;">隊伍中尚無任何成員。</div>';
  }

  const columnsHtml = members.map((m, idx) => {
    const isLeader = (idx === 0);
    const isAlive = (m.alive !== undefined ? m.alive : m.hp > 0);

    // 計算累計加成數值
    let bonusMin = 0;
    let bonusMax = 0;
    let bonusDef = 0;
    let bonusHp = 0;
    let bonusSan = 0;
    const eqMap = m.equipment || {};
    Object.values(eqMap).forEach(it => {
      if (it) {
        bonusMin += (it.bonusMinDamage || 0);
        bonusMax += (it.bonusMaxDamage || 0);
        bonusDef += (it.bonusDefense || 0);
        bonusHp += (it.bonusHp || 0);
        bonusSan += (it.bonusSan || 0);
      }
    });
    if (bonusMin === 0 && bonusMax === 0 && m.equippedWeapon) {
      bonusMin += (m.equippedWeapon.bonusMinDamage || 0);
      bonusMax += (m.equippedWeapon.bonusMaxDamage || 0);
    }
    if (bonusDef === 0 && m.equippedArmor) {
      bonusDef += (m.equippedArmor.bonusDefense || 0);
    }

    // 7 個裝備槽位
    const equipRowsHtml = EQUIP_SLOT_DEFS.map(slot => {
      const item = getMemberSlotItem(m, slot.key);
      if (item) {
        let statsParts = [];
        if (item.bonusMinDamage || item.bonusMaxDamage) statsParts.push(`攻 +${item.bonusMinDamage}~${item.bonusMaxDamage}`);
        if (item.bonusDefense) statsParts.push(`防 +${item.bonusDefense}`);
        if (item.bonusHp) statsParts.push(`血 +${item.bonusHp}`);
        if (item.bonusSan) statsParts.push(`心 +${item.bonusSan}`);
        const statStr = statsParts.length > 0 ? statsParts.join(' ') : '裝備中';

        return `
          <div class="equip-slot-item-row" onclick="window.openEquipPicker(${idx}, '${slot.key}', '${slot.alias}', '${slot.label}')" title="點擊挑選更換或比較屬性">
            <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:2px;">
              <div style="display:flex;align-items:center;gap:6px;">
                <span style="font-size:13px;color:#94a3b8;">${slot.icon} ${slot.label}</span>
                <span style="font-size:13px;font-weight:bold;color:#f1f5f9;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${item.name}</span>
              </div>
              <div style="font-size:13px;color:#38bdf8;">${statStr}</div>
            </div>
            <button class="act-btn btn-sm" onclick="event.stopPropagation();send('item unequip ${slot.alias} ${idx}');" style="font-size:12px;padding:2px 8px;margin-left:6px;background:rgba(239,68,68,0.2);color:#fca5a5;border:1px solid #ef4444;" title="卸下裝備">
              ✕
            </button>
          </div>
        `;
      } else {
        return `
          <div class="equip-slot-item-row is-empty" onclick="window.openEquipPicker(${idx}, '${slot.key}', '${slot.alias}', '${slot.label}')" title="點擊挑選裝備穿戴">
            <span style="font-size:13px;color:#64748b;">${slot.icon} ${slot.label}</span>
            <span style="font-size:12px;color:#94a3b8;">+ 挑選裝備</span>
          </div>
        `;
      }
    }).join('');

    return `
      <div class="equip-member-column" style="display:flex;flex-direction:column;gap:12px;background:#0f172a;border:1px solid #334155;border-radius:8px;padding:12px;">
        <div style="display:flex;justify-content:space-between;align-items:center;border-bottom:1px solid #334155;padding-bottom:8px;">
          <div>
            <div style="font-size:16px;font-weight:bold;color:${isLeader ? '#fde047' : '#38bdf8'};display:flex;align-items:center;gap:4px;">
              ${isLeader ? '👑' : '👤'} #${idx + 1} ${m.name}
            </div>
            <div style="font-size:12px;color:#94a3b8;margin-top:2px;">
              ${m.className || '道友'} ‧ Lv.${m.level || 1}
              ${!isAlive ? '<span style="color:#ef4444;font-weight:bold;">(💀 陣亡)</span>' : ''}
            </div>
          </div>
        </div>

        <div style="display:flex;justify-content:space-between;font-size:13px;color:#cbd5e1;background:#1e293b;padding:6px 10px;border-radius:4px;">
          <span>攻: <strong style="color:#fde047;">+${bonusMin}~+${bonusMax}</strong></span>
          <span>防: <strong style="color:#34d399;">+${bonusDef}</strong></span>
          <span>血: <strong style="color:#f87171;">+${bonusHp}</strong></span>
        </div>

        <div style="display:flex;flex-direction:column;gap:6px;flex:1;">
          ${equipRowsHtml}
        </div>
      </div>
    `;
  }).join('');

  return `
    <div class="equip-team-grid">
      ${columnsHtml}
    </div>
  `;
}

/**
 * 裝備挑選與 Diff 比較子視圖 (Sub-view Overlay)
 */
export function renderEquipmentDiffPickerView(party) {
  const picker = drpgState.equipPicker;
  if (!picker) return '';

  const memberIdx = picker.memberIdx || 0;
  const m = (party.members && party.members[memberIdx]) ? party.members[memberIdx] : null;
  if (!m) return '<div style="color:#94a3b8;padding:20px;font-size:14px;">隊員資料異常。</div>';

  const slotKey = picker.slotKey;
  const slotAlias = picker.slotAlias;
  const slotLabel = picker.slotLabel;
  const curItem = getMemberSlotItem(m, slotKey);

  // 1. 從背包過濾出符合此槽位的裝備候選清單
  const inv = (party && party.inventory) ? party.inventory : null;
  const allSlots = (inv && inv.slots) ? inv.slots : [];
  const candidates = allSlots.filter(it => isItemMatchingSlot(it, slotKey));

  // 2. 如果尚未指定選取項，預設選取候選列表第 1 項
  if (!picker.selectedSlotId && candidates.length > 0) {
    picker.selectedSlotId = candidates[0].slotId;
  }

  // 3. 20 筆單頁分頁計算
  const PAGE_SIZE = 20;
  const totalPages = Math.max(1, Math.ceil(candidates.length / PAGE_SIZE));
  const curPage = Math.min(Math.max(1, picker.page || 1), totalPages);
  picker.page = curPage;

  const startIdx = (curPage - 1) * PAGE_SIZE;
  const pagedCandidates = candidates.slice(startIdx, startIdx + PAGE_SIZE);

  // 4. 左側候選列表 HTML
  let candidateListHtml = '';
  if (pagedCandidates.length === 0) {
    candidateListHtml = `
      <div style="padding:40px 20px;text-align:center;color:#94a3b8;display:flex;flex-direction:column;gap:12px;align-items:center;grid-column:1/-1;">
        <span style="font-size:36px;">🎒</span>
        <span style="font-size:15px;color:#e2e8f0;font-weight:bold;">行囊中尚無可穿戴的【${slotLabel}】</span>
        <span style="font-size:13px;color:#64748b;">可至古塚探勘擊殺妖邪獲取裝備戰利品。</span>
      </div>
    `;
  } else {
    candidateListHtml = pagedCandidates.map(it => {
      const isSelected = (it.slotId === picker.selectedSlotId);
      let statsParts = [];
      if (it.bonusMinDamage || it.bonusMaxDamage) statsParts.push(`攻 +${it.bonusMinDamage}~${it.bonusMaxDamage}`);
      if (it.bonusDefense) statsParts.push(`防 +${it.bonusDefense}`);
      if (it.bonusHp) statsParts.push(`血 +${it.bonusHp}`);
      if (it.bonusSan) statsParts.push(`心 +${it.bonusSan}`);
      const statsStr = statsParts.length > 0 ? statsParts.join(' ‧ ') : '基礎裝備';

      return `
        <div class="equip-candidate-card ${isSelected ? 'is-selected' : ''}" onclick="window.selectEquipPickerItem('${it.slotId}')">
          <div style="font-size:24px;width:40px;height:40px;background:#0b1120;border:1px solid #334155;border-radius:6px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
            ${it.icon || '📦'}
          </div>
          <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:2px;">
            <div style="display:flex;align-items:center;gap:6px;">
              <span style="font-size:14px;font-weight:bold;color:#f1f5f9;">${it.name}</span>
              ${it.count > 1 ? `<span style="font-size:13px;color:#94a3b8;">x${it.count}</span>` : ''}
              ${isSelected ? '<span style="font-size:13px;color:#38bdf8;background:rgba(56,189,248,0.15);padding:1px 6px;border-radius:3px;">比較中</span>' : ''}
            </div>
            <div style="font-size:13px;color:#38bdf8;">${statsStr}</div>
          </div>
          <button class="act-btn btn-sm btn-blue" onclick="event.stopPropagation();window.confirmEquipItem('${it.slotId}', ${memberIdx});" style="font-size:13px;padding:4px 12px;flex-shrink:0;">
            ✨ 穿戴
          </button>
        </div>
      `;
    }).join('');
  }

  // 5. 右側 Diff 比較面板
  const selectedCandidate = allSlots.find(s => s.slotId === picker.selectedSlotId);

  const renderDiffMetric = (label, curVal, candVal) => {
    const cVal = curVal || 0;
    const nVal = candVal || 0;
    const diff = nVal - cVal;
    let diffBadge = '';
    if (diff > 0) {
      diffBadge = `<span style="color:#34d399;font-weight:bold;background:rgba(52,211,153,0.15);padding:2px 8px;border-radius:4px;border:1px solid #34d399;font-size:13px;">+${diff} ▲ (提升)</span>`;
    } else if (diff < 0) {
      diffBadge = `<span style="color:#f87171;font-weight:bold;background:rgba(239,68,68,0.15);padding:2px 8px;border-radius:4px;border:1px solid #ef4444;font-size:13px;">${diff} ▼ (降低)</span>`;
    } else {
      diffBadge = `<span style="color:#94a3b8;background:#1e293b;padding:2px 8px;border-radius:4px;font-size:13px;">持平</span>`;
    }

    return `
      <div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid #1e293b;font-size:13px;">
        <span style="color:#cbd5e1;">${label}</span>
        <div style="display:flex;align-items:center;gap:12px;">
          <span style="color:#94a3b8;">${cVal} ➔ <strong style="color:#f1f5f9;font-size:14px;">${nVal}</strong></span>
          ${diffBadge}
        </div>
      </div>
    `;
  };

  // 武器功法套路相容性分析
  let stanceCheckHtml = '';
  if (slotKey === 'MAIN_HAND') {
    const currentStance = m.basicSkillName || '基礎套路';
    let isStanceMatch = false;
    let candWeaponType = '兵刃';

    if (selectedCandidate) {
      const wName = selectedCandidate.name || '';
      const wSub = (selectedCandidate.subType || '').toUpperCase();
      if (wName.includes('劍') || wSub.includes('SWORD')) candWeaponType = '劍類';
      else if (wName.includes('刀') || wSub.includes('BLADE')) candWeaponType = '刀類';
      else if (wName.includes('拳') || wName.includes('掌') || wName.includes('爪') || wSub.includes('FIST')) candWeaponType = '拳掌類';
      else if (wName.includes('槍') || wName.includes('矛') || wSub.includes('SPEAR')) candWeaponType = '長槍類';
      else if (wName.includes('棍') || wName.includes('杖') || wSub.includes('STAFF')) candWeaponType = '棍杖類';

      // 檢查當前套路是否符合
      if (candWeaponType === '劍類' && (currentStance.includes('劍') || currentStance.includes('sword'))) isStanceMatch = true;
      else if (candWeaponType === '刀類' && (currentStance.includes('刀') || currentStance.includes('blade'))) isStanceMatch = true;
      else if (candWeaponType === '拳掌類' && (currentStance.includes('拳') || currentStance.includes('掌') || currentStance.includes('fist'))) isStanceMatch = true;
      else if (candWeaponType === '長槍類' && (currentStance.includes('槍') || currentStance.includes('spear'))) isStanceMatch = true;
      else if (candWeaponType === '棍杖類' && (currentStance.includes('棍') || currentStance.includes('杖') || currentStance.includes('staff'))) isStanceMatch = true;
      else if (currentStance.includes('基礎') || currentStance.includes('basic')) isStanceMatch = true;
    }

    if (selectedCandidate) {
      if (isStanceMatch) {
        stanceCheckHtml = `
          <div style="background:rgba(16,185,129,0.12);border:1px solid #10b981;border-radius:6px;padding:12px 14px;display:flex;flex-direction:column;gap:4px;">
            <div style="font-size:13px;font-weight:bold;color:#34d399;display:flex;align-items:center;gap:6px;">
              <span>✔ 武器功法契合：【${candWeaponType}】</span>
            </div>
            <div style="font-size:13px;color:#a7f3d0;line-height:1.4;">
              此武器與當前主力套路【${currentStance}】契合，普通攻擊與連攜招式可享全額增幅！
            </div>
          </div>
        `;
      } else {
        stanceCheckHtml = `
          <div style="background:rgba(245,158,11,0.12);border:1px solid #f59e0b;border-radius:6px;padding:12px 14px;display:flex;flex-direction:column;gap:4px;">
            <div style="font-size:13px;font-weight:bold;color:#fbbf24;display:flex;align-items:center;gap:6px;">
              <span>⚠️ 武器功法切換提示：【${candWeaponType}】</span>
            </div>
            <div style="font-size:13px;color:#fde68a;line-height:1.4;">
              當前主力套路為【${currentStance}】。裝備此武器後，建議前往「技能&法術」分頁啟用對應兵刃套路，以發揮最大威力。
            </div>
          </div>
        `;
      }
    }
  }

  let diffPanelHtml = '';
  if (!selectedCandidate) {
    diffPanelHtml = `
      <div style="display:flex;flex-direction:column;gap:16px;">
        <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:16px;color:#94a3b8;text-align:center;font-size:13px;">
          請從左側點選一件候選裝備進行屬性比較
        </div>
        ${curItem ? `
          <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:14px;display:flex;flex-direction:column;gap:8px;">
            <div style="font-size:14px;font-weight:bold;color:#cbd5e1;">當前穿戴中：</div>
            <div style="font-size:15px;font-weight:bold;color:#f1f5f9;">${curItem.icon || '📦'} ${curItem.name}</div>
            <button class="act-btn btn-sm btn-red" onclick="window.confirmUnequipItem('${slotAlias}', ${memberIdx})" style="font-size:13px;padding:6px 14px;margin-top:8px;">
              ✕ 卸下當前裝備
            </button>
          </div>
        ` : ''}
      </div>
    `;
  } else {
    diffPanelHtml = `
      <div style="display:flex;flex-direction:column;gap:12px;">
        <!-- 當前 vs 候選頂部卡片對比 -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
          <!-- 當前 -->
          <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:10px 12px;display:flex;flex-direction:column;gap:4px;">
            <span style="font-size:13px;color:#94a3b8;font-weight:bold;">【當前穿戴】</span>
            <span style="font-size:14px;font-weight:bold;color:${curItem ? '#f1f5f9' : '#64748b'};overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
              ${curItem ? `${curItem.icon || '📦'} ${curItem.name}` : '(未穿戴任何裝備)'}
            </span>
          </div>
          <!-- 候選 -->
          <div style="background:#1e293b;border:1px solid #38bdf8;border-radius:6px;padding:10px 12px;display:flex;flex-direction:column;gap:4px;">
            <span style="font-size:13px;color:#38bdf8;font-weight:bold;">【候選更換】</span>
            <span style="font-size:14px;font-weight:bold;color:#f1f5f9;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
              ${selectedCandidate.icon || '📦'} ${selectedCandidate.name}
            </span>
          </div>
        </div>

        <!-- 數值 Diff 條列 -->
        <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:10px 14px;">
          <div style="font-size:14px;font-weight:bold;color:#cbd5e1;margin-bottom:6px;">📊 屬性變化比較：</div>
          ${renderDiffMetric('物理攻擊 (最小)', curItem?.bonusMinDamage, selectedCandidate.bonusMinDamage)}
          ${renderDiffMetric('物理攻擊 (最大)', curItem?.bonusMaxDamage, selectedCandidate.bonusMaxDamage)}
          ${renderDiffMetric('護甲防禦', curItem?.bonusDefense, selectedCandidate.bonusDefense)}
          ${renderDiffMetric('氣血加成', curItem?.bonusHp, selectedCandidate.bonusHp)}
          ${renderDiffMetric('道心增幅', curItem?.bonusSan, selectedCandidate.bonusSan)}
        </div>

        <!-- 功法契合性提示 -->
        ${stanceCheckHtml}

        <!-- 物品描述與特殊道韻 -->
        ${(selectedCandidate.description || selectedCandidate.grantedSkillName) ? `
          <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:10px 14px;font-size:13px;color:#cbd5e1;line-height:1.4;">
            <div style="font-weight:bold;color:#94a3b8;margin-bottom:2px;">📜 法寶靈韻：</div>
            ${selectedCandidate.grantedSkillName ? `<div style="color:#fde047;font-weight:bold;margin-bottom:2px;">⚡ 附帶奧義：【${selectedCandidate.grantedSkillName}】</div>` : ''}
            <div>${selectedCandidate.description || ''}</div>
          </div>
        ` : ''}

        <!-- 操作按鈕列 -->
        <div style="display:flex;gap:10px;margin-top:6px;">
          <button class="act-btn btn-blue" onclick="window.confirmEquipItem('${selectedCandidate.slotId}', ${memberIdx})" style="flex:1;font-size:14px;font-weight:bold;padding:10px 16px;">
            ✨ 即刻確認穿戴
          </button>
          ${curItem ? `
            <button class="act-btn btn-red" onclick="window.confirmUnequipItem('${slotAlias}', ${memberIdx})" style="font-size:13px;padding:10px 14px;">
              ✕ 卸下當前
            </button>
          ` : ''}
        </div>
      </div>
    `;
  }

  const paginationBar = getRenderPaginationBar()(curPage, totalPages, candidates.length, 'window.changeEquipPickerPage');

  return `
    <div class="equip-diff-view">
      <!-- 左側候選列表 -->
      <div class="equip-candidate-list-col">
        <div style="font-size:14px;font-weight:bold;color:#e2e8f0;margin-bottom:10px;display:flex;justify-content:space-between;align-items:center;">
          <span>🎒 行囊中可穿戴的【${slotLabel}】(${candidates.length})</span>
          <span style="font-size:13px;color:#94a3b8;">每頁上限 20 筆</span>
        </div>
        <div class="equip-candidate-grid">
          ${candidateListHtml}
        </div>
        ${paginationBar}
      </div>

      <!-- 右側 Diff 比較面板 -->
      <div class="equip-diff-panel-col">
        <div style="font-size:15px;font-weight:bold;color:#38bdf8;border-bottom:1px solid #334155;padding-bottom:8px;">
          ⚖️ 法寶數值比較與功法相容性
        </div>
        ${diffPanelHtml}
      </div>
    </div>
  `;
}
