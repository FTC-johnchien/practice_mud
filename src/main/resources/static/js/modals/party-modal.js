import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';
import { escapeHtml } from '../core/ui-utils.js';

// 子模組垂直切片引入 (FE-01 Modularization)
import {
  toggleAddTacticsForm,
  handleTacticsCondChange,
  submitAddTactics,
  renderMemberTactics
} from './party-tactics-tab.js';

import {
  formatFormationClassName,
  toggleFormationViewMode,
  setFormationFilterSize,
  changeFormationPage,
  renderFormationLibraryView,
  renderTeamFormationView
} from './party-formation-tab.js';

import {
  EQUIP_SLOT_DEFS,
  getMemberSlotItem,
  isItemMatchingSlot,
  getPassiveSkillDisplayName,
  setItemsSecondaryFilter,
  changeItemsPage,
  renderItemsListHtml,
  renderMemberStatusDetailHtml,
  renderMemberEquipHtml,
  openEquipPicker,
  closeEquipPicker,
  selectEquipPickerItem,
  changeEquipPickerPage,
  confirmEquipItem,
  confirmUnequipItem,
  renderMemberEquipmentView,
  renderTeamEquipmentOverview,
  renderEquipmentDiffPickerView
} from './party-equipment-tab.js';

import {
  setSkillsTab,
  changeSkillsPage,
  openSkillPicker,
  closeSkillPicker,
  selectSkillPickerItem,
  changeSkillPickerPage,
  confirmEquipSkill,
  confirmUnequipSkill,
  renderMemberSkillsView,
  renderSkillPickerView,
  renderMemberSpellbook,
  switchSpellbookTab,
  switchSpellbookPage
} from './party-skills-tab.js';

/**
 * 角色裝備、武學法術書與戰術方針彈窗 (Party Modal Coordinator)
 * 負責外層模態視窗生命週期、導航標籤協調、系統存檔路由與全隊名冊管理
 */

const drpgState = store.getState();
const send = (cmd, silent) => sendCmd(cmd, silent);

export function triggerPartyAction() {
  const list = document.getElementById('party-members-list');
  if (list) {
    list.classList.remove('party-pulse');
    void list.offsetWidth;
    list.classList.add('party-pulse');
  }
  togglePartyModal();
  send('party');
}

/**
 * 開啟 / 關閉小隊 5+2 裝備與隊伍編制管理面板
 */
export function togglePartyModal(forceOpen) {
  const modal = document.getElementById('party-modal');
  if (!modal) return;

  if (forceOpen === undefined) {
    drpgState.isPartyModalOpen = !drpgState.isPartyModalOpen;
  } else {
    drpgState.isPartyModalOpen = !!forceOpen;
  }

  if (drpgState.isPartyModalOpen) {
    modal.classList.remove('hidden');
    renderPartyModal();
  } else {
    modal.classList.add('hidden');
    drpgState.equipPicker = null;
    drpgState.skillPicker = null;
    if (document.activeElement) {
      document.activeElement.blur();
    }
    // 父子視窗生命週期聯動：關閉狀態主視窗時，一併關閉由此開啟的子視窗 (如公共行囊)
    if (window.toggleBagDrawer && store.get('isBagDrawerOpen')) {
      window.toggleBagDrawer(false);
    }
  }
}

export function openPartyModal(memberIdx) {
  if (typeof memberIdx === 'number') {
    drpgState.selectedModalMemberIdx = memberIdx;
  }
  togglePartyModal(true);
}

export function closePartyModal() {
  if (drpgState.equipPicker) {
    drpgState.equipPicker = null;
    renderPartyModal();
    return;
  }
  if (drpgState.skillPicker) {
    drpgState.skillPicker = null;
    renderPartyModal();
    return;
  }
  togglePartyModal(false);
  if (document.activeElement) {
    document.activeElement.blur();
  }
}

export function selectPartyModalMember(idx) {
  drpgState.selectedModalMemberIdx = idx;
  drpgState.isAddingTactics = false;
  renderPartyModal();
}

export function selectPartyFormationTab() {
  drpgState.selectedModalMemberIdx = 'FORMATION';
  drpgState.isAddingTactics = false;
  renderPartyModal();
}

export function switchPartyModalSubTab(tab) {
  drpgState.partyModalSubTab = tab;
  drpgState.isAddingTactics = false;
  renderPartyModal();
}

