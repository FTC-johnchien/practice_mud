import { eventBus } from './event-bus.js';

/**
 * 前端全域狀態中心 (State Store)
 * 統一維護由後端 DRPG_STATE 驅動的狀態快照與本地 UI 狀態
 */
class StateStore {
  constructor() {
    this.state = {
      mode: 'TOWN',
      lastTown: null,
      isDevConsoleOpen: false,
      lastDungeon: null,
      lastParty: null,
      lastBattle: null,
      selectedMemberIdx: 0,
      isSkillDrawerOpen: false,
      isBagDrawerOpen: false,
      isInputFocused: false,
      lastStepTime: 0,
      radarPosition: localStorage.getItem('drpg_radar_pos') || 'left',
      radarMode: localStorage.getItem('drpg_radar_mode') || 'centered', // 'centered' 或 'full'
      saveSlots: [],
      isSaveModalOpen: false,
      saveModalMode: 'load',
      isPartyModalOpen: false,
      isShopModalOpen: false,
      lastShopCatalog: null,
      // 進入遊戲世界流程狀態 (三階段)
      isTitleScreenOpen: true,
      hasEnteredGameWorld: false,
      isNewGameModalOpen: false,
      isPrologueModalOpen: false,
      isGuideModalOpen: false,
      selectedFormation: 'formation_four_symbols',
      latestSaveSlot: null
    };
  }

  /**
   * 取得當前完整狀態副本或特定屬性
   */
  getState() {
    return this.state;
  }

  get(key) {
    return this.state[key];
  }

  /**
   * 更新狀態並派發變更事件
   * @param {Object} partial 新的狀態片段
   * @param {boolean} silent 是否靜默更新 (不觸發事件)
   */
  setState(partial, silent = false) {
    const changedKeys = [];
    for (const [key, value] of Object.entries(partial)) {
      if (this.state[key] !== value) {
        this.state[key] = value;
        changedKeys.push(key);
      }
    }

    if (!silent && changedKeys.length > 0) {
      eventBus.emit('state:changed', { state: this.state, changedKeys });
      for (const key of changedKeys) {
        eventBus.emit(`state:${key}`, this.state[key]);
      }
    }
  }

  /**
   * 監聽特定狀態屬性變化
   * @param {string} key 屬性名稱
   * @param {Function} handler 回呼
   */
  subscribe(key, handler) {
    return eventBus.on(`state:${key}`, handler);
  }
}

export const store = new StateStore();
// 掛載相容全域變數，供現有函式逐步過渡
if (typeof window !== 'undefined') {
  window.drpgStore = store;
}
