/**
 * DRPG View & Controller
 * 負責 10x10 地牢雷達渲染、6人小隊 HUD、陣法靈威條、半即時戰鬥面板與隊員技能抽屜
 */

// 內部狀態快取
const drpgState = {
  mode: 'TOWN',
  lastTown: null,
  isDevConsoleOpen: false,
  lastDungeon: null,
  lastParty: null,
  lastBattle: null,
  selectedMemberIdx: 0,
  isSkillDrawerOpen: false,
  isBagDrawerOpen: false,
  isInputFocused: false,
  lastStepTime: 0,
  radarPosition: localStorage.getItem('drpg_radar_pos') || 'left',
  radarMode: localStorage.getItem('drpg_radar_mode') || 'centered', // 'centered' 或 'full'
  saveSlots: [],
  isSaveModalOpen: false,
  saveModalMode: 'load',
  isPartyModalOpen: false,
  isShopModalOpen: false,
  lastShopCatalog: null,
  // 進入遊戲世界流程狀態 (三階段)
  isTitleScreenOpen: true,
  hasEnteredGameWorld: false,
  isNewGameModalOpen: false,
  isPrologueModalOpen: false,
  isGuideModalOpen: false,
  selectedFormation: 'formation_four_symbols',
  latestSaveSlot: null
};

/**
 * 切換雷達左右停靠位置
 */
function toggleRadarPosition() {
  drpgState.radarPosition = drpgState.radarPosition === 'left' ? 'right' : 'left';
  localStorage.setItem('drpg_radar_pos', drpgState.radarPosition);
  applyRadarPosition();
}

function applyRadarPosition() {
  const mainViewport = document.querySelector('.main-viewport');
  const btn = document.getElementById('radar-dock-toggle-btn');
  const townBtn = document.getElementById('town-swap-side-btn');
  const battleBtn = document.getElementById('battle-swap-side-btn');
  if (!mainViewport) return;
  if (drpgState.radarPosition === 'right') {
    mainViewport.classList.add('layout-radar-right');
    if (btn) btn.innerText = '⮂ 置左';
    if (townBtn) townBtn.innerText = '⮂ 置左';
    if (battleBtn) battleBtn.innerText = '⮂ 置左';
  } else {
    mainViewport.classList.remove('layout-radar-right');
    if (btn) btn.innerText = '⮃ 置右';
    if (townBtn) townBtn.innerText = '⮃ 置右';
    if (battleBtn) battleBtn.innerText = '⮃ 置右';
  }
}

/**
 * 切換視口跟隨模式 (居中視口 vs 全圖俯瞰)
 */
function toggleRadarViewMode() {
  drpgState.radarMode = drpgState.radarMode === 'centered' ? 'full' : 'centered';
  localStorage.setItem('drpg_radar_mode', drpgState.radarMode);
  updateRadarModeBtn();
  if (drpgState.lastDungeon) {
    renderMinimap(drpgState.lastDungeon);
  }
}

function updateRadarModeBtn() {
  const btn = document.getElementById('radar-mode-toggle-btn');
  if (btn) {
    btn.innerText = drpgState.radarMode === 'centered' ? '🎯 視口' : '🗺️ 全圖';
  }
}

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
 * 十字方向發送 (根據當前 mode 自動分流至城鎮出口或地牢步進)
 */
function handleDpad(dir) {
  if (drpgState.mode === 'TOWN') {
    sendTownMove(dir);
  } else {
    const dirMap = { north: 'w', south: 's', west: 'a', east: 'd' };
    sendStep(dirMap[dir] || dir);
  }
}

function sendTownMove(dir) {
  const now = Date.now();
  if (now - drpgState.lastStepTime < 120) {
    return;
  }
  drpgState.lastStepTime = now;

  // 檢查城鎮房間是否存在該方向出口
  if (drpgState.lastTown && Array.isArray(drpgState.lastTown.exits)) {
    const hasExit = drpgState.lastTown.exits.some(e => (e.direction || '').toLowerCase() === dir.toLowerCase());
    if (!hasExit) {
      const dirNames = { north: '北 (W)', south: '南 (S)', west: '西 (A)', east: '東 (D)' };
      const avail = drpgState.lastTown.exits.map(e => {
        const d = (e.direction || '').toLowerCase();
        return `${dirNames[d] || d}: ${e.targetRoomName || e.targetRoomId}`;
      }).join('、');
      if (typeof appendHtml === 'function') {
        appendHtml(`<span style="color:#f59e0b;">【前路不通】此處往 ${dirNames[dir] || dir} 並無路徑。可用出口：[${avail || '無'}]</span>`);
      }
      return;
    }
  }

  send(dir);
}

/**
 * 開發者終端控制台切換
 */
function toggleDevConsole(forceState) {
  const modal = document.getElementById('dev-console-modal');
  const input = document.getElementById('dev-cmd-input');
  if (!modal) return;

  drpgState.isDevConsoleOpen = (typeof forceState === 'boolean') ? forceState : !drpgState.isDevConsoleOpen;
  if (drpgState.isDevConsoleOpen) {
    modal.classList.remove('hidden');
    if (input) {
      input.value = '';
      setTimeout(() => input.focus(), 50);
    }
  } else {
    modal.classList.add('hidden');
    if (input) input.blur();
  }
}

function handleDevEnter() {
  const input = document.getElementById('dev-cmd-input');
  if (!input) return;
  const cmd = input.value.trim();
  if (cmd) {
    send(cmd);
    input.value = '';
  }
  toggleDevConsole(false);
}

/**
 * 接收後端推送的 DRPG_STATE 結構化資料
 */
function updateDrpgView(payload) {
  if (!payload) return;

  const mode = payload.mode || (payload.dungeon ? 'DUNGEON' : 'TOWN');
  drpgState.mode = mode;

  const dungeonPanel = document.getElementById('dungeon-radar-panel');
  const townPanel = document.getElementById('town-nav-panel');
  const battlePanel = document.getElementById('battle-arena-panel');

  const inBattle = Boolean(payload.battle && payload.battle.inBattle);

  if (inBattle) {
    if (dungeonPanel) dungeonPanel.classList.add('hidden');
    if (townPanel) townPanel.classList.add('hidden');
    if (battlePanel) battlePanel.classList.remove('hidden');
    drpgState.lastBattle = payload.battle;
    renderBattleArena(payload.battle);
    toggleBattleMode(true);
  } else {
    if (battlePanel) battlePanel.classList.add('hidden');
    drpgState.lastBattle = null;
    toggleBattleMode(false);
    if (document.activeElement && (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA' || document.activeElement.tagName === 'BUTTON')) {
      document.activeElement.blur();
    }

    if (mode === 'TOWN') {
      if (dungeonPanel) dungeonPanel.classList.add('hidden');
      if (townPanel) townPanel.classList.remove('hidden');
      if (payload.town) {
        drpgState.lastTown = payload.town;
        renderTownNav(payload.town);
      }
    } else {
      if (townPanel) townPanel.classList.add('hidden');
      if (dungeonPanel) dungeonPanel.classList.remove('hidden');
      if (payload.dungeon) {
        drpgState.lastDungeon = payload.dungeon;
        renderMinimap(payload.dungeon);
      }
    }
  }

  if (payload.party) {
    drpgState.lastParty = payload.party;
    renderPartyHud(payload.party);
    if (inBattle) {
      renderBattlePartyQuickBar(payload.party);
    }
    if (drpgState.isBagDrawerOpen) {
      renderBagDrawer();
    }
    if (drpgState.isPartyModalOpen) {
      renderPartyModal();
    }
  }
}

/**
 * 標準化城鎮 NPC 能力標籤 (支援後端富物件與前端向後相容)
 */
function normalizeTownCapability(cap, npc) {
  if (typeof cap === 'object' && cap && cap.label) {
    return {
      type: (cap.type || 'ASK').toLowerCase(),
      label: cap.label,
      icon: cap.icon || '✨',
      command: cap.command || `ask ${npc.alias || npc.id}`
    };
  }
  const key = (typeof cap === 'string' ? cap : (cap && cap.type ? cap.type : '')).toUpperCase();
  const alias = npc.alias || npc.id;
  switch (key) {
    case 'SHOP': return { type: 'shop', label: '貨棧買賣', icon: '🛒', command: 'shop' };
    case 'TALK': return { type: 'ask', label: '相談交談', icon: '💬', command: `ask ${alias}` };
    case 'REST': return { type: 'rest', label: '客棧安歇', icon: '🛏️', command: 'rest' };
    case 'RECRUIT': return { type: 'recruit', label: '招募入隊', icon: '🤝', command: `recruit ${alias}` };
    case 'DISMISS': return { type: 'dismiss', label: '請離隊友', icon: '👋', command: `dismiss ${alias}` };
    case 'QUEST': return { type: 'quest', label: '任務指引', icon: '📜', command: `ask ${alias}` };
    case 'FIGHT': return { type: 'fight', label: '拔劍迎擊', icon: '⚔️', command: `kill ${alias}` };
    default: return { type: 'ask', label: '交談', icon: '💬', command: `ask ${alias}` };
  }
}

/**
 * 渲染城鎮導航與能力標籤面板
 */
function renderTownNav(town) {
  if (!town) return;

  const zoneBadge = document.getElementById('town-zone-badge');
  const roomTitle = document.getElementById('town-room-title');
  const roomIdTag = document.getElementById('town-room-id-tag');
  const roomDesc = document.getElementById('town-room-desc');

  if (zoneBadge) zoneBadge.innerText = town.zoneName || '新手村';
  if (roomTitle) roomTitle.innerText = town.roomName || '客棧';
  if (roomIdTag) roomIdTag.innerText = town.roomId || '';
  if (roomDesc) roomDesc.innerText = town.description || '';

  // 1. 羅盤出口渲染 (正交 4 向)
  const exits = town.exits || [];
  const exitMap = {};
  const specialExits = [];

  exits.forEach(ex => {
    const d = (ex.direction || '').toLowerCase();
    if (['north', 'south', 'west', 'east'].includes(d)) {
      exitMap[d] = ex;
    } else {
      specialExits.push(ex);
    }
  });

  const dirs = [
    { key: 'north', btnId: 'exit-btn-north', nameId: 'exit-name-north', label: '▲ 北' },
    { key: 'south', btnId: 'exit-btn-south', nameId: 'exit-name-south', label: '▼ 南' },
    { key: 'west',  btnId: 'exit-btn-west',  nameId: 'exit-name-west',  label: '◀ 西' },
    { key: 'east',  btnId: 'exit-btn-east',  nameId: 'exit-name-east',  label: '東 ▶' }
  ];

  dirs.forEach(d => {
    const btn = document.getElementById(d.btnId);
    const nameEl = document.getElementById(d.nameId);
    const ex = exitMap[d.key];
    if (btn && nameEl) {
      if (ex) {
        btn.classList.remove('disabled');
        btn.disabled = false;
        btn.title = `前往：${ex.targetRoomName || ex.targetRoomId} (${d.key})`;
        nameEl.innerText = ex.targetRoomName || ex.targetRoomId;
      } else {
        btn.classList.add('disabled');
        btn.disabled = true;
        btn.title = '無出路';
        nameEl.innerText = '無出路';
      }
    }
  });

  // 特殊出口渲染 (up, down, enter 等)
  const specialContainer = document.getElementById('town-special-exits');
  if (specialContainer) {
    specialContainer.innerHTML = '';
    specialExits.forEach(ex => {
      const btn = document.createElement('button');
      btn.className = 'special-exit-btn';
      let icon = '🚪';
      if (ex.direction === 'up') icon = '🪜 向上';
      else if (ex.direction === 'down') icon = '🪜 向下';
      else if (ex.direction === 'enter') icon = '⛩️ 踏入';

      btn.innerHTML = `${icon} ${ex.targetRoomName || ex.targetRoomId}`;
      btn.onclick = () => {
        if (ex.actionCommand) {
          send(ex.actionCommand);
        } else {
          send(ex.direction);
        }
      };
      specialContainer.appendChild(btn);
    });
  }

  // 2. 身旁人物與互動能力標籤渲染
  const npcsContainer = document.getElementById('town-npcs-list');
  const npcCountBadge = document.getElementById('town-npc-count');
  if (npcsContainer) {
    npcsContainer.innerHTML = '';
    const npcs = town.npcs || [];
    if (npcCountBadge) {
      npcCountBadge.innerText = npcs.length > 0 ? `(${npcs.length})` : '';
    }

    if (npcs.length === 0) {
      npcsContainer.innerHTML = '<div style="font-size:12px; color:#64748b; padding:8px 4px;">(周圍暫無其他生靈)</div>';
    } else {
      npcs.forEach(npc => {
        const card = document.createElement('div');
        card.className = 'npc-card';

        const caps = npc.capabilities || [];
        const isHostile = caps.some(c => (typeof c === 'string' ? c === 'FIGHT' : c && c.type === 'FIGHT'));
        if (isHostile) card.classList.add('is-hostile');

        const rank = npc.rank || 'NORMAL';
        let rankBadge = '';
        if (rank === 'BOSS') {
          rankBadge = `<span style="font-size:10px; color:#f59e0b; background:rgba(245,158,11,0.2); border:1px solid #f59e0b; padding:1px 6px; border-radius:3px; margin-left:4px;">👑 首領</span>`;
        } else if (rank === 'ELITE') {
          rankBadge = `<span style="font-size:10px; color:#38bdf8; background:rgba(56,189,248,0.2); border:1px solid #38bdf8; padding:1px 6px; border-radius:3px; margin-left:4px;">⭐ 精英</span>`;
        }

        const roleHtml = isHostile
          ? `<span style="font-size:10px; color:#f87171; background:rgba(239,68,68,0.2); border:1px solid #ef4444; padding:1px 6px; border-radius:3px;">敵對</span>`
          : `<span style="font-size:11px; color:#94a3b8;">${npc.title || ''}</span>`;
        const header = document.createElement('div');
        header.className = 'npc-header';
        header.innerHTML = `<span class="npc-name">${npc.name}</span>${rankBadge}${roleHtml}`;
        card.appendChild(header);

        if (npc.description) {
          const desc = document.createElement('div');
          desc.className = 'npc-desc';
          desc.innerText = npc.description;
          card.appendChild(desc);
        }

        const chipsWrap = document.createElement('div');
        chipsWrap.className = 'capability-chips';

        caps.forEach(cap => {
          const norm = normalizeTownCapability(cap, npc);
          const chip = document.createElement('button');
          chip.className = `cap-chip cap-${norm.type}`;
          chip.innerHTML = `<span>${norm.icon}</span><span>${norm.label}</span>`;
          chip.onclick = () => {
            if (norm.command) {
              send(norm.command);
            }
          };
          chipsWrap.appendChild(chip);
        });

        card.appendChild(chipsWrap);
        npcsContainer.appendChild(card);
      });
    }
  }

  // 3. 地面物品渲染
  const itemsContainer = document.getElementById('town-items-list');
  const itemCountBadge = document.getElementById('town-item-count');
  if (itemsContainer) {
    itemsContainer.innerHTML = '';
    const items = town.items || [];
    if (itemCountBadge) {
      itemCountBadge.innerText = items.length > 0 ? `(${items.length})` : '';
    }
    if (items.length === 0) {
      itemsContainer.innerHTML = '<div style="font-size:12px; color:#64748b; padding:4px;">(地面空無一物)</div>';
    } else {
      items.forEach(it => {
        const chip = document.createElement('button');
        chip.className = 'ground-item-chip';
        const name = it.name || '物品';
        const isChest = name.includes('寶箱') || name.includes('棺槨') || name.includes('秘寶');
        const isPouch = name.includes('儲物袋') || name.includes('包裹') || (it.type === 'CONTAINER');

        if (isChest) {
          chip.classList.add('is-chest');
          chip.innerHTML = `<span>📦</span> 開啟 ${name}`;
        } else if (isPouch) {
          chip.classList.add('is-loot-pouch');
          chip.innerHTML = `<span>👝</span> 搜刮 ${name}`;
        } else {
          const count = it.count || it.amount || 1;
          const countStr = count > 1 ? ` x${count}` : '';
          chip.innerHTML = `<span>📦</span> 拾取 ${name}${countStr}`;
        }

        chip.onclick = () => {
          if (it.actionCommand) send(it.actionCommand);
          else send('get ' + it.id);
        };
        itemsContainer.appendChild(chip);
      });
    }
  }
}

/**
 * 輔助方法：依符號取得地塊樣式與圖標
 */
function populateTileAppearance(cell, symbol, gx, gy) {
  switch (symbol) {
    case '#':
      cell.classList.add('cell-wall');
      cell.innerText = '█';
      cell.title = `青岡石壁 (${gx}, ${gy})`;
      break;
    case '+':
      cell.classList.add('cell-door');
      cell.innerText = '☩';
      cell.title = `封印石門 (${gx}, ${gy})`;
      break;
    case '$':
    case '◆':
      cell.classList.add('cell-chest');
      cell.innerText = '◆';
      cell.title = `古仙棺槨寶箱 (${gx}, ${gy})`;
      break;
    case '◇':
      cell.classList.add('cell-chest-opened');
      cell.innerText = '◇';
      cell.title = `已開啟棺槨 (${gx}, ${gy})`;
      break;
    case '^':
      cell.classList.add('cell-trap');
      cell.innerText = '▲';
      cell.title = `深淵黏液陷阱 (${gx}, ${gy})`;
      break;
    case '>':
      cell.classList.add('cell-stairs');
      cell.innerText = '▼';
      cell.title = `通往下層石階 (${gx}, ${gy})`;
      break;
    case '.':
    default:
      cell.classList.add('cell-path');
      cell.innerText = '·';
      cell.title = `墓道通路 (${gx}, ${gy})`;
      break;
  }
}

/**
 * 渲染地牢雷達與前方視野 (支援隊伍居中跟隨相機)
 */
function renderMinimap(dungeon) {
  const gridContainer = document.getElementById('drpg-grid');
  const infoHeader = document.getElementById('drpg-floor-info');
  const forwardInspect = document.getElementById('drpg-forward-inspect');

  if (!gridContainer) return;

  if (infoHeader) {
    const dirMap = { 'NORTH': '北', 'EAST': '東', 'SOUTH': '南', 'WEST': '西' };
    const dirCn = dirMap[dungeon.direction] || dungeon.direction;

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

  if (drpgState.radarMode === 'centered') {
    // 視口居中模式 (半徑 4，共 9x9 視口，玩家隊伍永遠固定在中心 [4, 4])
    const radius = 4;
    const viewCols = radius * 2 + 1;
    gridContainer.style.gridTemplateColumns = `repeat(${viewCols}, 1fr)`;
    gridContainer.style.gridTemplateRows = `repeat(${viewCols}, 1fr)`;

    for (let dy = -radius; dy <= radius; dy++) {
      for (let dx = -radius; dx <= radius; dx++) {
        const gx = dungeon.x + dx;
        const gy = dungeon.y + dy;
        const cell = document.createElement('div');
        cell.className = 'grid-cell';

        const isPlayer = (dx === 0 && dy === 0);
        const isFront = (gx === fx && gy === fy);

        if (isPlayer) {
          cell.classList.add('cell-player');
          cell.innerText = dungeon.directionArrow || '▲';
          cell.title = `小隊所在 (${dungeon.x}, ${dungeon.y})`;
        } else if (gx < 0 || gx >= w || gy < 0 || gy >= h) {
          // 迷宮外緣幽冥禁制邊界
          cell.classList.add('cell-void');
          cell.innerText = '╳';
          cell.title = '古塚外緣禁制結界';
        } else {
          const isVisited = dungeon.visited && dungeon.visited[gy] && dungeon.visited[gy][gx];
          const symbol = (dungeon.tiles && dungeon.tiles[gy]) ? dungeon.tiles[gy][gx] : '#';

          if (isFront) {
            cell.classList.add('cell-target-front');
          }
          if (!isVisited) {
            cell.classList.add('cell-fog');
            cell.innerText = '░';
            cell.title = `未探知迷霧 (${gx}, ${gy})`;
          } else {
            populateTileAppearance(cell, symbol, gx, gy);
          }
        }
        gridContainer.appendChild(cell);
      }
    }
  } else {
    // 全圖模式 (固定 10x10)
    gridContainer.style.gridTemplateColumns = `repeat(${w}, 1fr)`;
    gridContainer.style.gridTemplateRows = `repeat(${h}, 1fr)`;

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
            cell.title = `未探知迷霧 (${x}, ${y})`;
          } else {
            populateTileAppearance(cell, symbol, x, y);
          }
        }
        gridContainer.appendChild(cell);
      }
    }
  }
}