export function switchMainMenuTab(tab, subOption) {
  drpgState.mainMenuTab = tab || 'FORMATION';
  drpgState.equipPicker = null;
  drpgState.skillPicker = null;
  if (subOption) {
    if (tab === 'SYSTEM') drpgState.mainMenuSystemMode = subOption;
    if (tab === 'FORMATION') drpgState.formationViewMode = subOption;
    if (tab === 'ITEMS') drpgState.mainMenuItemsTab = subOption;
  }
  if (tab === 'SYSTEM') {
    send('saves quiet', true);
  }
  renderPartyModal();
}

export function openMainMenu(tab, subOption) {
  drpgState.equipPicker = null;
  drpgState.skillPicker = null;
  if (tab) {
    drpgState.mainMenuTab = tab;
  }
  if (subOption) {
    if (tab === 'SYSTEM') drpgState.mainMenuSystemMode = subOption;
    if (tab === 'FORMATION') drpgState.formationViewMode = subOption;
    if (tab === 'ITEMS') drpgState.mainMenuItemsTab = subOption;
  }
  if (tab === 'SYSTEM') {
    send('saves quiet', true);
  }
  togglePartyModal(true);
}

/**
 * 通用 20 筆單頁分頁元件 (Pagination Component)
 */
export function renderPaginationBar(currentPage, totalPages, totalCount, onPageChangeFnName) {
  if (totalCount === 0) return '';
  const safeCurrent = Math.max(1, currentPage);
  const safeTotal = Math.max(1, totalPages);

  return `
    <div class="menu-pagination-bar" style="display:flex;justify-content:space-between;align-items:center;padding:10px 16px;background:#0b1120;border:1px solid #1e293b;border-radius:6px;margin-top:auto;">
      <div style="font-size:13px;color:#94a3b8;">
        共 <strong style="color:#e2e8f0;font-size:14px;">${totalCount}</strong> 筆項目 ‧ 每頁上限 20 筆
      </div>
      <div style="display:flex;align-items:center;gap:12px;">
        <button class="act-btn btn-sm" onclick="${onPageChangeFnName}(-1)" ${safeCurrent <= 1 ? 'disabled style="opacity:0.35;cursor:not-allowed;"' : ''} style="font-size:13px;padding:4px 12px;">
          ◀ 上一頁
        </button>
        <span style="font-size:13px;color:#cbd5e1;font-weight:bold;">
          第 ${safeCurrent} / ${safeTotal} 頁
        </span>
        <button class="act-btn btn-sm" onclick="${onPageChangeFnName}(1)" ${safeCurrent >= safeTotal ? 'disabled style="opacity:0.35;cursor:not-allowed;"' : ''} style="font-size:13px;padding:4px 12px;">
          下一頁 ▶
        </button>
      </div>
    </div>
  `;
}

/**
 * 渲染系統存讀檔槽位清單 (System View)
 */
