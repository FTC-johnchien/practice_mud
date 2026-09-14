/**
 * DRPG View & Controller
 * 負責 10x10 地牢雷達渲染、6人小隊 HUD、陣法靈威條、半即時戰鬥面板與隊員技能抽屜
 */

// 內部狀態快取
const drpgState = {
  lastDungeon: null,
  lastParty: null,
  lastBattle: null,
  selectedMemberIdx: 0,
  isSkillDrawerOpen: false,
  isInputFocused: false,
  lastStepTime: 0
};

/**
 * 鍵盤步進發送防抖 (Throttle ~120ms 防止長按過度觸發)
 */
function sendStep(dir) {
  const now = Date.now();
  if (now - drpgState.lastStepTime < 120) {
    return;
  }
  drpgState.lastStepTime = now;
  send('step ' + dir);
}

/**
 * 接收後端推送的 DRPG_STATE 結構化資料
 */
function updateDrpgView(payload) {
  if (!payload) return;

  if (payload.dungeon) {
    drpgState.lastDungeon = payload.dungeon;
    renderMinimap(payload.dungeon);
  }

  if (payload.party) {
    drpgState.lastParty = payload.party;
    renderPartyHud(payload.party);
  }

  if (payload.battle && payload.battle.inBattle) {
    drpgState.lastBattle = payload.battle;
    renderBattleArena(payload.battle);
    toggleBattleMode(true);
  } else {
    drpgState.lastBattle = null;
    hideBattleArena();
    toggleBattleMode(false);
  }
}

/**
 * 渲染 10x10 地牢雷達與前方視野
 */
function renderMinimap(dungeon) {
  const gridContainer = document.getElementById('drpg-grid');
  const infoHeader = document.getElementById('drpg-floor-info');
  const forwardInspect = document.getElementById('drpg-forward-inspect');

  if (!gridContainer) return;

  if (infoHeader) {
    const dirMap = { 'NORTH': '北', 'EAST': '東', 'SOUTH': '南', 'WEST': '西' };
    const dirCn = dirMap[dungeon.direction] || dungeon.direction;

    // 靈壓警戒度文字與等級
    const dangerMap = {
      'CALM': { label: '🟢 靈壓澄澈', cls: 'danger-calm' },
      'CAUTION': { label: '🟡 陰氣浮動', cls: 'danger-caution' },
      'DANGER': { label: '🟠 邪祟逼近', cls: 'danger-danger' },
      'ENCOUNTER': { label: '🔴 妖邪現身！', cls: 'danger-encounter' }
    };
    const dangerInfo = dangerMap[dungeon.dangerStatus] || dangerMap['CALM'];
    const dangerLevel = dungeon.dangerLevel || 0;

    infoHeader.innerHTML = `
      <div class="floor-header-row">
        <span class="floor-title">🏛️ ${dungeon.floorName}</span>
        <span class="coord-tag">[X: ${dungeon.x}, Y: ${dungeon.y}]</span>
        <span class="facing-tag">朝向: <strong class="arrow-glow">${dungeon.directionArrow}</strong> ${dirCn}</span>
      </div>
      <div class="danger-gauge-row">
        <span class="danger-status-badge ${dangerInfo.cls}">${dangerInfo.label}</span>
        <div class="danger-bar-track" title="警戒靈壓: ${dangerLevel}/100">
          <div class="danger-bar-fill ${dangerInfo.cls}" style="width: ${dangerLevel}%"></div>
        </div>
        <span class="danger-val-text">${dangerLevel}%</span>
      </div>
    `;
  }

  if (forwardInspect) {
    forwardInspect.innerText = dungeon.forwardInspection || '【前方無障礙】';
  }

  // 繪製 10x10 網格
  gridContainer.innerHTML = '';
  const w = dungeon.width || 10;
  const h = dungeon.height || 10;

  // 計算正前方一格座標
  let fx = dungeon.x;
  let fy = dungeon.y;
  if (dungeon.direction === 'NORTH') fy -= 1;
  else if (dungeon.direction === 'SOUTH') fy += 1;
  else if (dungeon.direction === 'EAST') fx += 1;
  else if (dungeon.direction === 'WEST') fx -= 1;

  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const cell = document.createElement('div');
      cell.className = 'grid-cell';

      const isVisited = dungeon.visited && dungeon.visited[y] && dungeon.visited[y][x];
      const isPlayer = (x === dungeon.x && y === dungeon.y);
      const isFront = (x === fx && y === fy);
      const symbol = (dungeon.tiles && dungeon.tiles[y]) ? dungeon.tiles[y][x] : '#';

      if (isPlayer) {
        cell.classList.add('cell-player');
        cell.innerText = dungeon.directionArrow || '▲';
        cell.title = `玩家位置 (${x}, ${y})`;
      } else {
        if (isFront) {
          cell.classList.add('cell-target-front');
        }
        if (!isVisited) {
          cell.classList.add('cell-fog');
          cell.innerText = '░';
        } else {
          // 已探明地塊
          switch (symbol) {
            case '#':
              cell.classList.add('cell-wall');
              cell.innerText = '█';
              cell.title = `青岡石壁 (${x}, ${y})`;
              break;
            case '+':
              cell.classList.add('cell-door');
              cell.innerText = '☩';
              cell.title = `封印石門 (${x}, ${y})`;
              break;
            case '$':
            case '◆':
              cell.classList.add('cell-chest');
              cell.innerText = '◆';
              cell.title = `古仙棺槨寶箱 (${x}, ${y})`;
              break;
            case '◇':
              cell.classList.add('cell-chest-opened');
              cell.innerText = '◇';
              cell.title = `已開啟棺槨 (${x}, ${y})`;
              break;
            case '^':
              cell.classList.add('cell-trap');
              cell.innerText = '▲';
              cell.title = `深淵黏液陷阱 (${x}, ${y})`;
              break;
            case '>':
              cell.classList.add('cell-stairs');
              cell.innerText = '▼';
              cell.title = `通往下層石階 (${x}, ${y})`;
              break;
            case '.':
            default:
              cell.classList.add('cell-path');
              cell.innerText = '·';
              cell.title = `墓道通路 (${x}, ${y})`;
              break;
          }
        }
      }
      gridContainer.appendChild(cell);
    }
  }
}

