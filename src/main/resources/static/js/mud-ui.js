/**
 * 處理輸入框的 Enter 事件 (相容處理，避免 DOM 查詢空指針)
 */
function handleEnter() {
    if (typeof window.handleDevEnter === 'function') {
        window.handleDevEnter();
        return;
    }
    const devInput = document.getElementById('dev-cmd-input') || document.getElementById('cmd-input');
    if (!devInput) return;
    const cmd = devInput.value.trim();
    if (cmd) {
        if (typeof send === 'function') {
            send(cmd); // send 函數會由各自的 HTML 實現 (WS 或 JavaFX)
        } else if (typeof window.sendWsCommand === 'function') {
            window.sendWsCommand(cmd);
        }
        devInput.value = "";
    }
    devInput.blur();
}

// 綁定輸入框 Enter 監聽 (若存在對應元素)
const legacyCmdInput = document.getElementById('dev-cmd-input') || document.getElementById('cmd-input');
if (legacyCmdInput) {
    legacyCmdInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleEnter();
    });
}

/**
 * 如果你需要處理一些通用的按鈕音效或震動回饋，也可以寫在這裡
 */
function provideFeedback() {
    if (typeof window !== 'undefined' && window.navigator && window.navigator.vibrate) {
        window.navigator.vibrate(10); // 觸控微震動
    }
}

if (typeof window !== 'undefined') {
    window.handleEnter = handleEnter;
    window.provideFeedback = provideFeedback;
}