export function renderSystemSlotsHtml(sysMode) {
  const slots = drpgState.saveSlots && drpgState.saveSlots.length > 0
    ? drpgState.saveSlots
    : [0, 1, 2, 3, 4, 5].map(id => ({ slotId: id, empty: true, title: id === 0 ? '自動存檔' : `存檔槽位 ${id}` }));

  return `
    <div style="margin-bottom:12px;display:flex;justify-content:space-between;align-items:center;background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px 16px;">
      <div>
        <span style="font-size:14px;color:#cbd5e1;font-weight:bold;">⚙️ 系統存讀檔與遊戲控制</span>
        <span style="font-size:13px;color:#94a3b8;margin-left:8px;">(可在此儲存/載入進度，或返回標題畫面)</span>
      </div>
      <button class="act-btn btn-primary" onclick="if(window.openTitleScreen) { if(window.closePartyModal) window.closePartyModal(); window.openTitleScreen(); }" style="font-size:13px;padding:6px 14px;font-weight:bold;cursor:pointer;">🏠 返回遊戲主封面</button>
    </div>
    <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(360px, 1fr));gap:14px;padding:4px 0;">
      ${slots.map(slot => {
        const isAuto = (slot.slotId === 0);
        return `
          <div class="slot-item-card ${isAuto ? 'is-autosave' : ''} ${slot.empty ? 'is-empty' : 'is-populated'}" style="margin:0;font-size:13px;">
            <div class="slot-info-col">
              <div class="slot-badge-row">
                <span class="slot-id-tag ${isAuto ? 'auto-tag' : ''}">${isAuto ? '⚡ 自動存檔' : `槽位 ${slot.slotId}`}</span>
                <span class="slot-title-text" style="font-size:14px;font-weight:bold;">${escapeHtml(slot.title || (isAuto ? '自動存檔' : `存檔槽位 ${slot.slotId}`))}</span>
              </div>
              <div class="slot-meta-row" style="font-size:13px;margin:6px 0;">
                ${slot.empty
                  ? '<span style="color:#64748b;">-- 空無道痕 (未存檔) --</span>'
                  : `<span>👤 主角: <strong>${escapeHtml(slot.protagonistName || '無名')}</strong></span>
                     <span>🏛️ <strong>${escapeHtml(slot.floorName || '太陰古塚')}</strong></span>
                     <span>☯️ <strong>${escapeHtml(slot.formationName || '四象辟邪陣')}</strong></span>
                     <span>🕒 <strong>${escapeHtml(slot.savedAt || '')}</strong></span>`}
              </div>
              ${(!slot.empty && slot.memberNames && slot.memberNames.length > 0)
                ? `<div class="slot-members-row" style="font-size:var(--font-xs);color:#94a3b8;">👥 小隊隊容：${slot.memberNames.map(n => escapeHtml(n)).join('、')}</div>`
                : ''}
            </div>
            <div class="slot-actions-col">
              ${slot.empty
                ? (!isAuto ? `<button class="slot-btn slot-btn-save" onclick="window.triggerSaveSlot(${slot.slotId})" style="font-size:13px;">💾 存檔於此</button>` : '<span style="color:#64748b;font-size:var(--font-xs);">待觸發</span>')
                : `
                  <button class="slot-btn slot-btn-load" onclick="window.triggerLoadSlot(${slot.slotId})" style="font-size:13px;">${isAuto ? '📂 載入自動存檔' : '📂 載入此檔'}</button>
                  ${!isAuto ? `
                    <button class="slot-btn slot-btn-save" onclick="window.triggerSaveSlot(${slot.slotId})" style="font-size:13px;">💾 覆蓋存檔</button>
                    <button class="slot-btn slot-btn-del" onclick="window.triggerDeleteSlot(${slot.slotId})" title="刪除此存檔" style="font-size:13px;">🗑️</button>
                  ` : ''}
                `}
            </div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

/**
 * 渲染隊伍名冊調整畫面 (Party Roster View)
 */
export function renderPartyRosterHtml(party) {
  const members = party.members || [];
  if (members.length === 0) {
    return '<div style="color:#94a3b8;padding:20px;text-align:center;">小隊尚無任何成員。</div>';
  }

  return `
    <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;">
      <div style="margin-bottom:14px;font-size:14px;color:#cbd5e1;">
        當前小隊編制 (${members.length} / 5 人)。點擊【▲ 上移】或【▼ 下移】調整隊員先發順位：
      </div>
      <div style="display:flex;flex-direction:column;gap:10px;">
        ${members.map((mem, idx) => {
          const isLeader = (idx === 0);
          const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
          return `
            <div style="display:flex;justify-content:space-between;align-items:center;background:#0f172a;border:1px solid #334155;border-radius:6px;padding:12px 18px;">
              <div style="display:flex;align-items:center;gap:16px;">
                <span style="font-size:18px;font-weight:bold;color:${isLeader ? '#fde047' : '#38bdf8'};width:36px;">#${idx + 1}</span>
                <div>
                  <div style="font-size:15px;font-weight:bold;color:#f1f5f9;display:flex;align-items:center;gap:8px;">
                    ${isLeader ? '👑' : '👤'} ${mem.name}
                    <span style="font-size:13px;color:#94a3b8;font-weight:normal;">(${mem.roleTitle || '道友'})</span>
                    ${!isAlive ? '<span style="font-size:var(--font-xs);color:#ef4444;font-weight:bold;background:#450a0a;padding:2px 8px;border-radius:4px;">💀 陣亡</span>' : ''}
                  </div>
                  <div style="font-size:13px;color:#64748b;margin-top:4px;">
                    境界 Lv.${mem.level || 1} ‧ 氣血 ${mem.hp}/${mem.maxHp} ‧ 真元/戰氣 ${mem.mp || 0}/${mem.maxMp || 0}
                  </div>
                </div>
              </div>
              <div style="display:flex;align-items:center;gap:10px;">
                <button class="act-btn btn-sm" onclick="send('party swap ${idx} ${idx - 1}')" ${idx === 0 ? 'disabled style="opacity:0.35;cursor:not-allowed;"' : ''} style="font-size:13px;padding:5px 12px;">
                  ▲ 上移
                </button>
                <button class="act-btn btn-sm" onclick="send('party swap ${idx} ${idx + 1}')" ${idx === members.length - 1 ? 'disabled style="opacity:0.35;cursor:not-allowed;"' : ''} style="font-size:13px;padding:5px 12px;">
                  ▼ 下移
                </button>
              </div>
            </div>
          `;
        }).join('')}
      </div>
    </div>
  `;
}

export function renderPartyModal() {
  const modal = document.getElementById('party-modal');
  if (!modal || modal.classList.contains('hidden')) return;

  const party = drpgState.lastParty;
  const subHeaderEl = document.getElementById('main-menu-sub-header');
  const contentEl = document.getElementById('main-menu-content');
  const titleTextEl = document.getElementById('main-menu-title-text');

  if (!party || !party.members || party.members.length === 0) {
    if (contentEl) contentEl.innerHTML = '<div style="color:#94a3b8;padding:40px;text-align:center;font-size:14px;">尚未載入小隊資料，請稍候...</div>';
    return;
  }

  // 1. 初始化當前選中的主選單 Tab
  if (!drpgState.mainMenuTab) {
    drpgState.mainMenuTab = 'FORMATION';
  }
  const currentTab = drpgState.mainMenuTab;

  // 2. 更新左側導覽列按鈕高亮
  const navBtns = document.querySelectorAll('#main-menu-nav .main-menu-nav-btn');
  navBtns.forEach(btn => {
    if (btn.getAttribute('data-menu') === currentTab) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  // 3. 確保選中隊員索引有效
  if (typeof drpgState.selectedModalMemberIdx !== 'number' || drpgState.selectedModalMemberIdx >= party.members.length || drpgState.selectedModalMemberIdx < 0) {
    drpgState.selectedModalMemberIdx = 0;
  }
  const selIdx = drpgState.selectedModalMemberIdx;
  const m = party.members[selIdx] || party.members[0];

  // 4. 更新頂部標題
  const titleMap = {
    'ITEMS': '🎒 仙道總覽・公共行囊與道具 (Items)',
    'EQUIP': '🛡️ 仙道總覽・全隊 5+2 裝備與被動功法 (Equipment)',
    'SKILLS': '📖 仙道總覽・武學法術典籍 (Skills & Spells)',
    'CHARACTERS': '👤 仙道總覽・角色詳細道基面板 (Character Status)',
    'PARTY': '👥 仙道總覽・隊伍名冊編號順序 (Party Roster)',
    'FORMATION': '☯️ 仙道總覽・道門陣法與 5×3 戰陣盤 (Formations)',
    'TACTICS': '🎯 仙道總覽・同伴戰術方針 (Gambit AI)',
    'SYSTEM': '💾 仙道總覽・仙道命冊 (System Save / Load)'
  };
  if (titleTextEl) {
    titleTextEl.innerText = titleMap[currentTab] || '📜 仙道總覽・功能選單';
  }

  // 輔助函式：渲染隊員切換頁籤組 (字體嚴格保持 13~14px)
  const renderMemberTabButtons = () => {
    return party.members.map((mem, idx) => {
      const isSel = (selIdx === idx);
      return `
        <button class="party-member-tab-btn ${isSel ? 'active' : ''}" type="button" onclick="selectPartyModalMember(${idx})"
          style="font-size:13px;padding:6px 12px;" title="#${idx + 1} ${mem.name} (${mem.className || '道友'})">
          <span style="font-weight:bold;">#${idx + 1} ${mem.name}</span>
          ${(idx === 0 && mem.freeStatPoints > 0) ? `<span class="hud-free-points-pill" style="margin-left:4px; font-size:var(--font-xs); padding:1px 5px;">+${mem.freeStatPoints}點</span>` : ''}
        </button>
      `;
    }).join('');
  };

  // 5. 依據 currentTab 路由渲染次級頁首與內容視口
  switch (currentTab) {
    case 'FORMATION': {
      const isLibrary = (drpgState.formationViewMode === 'LIBRARY');
      const isFormActive = Boolean(
        party.formationActive !== false &&
        party.formationName &&
        party.formationName !== '無' &&
        !party.formationName.includes('已崩解') &&
        !party.formationName.includes('未生效')
      );
      const formColor = isFormActive ? '#38bdf8' : '#f87171';
      const ultBtn = (party.canCastUltimate && isFormActive)
        ? `<button class="act-btn btn-ult" onclick="send('formation cast')" style="padding:4px 12px;font-size:13px;">⚡ 施展陣法奧義【${party.ultimateSkillName}】</button>`
        : (isFormActive
            ? `<span style="color:#94a3b8;font-size:13px;">奧義【${party.ultimateSkillName || '無'}】(充能 ${party.formationEnergy || 0}/100)</span>`
            : `<span style="color:#f87171;font-size:13px;font-weight:bold;">⚠️ 陣法未生效</span>`);

      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="act-btn ${!isLibrary ? 'btn-blue' : ''}" onclick="window.toggleFormationViewMode('BOARD')" style="font-size:13px;padding:5px 14px;">☯️ 5×3 戰陣盤</button>
            <button class="act-btn ${isLibrary ? 'btn-blue' : ''}" onclick="window.toggleFormationViewMode('LIBRARY')" style="font-size:13px;padding:5px 14px;">📚 陣法典籍庫</button>
          </div>
          <div style="display:flex;align-items:center;gap:12px;font-size:13px;color:#cbd5e1;">
            <span>當前道門陣法：<strong style="color:${formColor};">${party.formationName || '五行混元陣'}</strong></span>
            <span>⚡ 靈威：${party.formationEnergy || 0}/100</span>
            ${ultBtn}
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = isLibrary ? renderFormationLibraryView(party) : renderTeamFormationView(party);
      }
      break;
    }

    case 'SYSTEM': {
      const sysMode = drpgState.mainMenuSystemMode || 'load';
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="act-btn ${sysMode === 'load' ? 'btn-blue' : ''}" onclick="window.switchMainMenuTab('SYSTEM', 'load')" style="font-size:13px;padding:5px 14px;">📂 讀取存檔</button>
            <button class="act-btn ${sysMode === 'save' ? 'btn-blue' : ''}" onclick="window.switchMainMenuTab('SYSTEM', 'save')" style="font-size:13px;padding:5px 14px;">💾 覆蓋存檔</button>
          </div>
          <div style="font-size:13px;color:#94a3b8;">
            💡 支援 1 個即時自動存檔與 5 個手動命冊存檔槽位 (按 F5 快捷存檔)
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderSystemSlotsHtml(sysMode);
      }
      break;
    }

    case 'TACTICS': {
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:8px;overflow-x:auto;">
            ${renderMemberTabButtons()}
          </div>
          <div style="font-size:13px;color:#94a3b8;">
            🎯 FFXII Gambit 規則鏈：自上而下匹配並執行第一條符合條件的技能
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderMemberTactics(m, selIdx);
      }
      break;
    }

    case 'SKILLS': {
      const skillTab = drpgState.skillsTab || 'ACTIVE';
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:8px;overflow-x:auto;">
            ${renderMemberTabButtons()}
          </div>
          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
            <button class="act-btn btn-sm ${skillTab === 'FIELD_USABLE' ? 'btn-blue' : ''}" onclick="window.setSkillsTab('FIELD_USABLE')" style="font-size:13px;padding:4px 12px;">🌿 可使用 (探索/回復)</button>
            <button class="act-btn btn-sm ${skillTab === 'ACTIVE' ? 'btn-blue' : ''}" onclick="window.setSkillsTab('ACTIVE')" style="font-size:13px;padding:4px 12px;">⚡ 主動絕技</button>
            <button class="act-btn btn-sm ${skillTab === 'ALL' ? 'btn-blue' : ''}" onclick="window.setSkillsTab('ALL')" style="font-size:13px;padding:4px 12px;">📖 全部道法</button>
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderMemberSkillsView(m, selIdx, party, skillTab);
      }
      break;
    }

    case 'CHARACTERS': {
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:8px;overflow-x:auto;">
            ${renderMemberTabButtons()}
          </div>
          <div style="font-size:13px;color:#94a3b8;">
            👤 角色道基天賦、五維六道與實時氣血狀態
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderMemberStatusDetailHtml(m, selIdx, party);
      }
      break;
    }

    case 'PARTY': {
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="font-size:14px;font-weight:bold;color:#f1f5f9;">
            👥 隊伍出戰名冊順序編號調整
          </div>
          <div style="font-size:13px;color:#94a3b8;">
            💡 提示：調整名冊僅改變出戰順序，與 5×3 戰陣盤上的站位座標完全解耦
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderPartyRosterHtml(party);
      }
      break;
    }

    case 'ITEMS': {
      const itemTab = drpgState.mainMenuItemsTab || 'USABLE';
      const secFilter = drpgState.itemsSecondaryFilter || 'ALL';
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="act-btn ${itemTab === 'USABLE' ? 'btn-blue' : ''}" onclick="window.switchMainMenuTab('ITEMS', 'USABLE')" style="font-size:13px;padding:5px 14px;">🧪 可使用 (回復/Buff)</button>
            <button class="act-btn ${itemTab === 'QUEST' ? 'btn-blue' : ''}" onclick="window.switchMainMenuTab('ITEMS', 'QUEST')" style="font-size:13px;padding:5px 14px;">📜 任務道具</button>
            <button class="act-btn ${itemTab === 'ALL' ? 'btn-blue' : ''}" onclick="window.switchMainMenuTab('ITEMS', 'ALL')" style="font-size:13px;padding:5px 14px;">📦 全部道具</button>
          </div>
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="act-btn" onclick="toggleBagDrawer(true)" style="font-size:13px;padding:5px 12px;background:#334155;" title="展開右側獨立行囊抽屜">🎒 側欄行囊 (B)</button>
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderItemsListHtml(itemTab, secFilter, party);
      }
      break;
    }

    case 'EQUIP':
    default: {
      if (drpgState.equipPicker) {
        const pMemIdx = drpgState.equipPicker.memberIdx;
        const pMem = (party.members && party.members[pMemIdx]) ? party.members[pMemIdx] : m;
        if (subHeaderEl) {
          subHeaderEl.innerHTML = `
            <div style="display:flex;align-items:center;gap:12px;">
              <button class="act-btn btn-sm" onclick="window.closeEquipPicker()" style="font-size:13px;padding:4px 12px;background:#334155;">
                ◀ 返回裝備頁面
              </button>
              <span style="font-size:15px;font-weight:bold;color:#f1f5f9;">
                🛡️ 裝備挑選與 Diff 比較 - #${pMemIdx + 1} ${pMem ? pMem.name : ''} 的【${drpgState.equipPicker.slotLabel}】
              </span>
            </div>
            <div style="font-size:13px;color:#94a3b8;">
              挑選合適法寶，比較攻防加成與武器功法相容性
            </div>
          `;
        }
        if (contentEl) {
          contentEl.innerHTML = renderEquipmentDiffPickerView(party);
        }
      } else if (drpgState.skillPicker) {
        const pMemIdx = drpgState.skillPicker.memberIdx;
        const pMem = (party.members && party.members[pMemIdx]) ? party.members[pMemIdx] : m;
        if (subHeaderEl) {
          subHeaderEl.innerHTML = `
            <div style="display:flex;align-items:center;gap:12px;">
              <button class="act-btn btn-sm" onclick="window.closeSkillPicker()" style="font-size:13px;padding:4px 12px;background:#334155;">
                ◀ 返回裝備頁面
              </button>
              <span style="font-size:15px;font-weight:bold;color:#f1f5f9;">
                🧘 功法配置與挑選 - #${pMemIdx + 1} ${pMem ? pMem.name : ''} 的【${drpgState.skillPicker.categoryLabel}】
              </span>
            </div>
            <div style="font-size:13px;color:#94a3b8;">
              挑選合適的主修套路或常駐心法；未裝配時自動以角色基礎武學作為預設值
            </div>
          `;
        }
        if (contentEl) {
          contentEl.innerHTML = renderSkillPickerView(party);
        }
      } else {
        if (subHeaderEl) {
          subHeaderEl.innerHTML = `
            <div style="display:flex;align-items:center;gap:8px;overflow-x:auto;">
              ${renderMemberTabButtons()}
            </div>
            <div style="display:flex;align-items:center;gap:12px;">
              <span style="font-size:13px;color:#94a3b8;">💡 點擊槽位可挑選裝備或功法</span>
              <button class="act-btn btn-sm" onclick="toggleBagDrawer(true)" style="font-size:13px;padding:3px 10px;background:#1e293b;" title="開啟右側獨立行囊抽屜">🎒 側欄行囊 (B)</button>
            </div>
          `;
        }
        if (contentEl) {
          contentEl.innerHTML = renderMemberEquipmentView(m, selIdx, party);
        }
      }
      break;
    }
  }
}

