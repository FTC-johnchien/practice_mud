import { store } from '../core/state-store.js';
import { escapeHtml } from '../core/ui-utils.js';

/**
 * 戰鬥大捷結算視窗組件 (Battle Victory Modal Component - BATTLE-RESULT-01)
 * 負責渲染戰後結算翻牌、修為長條充能、境界突破光環與戰利品品質展現
 */

let isModalOpen = false;
let currentVictoryData = null;
let cardFlipTimers = [];

/**
 * 查詢結算視窗是否處於開啟狀態
 */
export function isVictoryModalOpen() {
  return isModalOpen;
}

/**
 * 開啟戰鬥大捷結算視窗
 * @param {Object} data 伺服器發送的 BattleVictoryDto 載荷
 */
export function showVictoryModal(data) {
  if (!data) return;
  currentVictoryData = data;
  isModalOpen = true;

  const modal = document.getElementById('battle-victory-modal');
  if (!modal) return;

  // 1. 修為與靈石統計數值
  const xpValEl = document.getElementById('victory-xp-val');
  if (xpValEl) {
    xpValEl.innerText = `+${data.totalXp || 0}`;
  }

  const coinsPill = document.getElementById('victory-coins-pill');
  const coinsValEl = document.getElementById('victory-coins-val');
  if (coinsPill && coinsValEl) {
    if (data.coinsGained && data.coinsGained > 0) {
      coinsValEl.innerText = `+${data.coinsGained}`;
      coinsPill.classList.remove('hidden');
    } else {
      coinsPill.classList.add('hidden');
    }
  }

  // 2. 渲染全員修為增長與境界突破
  renderVictoryMembers(data);

  // 3. 渲染戰利品翻牌網格
  renderVictoryLoots(data);

  // 4. 展現模態視窗並聚焦確認按鈕
  modal.classList.remove('hidden');
  const confirmBtn = document.getElementById('btn-confirm-victory');
  if (confirmBtn) {
    setTimeout(() => confirmBtn.focus(), 80);
  }
}

/**
 * 關閉結算視窗
 */
export function closeVictoryModal() {
  const modal = document.getElementById('battle-victory-modal');
  if (modal) {
    modal.classList.add('hidden');
  }
  isModalOpen = false;
  currentVictoryData = null;

  // 清除未完成的翻牌定時器
  cardFlipTimers.forEach(t => clearTimeout(t));
  cardFlipTimers = [];
}

/**
 * 一鍵翻開所有尚未翻開的戰利品卡片
 */
export function flipAllLoots() {
  const cards = document.querySelectorAll('#victory-loots-grid .loot-card-flipper');
  cards.forEach(card => {
    card.classList.add('is-flipped');
  });
}

/**
 * 翻開單張卡片
 */
export function flipLootCard(cardEl) {
  if (cardEl && !cardEl.classList.contains('is-flipped')) {
    cardEl.classList.add('is-flipped');
  }
}

/**
 * 渲染小隊成員修為增長列與境界突破光環
 */
function renderVictoryMembers(data) {
  const container = document.getElementById('victory-members-list');
  if (!container) return;
  container.innerHTML = '';

  const party = store.get('lastParty');
  const members = (party && party.members && party.members.length > 0) ? party.members : [];
  const levelUps = Array.isArray(data.levelUps) ? data.levelUps : [];

  // 若目前無快照隊員，降級以 levelUps 渲染
  if (members.length === 0 && levelUps.length > 0) {
    levelUps.forEach(up => {
      const row = document.createElement('div');
      row.className = 'victory-member-row is-breakthrough';
      row.innerHTML = `
        <div class="vm-info">
          <span class="vm-name">${escapeHtml(up.memberName || '同門')}</span>
          <span class="vm-breakthrough-tag">✨ 境界突破！ Lv.${up.oldLevel} ➔ Lv.${up.newLevel}</span>
        </div>
        <div class="vm-growth-desc">${escapeHtml(up.statGrowthDesc || '道法大進，元神凝練！')}</div>
      `;
      container.appendChild(row);
    });
    return;
  }

  members.forEach((m, idx) => {
    const levelUpInfo = levelUps.find(u =>
      (u.memberId && (u.memberId === m.id || u.memberId === m.name)) ||
      (u.memberName && u.memberName === m.name)
    );
    const isLeveledUp = !!levelUpInfo;

    const row = document.createElement('div');
    row.className = `victory-member-row${isLeveledUp ? ' is-breakthrough' : ''}`;

    const safeName = escapeHtml(m.name || `隊員 #${idx + 1}`);
    const safeRole = escapeHtml(m.roleTitle || m.className || '修士');
    const curLevel = isLeveledUp ? levelUpInfo.newLevel : (m.level || 1);

    // 經驗條百分比計算
    const expCur = m.exp || 0;
    const expNext = m.nextLevelExp || Math.max(100, (curLevel + 1) * 150);
    const pct = isLeveledUp ? 100 : Math.min(100, Math.max(10, Math.round((expCur / expNext) * 100)));

    let statusHtml = '';
    if (isLeveledUp) {
      statusHtml = `
        <div class="vm-breakthrough-badge">
          <span class="badge-sparkle">✨</span>
          <strong class="badge-text">境界突破！</strong>
          <span class="badge-levels">Lv.${levelUpInfo.oldLevel} ➔ Lv.${levelUpInfo.newLevel}</span>
        </div>
        ${levelUpInfo.statGrowthDesc ? `<div class="vm-growth-desc">${escapeHtml(levelUpInfo.statGrowthDesc)}</div>` : ''}
      `;
    } else {
      statusHtml = `
        <div class="vm-level-badge">修為 第 ${curLevel} 重</div>
      `;
    }

    row.innerHTML = `
      <div class="vm-header">
        <div class="vm-identity">
          <span class="vm-role-icon">👤</span>
          <span class="vm-name">${safeName}</span>
          <span class="vm-role-tag">${safeRole}</span>
        </div>
        <div class="vm-status-side">
          ${statusHtml}
        </div>
      </div>
      <div class="vm-bar-track" title="當前修為進度: ${pct}%">
        <div class="vm-bar-fill${isLeveledUp ? ' fill-breakthrough' : ''}" style="width: 0%;"></div>
      </div>
    `;

    container.appendChild(row);

    // 延遲帶動經驗條長條平滑充能
    setTimeout(() => {
      const fillEl = row.querySelector('.vm-bar-fill');
      if (fillEl) {
        fillEl.style.width = `${pct}%`;
      }
    }, 120 + idx * 70);
  });
}

