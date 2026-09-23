import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';

/**
 * 存讀檔管理與引導彈窗組件 (Save Modal & Entry Flow Component)
 * 負責單機 5+1 存檔管理、暗黑仙俠封面、開闢新途設定、序章沉浸導讀與玩法秘錄
 */

const drpgState = store.getState();
const send = (cmd, silent) => sendCmd(cmd, silent);

/**
 * 開啟存檔 / 讀檔面板
 */
function openSaveModal(mode) {
  drpgState.isSaveModalOpen = true;
  drpgState.saveModalMode = mode || 'load';
  const modal = document.getElementById('save-modal');
  const title = document.getElementById('save-modal-mode-title');
  if (modal) modal.classList.remove('hidden');
  if (title) {
    title.innerText = (mode === 'save') ? '💾 仙道命冊・選擇存檔槽位 (覆蓋進度)' : '📂 仙道命冊・讀取存檔 / 開闢新道途';
  }
  // 主動向後端查詢最新存檔 (靜默模式，不印出 ASCII 表格)
  send('saves quiet', true);
  renderSaveSlots();
}

/**
 * 關閉存檔面板
 */
function closeSaveModal() {
  drpgState.isSaveModalOpen = false;
  const modal = document.getElementById('save-modal');
  if (modal) modal.classList.add('hidden');
}

/**
 * 接收後端推送的 SAVE_SLOTS 陣列
 */
function updateSaveSlotsView(slots) {
  drpgState.saveSlots = slots || [];

  // 解析最新存檔 (優先排除空存檔)
  const populated = drpgState.saveSlots.filter(s => !s.empty);
  if (populated.length > 0) {
    populated.sort((a, b) => {
      const timeA = a.savedAt || '';
      const timeB = b.savedAt || '';
      return timeB.localeCompare(timeA);
    });
    drpgState.latestSaveSlot = populated[0];
  } else {
    drpgState.latestSaveSlot = null;
  }

  updateContinueButtonLabel();

  if (drpgState.isSaveModalOpen) {
    renderSaveSlots();
  }
}

/**
 * 渲染 6 個存檔卡片 (Slot 0 為自動存檔，Slot 1~5 為自訂手動存檔)
 */
