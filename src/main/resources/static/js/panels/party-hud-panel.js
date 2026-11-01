import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';
import { eventBus } from '../core/event-bus.js';
import { escapeHtml } from '../core/ui-utils.js';
import { renderAggregatedBuffBadges, renderShieldOverlayHtml, renderPoiseBarHtml } from '../core/buff-hud-helper.js';

/**
 * 6 人小隊狀態列面板 (Party HUD Panel)
 * 負責渲染成員卡片、前後衛標籤、HP/MP/SAN 狀態條、破防/重傷/精神萎靡狀態與裝備槽位
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

    // Option A 精神防護規範：氣血耗盡重傷 (FAINTED) 與 精神萎靡 (WEAKENED / SAN <= 0)
    const isFainted = m.hp <= 0 || m.isFainted;
    const isWeakened = m.isWeakened || m.madnessState === 'CHAOS' || (m.san !== undefined && m.san <= 0);

    let statusCardClass = '';
    let statusExtraHtml = '';

    if (isFainted) {
      statusCardClass = ' card-fainted';
      statusExtraHtml = `
        <div class="fainted-status-badge" title="氣血耗盡陷入昏厥重傷，無法行動，需調息或丹藥救治">
          💀 氣血耗盡・重傷
        </div>
      `;
    } else if (isWeakened) {
      statusCardClass = ' card-weakened';
      statusExtraHtml = `
        <div class="weakened-status-badge" title="精神防護耗盡陷入萎靡，輸出降低 15% / 全屬性降低 10%">
          🍂 精神萎靡 (-15%)
        </div>
      `;
    }

    card.className = `party-card${isSelected ? ' selected-card' : ''}${statusCardClass}`;
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

    // 聚合 Buff 圖示
    const buffsHtml = renderAggregatedBuffBadges(m.activeBuffs);
    const shieldOverlayHtml = renderShieldOverlayHtml(m.hp, m.maxHp, m.shield);
    const poiseBarHtml = renderPoiseBarHtml(m.poise, m.maxPoise, m.isPoiseBroken);

    const safeMemberName = escapeHtml(m.name || '無名');
    const safeSanState = escapeHtml(m.sanState || '');
    const safeCastingName = escapeHtml(m.castingSkillName || '施法');

    card.innerHTML = `
      <div class="card-info-side">
        <div class="card-name-row">
          <span class="member-name" title="${safeMemberName}">${safeMemberName}</span>
          ${m.isPoiseBroken ? '<span class="poise-broken-label" title="【力竭破防】架勢崩解！受傷 +35%，施法打斷！">破防</span>' : ''}
          <span class="member-level-num" title="修為境界：第 ${m.level || 1} 重">${m.level || 1}</span>
        </div>
        ${buffsHtml ? `<div class="card-buffs-wrap" style="margin-top:3px;">${buffsHtml}</div>` : ''}
      </div>
      <div class="card-bars-side">
        <div class="mini-bar-wrap" style="position:relative;" title="氣血 HP: ${m.hp}/${m.maxHp}${m.shield > 0 ? ' (護盾: ' + m.shield + ')' : ''}">
          <div class="mini-bar hp-fill" style="width: ${hpPct}%"></div>
          ${shieldOverlayHtml}
          <span class="mini-val" style="z-index:3;">HP ${m.hp}/${m.maxHp}</span>
        </div>
        ${poiseBarHtml}
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
        ${statusExtraHtml}
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
