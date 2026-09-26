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
  const backEnemies = enemies.filter(e => (e.row || 'FRONT').toUpperCase() === 'BACK');
  const frontEnemies = enemies.filter(e => (e.row || 'FRONT').toUpperCase() !== 'BACK');

  function createEnemyCard(e) {
    const card = document.createElement('div');
    const isTarget = (selectedEnemy && selectedEnemy.index === e.index) || e.isTarget || (battle.selectedTargetIndex === e.index);
    const isAlive = e.alive;
    const rowClass = (e.row || 'FRONT').toLowerCase();

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
        ${e.targetMemberName ? `<span class="enemy-aggro-badge" style="font-size:11px; color:#fca5a5; background:#450a0a; border:1px solid #ef4444; border-radius:3px; padding:1px 5px; display:inline-flex; align-items:center; gap:2px;" title="怪物當前仇恨鎖定目標">🎯 盯上: ${e.targetMemberName}</span>` : ''}
        ${e.isStunned ? '<span class="enemy-stun-badge">💫 眩暈中</span>' : ''}
        ${buffsHtml}
      </div>
    `;
    return card;
  }

  // 1. 敵方後排 (位於最上方)
  if (backEnemies.length > 0) {
    const tierBack = document.createElement('div');
    tierBack.className = 'battle-tier-wrapper tier-enemy-back';
    tierBack.innerHTML = `<div class="battle-tier-label">〈 敵方後衛陣線 〉</div><div class="battle-tier-cards"></div>`;
    const cardsBox = tierBack.querySelector('.battle-tier-cards');
    backEnemies.forEach(e => cardsBox.appendChild(createEnemyCard(e)));
    enemiesBox.appendChild(tierBack);
  }

  // 2. 敵方前排 (緊鄰交鋒線)
  if (frontEnemies.length > 0) {
    const tierFront = document.createElement('div');
    tierFront.className = 'battle-tier-wrapper tier-enemy-front';
    tierFront.innerHTML = `<div class="battle-tier-label">〈 敵方前衛陣線 〉</div><div class="battle-tier-cards"></div>`;
    const cardsBox = tierFront.querySelector('.battle-tier-cards');
    frontEnemies.forEach(e => cardsBox.appendChild(createEnemyCard(e)));
    enemiesBox.appendChild(tierFront);
  }

  // 渲染戰場我方隊伍陣列與底部暗黑地牢戰備指揮台
  const lastParty = store.get('lastParty');
  if (lastParty) {
    renderBattlePartyQuickBar(lastParty);
  }
}

/**
 * 輔助函式：渲染狀態效果膠囊列
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
        <span class="bpmc-name" style="font-weight:bold; color:${isAlive ? '#e2e8f0' : '#94a3b8'}; font-size:13px;">#${idx + 1} ${m.name}</span>
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
      ${m.formationSlotName ? `
        <div class="bpmc-slot-badge" style="font-size:11px; margin-top:2px; padding:2px 5px; border-radius:3px; display:flex; justify-content:space-between; align-items:center; ${isAlive && m.formationSlotActive ? 'background:rgba(16,185,129,0.12); color:#6ee7b7; border:1px solid rgba(16,185,129,0.3);' : 'background:rgba(239,68,68,0.12); color:#fca5a5; border:1px solid rgba(239,68,68,0.3);'}"
             title="陣法孔位：${m.formationSlotName} - ${m.formationSlotBonus || ''}">
          <span style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">💠 ${m.formationSlotName}</span>
          ${isAlive && m.formationSlotActive ? '<span style="font-size:10px; flex-shrink:0;">✓生效</span>' : '<span style="font-size:10px; color:#ef4444; flex-shrink:0;">' + (!isAlive ? '✗離陣' : '✗不符') + '</span>'}
        </div>
      ` : ''}
      <div class="bpmc-buffs-wrap">
        ${renderBuffBadges(m.activeBuffs)}
      </div>
    `;
    return card;
  }

  if (needFullRebuild) {
    bar.dataset.structureKey = structureKey;
    bar.innerHTML = '';

    // 1. 我方前衛 (緊鄰交鋒線)
    if (frontMembers.length > 0) {
      const tierFront = document.createElement('div');
      tierFront.className = 'battle-tier-wrapper tier-party-front';
      tierFront.innerHTML = `<div class="battle-tier-label" style="color:#f87171;">〈 我方前衛陣線 〉</div><div class="battle-tier-cards" style="display:flex; justify-content:center; gap:8px;"></div>`;
      const cardsBox = tierFront.querySelector('.battle-tier-cards');
      frontMembers.forEach(item => cardsBox.appendChild(createPartyCard(item.m, item.idx)));
      bar.appendChild(tierFront);
    }

    // 2. 我方中衛 (若陣法有中衛則展開)
    if (middleMembers.length > 0) {
      const tierMiddle = document.createElement('div');
      tierMiddle.className = 'battle-tier-wrapper tier-party-middle';
      tierMiddle.innerHTML = `<div class="battle-tier-label" style="color:#c084fc;">〈 我方中衛陣線 〉</div><div class="battle-tier-cards" style="display:flex; justify-content:center; gap:8px;"></div>`;
      const cardsBox = tierMiddle.querySelector('.battle-tier-cards');
      middleMembers.forEach(item => cardsBox.appendChild(createPartyCard(item.m, item.idx)));
      bar.appendChild(tierMiddle);
    }

    // 3. 我方後衛 (位於下方)
    if (backMembers.length > 0) {
      const tierBack = document.createElement('div');
      tierBack.className = 'battle-tier-wrapper tier-party-back';
      tierBack.innerHTML = `<div class="battle-tier-label" style="color:#60a5fa;">〈 我方後衛陣線 〉</div><div class="battle-tier-cards" style="display:flex; justify-content:center; gap:8px;"></div>`;
      const cardsBox = tierBack.querySelector('.battle-tier-cards');
      backMembers.forEach(item => cardsBox.appendChild(createPartyCard(item.m, item.idx)));
      bar.appendChild(tierBack);
    }

    // 4. 陣亡重傷待援區 (若有隊員陣亡)
    if (deadMembers.length > 0) {
      const tierDead = document.createElement('div');
      tierDead.className = 'battle-tier-wrapper tier-party-dead';
      tierDead.innerHTML = `<div class="battle-tier-label" style="color:#ef4444; font-size:10px;">〈 💀 陣亡重傷待援 〉</div><div class="battle-tier-cards" style="display:flex; justify-content:center; gap:8px;"></div>`;
      const cardsBox = tierDead.querySelector('.battle-tier-cards');
      deadMembers.forEach(item => cardsBox.appendChild(createPartyCard(item.m, item.idx)));
      bar.appendChild(tierDead);
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

  const heroProfileEl = document.getElementById('dock-hero-profile');
  const skillDeckEl = document.getElementById('dock-skill-deck');
  const tacticsDeckEl = document.getElementById('dock-tactics-deck');

  const curMemberIdxStr = String(selectedMemberIdx);

  // 1. 左側英雄面板 (In-place 更新數值，漏斗 GCD 置於頂部標題右側絕不推擠版面)
  if (heroProfileEl) {
    const hpPct = Math.min(100, Math.max(0, (m.hp / m.maxHp) * 100));
    const sanPct = Math.min(100, Math.max(0, (m.san / m.maxSan) * 100));
    const sanClass = `san-${(m.sanLevel || 'NORMAL').toLowerCase()}`;
    const rowBadge = m.row === 'FRONT' ? '前衛' : '後衛';

    const resType = m.resourceType || 'MP';
    const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
    const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;
    const resPct = Math.min(100, Math.max(0, maxRes > 0 ? (curRes / maxRes) * 100 : 0));
    let resLabel = (resType === 'MP') ? `MP ${curRes}/${maxRes}` : `戰氣 ${curRes}/${maxRes}`;
    let resFillClass = (resType === 'MP') ? 'mp-fill' : 'sp-fill';

    const needHeroRebuild = (heroProfileEl.dataset.memberIdx !== curMemberIdxStr);

    if (needHeroRebuild) {
      heroProfileEl.dataset.memberIdx = curMemberIdxStr;
      heroProfileEl.innerHTML = `
        <div class="dock-hero-header">
          <div class="dock-hero-header-left">
            <span class="dock-hero-name" title="${m.name}">#${selectedMemberIdx + 1} ${m.name}</span>
            <span class="dock-hero-lvl">Lv.${m.level || 1}</span>
            ${m.className ? `<span class="dock-hero-class">${m.className}</span>` : ''}
          </div>
          <div class="dock-hero-header-right">
            <span id="dock-hero-gcd" class="dock-hero-gcd-badge hidden">⏳ 0.0s</span>
          </div>
        </div>
        <div class="dock-hero-bars">
          <div class="mini-bar-wrap" title="氣血 HP">
            <div class="mini-bar hp-fill" style="width: ${hpPct}%"></div>
            <span class="mini-val hp-val">HP ${m.hp}/${m.maxHp}</span>
          </div>
          <div class="mini-bar-wrap" title="${resLabel}">
            <div class="mini-bar res-fill ${resFillClass}" style="width: ${resPct}%"></div>
            <span class="mini-val res-val">${resLabel}</span>
          </div>
          <div class="mini-bar-wrap san-bar-wrap" title="道心 SAN">
            <div class="mini-bar san-fill ${sanClass}" style="width: ${sanPct}%"></div>
            <span class="mini-val san-val ${sanClass}">SAN ${m.san}/${m.maxSan}</span>
          </div>
        </div>
        ${m.formationSlotName ? `
          <div id="dock-hero-slot" class="dock-formation-slot ${m.formationSlotActive ? 'active' : 'inactive'}"
               title="陣法孔位：${m.formationSlotName} - ${m.formationSlotBonus || ''}">
            <span class="slot-text" style="overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">💠 ${m.formationSlotName} <small>${m.formationSlotBonus || ''}</small></span>
            <span class="slot-status">${m.formationSlotActive ? '✓生效' : '✗站位不符'}</span>
          </div>` : '<div id="dock-hero-slot"></div>'}
        <div id="dock-hero-buffs" class="dock-buffs-wrap">
          ${renderBuffBadges(m.activeBuffs)}
        </div>
      `;
    } else {
      // In-place 更新血量、能量、SAN 與頂部 GCD 徽章 (零 DOM 銷毀、零高度跳動)
      const hpFill = heroProfileEl.querySelector('.hp-fill');
      const hpVal = heroProfileEl.querySelector('.hp-val');
      if (hpFill) hpFill.style.width = `${hpPct}%`;
      if (hpVal) hpVal.textContent = `HP ${m.hp}/${m.maxHp}`;

      const resFill = heroProfileEl.querySelector('.res-fill');
      const resVal = heroProfileEl.querySelector('.res-val');
      if (resFill) {
        resFill.style.width = `${resPct}%`;
        resFill.className = `mini-bar res-fill ${resFillClass}`;
      }
      if (resVal) resVal.textContent = resLabel;

      const sanFill = heroProfileEl.querySelector('.san-fill');
      const sanVal = heroProfileEl.querySelector('.san-val');
      if (sanFill) {
        sanFill.style.width = `${sanPct}%`;
        sanFill.className = `mini-bar san-fill ${sanClass}`;
      }
      if (sanVal) sanVal.textContent = `SAN ${m.san}/${m.maxSan}`;

      const slotEl = document.getElementById('dock-hero-slot');
      if (slotEl && m.formationSlotName) {
        slotEl.className = `dock-formation-slot ${m.formationSlotActive ? 'active' : 'inactive'}`;
        const slotStatus = slotEl.querySelector('.slot-status');
        if (slotStatus) slotStatus.textContent = m.formationSlotActive ? '✓生效' : '✗站位不符';
      }
    }

    // 頂部固定位置 GCD / 施法指示器更新 (位於標題右側，絕不推擠下層屬性條)
    const gcdBadge = document.getElementById('dock-hero-gcd');
    if (gcdBadge) {
      if (m.isCasting) {
        gcdBadge.classList.remove('hidden');
        gcdBadge.className = 'dock-hero-gcd-badge casting';
        gcdBadge.textContent = `🌀 吟唱 ${(m.castingRemainingMs / 1000).toFixed(1)}s`;
        gcdBadge.title = `正在施法【${m.castingSkillName}】，剩餘 ${(m.castingRemainingMs / 1000).toFixed(1)} 秒`;
      } else if (m.isOnGcd) {
        gcdBadge.classList.remove('hidden');
        gcdBadge.className = 'dock-hero-gcd-badge';
        gcdBadge.textContent = `⏳ 調息 ${(m.remainingGcdMs / 1000).toFixed(1)}s`;
        gcdBadge.title = `全域招式調息中 (GCD)，剩餘 ${(m.remainingGcdMs / 1000).toFixed(1)} 秒`;
      } else {
        gcdBadge.classList.add('hidden');
      }
    }
  }

  // 2. 中央技能動作欄 (實裝單頁 20 筆上限的分頁機制與 In-place 更新，零閃爍)
  if (skillDeckEl) {
    const activeTab = store.get('dockSkillTab') || 'ALL';
    const skills = m.skills || [];
    const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);

    const resType = m.resourceType || 'MP';
    const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
    const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;
    const resBadgeLabel = (resType === 'MP') ? `🔮 真元 ${curRes}/${maxRes}` : `⚡ 戰氣 ${curRes}/${maxRes}`;

    let filteredSkills = skills.filter(s => {
      const cat = (s.category || 'CLASS').toUpperCase();
      if (activeTab === 'ALL') return true;
      if (activeTab === 'WEAPON') return cat === 'WEAPON';
      if (activeTab === 'CLASS') return cat === 'CLASS';
      if (activeTab === 'SPELL') return cat === 'SPELL';
      if (activeTab === 'COMBO') return (cat === 'COMBO' || s.synergy);
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

    const needSkillRebuild = (skillDeckEl.dataset.memberIdx !== curMemberIdxStr) ||
                             (skillDeckEl.dataset.tab !== activeTab) ||
                             (skillDeckEl.dataset.page !== String(currentPage)) ||
                             (skillDeckEl.dataset.skillCount !== String(skills.length));

    if (needSkillRebuild) {
      skillDeckEl.dataset.memberIdx = curMemberIdxStr;
      skillDeckEl.dataset.tab = activeTab;
      skillDeckEl.dataset.page = String(currentPage);
      skillDeckEl.dataset.skillCount = String(skills.length);

      let skillsGridHtml = '';
      if (pageSkills.length === 0) {
        skillsGridHtml = `<div class="dock-empty-skills">當前分類暫無可用招式</div>`;
      } else {
        skillsGridHtml = pageSkills.map((s, sIdx) => {
          return `
            <button class="dock-skill-card" data-skill-idx="${sIdx}" data-skill-id="${s.id}">
              <div class="dock-skill-title-row">
                <span class="dock-skill-icon">${s.icon || '⚡'}</span>
                <span class="dock-skill-name">${s.name}</span>
              </div>
              <div class="dock-skill-meta">
                <span class="dock-skill-cost ${s.costType === 'MP' ? 'cost-mp' : 'cost-sp'}"></span>
                <span class="dock-skill-cd hidden"></span>
              </div>
            </button>
          `;
        }).join('');
      }

      skillDeckEl.innerHTML = `
        <div class="dock-skill-top-bar">
          <div class="dock-skill-tabs">
            <button class="dock-tab ${activeTab === 'ALL' ? 'active' : ''}" onclick="window.setDockSkillTab('ALL')">全部</button>
            <button class="dock-tab ${activeTab === 'WEAPON' ? 'active' : ''}" onclick="window.setDockSkillTab('WEAPON')">⚔️ 武學</button>
            <button class="dock-tab ${activeTab === 'CLASS' ? 'active' : ''}" onclick="window.setDockSkillTab('CLASS')">🛡️ 職業</button>
            <button class="dock-tab ${activeTab === 'SPELL' ? 'active' : ''}" onclick="window.setDockSkillTab('SPELL')">🔮 法術</button>
            <button class="dock-tab ${activeTab === 'COMBO' ? 'active' : ''}" onclick="window.setDockSkillTab('COMBO')">🌟 合擊</button>
          </div>
          <div class="dock-skill-paginator">
            <button class="dock-page-btn" ${currentPage <= 1 ? 'disabled' : ''} onclick="window.changeDockSkillPage(-1)" title="上一頁">◀</button>
            <span class="dock-page-text">${currentPage}/${totalPages}</span>
            <button class="dock-page-btn" ${currentPage >= totalPages ? 'disabled' : ''} onclick="window.changeDockSkillPage(1)" title="下一頁">▶</button>
          </div>
          <div class="dock-res-badge">${resBadgeLabel}</div>
        </div>
        <div class="dock-skills-grid">
          ${skillsGridHtml}
        </div>
      `;
    }

    // 更新能量徽章文字
    const resBadgeEl = skillDeckEl.querySelector('.dock-res-badge');
    if (resBadgeEl) resBadgeEl.textContent = resBadgeLabel;

    // In-place 更新當前頁各技能卡狀態 (冷卻、可用性、點擊事件，絕不重建節點)
    const cardNodes = skillDeckEl.querySelectorAll('.dock-skills-grid .dock-skill-card');
    pageSkills.forEach((s, sIdx) => {
      const cardBtn = cardNodes[sIdx];
      if (!cardBtn) return;

      const onCd = s.remainingCooldownMs > 0;
      let resOk = s.available;
      if (resOk === undefined) {
        resOk = (curRes >= (s.costValue || 0));
      }
      const canCast = Boolean(resOk && !onCd && isAlive);
      const cdSec = onCd ? (s.remainingCooldownMs / 1000).toFixed(1) : 0;

      let costLabel = s.costDescription;
      if (!costLabel || costLabel === 'undefined') {
        if (!s.costValue || s.costValue <= 0) {
          costLabel = '無消耗';
        } else if (s.costType === 'SP' || s.costType === 'RAGE' || s.costType === 'COMBO') {
          costLabel = `${s.costValue} 戰氣`;
        } else {
          costLabel = `${s.costValue} 真元`;
        }
      }

      // 更新樣式與提示 (防閃爍)
      cardBtn.className = `dock-skill-card ${canCast ? '' : 'cant-cast'}${s.synergy && canCast ? ' synergy-glow' : ''}`;
      cardBtn.title = `${s.name} - ${s.description}`;

      const costSpan = cardBtn.querySelector('.dock-skill-cost');
      if (costSpan) {
        costSpan.className = `dock-skill-cost ${s.costType === 'MP' ? 'cost-mp' : 'cost-sp'}`;
        costSpan.textContent = costLabel;
      }

      const cdSpan = cardBtn.querySelector('.dock-skill-cd');
      if (cdSpan) {
        if (onCd) {
          cdSpan.classList.remove('hidden');
          cdSpan.textContent = `⌛ ${cdSec}s`;
        } else {
          cdSpan.classList.add('hidden');
        }
      }

      cardBtn.onclick = () => {
        handleDockSkillCast(selectedMemberIdx, s.id, canCast, s.name, costLabel, onCd, cdSec, s.costDescription || '', Boolean(s.synergy));
      };
    });
  }

  // 3. 右側戰術指令欄 (明確的戰術行動按鈕：行囊、選單、遁地，徹底移除容易混淆的換位按鈕)
  if (tacticsDeckEl) {
    if (tacticsDeckEl.dataset.initialized !== 'true') {
      tacticsDeckEl.dataset.initialized = 'true';
      tacticsDeckEl.innerHTML = `
        <div class="dock-tactics-title">戰術指令</div>
        <div class="dock-tactic-actions-list">
          <button class="dock-tactic-sub-btn" onclick="window.toggleBagDrawer ? window.toggleBagDrawer() : null" title="開啟公共行囊 (B)">
            🎒 行囊 (B)
          </button>
          <button class="dock-tactic-sub-btn" onclick="window.openMainMenu ? window.openMainMenu('FORMATION') : (window.togglePartyModal ? window.togglePartyModal() : null)" title="開啟功能選單 (C)">
            📜 選單 (C)
          </button>
          <button class="dock-tactic-sub-btn btn-tactic-flee" onclick="window.send('battle flee')" title="遁地撤退 (-5 SAN) (Esc)">
            🏃 遁地 (Esc)
          </button>
        </div>
      `;
    }
  }
}

/**
 * 處理戰備指揮台技能施放
 */
export function handleDockSkillCast(memberIdx, skillId, canCast, skillName, costLabel, onCd, cdSec, costDesc, isSynergy) {
  if (!canCast) {
    let warnMsg = '';
    if (onCd) {
      warnMsg = `⏳【調息中】「${skillName}」正在調息冷卻中，尚需 ${cdSec} 秒！`;
    } else if (costDesc && costDesc.includes('需')) {
      warnMsg = `⚠️【兵刃未備】「${skillName}」${costDesc}！請在小隊面板 (P) 佩戴對應兵刃。`;
    } else {
      warnMsg = `⚠️【元氣未備】「${skillName}」釋放條件不足（需 ${costLabel}）！`;
    }
    if (typeof window.appendHtml === 'function') {
      window.appendHtml(warnMsg, '#f59e0b');
    }
    const focusHint = document.getElementById('battle-focus-hint');
    if (focusHint) {
      focusHint.innerHTML = `<span style="color:#f59e0b; font-weight:bold;">${warnMsg}</span>`;
      setTimeout(() => {
        const lastBattle = store.get('lastBattle');
        if (focusHint && lastBattle) {
          renderBattleArena(lastBattle);
        }
      }, 3500);
    }
    return;
  }
  if (isSynergy) {
    sendCmd(`battle combo ${skillId}`);
  } else {
    sendCmd(`skill cast ${memberIdx} ${skillId}`);
  }
}

/**
 * 切換戰備指揮台技能分類標籤
 */
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

  if (inBattle) {
    // 戰鬥模式：切換提示文字為戰鬥提示，保持常駐 40px 工具列可用 (選單/行囊/終端)
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
    // 探索模式：切換提示為探索指引，恢復外層隊伍 HUD
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