function renderSaveSlots() {
  const container = document.getElementById('save-slots-list');
  if (!container) return;

  container.innerHTML = '';
  const slots = drpgState.saveSlots && drpgState.saveSlots.length > 0
      ? drpgState.saveSlots
      : [0, 1, 2, 3, 4, 5].map(id => ({ slotId: id, empty: true, title: id === 0 ? '自動存檔' : `存檔槽位 ${id}` }));

  slots.forEach(slot => {
    const isAuto = (slot.slotId === 0);
    const card = document.createElement('div');
    card.className = `slot-item-card ${isAuto ? 'is-autosave' : ''} ${slot.empty ? 'is-empty' : 'is-populated'}`;

    const infoCol = document.createElement('div');
    infoCol.className = 'slot-info-col';

    const badgeRow = document.createElement('div');
    badgeRow.className = 'slot-badge-row';
    const tag = document.createElement('span');
    tag.className = `slot-id-tag ${isAuto ? 'auto-tag' : ''}`;
    tag.innerText = isAuto ? '⚡ 自動存檔' : `槽位 ${slot.slotId}`;
    badgeRow.appendChild(tag);

    const titleSpan = document.createElement('span');
    titleSpan.className = 'slot-title-text';
    titleSpan.innerText = slot.title || (isAuto ? '自動存檔' : `存檔槽位 ${slot.slotId}`);
    badgeRow.appendChild(titleSpan);
    infoCol.appendChild(badgeRow);

    const metaRow = document.createElement('div');
    metaRow.className = 'slot-meta-row';
    if (slot.empty) {
      metaRow.innerHTML = '<span>-- 空無道痕 (未存檔) --</span>';
    } else {
      metaRow.innerHTML = `<span>👤 主角: <strong>${slot.protagonistName || '無名'}</strong></span>`
          + `<span>🏛️ <strong>${slot.floorName || '太陰古塚'}</strong></span>`
          + `<span>☯️ <strong>${slot.formationName || '四象辟邪陣'}</strong></span>`
          + `<span>🕒 <strong>${slot.savedAt || ''}</strong></span>`;
    }
    infoCol.appendChild(metaRow);

    if (!slot.empty && slot.memberNames && slot.memberNames.length > 0) {
      const membersRow = document.createElement('div');
      membersRow.className = 'slot-members-row';
      membersRow.innerText = '👥 小隊隊容：' + slot.memberNames.join('、');
      infoCol.appendChild(membersRow);
    }
    card.appendChild(infoCol);

    const actionsCol = document.createElement('div');
    actionsCol.className = 'slot-actions-col';

    if (slot.empty) {
      if (!isAuto) {
        const saveBtn = document.createElement('button');
        saveBtn.className = 'slot-btn slot-btn-save';
        saveBtn.innerText = '💾 存檔於此';
        saveBtn.onclick = () => triggerSaveSlot(slot.slotId);
        actionsCol.appendChild(saveBtn);
      }
    } else {
      // 讀取按鈕 (自動存檔與一般存檔均可讀取)
      const loadBtn = document.createElement('button');
      loadBtn.className = 'slot-btn slot-btn-load';
      loadBtn.innerText = isAuto ? '📂 載入自動存檔' : '📂 載入此檔';
      loadBtn.onclick = () => triggerLoadSlot(slot.slotId);
      actionsCol.appendChild(loadBtn);

      if (!isAuto) {
        // 覆蓋存檔按鈕
        const saveBtn = document.createElement('button');
        saveBtn.className = 'slot-btn slot-btn-save';
        saveBtn.innerText = '💾 覆蓋存檔';
        saveBtn.onclick = () => triggerSaveSlot(slot.slotId);
        actionsCol.appendChild(saveBtn);

        // 刪除按鈕
        const delBtn = document.createElement('button');
        delBtn.className = 'slot-btn slot-btn-del';
        delBtn.innerText = '🗑️';
        delBtn.title = '刪除此存檔';
        delBtn.onclick = () => triggerDeleteSlot(slot.slotId);
        actionsCol.appendChild(delBtn);
      }
    }

    card.appendChild(actionsCol);
    container.appendChild(card);
  });
}

function triggerSaveSlot(slotId) {
  const defaultName = (drpgState.lastParty && drpgState.lastParty.members && drpgState.lastParty.members[0])
      ? drpgState.lastParty.members[0].name + '的太陰道途'
      : '';
  const title = prompt('請輸入存檔標題 (可直接按確定使用預設)：', defaultName);
  if (title !== null) {
    const cmd = title.trim() ? `save ${slotId} ${title.trim()}` : `save ${slotId}`;
    send(cmd);
  }
}

function triggerLoadSlot(slotId) {
  send(`load ${slotId}`);
  closeSaveModal();
  enterGameWorld();
}

function triggerDeleteSlot(slotId) {
  if (confirm(`確定要抹除【存檔槽位 ${slotId}】的冒險記錄嗎？`)) {
    send(`save del ${slotId}`);
  }
}

function triggerNewGame() {
  const input = document.getElementById('new-protagonist-input');
  const name = (input && input.value.trim()) ? input.value.trim() : '玄靈子';
  send(`new ${name}`);
  if (input) input.value = '';
  closeSaveModal();
  enterGameWorld();
}

/* ==========================================================================
   三階段進入遊戲流程控制器 (Stage 1: 封面 -> Stage 2: 模式/序幕 -> Stage 3: 靈境)
   ========================================================================== */

/**
 * 開啟主標題封面 (Title Screen)
 */
