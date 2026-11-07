import { sendCmd } from '../core/cmd-dispatcher.js';
import { escapeHtml } from '../core/ui-utils.js';

/**
 * 城鎮導航與營地中樞面板 (Town Hub & Facilities Navigation Panel)
 * 核心職責：渲染營地六大機能設施卡牌（支援認知透鏡三階動態轉譯與 Glitch 特效）、
 * 身旁生靈 (NPC/同伴) 與地面散落靈物清單。全面遵循 CSP 零內聯 onclick 規範。
 */

/**
 * 標準化城鎮 NPC 能力標籤 (支援後端富物件與前端向後相容)
 * @param {Object|string} cap 能力資料
 * @param {Object} npc NPC 資訊
 */
export function normalizeTownCapability(cap, npc) {
  if (typeof cap === 'object' && cap && cap.label) {
    return {
      type: (cap.type || 'ASK').toLowerCase(),
      label: cap.label,
      icon: cap.icon || '✨',
      command: cap.command || `ask ${npc.alias || npc.id}`
    };
  }
  const key = (typeof cap === 'string' ? cap : (cap && cap.type ? cap.type : '')).toUpperCase();
  const alias = npc.alias || npc.id;
  switch (key) {
    case 'SHOP': return { type: 'shop', label: '貨棧買賣', icon: '🛒', command: 'shop' };
    case 'TALK': return { type: 'ask', label: '相談交談', icon: '💬', command: `ask ${alias}` };
    case 'REST': return { type: 'rest', label: '客棧安歇', icon: '🛏️', command: 'rest' };
    case 'RECRUIT': return { type: 'recruit', label: '招募入隊', icon: '🤝', command: `recruit ${alias}` };
    case 'DISMISS': return { type: 'dismiss', label: '請離隊友', icon: '👋', command: `dismiss ${alias}` };
    case 'QUEST': return { type: 'quest', label: '任務指引', icon: '📜', command: `ask ${alias}` };
    case 'FIGHT': return { type: 'fight', label: '拔劍迎擊', icon: '⚔️', command: `kill ${alias}` };
    default: return { type: 'ask', label: '交談', icon: '💬', command: `ask ${alias}` };
  }
}

/**
 * 渲染六大營地設施卡牌網格
 * @param {Array} facilities 設施 DTO 清單
 */
export function renderFacilities(facilities) {
  const grid = document.getElementById('town-facilities-grid');
  if (!grid) return;

  grid.innerHTML = '';
  if (!facilities || facilities.length === 0) {
    grid.innerHTML = '<div style="font-size:var(--font-xs); color:#64748b; padding:8px 4px;">(暫無可用設施)</div>';
    return;
  }

  facilities.forEach(fac => {
    const card = document.createElement('div');
    card.className = 'facility-card' + (fac.glitch ? ' facility-glitch' : '');

    const safeId = escapeHtml(fac.id || '');
    const safeName = escapeHtml(fac.name || '');
    const safeDesc = escapeHtml(fac.description || '');
    const icon = fac.icon || '🏛️';
    const glitchBadge = fac.glitch ? '<span class="facility-glitch-badge">⚠️ 靈識異象</span>' : '';

    let actionsHtml = '';
    if (fac.id === 'fac_clinic') {
      actionsHtml = `
        <div class="facility-actions">
          <button class="facility-act-btn btn-primary" data-action="camp-rest">🛌 全員調息恢復</button>
        </div>`;
    } else if (fac.id === 'fac_dungeon_portal') {
      actionsHtml = `
        <div class="facility-actions">
          <button class="facility-act-btn btn-dungeon" data-action="enter-dungeon" data-dungeon-id="mozhu_mines_b1f">⛏️ 出征：墨竹山礦脈 B1F</button>
          <button class="facility-act-btn btn-dungeon" data-action="enter-dungeon" data-dungeon-id="taiyin_tomb_b1f">⛩️ 出征：太陰古塚 B1F</button>
        </div>`;
    } else if (fac.id === 'fac_dojo') {
      actionsHtml = `
        <div class="facility-actions">
          <button class="facility-act-btn" data-action="open-facility" data-facility-id="${safeId}">🥋 進入演武道場</button>
          <div class="facility-sub-actions">
            <button class="facility-sub-btn" data-action="open-menu" data-menu="PARTY">👥 編隊</button>
            <button class="facility-sub-btn" data-action="open-menu" data-menu="FORMATION">☯️ 陣法</button>
            <button class="facility-sub-btn" data-action="open-menu" data-menu="TACTICS">🎯 戰術</button>
          </div>
        </div>`;
    } else {
      actionsHtml = `
        <div class="facility-actions">
          <button class="facility-act-btn" data-action="open-facility" data-facility-id="${safeId}">${icon} 進入${safeName}</button>
        </div>`;
    }

    card.innerHTML = `
      <div class="facility-header">
        <div class="facility-title-wrap">
          <span class="facility-icon">${icon}</span>
          <span class="facility-name">${safeName}</span>
        </div>
        ${glitchBadge}
      </div>
      <div class="facility-desc">${safeDesc}</div>
      ${actionsHtml}
    `;

    grid.appendChild(card);
  });
}