// 相容掛載至 window 供 HTML 內聯事件呼叫
if (typeof window !== 'undefined') {
  window.triggerPartyAction = triggerPartyAction;
  window.togglePartyModal = togglePartyModal;
  window.openPartyModal = openPartyModal;
  window.closePartyModal = closePartyModal;
  window.switchMainMenuTab = switchMainMenuTab;
  window.openMainMenu = openMainMenu;
  window.selectPartyModalMember = selectPartyModalMember;
  window.selectPartyFormationTab = selectPartyFormationTab;
  window.switchPartyModalSubTab = switchPartyModalSubTab;
  window.toggleAddTacticsForm = toggleAddTacticsForm;
  window.handleTacticsCondChange = handleTacticsCondChange;
  window.submitAddTactics = submitAddTactics;
  window.renderMemberTactics = renderMemberTactics;
  window.renderTeamFormationView = renderTeamFormationView;
  window.renderPartyModal = renderPartyModal;
  window.renderPaginationBar = renderPaginationBar;
  window.renderMemberSpellbook = renderMemberSpellbook;
  window.renderMemberSkillsView = renderMemberSkillsView;
  window.switchSpellbookTab = switchSpellbookTab;
  window.switchSpellbookPage = switchSpellbookPage;
  window.toggleFormationViewMode = toggleFormationViewMode;
  window.setFormationFilterSize = setFormationFilterSize;
  window.changeFormationPage = changeFormationPage;
  window.changeItemsPage = changeItemsPage;
  window.setItemsSecondaryFilter = setItemsSecondaryFilter;
  window.changeSkillsPage = changeSkillsPage;
  window.setSkillsTab = setSkillsTab;
  window.openEquipPicker = openEquipPicker;
  window.closeEquipPicker = closeEquipPicker;
  window.selectEquipPickerItem = selectEquipPickerItem;
  window.changeEquipPickerPage = changeEquipPickerPage;
  window.confirmEquipItem = confirmEquipItem;
  window.confirmUnequipItem = confirmUnequipItem;
  window.renderMemberEquipmentView = renderMemberEquipmentView;
  window.renderTeamEquipmentOverview = renderTeamEquipmentOverview;
  window.renderEquipmentDiffPickerView = renderEquipmentDiffPickerView;
  window.openSkillPicker = openSkillPicker;
  window.closeSkillPicker = closeSkillPicker;
  window.selectSkillPickerItem = selectSkillPickerItem;
  window.changeSkillPickerPage = changeSkillPickerPage;
  window.confirmEquipSkill = confirmEquipSkill;
  window.confirmUnequipSkill = confirmUnequipSkill;
  window.renderSkillPickerView = renderSkillPickerView;
}