function openTitleScreen() {
  drpgState.isTitleScreenOpen = true;
  const overlay = document.getElementById('title-screen-overlay');
  if (overlay) overlay.classList.remove('hidden');
  updateContinueButtonLabel();
  if (typeof send === 'function') {
    send('saves quiet', true);
  }
}

/**
 * 關閉主標題封面
 */
function closeTitleScreen() {
  drpgState.isTitleScreenOpen = false;
  const overlay = document.getElementById('title-screen-overlay');
  if (overlay) overlay.classList.add('hidden');
}

/**
 * 正式進入遊戲世界 (階段三)
 */
function enterGameWorld() {
  drpgState.hasEnteredGameWorld = true;
  closeTitleScreen();
  closeNewGameModal();
  closePrologueModal();
  closeGuideModal();
  closeSaveModal();
  updateContinueButtonLabel();
}

/**
 * 繼續最近存檔 (階段一快捷進入)
 */
function continueLatestSave() {
  if (drpgState.hasEnteredGameWorld) {
    enterGameWorld();
    return;
  }
  if (drpgState.latestSaveSlot && !drpgState.latestSaveSlot.empty) {
    send(`load ${drpgState.latestSaveSlot.slotId}`);
    enterGameWorld();
  } else {
    openNewGameModal();
  }
}

/**
 * 開啟開闢新途視窗 (階段二)
 */
function openNewGameModal() {
  drpgState.isNewGameModalOpen = true;
  const modal = document.getElementById('new-game-modal');
  if (modal) modal.classList.remove('hidden');
  const input = document.getElementById('new-game-protagonist-input');
  if (input && !input.value) {
    input.value = '玄靈子';
  }
  selectFormation(drpgState.selectedFormation || 'formation_four_symbols');
}

/**
 * 關閉開闢新途視窗
 */
function closeNewGameModal() {
  drpgState.isNewGameModalOpen = false;
  const modal = document.getElementById('new-game-modal');
  if (modal) modal.classList.add('hidden');
}

/**
 * 挑選初始陣法
 */
function selectFormation(formationId) {
  drpgState.selectedFormation = formationId;
  const cardFour = document.getElementById('card-four-symbols');
  const cardXuan = document.getElementById('card-xuan-yin');
  if (cardFour && cardXuan) {
    if (formationId === 'formation_xuan_yin') {
      cardXuan.classList.add('selected');
      cardFour.classList.remove('selected');
    } else {
      cardFour.classList.add('selected');
      cardXuan.classList.remove('selected');
    }
  }
}

/**
 * 點擊「踏入命運 ‧ 啟程」，進入序章故事 (階段二 -> 序幕)
 */
function startPrologueFlow() {
  const input = document.getElementById('new-game-protagonist-input');
  const name = (input && input.value.trim()) ? input.value.trim() : '玄靈子';
  closeNewGameModal();
  openPrologueModal();
}

/**
 * 開啟序章故事導讀
 */
function openPrologueModal() {
  drpgState.isPrologueModalOpen = true;
  const modal = document.getElementById('prologue-modal');
  if (modal) modal.classList.remove('hidden');
}

/**
 * 關閉序章故事導讀
 */
function closePrologueModal() {
  drpgState.isPrologueModalOpen = false;
  const modal = document.getElementById('prologue-modal');
  if (modal) modal.classList.add('hidden');
}

/**
 * 完成序章導讀 / 點擊跳過，發送開局指令並正式踏入古塚 (階段二 -> 階段三)
 */
function finishPrologueAndEnter() {
  const input = document.getElementById('new-game-protagonist-input');
  const name = (input && input.value.trim()) ? input.value.trim() : '玄靈子';
  const formationId = drpgState.selectedFormation || 'formation_four_symbols';
  send(`new ${name} ${formationId}`);
  enterGameWorld();
}

/**
 * 開啟太陰秘錄 (遊戲指南)
 */
function openGuideModal() {
  drpgState.isGuideModalOpen = true;
  const modal = document.getElementById('guide-modal');
  if (modal) modal.classList.remove('hidden');
}

