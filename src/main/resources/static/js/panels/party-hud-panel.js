import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';
import { eventBus } from '../core/event-bus.js';

/**
 * 6 人小隊狀態列面板 (Party HUD Panel)
 * 負責渲染成員卡片、前後衛標籤、HP/MP/SAN 狀態條、走火入魔異變進度與裝備槽位
 */

/**
 * 渲染 6 人小隊 HUD (包含前後衛分色、三態能量條及點選觸發技能盤)
 * @param {Object} party 小隊狀態資料
 */
export function renderPartyHud(party) {
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
  const selectedMemberIdx = store.get('selectedMemberIdx');
  const isSkillDrawerOpen = store.get('isSkillDrawerOpen');

  party.members.forEach((m, idx) => {
    const card = document.createElement('div');
    const isSelected = (idx === selectedMemberIdx && isSkillDrawerOpen);

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
          <button class="seal-trigger-btn" onclick="event.stopPropagation(); window.send('seal ${idx}')" title="施展太上鎮魔符，凍結異變計數，暫停行動">🔒 施符鎮魔</button>
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
    if (resType === 'SP' || resType === 'RAGE' || resType === 'COMBO' || resType === 'STAMINA' || resType === 'FORCE' || resType === 'ENERGY') {
      resFillClass = 'sp-fill';
      resLabel = `戰氣 ${curRes}/${maxRes}`;
    }

    card.innerHTML = `
      <div class="card-info-side">
        <div class="card-name-row">
          <span class="member-idx">#${idx + 1}</span>
          <span class="member-name" title="${m.name}">${m.name}</span>
          <span class="member-level-badge" title="境界等級 Lv.${m.level || 1}">Lv.${m.level || 1}</span>
          <span class="member-row badge-${m.row.toLowerCase()}">${rowBadge}</span>
          ${m.className ? `<span class="member-class-badge" title="${m.classDescription || ''}" style="font-size:11px;color:#7dd3fc;background:#0f172a;border:1px solid #0284c7;border-radius:3px;padding:1px 4px;">${m.className}</span>` : ''}
          ${(idx === 0 && m.freeStatPoints > 0) ? `<span class="hud-free-points-pill" title="尚有 ${m.freeStatPoints} 點未分配自由點數！點擊開啟配點" onclick="event.stopPropagation(); if(window.openPartyModal) window.openPartyModal(0);">+${m.freeStatPoints}點</span>` : ''}
        </div>
        ${m.roleTitle ? `<div class="member-title" title="${m.roleTitle}">${m.roleTitle}</div>` : ''}
        ${m.formationSlotName ? `
          <div class="member-formation-slot" style="font-size:11px; margin:2px 0; display:inline-flex; align-items:center; gap:4px; padding:2px 6px; border-radius:4px; ${m.formationSlotActive ? 'background:rgba(16,185,129,0.15); border:1px solid #10b981; color:#6ee7b7;' : 'background:rgba(239,68,68,0.15); border:1px solid #ef4444; color:#fca5a5;'}"
               title="陣法孔位：${m.formationSlotName} (要求: ${m.formationSlotRequiredRow === 'FRONT' ? '前衛' : (m.formationSlotRequiredRow === 'BACK' ? '後衛' : '任意')}) - ${m.formationSlotBonus || ''} ${m.formationSlotActive ? '【加成生效中】' : '【站位不符，無法獲得加成】'}">
            <span>💠 ${m.formationSlotName}</span>
            <span style="font-size:10px; opacity:0.9;">${m.formationSlotBonus ? m.formationSlotBonus : ''}</span>
            ${!m.formationSlotActive ? '<span style="font-weight:bold; color:#ef4444;">⚠️需' + (m.formationSlotRequiredRow === 'FRONT' ? '前衛' : '後衛') + '</span>' : ''}
          </div>` : ''}
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
        ${m.isCasting ? `
        <div class="mini-bar-wrap casting-bar-wrap" style="border-color:#38bdf8; background:rgba(14,165,233,0.15);" title="正在施法: ${m.castingSkillName || '凝氣運轉'} (剩餘 ${(m.castingRemainingMs / 1000).toFixed(1)} 秒)">
          <div class="mini-bar casting-fill" style="width: ${Math.min(100, Math.max(0, (1 - (m.castingRemainingMs / Math.max(1, m.castingDurationMs))) * 100))}%; background: linear-gradient(90deg, #38bdf8, #818cf8);"></div>
          <span class="mini-val" style="color:#e0f2fe; text-shadow:0 0 3px #0284c7; font-weight:bold;">🌀 吟唱: ${m.castingSkillName || '施法'} ${(m.castingRemainingMs / 1000).toFixed(1)}s</span>
        </div>` : ''}
        ${m.isOnGcd && !m.isCasting ? `
        <div style="font-size:11px; color:#94a3b8; text-align:right; padding-right:2px;" title="全域招式調息中 (GCD)">
          ⏳ 調息 ${(m.remainingGcdMs / 1000).toFixed(1)}s
        </div>` : ''}
        ${m.activeBuffs && Array.isArray(m.activeBuffs) && m.activeBuffs.length > 0 ? `
          <div class="buff-badges-row" style="margin-top:2px;">
            ${m.activeBuffs.map(b => {
              const cat = (b.category || 'SHIELD').toLowerCase();
              let valText = '';
              if (cat === 'shield' && b.value > 0) valText = ` ${b.value}`;
              else if (cat === 'hot' && b.value > 0) valText = ` +${b.value}`;
              else if (cat === 'dot' && b.value > 0) valText = ` -${b.value}`;
              else if (b.stacks > 1) valText = ` x${b.stacks}`;
              return `<span class="buff-badge buff-${cat}" title="${b.name} (${b.category}): ${valText} 剩餘 ${b.remainingSeconds || 0}s">${b.icon || '✨'}${valText} (${b.remainingSeconds || 0}s)</span>`;
            }).join('')}
          </div>
        ` : ''}
        ${madnessExtraHtml}
      </div>
    `;
    partyContainer.appendChild(card);
  });

  // 若技能抽屜已開啟，發送事件更新抽屜
  if (store.get('isSkillDrawerOpen')) {
    eventBus.emit('ui:refreshSkillDrawer');
  }
}

/**
 * 點選成員卡片觸發技能盤展開/切換
 * @param {number} idx 成員索引 (0~5)
 */
export function selectPartyMember(idx) {
  const lastBattle = store.get('lastBattle');
  if (lastBattle && lastBattle.inBattle) {
    if (typeof window.selectCombatMember === 'function') {
      window.selectCombatMember(idx);
    }
    return;
  }

  const currentIdx = store.get('selectedMemberIdx');
  const isDrawerOpen = store.get('isSkillDrawerOpen');
  if (currentIdx === idx && isDrawerOpen) {
    // 再次點擊同一成員可切換收合
    eventBus.emit('ui:closeSkillDrawer');
    return;
  }
  store.setState({ selectedMemberIdx: idx, isSkillDrawerOpen: true });
  const lastParty = store.get('lastParty');
  if (lastParty) {
    renderPartyHud(lastParty);
  }
  eventBus.emit('ui:openSkillDrawer', idx);
}

// 相容掛載至 window
if (typeof window !== 'undefined') {
  window.renderPartyHud = renderPartyHud;
  window.selectPartyMember = selectPartyMember;
}
