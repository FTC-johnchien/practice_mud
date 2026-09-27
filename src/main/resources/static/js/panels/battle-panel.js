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

  // 尋找當前鎖定集火目標 (優先以 battle.selectedTargetIndex 或 isTarget 判定)
  let selectedEnemy = null;
  if (battle.selectedTargetIndex !== undefined && battle.selectedTargetIndex >= 0) {
    selectedEnemy = livingEnemies.find(e => e.index === battle.selectedTargetIndex);
  }
  if (!selectedEnemy) {
    selectedEnemy = livingEnemies.find(e => e.isTarget || e.isSelectedTarget);
  }
  if (!selectedEnemy) {
    // 預設鎖定前排第一個存活者，或任意存活者
    selectedEnemy = livingEnemies.find(e => (e.row || 'FRONT').toUpperCase() !== 'BACK') || livingEnemies[0];
  }

  if (focusNameEl) {
    if (selectedEnemy) {
      focusNameEl.innerText = `#${selectedEnemy.index + 1} ${selectedEnemy.name}`;
    } else {
      focusNameEl.innerText = '無（全體覆滅）';
    }
  }

  // 存活敵人前後排推進：若原本前排存活怪物全滅，後排存活怪物自動向前挺進！
  const hasLivingFront = livingEnemies.some(e => (e.row || 'FRONT').toUpperCase() !== 'BACK');
  let activeFront = [];
  let activeBack = [];

  if (hasLivingFront) {
    activeFront = livingEnemies.filter(e => (e.row || 'FRONT').toUpperCase() !== 'BACK');
    activeBack = livingEnemies.filter(e => (e.row || 'FRONT').toUpperCase() === 'BACK');
  } else {
    // 前排全滅，後排活敵全部推進至前排！
    activeFront = livingEnemies;
    activeBack = [];
  }

  // 結構鍵判定：怪物數量、前後排分佈或巨怪狀態變更時才重繪 DOM，其餘時間全 In-place 更新防閃爍！
  const structureKey = [
    activeBack.map(e => `${e.index}_${e.id}_${e.isLarge ? 'L' : 'S'}`).join(','),
    activeFront.map(e => `${e.index}_${e.id}_${e.isLarge ? 'L' : 'S'}`).join(',')
  ].join('|');

  const needFullRebuild = (enemiesBox.dataset.structureKey !== structureKey);


    function createEnemyCard(e) {
      const card = document.createElement('div');
      card.dataset.enemyIndex = String(e.index);
      const isTarget = (selectedEnemy && selectedEnemy.index === e.index) || e.isTarget || (battle.selectedTargetIndex === e.index);
      const isLarge = Boolean(e.isLarge);

      card.className = `enemy-card${isLarge ? ' size-2x2' : ''}${isTarget ? ' selected-target' : ''}`;
      card.title = `點擊鎖定 ${e.name} 為集火目標`;
      card.onclick = () => selectBattleTarget(e.index);

      const hpPct = Math.min(100, Math.max(0, (e.hp / e.maxHp) * 100));

      const targetBadge = e.targetMemberName
        ? `<span class="enemy-aggro-badge" style="background:#450a0a; color:#fca5a5; border:1px solid #7f1d1d; padding:1px 4px; border-radius:3px; font-size:10px;">🎯 ${e.targetMemberName}</span>`
        : '';
      const stunBadge = e.isStunned
        ? `<span class="enemy-stun-badge" style="background:#422006; color:#fdba74; border:1px solid #9a3412; padding:1px 4px; border-radius:3px; font-size:10px;">💫 暈眩</span>`
        : '';

      let buffsHtml = '';
      if (e.activeBuffs) {
          buffsHtml = e.activeBuffs.map(b => `<span class="enemy-buff-badge" style="background:#172554; color:#93c5fd; border:1px solid #1e3a8a; padding:1px 4px; border-radius:3px; font-size:10px;">${b.icon || '✨'} ${b.name}</span>`).join('');
      }

      if (isLarge) {
        card.innerHTML = `
          <div class="enemy-name-row">
            <div style="display:flex; align-items:center; gap:6px; min-width:0; flex:1; overflow:hidden;">
              <span class="large-enemy-badge">👹 巨型 2x2</span>
              <span class="enemy-name" style="font-size:var(--font-base); font-weight:bold;" title="${e.name}">#${e.index + 1} ${e.name}</span>
            </div>
            <div class="enemy-badges-right" style="display:flex; gap:4px;">
              ${targetBadge}
              ${stunBadge}
              ${buffsHtml}
            </div>
          </div>
          <div class="large-enemy-info" style="font-size:var(--font-xs); color:#c084fc; display:flex; align-items:center; gap:6px;">
            <span>🛡️ 雙倍防禦 • 巨型陣樁 (2x2 佔位)</span>
          </div>
          <div class="enemy-hp-wrap large-hp-wrap" title="生命 HP: ${e.hp}/${e.maxHp}">
            <div class="enemy-hp-bar" style="width: ${hpPct}%; background: linear-gradient(90deg, #dc2626, #f97316);"></div>
            <span class="enemy-hp-text">HP ${e.hp}/${e.maxHp}</span>
          </div>
        `;
      } else {
        card.innerHTML = `
          <div class="enemy-name-row">
            <span class="enemy-name" title="${e.name}">#${e.index + 1} ${e.name}</span>
            <div class="enemy-badges-right" style="display:flex; gap:4px;">
              ${targetBadge}
              ${stunBadge}
              ${buffsHtml}
            </div>
          </div>
          <div class="enemy-hp-wrap" title="生命 HP: ${e.hp}/${e.maxHp}">
            <div class="enemy-hp-bar" style="width: ${hpPct}%"></div>
            <span class="enemy-hp-text">HP ${e.hp}/${e.maxHp}</span>
          </div>
        `;
      }
      return card;
    }


  if (needFullRebuild) {
    enemiesBox.dataset.structureKey = structureKey;
    enemiesBox.innerHTML = '';

    // 1. 敵方後排 (若有存活者且前排未全滅)
    if (activeBack.length > 0) {
      const tierBack = document.createElement('div');
      tierBack.className = 'battle-tier-wrapper tier-enemy-back';
      const cardsBox = document.createElement('div');
      cardsBox.className = 'battle-tier-cards';
      activeBack.forEach(e => cardsBox.appendChild(createEnemyCard(e)));
      tierBack.appendChild(cardsBox);
      enemiesBox.appendChild(tierBack);
    }

    // 2. 敵方前排 (緊鄰交鋒線)
    if (activeFront.length > 0) {
      const tierFront = document.createElement('div');
      tierFront.className = 'battle-tier-wrapper tier-enemy-front';
      const cardsBox = document.createElement('div');
      cardsBox.className = 'battle-tier-cards';
      activeFront.forEach(e => cardsBox.appendChild(createEnemyCard(e)));
      tierFront.appendChild(cardsBox);
      enemiesBox.appendChild(tierFront);
    }

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
  const bar = document.getElementById('battle-party-quick-bar');
  if (!bar || !party || !party.members) return;

  let selectedMemberIdx = store.get('selectedMemberIdx');
  if (selectedMemberIdx === undefined || selectedMemberIdx === null || selectedMemberIdx < 0 || selectedMemberIdx >= party.members.length) {
    selectedMemberIdx = 0;
    store.setState({ selectedMemberIdx: 0 });
  }

  const allMembers = party.members.map((m, idx) => ({ m, idx }));
  const aliveMembers = allMembers.filter(item => (item.m.alive !== undefined ? item.m.alive : item.m.hp > 0));
  const deadMembers = allMembers.filter(item => !(item.m.alive !== undefined ? item.m.alive : item.m.hp > 0));

  const frontMembers = aliveMembers.filter(item => item.m.row === 'FRONT');
  const middleMembers = aliveMembers.filter(item => item.m.row === 'MIDDLE');
  const backMembers = aliveMembers.filter(item => item.m.row === 'BACK');

  const structureKey = party.members.map(m => `${m.id || m.name}_${m.row}_${(m.alive !== undefined ? m.alive : m.hp > 0)}`).join('|');
  const needFullRebuild = (bar.dataset.structureKey !== structureKey);

  function createPartyCard(m, idx) {
    const isSelected = (selectedMemberIdx === idx);
    const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
    const hpPct = Math.min(100, Math.max(0, (m.hp / m.maxHp) * 100));
    const rowBadge = m.row === 'FRONT' ? '前衛' : (m.row === 'MIDDLE' ? '中衛' : '後衛');
    const rowColor = m.row === 'FRONT' ? '#f87171' : (m.row === 'MIDDLE' ? '#c084fc' : '#60a5fa');

    const card = document.createElement('div');
    card.dataset.memberIdx = String(idx);
    card.className = `battle-party-mini-card row-${(m.row || 'FRONT').toLowerCase()} ${isSelected ? 'active-selected' : ''} ${!isAlive ? 'is-dead' : ''}`;
    card.onclick = () => selectCombatMember(idx);
    card.title = `點選 #${idx + 1} ${m.name} 切換戰備指揮台`;
    if (!isAlive) {
      card.style.opacity = '0.7';
      card.style.borderColor = '#ef4444';
    }

    card.innerHTML = `
      <div style="display:flex; justify-content:space-between; align-items:center;">
        <span class="bpmc-name" style="font-weight:bold; color:${isAlive ? '#e2e8f0' : '#94a3b8'}; font-size:13px;">${m.name}</span>
        <div style="display:flex; align-items:center; gap:4px;">
          ${!isAlive ? '<span style="font-size:var(--font-xs); color:#ef4444; background:rgba(239,68,68,0.2); border:1px solid #ef4444; border-radius:3px; padding:1px 4px;">💀陣亡</span>' : ''}
        </div>
      </div>
      <div class="enemy-hp-wrap" style="height:12px; margin:3px 0;">
        <div class="hp-bar" style="width:${hpPct}%; background:${isAlive ? '#10b981' : '#6b7280'}; height:100%;"></div>
      </div>
      <div style="font-size:11px; color:#94a3b8; display:flex; justify-content:space-between; align-items:center;">
        <span class="bpmc-hp-text">HP ${m.hp}/${m.maxHp}</span>
        <span class="bpmc-select-indicator" style="color:${isAlive ? '#38bdf8' : '#ef4444'}; font-weight:bold;">${isSelected ? '▶ 當前選中' : (isAlive ? '點選切換' : '點選施救')}</span>
      </div>
      <div class="bpmc-buffs-wrap">
        ${renderBuffBadges(m.activeBuffs)}
      </div>
    `;
    return card;
  }

  if (needFullRebuild) {
    bar.dataset.structureKey = structureKey;
    bar.innerHTML = '';

    const tiers = [
      { key: 'FRONT', list: frontMembers, cls: 'tier-party-front' },
      { key: 'MIDDLE', list: middleMembers, cls: 'tier-party-middle' },
      { key: 'BACK', list: backMembers, cls: 'tier-party-back' }
    ];

    let hasAnyTier = false;
    tiers.forEach(t => {
      if (t.list.length > 0) {
        hasAnyTier = true;
        const tierWrap = document.createElement('div');
        tierWrap.className = `battle-tier-wrapper ${t.cls}`;
        const cardsBox = document.createElement('div');
        cardsBox.className = 'battle-tier-cards';
        t.list.forEach(item => cardsBox.appendChild(createPartyCard(item.m, item.idx)));
        tierWrap.appendChild(cardsBox);
        bar.appendChild(tierWrap);
      }
    });

    if (!hasAnyTier) {
      const tierWrap = document.createElement('div');
      tierWrap.className = 'battle-tier-wrapper tier-party-front';
      const cardsBox = document.createElement('div');
      cardsBox.className = 'battle-tier-cards';
      aliveMembers.forEach(item => cardsBox.appendChild(createPartyCard(item.m, item.idx)));
      tierWrap.appendChild(cardsBox);
      bar.appendChild(tierWrap);
    }

    if (deadMembers.length > 0) {
      const deadWrap = document.createElement('div');
      deadWrap.className = 'battle-tier-wrapper tier-party-dead';
      const cardsBox = document.createElement('div');
      cardsBox.className = 'battle-tier-cards';
      deadMembers.forEach(item => cardsBox.appendChild(createPartyCard(item.m, item.idx)));
      deadWrap.appendChild(cardsBox);
      bar.appendChild(deadWrap);
    }
  } else {
    // In-place 更新隊員卡片數值與選取狀態，絕不重構 DOM (防閃爍)
    const cardNodes = bar.querySelectorAll('.battle-party-mini-card');
    cardNodes.forEach(card => {
      const idx = parseInt(card.dataset.memberIdx, 10);
      const m = party.members[idx];
      if (!m) return;

      const isSelected = (selectedMemberIdx === idx);
      const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
      const hpPct = Math.min(100, Math.max(0, (m.hp / m.maxHp) * 100));

      card.classList.toggle('active-selected', isSelected);

      const hpBar = card.querySelector('.hp-bar');
      if (hpBar) {
        hpBar.style.width = `${hpPct}%`;
        hpBar.style.background = isAlive ? '#10b981' : '#6b7280';
      }

      const hpText = card.querySelector('.bpmc-hp-text');
      if (hpText) hpText.textContent = `HP ${m.hp}/${m.maxHp}`;

      const selInd = card.querySelector('.bpmc-select-indicator');
      if (selInd) selInd.textContent = isSelected ? '▶ 當前選中' : '點選切換';
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

  const activeTab = store.get('dockSkillTab') || 'DEFAULT';

  // Left Column: Actions
  actionsColEl.innerHTML = `
    <div style="color: var(--text-color); font-weight: bold; margin-bottom: 4px;">🎯 ${m.name}</div>
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

  // Right Column: Content
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

    contentColEl.innerHTML = `
      <div class="dock-info-view">
        <div style="display: flex; gap: 12px;">
          <div style="flex: 1;">
            <div class="mini-bar-wrap">
              <div class="mini-bar hp-fill" style="width: ${hpPct}%"></div>
              <span class="mini-val hp-val">HP ${m.hp}/${m.maxHp}</span>
            </div>
            <div class="mini-bar-wrap">
              <div class="mini-bar mp-fill ${maxMp > 0 ? '' : 'empty-res-fill'}" style="width: ${mpPct}%"></div>
              <span class="mini-val mp-val">MP ${maxMp > 0 ? curMp+'/'+maxMp : '無'}</span>
            </div>
          </div>
          <div style="flex: 1;">
            <div class="mini-bar-wrap">
              <div class="mini-bar sp-fill ${maxSp > 0 ? '' : 'empty-res-fill'}" style="width: ${spPct}%"></div>
              <span class="mini-val sp-val">SP ${maxSp > 0 ? curSp+'/'+maxSp : '無'}</span>
            </div>
            <div class="mini-bar-wrap san-bar-wrap">
              <div class="mini-bar san-fill san-${(m.sanLevel || 'NORMAL').toLowerCase()}" style="width: ${sanPct}%"></div>
              <span class="mini-val san-val">SAN ${m.san}/${m.maxSan}</span>
            </div>
          </div>
        </div>
        <div style="color: #94a3b8; font-size: 11px;">狀態加持：</div>
        <div class="card-buffs-grid" style="display: flex; flex-wrap: wrap; gap: 4px;">
          ${buffsHtml || '<span style="color:#64748b;font-size:11px;">(無狀態)</span>'}
        </div>
      </div>
    `;
  } else {
    // SKILL or SPELL tab
    const skills = m.skills || [];
    let filteredSkills = skills.filter(s => {
      const cat = (s.category || 'CLASS').toUpperCase();
      if (activeTab === 'SKILL') return cat !== 'SPELL';
      if (activeTab === 'SPELL') return cat === 'SPELL';
      return true;
    });

    const PAGE_SIZE = 20;
    const totalPages = Math.max(1, Math.ceil(filteredSkills.length / PAGE_SIZE));
    let currentPage = store.get('dockSkillPage') || 1;
    if (currentPage > totalPages) {
      currentPage = totalPages;
      store.setState({ dockSkillPage: currentPage });
    }
    const startIndex = (currentPage - 1) * PAGE_SIZE;
    const pageSkills = filteredSkills.slice(startIndex, startIndex + PAGE_SIZE);

    let slotsHtml = '';
    const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);

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
               title="${s.name}\n${s.description || ''}${onCd ? '\n[調息中 ' + cdSec + '秒]' : ''}"
               onclick="${disabled ? '' : `handleDockSkillCast('${s.id}', '${s.name}'); setDockSkillTab('DEFAULT');`}">
            <span class="skill-icon">${s.icon || '⚡'}</span>
            <span class="skill-name">${s.name}</span>
            <span class="skill-cost">${costLabel}</span>
          </div>
        `;
      } else {
        slotsHtml += `<div class="dock-mini-skill" style="opacity: 0.1; cursor: default;"></div>`;
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
  }
}

/**
 * 處理技能快捷施放
 */
export function handleDockSkillCast(skillId, skillName) {
  const selectedMemberIdx = store.get('selectedMemberIdx') || 0;
  sendCmd(`skill cast ${selectedMemberIdx} ${skillId}`);
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
  window.handleDockSkillCast = handleDockSkillCast;
  window.setDockSkillTab = setDockSkillTab;
  window.changeDockSkillPage = changeDockSkillPage;
  window.hideBattleArena = hideBattleArena;
  window.selectBattleTarget = selectBattleTarget;
  window.toggleBattleMode = toggleBattleMode;
  window.selectPartyMemberForSkill = selectPartyMemberForSkill;
}
