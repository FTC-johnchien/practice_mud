// 初始化 ANSI 轉換器
const ansi_up = new AnsiUp();
ansi_up.use_classes = false;

const logDiv = document.getElementById('log');
const cmdInput = document.getElementById('cmd-input');

/**
 * 更新血量與魔力條
 */
function updateStats(s) {
    const payload = s.payload || s;
    if (payload.hp !== undefined) {
        const hpMax = payload.maxHp || 100;
        document.getElementById('hp-val').innerText = `${payload.hp}/${hpMax}`;
        document.getElementById('hp-bar').style.width = (payload.hp / hpMax * 100) + "%";
    }
    if (payload.mp !== undefined) {
        const mpMax = payload.maxMp || 100;
        document.getElementById('mp-val').innerText = `${payload.mp}/${mpMax}`;
        document.getElementById('mp-bar').style.width = (payload.mp / mpMax * 100) + "%";
    }
}

/**
 * 將文字渲染到 Log 區（支援 ANSI 色碼）
 */
function appendHtml(rawText, color) {
    if (!rawText) return;
    const div = document.createElement('div');
    if (color) div.style.color = color;

    try {
        div.innerHTML = ansi_up.ansi_to_html(rawText);
        logDiv.appendChild(div);
        logDiv.scrollTop = logDiv.scrollHeight;

        // 限制行數
        if (logDiv.childNodes.length > 200) {
            logDiv.removeChild(logDiv.firstChild);
        }
    } catch (err) {
        console.error("渲染錯誤:", err);
    }
}

/**
 * 處理怪物詳細資訊
 */
function handleMobDetail(json) {
  appendHtml(`${json.description}`);
  appendHtml(`${json.healthStatus}`, `#00FF00`);
  if (json.items && json.items.length > 0) {
    appendHtml("身上帶著﹕\n" + json.items.map(item => `  ˇ${item}`).join("\n"));
  }
  appendHtml("\n");
}

/**
 * 處理屍體詳細資訊
 */
function handleCorpseDetail(json) {
  appendHtml(`${json.description}現在只剩下一具冰冷的屍體靜靜地躺在這裡`);
  if (json.items && json.items.length > 0) {
    appendHtml("找到的遺物有﹕\n" + json.items.map(item => `  ˇ${item}`).join("\n"));
  }
  appendHtml("\n");
}

/**
 * 統一的訊息分配器 (Dispatcher)
 */
function handleServerMessage(data) {
    // 支援純文字或物件格式
    if (typeof data === 'string') {
        try {
            data = JSON.parse(data);
        } catch (e) {
            appendHtml(data);
            return;
        }
    }

    if (data.type === 'TEXT' || data.content) {
        appendHtml(data.content || data.text);
    } else if (data.type === 'STAT_UPDATE' || data.type === 'stats' || data.hp !== undefined) {
        updateStats(data);
    } else if (data.type === 'MOB_DETAIL') {
        handleMobDetail(data.payload);
    } else if (data.type === 'CORPSE_DETAIL') {
        handleCorpseDetail(data.payload);
    }
}