/**
 * 渲染 6 人小隊 HUD (包含前後衛分色、三態能量條及點選觸發技能盤)
 */
function renderPartyHud(party) {
  const partyContainer = document.getElementById('party-members-list');
  const formNameEl = document.getElementById('formation-name-badge') || document.querySelector('.formation-name');
  const energyFillEl = document.getElementById('formation-energy-fill') || document.getElementById('formation-bar');
  const energyValEl = document.getElementById('formation-energy-val') || document.querySelector('.formation-energy-val');
  const ultBtn = document.getElementById('formation-ult-btn') || document.getElementById('ult-btn');

  // 更新頂部道門陣法與靈威狀態
  if (party.formationName && formNameEl) {
    formNameEl.innerText = `☯️ 陣法：【${party.formationName}】`;
  }
  if (party.formationEnergy !== undefined) {
    if (energyFillEl) energyFillEl.style.width = `${party.formationEnergy}%`;
    if (energyValEl) energyValEl.innerText = `靈威: ${party.formationEnergy}/100`;

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

    let madnessCardClass = '';
    let madnessExtraHtml = '';

    if (m.madnessState === 'CHAOS') {
      madnessCardClass = ' card-chaos';
      madnessExtraHtml = `
        <div class="mini-bar-wrap aberration-bar-wrap" title="⚠️ 走火入魔！異變進度 ${m.aberrationCounter}% (滿 100% 變為古神畸變 Boss！每回合自殘 15% HP，40% 背刺隊友！)">
          <div class="mini-bar aberration-fill" style="width: ${m.aberrationCounter}%"></div>
          <span class="mini-val aberration-val">⚠️ 異變 ${m.aberrationCounter}%</span>
        </div>
        <div class="chaos-action-row">
          <button class="seal-trigger-btn" onclick="event.stopPropagation(); send('seal ${idx}')" title="施展太上鎮魔符，凍結異變計數，暫停行動">🔒 施符鎮魔</button>
        </div>
      `;
    } else if (m.madnessState === 'SEALED') {
      madnessCardClass = ' card-sealed';
      madnessExtraHtml = `
        <div class="sealed-status-badge" title="已被太上鎮魔符鎮壓靈台，異變凍結在 ${m.aberrationCounter}%，定身無法行動">
          🔒 已施符鎮魔 (${m.aberrationCounter}%)
        </div>
      `;
    } else if (m.madnessState === 'DEAD_MEAT') {
      madnessCardClass = ' card-dead-meat';
      madnessExtraHtml = `
        <div class="dead-status-badge" title="肉身承受不住煞氣崩解為一灘死肉，永久陣亡">
          💀 血肉崩解・身殞
        </div>
      `;
    } else if (m.madnessState === 'ABERRATION') {
      madnessCardClass = ' card-aberration';
      madnessExtraHtml = `
        <div class="aberration-status-badge" title="已化為域外古神畸變體！">
          👁️ 畸變成魔
        </div>
      `;
    }

    card.className = `party-card row-${m.row.toLowerCase()}${isSelected ? ' selected-card' : ''}${madnessCardClass}`;
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

    // 7 大部位裝備槽位 (5 基礎 + 2 飾品)
    const allSlots = [
      { key: 'MAIN_HAND', alias: 'weapon', label: '主手', icon: '🗡️', defaultName: '空主' },
      { key: 'OFF_HAND', alias: 'shield', label: '副手', icon: '🛡️', defaultName: '空副' },
      { key: 'HEAD', alias: 'head', label: '頭部', icon: '👑', defaultName: '空頭' },
      { key: 'BODY', alias: 'armor', label: '身軀', icon: '🥋', defaultName: '空身' },
      { key: 'FEET', alias: 'feet', label: '靴履', icon: '👢', defaultName: '空履' },
      { key: 'ACCESSORY_1', alias: 'acc1', label: '飾品1', icon: '💍', defaultName: '空飾1' },
      { key: 'ACCESSORY_2', alias: 'acc2', label: '飾品2', icon: '📿', defaultName: '空飾2' }
    ];

    let equipHtml = '';
    for (const slot of allSlots) {
      let item = m.equipment ? m.equipment[slot.key] : null;
      if (!item && slot.key === 'MAIN_HAND' && m.equippedWeapon) item = m.equippedWeapon;
      if (!item && slot.key === 'BODY' && m.equippedArmor) item = m.equippedArmor;

      if (item) {
        const btn = `<button class="unequip-mini-btn" onclick="event.stopPropagation(); send('item unequip ${slot.alias} ${idx}')" title="卸下${slot.label}【${item.name}】放回行囊">✕</button>`;
        equipHtml += `<span class="equip-tag equipped-slot" title="${slot.label}: ${item.name} (${item.description || ''})">${item.icon || slot.icon} ${item.name}${btn}</span>`;
      } else {
        equipHtml += `<span class="equip-tag empty-slot" title="${slot.label} (未穿戴)">${slot.icon}${slot.defaultName}</span>`;
      }
    }

    card.innerHTML = `
      <div class="card-info-side">
        <div class="card-name-row">
          <span class="member-idx">#${idx + 1}</span>
          <span class="member-name" title="${m.name}">${m.name}</span>
          <span class="member-level-badge" title="境界等級 Lv.${m.level || 1}">Lv.${m.level || 1}</span>
          <span class="member-row badge-${m.row.toLowerCase()}">${rowBadge}</span>
          ${m.className ? `<span class="member-class-badge" title="${m.classDescription || ''}" style="font-size:9px;color:#7dd3fc;background:#0f172a;border:1px solid #0284c7;border-radius:3px;padding:0 3px;">${m.className}</span>` : ''}
          ${(idx === 0 && m.freeStatPoints > 0) ? `<span class="hud-free-points-pill" title="尚有 ${m.freeStatPoints} 點未分配自由點數！點擊開啟配點" onclick="event.stopPropagation(); openPartyModal(0);">+${m.freeStatPoints}點</span>` : ''}
        </div>
        <div class="member-title" title="${m.roleTitle}">${m.roleTitle}</div>
        <div class="member-equip-row">
          ${equipHtml}
        </div>
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
        ${madnessExtraHtml}
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
/**
 * 渲染所選隊員的技能抽屜 (Skill Drawer，具備 DOM 複用與 In-place 更新，徹底根除 hover 閃爍與點擊丟失)
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

  const skills = m.skills || [];
  const currentMemberIdxStr = String(drpgState.selectedMemberIdx);
  const container = drawer.querySelector('.drawer-skills-container');

  // 若尚未初始化骨架，或切換了隊員，或技能數量改變，則重新建立骨架
  const needFullRebuild = !container || 
      drawer.dataset.memberIdx !== currentMemberIdxStr || 
      drawer.dataset.skillCount !== String(skills.length);

  if (needFullRebuild) {
    drawer.dataset.memberIdx = currentMemberIdxStr;
    drawer.dataset.skillCount = String(skills.length);

    let skillsHtml = '';
    if (skills.length === 0) {
      skillsHtml = '<span style="color:#9ca3af;font-size:12px;">該成員專注於自動平砍，暫無主動奧義。</span>';
    } else {
      skillsHtml = skills.map((s, sIdx) => {
        return `
          <button class="skill-btn" data-skill-idx="${sIdx}" data-skill-id="${s.id}">
            <div class="skill-btn-title-row">
              <span class="skill-btn-name">${s.icon || '⚡'} ${s.name}</span>
              <span class="skill-btn-cost"></span>
            </div>
            <div class="skill-btn-desc">${s.description}</div>
            <div class="skill-cd-overlay hidden"></div>
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
  } else {
    // 隊員與技能樹未變，僅 In-place 更新數值與狀態，不銷毀 DOM 節點
    const resBadge = drawer.querySelector('.drawer-resource-badge');
    if (resBadge) {
      resBadge.className = `drawer-resource-badge ${badgeCls}`;
      resBadge.textContent = `${badgeIcon} ${curRes}/${maxRes}`;
    }
  }

  // 對各個按鈕進行 In-place 狀態更新 (Class, 點擊事件, 剩餘冷卻, 能量消耗)
  if (skills.length > 0) {
    const btnNodes = drawer.querySelectorAll('.drawer-skills-container .skill-btn');
    skills.forEach((s, sIdx) => {
      const btn = btnNodes[sIdx];
      if (!btn) return;

      const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
      const onCd = s.remainingCooldownMs > 0;
      let resOk = s.available;
      if (resOk === undefined) {
        resOk = (curRes >= (s.costValue || 0));
      }
      const canCast = Boolean(resOk && !onCd && isAlive);
      const cdSec = onCd ? (s.remainingCooldownMs / 1000).toFixed(1) : 0;

      let costTagClass = 'res-badge-mp';
      if (s.costType === 'RAGE') costTagClass = 'res-badge-rage';
      else if (s.costType === 'COMBO') costTagClass = 'res-badge-combo';

      let costLabel = s.costDescription;
      if (!costLabel || costLabel === 'undefined') {
        if (!s.costValue || s.costValue <= 0) {
          costLabel = '無消耗';
        } else if (s.costType === 'RAGE') {
          costLabel = `${s.costValue} 怒氣`;
        } else if (s.costType === 'COMBO') {
          costLabel = `${s.costValue} 連擊`;
        } else {
          costLabel = `${s.costValue} 真元`;
        }
      }

      // 更新按鈕 class (絕不替換元素節點)
      btn.className = `skill-btn ${canCast ? '' : 'cant-cast'}`;
      btn.title = `${s.name} - ${s.description}${!canCast ? ' (點擊查看限制)' : ''}`;

      const costSpan = btn.querySelector('.skill-btn-cost');
      if (costSpan) {
        costSpan.className = `skill-btn-cost ${costTagClass}`;
        costSpan.textContent = costLabel;
      }

      const cdOverlay = btn.querySelector('.skill-cd-overlay');
      if (cdOverlay) {
        if (onCd) {
          cdOverlay.classList.remove('hidden');
          cdOverlay.textContent = `⌛ ${cdSec}s`;
        } else {
          cdOverlay.classList.add('hidden');
        }
      }

      // 綁定最新狀態點擊
      btn.onclick = () => {
        onSkillBtnClick(drpgState.selectedMemberIdx, s.id, canCast, s.name, costLabel, onCd, cdSec, s.costDescription || '');
      };
    });
  }
}