/**
 * 渲染戰利品 3D 翻牌卡片清單
 */
function renderVictoryLoots(data) {
  const grid = document.getElementById('victory-loots-grid');
  if (!grid) return;
  grid.innerHTML = '';

  cardFlipTimers.forEach(t => clearTimeout(t));
  cardFlipTimers = [];

  const loots = Array.isArray(data.loots) ? data.loots : [];

  if (loots.length === 0) {
    grid.innerHTML = `
      <div class="no-loots-placeholder">
        <span class="placeholder-icon">🍃</span>
        <span class="placeholder-text">此戰未剖得實體靈物，妖邪煞氣已化作滿天修為道韻消散</span>
      </div>
    `;
    return;
  }

  loots.forEach((loot, idx) => {
    const quality = (loot.quality || 'COMMON').toUpperCase();
    const qualityLabel = formatQualityName(quality);

    const flipper = document.createElement('div');
    flipper.className = `loot-card-flipper quality-${quality.toLowerCase()}`;
    flipper.setAttribute('data-action', 'flip-single-loot');
    flipper.setAttribute('tabindex', '0');
    flipper.setAttribute('role', 'button');
    flipper.setAttribute('aria-label', `戰利品卡片：${loot.name || '靈物'}`);

    const safeName = escapeHtml(loot.name || '戰利品');
    const safeIcon = escapeHtml(loot.icon || '📦');
    const safeDesc = escapeHtml(loot.description || '戰後繳獲之靈物');
    const countText = (loot.count && loot.count > 1) ? `×${loot.count}` : '';

    flipper.innerHTML = `
      <div class="loot-card-inner">
        <!-- 卡片背面 (封印面) -->
        <div class="loot-card-face loot-card-back">
          <div class="card-seal-icon">☯️</div>
          <div class="card-seal-text">秘寶封印</div>
          <div class="card-seal-sub">點擊翻開</div>
        </div>

        <!-- 卡片正面 (展示面) -->
        <div class="loot-card-face loot-card-front">
          <div class="loot-quality-ribbon">${qualityLabel}</div>
          <div class="loot-card-icon">${safeIcon}</div>
          <div class="loot-card-name" title="${safeName}">${safeName}</div>
          ${countText ? `<div class="loot-card-count">${countText}</div>` : ''}
          <div class="loot-card-desc" title="${safeDesc}">${safeDesc}</div>
        </div>
      </div>
    `;

    flipper.onclick = () => flipLootCard(flipper);
    flipper.onkeydown = (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        flipLootCard(flipper);
      }
    };

    grid.appendChild(flipper);

    // 依序階梯式自動翻牌 (間隔 200ms)
    const timer = setTimeout(() => {
      flipLootCard(flipper);
    }, 450 + idx * 200);
    cardFlipTimers.push(timer);
  });
}

/**
 * 品質枚舉轉換為典雅修仙中文標籤
 */
function formatQualityName(quality) {
  switch (quality) {
    case 'LEGENDARY': return '極品仙品';
    case 'EPIC': return '上品天罡';
    case 'RARE': return '中品地煞';
    case 'UNCOMMON': return '下品靈器';
    case 'COMMON':
    default:
      return '凡品雜物';
  }
}

// 掛載至 window 供相容呼叫
if (typeof window !== 'undefined') {
  window.showVictoryModal = showVictoryModal;
  window.closeVictoryModal = closeVictoryModal;
  window.flipAllLoots = flipAllLoots;
  window.isVictoryModalOpen = isVictoryModalOpen;
}
