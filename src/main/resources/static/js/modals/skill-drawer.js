import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';
import { eventBus } from '../core/event-bus.js';
import { renderBattleArena } from '../panels/battle-panel.js';

/**
 * 隊員技能抽屜組件 (Skill Drawer Component)
 * 負責渲染隊員專屬技能樹、實時冷卻倒數 (In-place DOM 更新防閃爍) 與出招釋放
 */

/**
 * 渲染所選隊員的技能抽屜
 */
export function renderSkillDrawer() {
  const drawer = document.getElementById('skill-drawer');
  const lastBattle = store.get('lastBattle');
  if (lastBattle && lastBattle.inBattle) {
    if (drawer) drawer.classList.add('hidden');
    return;
  }
  const lastParty = store.get('lastParty');
  if (!drawer || !lastParty || !lastParty.members) return;

  const selectedMemberIdx = store.get('selectedMemberIdx');
  const m = lastParty.members[selectedMemberIdx];
  if (!m) return;

  drawer.classList.remove('hidden');

  const resType = m.resourceType || 'MP';
  const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
  const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;

  let badgeCls = 'res-badge-mp';
  let badgeIcon = '🔮 真元';
  if (resType === 'SP' || resType === 'RAGE' || resType === 'COMBO' || resType === 'STAMINA' || resType === 'FORCE' || resType === 'ENERGY') {
    badgeCls = 'res-badge-sp';
    badgeIcon = '⚡ 戰氣';
  }

  const skills = m.skills || [];
  const currentMemberIdxStr = String(selectedMemberIdx);
  const container = drawer.querySelector('.drawer-skills-container');

  // 若尚未初始化骨架，或切換了隊員，或技能數量改變，則重新建立骨架
  const needFullRebuild = !container ||
      drawer.dataset.memberIdx !== currentMemberIdxStr ||
      drawer.dataset.skillCount !== String(skills.length);

  const activeTab = store.get('skillDrawerTab') || 'ALL';

  if (needFullRebuild) {
    drawer.dataset.memberIdx = currentMemberIdxStr;
    drawer.dataset.skillCount = String(skills.length);

    let skillsHtml = '';
    if (skills.length === 0) {
      skillsHtml = '<span style="color:#9ca3af;font-size:12px;">該成員專注於自動平砍，暫無主動奧義。</span>';
    } else {
      skillsHtml = skills.map((s, sIdx) => {
        const cat = s.category || 'CLASS';
        return `
          <button class="skill-btn" data-skill-idx="${sIdx}" data-skill-id="${s.id}" data-category="${cat}">
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

    const tabsHtml = `
      <div class="drawer-category-tabs">
        <button class="drawer-tab ${activeTab === 'ALL' ? 'active' : ''}" onclick="window.switchSkillDrawerTab('ALL')">全部</button>
        <button class="drawer-tab ${activeTab === 'WEAPON' ? 'active' : ''}" onclick="window.switchSkillDrawerTab('WEAPON')">⚔️ 武學</button>
        <button class="drawer-tab ${activeTab === 'CLASS' ? 'active' : ''}" onclick="window.switchSkillDrawerTab('CLASS')">🛡️ 職業</button>
        <button class="drawer-tab ${activeTab === 'SPELL' ? 'active' : ''}" onclick="window.switchSkillDrawerTab('SPELL')">🔮 法術</button>
        <button class="drawer-tab ${activeTab === 'COMBO' ? 'active' : ''}" onclick="window.switchSkillDrawerTab('COMBO')">🌟 合擊</button>
      </div>
    `;

    drawer.innerHTML = `
      <div class="drawer-member-info">
        <div>
          <span class="drawer-member-name">#${selectedMemberIdx + 1} ${m.name}</span>
          <span class="drawer-member-role">${m.roleTitle}</span>
        </div>
        <span class="drawer-resource-badge ${badgeCls}">${badgeIcon} ${curRes}/${maxRes}</span>
      </div>
      ${tabsHtml}
      <div class="drawer-skills-container">
        ${skillsHtml}
      </div>
      <button class="drawer-close-btn" onclick="window.closeSkillDrawer ? window.closeSkillDrawer() : null" title="關閉技能盤 (Esc)">✕</button>
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

      const cat = (s.category || 'CLASS').toUpperCase();
      const matchTab = (activeTab === 'ALL') ||
          (activeTab === 'WEAPON' && cat === 'WEAPON') ||
          (activeTab === 'CLASS' && cat === 'CLASS') ||
          (activeTab === 'SPELL' && cat === 'SPELL') ||
          (activeTab === 'COMBO' && (cat === 'COMBO' || s.synergy));

      btn.style.display = matchTab ? 'flex' : 'none';

      const isAlive = (m.alive !== undefined) ? m.alive : (m.hp > 0);
      const onCd = s.remainingCooldownMs > 0;
      let resOk = s.available;
      if (resOk === undefined) {
        resOk = (curRes >= (s.costValue || 0));
      }
      const canCast = Boolean(resOk && !onCd && isAlive);
      const cdSec = onCd ? (s.remainingCooldownMs / 1000).toFixed(1) : 0;

      let costTagClass = 'res-badge-mp';
      if (s.costType === 'SP') costTagClass = 'res-badge-sp';
      else if (s.costType === 'RAGE') costTagClass = 'res-badge-rage';
      else if (s.costType === 'COMBO') costTagClass = 'res-badge-combo';

      let costLabel = s.costDescription;
      if (!costLabel || costLabel === 'undefined') {
        if (!s.costValue || s.costValue <= 0) {
          costLabel = '無消耗';
        } else if (s.costType === 'SP') {
          costLabel = `${s.costValue} 戰氣`;
        } else if (s.costType === 'RAGE') {
          costLabel = `${s.costValue} 怒氣`;
        } else if (s.costType === 'COMBO') {
          costLabel = `${s.costValue} 連擊`;
        } else {
          costLabel = `${s.costValue} 真元`;
        }
      }

      // 更新按鈕 class (絕不替換元素節點)
      btn.className = `skill-btn ${canCast ? '' : 'cant-cast'}${s.synergy && canCast ? ' synergy-glow' : ''}`;
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
        onSkillBtnClick(selectedMemberIdx, s.id, canCast, s.name, costLabel, onCd, cdSec, s.costDescription || '', Boolean(s.synergy));
      };
    });
  }
}

