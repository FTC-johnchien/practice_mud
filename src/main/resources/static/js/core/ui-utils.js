/**
 * 前端通用純粹 UI 工具函式庫 (Pure UI Utilities)
 * 包含全域 DOM XSS 防禦轉義、資源百分比換算、進度條模板與樣式輔助函式。
 */

/**
 * 1. 全域 DOM XSS 防禦轉義 (P0 級安全核心)
 * 將 HTML 特殊字元替換為安全實體，杜絕外部不可信字串（如玩家名稱、怪物名、對話內容）注入。
 *
 * @param {any} str 原始輸入字串
 * @returns {string} 轉義後安全 HTML 字串
 */
export function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

/**
 * 2. 安全百分比換算 (避免除以零或溢出)
 *
 * @param {number} current 當前數值
 * @param {number} max 最大數值
 * @returns {number} 0 ~ 100 整數
 */
export function calculatePercentage(current, max) {
  const curVal = Number(current) || 0;
  const maxVal = Number(max) || 1;
  if (maxVal <= 0) return 0;
  return Math.min(100, Math.max(0, Math.round((curVal / maxVal) * 100)));
}

/**
 * 3. 資源進度條樣板產生器 (自動執行安全轉義)
 *
 * @param {number} current 當前數值
 * @param {number} max 最大數值
 * @param {string} type 資源類別 ('hp' | 'mp' | 'sp' | 'rage' | 'san')
 * @returns {string} 安全 HTML 模板
 */
export function createResourceBar(current, max, type = 'hp') {
  const safeType = escapeHtml(type);
  const pct = calculatePercentage(current, max);
  const safeCur = Number(current) || 0;
  const safeMax = Number(max) || 0;

  return `
    <div class="hud-bar-track hud-bar-${safeType}">
      <div class="hud-bar-fill" style="width: ${pct}%"></div>
      <span class="hud-bar-text">${safeCur}/${safeMax}</span>
    </div>
  `.trim();
}

/**
 * 4. 裝備/技能品質標籤 CSS 類別解析 (完全由後端 tag/quality 驅動)
 *
 * @param {string} quality 品質等級字串
 * @returns {string} 對應的 CSS 類別名稱
 */
export function getQualityClass(quality) {
  const q = String(quality || 'COMMON').toUpperCase();
  const map = {
    POOR: 'quality-gray',
    COMMON: 'quality-white',
    UNCOMMON: 'quality-green',
    RARE: 'quality-blue',
    EPIC: 'quality-purple',
    LEGENDARY: 'quality-orange',
    MYTHIC: 'quality-red'
  };
  return map[q] || 'quality-white';
}

/**
 * 5. 數值千分位格式化
 *
 * @param {number} num 數值
 * @returns {string} 格式化字串 (如 1,234,567)
 */
export function formatNumber(num) {
  const n = Number(num);
  if (isNaN(n)) return '0';
  return n.toLocaleString('en-US');
}

// 掛載至 window 提供向後相容
if (typeof window !== 'undefined') {
  window.escapeHtml = escapeHtml;
}

export const UiUtils = {
  escapeHtml,
  calculatePercentage,
  createResourceBar,
  getQualityClass,
  formatNumber
};
