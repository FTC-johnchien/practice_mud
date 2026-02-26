/**
 * 處理輸入框的 Enter 事件
 */
function handleEnter() {
    const cmd = cmdInput.value.trim();
    if (cmd) {
        send(cmd); // send 函數會由各自的 HTML 實現 (WS 或 JavaFX)
        cmdInput.value = "";
    }
}

// 綁定輸入框 Enter 監聽
if (cmdInput) {
    cmdInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') handleEnter();
    });
}

/**
 * 如果你需要處理一些通用的按鈕音效或震動回饋，也可以寫在這裡
 */
function provideFeedback() {
    if (window.navigator && window.navigator.vibrate) {
        window.navigator.vibrate(10); // 觸控微震動
    }
}