/**
 * 渲染 6 人小隊 HUD (包含前後衛分色、三態能量條及點選觸發技能盤)
 */
function renderPartyHud(party) {
  const partyContainer = document.getElementById('party-members-list');
  const formNameEl = document.getElementById('formation-name-badge');
  const energyFillEl = document.getElementById('formation-energy-fill');
  const energyValEl = document.getElementById('formation-energy-val');
  const ultBtn = document.getElementById('formation-ult-btn');

  // 更新頂部道門陣法與靈威狀態
  if (party.formationName && formNameEl) {
    formNameEl.innerText = `☯️ ${party.formationName}`;
  }
  if (party.formationEnergy !== undefined && energyFillEl && energyValEl) {
    energyFillEl.style.width = `${party.formationEnergy}%`;
    energyValEl.innerText = `${party.formationEnergy}/100`;

    if (ultBtn) {
      if (party.canCastUltimate) {
        ultBtn.classList.add('ready-pulse');
        ultBtn.title = `★ 奧義就緒：點擊釋放【${party.ultimateSkillName}】(消耗 100 靈威)`;
      } else {
        ultBtn.classList.remove('ready-pulse');
        ultBtn.title = `陣法奧義【${party.ultimateSkillName || '大招'}】(需 100 靈威)`;
      }
    }
  }

  if (!partyContainer || !party.members) return;

  partyContainer.innerHTML = '';
  party.members.forEach((m, idx) => {
    const card = document.createElement('div');
    const isSelected = (idx === drpgState.selectedMemberIdx && drpgState.isSkillDrawerOpen);
    card.className = `party-card row-${m.row.toLowerCase()}${isSelected ? ' selected-card' : ''}`;
    card.onclick = () => selectPartyMember(idx);
    card.title = `點選 #${idx + 1} ${m.name} 展開技能盤`;

    const hpPct = Math.min(100, Math.max(0, (m.hp / m.maxHp) * 100));
    const sanPct = Math.min(100, Math.max(0, (m.san / m.maxSan) * 100));
    const sanClass = `san-${(m.sanLevel || 'NORMAL').toLowerCase()}`;
    const rowBadge = m.row === 'FRONT' ? '前衛' : '後衛';

    // 依職業三態資源判定能量條
    const resType = m.resourceType || 'MP';
    const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
    const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;
    const resPct = Math.min(100, Math.max(0, maxRes > 0 ? (curRes / maxRes) * 100 : 0));

    let resFillClass = 'mp-fill';
    let resLabel = `MP ${curRes}/${maxRes}`;
    if (resType === 'RAGE') {
      resFillClass = 'rage-fill';
      resLabel = `怒氣 ${curRes}/${maxRes}`;
    } else if (resType === 'COMBO') {
      resFillClass = 'combo-fill';
      resLabel = `連擊 ${curRes}/${maxRes}`;
    }

    card.innerHTML = `
      <div class="card-info-side">
        <div class="card-name-row">
          <span class="member-idx">#${idx + 1}</span>
          <span class="member-name" title="${m.name}">${m.name}</span>
          <span class="member-row badge-${m.row.toLowerCase()}">${rowBadge}</span>
        </div>
        <div class="member-title" title="${m.roleTitle}">${m.roleTitle}</div>
      </div>
      <div class="card-bars-side">
        <div class="mini-bar-wrap" title="氣血 HP: ${m.hp}/${m.maxHp}">
          <div class="mini-bar hp-fill" style="width: ${hpPct}%"></div>
          <span class="mini-val">HP ${m.hp}/${m.maxHp}</span>
        </div>
        <div class="mini-bar-wrap" title="${resLabel}">
          <div class="mini-bar ${resFillClass}" style="width: ${resPct}%"></div>
          <span class="mini-val">${resLabel}</span>
        </div>
        <div class="mini-bar-wrap san-bar-wrap" title="道心 SAN: ${m.san}/${m.maxSan} ${m.sanState || ''}">
          <div class="mini-bar san-fill ${sanClass}" style="width: ${sanPct}%"></div>
          <span class="mini-val ${sanClass}">SAN ${m.san}/${m.maxSan}</span>
        </div>
      </div>
    `;
    partyContainer.appendChild(card);
  });

  // 若技能抽屜已開啟，同步刷新最新冷卻與能量數值
  if (drpgState.isSkillDrawerOpen) {
    renderSkillDrawer();
  }
}