// 重新匯出所有模組函式，維持既有 import 介面 100% 向後相容
export {
  // Tactics
  toggleAddTacticsForm,
  handleTacticsCondChange,
  submitAddTactics,
  renderMemberTactics,
  // Formation
  formatFormationClassName,
  toggleFormationViewMode,
  setFormationFilterSize,
  changeFormationPage,
  renderFormationLibraryView,
  renderTeamFormationView,
  // Equipment & Items
  EQUIP_SLOT_DEFS,
  getMemberSlotItem,
  isItemMatchingSlot,
  getPassiveSkillDisplayName,
  setItemsSecondaryFilter,
  changeItemsPage,
  renderItemsListHtml,
  renderMemberStatusDetailHtml,
  renderMemberEquipHtml,
  openEquipPicker,
  closeEquipPicker,
  selectEquipPickerItem,
  changeEquipPickerPage,
  confirmEquipItem,
  confirmUnequipItem,
  renderMemberEquipmentView,
  renderTeamEquipmentOverview,
  renderEquipmentDiffPickerView,
  // Skills & Spellbook
  setSkillsTab,
  changeSkillsPage,
  openSkillPicker,
  closeSkillPicker,
  selectSkillPickerItem,
  changeSkillPickerPage,
  confirmEquipSkill,
  confirmUnequipSkill,
  renderMemberSkillsView,
  renderSkillPickerView,
  renderMemberSpellbook,
  switchSpellbookTab,
  switchSpellbookPage
};

