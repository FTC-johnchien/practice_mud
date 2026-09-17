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
  if (!mainViewport) return;
  if (drpgState.radarPosition === 'right') {
    mainViewport.classList.add('layout-radar-right');
    if (btn) btn.innerText = '⮂ 置左';
  } else {
    mainViewport.classList.remove('layout-radar-right');
    if (btn) btn.innerText = '⮃ 置右';
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

  if (payload.party) {
    drpgState.lastParty = payload.party;
    renderPartyHud(payload.party);
    if (drpgState.isBagDrawerOpen) {
      renderBagDrawer();
    }
    if (drpgState.isPartyModalOpen) {
      renderPartyModal();
    }
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

        const header = document.createElement('div');
        header.className = 'npc-header';
        const roleHtml = isHostile
          ? `<span style="font-size:10px; color:#f87171; background:rgba(239,68,68,0.2); border:1px solid #ef4444; padding:1px 6px; border-radius:3px;">敵對</span>`
          : `<span style="font-size:11px; color:#94a3b8;">${npc.title || ''}</span>`;
        header.innerHTML = `<span class="npc-name">${npc.name}</span>${roleHtml}`;
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
        chip.innerText = `拾取 ${it.name} x${it.count || it.amount || 1}`;
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
          <span class="member-row badge-${m.row.toLowerCase()}">${rowBadge}</span>
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

      return `
        <button class="skill-btn" ${canCast ? '' : 'disabled'}
          onclick="castPartySkill(${drpgState.selectedMemberIdx}, '${s.id}')"
          title="${s.name} - ${s.description}">
          <div class="skill-btn-title-row">
            <span class="skill-btn-name">${s.icon || '⚡'} ${s.name}</span>
            <span class="skill-btn-cost ${costTagClass}">${costLabel}</span>
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
  }
}

function closePartyModal() {
  togglePartyModal(false);
}

function renderPartyModal() {
  const modal = document.getElementById('party-modal');
  if (!modal || modal.classList.contains('hidden')) return;

  const party = drpgState.lastParty;
  const formInfoEl = document.getElementById('party-modal-formation-info');
  const membersListEl = document.getElementById('party-modal-members-list');
  if (!party || !party.members) {
    if (membersListEl) membersListEl.innerHTML = '<div style="color:#94a3b8;padding:20px;text-align:center;">尚未載入小隊資料，請稍候...</div>';
    return;
  }

  // 渲染陣法與靈威狀態
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

  if (!membersListEl) return;
  membersListEl.innerHTML = '';

  const allSlots = [
    { key: 'MAIN_HAND', alias: 'weapon', label: '主手武器', icon: '🗡️' },
    { key: 'OFF_HAND', alias: 'shield', label: '副手防具', icon: '🛡️' },
    { key: 'HEAD', alias: 'head', label: '頭部盔甲', icon: '👑' },
    { key: 'BODY', alias: 'armor', label: '身軀道袍', icon: '🥋' },
    { key: 'FEET', alias: 'feet', label: '靴履護具', icon: '👢' },
    { key: 'ACCESSORY_1', alias: 'acc1', label: '本命法寶', icon: '💍' },
    { key: 'ACCESSORY_2', alias: 'acc2', label: '輔佐靈寶', icon: '📿' }
  ];

  party.members.forEach((m, idx) => {
    const card = document.createElement('div');
    card.className = 'party-detail-card';

    const rowBadge = m.row === 'FRONT' ? '前衛' : '後衛';
    const rowClass = `badge-${m.row.toLowerCase()}`;

    // 依職業三態資源判定能量條
    const resType = m.resourceType || 'MP';
    const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
    const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;
    let resLabel = `MP: ${curRes}/${maxRes}`;
    if (resType === 'RAGE') resLabel = `怒氣: ${curRes}/${maxRes}`;
    else if (resType === 'COMBO') resLabel = `連擊: ${curRes}/${maxRes}`;

    // 產生 5+2 裝備槽位
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
              <button class="unequip-mini-btn" onclick="send('item unequip ${slot.alias} ${idx}')" title="卸下放回行囊">✕ 卸下</button>
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

    let skillsHtml = '';
    const skills = m.skills || [];
    if (skills.length > 0) {
      skillsHtml = `
        <div style="font-size:11px;color:#94a3b8;font-weight:bold;margin-top:8px;">修習武學與道門道術 (${skills.length})：</div>
        <div class="party-skills-list" style="display:flex;flex-wrap:wrap;gap:6px;margin-top:4px;">
          ${skills.map(s => `
            <div class="party-skill-chip" title="${s.description || ''}" style="background:rgba(30,41,59,0.8);border:1px solid #334155;border-radius:5px;padding:3px 8px;font-size:11px;display:flex;align-items:center;gap:4px;">
              <span>${s.icon || '⚔️'}</span>
              <strong style="color:#e2e8f0;">${s.name}</strong>
              <span style="color:#60a5fa;font-size:10px;">(${s.costDescription || '無消耗'})</span>
            </div>
          `).join('')}
        </div>
      `;
    }

    card.innerHTML = `
      <div class="party-detail-header">
        <div class="party-detail-name-wrap">
          <span style="color:#38bdf8;font-weight:bold;">#${idx + 1}</span>
          <span class="party-detail-name">${m.name}</span>
          <span class="member-row ${rowClass}">${rowBadge}</span>
          <button class="item-act-mini-btn" onclick="send('formation switch ${idx}')" title="切換前排/後排站位" style="font-size:10px;">站位切換</button>
        </div>
        <span class="party-detail-role">${m.roleTitle}</span>
      </div>
      <div class="party-detail-bars">
        <div style="font-size:11px;color:#f87171;">HP: ${m.hp}/${m.maxHp}</div>
        <div style="font-size:11px;color:#60a5fa;">${resLabel}</div>
        <div style="font-size:11px;color:#34d399;">SAN: ${m.san}/${m.maxSan}</div>
      </div>
      <div style="font-size:11px;color:#94a3b8;font-weight:bold;margin-top:2px;">裝備槽位 (5 基礎 + 2 飾品)：</div>
      <div class="party-detail-equip-grid">
        ${equipSlotsHtml}
      </div>
      ${skillsHtml}
    `;
    membersListEl.appendChild(card);
  });
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

  window.addEventListener('keydown', (e) => {
    // 0. 若正在開發者控制台中打字
    const devInputEl = document.getElementById('dev-cmd-input');
    if (drpgState.isDevConsoleOpen || document.activeElement === devInputEl) {
      if (e.key === 'Escape' || e.key === '`' || e.key === '~') {
        e.preventDefault();
        toggleDevConsole(false);
      } else if (e.key === 'Enter') {
        e.preventDefault();
        handleDevEnter();
      }
      return;
    }

    // 1. 若目前正在舊版文字輸入框中打字
    if (document.activeElement === inputEl) {
      if (e.key === 'Escape') {
        inputEl.blur();
        updateModeBadge(false);
      }
      return;
    }

    // 若在主角自訂道號輸入框打字
    const newNameInput = document.getElementById('new-game-protagonist-input');
    if (newNameInput && document.activeElement === newNameInput) {
      if (e.key === 'Enter') {
        e.preventDefault();
        startPrologueFlow();
      } else if (e.key === 'Escape') {
        closeNewGameModal();
      }
      return;
    }

    // 2. 若在主封面或任一全螢幕模態視窗中，攔截快捷鍵防止穿透，並支援 Esc 依序返回
    const isAnyModalOrTitleOpen = drpgState.isTitleScreenOpen || drpgState.isSaveModalOpen || drpgState.isNewGameModalOpen || drpgState.isPrologueModalOpen || drpgState.isGuideModalOpen || drpgState.isPartyModalOpen;
    if (isAnyModalOrTitleOpen) {
      if (e.key === 'Escape') {
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
      return; // 阻止在選單/封面中按 WASD 造成背景移動
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
      } else if (key === 'p') {
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