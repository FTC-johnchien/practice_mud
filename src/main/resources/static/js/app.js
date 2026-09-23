/**
 * 主應用程式模組入口 (Application Entry Point)
 * 負責整合所有核心模組、面板與彈窗，路由伺服器狀態快照，並監聽全域快捷鍵
 */

import { eventBus } from './core/event-bus.js';
import { store } from './core/state-store.js';
import { sendCmd, sendStep, sendTownMove, handleDpad } from './core/cmd-dispatcher.js';

import { renderTownNav, normalizeTownCapability } from './panels/town-panel.js';
import { renderMinimap, applyRadarPosition, updateRadarModeBtn, toggleRadarPosition, toggleRadarViewMode } from './panels/dungeon-panel.js';
import { renderPartyHud, selectPartyMember } from './panels/party-hud-panel.js';
import { renderBattleArena, renderBattlePartyQuickBar, hideBattleArena, selectBattleTarget, toggleBattleMode, selectPartyMemberForSkill } from './panels/battle-panel.js';
import { appendLog, clearLog } from './panels/message-log-panel.js';

import { renderSkillDrawer, closeSkillDrawer, onSkillBtnClick, castPartySkill } from './modals/skill-drawer.js';
import { toggleBagDrawer, renderBagDrawer } from './modals/bag-drawer.js';
import {
  triggerPartyAction,
  togglePartyModal,
  openPartyModal,
  closePartyModal,
  selectPartyModalMember,
  selectPartyFormationTab,
  switchPartyModalSubTab,
  toggleAddTacticsForm,
  handleTacticsCondChange,
  submitAddTactics,
  renderMemberTactics,
  renderTeamFormationView,
  renderPartyModal,
  renderMemberSpellbook,
  switchSpellbookTab,
  switchSpellbookPage
} from './modals/party-modal.js';

import {
  openShopModal,
  toggleShopModal,
  renderShopCatalogInLog,
  adjustShopQty,
  triggerShopBuy
} from './modals/shop-modal.js';

import {
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
} from './modals/save-modal.js';

const drpgState = store.getState();

/**
 * 開發者終端控制台切換
 */
export function toggleDevConsole(forceState) {
  const modal = document.getElementById('dev-console-modal');
  const input = document.getElementById('dev-cmd-input');
  if (!modal) return;

  const current = store.get('isDevConsoleOpen');
  const target = (typeof forceState === 'boolean') ? forceState : !current;
  store.setState({ isDevConsoleOpen: target });

  if (target) {
    modal.classList.remove('hidden');
    if (input) {
      input.value = '';
      setTimeout(() => input.focus(), 50);
    }
  } else {
    modal.classList.add('hidden');
    if (input) input.blur();
  }
}

export function handleDevEnter() {
  const input = document.getElementById('dev-cmd-input');
  if (!input) return;
  const cmd = input.value.trim();
  if (cmd) {
    sendCmd(cmd);
    input.value = '';
  }
  toggleDevConsole(false);
}

/**
 * 快捷操作按鈕處理函式 (包含即時 CSS 視覺反饋)
 */
export function triggerMapAction() {
  const grid = document.getElementById('drpg-grid');
  if (grid) {
    grid.classList.remove('radar-pulse');
    void grid.offsetWidth;
    grid.classList.add('radar-pulse');
  }
  sendCmd('map');
}

export function triggerFormationAction() {
  const bar = document.querySelector('.formation-status-wrap');
  if (bar) {
    bar.classList.remove('radar-pulse');
    void bar.offsetWidth;
    bar.classList.add('radar-pulse');
  }
  sendCmd('formation toggle');
}

export function triggerInspectAction() {
  const inspect = document.getElementById('drpg-forward-inspect');
  if (inspect) {
    inspect.classList.remove('inspect-pulse');
    void inspect.offsetWidth;
    inspect.classList.add('inspect-pulse');
  }
  sendCmd('dungeon look');
}

export function triggerRestAction() {
  sendCmd('rest');
}

/**
 * 接收後端推送的 DRPG_STATE 結構化資料，路由派發至相應面板
 * @param {Object} payload 狀態快照
 */
