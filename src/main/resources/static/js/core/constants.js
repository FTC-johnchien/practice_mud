/**
 * 前端全域常數中樞 (Global UI Constants)
 * 統一集中管理所有尺寸下限、日誌容量、分頁設定、快捷鍵與資源主題樣式。
 * 遵循「禁止前端硬編碼散落」的架構鐵律。
 */

export const UI_CONSTANTS = Object.freeze({
  // 字級防禦下限 (杜絕小於 12px 擠壓破版)
  MIN_FONT_SIZE_PX: 13,

  // 日誌最大留存行數 (防止 DOM 節點無限增長)
  MAX_LOG_HISTORY_LINES: 200,

  // 主選單 DQ/FF 規格
  MENU: {
    MAX_ITEMS_PER_PAGE: 20,
    BASE_WIDTH: 1600,
    BASE_HEIGHT: 900
  },

  // 全域快捷鍵定義
  KEYS: {
    MENU: ['c', 'C', 'p', 'P'],
    BAG: ['b', 'B'],
    INSPECT: ['i', 'I'],
    REST: ['r', 'R'],
    CONSOLE: ['`', '~'],
    ESCAPE: ['Escape'],
    SPACE: [' ']
  },

  // 戰鬥資源主題 (與後端 CombatResourceType 對齊)
  RESOURCE_THEMES: {
    HP: { label: '氣血', cssClass: 'resource-hp', color: 'var(--color-resource-hp, #ef4444)' },
    MP: { label: '真元', cssClass: 'resource-mp', color: 'var(--color-resource-mp, #3b82f6)' },
    SP: { label: '戰氣', cssClass: 'resource-sp', color: 'var(--color-resource-sp, #10b981)' },
    RAGE: { label: '怒氣', cssClass: 'resource-rage', color: 'var(--color-resource-rage, #f59e0b)' },
    COMBO: { label: '連擊', cssClass: 'resource-combo', color: 'var(--color-resource-combo, #8b5cf6)' },
    SAN: { label: '道心', cssClass: 'resource-san', color: 'var(--color-resource-san, #06b6d4)' }
  }
});
