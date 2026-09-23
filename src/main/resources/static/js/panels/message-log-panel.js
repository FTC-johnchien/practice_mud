/**
 * 訊息日誌區塊面板 (Message Log Panel)
 * 負責 ANSI 色碼文字轉譯、即時滾動日誌、最長行數記憶體防護
 */

const MAX_LOG_LINES = 200;

export function getLogContainer() {
  return document.getElementById('log');
}

export function appendLog(rawText, color) {
  if (!rawText) return;
  const logDiv = getLogContainer();
  if (!logDiv) return;

  const div = document.createElement('div');
  if (color) div.style.color = color;

  try {
    if (typeof window !== 'undefined' && window.ansi_up) {
      div.innerHTML = window.ansi_up.ansi_to_html(rawText);
    } else {
      div.innerText = rawText;
    }
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
