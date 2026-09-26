import { sendCmd } from '../core/cmd-dispatcher.js';

/**
 * 城鎮導航與房間互動面板 (Town Navigation & Room Interaction Panel)
 * 負責渲染城鎮場景描述、四向羅盤出口、身旁生靈 (NPC/怪物) 及地面物品清單
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
 * 渲染城鎮導航與能力標籤面板
 * @param {Object} town 城鎮狀態資料
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

  // 1. 羅盤出口渲染 (正交 4 向)
  const exits = town.exits || [];
  const exitMap = {};
  const specialExits = [];

  exits.forEach(ex => {
    const d = (ex.direction || '').toLowerCase();
    if (['north', 'south', 'west', 'east'].includes(d)) {
      exitMap[d] = ex;
    } else {
      specialExits.push(ex);
    }
  });

  const dirs = [
    { key: 'north', btnId: 'exit-btn-north', nameId: 'exit-name-north', label: '▲ 北' },
    { key: 'south', btnId: 'exit-btn-south', nameId: 'exit-name-south', label: '▼ 南' },
    { key: 'west',  btnId: 'exit-btn-west',  nameId: 'exit-name-west',  label: '◀ 西' },
    { key: 'east',  btnId: 'exit-btn-east',  nameId: 'exit-name-east',  label: '東 ▶' }
  ];

  dirs.forEach(d => {
    const btn = document.getElementById(d.btnId);
    const nameEl = document.getElementById(d.nameId);
    const ex = exitMap[d.key];
    if (btn && nameEl) {
      if (ex) {
        btn.classList.remove('disabled');
        btn.disabled = false;
        btn.title = `前往：${ex.targetRoomName || ex.targetRoomId} (${d.key})`;
        nameEl.innerText = ex.targetRoomName || ex.targetRoomId;
      } else {
        btn.classList.add('disabled');
        btn.disabled = true;
        btn.title = '無出路';
        nameEl.innerText = '無出路';
      }
    }
  });

  // 特殊出口渲染 (up, down, enter 等)
  const specialContainer = document.getElementById('town-special-exits');
  if (specialContainer) {
    specialContainer.innerHTML = '';
    specialExits.forEach(ex => {
      const btn = document.createElement('button');
      btn.className = 'special-exit-btn';
      let icon = '🚪';
      if (ex.direction === 'up') icon = '🪜 向上';
      else if (ex.direction === 'down') icon = '🪜 向下';
      else if (ex.direction === 'enter') icon = '⛩️ 踏入';

      btn.innerHTML = `${icon} ${ex.targetRoomName || ex.targetRoomId}`;
      btn.onclick = () => {
        if (ex.actionCommand) {
          sendCmd(ex.actionCommand);
        } else {
          sendCmd(ex.direction);
        }
      };
      specialContainer.appendChild(btn);
    });
  }

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
          : `<span style="font-size:var(--font-xs); color:#94a3b8;">${npc.title || ''}</span>`;
        const header = document.createElement('div');
        header.className = 'npc-header';
        header.innerHTML = `<span class="npc-name">${npc.name}</span>${rankBadge}${roleHtml}`;
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
          chip.innerHTML = `<span>${norm.icon}</span><span>${norm.label}</span>`;
          chip.onclick = () => {
            if (norm.command) {
              sendCmd(norm.command);
            }
          };
          chipsWrap.appendChild(chip);
        });

        card.appendChild(chipsWrap);
        npcsContainer.appendChild(card);
      });
    }
  }

  // 3. 地面物品渲染
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
        const name = it.name || '物品';
        const isChest = name.includes('寶箱') || name.includes('棺槨') || name.includes('秘寶');
        const isPouch = name.includes('儲物袋') || name.includes('包裹') || (it.type === 'CONTAINER');

        if (isChest) {
          chip.classList.add('is-chest');
          chip.innerHTML = `<span>📦</span> 開啟 ${name}`;
        } else if (isPouch) {
          chip.classList.add('is-loot-pouch');
          chip.innerHTML = `<span>👝</span> 搜刮 ${name}`;
        } else {
          const count = it.count || it.amount || 1;
          const countStr = count > 1 ? ` x${count}` : '';
          chip.innerHTML = `<span>📦</span> 拾取 ${name}${countStr}`;
        }

        chip.onclick = () => {
          if (it.actionCommand) sendCmd(it.actionCommand);
          else sendCmd('get ' + it.id);
        };
        itemsContainer.appendChild(chip);
      });
    }
  }
}

// 相容掛載至 window
if (typeof window !== 'undefined') {
  window.renderTownNav = renderTownNav;
  window.normalizeTownCapability = normalizeTownCapability;
}