export function updateDrpgView(payload) {
  if (!payload) return;

  const mode = payload.mode || (payload.dungeon ? 'DUNGEON' : 'TOWN');
  store.setState({ mode });

  const dungeonPanel = document.getElementById('dungeon-radar-panel');
  const townPanel = document.getElementById('town-nav-panel');
  const battlePanel = document.getElementById('battle-arena-panel');

  const inBattle = Boolean(payload.battle && payload.battle.inBattle);

  if (inBattle) {
    if (dungeonPanel) dungeonPanel.classList.add('hidden');
    if (townPanel) townPanel.classList.add('hidden');
    if (battlePanel) battlePanel.classList.remove('hidden');
    store.setState({ lastBattle: payload.battle });
    renderBattleArena(payload.battle);
    toggleBattleMode(true);
  } else {
    if (battlePanel) battlePanel.classList.add('hidden');
    store.setState({ lastBattle: null });
    toggleBattleMode(false);
    if (document.activeElement && (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA' || document.activeElement.tagName === 'BUTTON')) {
      document.activeElement.blur();
    }

    if (mode === 'TOWN') {
      if (dungeonPanel) dungeonPanel.classList.add('hidden');
      if (townPanel) townPanel.classList.remove('hidden');
      if (payload.town) {
        store.setState({ lastTown: payload.town });
        renderTownNav(payload.town);
      }
    } else {
      if (townPanel) townPanel.classList.add('hidden');
      if (dungeonPanel) dungeonPanel.classList.remove('hidden');
      if (payload.dungeon) {
        store.setState({ lastDungeon: payload.dungeon });
        renderMinimap(payload.dungeon);
      }
    }
  }

  if (payload.party) {
    store.setState({ lastParty: payload.party });
    renderPartyHud(payload.party);
    if (inBattle) {
      renderBattlePartyQuickBar(payload.party);
    }
    if (store.get('isBagDrawerOpen')) {
      renderBagDrawer();
    }
    if (store.get('isPartyModalOpen')) {
      renderPartyModal();
    }
  }
}

/**
 * 初始化全局鍵盤監聽事件 (WASD、探索/戰鬥快捷鍵)
 */
export function initKeyboardControls() {
  const inputEl = document.getElementById('cmd-input');
  const modeBadge = document.getElementById('control-mode-badge');

  function updateModeBadge(inInput) {
    store.setState({ isInputFocused: inInput });
    const lastBattle = store.get('lastBattle');
    if (lastBattle && lastBattle.inBattle) {
      toggleBattleMode(true);
      return;
    }
    if (modeBadge) {
      if (inInput) {
        modeBadge.className = 'mode-badge mode-typing';
        modeBadge.innerText = '⌨️ 文字輸入中 (按 Esc 或 Enter 恢復步進)';
      } else {
        modeBadge.className = 'mode-badge mode-exploring';
        modeBadge.innerText = '🎮 步進探索模式 (按 W/A/S/D 移動，按 / 或 Enter 輸入指令)';
      }
    }
  }

  if (inputEl) {
    inputEl.addEventListener('focus', () => updateModeBadge(true));
    inputEl.addEventListener('blur', () => updateModeBadge(false));
  }

  // 點擊非輸入框區域自動釋放文字焦點，確保 WASD 隨時可用
  document.addEventListener('pointerdown', (e) => {
    if (!e.target.closest('input, textarea, select, #dev-console-modal')) {
      if (document.activeElement && (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA')) {
        document.activeElement.blur();
        updateModeBadge(false);
      }
    }
  });

  window.addEventListener('keydown', (e) => {
    // 0. 若目前正在任何文字輸入控制項或下拉選單中
    const activeEl = document.activeElement;
    const devInputEl = document.getElementById('dev-cmd-input');
    const newNameInput = document.getElementById('new-game-protagonist-input');

    if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA' || activeEl.tagName === 'SELECT')) {
      if (store.get('isDevConsoleOpen') || activeEl === devInputEl) {
        if (e.key === 'Escape' || e.key === '`' || e.key === '~') {
          e.preventDefault();
          toggleDevConsole(false);
        } else if (e.key === 'Enter') {
          e.preventDefault();
          handleDevEnter();
        }
        return;
      }
      if (activeEl === inputEl) {
        if (e.key === 'Escape') {
          inputEl.blur();
          updateModeBadge(false);
        }
        return;
      }
      if (newNameInput && activeEl === newNameInput) {
        if (e.key === 'Enter') {
          e.preventDefault();
          startPrologueFlow();
        } else if (e.key === 'Escape') {
          closeNewGameModal();
        }
        return;
      }
      if (e.key === 'Escape') {
        activeEl.blur();
      }
      return; // 正在其他輸入項 (如貨棧數量、方針數值) 打字時，不觸發 WASD 移動或功能鍵
    }

    // 1. 若在主封面或任一模態視窗中，攔截快捷鍵防止穿透，並支援 Esc 依序返回關閉
    const isAnyModalOrTitleOpen = store.get('isTitleScreenOpen') ||
      store.get('isSaveModalOpen') ||
      store.get('isNewGameModalOpen') ||
      store.get('isPrologueModalOpen') ||
      store.get('isGuideModalOpen') ||
      store.get('isPartyModalOpen') ||
      store.get('isShopModalOpen');

    if (isAnyModalOrTitleOpen) {
      if (e.key === 'Escape') {
        if (store.get('isShopModalOpen')) {
          toggleShopModal(false);
          return;
        }
        if (store.get('isPartyModalOpen')) {
          closePartyModal();
          return;
        }
        if (store.get('isPrologueModalOpen')) {
          closePrologueModal();
          return;
        }
        if (store.get('isNewGameModalOpen')) {
          closeNewGameModal();
          return;
        }
        if (store.get('isGuideModalOpen')) {
          closeGuideModal();
          return;
        }
        if (store.get('isSaveModalOpen')) {
          closeSaveModal();
          return;
        }
        if (store.get('isTitleScreenOpen') && store.get('hasEnteredGameWorld')) {
          enterGameWorld();
          return;
        }
      }
      return; // 阻止在選單/封面/彈窗中按 WASD 造成背景地圖移動
    }

    // 快捷鍵 ~ 開啟/關閉開發者終端
    if (e.key === '`' || e.key === '~') {
      e.preventDefault();
      toggleDevConsole();
      return;
    }

    const key = e.key.toLowerCase();

    // 3. 快捷鍵轉至文字指令輸入 (開啟開發者終端)
    if (key === '/' || e.key === 'Enter') {
      e.preventDefault();
      toggleDevConsole(true);
      return;
    }

    // 4. 戰鬥中與非戰鬥共通快捷鍵
    if (e.key === 'Escape') {
      if (store.get('isDevConsoleOpen')) {
        toggleDevConsole(false);
        return;
      }
      if (store.get('isShopModalOpen')) {
        toggleShopModal(false);
        return;
      }
      if (store.get('isPartyModalOpen')) {
        closePartyModal();
        return;
      }
      if (store.get('isSaveModalOpen')) {
        closeSaveModal();
        return;
      }
      if (store.get('isBagDrawerOpen')) {
        toggleBagDrawer(false);
        return;
      }
      if (store.get('isSkillDrawerOpen')) {
        closeSkillDrawer();
        return;
      }
      const lastBattle = store.get('lastBattle');
      if (lastBattle && lastBattle.inBattle) {
        sendCmd('battle flee');
        return;
      }
    }

    if (e.key === 'F5') {
      e.preventDefault();
      openSaveModal('save');
      return;
    }

    // 行囊快捷鍵 B (戰鬥中與非戰鬥均可開啟)
    if (key === 'b') {
      e.preventDefault();
      toggleBagDrawer();
      return;
    }

    // 5. 數字鍵 1~6 快捷選取隊員展開技能盤
    if (['1', '2', '3', '4', '5', '6'].includes(key)) {
      e.preventDefault();
      const idx = parseInt(key) - 1;
      selectPartyMember(idx);
      return;
    }

    // 6. 戰鬥中快捷鍵
    const lastBattle = store.get('lastBattle');
    if (lastBattle && lastBattle.inBattle) {
      if (key === ' ' || key === 'Spacebar' || e.code === 'Space') {
        e.preventDefault();
        sendCmd('battle fight');
      } else if (key === 'u') {
        e.preventDefault();
        sendCmd('formation cast');
      } else if (key === 't') {
        e.preventDefault();
        sendCmd('battle target 0');
      } else if (key === 'p' || key === 'c') {
        e.preventDefault();
        triggerPartyAction();
      }
      return;
    }

    // 7. 探索模式步進與快捷功能 (支援城鎮出口與地牢步進自適應)
    if (key === 'w' || e.key === 'ArrowUp') {
      e.preventDefault();
      handleDpad('north');
    } else if (key === 's' || e.key === 'ArrowDown') {
      e.preventDefault();
      handleDpad('south');
    } else if (key === 'a' || e.key === 'ArrowLeft') {
      e.preventDefault();
      handleDpad('west');
    } else if (key === 'd' || e.key === 'ArrowRight') {
      e.preventDefault();
      handleDpad('east');
    } else if (key === 'm') {
      e.preventDefault();
      triggerMapAction();
    } else if (key === 'p' || key === 'c') {
      e.preventDefault();
      triggerPartyAction();
    } else if (key === 'f') {
      e.preventDefault();
      triggerFormationAction();
    } else if (key === 'i' || key === 'l') {
      e.preventDefault();
      triggerInspectAction();
    } else if (key === 'r') {
      e.preventDefault();
      triggerRestAction();
    } else if (key === 'u') {
      e.preventDefault();
      sendCmd('formation cast');
    }
  });

  updateModeBadge(false);
}

// 頁面載入完成後初始化與全域函式掛載 (維持 100% 向後相容)
if (typeof window !== 'undefined') {
  window.updateDrpgView = updateDrpgView;
  window.toggleDevConsole = toggleDevConsole;
  window.handleDevEnter = handleDevEnter;
  window.triggerMapAction = triggerMapAction;
  window.triggerPartyAction = triggerPartyAction;
  window.triggerFormationAction = triggerFormationAction;
  window.triggerInspectAction = triggerInspectAction;
  window.triggerRestAction = triggerRestAction;
  window.initKeyboardControls = initKeyboardControls;
}

window.addEventListener('DOMContentLoaded', () => {
  initKeyboardControls();
  applyRadarPosition();
  updateRadarModeBtn();
  setTimeout(() => {
    if (typeof window.send === 'function' && (!store.get('saveSlots') || store.get('saveSlots').length === 0)) {
      window.send('saves quiet', true);
    }
  }, 300);
});