/**
 * 點擊隊員技能按鈕 (若不可用則給予戰術原因提示，絕不靜默無效)
 */
function onSkillBtnClick(memberIdx, skillId, canCast, skillName, costLabel, onCd, cdSec, costDesc) {
  if (!canCast) {
    let warnMsg = '';
    if (onCd) {
      warnMsg = `⏳【調息中】「${skillName}」正在調息冷卻中，尚需 ${cdSec} 秒！`;
    } else if (costDesc && costDesc.includes('需')) {
      warnMsg = `⚠️【兵刃未備】「${skillName}」${costDesc}！當前未佩戴合適兵刃（或處於赤手狀態）。請在小隊面板 (P) 佩戴對應兵刃。`;
    } else {
      warnMsg = `⚠️【元氣未備】「${skillName}」釋放條件不足（需 ${costLabel}）！請在戰鬥中累積足夠點數後再施展。`;
    }
    if (typeof appendHtml === 'function') {
      appendHtml(warnMsg, '#f59e0b');
    }
    const focusHint = document.getElementById('battle-focus-hint');
    if (focusHint) {
      const prevHtml = focusHint.innerHTML;
      focusHint.innerHTML = `<span style="color:#f59e0b; font-weight:bold;">${warnMsg}</span>`;
      setTimeout(() => {
        if (focusHint && drpgState.lastBattle) {
          renderBattleArena(drpgState.lastBattle);
        }
      }, 3500);
    }
    return;
  }
  castPartySkill(memberIdx, skillId);
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
 * 切換隊伍行囊抽屜
 */
function toggleBagDrawer(show) {
  const drawer = document.getElementById('bag-drawer');
  if (!drawer) return;
  if (show === undefined) {
    drpgState.isBagDrawerOpen = !drpgState.isBagDrawerOpen;
  } else {
    drpgState.isBagDrawerOpen = !!show;
  }

  if (drpgState.isBagDrawerOpen) {
    drawer.classList.remove('hidden');
    renderBagDrawer();
  } else {
    drawer.classList.add('hidden');
  }
}

/**
 * 渲染隊伍行囊內容
 */
function renderBagDrawer() {
  const drawer = document.getElementById('bag-drawer');
  const countEl = document.getElementById('bag-capacity-badge');
  const listEl = document.getElementById('bag-items-list');
  if (!drawer || !listEl) return;

  const party = drpgState.lastParty;
  const inv = party ? party.inventory : null;
  const members = party ? party.members : [];

  const slots = (inv && inv.slots) ? inv.slots : [];
  const capacity = inv ? inv.capacity : 30;

  if (countEl) {
    countEl.innerText = `${slots.length}/${capacity}`;
  }

  listEl.innerHTML = '';
  if (slots.length === 0) {
    listEl.innerHTML = '<div class="bag-empty-notice">🎒 行囊空空如也，探索墓塚或斬除妖邪可獲取法寶與靈丹！</div>';
    return;
  }

  slots.forEach((item, slotIdx) => {
    const card = document.createElement('div');
    const qualityCls = `quality-${(item.quality || 'COMMON').toLowerCase()}`;
    card.className = `bag-item-card ${qualityCls}`;

    let effectDesc = '';
    if (item.weapon) {
      effectDesc = `🗡️ 攻擊力 +${item.bonusMinDamage}~${item.bonusMaxDamage}`;
    } else if (item.armor) {
      effectDesc = `🥋 防禦 +${item.bonusDefense}, 氣血 +${item.bonusHp}`;
    } else if (item.effectType === 'HEAL_HP') {
      effectDesc = `🌿 服用回復 ${item.effectValue} HP`;
    } else if (item.effectType === 'RESTORE_SAN') {
      effectDesc = `📜 服用回復 ${item.effectValue} SAN (解走火入魔)`;
    } else if (item.effectType === 'LEARN_SKILL') {
      effectDesc = `🧬 煉化領悟絕學【${item.grantedSkillName || '道種'}】`;
    }

    // 構造快捷操作按鍵 (對 6 位隊員)
    let actionButtons = '';
    if (item.consumable) {
      const btns = members.map((m, mIdx) => {
        const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
        return `<button class="item-act-mini-btn btn-use" ${isAlive ? '' : 'disabled'}
          onclick="send('use ${item.slotId} ${mIdx}')" title="為 #${mIdx + 1} ${m.name} 使用">
          #${mIdx + 1} ${m.name.slice(0, 3)}
        </button>`;
      }).join('');
      actionButtons += `
        <div class="item-action-row">
          <span class="action-type-label">使用/服用:</span>
          <div class="action-targets-grid">${btns}</div>
        </div>
      `;
    }

    if (item.weapon || item.armor || item.equipment || item.equipSlot || item.itemType === 'WEAPON' || item.itemType === 'ARMOR' || item.itemType === 'SHIELD' || item.itemType === 'ACCESSORY') {
      const btns = members.map((m, mIdx) => {
        const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
        return `<button class="item-act-mini-btn btn-equip" ${isAlive ? '' : 'disabled'}
          onclick="send('equip ${item.slotId} ${mIdx}')" title="為 #${mIdx + 1} ${m.name} 穿戴">
          #${mIdx + 1} ${m.name.slice(0, 3)}
        </button>`;
      }).join('');
      actionButtons += `
        <div class="item-action-row">
          <span class="action-type-label">穿戴裝備:</span>
          <div class="action-targets-grid">${btns}</div>
        </div>
      `;
    }

    card.innerHTML = `
      <div class="bag-item-top">
        <span class="bag-item-icon">${item.icon || '📦'}</span>
        <div class="bag-item-meta">
          <div class="bag-item-name-line">
            <span class="bag-item-title">${item.name}</span>
            <span class="bag-item-qty">x${item.count}</span>
          </div>
          <div class="bag-item-effect">${effectDesc}</div>
        </div>
      </div>
      <div class="bag-item-desc">${item.description || ''}</div>
      ${actionButtons ? `<div class="bag-item-actions">${actionButtons}</div>` : ''}
    `;

    listEl.appendChild(card);
  });
}

/**
 * 渲染戰場敵方陣列與主舞台交鋒態勢 (Battle Arena Main Stage)
 */
function renderBattleArena(battle) {
  const panel = document.getElementById('battle-arena-panel');
  const enemiesBox = document.getElementById('battle-enemies-container');
  const badge = document.getElementById('battle-status-badge');
  const countBadge = document.getElementById('battle-enemies-count');
  const focusNameEl = document.getElementById('battle-focus-name');

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

  const enemies = battle.enemies || [];
  if (countBadge) {
    countBadge.innerText = `${enemies.length} 體`;
  }

  // 尋找當前鎖定集火目標 (優先以 battle.selectedTargetIndex 或 isTarget 判定)
  let selectedEnemy = null;
  if (battle.selectedTargetIndex !== undefined && battle.selectedTargetIndex >= 0) {
    selectedEnemy = enemies.find(e => e.index === battle.selectedTargetIndex && e.alive);
  }
  if (!selectedEnemy) {
    selectedEnemy = enemies.find(e => (e.isTarget || e.isSelectedTarget) && e.alive);
  }
  if (!selectedEnemy) {
    // 預設鎖定前排第一個存活者，或任意存活者
    selectedEnemy = enemies.find(e => e.row === 'FRONT' && e.alive) || enemies.find(e => e.alive);
  }

  if (focusNameEl) {
    if (selectedEnemy) {
      focusNameEl.innerText = `#${selectedEnemy.index + 1} ${selectedEnemy.name} (${selectedEnemy.row === 'FRONT' ? '前衛' : '後衛'})`;
    } else {
      focusNameEl.innerText = '無（全體覆滅）';
    }
  }

  enemiesBox.innerHTML = '';
  enemies.forEach(e => {
    const card = document.createElement('div');
    const isTarget = (selectedEnemy && selectedEnemy.index === e.index) || e.isTarget || (battle.selectedTargetIndex === e.index);
    const isAlive = e.alive;
    const rowClass = (e.row || 'FRONT').toLowerCase();

    // 支援敵人體型自訂網格跨欄跨行 (預設 1/5 格，Boss/巨獸可自訂佔 2 欄或多行)
    const colSpan = e.colSpan || (e.rank === 'BOSS' ? 2 : 1);
    card.style.gridColumn = `span ${colSpan}`;
    if (e.rowSpan && e.rowSpan > 1) {
      card.style.gridRow = `span ${e.rowSpan}`;
    }

    card.className = `enemy-card row-${rowClass}${isTarget && isAlive ? ' selected-target' : ''}${!isAlive ? ' dead' : ''}`;
    card.title = isAlive ? `點擊鎖定【${e.name}】為集火目標` : '已伏誅';
    card.onclick = () => {
      if (isAlive) {
        selectBattleTarget(e.index);
      }
    };

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

  if (drpgState.lastParty) {
    renderBattlePartyQuickBar(drpgState.lastParty);
  }
}

/**
 * 渲染戰場我方隊員快速資訊列 (Battle Party Quick Bar)
 */
function renderBattlePartyQuickBar(party) {
  const bar = document.getElementById('battle-party-quick-bar');
  if (!bar || !party || !party.members) return;

  const cards = bar.querySelectorAll('.battle-party-mini-card');
  if (cards.length !== party.members.length) {
    bar.innerHTML = party.members.map((m, idx) => {
      const isSelected = (drpgState.selectedMemberIdx === idx);
      const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
      const hpPct = Math.min(100, Math.max(0, (m.hp / m.maxHp) * 100));
      const rowBadge = m.row === 'FRONT' ? '前衛' : '後衛';

      return `
        <div class="battle-party-mini-card ${isSelected ? 'active-selected' : ''}" data-idx="${idx}"
          onclick="selectPartyMemberForSkill(${idx})" title="點擊展開 #${idx + 1} ${m.name} 的專屬技能盤">
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <span class="bpmc-name" style="font-weight:bold; color:#e2e8f0; font-size:12px;">#${idx + 1} ${m.name}</span>
            <span class="bpmc-badge" style="font-size:9px; color:${m.row === 'FRONT' ? '#f87171' : '#60a5fa'};">[${rowBadge}]</span>
          </div>
          <div class="enemy-hp-wrap" style="height:10px; margin:2px 0;">
            <div class="hp-bar" style="width:${hpPct}%; background:${isAlive ? '#10b981' : '#6b7280'}; height:100%;"></div>
          </div>
          <div style="font-size:9px; color:#94a3b8; display:flex; justify-content:space-between;">
            <span class="bpmc-hp-text">HP ${m.hp}/${m.maxHp}</span>
            <span style="color:#38bdf8; font-weight:bold;">⚡ 招式盤</span>
          </div>
        </div>
      `;
    }).join('');
  } else {
    party.members.forEach((m, idx) => {
      const card = cards[idx];
      if (!card) return;
      const isSelected = (drpgState.selectedMemberIdx === idx);
      const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
      const hpPct = Math.min(100, Math.max(0, (m.hp / m.maxHp) * 100));
      const rowBadge = m.row === 'FRONT' ? '前衛' : '後衛';

      card.className = `battle-party-mini-card ${isSelected ? 'active-selected' : ''}`;
      const nameEl = card.querySelector('.bpmc-name');
      if (nameEl) nameEl.textContent = `#${idx + 1} ${m.name}`;
      const badgeEl = card.querySelector('.bpmc-badge');
      if (badgeEl) {
        badgeEl.textContent = `[${rowBadge}]`;
        badgeEl.style.color = (m.row === 'FRONT' ? '#f87171' : '#60a5fa');
      }
      const hpBar = card.querySelector('.hp-bar');
      if (hpBar) {
        hpBar.style.width = `${hpPct}%`;
        hpBar.style.background = isAlive ? '#10b981' : '#6b7280';
      }
      const hpText = card.querySelector('.bpmc-hp-text');
      if (hpText) hpText.textContent = `HP ${m.hp}/${m.maxHp}`;
    });
  }
}

/**
 * 隱藏戰場主舞台
 */
function hideBattleArena() {
  const panel = document.getElementById('battle-arena-panel');
  if (panel) {
    panel.classList.add('hidden');
  }
}

/**
 * 鎖定集火目標 (具備本地即時準星反饋)
 */
function selectBattleTarget(idx) {
  send(`battle target ${idx}`);

  // 即時樂觀更新目標鎖定
  if (drpgState.lastBattle) {
    drpgState.lastBattle.selectedTargetIndex = idx;
    if (drpgState.lastBattle.enemies) {
      drpgState.lastBattle.enemies.forEach(e => {
        e.isTarget = (e.index === idx);
        e.isSelectedTarget = (e.index === idx);
      });
    }
    renderBattleArena(drpgState.lastBattle);
  }
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
  togglePartyModal();
  send('party');
}

/**
 * 開啟 / 關閉小隊 5+2 裝備與隊伍編制管理面板
 */
function togglePartyModal(forceOpen) {
  const modal = document.getElementById('party-modal');
  if (!modal) return;

  if (forceOpen === undefined) {
    drpgState.isPartyModalOpen = !drpgState.isPartyModalOpen;
  } else {
    drpgState.isPartyModalOpen = !!forceOpen;
  }

  if (drpgState.isPartyModalOpen) {
    modal.classList.remove('hidden');
    renderPartyModal();
  } else {
    modal.classList.add('hidden');
    if (document.activeElement) {
      document.activeElement.blur();
    }
  }
}

function openPartyModal(memberIdx) {
  if (typeof memberIdx === 'number') {
    drpgState.selectedModalMemberIdx = memberIdx;
  }
  togglePartyModal(true);
}

function closePartyModal() {
  togglePartyModal(false);
  if (document.activeElement) {
    document.activeElement.blur();
  }
}

function selectPartyModalMember(idx) {
  drpgState.selectedModalMemberIdx = idx;
  drpgState.isAddingTactics = false;
  renderPartyModal();
}

function selectPartyFormationTab() {
  drpgState.selectedModalMemberIdx = 'FORMATION';
  drpgState.isAddingTactics = false;
  renderPartyModal();
}

function switchPartyModalSubTab(tab) {
  drpgState.partyModalSubTab = tab;
  drpgState.isAddingTactics = false;
  renderPartyModal();
}

function toggleAddTacticsForm(show) {
  drpgState.isAddingTactics = (show !== undefined) ? !!show : !drpgState.isAddingTactics;
  renderPartyModal();
}

function handleTacticsCondChange() {
  const condEl = document.getElementById('t-builder-cond');
  const valEl = document.getElementById('t-builder-val');
  const valLabel = document.getElementById('t-builder-val-label');
  if (!condEl || !valEl) return;
  const cond = condEl.value;
  if (cond === 'ALWAYS' || cond === 'ENEMY_IS_BOSS') {
    valEl.style.display = 'none';
    if (valLabel) valLabel.style.display = 'none';
  } else {
    valEl.style.display = 'inline-block';
    if (valLabel) valLabel.style.display = 'inline-block';
    if (cond === 'ALLY_HP_LESS_THAN' || cond === 'SELF_HP_LESS_THAN') {
      if (valLabel) valLabel.innerText = '氣血 (%):';
      valEl.value = 50;
    } else if (cond === 'RESOURCE_GTE') {
      if (valLabel) valLabel.innerText = '資源 (點):';
      valEl.value = 30;
    } else if (cond === 'ENEMY_COUNT_GTE') {
      if (valLabel) valLabel.innerText = '數量 (體):';
      valEl.value = 2;
    }
  }
}

function submitAddTactics(memberIdx) {
  const prioEl = document.getElementById('t-builder-prio');
  const condEl = document.getElementById('t-builder-cond');
  const valEl = document.getElementById('t-builder-val');
  const targetEl = document.getElementById('t-builder-target');
  const skillEl = document.getElementById('t-builder-skill');

  if (!prioEl || !condEl || !targetEl || !skillEl) return;
  const prio = parseInt(prioEl.value) || 1;
  const cond = condEl.value;
  const val = parseInt(valEl ? valEl.value : 0) || 0;
  const target = targetEl.value;
  const skillId = skillEl.value;

  send(`party tactics ${memberIdx} add ${prio} ${cond} ${val} ${target} ${skillId}`);
  drpgState.isAddingTactics = false;
}

function renderMemberTactics(m, idx) {
  if (idx === 0) {
    return `
      <div class="tactics-leader-box">
        <div class="tactics-leader-icon">👑</div>
        <div class="tactics-leader-title">隊長（道友親自操控）</div>
        <div class="tactics-leader-desc">
          主角為問道隊伍之核心領袖，戰鬥中所有普通攻擊、絕技道法、陣法奧義與行囊靈藥均由道友在戰場中即時親自下達指令，享有 100% 自由決策權，無需設定自動戰術方針。
        </div>
        <div class="tactics-leader-tip">
          💡 提示：點擊上方頁籤切換至同伴（如「鐵牛」、「凌霜」），即可為同伴設定專屬的 Gambit 戰鬥 AI 方針！
        </div>
      </div>
    `;
  }

  const tacticsList = m.tactics || [];
  const nextPriority = tacticsList.length > 0
    ? Math.max(...tacticsList.map(r => r.priority)) + 1
    : 1;

  let builderHtml = '';
  if (drpgState.isAddingTactics) {
    let skillOptionsHtml = `<option value="basic_attack">🗡️ 基礎普攻 (${m.basicSkillName || '普通攻擊'})</option>`;
    if (m.skills && m.skills.length > 0) {
      m.skills.forEach(s => {
        skillOptionsHtml += `<option value="${s.id}">⚡ ${s.name} (${s.costDescription || (s.costValue ? s.costValue + '消耗' : '絕技')})</option>`;
      });
    }
    if (m.availableStances && m.availableStances.length > 0) {
      m.availableStances.forEach(st => {
        skillOptionsHtml += `<option value="${st.skillId}">⚔️ ${st.skillName} (套路)</option>`;
      });
    }

    builderHtml = `
      <div class="tactics-builder-card">
        <div class="tactics-builder-title">➕ 新增戰術方針規則 (Gambit Rule)</div>
        <div class="tactics-builder-row">
          <label style="font-size:12px;color:#94a3b8;">優先級：</label>
          <input type="number" id="t-builder-prio" value="${nextPriority}" min="1" max="99" style="width:55px;" />

          <label style="font-size:12px;color:#94a3b8;">觸發條件：</label>
          <select id="t-builder-cond" onchange="handleTacticsCondChange()">
            <option value="ALLY_HP_LESS_THAN">隊友氣血低於 (%)</option>
            <option value="SELF_HP_LESS_THAN">自身氣血低於 (%)</option>
            <option value="RESOURCE_GTE">自身資源 >= (點/怒氣/連擊)</option>
            <option value="ENEMY_COUNT_GTE">敵方存活數量 >= (體)</option>
            <option value="ENEMY_IS_BOSS">敵方存在首領 (Boss)</option>
            <option value="ALWAYS">無條件施展 (必定觸發)</option>
          </select>

          <label id="t-builder-val-label" style="font-size:12px;color:#94a3b8;">閥值：</label>
          <input type="number" id="t-builder-val" value="50" min="0" max="9999" style="width:65px;" />
        </div>
        <div class="tactics-builder-row">
          <label style="font-size:12px;color:#94a3b8;">目標：</label>
          <select id="t-builder-target">
            <option value="LOWEST_HP_ALLY">氣血最低隊友</option>
            <option value="SELF">自身</option>
            <option value="CURRENT_ENEMY">當前集火目標</option>
            <option value="ALL_ENEMIES">全體敵怪</option>
            <option value="ALL_ALLIES">全體隊友</option>
          </select>

          <label style="font-size:12px;color:#94a3b8;">執行武學：</label>
          <select id="t-builder-skill">
            ${skillOptionsHtml}
          </select>
        </div>
        <div style="display:flex;gap:8px;margin-top:6px;">
          <button class="act-btn btn-green" type="button" onclick="submitAddTactics(${idx})">💾 確定新增規則</button>
          <button class="act-btn" type="button" onclick="toggleAddTacticsForm(false)">✕ 取消</button>
        </div>
      </div>
    `;
  }

  let rulesHtml = '';
  if (tacticsList.length === 0) {
    rulesHtml = '<div class="tactics-empty-hint">尚無設定戰術方針。該同伴在戰鬥中將默認執行基礎套路普通攻擊。</div>';
  } else {
    const sorted = [...tacticsList].sort((a, b) => a.priority - b.priority);
    rulesHtml = `
      <div class="tactics-rule-list">
        ${sorted.map(r => {
          const isAlways = (r.condition === 'ALWAYS');
          const isBoss = (r.condition === 'ENEMY_IS_BOSS');
          const valDisplay = (!isAlways && !isBoss) ? ` ${r.conditionValue}` : '';
          return `
            <div class="tactics-rule-row ${r.enabled ? 'enabled' : 'disabled'}">
              <div class="tactics-prio-badge">#${r.priority}</div>
              <div class="tactics-rule-desc">
                <span class="tactics-cond-tag">${r.conditionLabel}${valDisplay}</span>
                <span class="tactics-arrow">➜</span>
                <span class="tactics-target-tag">對 ${r.targetLabel}</span>
                <span class="tactics-arrow">➜</span>
                <span class="tactics-skill-tag">施展【${r.skillName}】</span>
              </div>
              <div class="tactics-rule-actions">
                <button class="act-btn btn-sm ${r.enabled ? 'btn-green' : 'btn-gray'}" type="button"
                  onclick="send('party tactics ${idx} toggle ${r.priority}')" title="點擊啟用或停用此規則">
                  ${r.enabled ? '🟢 啟用中' : '⚪ 已停用'}
                </button>
                <button class="act-btn btn-sm btn-red" type="button"
                  onclick="send('party tactics ${idx} delete ${r.priority}')" title="刪除此規則">
                  🗑️ 刪除
                </button>
              </div>
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  return `
    <div class="tactics-container">
      <div class="tactics-header-banner">
        <div>
          <div class="tactics-banner-title">🎯 同伴戰鬥方針設定 (Gambit AI)</div>
          <div class="tactics-banner-desc">戰鬥中輪到該同伴行動時，將依優先級序號 (#1, #2, #3...) 由上至下判定條件，首條滿足者即刻施展。</div>
        </div>
        <div class="tactics-banner-actions">
          <button class="act-btn btn-blue btn-sm" type="button" onclick="toggleAddTacticsForm(true)">➕ 新增方針</button>
          <button class="act-btn btn-sm" type="button" onclick="send('party tactics ${idx} reset')">🔄 重置預設</button>
          <button class="act-btn btn-red btn-sm" type="button" onclick="send('party tactics ${idx} clear')">🗑️ 清空方針</button>
        </div>
      </div>
      ${builderHtml}
      ${rulesHtml}
    </div>
  `;
}

/**
 * 渲染全隊共有的道門陣法奧義與站位配置視圖
 */
function renderTeamFormationView(party) {
  const isSymbols = Boolean(party.formationName && party.formationName.includes('四象'));
  const energy = party.formationEnergy || 0;
  const energyPct = Math.min(100, Math.max(0, energy));
  const ultName = party.ultimateSkillName || (isSymbols ? '四象封魔印' : '百鬼噬心');
  const inBattle = Boolean(drpgState.lastBattle && drpgState.lastBattle.inBattle);
  const canCast = Boolean(party.canCastUltimate || (energy >= 100 && inBattle));

  // 前排與後排成員劃分
  const frontMembers = [];
  const backMembers = [];
  (party.members || []).forEach((mem, idx) => {
    if (mem.row === 'FRONT') frontMembers.push({ mem, idx });
    else backMembers.push({ mem, idx });
  });

  const renderMemberSlot = (item) => {
    const { mem, idx } = item;
    const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
    const hpPct = Math.min(100, Math.max(0, (mem.hp / mem.maxHp) * 100));
    const rowClass = mem.row === 'FRONT' ? 'badge-front' : 'badge-back';
    const rowBadge = mem.row === 'FRONT' ? '前衛' : '後衛';
    const nextRow = mem.row === 'FRONT' ? '後衛' : '前衛';
    return `
      <div class="formation-slot-card" style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:10px;display:flex;flex-direction:column;gap:6px;">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <div style="display:flex;align-items:center;gap:6px;">
            <span style="color:#38bdf8;font-weight:bold;font-size:13px;">#${idx + 1} ${mem.name}</span>
            <span class="member-row ${rowClass}" style="font-size:10px;">[${rowBadge}]</span>
          </div>
          ${mem.className ? `<span style="font-size:10px;color:#7dd3fc;background:#1e293b;padding:1px 6px;border-radius:4px;">${mem.className}</span>` : ''}
        </div>
        <div style="font-size:11px;color:#94a3b8;">${mem.roleTitle || '同伴'}</div>
        <div style="font-size:11px;color:#f87171;display:flex;justify-content:space-between;">
          <span>❤️ 氣血: ${mem.hp}/${mem.maxHp}</span>
          <span>🧘 心: ${mem.san}/${mem.maxSan}</span>
        </div>
        <div style="width:100%;height:4px;background:#334155;border-radius:2px;overflow:hidden;">
          <div style="width:${hpPct}%;height:100%;background:${isAlive ? '#10b981' : '#ef4444'};"></div>
        </div>
        <div style="margin-top:4px;display:flex;gap:6px;">
          <button class="act-btn btn-sm" onclick="send('party switch ${idx}')" style="flex:1;padding:4px;font-size:11px;background:#1e293b;border:1px solid #475569;color:#cbd5e1;" title="將 #${idx + 1} ${mem.name} 切換至${nextRow}">
            🔄 調至${nextRow}
          </button>
          <button class="act-btn btn-sm" onclick="selectPartyModalMember(${idx})" style="padding:4px 8px;font-size:11px;background:#334155;border:1px solid #64748b;color:#f1f5f9;" title="檢視個人裝備與武學">
            👤 詳情
          </button>
        </div>
      </div>
    `;
  };

  const frontHtml = frontMembers.length > 0
    ? frontMembers.map(renderMemberSlot).join('')
    : '<div style="color:#64748b;font-size:12px;padding:12px;text-align:center;grid-column:1/-1;">(前衛空缺，後排將直接承受猛烈衝擊！)</div>';

  const backHtml = backMembers.length > 0
    ? backMembers.map(renderMemberSlot).join('')
    : '<div style="color:#64748b;font-size:12px;padding:12px;text-align:center;grid-column:1/-1;">(後衛空缺，全體在前線禦敵)</div>';

  return `
    <div class="team-formation-container" style="display:flex;flex-direction:column;gap:16px;">
      <!-- 1. 當前陣法與靈威奧義展示卡 -->
      <div style="background:linear-gradient(135deg, rgba(30,27,75,0.8), rgba(15,23,42,0.9));border:1px solid #6366f1;border-radius:8px;padding:16px;">
        <div style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;margin-bottom:12px;">
          <div>
            <div style="font-size:18px;font-weight:bold;color:#c084fc;display:flex;align-items:center;gap:8px;">
              <span>☯️ 小隊當前結成陣法：【${party.formationName || '四象辟邪陣'}】</span>
              <span style="font-size:11px;background:#4338ca;color:#e0e7ff;padding:2px 8px;border-radius:4px;">全隊陣眼常駐加持</span>
            </div>
            <div style="font-size:12px;color:#cbd5e1;margin-top:4px;">
              ${isSymbols ? '🛡️ 光環加成：全隊物理與法術承傷減免 15%，步步為營，道心穩如磐石。' : '💀 光環加成：全隊暴擊率 +20%，敵弱我強，擊殺時引導煞氣回饋全隊靈威。'}
            </div>
          </div>
          <div style="display:flex;gap:8px;">
            <button class="act-btn" onclick="send('formation toggle')" style="background:#4f46e5;color:#fff;padding:6px 14px;font-size:12px;border:none;border-radius:6px;cursor:pointer;font-weight:bold;">
              🔄 切換陣法 (F)
            </button>
          </div>
        </div>

        <!-- 陣法奧義與充能進度條 -->
        <div style="background:rgba(15,23,42,0.6);border:1px solid rgba(99,102,241,0.3);border-radius:6px;padding:12px;">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
            <span style="font-size:13px;font-weight:bold;color:#facc15;">⚡ 全隊終極奧義：【${ultName}】</span>
            <span style="font-size:12px;color:#94a3b8;">小隊靈威充能：<strong style="color:${energy >= 100 ? '#4ade80' : '#38bdf8'};">${energy}</strong> / 100</span>
          </div>
          <div style="width:100%;height:8px;background:#1e293b;border-radius:4px;overflow:hidden;margin-bottom:8px;">
            <div style="width:${energyPct}%;height:100%;background:linear-gradient(90deg, #6366f1, #a855f7, #f59e0b);transition:width 0.3s ease;"></div>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center;font-size:11px;color:#94a3b8;">
            <span>${isSymbols ? '效果：引青龍白虎朱雀玄武四相真靈鎮壓敵方全體，造成巨額靈能衝擊並大幅降低敵方攻擊。' : '效果：自九幽深淵引動萬千厲鬼撲殺敵陣，撕裂護甲並附加幽火流血持續重創。'}</span>
            ${canCast
              ? `<button class="act-btn btn-ult" onclick="send('formation cast')" style="padding:4px 12px;font-size:11px;font-weight:bold;">⚡ 施展陣法奧義 (U)</button>`
              : `<span style="color:#64748b;">(靈威滿 100 且戰鬥中方可施展)</span>`}
          </div>
        </div>
      </div>

      <!-- 2. 小隊戰鬥站位調配盤 (前衛 vs 後衛) -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
          <div>
            <div style="font-size:15px;font-weight:bold;color:#e2e8f0;">🛡️ 隊伍戰鬥站位編排 (前後排陣形)</div>
            <div style="font-size:11px;color:#94a3b8;margin-top:2px;">前衛優先承受近戰普攻與敵方仇恨；後衛受前衛掩護減免 20% 物理傷害。點擊即可自由調動站位。</div>
          </div>
          <div style="font-size:11px;color:#cbd5e1;background:#0f172a;padding:4px 10px;border-radius:4px;">
            總人數：${(party.members || []).length} / 5
          </div>
        </div>

        <!-- 前衛排 -->
        <div style="margin-bottom:14px;">
          <div style="font-size:12px;font-weight:bold;color:#f87171;margin-bottom:8px;display:flex;align-items:center;gap:6px;">
            <span>⚔️ 前衛戰鬥線 (Front Row)</span>
            <span style="font-size:10px;color:#94a3b8;font-weight:normal;">- 承傷核心、反擊與拉怪第一線</span>
          </div>
          <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(180px, 1fr));gap:10px;">
            ${frontHtml}
          </div>
        </div>

        <!-- 後衛排 -->
        <div>
          <div style="font-size:12px;font-weight:bold;color:#60a5fa;margin-bottom:8px;display:flex;align-items:center;gap:6px;">
            <span>🏹 後衛支援線 (Back Row)</span>
            <span style="font-size:10px;color:#94a3b8;font-weight:normal;">- 遠程輸出、術法引導與回血輔佐</span>
          </div>
          <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(180px, 1fr));gap:10px;">
            ${backHtml}
          </div>
        </div>
      </div>

      <!-- 3. 道門可用陣法典籍庫 -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;">
        <div style="font-size:15px;font-weight:bold;color:#e2e8f0;margin-bottom:10px;">📜 道門陣法典籍庫</div>
        <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(300px, 1fr));gap:12px;">
          <!-- 四象辟邪陣 -->
          <div style="background:#0f172a;border:1px solid ${isSymbols ? '#38bdf8' : '#334155'};border-radius:8px;padding:12px;display:flex;flex-direction:column;gap:8px;">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <span style="font-weight:bold;color:#38bdf8;font-size:14px;">☯️ 《四象辟邪陣》</span>
              ${isSymbols
                ? '<span style="font-size:11px;color:#34d399;font-weight:bold;">✔ 當前運轉中</span>'
                : '<button class="act-btn btn-sm" onclick="send(\'formation equip formation_four_symbols\')" style="padding:3px 10px;font-size:11px;background:#0284c7;color:#fff;">結成此陣</button>'}
            </div>
            <div style="font-size:11px;color:#cbd5e1;">正統道門防禦大陣。四相真靈護體，隊伍受到物理與法術傷害減免 15%，道心守護，步步為營。</div>
            <div style="font-size:11px;color:#facc15;">專屬奧義：【四象封魔印】（群體靈能重創 + 敵方全體弱化）</div>
          </div>

          <!-- 玄陰噬魂陣 -->
          <div style="background:#0f172a;border:1px solid ${!isSymbols ? '#a855f7' : '#334155'};border-radius:8px;padding:12px;display:flex;flex-direction:column;gap:8px;">
            <div style="display:flex;justify-content:space-between;align-items:center;">
              <span style="font-weight:bold;color:#c084fc;font-size:14px;">💀 《玄陰噬魂陣》</span>
              ${!isSymbols
                ? '<span style="font-size:11px;color:#34d399;font-weight:bold;">✔ 當前運轉中</span>'
                : '<button class="act-btn btn-sm" onclick="send(\'formation equip formation_xuan_yin\')" style="padding:3px 10px;font-size:11px;background:#7e22ce;color:#fff;">結成此陣</button>'}
            </div>
            <div style="font-size:11px;color:#cbd5e1;">太陰古墓禁忌凶陣。引動九幽星煞，隊伍暴擊率 +20%，敵弱我強，斬殺強敵反哺靈威。</div>
            <div style="font-size:11px;color:#facc15;">專屬奧義：【百鬼噬心】（幽冥群體穿甲撕裂 + 流血重創）</div>
          </div>
        </div>
      </div>
    </div>
  `;
}

function renderPartyModal() {
  const modal = document.getElementById('party-modal');
  if (!modal || modal.classList.contains('hidden')) return;

  const party = drpgState.lastParty;
  const formInfoEl = document.getElementById('party-modal-formation-info');
  const memberTabsEl = document.getElementById('party-modal-member-tabs');
  const subTabsEl = document.getElementById('party-modal-sub-tabs');
  const membersListEl = document.getElementById('party-modal-members-list');

  if (!party || !party.members || party.members.length === 0) {
    if (membersListEl) membersListEl.innerHTML = '<div style="color:#94a3b8;padding:20px;text-align:center;">尚未載入小隊資料，請稍候...</div>';
    return;
  }

  // 1. 確保選中狀態正常
  const isFormationMode = (drpgState.selectedModalMemberIdx === 'FORMATION');
  if (!isFormationMode) {
    if (typeof drpgState.selectedModalMemberIdx !== 'number' || drpgState.selectedModalMemberIdx >= party.members.length || drpgState.selectedModalMemberIdx < 0) {
      drpgState.selectedModalMemberIdx = 0;
    }
  }
  if (!drpgState.partyModalSubTab) {
    drpgState.partyModalSubTab = 'EQUIP';
  }

  const selIdx = drpgState.selectedModalMemberIdx;
  const m = (!isFormationMode && party.members[selIdx]) ? party.members[selIdx] : party.members[0];

  // 2. 渲染陣法與靈威狀態
  if (formInfoEl) {
    const ultBtn = party.canCastUltimate
      ? `<button class="act-btn btn-ult" onclick="send('formation cast')" style="padding:2px 8px;font-size:11px;">⚡ 施展陣法奧義【${party.ultimateSkillName}】</button>`
      : `<span style="color:#94a3b8;font-size:11px;">奧義【${party.ultimateSkillName || '無'}】(充能 ${party.formationEnergy || 0}/100)</span>`;

    formInfoEl.innerHTML = `
      <div style="display:flex;align-items:center;gap:10px;">
        <span>☯️ 當前道門陣法：<strong style="color:#38bdf8;">${party.formationName || '四象辟邪陣'}</strong></span>
        <button class="act-btn" onclick="send('formation toggle')" style="padding:2px 8px;font-size:11px;">切換陣法 (F)</button>
      </div>
      <div style="display:flex;align-items:center;gap:10px;">
        <span>⚡ 靈威：${party.formationEnergy || 0}/100</span>
        ${ultBtn}
      </div>
    `;
  }

  // 3. 渲染頂部隊員切換頁籤列
  if (memberTabsEl) {
    const memberBtns = party.members.map((mem, idx) => {
      const isSel = (!isFormationMode && selIdx === idx);
      const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
      const rowBadge = mem.row === 'FRONT' ? '前衛' : '後衛';
      const hpPct = Math.min(100, Math.max(0, (mem.hp / mem.maxHp) * 100));
      return `
        <button class="party-member-tab-btn ${isSel ? 'active' : ''}" type="button" onclick="selectPartyModalMember(${idx})">
          <span style="font-weight:bold;">#${idx + 1} ${mem.name}</span>
          <span class="member-level-badge" style="font-size:10px;">Lv.${mem.level || 1}</span>
          <span style="font-size:10px; color:${mem.row === 'FRONT' ? '#f87171' : '#60a5fa'};">[${rowBadge}]</span>
          ${mem.className ? `<span style="font-size:10px; color:#38bdf8;">${mem.className}</span>` : ''}
          ${(idx === 0 && mem.freeStatPoints > 0) ? `<span class="hud-free-points-pill" style="margin-left:2px;">+${mem.freeStatPoints}</span>` : ''}
          <div style="width:40px; height:4px; background:#334155; border-radius:2px; overflow:hidden;">
            <div style="width:${hpPct}%; height:100%; background:${isAlive ? '#10b981' : '#ef4444'};"></div>
          </div>
        </button>
      `;
    }).join('');

    const formationBtn = `
      <button class="party-member-tab-btn ${isFormationMode ? 'active' : ''}" type="button" onclick="selectPartyFormationTab()" style="${isFormationMode ? 'border-color:#a855f7; box-shadow:0 -2px 10px rgba(168,85,247,0.3);' : 'border-left:2px solid #a855f7;'}">
        <span style="font-weight:bold; color:#c084fc;">☯️ 全隊陣法奧義</span>
        <span style="font-size:10px; color:#a855f7;">[全隊]</span>
        <span style="font-size:10px; color:#e9d5ff;">${party.formationName || '四象辟邪陣'}</span>
      </button>
    `;

    memberTabsEl.innerHTML = memberBtns + formationBtn;
  }

  // 4. 若為全隊陣法模式，直接渲染陣法奧義視圖
  if (isFormationMode) {
    if (subTabsEl) subTabsEl.style.display = 'none';
    if (membersListEl) {
      membersListEl.innerHTML = renderTeamFormationView(party);
    }
    return;
  }

  if (subTabsEl) subTabsEl.style.display = 'flex';

  // 5. 渲染子分頁切換列 (屬性裝備 / 武學法術 / 戰術方針)
  if (subTabsEl) {
    const activeSub = drpgState.partyModalSubTab;
    const tacticsCount = (m.tactics && m.tactics.length > 0) ? `(${m.tactics.length})` : '';
    subTabsEl.innerHTML = `
      <button class="party-sub-tab-btn ${activeSub === 'EQUIP' ? 'active' : ''}" type="button" onclick="switchPartyModalSubTab('EQUIP')">
        🛡️ 屬性與裝備
      </button>
      <button class="party-sub-tab-btn ${activeSub === 'SPELLBOOK' ? 'active' : ''}" type="button" onclick="switchPartyModalSubTab('SPELLBOOK')">
        📖 武學法術
      </button>
      <button class="party-sub-tab-btn ${activeSub === 'TACTICS' ? 'active' : ''}" type="button" onclick="switchPartyModalSubTab('TACTICS')">
        🎯 戰術方針 (Gambit AI) ${tacticsCount}
      </button>
    `;
  }

  if (!membersListEl) return;
  membersListEl.innerHTML = '';

  // 6. 依子頁籤渲染內容
  if (drpgState.partyModalSubTab === 'SPELLBOOK') {
    membersListEl.innerHTML = renderMemberSpellbook(m, selIdx);
    return;
  }

  if (drpgState.partyModalSubTab === 'TACTICS') {
    membersListEl.innerHTML = renderMemberTactics(m, selIdx);
    return;
  }

  // 預設 'EQUIP' 屬性與裝備
  const allSlots = [
    { key: 'MAIN_HAND', alias: 'weapon', label: '主手武器', icon: '🗡️' },
    { key: 'OFF_HAND', alias: 'shield', label: '副手防具', icon: '🛡️' },
    { key: 'HEAD', alias: 'head', label: '頭部盔甲', icon: '👑' },
    { key: 'BODY', alias: 'armor', label: '身軀道袍', icon: '🥋' },
    { key: 'FEET', alias: 'feet', label: '靴履護具', icon: '👢' },
    { key: 'ACCESSORY_1', alias: 'acc1', label: '本命法寶', icon: '💍' },
    { key: 'ACCESSORY_2', alias: 'acc2', label: '輔佐靈寶', icon: '📿' }
  ];

  const rowBadge = m.row === 'FRONT' ? '前衛' : '後衛';
  const rowClass = `badge-${m.row.toLowerCase()}`;

  const resType = m.resourceType || 'MP';
  const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
  const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;
  let resLabel = `MP: ${curRes}/${maxRes}`;
  if (resType === 'RAGE') resLabel = `怒氣: ${curRes}/${maxRes}`;
  else if (resType === 'COMBO') resLabel = `連擊: ${curRes}/${maxRes}`;

  let equipSlotsHtml = '';
  for (const slot of allSlots) {
    let item = m.equipment ? m.equipment[slot.key] : null;
    if (!item && slot.key === 'MAIN_HAND' && m.equippedWeapon) item = m.equippedWeapon;
    if (!item && slot.key === 'BODY' && m.equippedArmor) item = m.equippedArmor;

    if (item) {
      let statsParts = [];
      if (item.bonusMinDamage || item.bonusMaxDamage) statsParts.push(`攻 ${item.bonusMinDamage}~${item.bonusMaxDamage}`);
      if (item.bonusDefense) statsParts.push(`防 +${item.bonusDefense}`);
      if (item.bonusHp) statsParts.push(`血 +${item.bonusHp}`);
      if (item.bonusSan) statsParts.push(`心 +${item.bonusSan}`);
      const statsStr = statsParts.length > 0 ? statsParts.join(' ') : '基礎裝備';

      equipSlotsHtml += `
        <div class="equip-slot-box has-item" title="${item.description || ''}">
          <div class="equip-slot-title">
            <span>${slot.icon} ${slot.label}</span>
            <button class="unequip-mini-btn" onclick="send('item unequip ${slot.alias} ${selIdx}')" title="卸下放回行囊">✕ 卸下</button>
          </div>
          <div class="equip-slot-name">${item.icon || '📦'} ${item.name}</div>
          <div class="equip-slot-stats">${statsStr}</div>
        </div>
      `;
    } else {
      equipSlotsHtml += `
        <div class="equip-slot-box empty">
          <div class="equip-slot-title">
            <span>${slot.icon} ${slot.label}</span>
          </div>
          <div class="equip-slot-name" style="color:#64748b;font-weight:normal;">(未穿戴)</div>
          <div class="equip-slot-act">
            <button class="item-act-mini-btn" onclick="toggleBagDrawer(true)" title="開啟行囊挑選裝備穿戴">🎒 挑選</button>
          </div>
        </div>
      `;
    }
  }

  const isLeader = (m.id && (m.id === 'm-leader' || m.id.includes('leader'))) || selIdx === 0;
  const level = m.level || 1;
  const exp = m.exp || 0;
  const nextExp = m.nextLevelExp || 180;
  const expPct = Math.min(100, Math.max(0, Math.floor((exp / nextExp) * 100)));
  const freePoints = m.freeStatPoints || 0;

  const strVal = m.str || 5;
  const conVal = m.con || 5;
  const dexVal = m.dex || 5;
  const intVal = m.intStat !== undefined ? m.intStat : (m.intelligence || 5);
  const wisVal = m.wis || 5;

  const renderStatAddBtn = (statKey, label) => {
    if (!isLeader) return '';
    if (freePoints > 0) {
      return `<button class="stat-add-btn" onclick="send('party stat add ${statKey} 1')" title="點擊投入 1 點自由修為點數提升【${label}】">+1</button>`;
    } else {
      return `<button class="stat-add-btn disabled" disabled title="無可用自由修為點數">+1</button>`;
    }
  };

  let freePointsBannerHtml = '';
  if (isLeader) {
    if (freePoints > 0) {
      freePointsBannerHtml = `
        <div class="free-points-banner">
          <div style="display:flex;align-items:center;gap:6px;">
            <span>⭐</span>
            <span><strong>道胎未定・造化充盈</strong>：尚有 <strong style="font-size:15px;color:#fde047;">${freePoints}</strong> 點自由修為點數！</span>
          </div>
          <span style="font-size:11px;color:#fef3c7;">(點擊下方屬性右側 [+1] 按鈕即刻分配)</span>
        </div>
      `;
    } else {
      freePointsBannerHtml = `
        <div style="font-size:11px;color:#94a3b8;display:flex;justify-content:space-between;padding:2px 4px;">
          <span>⭐ 自由修為點數：0 點</span>
          <span style="color:#64748b;">(主角每升一級額外獲贈 2 點自由分配點數)</span>
        </div>
      `;
    }
  } else {
    freePointsBannerHtml = `
      <div style="font-size:11px;color:#94a3b8;display:flex;justify-content:space-between;padding:2px 4px;">
        <span>🏷️ 成長模式：【職業範本自適應】</span>
        <span style="color:#64748b;">(同伴升級自動提升五維，無需手動微操)</span>
      </div>
    `;
  }

  const card = document.createElement('div');
  card.className = 'party-detail-card';
  card.innerHTML = `
    <div class="party-detail-header">
      <div class="party-detail-name-wrap">
        <span style="color:#38bdf8;font-weight:bold;font-size:16px;">#${selIdx + 1} ${m.name}</span>
        <span class="member-level-badge" style="font-size:11px;background:#0f172a;border:1px solid #eab308;padding:1px 6px;border-radius:4px;">Lv.${level}</span>
        <span class="member-row ${rowClass}">[${rowBadge}]</span>
        ${m.className ? `<span class="member-class-badge" title="${m.classDescription || ''}" style="background:#1e293b;border:1px solid #38bdf8;color:#7dd3fc;padding:2px 8px;border-radius:4px;font-size:11px;">🏷️ ${m.className}</span>` : ''}
        <button class="item-act-mini-btn" onclick="send('party switch ${selIdx}')" title="切換前排/後排站位" style="font-size:11px;">🔄 站位切換 (${rowBadge})</button>
      </div>
      <span class="party-detail-role" style="font-size:12px;color:#94a3b8;">${m.roleTitle}</span>
    </div>

    <!-- 1. 修為境界與 EXP 進度條 -->
    <div class="party-detail-exp-card">
      <div class="party-detail-exp-header">
        <div class="party-detail-exp-title">
          <span>✨ 境界修為</span>
          <strong style="color:#fde047;">Lv.${level}</strong>
        </div>
        <div class="party-detail-exp-val">EXP: ${exp} / ${nextExp} (${expPct}%)</div>
      </div>
      <div class="party-detail-exp-bar" title="晉升下一級所需修為：${exp}/${nextExp} (${expPct}%)">
        <div class="party-detail-exp-fill" style="width:${expPct}%;"></div>
      </div>
    </div>

    <!-- 2. 自由點數提示橫幅 -->
    ${freePointsBannerHtml}

    <!-- 3. 五維先天道基屬性網格 -->
    <div style="font-size:13px;color:#cbd5e1;font-weight:bold;margin-top:2px;display:flex;justify-content:space-between;align-items:center;">
      <span>☯️ 五維先天道基：</span>
      ${isLeader && freePoints > 0 ? `<span style="font-size:11px;color:#facc15;">請點擊右側 [+1] 分配點數</span>` : ''}
    </div>
    <div class="party-detail-stats-grid">
      <div class="stat-item-box">
        <div class="stat-item-header">
          <span class="stat-item-label">💪 力量 STR</span>
          ${renderStatAddBtn('str', '力量')}
        </div>
        <div class="stat-item-val">${strVal}</div>
        <div class="stat-item-desc">物理傷害、負重、招架</div>
      </div>

      <div class="stat-item-box">
        <div class="stat-item-header">
          <span class="stat-item-label">🫀 根骨 CON</span>
          ${renderStatAddBtn('con', '根骨')}
        </div>
        <div class="stat-item-val">${conVal}</div>
        <div class="stat-item-desc">血量上限 (+10/點)、減傷</div>
      </div>

      <div class="stat-item-box">
        <div class="stat-item-header">
          <span class="stat-item-label">⚡ 靈巧 DEX</span>
          ${renderStatAddBtn('dex', '靈巧')}
        </div>
        <div class="stat-item-val">${dexVal}</div>
        <div class="stat-item-desc">暴擊率、命中、身法閃避</div>
      </div>

      <div class="stat-item-box">
        <div class="stat-item-header">
          <span class="stat-item-label">🧠 悟性 INT</span>
          ${renderStatAddBtn('int', '悟性')}
        </div>
        <div class="stat-item-val">${intVal}</div>
        <div class="stat-item-desc">真元上限 (+8/點)、法術</div>
      </div>

      <div class="stat-item-box">
        <div class="stat-item-header">
          <span class="stat-item-label">🧘 定力 WIS</span>
          ${renderStatAddBtn('wis', '定力')}
        </div>
        <div class="stat-item-val">${wisVal}</div>
        <div class="stat-item-desc">治療增幅、法力恢復、抗性</div>
      </div>
    </div>

    <!-- 4. 當前實時動態條 (HP / MP / SAN) -->
    <div class="party-detail-bars" style="background:rgba(15,23,42,0.6);padding:10px;border-radius:6px;">
      <div style="font-size:12px;color:#f87171;font-weight:bold;">❤️ 氣血 HP: ${m.hp}/${m.maxHp}</div>
      <div style="font-size:12px;color:#60a5fa;font-weight:bold;">⚡ ${resLabel}</div>
      <div style="font-size:12px;color:#34d399;font-weight:bold;">🧘 道心 SAN: ${m.san}/${m.maxSan} (${m.sanityStatus || '心境平穩'})</div>
    </div>

    <!-- 5. 裝備槽位 (5 基礎 + 2 飾品) -->
    <div style="font-size:13px;color:#cbd5e1;font-weight:bold;margin-top:4px;">🛡️ 裝備槽位 (5 基礎 + 2 飾品)：</div>
    <div class="party-detail-equip-grid">
      ${equipSlotsHtml}
    </div>
  `;
  membersListEl.appendChild(card);
}

/**
 * 渲染單一成員的 WoW 經典修仙武學典籍 (Spellbook)
 * 陣法奧義已抽離至全隊陣法面板，此處專注於兵刃套路與門派絕技
 */
function renderMemberSpellbook(m, memberIdx) {
  if (!drpgState.spellbookState) {
    drpgState.spellbookState = {};
  }
  if (!drpgState.spellbookState[memberIdx]) {
    drpgState.spellbookState[memberIdx] = { tab: 'STANCES', page: 1 };
  }
  const curState = drpgState.spellbookState[memberIdx];
  if (curState.tab !== 'STANCES' && curState.tab !== 'SKILLS') {
    curState.tab = 'STANCES';
  }
  const curTab = curState.tab || 'STANCES';
  let curPage = curState.page || 1;

  // 1. 整理各 Tab 項目 (兵刃套路與門派絕技)
  const stances = m.availableStances || [];
  const skills = m.skills || [];

  const tabs = [
    { key: 'STANCES', label: '🗡️ 兵刃套路', count: stances.length > 0 ? stances.length : 1 },
    { key: 'SKILLS', label: '⚡ 門派絕技', count: skills.length }
  ];

  let items = [];
  if (curTab === 'STANCES') {
    if (stances.length > 0) {
      items = stances.map(st => {
        let icon = '🗡️';
        const sId = (st.skillId || '').toLowerCase();
        if (sId.includes('blade')) icon = '⚔️';
        else if (sId.includes('axe')) icon = '🪓';
        else if (sId.includes('staff')) icon = '🦯';
        else if (sId.includes('hammer') || sId.includes('blunt')) icon = '🔨';
        else if (sId.includes('dagger')) icon = '⚡';
        else if (sId.includes('bow')) icon = '🏹';
        else if (sId.includes('fist') || sId.includes('unarmed')) icon = '👊';

        return {
          id: st.skillId,
          name: st.skillName,
          icon: icon,
          cost: '自動普攻',
          desc: st.description || '隨武器揮舞自動施展之套路招式。',
          isCurrent: !!st.isCurrentEnabled,
          canSwitch: !st.isCurrentEnabled
        };
      });
    } else {
      items.push({
        id: m.basicSkillId || 'basic_attack',
        name: m.basicSkillName || '基礎武學',
        icon: '🗡️',
        cost: '自動普攻',
        desc: '隨手施展之門派基礎套路。',
        isCurrent: true,
        canSwitch: false
      });
    }
  } else if (curTab === 'SKILLS') {
    if (skills.length > 0) {
      items = skills.map(s => ({
        id: s.id,
        name: s.name,
        icon: s.icon || '⚡',
        cost: s.costDescription || (s.costValue ? `${s.costValue} 消耗` : '無消耗'),
        desc: s.description || '引導煞氣或靈威爆發之奧義招式。',
        isCurrent: false,
        canSwitch: false
      }));
    } else {
      items.push({
        id: 'none',
        name: '暫無主動絕技',
        icon: '📜',
        cost: '專注平砍',
        desc: '該角色當前專注於武器套路平砍，未修習主動絕技。',
        isCurrent: false,
        canSwitch: false
      });
    }
  }

  // 分頁計算 (每頁 4 個條目，2x2 雙欄卡片佈局)
  const pageSize = 4;
  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  if (curPage > totalPages) curPage = totalPages;
  curState.page = curPage;

  const startIdx = (curPage - 1) * pageSize;
  const pageItems = items.slice(startIdx, startIdx + pageSize);

  // 雙欄網格條目 HTML
  const spellsGridHtml = pageItems.map(it => {
    const activeClass = it.isCurrent ? 'active-spell' : '';
    let actBtnHtml = '';
    if (it.isCurrent) {
      actBtnHtml = '<span class="spell-active-badge">✔ 參悟運轉中</span>';
    } else if (it.canSwitch) {
      actBtnHtml = `<button class="spell-switch-btn" onclick="send('party enable ${memberIdx} ${it.id}')" title="啟用為當前主力普攻套路">⚡ 啟用套路</button>`;
    } else if (curTab === 'SKILLS') {
      actBtnHtml = '<span style="font-size:9px;color:#60a5fa;">戰鬥快捷施展</span>';
    }

    return `
      <div class="spell-card ${activeClass}" title="${it.desc}">
        <div class="spell-icon-box">${it.icon}</div>
        <div class="spell-info">
          <div class="spell-name-row">
            <span class="spell-name">${it.name}</span>
            <span class="spell-cost-badge">${it.cost}</span>
          </div>
          <div class="spell-desc">${it.desc}</div>
          <div class="spell-action-row">
            ${actBtnHtml}
          </div>
        </div>
      </div>
    `;
  }).join('');

  // 右側垂直 Tab 標籤列
  const tabsHtml = tabs.map(t => {
    const isSelected = (curTab === t.key);
    return `
      <button class="spellbook-tab-btn ${isSelected ? 'active' : ''}"
        onclick="switchSpellbookTab(${memberIdx}, '${t.key}')" title="${t.label}">
        ${t.label} (${t.count})
      </button>
    `;
  }).join('');

  return `
    <div class="wow-spellbook-container">
      <div class="wow-spellbook-header">
        <span class="wow-spellbook-title">📖 修仙武學典籍・道種法術書 (Spellbook)</span>
        <span style="font-size:10px;color:#94a3b8;">${tabs.find(t=>t.key===curTab)?.label || ''}</span>
      </div>
      <div class="wow-spellbook-layout">
        <div class="wow-spellbook-page">
          <div class="spell-grid">
            ${spellsGridHtml}
          </div>
          <div class="spellbook-pagination">
            <button class="spellbook-page-btn" onclick="switchSpellbookPage(${memberIdx}, -1)" ${curPage <= 1 ? 'disabled' : ''}>◀ 上一頁</button>
            <span>第 ${curPage} 頁 / 共 ${totalPages} 頁 (共 ${items.length} 條目)</span>
            <button class="spellbook-page-btn" onclick="switchSpellbookPage(${memberIdx}, 1)" ${curPage >= totalPages ? 'disabled' : ''}>下一頁 ▶</button>
          </div>
        </div>
        <div class="wow-spellbook-tabs">
          ${tabsHtml}
        </div>
      </div>
      <div style="margin-top:12px;padding:8px 14px;background:rgba(99,102,241,0.15);border:1px solid rgba(99,102,241,0.3);border-radius:6px;display:flex;align-items:center;justify-content:space-between;font-size:11px;color:#c7d2fe;">
        <span>☯️ <strong>全隊陣法奧義</strong>屬於全隊共有，不局限於個人武學。請前往全隊陣法面板進行切換與站位調配。</span>
        <button class="act-btn btn-sm" onclick="selectPartyFormationTab()" style="padding:3px 10px;font-size:11px;background:#4f46e5;color:#fff;">前往全隊陣法 ➔</button>
      </div>
    </div>
  `;
}

function switchSpellbookTab(memberIdx, tab) {
  if (!drpgState.spellbookState) drpgState.spellbookState = {};
  drpgState.spellbookState[memberIdx] = { tab: tab, page: 1 };
  renderPartyModal();
}

function switchSpellbookPage(memberIdx, delta) {
  if (!drpgState.spellbookState) drpgState.spellbookState = {};
  if (!drpgState.spellbookState[memberIdx]) drpgState.spellbookState[memberIdx] = { tab: 'STANCES', page: 1 };
  drpgState.spellbookState[memberIdx].page = Math.max(1, (drpgState.spellbookState[memberIdx].page || 1) + delta);
  renderPartyModal();
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
 * 開啟存檔 / 讀檔面板
 */
function openSaveModal(mode) {
  drpgState.isSaveModalOpen = true;
  drpgState.saveModalMode = mode || 'load';
  const modal = document.getElementById('save-modal');
  const title = document.getElementById('save-modal-mode-title');
  if (modal) modal.classList.remove('hidden');
  if (title) {
    title.innerText = (mode === 'save') ? '💾 仙道命冊・選擇存檔槽位 (覆蓋進度)' : '📂 仙道命冊・讀取存檔 / 開闢新道途';
  }
  // 主動向後端查詢最新存檔 (靜默模式，不印出 ASCII 表格)
  send('saves quiet', true);
  renderSaveSlots();
}

/**
 * 關閉存檔面板
 */
function closeSaveModal() {
  drpgState.isSaveModalOpen = false;
  const modal = document.getElementById('save-modal');
  if (modal) modal.classList.add('hidden');
}

/**
 * 接收後端推送的 SAVE_SLOTS 陣列
 */
function updateSaveSlotsView(slots) {
  drpgState.saveSlots = slots || [];

  // 解析最新存檔 (優先排除空存檔)
  const populated = drpgState.saveSlots.filter(s => !s.empty);
  if (populated.length > 0) {
    populated.sort((a, b) => {
      const timeA = a.savedAt || '';
      const timeB = b.savedAt || '';
      return timeB.localeCompare(timeA);
    });
    drpgState.latestSaveSlot = populated[0];
  } else {
    drpgState.latestSaveSlot = null;
  }

  updateContinueButtonLabel();

  if (drpgState.isSaveModalOpen) {
    renderSaveSlots();
  }
}

/**
 * 渲染 6 個存檔卡片 (Slot 0 為自動存檔，Slot 1~5 為自訂手動存檔)
 */
function renderSaveSlots() {
  const container = document.getElementById('save-slots-list');
  if (!container) return;

  container.innerHTML = '';
  const slots = drpgState.saveSlots && drpgState.saveSlots.length > 0
      ? drpgState.saveSlots
      : [0, 1, 2, 3, 4, 5].map(id => ({ slotId: id, empty: true, title: id === 0 ? '自動存檔' : `存檔槽位 ${id}` }));

  slots.forEach(slot => {
    const isAuto = (slot.slotId === 0);
    const card = document.createElement('div');
    card.className = `slot-item-card ${isAuto ? 'is-autosave' : ''} ${slot.empty ? 'is-empty' : 'is-populated'}`;

    const infoCol = document.createElement('div');
    infoCol.className = 'slot-info-col';

    const badgeRow = document.createElement('div');
    badgeRow.className = 'slot-badge-row';
    const tag = document.createElement('span');
    tag.className = `slot-id-tag ${isAuto ? 'auto-tag' : ''}`;
    tag.innerText = isAuto ? '⚡ 自動存檔' : `槽位 ${slot.slotId}`;
    badgeRow.appendChild(tag);

    const titleSpan = document.createElement('span');
    titleSpan.className = 'slot-title-text';
    titleSpan.innerText = slot.title || (isAuto ? '自動存檔' : `存檔槽位 ${slot.slotId}`);
    badgeRow.appendChild(titleSpan);
    infoCol.appendChild(badgeRow);

    const metaRow = document.createElement('div');
    metaRow.className = 'slot-meta-row';
    if (slot.empty) {
      metaRow.innerHTML = '<span>-- 空無道痕 (未存檔) --</span>';
    } else {
      metaRow.innerHTML = `<span>👤 主角: <strong>${slot.protagonistName || '無名'}</strong></span>`
          + `<span>🏛️ <strong>${slot.floorName || '太陰古塚'}</strong></span>`
          + `<span>☯️ <strong>${slot.formationName || '四象辟邪陣'}</strong></span>`
          + `<span>🕒 <strong>${slot.savedAt || ''}</strong></span>`;
    }
    infoCol.appendChild(metaRow);

    if (!slot.empty && slot.memberNames && slot.memberNames.length > 0) {
      const membersRow = document.createElement('div');
      membersRow.className = 'slot-members-row';
      membersRow.innerText = '👥 小隊隊容：' + slot.memberNames.join('、');
      infoCol.appendChild(membersRow);
    }
    card.appendChild(infoCol);

    const actionsCol = document.createElement('div');
    actionsCol.className = 'slot-actions-col';

    if (slot.empty) {
      if (!isAuto) {
        const saveBtn = document.createElement('button');
        saveBtn.className = 'slot-btn slot-btn-save';
        saveBtn.innerText = '💾 存檔於此';
        saveBtn.onclick = () => triggerSaveSlot(slot.slotId);
        actionsCol.appendChild(saveBtn);
      }
    } else {
      // 讀取按鈕 (自動存檔與一般存檔均可讀取)
      const loadBtn = document.createElement('button');
      loadBtn.className = 'slot-btn slot-btn-load';
      loadBtn.innerText = isAuto ? '📂 載入自動存檔' : '📂 載入此檔';
      loadBtn.onclick = () => triggerLoadSlot(slot.slotId);
      actionsCol.appendChild(loadBtn);

      if (!isAuto) {
        // 覆蓋存檔按鈕
        const saveBtn = document.createElement('button');
        saveBtn.className = 'slot-btn slot-btn-save';
        saveBtn.innerText = '💾 覆蓋存檔';
        saveBtn.onclick = () => triggerSaveSlot(slot.slotId);
        actionsCol.appendChild(saveBtn);

        // 刪除按鈕
        const delBtn = document.createElement('button');
        delBtn.className = 'slot-btn slot-btn-del';
        delBtn.innerText = '🗑️';
        delBtn.title = '刪除此存檔';
        delBtn.onclick = () => triggerDeleteSlot(slot.slotId);
        actionsCol.appendChild(delBtn);
      }
    }

    card.appendChild(actionsCol);
    container.appendChild(card);
  });
}

function triggerSaveSlot(slotId) {
  const defaultName = (drpgState.lastParty && drpgState.lastParty.members && drpgState.lastParty.members[0])
      ? drpgState.lastParty.members[0].name + '的太陰道途'
      : '';
  const title = prompt('請輸入存檔標題 (可直接按確定使用預設)：', defaultName);
  if (title !== null) {
    const cmd = title.trim() ? `save ${slotId} ${title.trim()}` : `save ${slotId}`;
    send(cmd);
  }
}

function triggerLoadSlot(slotId) {
  send(`load ${slotId}`);
  closeSaveModal();
  enterGameWorld();
}

function triggerDeleteSlot(slotId) {
  if (confirm(`確定要抹除【存檔槽位 ${slotId}】的冒險記錄嗎？`)) {
    send(`save del ${slotId}`);
  }
}

function triggerNewGame() {
  const input = document.getElementById('new-protagonist-input');
  const name = (input && input.value.trim()) ? input.value.trim() : '玄靈子';
  send(`new ${name}`);
  if (input) input.value = '';
  closeSaveModal();
  enterGameWorld();
}

/* ==========================================================================
   三階段進入遊戲流程控制器 (Stage 1: 封面 -> Stage 2: 模式/序幕 -> Stage 3: 靈境)
   ========================================================================== */

/**
 * 開啟主標題封面 (Title Screen)
 */
function openTitleScreen() {
  drpgState.isTitleScreenOpen = true;
  const overlay = document.getElementById('title-screen-overlay');
  if (overlay) overlay.classList.remove('hidden');
  updateContinueButtonLabel();
  if (typeof send === 'function') {
    send('saves quiet', true);
  }
}

/**
 * 關閉主標題封面
 */
function closeTitleScreen() {
  drpgState.isTitleScreenOpen = false;
  const overlay = document.getElementById('title-screen-overlay');
  if (overlay) overlay.classList.add('hidden');
}

/**
 * 正式進入遊戲世界 (階段三)
 */
function enterGameWorld() {
  drpgState.hasEnteredGameWorld = true;
  closeTitleScreen();
  closeNewGameModal();
  closePrologueModal();
  closeGuideModal();
  closeSaveModal();
  updateContinueButtonLabel();
}

/**
 * 繼續最近存檔 (階段一快捷進入)
 */
function continueLatestSave() {
  if (drpgState.hasEnteredGameWorld) {
    enterGameWorld();
    return;
  }
  if (drpgState.latestSaveSlot && !drpgState.latestSaveSlot.empty) {
    send(`load ${drpgState.latestSaveSlot.slotId}`);
    enterGameWorld();
  } else {
    openNewGameModal();
  }
}

/**
 * 開啟開闢新途視窗 (階段二)
 */
function openNewGameModal() {
  drpgState.isNewGameModalOpen = true;
  const modal = document.getElementById('new-game-modal');
  if (modal) modal.classList.remove('hidden');
  const input = document.getElementById('new-game-protagonist-input');
  if (input && !input.value) {
    input.value = '玄靈子';
  }
  selectFormation(drpgState.selectedFormation || 'formation_four_symbols');
}

/**
 * 關閉開闢新途視窗
 */
function closeNewGameModal() {
  drpgState.isNewGameModalOpen = false;
  const modal = document.getElementById('new-game-modal');
  if (modal) modal.classList.add('hidden');
}

/**
 * 挑選初始陣法
 */
function selectFormation(formationId) {
  drpgState.selectedFormation = formationId;
  const cardFour = document.getElementById('card-four-symbols');
  const cardXuan = document.getElementById('card-xuan-yin');
  if (cardFour && cardXuan) {
    if (formationId === 'formation_xuan_yin') {
      cardXuan.classList.add('selected');
      cardFour.classList.remove('selected');
    } else {
      cardFour.classList.add('selected');
      cardXuan.classList.remove('selected');
    }
  }
}

/**
 * 點擊「踏入命運 ‧ 啟程」，進入序章故事 (階段二 -> 序幕)
 */
function startPrologueFlow() {
  const input = document.getElementById('new-game-protagonist-input');
  const name = (input && input.value.trim()) ? input.value.trim() : '玄靈子';
  closeNewGameModal();
  openPrologueModal();
}

/**
 * 開啟序章故事導讀
 */
function openPrologueModal() {
  drpgState.isPrologueModalOpen = true;
  const modal = document.getElementById('prologue-modal');
  if (modal) modal.classList.remove('hidden');
}

/**
 * 關閉序章故事導讀
 */
function closePrologueModal() {
  drpgState.isPrologueModalOpen = false;
  const modal = document.getElementById('prologue-modal');
  if (modal) modal.classList.add('hidden');
}

/**
 * 完成序章導讀 / 點擊跳過，發送開局指令並正式踏入古塚 (階段二 -> 階段三)
 */
function finishPrologueAndEnter() {
  const input = document.getElementById('new-game-protagonist-input');
  const name = (input && input.value.trim()) ? input.value.trim() : '玄靈子';
  const formationId = drpgState.selectedFormation || 'formation_four_symbols';
  send(`new ${name} ${formationId}`);
  enterGameWorld();
}

/**
 * 開啟太陰秘錄 (遊戲指南)
 */
function openGuideModal() {
  drpgState.isGuideModalOpen = true;
  const modal = document.getElementById('guide-modal');
  if (modal) modal.classList.remove('hidden');
}

/**
 * 關閉太陰秘錄
 */
function closeGuideModal() {
  drpgState.isGuideModalOpen = false;
  const modal = document.getElementById('guide-modal');
  if (modal) modal.classList.add('hidden');
}

/**
 * 動態更新封面「繼續冒險」按鈕的提示文字與狀態
 */
function updateContinueButtonLabel() {
  const continueBtn = document.getElementById('btn-continue-game');
  const continueHint = document.getElementById('continue-slot-hint');
  if (!continueBtn) return;

  if (drpgState.hasEnteredGameWorld) {
    continueBtn.disabled = false;
    const mainLabel = continueBtn.querySelector('.btn-main-label');
    if (mainLabel) mainLabel.innerText = '返回靈境探索';
    if (continueHint) {
      const charName = (drpgState.lastParty && drpgState.lastParty.members && drpgState.lastParty.members[0])
          ? drpgState.lastParty.members[0].name
          : '玄靈子';
      continueHint.innerText = `目前進度：${charName} (古塚靈境)`;
    }
  } else if (drpgState.latestSaveSlot && !drpgState.latestSaveSlot.empty) {
    continueBtn.disabled = false;
    const mainLabel = continueBtn.querySelector('.btn-main-label');
    if (mainLabel) mainLabel.innerText = '繼續冒險';
    if (continueHint) {
      const slotTag = drpgState.latestSaveSlot.slotId === 0 ? '⚡自動存檔' : `槽位${drpgState.latestSaveSlot.slotId}`;
      continueHint.innerText = `${slotTag}：${drpgState.latestSaveSlot.protagonistName || '無名'} (${drpgState.latestSaveSlot.floorName || '太陰古塚'})`;
    }
  } else {
    continueBtn.disabled = true;
    const mainLabel = continueBtn.querySelector('.btn-main-label');
    if (mainLabel) mainLabel.innerText = '繼續冒險';
    if (continueHint) {
      continueHint.innerText = '尚無修仙道痕 (請開闢新途)';
    }
  }
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

  // 點擊非輸入框區域自動釋放文字焦點，確保 WASD 隨時可用
  document.addEventListener('pointerdown', (e) => {
    if (!e.target.closest('input, textarea, select, #dev-console-modal')) {
      if (document.activeElement && (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA')) {
        document.activeElement.blur();
        updateModeBadge(false);
      }
    }
  });

  window.addEventListener('keydown', (e) => {
    // 0. 若目前正在任何文字輸入控制項或下拉選單中
    const activeEl = document.activeElement;
    const devInputEl = document.getElementById('dev-cmd-input');
    const newNameInput = document.getElementById('new-game-protagonist-input');

    if (activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA' || activeEl.tagName === 'SELECT')) {
      if (drpgState.isDevConsoleOpen || activeEl === devInputEl) {
        if (e.key === 'Escape' || e.key === '`' || e.key === '~') {
          e.preventDefault();
          toggleDevConsole(false);
        } else if (e.key === 'Enter') {
          e.preventDefault();
          handleDevEnter();
        }
        return;
      }
      if (activeEl === inputEl) {
        if (e.key === 'Escape') {
          inputEl.blur();
          updateModeBadge(false);
        }
        return;
      }
      if (newNameInput && activeEl === newNameInput) {
        if (e.key === 'Enter') {
          e.preventDefault();
          startPrologueFlow();
        } else if (e.key === 'Escape') {
          closeNewGameModal();
        }
        return;
      }
      if (e.key === 'Escape') {
        activeEl.blur();
      }
      return; // 正在其他輸入項 (如貨棧數量、方針數值) 打字時，不觸發 WASD 移動或功能鍵
    }

    // 1. 若在主封面或任一模態視窗中，攔截快捷鍵防止穿透，並支援 Esc 依序返回關閉
    const isAnyModalOrTitleOpen = drpgState.isTitleScreenOpen || drpgState.isSaveModalOpen || drpgState.isNewGameModalOpen || drpgState.isPrologueModalOpen || drpgState.isGuideModalOpen || drpgState.isPartyModalOpen || drpgState.isShopModalOpen;
    if (isAnyModalOrTitleOpen) {
      if (e.key === 'Escape') {
        if (drpgState.isShopModalOpen) {
          toggleShopModal(false);
          return;
        }
        if (drpgState.isPartyModalOpen) {
          closePartyModal();
          return;
        }
        if (drpgState.isPrologueModalOpen) {
          closePrologueModal();
          return;
        }
        if (drpgState.isNewGameModalOpen) {
          closeNewGameModal();
          return;
        }
        if (drpgState.isGuideModalOpen) {
          closeGuideModal();
          return;
        }
        if (drpgState.isSaveModalOpen) {
          closeSaveModal();
          return;
        }
        if (drpgState.isTitleScreenOpen && drpgState.hasEnteredGameWorld) {
          enterGameWorld();
          return;
        }
      }
      return; // 阻止在選單/封面/彈窗中按 WASD 造成背景地圖移動
    }

    // 快捷鍵 ~ 開啟/關閉開發者終端
    if (e.key === '`' || e.key === '~') {
      e.preventDefault();
      toggleDevConsole();
      return;
    }

    const key = e.key.toLowerCase();

    // 3. 快捷鍵轉至文字指令輸入 (開啟開發者終端)
    if (key === '/' || e.key === 'Enter') {
      e.preventDefault();
      toggleDevConsole(true);
      return;
    }

    // 4. 戰鬥中與非戰鬥共通快捷鍵
    if (e.key === 'Escape') {
      if (drpgState.isDevConsoleOpen) {
        toggleDevConsole(false);
        return;
      }
      if (drpgState.isShopModalOpen) {
        toggleShopModal(false);
        return;
      }
      if (drpgState.isPartyModalOpen) {
        closePartyModal();
        return;
      }
      if (drpgState.isSaveModalOpen) {
        closeSaveModal();
        return;
      }
      if (drpgState.isBagDrawerOpen) {
        toggleBagDrawer(false);
        return;
      }
      if (drpgState.isSkillDrawerOpen) {
        closeSkillDrawer();
        return;
      }
      if (drpgState.lastBattle && drpgState.lastBattle.inBattle) {
        send('battle flee');
        return;
      }
    }

    if (e.key === 'F5') {
      e.preventDefault();
      openSaveModal('save');
      return;
    }

    // 行囊快捷鍵 B (戰鬥中與非戰鬥均可開啟)
    if (key === 'b') {
      e.preventDefault();
      toggleBagDrawer();
      return;
    }

    // 5. 數字鍵 1~6 快捷選取隊員展開技能盤
    if (['1', '2', '3', '4', '5', '6'].includes(key)) {
      e.preventDefault();
      const idx = parseInt(key) - 1;
      selectPartyMember(idx);
      return;
    }

    // 6. 戰鬥中快捷鍵
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
      } else if (key === 'p' || key === 'c') {
        e.preventDefault();
        triggerPartyAction();
      }
      return;
    }

    // 7. 探索模式步進與快捷功能 (支援城鎮出口與地牢步進自適應)
    if (key === 'w' || e.key === 'ArrowUp') {
      e.preventDefault();
      handleDpad('north');
    } else if (key === 's' || e.key === 'ArrowDown') {
      e.preventDefault();
      handleDpad('south');
    } else if (key === 'a' || e.key === 'ArrowLeft') {
      e.preventDefault();
      handleDpad('west');
    } else if (key === 'd' || e.key === 'ArrowRight') {
      e.preventDefault();
      handleDpad('east');
    } else if (key === 'm') {
      e.preventDefault();
      triggerMapAction();
    } else if (key === 'p' || key === 'c') {
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
window.handleDpad = handleDpad;
window.sendTownMove = sendTownMove;
window.toggleDevConsole = toggleDevConsole;
window.handleDevEnter = handleDevEnter;
window.renderTownNav = renderTownNav;
window.updateDrpgView = updateDrpgView;
window.selectPartyMember = selectPartyMember;
window.closeSkillDrawer = closeSkillDrawer;
window.castPartySkill = castPartySkill;
window.toggleBagDrawer = toggleBagDrawer;
window.renderBagDrawer = renderBagDrawer;
window.triggerPartyAction = triggerPartyAction;
window.togglePartyModal = togglePartyModal;
window.closePartyModal = closePartyModal;
window.renderPartyModal = renderPartyModal;
window.selectPartyFormationTab = selectPartyFormationTab;
window.selectBattleTarget = selectBattleTarget;
window.triggerBattleMode = toggleBattleMode;
window.toggleRadarPosition = toggleRadarPosition;
window.toggleRadarViewMode = toggleRadarViewMode;
window.openSaveModal = openSaveModal;
window.closeSaveModal = closeSaveModal;
window.updateSaveSlotsView = updateSaveSlotsView;
window.triggerSaveSlot = triggerSaveSlot;
window.triggerLoadSlot = triggerLoadSlot;
window.triggerDeleteSlot = triggerDeleteSlot;
window.triggerNewGame = triggerNewGame;

// 新增流程函式全域掛載
window.openTitleScreen = openTitleScreen;
window.closeTitleScreen = closeTitleScreen;
window.enterGameWorld = enterGameWorld;
window.continueLatestSave = continueLatestSave;
window.openNewGameModal = openNewGameModal;
window.closeNewGameModal = closeNewGameModal;
window.selectFormation = selectFormation;
window.startPrologueFlow = startPrologueFlow;
window.openPrologueModal = openPrologueModal;
window.closePrologueModal = closePrologueModal;
window.finishPrologueAndEnter = finishPrologueAndEnter;
window.openGuideModal = openGuideModal;
window.closeGuideModal = closeGuideModal;

/**
 * 開啟貨棧交易模態視窗 (Shop Modal)
 */
function openShopModal(data) {
  if (!data) return;
  drpgState.lastShopCatalog = data;

  const modal = document.getElementById('shop-modal');
  if (!modal) return;

  const titleEl = document.getElementById('shop-modal-title-text');
  if (titleEl) {
    titleEl.innerText = `🏪【${data.shopName || '貨棧櫃檯'}】`;
  }

  const coinEl = document.getElementById('shop-modal-coin-val');
  if (coinEl) {
    coinEl.innerText = `${data.playerCoin || 0} 靈石`;
  }

  // 若彈窗已經開啟，動態刷新各商品的剩餘庫存與購買按鈕狀態，而不重置整個 DOM 避免干擾輸入
  if (drpgState.isShopModalOpen && !modal.classList.contains('hidden')) {
    if (data.goods && data.goods.length > 0) {
      data.goods.forEach(item => {
        const row = document.getElementById(`shop-item-${item.id}`);
        if (!row) return;
        const stockEl = row.querySelector('.shop-item-stock');
        if (stockEl) {
          if (item.stock !== undefined && item.stock >= 0) {
            stockEl.style.color = item.stock > 0 ? '#38bdf8' : '#ef4444';
            stockEl.innerText = `(庫存: ${item.stock})`;
          } else {
            stockEl.style.color = '#10b981';
            stockEl.innerText = '(充足)';
          }
        }
        const buyBtn = row.querySelector('.shop-buy-btn');
        const qtyInput = document.getElementById(`shop-qty-${item.id}`);
        const isOutOfStock = (item.stock !== undefined && item.stock === 0);
        if (qtyInput && item.stock !== undefined && item.stock >= 0) {
          qtyInput.max = item.stock;
          if (parseInt(qtyInput.value) > item.stock) {
            qtyInput.value = Math.max(1, item.stock);
          }
        }
        if (buyBtn) {
          if (isOutOfStock) {
            buyBtn.disabled = true;
            buyBtn.style.opacity = '0.5';
            buyBtn.style.cursor = 'not-allowed';
            buyBtn.innerText = '❌ 售罄';
          } else {
            buyBtn.disabled = false;
            buyBtn.style.opacity = '1';
            buyBtn.style.cursor = 'pointer';
            buyBtn.innerText = '🛒 購買';
          }
        }
      });
    }
    return;
  }

  drpgState.isShopModalOpen = true;
  const goodsContainer = document.getElementById('shop-modal-goods-list');
  if (goodsContainer) {
    let goodsHtml = '';
    if (data.goods && data.goods.length > 0) {
      data.goods.forEach(item => {
        const isOutOfStock = (item.stock !== undefined && item.stock === 0);
        const stockHtml = (item.stock !== undefined && item.stock >= 0)
          ? `<span class="shop-item-stock" style="color: ${item.stock > 0 ? '#38bdf8' : '#ef4444'}; margin-left: 8px; font-size: 0.85em;">(庫存: ${item.stock})</span>`
          : `<span class="shop-item-stock" style="color: #10b981; margin-left: 8px; font-size: 0.85em;">(充足)</span>`;
        const buyBtnText = isOutOfStock ? '❌ 售罄' : '🛒 購買';
        const buyBtnDisabled = isOutOfStock ? 'disabled style="opacity: 0.5; cursor: not-allowed;"' : '';
        const maxQty = (item.stock !== undefined && item.stock > 0) ? item.stock : 99;

        goodsHtml += `
          <div class="shop-item-row" id="shop-item-${item.id}">
            <div class="shop-item-info">
              <span class="shop-item-badge">#${item.index}</span>
              <span class="shop-item-name">${item.name}</span>
              <span class="shop-item-price">💰 ${item.price} 靈石</span>
              ${stockHtml}
            </div>
            <div class="shop-item-desc">${item.description || ''}</div>
            <div class="shop-item-actions">
              <div class="shop-qty-picker">
                <button class="qty-btn" type="button" onclick="adjustShopQty('${item.id}', -1)">-</button>
                <input type="number" id="shop-qty-${item.id}" class="shop-qty-input" value="1" min="1" max="${maxQty}" />
                <button class="qty-btn" type="button" onclick="adjustShopQty('${item.id}', 1)">+</button>
              </div>
              <button class="shop-buy-btn" type="button" ${buyBtnDisabled} onclick="triggerShopBuy('${item.id}')">${buyBtnText}</button>
            </div>
          </div>
        `;
      });
    } else {
      goodsHtml = '<div style="color: #94a3b8; padding: 20px; text-align: center;">貨架空空如也，掌櫃尚在進貨中...</div>';
    }
    goodsContainer.innerHTML = goodsHtml;
  }

  modal.classList.remove('hidden');
}

/**
 * 開關貨棧交易視窗
 */
function toggleShopModal(show) {
  const modal = document.getElementById('shop-modal');
  if (!modal) return;
  if (show === undefined) {
    drpgState.isShopModalOpen = !drpgState.isShopModalOpen;
  } else {
    drpgState.isShopModalOpen = !!show;
  }
  if (drpgState.isShopModalOpen) {
    modal.classList.remove('hidden');
  } else {
    modal.classList.add('hidden');
    if (document.activeElement) {
      document.activeElement.blur();
    }
  }
}

function renderShopCatalogInLog(data) {
  openShopModal(data);
}

function adjustShopQty(itemId, delta) {
  const input = document.getElementById(`shop-qty-${itemId}`);
  if (!input) return;
  let val = parseInt(input.value) || 1;
  val = Math.max(1, Math.min(99, val + delta));
  input.value = val;
}

function triggerShopBuy(itemId) {
  const input = document.getElementById(`shop-qty-${itemId}`);
  const qty = input ? (parseInt(input.value) || 1) : 1;
  send(`buy ${itemId} ${qty}`);
}

window.openShopModal = openShopModal;
window.toggleShopModal = toggleShopModal;
window.renderShopCatalogInLog = renderShopCatalogInLog;
window.adjustShopQty = adjustShopQty;
window.triggerShopBuy = triggerShopBuy;
window.openPartyModal = openPartyModal;
window.togglePartyModal = togglePartyModal;
window.selectPartyModalMember = selectPartyModalMember;
window.switchPartyModalSubTab = switchPartyModalSubTab;
window.toggleAddTacticsForm = toggleAddTacticsForm;
window.handleTacticsCondChange = handleTacticsCondChange;
window.submitAddTactics = submitAddTactics;

window.addEventListener('DOMContentLoaded', () => {
  initKeyboardControls();
  applyRadarPosition();
  updateRadarModeBtn();
  setTimeout(() => {
    if (typeof send === 'function' && (!drpgState.saveSlots || drpgState.saveSlots.length === 0)) {
      send('saves quiet', true);
    }
  }, 300);
});