/**
 * 渲染營地中樞聚合視圖 (TownHub)
 * @param {Object} townOrHub TownViewDto 或 TownHubDto
 */
export function renderTownHub(townOrHub) {
  if (!townOrHub) return;
  const hub = townOrHub.hub || townOrHub;
  if (!hub) return;

  const perceptionBadge = document.getElementById('town-perception-badge');
  if (perceptionBadge) {
    if (typeof hub.chipRate === 'number') {
      perceptionBadge.innerText = `晶片加載 ${hub.chipRate}%`;
      const tierName = (hub.perceptionTier && hub.perceptionTier.displayName)
          ? hub.perceptionTier.displayName
          : (typeof hub.perceptionTier === 'string' ? hub.perceptionTier : '');
      perceptionBadge.title = tierName ? `靈識位階: ${tierName}` : '';
    } else {
      perceptionBadge.innerText = '';
    }
  }

  if (hub.facilities) {
    renderFacilities(hub.facilities);
  }
}

/**
 * 渲染城鎮場景與生靈清單面板
 * @param {Object} town 城鎮狀態資料 (TownViewDto)
 */
export function renderTownNav(town) {
  if (!town) return;

  const zoneBadge = document.getElementById('town-zone-badge');
  const roomTitle = document.getElementById('town-room-title');
  const roomIdTag = document.getElementById('town-room-id-tag');
  const roomDesc = document.getElementById('town-room-desc');

  if (zoneBadge) zoneBadge.innerText = town.zoneName || '新手村';
  if (roomTitle) roomTitle.innerText = town.roomName || '客棧';
  if (roomIdTag) roomIdTag.innerText = town.roomId || '';
  if (roomDesc) roomDesc.innerText = town.description || '';

  // 1. 渲染營地六大機能設施
  renderTownHub(town);

  // 2. 身旁人物與互動能力標籤渲染
  const npcsContainer = document.getElementById('town-npcs-list');
  const npcCountBadge = document.getElementById('town-npc-count');
  if (npcsContainer) {
    npcsContainer.innerHTML = '';
    const npcs = town.npcs || [];
    if (npcCountBadge) {
      npcCountBadge.innerText = npcs.length > 0 ? `(${npcs.length})` : '';
    }

    if (npcs.length === 0) {
      npcsContainer.innerHTML = '<div style="font-size:var(--font-xs); color:#64748b; padding:8px 4px;">(周圍暫無其他生靈)</div>';
    } else {
      npcs.forEach(npc => {
        const card = document.createElement('div');
        card.className = 'npc-card';

        const caps = npc.capabilities || [];
        const isHostile = caps.some(c => (typeof c === 'string' ? c === 'FIGHT' : c && c.type === 'FIGHT'));
        if (isHostile) card.classList.add('is-hostile');

        const rank = npc.rank || 'NORMAL';
        let rankBadge = '';
        if (rank === 'BOSS') {
          rankBadge = `<span style="font-size:var(--font-xs); color:#f59e0b; background:rgba(245,158,11,0.2); border:1px solid #f59e0b; padding:1px 6px; border-radius:3px; margin-left:4px;">👑 首領</span>`;
        } else if (rank === 'ELITE') {
          rankBadge = `<span style="font-size:var(--font-xs); color:#38bdf8; background:rgba(56,189,248,0.2); border:1px solid #38bdf8; padding:1px 6px; border-radius:3px; margin-left:4px;">⭐ 精英</span>`;
        }

        const roleHtml = isHostile
          ? `<span style="font-size:var(--font-xs); color:#f87171; background:rgba(239,68,68,0.2); border:1px solid #ef4444; padding:1px 6px; border-radius:3px;">敵對</span>`
          : `<span style="font-size:var(--font-xs); color:#94a3b8;">${escapeHtml(npc.title || '')}</span>`;
        const header = document.createElement('div');
        header.className = 'npc-header';
        header.innerHTML = `<span class="npc-name">${escapeHtml(npc.name || '')}</span>${rankBadge}${roleHtml}`;
        card.appendChild(header);

        if (npc.description) {
          const desc = document.createElement('div');
          desc.className = 'npc-desc';
          desc.innerText = npc.description;
          card.appendChild(desc);
        }

        const chipsWrap = document.createElement('div');
        chipsWrap.className = 'capability-chips';

        caps.forEach(cap => {
          const norm = normalizeTownCapability(cap, npc);
          const chip = document.createElement('button');
          chip.className = `cap-chip cap-${norm.type}`;
          chip.setAttribute('data-action', 'send-cmd');
          chip.setAttribute('data-cmd', norm.command || '');
          chip.innerHTML = `<span>${norm.icon}</span><span>${escapeHtml(norm.label)}</span>`;
          chipsWrap.appendChild(chip);
        });

        card.appendChild(chipsWrap);
        npcsContainer.appendChild(card);
      });
    }
  }

  // 3. 地面物品渲染 (全面改為 data-action="send-cmd")
  const itemsContainer = document.getElementById('town-items-list');
  const itemCountBadge = document.getElementById('town-item-count');
  if (itemsContainer) {
    itemsContainer.innerHTML = '';
    const items = town.items || [];
    if (itemCountBadge) {
      itemCountBadge.innerText = items.length > 0 ? `(${items.length})` : '';
    }
    if (items.length === 0) {
      itemsContainer.innerHTML = '<div style="font-size:var(--font-xs); color:#64748b; padding:4px;">(地面空無一物)</div>';
    } else {
      items.forEach(it => {
        const chip = document.createElement('button');
        chip.className = 'ground-item-chip';
        chip.setAttribute('data-action', 'send-cmd');
        chip.setAttribute('data-cmd', it.actionCommand ? it.actionCommand : ('get ' + it.id));

        const rawName = it.name || '物品';
        const safeName = escapeHtml(rawName);
        const isChest = rawName.includes('寶箱') || rawName.includes('棺槨') || rawName.includes('秘寶');
        const isPouch = rawName.includes('儲物袋') || rawName.includes('包裹') || (it.type === 'CONTAINER');

        if (isChest) {
          chip.classList.add('is-chest');
          chip.innerHTML = `<span>📦</span> 開啟 ${safeName}`;
        } else if (isPouch) {
          chip.classList.add('is-loot-pouch');
          chip.innerHTML = `<span>👝</span> 搜刮 ${safeName}`;
        } else {
          const count = it.count || it.amount || 1;
          const countStr = count > 1 ? ` x${count}` : '';
          chip.innerHTML = `<span>📦</span> 拾取 ${safeName}${countStr}`;
        }

        itemsContainer.appendChild(chip);
      });
    }
  }
}

// 相容掛載至 window
if (typeof window !== 'undefined') {
  window.renderTownNav = renderTownNav;
  window.renderTownHub = renderTownHub;
  window.renderFacilities = renderFacilities;
  window.normalizeTownCapability = normalizeTownCapability;
}
