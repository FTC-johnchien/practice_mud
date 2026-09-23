import { store } from './state-store.js';

/**
 * 指令發送派發器 (Command Dispatcher)
 * 負責透過 WebSocket 發送玩家指令，並處理步進防抖與方向路由
 */

/**
 * 發送原始指令至服務端
 * @param {string} cmd 指令內容
 * @param {boolean} silent 是否不印出至文字日誌
 */
export function sendCmd(cmd, silent = false) {
  if (typeof window !== 'undefined' && typeof window.send === 'function') {
    window.send(cmd, silent);
  }
}

/**
 * 鍵盤步進發送防抖 (Throttle ~120ms 防止長按過度觸發)
 * @param {string} dir 方向 (w, a, s, d)
 */
export function sendStep(dir) {
  const now = Date.now();
  const lastTime = store.get('lastStepTime') || 0;
  if (now - lastTime < 120) {
    return;
  }
  store.setState({ lastStepTime: now }, true);
  sendCmd('step ' + dir);
}

/**
 * 城鎮方向移動 (檢查可用出口與防抖)
 * @param {string} dir 方向 (north, south, east, west)
 */
export function sendTownMove(dir) {
  const now = Date.now();
  const lastTime = store.get('lastStepTime') || 0;
  if (now - lastTime < 120) {
    return;
  }
  store.setState({ lastStepTime: now }, true);

  const lastTown = store.get('lastTown');
  // 檢查城鎮房間是否存在該方向出口
  if (lastTown && Array.isArray(lastTown.exits)) {
    const hasExit = lastTown.exits.some(e => (e.direction || '').toLowerCase() === dir.toLowerCase());
    if (!hasExit) {
      const dirNames = { north: '北 (W)', south: '南 (S)', west: '西 (A)', east: '東 (D)' };
      const avail = lastTown.exits.map(e => {
        const d = (e.direction || '').toLowerCase();
        return `${dirNames[d] || d}: ${e.targetRoomName || e.targetRoomId}`;
      }).join('、');
      if (typeof window.appendHtml === 'function') {
        window.appendHtml(`<span style="color:#f59e0b;">【前路不通】此處往 ${dirNames[dir] || dir} 並無路徑。可用出口：[${avail || '無'}]</span>`);
      }
      return;
    }
  }

  sendCmd(dir);
}

/**
 * 十字方向發送 (根據當前 mode 自動分流至城鎮出口或地牢步進)
 * @param {string} dir 方向 (north, south, west, east)
 */
export function handleDpad(dir) {
  if (store.get('mode') === 'TOWN') {
    sendTownMove(dir);
  } else {
    const dirMap = { north: 'w', south: 's', west: 'a', east: 'd' };
    sendStep(dirMap[dir] || dir);
  }
}

// 掛載至 window 供 HTML 內聯 onclick 呼叫相容
if (typeof window !== 'undefined') {
  window.sendStep = sendStep;
  window.sendTownMove = sendTownMove;
  window.handleDpad = handleDpad;
}
