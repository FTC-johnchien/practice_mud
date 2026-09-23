import { store } from '../core/state-store.js';

/**
 * 地牢雷達與視野面板 (Dungeon Radar & Minimap Panel)
 * 負責渲染 10x10 地牢雷達迷霧、座標朝向、警戒靈壓、前方視野與視口切換
 */

/**
 * 切換雷達左右停靠位置
 */
export function toggleRadarPosition() {
  const current = store.get('radarPosition');
  const next = current === 'left' ? 'right' : 'left';
  store.setState({ radarPosition: next });
  localStorage.setItem('drpg_radar_pos', next);
  applyRadarPosition();
}

export function applyRadarPosition() {
  const mainViewport = document.querySelector('.main-viewport');
  const btn = document.getElementById('radar-dock-toggle-btn');
  const townBtn = document.getElementById('town-swap-side-btn');
  const battleBtn = document.getElementById('battle-swap-side-btn');
  if (!mainViewport) return;
  const pos = store.get('radarPosition');
  if (pos === 'right') {
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
export function toggleRadarViewMode() {
  const current = store.get('radarMode');
  const next = current === 'centered' ? 'full' : 'centered';
  store.setState({ radarMode: next });
  localStorage.setItem('drpg_radar_mode', next);
  updateRadarModeBtn();
  const lastDungeon = store.get('lastDungeon');
  if (lastDungeon) {
    renderMinimap(lastDungeon);
  }
}

export function updateRadarModeBtn() {
  const btn = document.getElementById('radar-mode-toggle-btn');
  if (btn) {
    btn.innerText = store.get('radarMode') === 'centered' ? '🎯 視口' : '🗺️ 全圖';
  }
}

/**
 * 輔助方法：依符號取得地塊樣式與圖標
 */
export function populateTileAppearance(cell, symbol, gx, gy) {
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
 * @param {Object} dungeon 地牢狀態資料
 */
export function renderMinimap(dungeon) {
  const gridContainer = document.getElementById('drpg-grid');
  const infoHeader = document.getElementById('drpg-floor-info');
  const forwardInspect = document.getElementById('drpg-forward-inspect');

  if (!gridContainer || !dungeon) return;

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

  if (store.get('radarMode') === 'centered') {
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

// 相容掛載至 window
if (typeof window !== 'undefined') {
  window.toggleRadarPosition = toggleRadarPosition;
  window.applyRadarPosition = applyRadarPosition;
  window.toggleRadarViewMode = toggleRadarViewMode;
  window.updateRadarModeBtn = updateRadarModeBtn;
  window.renderMinimap = renderMinimap;
}