/**
 * 點選成員卡片觸發技能盤展開/切換
 */
function selectPartyMember(idx) {
  if (drpgState.selectedMemberIdx === idx && drpgState.isSkillDrawerOpen) {
    // 再次點擊同一成員可切換收合
    closeSkillDrawer();
    return;
  }
  drpgState.selectedMemberIdx = idx;
  drpgState.isSkillDrawerOpen = true;
  if (drpgState.lastParty) {
    renderPartyHud(drpgState.lastParty);
  }
  renderSkillDrawer();
}

/**
 * 渲染所選隊員的技能抽屜 (Skill Drawer)
 */
function renderSkillDrawer() {
  const drawer = document.getElementById('skill-drawer');
  if (!drawer || !drpgState.lastParty || !drpgState.lastParty.members) return;

  const m = drpgState.lastParty.members[drpgState.selectedMemberIdx];
  if (!m) return;

  drawer.classList.remove('hidden');

  const resType = m.resourceType || 'MP';
  const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
  const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;

  let badgeCls = 'res-badge-mp';
  let badgeIcon = '🔮 真元';
  if (resType === 'RAGE') {
    badgeCls = 'res-badge-rage';
    badgeIcon = '🩸 怒氣';
  } else if (resType === 'COMBO') {
    badgeCls = 'res-badge-combo';
    badgeIcon = '⚔️ 連擊點';
  }

  // 構造技能按鍵
  const skills = m.skills || [];
  let skillsHtml = '';
  if (skills.length === 0) {
    skillsHtml = '<span style="color:#9ca3af;font-size:12px;">該成員專注於自動平砍，暫無主動奧義。</span>';
  } else {
    skillsHtml = skills.map(s => {
      const onCd = s.remainingCooldownMs > 0;
      const canCast = s.available && !onCd && m.alive;
      const cdSec = onCd ? (s.remainingCooldownMs / 1000).toFixed(1) : 0;

      let costTagClass = 'res-badge-mp';
      if (s.costType === 'RAGE') costTagClass = 'res-badge-rage';
      else if (s.costType === 'COMBO') costTagClass = 'res-badge-combo';

      return `
        <button class="skill-btn" ${canCast ? '' : 'disabled'}
          onclick="castPartySkill(${drpgState.selectedMemberIdx}, '${s.id}')"
          title="${s.name} - ${s.description}">
          <div class="skill-btn-title-row">
            <span class="skill-btn-name">${s.icon || '⚡'} ${s.name}</span>
            <span class="skill-btn-cost ${costTagClass}">${s.costDescription}</span>
          </div>
          <div class="skill-btn-desc">${s.description}</div>
          ${onCd ? `<div class="skill-cd-overlay">⌛ ${cdSec}s</div>` : ''}
        </button>
      `;
    }).join('');
  }

  drawer.innerHTML = `
    <div class="drawer-member-info">
      <div>
        <span class="drawer-member-name">#${drpgState.selectedMemberIdx + 1} ${m.name}</span>
        <span class="drawer-member-role">${m.roleTitle}</span>
      </div>
      <span class="drawer-resource-badge ${badgeCls}">${badgeIcon} ${curRes}/${maxRes}</span>
    </div>
    <div class="drawer-skills-container">
      ${skillsHtml}
    </div>
    <button class="drawer-close-btn" onclick="closeSkillDrawer()" title="關閉技能盤 (Esc)">✕</button>
  `;
}

