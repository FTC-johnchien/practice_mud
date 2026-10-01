import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';
import { selectPartyMember } from './party-hud-panel.js';

/**
 * 戰鬥主舞台與敵方陣列面板 (Battle Arena Panel)
 * 負責渲染敵方雙排陣列 (1~7 體)、集火鎖定準星、戰場我方快顯 HUD 及戰鬥專屬按鈕列
 */

/**
 * 渲染戰場敵方陣列與主舞台交鋒態勢
 * @param {Object} battle 戰鬥狀態資料
 */
export function renderBattleArena(battle) {
  const panel = document.getElementById('battle-arena-panel');
  if (!panel || !battle) return;

  if (battle.inBattle) {
    panel.classList.remove('hidden');
  } else {
    panel.classList.add('hidden');
    return;
  }

  const countBadge = document.getElementById('battle-enemies-count');
  const enemiesBox = document.getElementById('battle-enemies-container');
  const focusNameEl = document.getElementById('battle-focus-name');
  const badge = document.getElementById('battle-status-badge');

  if (badge) {
    if (battle.state === 'VICTORY') {
      badge.innerText = '🎉 戰鬥大捷！';
      badge.style.background = 'rgba(16, 185, 129, 0.3)';
      badge.style.borderColor = 'rgba(16, 185, 129, 0.5)';
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
  const livingEnemies = enemies.filter(e => e.alive);
  if (countBadge) {
    countBadge.innerText = `${livingEnemies.length} 體`;
  }

  // 俄羅斯方塊 / Knight & Dragon 3 通用陣列物理重力下落模擬 (Tetris Gravity Simulation)
  // 依照怪物原始排位 (rowIdx) 由前往後排序，讓靠近交鋒線的怪物先沉降就位，後方怪物依序向下墜落堆疊
  const sortedEnemies = [...livingEnemies].sort((a, b) => {
    const rA = Number(a.rowIdx) || 1;
    const rB = Number(b.rowIdx) || 1;
    return rA - rB;
  });

  // 記錄 1~5 欄每欄當前已被怪物佔據的最高排位 (0 代表該欄完全空蕩)
  const colHighest = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
  const enemyEffectiveRows = new Map();

  sortedEnemies.forEach(e => {
    const cStart = Math.max(1, Math.min(5, Number(e.colIdx) || 1));
    const w = Math.max(1, Number(e.width) || Number(e.size) || 1);
    const h = Math.max(1, Number(e.height) || Number(e.size) || 1);

    // 尋找此怪物跨越的所有欄位中，下方現存最高障礙物的排位
    let maxObstacleRow = 0;
    for (let c = cStart; c < cStart + w && c <= 5; c++) {
      if ((colHighest[c] || 0) > maxObstacleRow) {
        maxObstacleRow = colHighest[c];
      }
    }

    // 怪物受重力吸引盡可能落下，停在最高障礙物的正上方 (最低為第 1 排交鋒線)
    const effR = maxObstacleRow + 1;
    enemyEffectiveRows.set(e.index, effR);

    // 更新該怪物覆蓋的所有欄位的最高佔位
    const topRow = effR + h - 1;
    for (let c = cStart; c < cStart + w && c <= 5; c++) {
      colHighest[c] = topRow;
    }
  });

  // 尋找當前鎖定集火目標 (優先以玩家手動指定 > 怪物仇恨標記 > 前線 Row 1 第一個存活者)
  let selectedEnemy = null;
  if (battle.selectedTargetIndex !== undefined && battle.selectedTargetIndex >= 0) {
    selectedEnemy = livingEnemies.find(e => e.index === battle.selectedTargetIndex);
  }
  if (!selectedEnemy) {
    selectedEnemy = livingEnemies.find(e => e.isTarget || e.isSelectedTarget);
  }
  if (!selectedEnemy) {
    selectedEnemy = livingEnemies.find(e => enemyEffectiveRows.get(e.index) === 1) || livingEnemies[0];
  }

  if (focusNameEl) {
    if (selectedEnemy) {
      focusNameEl.innerText = `${selectedEnemy.name}`;
    } else {
      focusNameEl.innerText = '無（全體覆滅）';
    }
  }

  // 結構鍵判定：任一怪物陣亡或跌落導致有效排位變更時，立即重繪
  const structureKey = livingEnemies.map(e => `${e.index}_${e.id}_C${e.colIdx}_R${enemyEffectiveRows.get(e.index)}_W${e.width || e.size}_H${e.height || e.size}`).join('|');
  const needFullRebuild = (enemiesBox.dataset.structureKey !== structureKey);

  function createEnemyCard(e) {
    const card = document.createElement('div');
    card.dataset.enemyIndex = String(e.index);
    const isTarget = (selectedEnemy && selectedEnemy.index === e.index) || e.isTarget || (battle.selectedTargetIndex === e.index);
    const w = Number(e.width) || Number(e.size) || 1;
    const h = Number(e.height) || Number(e.size) || 1;
    const effR = enemyEffectiveRows.get(e.index) || e.rowIdx || 1;
    const gridRowStart = Math.max(1, 6 - (effR + h - 1));
    const colStart = Math.max(1, Math.min(5, e.colIdx || 1));

    let sizeClass = ` size-${w}x${h}`;
    if (w === 1 && h === 1) sizeClass = ' size-1x1';
    else if (w === 2 && h === 2) sizeClass = ' size-2x2';
    else if (w === 3 && h === 3) sizeClass = ' size-3x3';
    else if (w === 1 && h === 5) sizeClass = ' size-1x5';
    else if (w === 1 && h === 3) sizeClass = ' size-1x3';

    card.className = `enemy-card${sizeClass}${isTarget ? ' selected-target' : ''}`;
    card.title = `點擊鎖定 ${e.name} 為集火目標`;
    card.onclick = () => selectBattleTarget(e.index);

    // CSS Grid 5x5 精確座標
    card.style.gridColumn = `${colStart} / span ${w}`;
    card.style.gridRow = `${gridRowStart} / span ${h}`;

    const hpPct = Math.min(100, Math.max(0, (e.hp / e.maxHp) * 100));

    const targetBadge = e.targetMemberName
      ? `<span class="enemy-aggro-badge" style="background:#450a0a; color:#fca5a5; border:1px solid #7f1d1d; padding:1px 4px; border-radius:3px; font-size:12px; white-space:nowrap;" title="仇恨鎖定：${e.targetMemberName}">🎯${e.targetMemberName}</span>`
      : '';
    const stunBadge = e.isStunned
      ? `<span class="enemy-stun-badge" style="background:#422006; color:#fdba74; border:1px solid #9a3412; padding:1px 4px; border-radius:3px; font-size:12px;">💫 暈</span>`
      : '';

    let buffsHtml = '';
    if (e.activeBuffs) {
      buffsHtml = e.activeBuffs.map(b => `<span class="enemy-buff-badge" style="background:#172554; color:#93c5fd; border:1px solid #1e3a8a; padding:1px 4px; border-radius:3px; font-size:12px;">${b.icon || '✨'}${b.name}</span>`).join('');
    }

    if (w === 1 && h === 5) {
      // 1x5 貫通陣柱
      card.innerHTML = `
        <div class="enemy-name-row" style="align-items:center;">
          <span class="large-enemy-badge" style="font-size:11px; padding:1px 4px; background:rgba(59,130,246,0.3); border:1px solid #3b82f6; color:#93c5fd;">🏛️ 1x5</span>
          <div class="enemy-badges-right" style="display:flex; gap:2px;">
            ${targetBadge}
            ${stunBadge}
          </div>
        </div>
        <div style="flex:1; display:flex; flex-direction:column; justify-content:center; align-items:center; gap:8px;">
          <div style="writing-mode:vertical-rl; font-size:14px; font-weight:bold; letter-spacing:4px; color:#60a5fa;" title="${e.name}">${e.name}</div>
          <div style="font-size:11px; color:#93c5fd; background:rgba(30,58,138,0.4); padding:2px 4px; border-radius:3px; text-align:center;">天罡通天</div>
        </div>
        <div class="enemy-hp-wrap large-hp-wrap" style="height:22px; border-radius:4px; overflow:hidden;" title="生命 HP: ${e.hp}/${e.maxHp}">
          <div class="enemy-hp-bar" style="width: ${hpPct}%; background: linear-gradient(90deg, #2563eb, #38bdf8);"></div>
          <span class="enemy-hp-text" style="font-size:12px; font-weight:bold; text-shadow:0 1px 2px #000;">${e.hp}/${e.maxHp}</span>
        </div>
      `;
    } else if (w === 1 && h === 3) {
      // 1x3 深處天樁
      card.innerHTML = `
        <div class="enemy-name-row" style="align-items:center;">
          <span class="large-enemy-badge" style="font-size:11px; padding:1px 4px; background:rgba(168,85,247,0.3); border:1px solid #a855f7; color:#c084fc;">⚡ 1x3</span>
          <div class="enemy-badges-right" style="display:flex; gap:2px;">
            ${targetBadge}
            ${stunBadge}
          </div>
        </div>
        <div style="flex:1; display:flex; flex-direction:column; justify-content:center; align-items:center; gap:6px;">
          <div style="writing-mode:vertical-rl; font-size:13px; font-weight:bold; letter-spacing:2px; color:#d8b4fe;" title="${e.name}">${e.name}</div>
          <div style="font-size:11px; color:#c084fc; text-align:center;">紫電雷罡</div>
        </div>
        <div class="enemy-hp-wrap large-hp-wrap" style="height:20px; border-radius:4px; overflow:hidden;" title="生命 HP: ${e.hp}/${e.maxHp}">
          <div class="enemy-hp-bar" style="width: ${hpPct}%; background: linear-gradient(90deg, #7c3aed, #c084fc);"></div>
          <span class="enemy-hp-text" style="font-size:12px; font-weight:bold; text-shadow:0 1px 2px #000;">${e.hp}/${e.maxHp}</span>
        </div>
      `;
    } else if (w === 3 && h === 3) {
      // 3x3 巨霸
      card.innerHTML = `
        <div class="enemy-name-row" style="align-items:center;">
          <div style="display:flex; align-items:center; gap:6px; min-width:0; flex:1; overflow:hidden;">
            <span class="large-enemy-badge size-3x3-badge" style="background:rgba(236,72,153,0.3); border:1px solid #ec4899; color:#f472b6; font-size:13px; font-weight:bold; padding:2px 7px; border-radius:4px;">👹 巨擘 3x3</span>
            <span class="enemy-name" style="font-size:16px; font-weight:bold; color:#f472b6;" title="${e.name}">${e.name}</span>
          </div>
          <div class="enemy-badges-right" style="display:flex; gap:4px;">
            ${targetBadge}
            ${stunBadge}
            ${buffsHtml}
          </div>
        </div>
        <div class="large-enemy-info" style="font-size:13px; color:#f0abfc; display:flex; align-items:center; gap:6px; background:rgba(236,72,153,0.1); padding:2px 6px; border-radius:4px;">
          <span>🛡️ 四倍玄罡 • 太陰巨樁 (3x3 佔位)</span>
        </div>
        <div style="font-size:13px; color:#c084fc; display:flex; align-items:center; gap:6px;">
          <span>⚡ 核心靈威共振 • 巍然撼嶽</span>
        </div>
        <div class="enemy-hp-wrap large-hp-wrap" style="height:26px; border-radius:4px; overflow:hidden;" title="生命 HP: ${e.hp}/${e.maxHp}">
          <div class="enemy-hp-bar" style="width: ${hpPct}%; background: linear-gradient(90deg, #db2777, #f59e0b);"></div>
          <span class="enemy-hp-text" style="font-size:14px; font-weight:bold; text-shadow:0 1px 3px #000;">HP ${e.hp}/${e.maxHp}</span>
        </div>
      `;
    } else if (w === 2 && h === 2) {
      // 2x2 巨型
      card.innerHTML = `
        <div class="enemy-name-row" style="align-items:center;">
          <div style="display:flex; align-items:center; gap:6px; min-width:0; flex:1; overflow:hidden;">
            <span class="large-enemy-badge" style="font-size:13px; padding:1px 6px;">👹 巨型 2x2</span>
            <span class="enemy-name" style="font-size:14px; font-weight:bold;" title="${e.name}">${e.name}</span>
          </div>
          <div class="enemy-badges-right" style="display:flex; gap:4px;">
            ${targetBadge}
            ${stunBadge}
            ${buffsHtml}
          </div>
        </div>
        <div class="large-enemy-info" style="font-size:13px; color:#c084fc; display:flex; align-items:center; gap:6px;">
          <span>🛡️ 雙倍防禦 • 巨型陣樁 (2x2 佔位)</span>
        </div>
        <div class="enemy-hp-wrap large-hp-wrap" style="height:22px; border-radius:4px; overflow:hidden;" title="生命 HP: ${e.hp}/${e.maxHp}">
          <div class="enemy-hp-bar" style="width: ${hpPct}%; background: linear-gradient(90deg, #dc2626, #f97316);"></div>
          <span class="enemy-hp-text" style="font-size:13px; font-weight:bold; text-shadow:0 1px 2px #000;">HP ${e.hp}/${e.maxHp}</span>
        </div>
      `;
    } else {
      // 1x1 標準怪
      card.innerHTML = `
        <div class="enemy-name-row" style="align-items:center; height:20px;">
          <span class="enemy-name" style="font-size:13px; font-weight:bold; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; flex:1;" title="${e.name}">${e.name}</span>
          <div class="enemy-badges-right" style="display:flex; gap:3px; flex-shrink:0;">
            ${targetBadge}
            ${stunBadge}
            ${buffsHtml}
          </div>
        </div>
        <div class="enemy-hp-wrap" style="height:20px; border-radius:3px; overflow:hidden;" title="生命 HP: ${e.hp}/${e.maxHp}">
          <div class="enemy-hp-bar" style="width: ${hpPct}%"></div>
          <span class="enemy-hp-text" style="font-size:13px; font-weight:bold; text-shadow:0 1px 2px #000; white-space:nowrap;">HP ${e.hp}/${e.maxHp}</span>
        </div>
      `;
    }
    return card;
  }

  if (needFullRebuild) {
    enemiesBox.dataset.structureKey = structureKey;
    enemiesBox.innerHTML = '';

    const gridBox = document.createElement('div');
    gridBox.className = 'battle-enemies-grid-5x5';

    livingEnemies.forEach(e => {
      gridBox.appendChild(createEnemyCard(e));
    });

    enemiesBox.appendChild(gridBox);
    // 【重要】死亡怪物立即移出戰場陣列，不再渲染任何佔位條 (deadStrip)，空間完全釋放！
  } else {
    // In-place 零閃爍更新各怪物數值與選取標記，絕不摧毀重建節點
    const cardNodes = enemiesBox.querySelectorAll('.enemy-card');
    cardNodes.forEach(card => {
      const idx = parseInt(card.dataset.enemyIndex, 10);
      const e = livingEnemies.find(item => item.index === idx);
      if (!e) return;

      const isTarget = (selectedEnemy && selectedEnemy.index === idx);
      card.classList.toggle('selected-target', isTarget);

      const hpPct = Math.min(100, Math.max(0, (e.hp / e.maxHp) * 100));
      const hpBar = card.querySelector('.enemy-hp-bar');
      if (hpBar) hpBar.style.width = `${hpPct}%`;

      const hpText = card.querySelector('.enemy-hp-text');
      if (hpText) hpText.textContent = `HP ${e.hp}/${e.maxHp}`;

      const badgesRight = card.querySelector('.enemy-badges-right');
      if (badgesRight) {
        const targetBadge = e.targetMemberName
          ? `<span class="enemy-aggro-badge" title="怪物當前仇恨鎖定目標">🎯 ${e.targetMemberName}</span>`
          : '';
        const stunBadge = e.isStunned
          ? '<span class="enemy-stun-badge" title="眩暈不可行動">💫 暈</span>'
          : '';
        const buffsHtml = renderBuffBadges(e.activeBuffs);
        badgesRight.innerHTML = `${targetBadge}${stunBadge}${buffsHtml}`;
      }
    });
  }

  // 渲染戰場我方隊伍陣列與底部暗黑地牢戰備指揮台
  const lastParty = store.get('lastParty');
  if (lastParty) {
    renderBattlePartyQuickBar(lastParty);
  }
}

function renderBuffBadges(activeBuffs) {
  if (!activeBuffs || !Array.isArray(activeBuffs) || activeBuffs.length === 0) return '';
  return `
    <div class="buff-badges-row">
      ${activeBuffs.map(b => {
        const cat = (b.category || 'SHIELD').toLowerCase();
        let valText = '';
        if (cat === 'shield' && b.value > 0) valText = ` ${b.value}`;
        else if (cat === 'hot' && b.value > 0) valText = ` +${b.value}`;
        else if (cat === 'dot' && b.value > 0) valText = ` -${b.value}`;
        else if (b.stacks > 1) valText = ` x${b.stacks}`;

        const secText = b.remainingSeconds !== undefined ? `(${b.remainingSeconds}s)` : '';
        const title = `${b.name} [${b.category}]: ${valText || ''} 剩餘 ${b.remainingSeconds || 0}秒`;

        return `
          <span class="buff-badge buff-${cat}" title="${title}">
            ${b.icon || '✨'}${valText} ${secText}
          </span>
        `;
      }).join('')}
    </div>
  `;
}

/**
 * 渲染戰場我方隊員快速資訊列 (雙層前後排、居中、陣法孔位與換位按鈕，支援 In-place 更新防閃爍)
 * @param {Object} party 小隊狀態
 */
export function renderBattlePartyQuickBar(party) {
  const bar = document.getElementById('battle-party-grid-5x3') || document.getElementById('battle-party-quick-bar');
  if (!bar || !party || !party.members) return;

  let selectedMemberIdx = store.get('selectedMemberIdx');
  if (selectedMemberIdx === undefined || selectedMemberIdx === null || selectedMemberIdx < 0 || selectedMemberIdx >= party.members.length) {
    selectedMemberIdx = 0;
    store.setState({ selectedMemberIdx: 0 });
  }

  // 1. 5x3 戰陣盤矩陣建立 (3 排 x 5 欄)
  // Row 0: 前排, Row 1: 中排, Row 2: 後排
  const gridMatrix = [
    [null, null, null, null, null],
    [null, null, null, null, null],
    [null, null, null, null, null]
  ];

  party.members.forEach((m, idx) => {
    let gx = (m.gridX !== undefined && m.gridX !== null) ? m.gridX : (idx % 5);
    let gy = (m.gridY !== undefined && m.gridY !== null) ? m.gridY : 0;
    gx = Math.max(0, Math.min(4, gx));
    gy = Math.max(0, Math.min(2, gy));

    // 防重疊保護：若該孔位已被佔用，在 5x3 盤中循序找下一個空孔位
    if (gridMatrix[gy][gx]) {
      let placed = false;
      for (let c = 0; c < 5; c++) {
        if (!gridMatrix[gy][c]) {
          gx = c;
          placed = true;
          break;
        }
      }
      if (!placed) {
        for (let r = 0; r < 3 && !placed; r++) {
          for (let c = 0; c < 5 && !placed; c++) {
            if (!gridMatrix[r][c]) {
              gy = r;
              gx = c;
              placed = true;
            }
          }
        }
      }
    }
    gridMatrix[gy][gx] = { m, idx };
  });

  const structureKey = party.members.map(m => `${m.id || m.name}_${m.gridX}_${m.gridY}_${(m.alive !== undefined ? m.alive : m.hp > 0)}`).join('|');
  const needFullRebuild = (bar.dataset.structureKey !== structureKey);

  function createPartyCard(m, idx) {
    const isSelected = (selectedMemberIdx === idx);
    const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
    const hpPct = isAlive ? Math.min(100, Math.max(0, (m.hp / (m.maxHp || 1)) * 100)) : 0;

    const card = document.createElement('div');
    card.dataset.memberIdx = String(idx);
    card.className = `battle-party-grid-card ${isSelected ? 'active-selected' : ''} ${!isAlive ? 'is-dead' : ''}`;
    card.onclick = () => selectCombatMember(idx);
    card.title = isAlive ? `點選 #${idx + 1} ${m.name} 切換戰備指揮台` : `點選 #${idx + 1} ${m.name} (已陣亡，保留站位)`;

    card.innerHTML = `
      <div class="bpg-name-row">
        <span class="bpg-name" style="font-size:13px; font-weight:bold; color:${isAlive ? '#f1f5f9' : '#f87171'}; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">
          ${m.name}
        </span>
        ${!isAlive ? '<span class="bpg-dead-badge" style="font-size:13px; font-weight:bold; color:#ef4444;">💀</span>' : ''}
      </div>
      <div class="bpg-hp-wrap">
        <div class="bpg-hp-bar ${!isAlive ? 'is-dead' : ''}" style="width:${hpPct}%;"></div>
        <span class="bpg-hp-text">HP ${isAlive ? m.hp : 0}/${m.maxHp}</span>
      </div>
    `;
    return card;
  }

  function createEmptyCell(colIdx, rowIdx) {
    const emptyCell = document.createElement('div');
    emptyCell.className = 'bpg-empty-cell';
    emptyCell.title = `陣法空位 (${rowIdx === 0 ? '前排' : (rowIdx === 1 ? '中排' : '後排')} 第${colIdx + 1}孔)`;
    return emptyCell;
  }

  if (needFullRebuild) {
    bar.dataset.structureKey = structureKey;
    bar.innerHTML = '';

    for (let r = 0; r < 3; r++) {
      const rowEl = document.createElement('div');
      rowEl.className = `bpg-row bpg-row-${r}`;
      for (let c = 0; c < 5; c++) {
        const slot = gridMatrix[r][c];
        if (slot) {
          rowEl.appendChild(createPartyCard(slot.m, slot.idx));
        } else {
          rowEl.appendChild(createEmptyCell(c, r));
        }
      }
      bar.appendChild(rowEl);
    }
  } else {
    // In-place 零閃爍更新各隊員數值與選取狀態
    const cardNodes = bar.querySelectorAll('.battle-party-grid-card');
    cardNodes.forEach(card => {
      const idx = parseInt(card.dataset.memberIdx, 10);
      const m = party.members[idx];
      if (!m) return;

      const isSelected = (selectedMemberIdx === idx);
      const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
      const hpPct = isAlive ? Math.min(100, Math.max(0, (m.hp / (m.maxHp || 1)) * 100)) : 0;

      card.classList.toggle('active-selected', isSelected);
      card.classList.toggle('is-dead', !isAlive);

      const nameEl = card.querySelector('.bpg-name');
      if (nameEl) {
        nameEl.style.color = isAlive ? '#f1f5f9' : '#f87171';
      }

      const hpBar = card.querySelector('.bpg-hp-bar');
      if (hpBar) {
        hpBar.style.width = `${hpPct}%`;
        hpBar.classList.toggle('is-dead', !isAlive);
      }

      const hpText = card.querySelector('.bpg-hp-text');
      if (hpText) {
        hpText.textContent = `HP ${isAlive ? m.hp : 0}/${m.maxHp}`;
      }
    });
  }

  // 渲染 Darkest Dungeon 戰備指揮台 (固定高度，無跳動)
  renderCombatCommandDock(party, selectedMemberIdx);
}

/**
 * 選取戰鬥中操作之隊員 (原地更新指揮台，零畫面跳動)
 * @param {number} idx 隊員索引
 */
export function selectCombatMember(idx) {
  store.setState({ selectedMemberIdx: idx, dockSkillPage: 1 });
  const lastParty = store.get('lastParty');
  if (lastParty) {
    renderBattlePartyQuickBar(lastParty);
  }
}

/**
 * 渲染暗黑地牢式固定戰備指揮台 (Combat Command Dock，支援全 In-place 更新防閃爍)
 * @param {Object} party 隊伍快照
 * @param {number} selectedMemberIdx 當前選中隊員索引
 */
export function renderCombatCommandDock(party, selectedMemberIdx) {
  const dock = document.getElementById('combat-command-dock');
  if (!dock || !party || !party.members) return;

  if (selectedMemberIdx === undefined || selectedMemberIdx === null || selectedMemberIdx < 0 || selectedMemberIdx >= party.members.length) {
    selectedMemberIdx = 0;
  }

  const m = party.members[selectedMemberIdx];
  if (!m) return;

  const actionsColEl = document.getElementById('dock-actions-column');
  const contentColEl = document.getElementById('dock-content-column');

  if (!actionsColEl || !contentColEl) return;

  if (!contentColEl.dataset.hasSkillClickListener) {
    contentColEl.dataset.hasSkillClickListener = 'true';
    contentColEl.addEventListener('click', (ev) => {
      const card = ev.target.closest('.dock-mini-skill');
      if (card && !card.classList.contains('empty-slot') && !card.classList.contains('disabled')) {
        handleDockSkillCardClick(ev, card);
      }
    });
  }

  const activeTab = store.get('dockSkillTab') || 'DEFAULT';

  // Left Column: Actions (只有隊員或分頁改變時才重建，否則保持 DOM)
  const actionKey = `${m.id || m.name}_${activeTab}`;
  if (actionsColEl.dataset.actionKey !== actionKey) {
    actionsColEl.dataset.actionKey = actionKey;
    actionsColEl.innerHTML = `
      <div style="color: var(--text-color); font-weight: bold; margin-bottom: 2px; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${m.name}</div>
      <div class="dock-action-btn ${activeTab === 'DEFAULT' ? 'active' : ''}" onclick="setDockSkillTab('DEFAULT')">
        <span>屬性與狀態</span><span>👁️</span>
      </div>
      <div class="dock-action-btn ${activeTab === 'SKILL' ? 'active' : ''}" onclick="setDockSkillTab('SKILL')">
        <span>戰陣武技</span><span>⚔️</span>
      </div>
      <div class="dock-action-btn ${activeTab === 'SPELL' ? 'active' : ''}" onclick="setDockSkillTab('SPELL')">
        <span>仙道法術</span><span>✨</span>
      </div>
    `;
  }

  // Right Column: Content (In-place 防閃爍更新)
  if (activeTab === 'DEFAULT') {
    const hpPct = Math.min(100, Math.max(0, (m.hp / m.maxHp) * 100));
    const curMp = m.mp !== undefined ? m.mp : 0;
    const maxMp = m.maxMp !== undefined ? m.maxMp : 0;
    const mpPct = Math.min(100, Math.max(0, maxMp > 0 ? (curMp / maxMp) * 100 : 0));

    const curSp = m.sp !== undefined ? m.sp : (m.currentSp !== undefined ? m.currentSp : (m.resourceType === 'SP' ? m.currentResource : 0));
    const maxSp = m.maxSp !== undefined ? m.maxSp : 100;
    const spPct = Math.min(100, Math.max(0, maxSp > 0 ? (curSp / maxSp) * 100 : 0));

    const sanPct = Math.min(100, Math.max(0, (m.san / m.maxSan) * 100));

    const buffs = (m.activeBuffs && Array.isArray(m.activeBuffs)) ? m.activeBuffs : [];
    const buffsHtml = buffs.slice(0, 10).map(b => {
      const cat = (b.category || 'SHIELD').toLowerCase();
      return `<span class="party-buff-chip buff-${cat}" title="${b.name} (${b.category}): 餘 ${b.remainingSeconds || 0}秒">${b.icon || '✨'}</span>`;
    }).join('');

    const defaultKey = `DEFAULT_${m.id || m.name}`;
    if (contentColEl.dataset.viewKey !== defaultKey) {
      contentColEl.dataset.viewKey = defaultKey;
      contentColEl.innerHTML = `
        <div class="dock-info-view">
          <div style="display: flex; gap: 6px;">
            <div style="flex: 1; display: flex; flex-direction: column; gap: 3px;">
              <div class="mini-bar-wrap" style="height: 14px; position: relative;">
                <div class="mini-bar hp-fill" style="width: ${hpPct}%; height: 100%;"></div>
                <span class="mini-val hp-val" style="position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: bold; color: #fff; text-shadow: 0 0 2px #000;">HP ${m.hp}/${m.maxHp}</span>
              </div>
              <div class="mini-bar-wrap" style="height: 14px; position: relative;">
                <div class="mini-bar mp-fill ${maxMp > 0 ? '' : 'empty-res-fill'}" style="width: ${mpPct}%; height: 100%;"></div>
                <span class="mini-val mp-val" style="position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: bold; color: #fff; text-shadow: 0 0 2px #000;">MP ${maxMp > 0 ? curMp+'/'+maxMp : '無'}</span>
              </div>
            </div>
            <div style="flex: 1; display: flex; flex-direction: column; gap: 3px;">
              <div class="mini-bar-wrap" style="height: 14px; position: relative;">
                <div class="mini-bar sp-fill ${maxSp > 0 ? '' : 'empty-res-fill'}" style="width: ${spPct}%; height: 100%;"></div>
                <span class="mini-val sp-val" style="position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: bold; color: #fff; text-shadow: 0 0 2px #000;">SP ${maxSp > 0 ? curSp+'/'+maxSp : '無'}</span>
              </div>
              <div class="mini-bar-wrap san-bar-wrap" style="height: 14px; position: relative;">
                <div class="mini-bar san-fill san-${(m.sanLevel || 'NORMAL').toLowerCase()}" style="width: ${sanPct}%; height: 100%;"></div>
                <span class="mini-val san-val" style="position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: bold; color: #fff; text-shadow: 0 0 2px #000;">SAN ${m.san}/${m.maxSan}</span>
              </div>
            </div>
          </div>
          <div style="color: #94a3b8; font-size: 13px; font-weight: 500; margin-top: 2px;">狀態加持：</div>
          <div class="card-buffs-grid" style="display: flex; flex-wrap: wrap; gap: 4px; overflow-y: auto; max-height: 48px;">
            ${buffsHtml || '<span style="color:#64748b;font-size:13px;">(無狀態)</span>'}
          </div>
        </div>
      `;
    } else {
      // In-place 零閃爍更新數值條
      const hpFill = contentColEl.querySelector('.hp-fill');
      if (hpFill) hpFill.style.width = `${hpPct}%`;
      const hpVal = contentColEl.querySelector('.hp-val');
      if (hpVal) hpVal.textContent = `HP ${m.hp}/${m.maxHp}`;

      const mpFill = contentColEl.querySelector('.mp-fill');
      if (mpFill) mpFill.style.width = `${mpPct}%`;
      const mpVal = contentColEl.querySelector('.mp-val');
      if (mpVal) mpVal.textContent = `MP ${maxMp > 0 ? curMp+'/'+maxMp : '無'}`;

      const spFill = contentColEl.querySelector('.sp-fill');
      if (spFill) spFill.style.width = `${spPct}%`;
      const spVal = contentColEl.querySelector('.sp-val');
      if (spVal) spVal.textContent = `SP ${maxSp > 0 ? curSp+'/'+maxSp : '無'}`;

      const sanFill = contentColEl.querySelector('.san-fill');
      if (sanFill) sanFill.style.width = `${sanPct}%`;
      const sanVal = contentColEl.querySelector('.san-val');
      if (sanVal) sanVal.textContent = `SAN ${m.san}/${m.maxSan}`;

      const buffsGrid = contentColEl.querySelector('.card-buffs-grid');
      if (buffsGrid) {
        buffsGrid.innerHTML = buffsHtml || '<span style="color:#64748b;font-size:13px;">(無狀態)</span>';
      }
    }
  } else {
    // SKILL or SPELL tab
    const skills = m.skills || [];
    let filteredSkills = skills.filter(s => {
      const cat = (s.category || 'CLASS').toUpperCase();
      if (activeTab === 'SKILL') return cat !== 'SPELL';
      if (activeTab === 'SPELL') return cat === 'SPELL';
      return true;
    });

    const PAGE_SIZE = 10;
    const totalPages = Math.max(1, Math.ceil(filteredSkills.length / PAGE_SIZE));
    let currentPage = store.get('dockSkillPage') || 1;
    if (currentPage > totalPages) {
      currentPage = totalPages;
      store.setState({ dockSkillPage: currentPage });
    }
    const startIndex = (currentPage - 1) * PAGE_SIZE;
    const pageSkills = filteredSkills.slice(startIndex, startIndex + PAGE_SIZE);

    const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
    const skillListKey = `${activeTab}_${m.id || m.name}_P${currentPage}_${pageSkills.map(s => s.id).join(',')}`;

    if (contentColEl.dataset.viewKey !== skillListKey) {
      // 只有在分頁或隊員技能清單改變時才整體建構一次 DOM，避免高頻摧毀節點導致點擊無效或閃爍！
      contentColEl.dataset.viewKey = skillListKey;

      let slotsHtml = '';
      for (let i = 0; i < PAGE_SIZE; i++) {
        if (i < pageSkills.length) {
          const s = pageSkills[i];
          const onCd = s.remainingCooldownMs > 0;
          const disabled = !isAlive || !s.available || onCd;
          const cdSec = onCd ? (s.remainingCooldownMs / 1000).toFixed(1) : 0;

          let costLabel = '';
          if (s.costValue > 0) {
            if (s.costType === 'MP') costLabel = `<span class="cost-mp">${s.costValue}</span>`;
            else if (s.costType === 'RAGE') costLabel = `<span class="cost-rage">${s.costValue}</span>`;
            else if (s.costType === 'COMBO') costLabel = `<span class="cost-combo">${s.costValue}</span>`;
            else costLabel = `<span style="color:#cbd5e1">${s.costValue}</span>`;
          }

          slotsHtml += `
            <div class="dock-mini-skill ${disabled ? 'disabled' : ''}"
                 data-skill-id="${s.id}"
                 data-skill-name="${s.name}"
                 title="${s.name}\n${s.description || ''}${onCd ? '\n[調息中 ' + cdSec + '秒]' : ''}"
                 onclick="handleDockSkillCardClick(event, this)">
              <span class="skill-icon">${s.icon || '⚡'}</span>
              <span class="skill-name">${s.name}</span>
              <span class="skill-cost">${costLabel}</span>
            </div>
          `;
        } else {
          slotsHtml += `<div class="dock-mini-skill empty-slot" style="opacity: 0.1; cursor: default;"></div>`;
        }
      }

      contentColEl.innerHTML = `
        <div class="dock-skills-view">
          <div class="dock-skills-grid">
            ${slotsHtml}
          </div>
          <div class="dock-skill-pagination">
            <button class="page-btn" ${currentPage <= 1 ? 'disabled' : ''} onclick="changeDockSkillPage(-1)">◀</button>
            <span>頁次 ${currentPage} / ${totalPages}</span>
            <button class="page-btn" ${currentPage >= totalPages ? 'disabled' : ''} onclick="changeDockSkillPage(1)">▶</button>
          </div>
        </div>
      `;
    } else {
      // In-place 零閃爍更新既有技能按鈕的 CD 狀態與可用性，絕不摧毀 DOM，保證點擊 100% 成功！
      const skillCards = contentColEl.querySelectorAll('.dock-mini-skill:not(.empty-slot)');
      pageSkills.forEach((s, idx) => {
        const card = skillCards[idx];
        if (!card) return;
        const onCd = s.remainingCooldownMs > 0;
        const disabled = !isAlive || !s.available || onCd;
        const cdSec = onCd ? (s.remainingCooldownMs / 1000).toFixed(1) : 0;

        card.classList.toggle('disabled', disabled);
        card.title = `${s.name}\n${s.description || ''}${onCd ? '\n[調息中 ' + cdSec + '秒]' : ''}`;
      });
    }
  }
}

/**
 * 點擊技能卡片進行施法 (不跳回 DEFAULT，方便連續查看與施放)
 */
export function handleDockSkillCardClick(event, el) {
  if (event) {
    event.stopPropagation();
  }
  if (!el || el.classList.contains('disabled')) return;
  const skillId = el.dataset.skillId;
  const skillName = el.dataset.skillName;
  if (skillId) {
    // 點擊微反饋動畫
    el.style.transform = 'scale(0.95)';
    setTimeout(() => {
      if (el) el.style.transform = '';
    }, 120);
    handleDockSkillCast(skillId, skillName || '武技');
  }
}

/**
 * 處理技能快捷施放 (自動帶入當前集火鎖定目標)
 */
export function handleDockSkillCast(skillId, skillName) {
  const selectedMemberIdx = store.get('selectedMemberIdx') || 0;
  const lastBattle = store.get('lastBattle');
  let targetIdx = -1;
  if (lastBattle) {
    if (lastBattle.selectedTargetIndex !== undefined && lastBattle.selectedTargetIndex >= 0) {
      targetIdx = lastBattle.selectedTargetIndex;
    } else if (lastBattle.enemies) {
      const living = lastBattle.enemies.filter(e => e.alive !== false && e.hp > 0);
      const t = living.find(e => e.isTarget || e.isSelectedTarget) || living[0];
      if (t) targetIdx = t.index;
    }
  }

  const cmd = (targetIdx >= 0)
    ? `skill cast ${selectedMemberIdx} ${skillId} ${targetIdx}`
    : `skill cast ${selectedMemberIdx} ${skillId}`;
  sendCmd(cmd);
  if (typeof window.appendHtml === 'function') {
    window.appendHtml(`隊員施放【${skillName}】！`, '#38bdf8');
  }
}

export function setDockSkillTab(tab) {
  store.setState({ dockSkillTab: tab, dockSkillPage: 1 });
  const lastParty = store.get('lastParty');
  const selectedMemberIdx = store.get('selectedMemberIdx') || 0;
  if (lastParty) {
    renderCombatCommandDock(lastParty, selectedMemberIdx);
  }
}

/**
 * 切換戰備指揮台技能分頁
 * @param {number} delta 換頁增量 (+1 或 -1)
 */
export function changeDockSkillPage(delta) {
  const curPage = store.get('dockSkillPage') || 1;
  const newPage = Math.max(1, curPage + delta);
  store.setState({ dockSkillPage: newPage });
  const lastParty = store.get('lastParty');
  const selectedMemberIdx = store.get('selectedMemberIdx') || 0;
  if (lastParty) {
    renderCombatCommandDock(lastParty, selectedMemberIdx);
  }
}

/**
 * 隱藏戰場主舞台
 */
export function hideBattleArena() {
  const panel = document.getElementById('battle-arena-panel');
  if (panel) {
    panel.classList.add('hidden');
  }
}

/**
 * 鎖定集火目標 (具備本地即時準星反饋)
 * @param {number} idx 目標索引
 */
export function selectBattleTarget(idx) {
  sendCmd(`battle target ${idx}`);

  // 即時樂觀更新目標鎖定
  const lastBattle = store.get('lastBattle');
  if (lastBattle) {
    lastBattle.selectedTargetIndex = idx;
    if (lastBattle.enemies) {
      lastBattle.enemies.forEach(e => {
        e.isTarget = (e.index === idx);
        e.isSelectedTarget = (e.index === idx);
      });
    }
    renderBattleArena(lastBattle);
  }
}

/**
 * 切換戰鬥模式與探索模式佈局 (嚴格遵守模組職責分離)
 * @param {boolean} inBattle 是否處於戰鬥中
 */
export function toggleBattleMode(inBattle) {
  const actGroup = document.getElementById('action-buttons-group');
  const battleStatusDock = document.getElementById('battle-status-dock');
  const dpadGrid = document.getElementById('dpad-grid') || document.querySelector('.dpad-grid');
  const partyHud = document.querySelector('.party-hud-panel');
  const skillDrawer = document.getElementById('skill-drawer');
  const bottomControlPanel = document.querySelector('.bottom-control-panel');
  const modeBadge = document.getElementById('control-mode-badge');
  const footerHintExplore = document.getElementById('footer-hint-explore');
  const footerUltBtn = document.getElementById('footer-ult-btn');
  const footerFleeBtn = document.getElementById('footer-flee-btn');

  if (inBattle) {
    // 戰鬥模式：顯示底部陣法奧義與遁地按鈕，切換提示文字為戰鬥提示
    if (footerUltBtn) footerUltBtn.classList.remove('hidden');
    if (footerFleeBtn) footerFleeBtn.classList.remove('hidden');
    if (footerHintExplore) footerHintExplore.classList.add('hidden');
    if (battleStatusDock) battleStatusDock.classList.remove('hidden');
    if (partyHud) partyHud.classList.add('hidden');
    if (skillDrawer) skillDrawer.classList.add('hidden');
    if (bottomControlPanel) bottomControlPanel.classList.remove('hidden');
    store.setState({ isSkillDrawerOpen: false });

    if (modeBadge && !store.get('isInputFocused')) {
      modeBadge.className = 'mode-badge mode-typing';
      modeBadge.style.background = 'rgba(239, 68, 68, 0.2)';
      modeBadge.style.borderColor = '#ef4444';
      modeBadge.style.color = '#fca5a5';
      modeBadge.innerText = '⚔️ 戰鬥交鋒中';
    }
  } else {
    // 探索模式：隱藏底部戰鬥專用按鈕，切換提示為探索指引，恢復外層隊伍 HUD
    if (footerUltBtn) footerUltBtn.classList.add('hidden');
    if (footerFleeBtn) footerFleeBtn.classList.add('hidden');
    if (bottomControlPanel) bottomControlPanel.classList.remove('hidden');
    if (battleStatusDock) battleStatusDock.classList.add('hidden');
    if (footerHintExplore) footerHintExplore.classList.remove('hidden');
    if (partyHud) partyHud.classList.remove('hidden');

    if (modeBadge && !store.get('isInputFocused')) {
      modeBadge.className = 'mode-badge mode-stepping';
      modeBadge.style.background = 'rgba(16, 185, 129, 0.2)';
      modeBadge.style.borderColor = '#10b981';
      modeBadge.style.color = '#6ee7b7';
      modeBadge.innerText = '🧭 靈境步進';
    }
  }
}

export function selectPartyMemberForSkill(idx) {
  const lastBattle = store.get('lastBattle');
  if (lastBattle && lastBattle.inBattle) {
    selectCombatMember(idx);
  } else {
    selectPartyMember(idx);
  }
}

// 相容掛載至 window
if (typeof window !== 'undefined') {
  window.renderBattleArena = renderBattleArena;
  window.renderBattlePartyQuickBar = renderBattlePartyQuickBar;
  window.renderCombatCommandDock = renderCombatCommandDock;
  window.selectCombatMember = selectCombatMember;
  window.handleDockSkillCardClick = handleDockSkillCardClick;
  window.handleDockSkillCast = handleDockSkillCast;
  window.setDockSkillTab = setDockSkillTab;
  window.changeDockSkillPage = changeDockSkillPage;
  window.hideBattleArena = hideBattleArena;
  window.selectBattleTarget = selectBattleTarget;
  window.toggleBattleMode = toggleBattleMode;
  window.selectPartyMemberForSkill = selectPartyMemberForSkill;
}
