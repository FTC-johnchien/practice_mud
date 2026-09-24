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
  const enemiesBox = document.getElementById('battle-enemies-container');
  const badge = document.getElementById('battle-status-badge');
  const countBadge = document.getElementById('battle-enemies-count');
  const focusNameEl = document.getElementById('battle-focus-name');

  if (!panel || !enemiesBox || !battle) return;

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
    const buffsHtml = renderBuffBadges(e.activeBuffs);

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
        ${buffsHtml}
      </div>
    `;
    enemiesBox.appendChild(card);
  });

  const lastParty = store.get('lastParty');
  if (lastParty) {
    renderBattlePartyQuickBar(lastParty);
  }
}

/**
 * 輔助函式：渲染 WoW 風格狀態效果膠囊列
 */
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
 * 渲染戰場我方隊員快速資訊列
 * @param {Object} party 小隊狀態
 */
export function renderBattlePartyQuickBar(party) {
  const bar = document.getElementById('battle-party-quick-bar');
  if (!bar || !party || !party.members) return;

  const cards = bar.querySelectorAll('.battle-party-mini-card');
  const selectedMemberIdx = store.get('selectedMemberIdx');

  if (cards.length !== party.members.length) {
    bar.innerHTML = party.members.map((m, idx) => {
      const isSelected = (selectedMemberIdx === idx);
      const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
      const hpPct = Math.min(100, Math.max(0, (m.hp / m.maxHp) * 100));
      const rowBadge = m.row === 'FRONT' ? '前衛' : '後衛';

      return `
        <div class="battle-party-mini-card ${isSelected ? 'active-selected' : ''}" data-idx="${idx}"
          onclick="window.selectPartyMemberForSkill ? window.selectPartyMemberForSkill(${idx}) : window.selectPartyMember(${idx})" title="點擊展開 #${idx + 1} ${m.name} 的專屬技能盤">
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
          ${renderBuffBadges(m.activeBuffs)}
        </div>
      `;
    }).join('');
  } else {
    party.members.forEach((m, idx) => {
      const card = cards[idx];
      if (!card) return;
      const isSelected = (selectedMemberIdx === idx);
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
 * 切換戰鬥模式按鈕與探索模式按鈕
 * @param {boolean} inBattle 是否處於戰鬥中
 */
export function toggleBattleMode(inBattle) {
  const actGroup = document.getElementById('action-buttons-group');
  const btlGroup = document.getElementById('battle-buttons-group');
  const modeBadge = document.getElementById('control-mode-badge');

  if (actGroup && btlGroup) {
    if (inBattle) {
      actGroup.classList.add('hidden');
      btlGroup.classList.remove('hidden');
      if (modeBadge && !store.get('isInputFocused')) {
        modeBadge.className = 'mode-badge mode-typing';
        modeBadge.style.background = 'rgba(239, 68, 68, 0.2)';
        modeBadge.style.borderColor = '#ef4444';
        modeBadge.style.color = '#fca5a5';
        modeBadge.innerText = '⚔️ 戰鬥交鋒';
      }
    } else {
      btlGroup.classList.add('hidden');
      actGroup.classList.remove('hidden');
      if (modeBadge && !store.get('isInputFocused')) {
        modeBadge.className = 'mode-badge mode-stepping';
        modeBadge.style.background = 'rgba(16, 185, 129, 0.2)';
        modeBadge.style.borderColor = '#10b981';
        modeBadge.style.color = '#6ee7b7';
        modeBadge.innerText = '🧭 靈境步進';
      }
    }
  }
}

export function selectPartyMemberForSkill(idx) {
  selectPartyMember(idx);
}

// 相容掛載至 window
if (typeof window !== 'undefined') {
  window.renderBattleArena = renderBattleArena;
  window.renderBattlePartyQuickBar = renderBattlePartyQuickBar;
  window.hideBattleArena = hideBattleArena;
  window.selectBattleTarget = selectBattleTarget;
  window.toggleBattleMode = toggleBattleMode;
  window.selectPartyMemberForSkill = selectPartyMemberForSkill;
}