/**
 * 關閉技能盤
 */
function closeSkillDrawer() {
  drpgState.isSkillDrawerOpen = false;
  const drawer = document.getElementById('skill-drawer');
  if (drawer) {
    drawer.classList.add('hidden');
  }
  if (drpgState.lastParty) {
    renderPartyHud(drpgState.lastParty);
  }
}

/**
 * 釋放隊員專屬技能
 */
function castPartySkill(memberIdx, skillId) {
  send(`skill cast ${memberIdx} ${skillId}`);
}

/**
 * 渲染戰場敵方陣列 (Battle Arena)
 */
function renderBattleArena(battle) {
  const panel = document.getElementById('battle-arena-panel');
  const enemiesBox = document.getElementById('battle-enemies-container');
  const badge = document.getElementById('battle-status-badge');

  if (!panel || !enemiesBox) return;

  panel.classList.remove('hidden');

  if (badge) {
    if (battle.state === 'VICTORY') {
      badge.innerText = '★ 戰鬥大捷！';
      badge.style.background = 'rgba(16, 185, 129, 0.2)';
      badge.style.borderColor = '#10b981';
      badge.style.color = '#34d399';
    } else if (battle.state === 'DEFEAT') {
      badge.innerText = '⚠️ 戰陣潰散！';
      badge.style.background = 'rgba(239, 68, 68, 0.3)';
    } else {
      badge.innerText = '⚔️ 戰鬥交鋒中';
      badge.style.background = 'rgba(239, 68, 68, 0.2)';
      badge.style.borderColor = 'rgba(239, 68, 68, 0.4)';
      badge.style.color = '#fca5a5';
    }
  }

  enemiesBox.innerHTML = '';
  (battle.enemies || []).forEach(e => {
    const card = document.createElement('div');
    const isTarget = e.isSelectedTarget;
    const isAlive = e.alive;
    const rowClass = (e.row || 'FRONT').toLowerCase();

    card.className = `enemy-card row-${rowClass}${isTarget ? ' selected-target' : ''}${!isAlive ? ' dead' : ''}`;
    card.onclick = () => selectBattleTarget(e.index);

    const hpPct = Math.min(100, Math.max(0, (e.hp / e.maxHp) * 100));
    const rowBadge = e.row === 'FRONT' ? '前衛' : '後衛';

    card.innerHTML = `
      <div class="enemy-name-row">
        <span class="enemy-name" title="${e.name}">#${e.index + 1} ${e.name}</span>
        <span class="enemy-row-badge ${e.row === 'FRONT' ? 'badge-front' : 'badge-back'}">${rowBadge}</span>
      </div>
      <div class="enemy-hp-wrap" title="生命 HP: ${e.hp}/${e.maxHp}">
        <div class="enemy-hp-bar" style="width: ${hpPct}%"></div>
        <span class="enemy-hp-text">${isAlive ? `${e.hp}/${e.maxHp}` : '已伏誅'}</span>
      </div>
      <div class="enemy-status-row">
        ${e.isStunned ? '<span class="enemy-stun-badge">💫 眩暈中</span>' : ''}
      </div>
    `;
    enemiesBox.appendChild(card);
  });
}

