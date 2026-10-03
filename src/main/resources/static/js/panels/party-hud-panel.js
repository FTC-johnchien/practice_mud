import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';
import { eventBus } from '../core/event-bus.js';
import { escapeHtml } from '../core/ui-utils.js';

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
  const formNameEl = document.getElementById('formation-name') || document.getElementById('formation-name-badge') || document.querySelector('.formation-name');
  const energyFillEl = document.getElementById('formation-bar') || document.getElementById('formation-energy-fill');
  const energyValEl = document.getElementById('formation-energy-val') || document.querySelector('.formation-energy-val');
  const ultBtn = document.getElementById('dock-ult-btn') || document.getElementById('formation-ult-btn') || document.getElementById('ult-btn');

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

    card.className = `party-card${isSelected ? ' selected-card' : ''}${madnessCardClass}`;
    card.onclick = () => selectPartyMember(idx);
    card.title = `點選 #${idx + 1} ${m.name} 展開技能盤 (按 C 開啟選單查看完整道基/裝備)`;

    // 1. HP 氣血
    const hpPct = Math.min(100, Math.max(0, (m.hp / m.maxHp) * 100));

    // 2. MP 真元 (法術/神通資源)
    const curMp = m.mp !== undefined ? m.mp : 0;
    const maxMp = m.maxMp !== undefined ? m.maxMp : 0;
    const mpPct = Math.min(100, Math.max(0, maxMp > 0 ? (curMp / maxMp) * 100 : 0));
    const mpText = maxMp > 0 ? `${curMp}/${maxMp}` : '—';
    const mpFillClass = maxMp > 0 ? 'mp-fill' : 'empty-res-fill';

    // 3. SP 戰氣 (武學/套路/身法資源，支援 RAGE 與 COMBO 雙軌提示)
    const curSp = m.sp !== undefined ? m.sp : (m.currentSp !== undefined ? m.currentSp : (m.resourceType === 'SP' ? m.currentResource : 0));
    const maxSp = m.maxSp !== undefined ? m.maxSp : 100;
    const spPct = Math.min(100, Math.max(0, maxSp > 0 ? (curSp / maxSp) * 100 : 0));
    const spText = maxSp > 0 ? `${curSp}/${maxSp}` : '—';
    const spFillClass = maxSp > 0 ? 'sp-fill' : 'empty-res-fill';
    let spLabel = 'SP';
    let spTitle = `戰氣 SP: ${spText}`;
    if (m.resourceType === 'RAGE') {
      spLabel = '怒氣';
      const curRage = m.rage !== undefined ? m.rage : curSp;
      spTitle = `怒氣 (Rage): ${curRage}/${maxSp}`;
    } else if (m.resourceType === 'COMBO') {
      spLabel = '連擊';
      const curCombo = m.combo !== undefined ? m.combo : Math.min(5, Math.floor(curSp / 20));
      spTitle = `連擊 (Combo): ${curCombo}/5 點 (戰氣 ${curSp}/${maxSp})`;
    }

    // 4. SAN 道心
    const sanPct = Math.min(100, Math.max(0, (m.san / m.maxSan) * 100));
    const sanClass = `san-${(m.sanLevel || 'NORMAL').toLowerCase()}`;

    // 精簡 Buff 圖示 (9~10 個小圖示方塊)
    let buffsHtml = '';
    const buffs = (m.activeBuffs && Array.isArray(m.activeBuffs)) ? m.activeBuffs : [];
    buffsHtml = `
      <div class="card-buffs-grid">
        ${buffs.slice(0, 10).map(b => {
          const cat = escapeHtml((b.category || 'SHIELD').toLowerCase());
          const safeName = escapeHtml(b.name || '');
          const safeCat = escapeHtml(b.category || '');
          const safeIcon = escapeHtml(b.icon || '✨');
          return `<span class="party-buff-chip buff-${cat}" title="${safeName} (${safeCat}): 剩餘 ${b.remainingSeconds || 0}秒">${safeIcon}</span>`;
        }).join('')}
      </div>
    `;

    const safeMemberName = escapeHtml(m.name || '無名');
    const safeSanState = escapeHtml(m.sanState || '');
    const safeCastingName = escapeHtml(m.castingSkillName || '施法');

    card.innerHTML = `
      <div class="card-info-side">
        <div class="card-name-row">
          <span class="member-name" title="${safeMemberName}">${safeMemberName}</span>
          <span class="member-level-num" title="修為境界：第 ${m.level || 1} 重">${m.level || 1}</span>
        </div>
        ${buffsHtml}
      </div>
      <div class="card-bars-side">
        <div class="mini-bar-wrap" title="氣血 HP: ${m.hp}/${m.maxHp}">
          <div class="mini-bar hp-fill" style="width: ${hpPct}%"></div>
          <span class="mini-val">HP ${m.hp}/${m.maxHp}</span>
        </div>
        <div class="mini-bar-wrap" title="真元 MP: ${mpText}">
          <div class="mini-bar ${mpFillClass}" style="width: ${mpPct}%"></div>
          <span class="mini-val">MP ${mpText}</span>
        </div>
        <div class="mini-bar-wrap" title="${spTitle}">
          <div class="mini-bar ${spFillClass}" style="width: ${spPct}%"></div>
          <span class="mini-val">${spLabel} ${spText}</span>
        </div>
        <div class="mini-bar-wrap san-bar-wrap" title="道心 SAN: ${m.san}/${m.maxSan} ${safeSanState}">
          <div class="mini-bar san-fill ${sanClass}" style="width: ${sanPct}%"></div>
          <span class="mini-val ${sanClass}">SAN ${m.san}/${m.maxSan}</span>
        </div>
        ${m.isCasting ? `
        <div class="mini-bar-wrap casting-bar-wrap" style="border-color:#38bdf8; background:rgba(14,165,233,0.15);" title="正在施法: ${safeCastingName} (剩餘 ${(m.castingRemainingMs / 1000).toFixed(1)} 秒)">
          <div class="mini-bar casting-fill" style="width: ${Math.min(100, Math.max(0, (1 - (m.castingRemainingMs / Math.max(1, m.castingDurationMs))) * 100))}%; background: linear-gradient(90deg, #38bdf8, #818cf8);"></div>
          <span class="mini-val" style="color:#e0f2fe; text-shadow:0 0 3px #0284c7; font-weight:bold;">🌀 ${safeCastingName} ${(m.castingRemainingMs / 1000).toFixed(1)}s</span>
        </div>` : ''}
        ${m.isOnGcd && !m.isCasting ? `
        <div style="font-size:var(--font-xs); color:#94a3b8; text-align:right; padding-right:2px;" title="全域招式調息中 (GCD)">
          ⏳ 調息 ${(m.remainingGcdMs / 1000).toFixed(1)}s
        </div>` : ''}
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
