/**
 * 前端全域常數中樞 (Global UI Constants)
 * 統一集中管理所有尺寸下限、日誌容量、分頁設定、快捷鍵與資源主題樣式。
 * 遵循「禁止前端硬編碼散落」的架構鐵律。
 */

export const MIN_FONT_SIZE_PX = 13;
export const MAX_LOG_HISTORY_LINES = 200;
export const MENU = Object.freeze({
  MAX_ITEMS_PER_PAGE: 20,
  BASE_WIDTH: 1600,
  BASE_HEIGHT: 900
});
export const KEYS = Object.freeze({
  MENU: ['c', 'C', 'p', 'P'],
  BAG: ['b', 'B'],
  INSPECT: ['i', 'I'],
  REST: ['r', 'R'],
  CONSOLE: ['`', '~'],
  ESCAPE: ['Escape'],
  SPACE: [' ']
});
export const RESOURCE_THEMES = Object.freeze({
  HP: { label: '氣血', cssClass: 'resource-hp', color: 'var(--color-resource-hp, #ef4444)' },
  MP: { label: '真元', cssClass: 'resource-mp', color: 'var(--color-resource-mp, #3b82f6)' },
  SP: { label: '戰氣', cssClass: 'resource-sp', color: 'var(--color-resource-sp, #10b981)' },
  RAGE: { label: '怒氣', cssClass: 'resource-rage', color: 'var(--color-resource-rage, #f59e0b)' },
  COMBO: { label: '連擊', cssClass: 'resource-combo', color: 'var(--color-resource-combo, #8b5cf6)' },
  SAN: { label: '道心', cssClass: 'resource-san', color: 'var(--color-resource-san, #06b6d4)' }
});

export const UI_CONSTANTS = Object.freeze({
  MIN_FONT_SIZE_PX,
  MAX_LOG_HISTORY_LINES,
  MENU,
  KEYS,
  RESOURCE_THEMES
});
