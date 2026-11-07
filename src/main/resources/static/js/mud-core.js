// 初始化 ANSI 轉換器 (向下相容)
let ansi_up = null;
if (typeof AnsiUp !== 'undefined') {
    try {
        ansi_up = new AnsiUp();
        ansi_up.use_classes = false;
    } catch (e) {
        console.warn("[mud-core] 初始化 AnsiUp 失敗:", e);
    }
}

/**
 * 更新血量與魔力條 (相容處理，防範 DOM 元素不存在拋出 TypeError)
 */
function updateStats(s) {
    if (!s) return;
    const payload = s.payload || s;
    if (payload.hp !== undefined) {
        const hpMax = payload.maxHp || 100;
        const hpVal = document.getElementById('hp-val');
        const hpBar = document.getElementById('hp-bar');
        if (hpVal) hpVal.innerText = `${payload.hp}/${hpMax}`;
        if (hpBar) hpBar.style.width = (payload.hp / hpMax * 100) + "%";
    }
    if (payload.mp !== undefined) {
        const mpMax = payload.maxMp || 100;
        const mpVal = document.getElementById('mp-val');
        const mpBar = document.getElementById('mp-bar');
        if (mpVal) mpVal.innerText = `${payload.mp}/${mpMax}`;
        if (mpBar) mpBar.style.width = (payload.mp / mpMax * 100) + "%";
    }
}

/**
 * 將文字渲染到 Log 區（統一導流至 message-log-panel 的 appendLog）
 */
function appendHtml(rawText, color, explicitCat) {
    if (!rawText) return;
    if (typeof window !== 'undefined' && typeof window.appendLog === 'function') {
        window.appendLog(rawText, color, explicitCat);
        return;
    }

    // 若 message-log-panel 尚未加載時之後備渲染
    const logDiv = document.getElementById('log');
    if (!logDiv) return;
    const div = document.createElement('div');
    div.className = 'log-entry';
    div.dataset.category = explicitCat || 'system';
    if (color) div.style.color = color;

    try {
        const safeText = (typeof window.escapeHtml === 'function')
          ? window.escapeHtml(rawText)
          : String(rawText).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#039;');
        div.innerHTML = (ansi_up && typeof ansi_up.ansi_to_html === 'function')
          ? ansi_up.ansi_to_html(safeText)
          : safeText;
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
function handleEntityDetail(json) {
  if (!json) return;
  appendHtml(`${json.description}`, null, 'combat');
  appendHtml(`${json.healthStatus}`, `#00FF00`, 'combat');
  if (json.items && json.items.length > 0) {
    appendHtml("身上帶著﹕\n" + json.items.map(item => `  ˇ${item}`).join("\n"), null, 'combat');
  }
  appendHtml("\n", null, 'combat');
}

/**
 * 處理屍體詳細資訊
 */
function handleCorpseDetail(json) {
  if (!json) return;
  appendHtml(`${json.description}現在只剩下一具冰冷的屍體靜靜地躺在這裡`, null, 'story');
  if (json.items && json.items.length > 0) {
    appendHtml("找到的遺物有﹕\n" + json.items.map(item => `  ˇ${item}`).join("\n"), null, 'story');
  }
  appendHtml("\n", null, 'story');
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

    if (data.type === 'DRPG_STATE') {
        if (typeof updateDrpgView === 'function') {
            updateDrpgView(data);
        }
    } else if (data.type === 'BATTLE_EVENTS') {
        if (typeof window.onBattleEvents === 'function') {
            window.onBattleEvents(data);
        }
    } else if (data.type === 'BATTLE_VICTORY') {
        if (typeof window.onBattleVictory === 'function') {
            window.onBattleVictory(data);
        }
    } else if (data.type === 'SAVE_SLOTS') {
        if (typeof updateSaveSlotsView === 'function') {
            updateSaveSlotsView(data.slots);
        }
    } else if (data.type === 'SHOP_CATALOG') {
        if (typeof openShopModal === 'function') {
            openShopModal(data);
        } else if (typeof renderShopCatalogInLog === 'function') {
            renderShopCatalogInLog(data);
        }
    } else if (data.type === 'TOWN_HUB') {
        if (typeof window.onTownHub === 'function') {
            window.onTownHub(data.data || data);
        }
    } else if (data.type === 'OPEN_FACILITY') {
        if (typeof window.onOpenFacility === 'function') {
            window.onOpenFacility(data);
        }
    } else if (data.type === 'CAMP_REST_RESULT') {
        if (typeof window.onCampRestResult === 'function') {
            window.onCampRestResult(data);
        }
    } else if (data.type === 'TEXT' || data.content) {
        appendHtml(data.content || data.text);
    } else if (data.type === 'STAT_UPDATE' || data.type === 'stats' || data.hp !== undefined) {
        updateStats(data);
    } else if (data.type === 'ENTITY_DETAIL') {
        handleEntityDetail(data.payload);
    } else if (data.type === 'CORPSE_DETAIL') {
        handleCorpseDetail(data.payload);
    }
}

// 掛載至 window
if (typeof window !== 'undefined') {
    window.appendHtml = appendHtml;
    window.handleServerMessage = handleServerMessage;
    window.updateStats = updateStats;
    window.handleEntityDetail = handleEntityDetail;
    window.handleCorpseDetail = handleCorpseDetail;
}