/**
 * 關閉太陰秘錄
 */
function closeGuideModal() {
  drpgState.isGuideModalOpen = false;
  const modal = document.getElementById('guide-modal');
  if (modal) modal.classList.add('hidden');
}

/**
 * 動態更新封面「繼續冒險」按鈕的提示文字與狀態
 */
function updateContinueButtonLabel() {
  const continueBtn = document.getElementById('btn-continue-game');
  const continueHint = document.getElementById('continue-slot-hint');
  if (!continueBtn) return;

  if (drpgState.hasEnteredGameWorld) {
    continueBtn.disabled = false;
    const mainLabel = continueBtn.querySelector('.btn-main-label');
    if (mainLabel) mainLabel.innerText = '返回靈境探索';
    if (continueHint) {
      const charName = (drpgState.lastParty && drpgState.lastParty.members && drpgState.lastParty.members[0])
          ? drpgState.lastParty.members[0].name
          : '玄靈子';
      continueHint.innerText = `目前進度：${charName} (古塚靈境)`;
    }
  } else if (drpgState.latestSaveSlot && !drpgState.latestSaveSlot.empty) {
    continueBtn.disabled = false;
    const mainLabel = continueBtn.querySelector('.btn-main-label');
    if (mainLabel) mainLabel.innerText = '繼續冒險';
    if (continueHint) {
      const slotTag = drpgState.latestSaveSlot.slotId === 0 ? '⚡自動存檔' : `槽位${drpgState.latestSaveSlot.slotId}`;
      continueHint.innerText = `${slotTag}：${drpgState.latestSaveSlot.protagonistName || '無名'} (${drpgState.latestSaveSlot.floorName || '太陰古塚'})`;
    }
  } else {
    continueBtn.disabled = true;
    const mainLabel = continueBtn.querySelector('.btn-main-label');
    if (mainLabel) mainLabel.innerText = '繼續冒險';
    if (continueHint) {
      continueHint.innerText = '尚無修仙道痕 (請開闢新途)';
    }
  }
}



// 相容掛載至 window 供 HTML 內聯事件呼叫
if (typeof window !== 'undefined') {
  window.openSaveModal = openSaveModal;
  window.closeSaveModal = closeSaveModal;
  window.updateSaveSlotsView = updateSaveSlotsView;
  window.renderSaveSlots = renderSaveSlots;
  window.triggerSaveSlot = triggerSaveSlot;
  window.triggerLoadSlot = triggerLoadSlot;
  window.triggerDeleteSlot = triggerDeleteSlot;
  window.triggerNewGame = triggerNewGame;
  window.openTitleScreen = openTitleScreen;
  window.closeTitleScreen = closeTitleScreen;
  window.enterGameWorld = enterGameWorld;
  window.continueLatestSave = continueLatestSave;
  window.openNewGameModal = openNewGameModal;
  window.closeNewGameModal = closeNewGameModal;
  window.selectFormation = selectFormation;
  window.startPrologueFlow = startPrologueFlow;
  window.openPrologueModal = openPrologueModal;
  window.closePrologueModal = closePrologueModal;
  window.finishPrologueAndEnter = finishPrologueAndEnter;
  window.openGuideModal = openGuideModal;
  window.closeGuideModal = closeGuideModal;
  window.updateContinueButtonLabel = updateContinueButtonLabel;
}

export {
  openSaveModal,
  closeSaveModal,
  updateSaveSlotsView,
  renderSaveSlots,
  triggerSaveSlot,
  triggerLoadSlot,
  triggerDeleteSlot,
  triggerNewGame,
  openTitleScreen,
  closeTitleScreen,
  enterGameWorld,
  continueLatestSave,
  openNewGameModal,
  closeNewGameModal,
  selectFormation,
  startPrologueFlow,
  openPrologueModal,
  closePrologueModal,
  finishPrologueAndEnter,
  openGuideModal,
  closeGuideModal,
  updateContinueButtonLabel
};
