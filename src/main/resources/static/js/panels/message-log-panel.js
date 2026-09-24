/**
 * 訊息日誌區塊面板 (Message Log Panel)
 * 負責 ANSI 色碼文字轉譯、即時滾動日誌、最長行數記憶體防護
 */

const MAX_LOG_LINES = 200;

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
  const conv = getAnsiConverter();
  if (conv && text.includes('\u001B')) {
    return conv.ansi_to_html(text);
  }
  // 容錯支援 \u001B 色碼或直接呈現的 [1;36m 格式
  let html = text
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

export function appendLog(rawText, color) {
  if (!rawText) return;
  const logDiv = getLogContainer();
  if (!logDiv) return;

  const div = document.createElement('div');
  if (color) div.style.color = color;

  try {
    div.innerHTML = parseAnsiText(rawText);
    logDiv.appendChild(div);
    logDiv.scrollTop = logDiv.scrollHeight;

    // 限制行數防止記憶體溢出
    if (logDiv.childNodes.length > MAX_LOG_LINES) {
      logDiv.removeChild(logDiv.firstChild);
    }
  } catch (err) {
    console.error('[MessageLogPanel] 渲染錯誤:', err);
  }
}

export function clearLog() {
  const logDiv = getLogContainer();
  if (logDiv) {
    logDiv.innerHTML = '';
  }
}

// 相容掛載至 window
if (typeof window !== 'undefined') {
  window.appendHtml = appendLog;
}
