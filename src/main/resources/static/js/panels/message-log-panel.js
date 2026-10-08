import { MAX_LOG_HISTORY_LINES } from '../core/constants.js';
import { escapeHtml } from '../core/ui-utils.js';

/**
 * 訊息日誌區塊面板 (Message Log Panel)
 * 負責 ANSI 色碼文字轉譯、即時滾動日誌、最長行數記憶體防護
 */

const MAX_LOG_LINES = MAX_LOG_HISTORY_LINES || 200;

export function getLogContainer() {
  return document.getElementById('log');
}

let ansiUp = null;
function getAnsiConverter() {
  if (ansiUp) return ansiUp;
  if (typeof window !== 'undefined') {
    if (window.ansi_up && typeof window.ansi_up.ansi_to_html === 'function') {
      ansiUp = window.ansi_up;
      return ansiUp;
    }
    const Cls = window.AnsiUp || (typeof AnsiUp !== 'undefined' ? AnsiUp : null);
    if (Cls && typeof Cls === 'function') {
      ansiUp = new Cls();
      ansiUp.use_classes = false;
      window.ansi_up = ansiUp;
      return ansiUp;
    }
  }
  return null;
}

const colorMap = {
  '30': '#1e293b', '31': '#ef4444', '32': '#22c55e', '33': '#eab308',
  '34': '#3b82f6', '35': '#a855f7', '36': '#06b6d4', '37': '#f8fafc',
  '1;30': '#64748b', '1;31': '#f87171', '1;32': '#4ade80', '1;33': '#fde047',
  '1;34': '#60a5fa', '1;35': '#c084fc', '1;36': '#38bdf8', '1;37': '#ffffff'
};

function parseAnsiText(text) {
  if (!text) return '';
  // 先對潛在的惡意 HTML 標籤進行轉義，防止 Log XSS
  const safeText = escapeHtml(text);
  const conv = getAnsiConverter();
  if (conv && text.includes('\u001B')) {
    return conv.ansi_to_html(safeText);
  }
  // 容錯支援 \u001B 色碼或直接呈現的 [1;36m 格式
  let html = safeText
    .replace(/\u001B\[0m/g, '</span>')
    .replace(/\u001B\[([0-9;]+)m/g, (match, code) => {
      const c = colorMap[code] || '#94a3b8';
      return `<span style="color:${c};font-weight:${code.startsWith('1;') ? 'bold' : 'normal'}">`;
    })
    .replace(/\[0m/g, '</span>')
    .replace(/\[([0-9;]{2,5})m/g, (match, code) => {
      if (colorMap[code]) {
        return `<span style="color:${colorMap[code]};font-weight:${code.startsWith('1;') ? 'bold' : 'normal'}">`;
      }
      return match;
    });
  return html;
}

// 智慧分析文本所屬分類 (戰鬥、劇情、系統)
function detectCategory(rawText) {
  if (!rawText) return 'system';
  if (/造成|受創|命中|暴擊|致命|閃避|招架|格擋|氣血|真元|絕學|陣法|劍氣|撕咬|爪擊|撲咬|施展|倒下|陣亡|戰鬥/i.test(rawText)) {
    return 'combat';
  }
  if (/說道|笑道|問道|嘆道|喃喃|掌櫃|客棧|福伯|老人|交談|對話|言道|低語|回覆/i.test(rawText)) {
    return 'story';
  }
  return 'system';
}

// 剝除 ANSI 與 HTML 標籤取得純文字
function stripTags(htmlOrAnsi) {
  if (!htmlOrAnsi) return '';
  return htmlOrAnsi
    .replace(/\u001B\[[0-9;]*m/g, '')
    .replace(/\[[0-9;]{1,5}m/g, '')
    .replace(/<[^>]*>/g, '')
    .trim();
}

let currentFilter = 'all';

export function updateTicker(plainText) {
  const tickerText = document.getElementById('hud-ticker-text');
  if (tickerText && plainText) {
    tickerText.innerText = plainText;
  }
}

export function appendLog(rawText, color, explicitCat) {
  if (!rawText) return;
  const logDiv = getLogContainer();
  if (!logDiv) return;

  const cat = explicitCat || detectCategory(rawText);
  const div = document.createElement('div');
  div.className = 'log-entry';
  div.dataset.category = cat;
  if (color) div.style.color = color;

  // 檢查是否符合當前過濾規則
  if (currentFilter !== 'all' && currentFilter !== cat) {
    div.classList.add('filtered-out');
  }

  try {
    div.innerHTML = parseAnsiText(rawText);
    logDiv.appendChild(div);
    logDiv.scrollTop = logDiv.scrollHeight;

    // 同步更新底部 HUD 跑馬燈
    const cleanText = stripTags(rawText);
    if (cleanText) {
      updateTicker(cleanText);
    }

    // 限制行數防止記憶體溢出
    if (logDiv.childNodes.length > MAX_LOG_LINES) {
      logDiv.removeChild(logDiv.firstChild);
    }
  } catch (err) {
    console.error('[MessageLogPanel] 渲染錯誤:', err);
  }
}

export function filterLog(category) {
  currentFilter = category || 'all';
  const logDiv = getLogContainer();
  if (!logDiv) return;

  // 更新所有按鈕 active 樣式
  const btns = document.querySelectorAll('.log-filter-btn');
  btns.forEach(btn => {
    if (btn.dataset.cat === currentFilter) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  // 逐筆條目切換過濾
  const entries = logDiv.querySelectorAll('.log-entry');
  entries.forEach(entry => {
    if (currentFilter === 'all' || entry.dataset.category === currentFilter) {
      entry.classList.remove('filtered-out');
    } else {
      entry.classList.add('filtered-out');
    }
  });
  logDiv.scrollTop = logDiv.scrollHeight;
}

export function toggleLogCollapse() {
  const viewport = document.querySelector('.main-viewport');
  if (!viewport) return;

  const isCollapsed = viewport.classList.toggle('log-collapsed');
  
  // 更新跑馬燈切換按鈕文字
  const tickerBtn = document.querySelector('.hud-event-ticker .ticker-toggle-btn');
  if (tickerBtn) {
    tickerBtn.innerText = isCollapsed ? '展開 ▴' : '收合 ▾';
  }

  // 儲存偏好至 localStorage
  try {
    localStorage.setItem('drpg_log_collapsed', isCollapsed ? 'true' : 'false');
  } catch (e) {
    // ignore
  }
}

export function clearLog() {
  const logDiv = getLogContainer();
  if (logDiv) {
    logDiv.innerHTML = '';
  }
}

// 初始化日誌狀態與事件綁定
export function initMessageLogControls() {
  // 恢復 localStorage 偏好設定
  try {
    const saved = localStorage.getItem('drpg_log_collapsed');
    if (saved === 'true') {
      const viewport = document.querySelector('.main-viewport');
      if (viewport) viewport.classList.add('log-collapsed');
      const tickerBtn = document.querySelector('.hud-event-ticker .ticker-toggle-btn');
      if (tickerBtn) tickerBtn.innerText = '展開 ▴';
    }
  } catch (e) {
    // ignore
  }
}

// 相容掛載至 window
if (typeof window !== 'undefined') {
  window.appendHtml = appendLog;
  window.filterLog = filterLog;
  window.toggleLogCollapse = toggleLogCollapse;
  window.clearLog = clearLog;
}