/**
 * 切換技能抽屜分頁 (Tabs)
 */
export function switchSkillDrawerTab(tab) {
  store.setState({ skillDrawerTab: tab });
  const drawer = document.getElementById('skill-drawer');
  if (drawer) {
    const tabs = drawer.querySelectorAll('.drawer-category-tabs .drawer-tab');
    tabs.forEach(t => {
      const isTarget = t.getAttribute('onclick')?.includes(`'${tab}'`);
      t.classList.toggle('active', Boolean(isTarget));
    });
  }
  renderSkillDrawer();
}

/**
 * 點擊隊員技能按鈕 (若不可用則給予戰術原因提示)
 */
export function onSkillBtnClick(memberIdx, skillId, canCast, skillName, costLabel, onCd, cdSec, costDesc, isSynergy) {
  if (!canCast) {
    let warnMsg = '';
    if (onCd) {
      warnMsg = `⏳【調息中】「${skillName}」正在調息冷卻中，尚需 ${cdSec} 秒！`;
    } else if (costDesc && costDesc.includes('需')) {
      warnMsg = `⚠️【兵刃未備】「${skillName}」${costDesc}！當前未佩戴合適兵刃（或處於赤手狀態）。請在小隊面板 (P) 佩戴對應兵刃。`;
    } else {
      warnMsg = `⚠️【元氣未備】「${skillName}」釋放條件不足（需 ${costLabel}）！請在戰鬥中累積足夠點數後再施展。`;
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
  castPartySkill(memberIdx, skillId, isSynergy);
}

/**
 * 關閉技能盤
 */
export function closeSkillDrawer() {
  store.setState({ isSkillDrawerOpen: false });
  const drawer = document.getElementById('skill-drawer');
  if (drawer) {
    drawer.classList.add('hidden');
  }
  const lastParty = store.get('lastParty');
  if (lastParty && typeof window.renderPartyHud === 'function') {
    window.renderPartyHud(lastParty);
  }
}

/**
 * 釋放隊員專屬技能
 */
export function castPartySkill(memberIdx, skillId, isSynergy) {
  if (isSynergy) {
    sendCmd(`battle combo ${skillId}`);
  } else {
    sendCmd(`skill cast ${memberIdx} ${skillId}`);
  }
}

// 註冊 EventBus 事件監聽
eventBus.on('ui:openSkillDrawer', (idx) => {
  const lastBattle = store.get('lastBattle');
  if (lastBattle && lastBattle.inBattle) return;
  store.setState({ selectedMemberIdx: idx, isSkillDrawerOpen: true });
  renderSkillDrawer();
});

eventBus.on('ui:closeSkillDrawer', () => {
  closeSkillDrawer();
});

eventBus.on('ui:refreshSkillDrawer', () => {
  const lastBattle = store.get('lastBattle');
  if (lastBattle && lastBattle.inBattle) return;
  renderSkillDrawer();
});

// 相容掛載至 window
if (typeof window !== 'undefined') {
  window.renderSkillDrawer = renderSkillDrawer;
  window.closeSkillDrawer = closeSkillDrawer;
  window.onSkillBtnClick = onSkillBtnClick;
  window.castPartySkill = castPartySkill;
  window.switchSkillDrawerTab = switchSkillDrawerTab;
}