/**
 * 隱藏戰場面板
 */
function hideBattleArena() {
  const panel = document.getElementById('battle-arena-panel');
  if (panel) {
    panel.classList.add('hidden');
  }
}

/**
 * 鎖定集火目標
 */
function selectBattleTarget(idx) {
  send(`battle target ${idx}`);
}

/**
 * 切換戰鬥模式按鈕與探索模式按鈕
 */
function toggleBattleMode(inBattle) {
  const actGroup = document.getElementById('action-buttons-group');
  const btlGroup = document.getElementById('battle-buttons-group');
  const modeBadge = document.getElementById('control-mode-badge');

  if (actGroup && btlGroup) {
    if (inBattle) {
      actGroup.classList.add('hidden');
      btlGroup.classList.remove('hidden');
      if (modeBadge && !drpgState.isInputFocused) {
        modeBadge.className = 'mode-badge mode-typing';
        modeBadge.style.background = 'rgba(239, 68, 68, 0.2)';
        modeBadge.style.color = '#fca5a5';
        modeBadge.style.border = '1px solid #ef4444';
        modeBadge.innerText = '⚔️ 戰鬥交鋒中 (按 Space 迎戰，點怪集火，選隊員放招，Esc 遁地)';
      }
    } else {
      actGroup.classList.remove('hidden');
      btlGroup.classList.add('hidden');
      if (modeBadge && !drpgState.isInputFocused) {
        modeBadge.style.background = '';
        modeBadge.style.color = '';
        modeBadge.style.border = '';
        modeBadge.className = 'mode-badge mode-exploring';
        modeBadge.innerText = '🎮 步進探索模式 (按 W/A/S/D 移動，按 / 或 Enter 輸入指令)';
      }
    }
  }
}

/**
 * 快捷操作按鈕處理函式 (包含即時 CSS 視覺反饋)
 */
function triggerMapAction() {
  const grid = document.getElementById('drpg-grid');
  if (grid) {
    grid.classList.remove('radar-pulse');
    void grid.offsetWidth;
    grid.classList.add('radar-pulse');
  }
  send('map');
}

function triggerPartyAction() {
  const list = document.getElementById('party-members-list');
  if (list) {
    list.classList.remove('party-pulse');
    void list.offsetWidth;
    list.classList.add('party-pulse');
  }
  send('party');
}

function triggerFormationAction() {
  const bar = document.querySelector('.formation-status-wrap');
  if (bar) {
    bar.classList.remove('radar-pulse');
    void bar.offsetWidth;
    bar.classList.add('radar-pulse');
  }
  send('formation toggle');
}

function triggerInspectAction() {
  const inspect = document.getElementById('drpg-forward-inspect');
  if (inspect) {
    inspect.classList.remove('inspect-pulse');
    void inspect.offsetWidth;
    inspect.classList.add('inspect-pulse');
  }
  send('dungeon look');
}

function triggerRestAction() {
  send('rest');
}

/**
 * 初始化全局鍵盤監聽事件 (WASD、探索/戰鬥快捷鍵)
 */
