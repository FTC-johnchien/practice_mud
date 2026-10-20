/**
 * WebSocket 通訊與斷線重連管線 (WebSocket Network Pipeline)
 * 負責維護前後端全雙工長連線、指數退避重連、指令序列化發送與伺服端訊息轉發
 * 遵循架構決策二：本機單機環境除役高頻文字 Ping 保活，專注於事件驅動與斷線平滑恢復
 */

let ws = null;
let reconnectTimer = null;
let currentRetryDelay = 1000;
const MIN_RETRY_DELAY = 1000;   // 初始重試延遲：1 秒
const MAX_RETRY_DELAY = 15000;  // 最大重試延遲：15 秒
let isExplicitlyClosed = false;
let hasConnectedBefore = false;

/**
 * 輔助方法：輸出系統日誌至前端終端
 * 相容 window.appendHtml 與 console
 */
function logSystemMessage(msg, color) {
  if (typeof window !== 'undefined' && typeof window.appendHtml === 'function') {
    window.appendHtml(msg, color);
  } else if (typeof console !== 'undefined') {
    console.log(msg);
  }
}

/**
 * 建立並初始化 WebSocket 連線
 * @param {Object} [options] 自訂選項 (如 url)
 * @returns {WebSocket|null} WebSocket 實例
 */
export function connectWebSocket(options = {}) {
  // 若已有連線中或連線已建立的實例，直接返回
  if (ws && (ws.readyState === WebSocket.CONNECTING || ws.readyState === WebSocket.OPEN)) {
    return ws;
  }

  isExplicitlyClosed = false;

  let socketUrl = options.url;
  if (!socketUrl && typeof window !== 'undefined') {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host || 'localhost:8080';
    socketUrl = `${protocol}//${host}/ws`;
  }

  try {
    ws = new WebSocket(socketUrl);
  } catch (err) {
    logSystemMessage(`系統：連線初始化失敗 (${err.message})`, '#ef4444');
    scheduleReconnect();
    return null;
  }

  ws.onopen = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    currentRetryDelay = MIN_RETRY_DELAY;

    if (!hasConnectedBefore) {
      logSystemMessage("系統：神識已連結至太陰地宮！", "#10b981");
      hasConnectedBefore = true;
    } else {
      logSystemMessage("系統：靈識重新凝結，神識已平滑恢復連線！", "#10b981");
    }

    // 連線恢復後主動獲取存檔與最新全景狀態
    sendWsCommand('saves quiet', true);
  };

  ws.onclose = () => {
    if (isExplicitlyClosed) {
      logSystemMessage("系統：神識連線已主動切斷。", "#9ca3af");
      return;
    }

    const delaySec = Math.round(currentRetryDelay / 1000);
    logSystemMessage(`系統：靈識中斷，${delaySec} 秒後嘗試凝神重連...`, "#ef4444");
    scheduleReconnect();
  };

  ws.onerror = (error) => {
    // 瀏覽器 onerror 通常不帶詳細文字，僅記錄或忽略，隨後會觸發 onclose
    console.warn("WebSocket 連線異常:", error);
  };

  ws.onmessage = (event) => {
    if (typeof window !== 'undefined' && typeof window.handleServerMessage === 'function') {
      window.handleServerMessage(event.data);
    }
  };

  return ws;
}

/**
 * 排程指數退避重連
 */
function scheduleReconnect() {
  if (reconnectTimer || isExplicitlyClosed) {
    return;
  }
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connectWebSocket();
  }, currentRetryDelay);

  // 指數遞增延遲，最大 15 秒 (1s -> 2s -> 4s -> 8s -> 15s)
  currentRetryDelay = Math.min(currentRetryDelay * 2, MAX_RETRY_DELAY);
}

/**
 * 發送指令至伺服端
 * @param {string} cmd 指令內容
 * @param {boolean} [silent=false] 是否在文字日誌中靜默（不回顯）
 * @returns {boolean} 是否成功送出
 */
export function sendWsCommand(cmd, silent = false) {
  if (!cmd) return false;
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    if (!silent) {
      logSystemMessage("系統：靈識未凝聚，無法傳遞法旨...", "#ef4444");
    }
    return false;
  }

  ws.send(JSON.stringify({ type: "INPUT", text: cmd }));

  // 步進與靜默指令不需洗版文字日誌，僅記錄玩家主動操作
  const isSilentCmd = silent || cmd.startsWith('step ') || cmd.includes('quiet') || cmd.includes('silent');
  if (!isSilentCmd) {
    logSystemMessage("> " + cmd, "#6b7280");
  }
  return true;
}

/**
 * 主動中斷 WebSocket 連線（不觸發自動重連）
 */
export function disconnectWebSocket() {
  isExplicitlyClosed = true;
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }
  if (ws) {
    ws.close();
    ws = null;
  }
}

/**
 * 取得當前 WebSocket 實例
 * @returns {WebSocket|null}
 */
export function getWebSocket() {
  return ws;
}

/**
 * 檢查當前連線狀態是否正常開啟
 * @returns {boolean}
 */
export function isWebSocketConnected() {
  return ws !== null && ws.readyState === WebSocket.OPEN;
}

// 掛載至全域 window 物件以維持 100% 向後相容
if (typeof window !== 'undefined') {
  window.connectWebSocket = connectWebSocket;
  window.connect = connectWebSocket;
  window.sendWsCommand = sendWsCommand;
  window.send = sendWsCommand;
  window.disconnectWebSocket = disconnectWebSocket;
  window.getWebSocket = getWebSocket;
  window.isWebSocketConnected = isWebSocketConnected;
}