function initKeyboardControls() {
  const inputEl = document.getElementById('cmd-input');
  const modeBadge = document.getElementById('control-mode-badge');

  function updateModeBadge(inInput) {
    drpgState.isInputFocused = inInput;
    if (drpgState.lastBattle && drpgState.lastBattle.inBattle) {
      toggleBattleMode(true);
      return;
    }
    if (modeBadge) {
      if (inInput) {
        modeBadge.className = 'mode-badge mode-typing';
        modeBadge.innerText = '⌨️ 文字輸入中 (按 Esc 或 Enter 恢復步進)';
      } else {
        modeBadge.className = 'mode-badge mode-exploring';
        modeBadge.innerText = '🎮 步進探索模式 (按 W/A/S/D 移動，按 / 或 Enter 輸入指令)';
      }
    }
  }

  if (inputEl) {
    inputEl.addEventListener('focus', () => updateModeBadge(true));
    inputEl.addEventListener('blur', () => updateModeBadge(false));
  }

  window.addEventListener('keydown', (e) => {
    // 1. 若目前正在輸入框中打字
    if (document.activeElement === inputEl) {
      if (e.key === 'Escape') {
        inputEl.blur();
        updateModeBadge(false);
      }
      return;
    }

    const key = e.key.toLowerCase();

    // 2. 快捷鍵轉至文字輸入
    if (key === '/' || e.key === 'Enter') {
      e.preventDefault();
      if (inputEl) {
        inputEl.focus();
        updateModeBadge(true);
      }
      return;
    }

    // 3. 戰鬥中與非戰鬥共通快捷鍵
    if (e.key === 'Escape') {
      if (drpgState.isSkillDrawerOpen) {
        closeSkillDrawer();
        return;
      }
      if (drpgState.lastBattle && drpgState.lastBattle.inBattle) {
        send('battle flee');
        return;
      }
    }

    // 4. 數字鍵 1~6 快捷選取隊員展開技能盤
    if (['1', '2', '3', '4', '5', '6'].includes(key)) {
      e.preventDefault();
      const idx = parseInt(key) - 1;
      selectPartyMember(idx);
      return;
    }

    // 5. 戰鬥中快捷鍵
    if (drpgState.lastBattle && drpgState.lastBattle.inBattle) {
      if (key === ' ' || key === 'Spacebar' || e.code === 'Space') {
        e.preventDefault();
        send('battle fight');
      } else if (key === 'u') {
        e.preventDefault();
        send('formation cast');
      } else if (key === 't') {
        e.preventDefault();
        send('battle target 0');
      } else if (key === 'p') {
        e.preventDefault();
        triggerPartyAction();
      }
      return;
    }

    // 6. 探索模式步進與快捷功能
    if (key === 'w' || e.key === 'ArrowUp') {
      e.preventDefault();
      sendStep('w');
    } else if (key === 's' || e.key === 'ArrowDown') {
      e.preventDefault();
      sendStep('s');
    } else if (key === 'a' || e.key === 'ArrowLeft') {
      e.preventDefault();
      sendStep('a');
    } else if (key === 'd' || e.key === 'ArrowRight') {
      e.preventDefault();
      sendStep('d');
    } else if (key === 'm') {
      e.preventDefault();
      triggerMapAction();
    } else if (key === 'p') {
      e.preventDefault();
      triggerPartyAction();
    } else if (key === 'f') {
      e.preventDefault();
      triggerFormationAction();
    } else if (key === 'i' || key === 'l') {
      e.preventDefault();
      triggerInspectAction();
    } else if (key === 'r') {
      e.preventDefault();
      triggerRestAction();
    } else if (key === 'u') {
      e.preventDefault();
      send('formation cast');
    }
  });

  updateModeBadge(false);
}

// 頁面載入完成後初始化與全域函式掛載
window.triggerMapAction = triggerMapAction;
window.triggerPartyAction = triggerPartyAction;
window.triggerFormationAction = triggerFormationAction;
window.triggerInspectAction = triggerInspectAction;
window.triggerRestAction = triggerRestAction;
window.sendStep = sendStep;
window.updateDrpgView = updateDrpgView;
window.selectPartyMember = selectPartyMember;
window.closeSkillDrawer = closeSkillDrawer;
window.castPartySkill = castPartySkill;
window.selectBattleTarget = selectBattleTarget;
window.triggerFightAction = function() { send('battle fight'); };

window.addEventListener('DOMContentLoaded', () => {
  initKeyboardControls();
});