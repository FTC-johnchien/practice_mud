import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';

/**
 * 陣法戰陣子模組 (Party Formation Tab)
 * 負責渲染浪漫沙加式 5×3 戰術站位盤與道門陣法典籍庫
 */

const drpgState = store.getState();
const send = (cmd, silent) => sendCmd(cmd, silent);

function getRenderPartyModal() {
  return window.renderPartyModal;
}

export function formatFormationClassName(c) {
  if (!c) return '';
  switch (c) {
    case 'WARRIOR': return '體修(戰)';
    case 'MAGE': return '符修(法)';
    case 'CLERIC': return '丹修(牧)';
    case 'ROGUE': return '遊俠(刺)';
    case 'SWORDSMAN': return '劍修';
    default: return c;
  }
}

export function toggleFormationViewMode(mode) {
  drpgState.formationViewMode = mode || (drpgState.formationViewMode === 'TACTICAL' ? 'LIBRARY' : 'TACTICAL');
  const render = getRenderPartyModal();
  if (render) render();
}

export function setFormationFilterSize(size) {
  drpgState.formationFilterSize = size;
  drpgState.formationPage = 0;
  const render = getRenderPartyModal();
  if (render) render();
}

export function changeFormationPage(delta) {
  drpgState.formationPage = Math.max(0, (drpgState.formationPage || 0) + delta);
  const render = getRenderPartyModal();
  if (render) render();
}

/**
 * 模式 B: 道門陣法典籍庫 (專屬切換視窗，支援 2/3/4/5人頁籤與分頁)
 */
export function renderFormationLibraryView(party) {
  const allList = party.availableFormations || [];
  const currentFilter = drpgState.formationFilterSize || 'ALL';
  const currentPage = drpgState.formationPage || 0;
  const PAGE_SIZE = 20;

  // 1. 根據人數頁籤篩選
  const filteredList = allList.filter(f => {
    if (currentFilter === 'ALL') return true;
    return f.requiredPartySize === Number(currentFilter);
  });

  const totalPages = Math.max(1, Math.ceil(filteredList.length / PAGE_SIZE));
  const safePage = Math.min(Math.max(0, currentPage), totalPages - 1);
  const pagedList = filteredList.slice(safePage * PAGE_SIZE, (safePage + 1) * PAGE_SIZE);

  // 統計各人數數量
  const countAll = allList.length;
  const count5 = allList.filter(f => f.requiredPartySize === 5).length;
  const count4 = allList.filter(f => f.requiredPartySize === 4).length;
  const count3 = allList.filter(f => f.requiredPartySize === 3).length;
  const count2 = allList.filter(f => f.requiredPartySize === 2).length;

  const isPartyBroken = Boolean(party.formationName && party.formationName.includes('已崩解'));

  // 迷你 5x3 點陣預覽圖產生器
  const renderMiniGrid = (slots) => {
    const grid = [
      [false, false, false, false, false],
      [false, false, false, false, false],
      [false, false, false, false, false]
    ];
    (slots || []).forEach(s => {
      const gx = (typeof s.gridX === 'number' && s.gridX >= 0 && s.gridX < 5) ? s.gridX : 2;
      const gy = (typeof s.gridY === 'number' && s.gridY >= 0 && s.gridY < 3) ? s.gridY : 1;
      grid[gy][gx] = true;
    });

    return `
      <div style="display:grid;grid-template-columns:repeat(5, 12px);gap:3px;background:#0b0f19;padding:4px 6px;border-radius:4px;border:1px solid #1e293b;width:fit-content;" title="浪漫沙加 5×3 孔位分佈圖">
        ${grid.flatMap(row => row.map(dot => `
          <div style="width:12px;height:12px;border-radius:2px;background:${dot ? '#38bdf8' : '#1e293b'};box-shadow:${dot ? '0 0 5px #38bdf8' : 'none'};"></div>
        `)).join('')}
      </div>
    `;
  };

  return `
    <div class="formation-library-container" style="display:flex;flex-direction:column;gap:14px;">
      <!-- 典籍庫頂部列：返回戰術盤按鈕與標題 -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:12px 16px;display:flex;justify-content:space-between;align-items:center;">
        <div>
          <div style="font-size:16px;font-weight:bold;color:#e2e8f0;display:flex;align-items:center;gap:8px;">
            <span>📜 道門陣法典籍庫</span>
            <span style="font-size:var(--font-xs);color:#94a3b8;font-weight:normal;">- 典藏太古修仙陣圖與凡世軍道殺陣</span>
          </div>
          <div style="font-size:var(--font-xs);color:#94a3b8;margin-top:2px;">
            當前出戰人數：${(party.members || []).length} 人 | 存活人數：${(party.members || []).filter(m => (m.alive !== undefined ? m.alive : m.hp > 0)).length} 人
          </div>
        </div>
        <button class="act-btn" onclick="window.toggleFormationViewMode('TACTICAL')" style="background:#0284c7;color:#fff;padding:6px 14px;font-size:var(--font-xs);border:none;border-radius:4px;cursor:pointer;font-weight:bold;">
          🛡️ 返回 5×3 戰陣盤
        </button>
      </div>

      <!-- 人數分類頁籤 (2 / 3 / 4 / 5 人分類) -->
      <div style="display:flex;gap:8px;border-bottom:1px solid #334155;padding-bottom:8px;flex-wrap:wrap;">
        <button class="act-btn btn-sm ${currentFilter === 'ALL' ? 'active' : ''}" onclick="window.setFormationFilterSize('ALL')" style="padding:4px 12px;font-size:var(--font-xs);background:${currentFilter === 'ALL' ? '#38bdf8' : '#1e293b'};color:${currentFilter === 'ALL' ? '#0f172a' : '#cbd5e1'};border:1px solid #475569;font-weight:bold;border-radius:4px;">
          全部 (${countAll})
        </button>
        <button class="act-btn btn-sm ${currentFilter === 5 ? 'active' : ''}" onclick="window.setFormationFilterSize(5)" style="padding:4px 12px;font-size:var(--font-xs);background:${currentFilter === 5 ? '#38bdf8' : '#1e293b'};color:${currentFilter === 5 ? '#0f172a' : '#cbd5e1'};border:1px solid #475569;font-weight:bold;border-radius:4px;">
          5人陣法 (${count5})
        </button>
        <button class="act-btn btn-sm ${currentFilter === 4 ? 'active' : ''}" onclick="window.setFormationFilterSize(4)" style="padding:4px 12px;font-size:var(--font-xs);background:${currentFilter === 4 ? '#38bdf8' : '#1e293b'};color:${currentFilter === 4 ? '#0f172a' : '#cbd5e1'};border:1px solid #475569;font-weight:bold;border-radius:4px;">
          4人陣法 (${count4})
        </button>
        <button class="act-btn btn-sm ${currentFilter === 3 ? 'active' : ''}" onclick="window.setFormationFilterSize(3)" style="padding:4px 12px;font-size:var(--font-xs);background:${currentFilter === 3 ? '#38bdf8' : '#1e293b'};color:${currentFilter === 3 ? '#0f172a' : '#cbd5e1'};border:1px solid #475569;font-weight:bold;border-radius:4px;">
          3人陣法 (${count3})
        </button>
        <button class="act-btn btn-sm ${currentFilter === 2 ? 'active' : ''}" onclick="window.setFormationFilterSize(2)" style="padding:4px 12px;font-size:var(--font-xs);background:${currentFilter === 2 ? '#38bdf8' : '#1e293b'};color:${currentFilter === 2 ? '#0f172a' : '#cbd5e1'};border:1px solid #475569;font-weight:bold;border-radius:4px;">
          2人陣法 (${count2})
        </button>
      </div>

      <!-- 典籍陣法列表 (分頁展示，每頁 6 張卡片，完美承載 20+ 陣法擴充) -->
      <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(320px, 1fr));gap:12px;">
        ${pagedList.length === 0 ? '<div style="color:#94a3b8;padding:20px;grid-column:1/-1;text-align:center;">此分類下尚無解鎖陣法</div>' : ''}
        ${pagedList.map(f => {
          const isCurrent = Boolean(f.current);
          const isSelectable = Boolean(f.selectable);
          const borderColor = isCurrent ? (isPartyBroken ? '#ef4444' : '#38bdf8') : (f.basic ? '#334155' : '#475569');
          const titleColor = isCurrent ? (isPartyBroken ? '#f87171' : '#38bdf8') : (f.basic ? '#7dd3fc' : '#c084fc');

          let actionBtn = '';
          if (isCurrent && isPartyBroken) {
            actionBtn = '<span style="font-size:var(--font-xs);color:#f87171;font-weight:bold;background:rgba(239,68,68,0.2);padding:2px 8px;border-radius:4px;border:1px solid #ef4444;">⚠️ 陣法已崩解</span>';
          } else if (isCurrent) {
            actionBtn = '<span style="font-size:var(--font-xs);color:#34d399;font-weight:bold;background:rgba(52,211,153,0.15);padding:2px 8px;border-radius:4px;border:1px solid #34d399;">✔ 當前運轉中</span>';
          } else if (isSelectable) {
            actionBtn = `<button class="act-btn btn-sm" onclick="send('formation equip ${f.id}')" style="padding:4px 12px;font-size:var(--font-xs);background:#0284c7;color:#fff;font-weight:bold;">結成此陣</button>`;
          } else {
            actionBtn = `<span style="font-size:var(--font-xs);color:#ef4444;background:rgba(239,68,68,0.15);border:1px solid #ef4444;padding:2px 8px;border-radius:4px;" title="${f.lockReason || '隊伍條件不符'}">🔒 ${f.lockReason || '不可選'}</span>`;
          }

          const typeBadge = f.basic
            ? '<span style="font-size:var(--font-xs);background:#059669;color:#ecfdf5;padding:1px 6px;border-radius:3px;">基本陣法</span>'
            : '<span style="font-size:var(--font-xs);background:#7c3aed;color:#ede9fe;padding:1px 6px;border-radius:3px;">進階陣法</span>';

          const sizeBadge = `<span style="font-size:var(--font-xs);background:#334155;color:#cbd5e1;padding:1px 6px;border-radius:3px;">${f.requiredPartySize}人陣</span>`;

          const reqClassesHtml = (f.requiredClasses && f.requiredClasses.length > 0)
            ? `<div style="font-size:var(--font-xs);color:#fbbf24;display:flex;align-items:center;gap:4px;">
                 <span>⚠️ 需職業：</span>
                 <span>${f.requiredClasses.map(formatFormationClassName).join('、')}</span>
               </div>`
            : '';

          const ultHtml = f.ultimateSkillName
            ? `<div style="font-size:var(--font-xs);color:#facc15;">⚡ 專屬奧義：【${f.ultimateSkillName}】</div>`
            : '';

          return `
            <div style="background:#0f172a;border:1px solid ${borderColor};border-radius:8px;padding:12px;display:flex;flex-direction:column;gap:8px;box-shadow:0 2px 6px rgba(0,0,0,0.3);">
              <div style="display:flex;justify-content:space-between;align-items:center;">
                <div style="display:flex;align-items:center;gap:6px;">
                  <span style="font-weight:bold;color:${titleColor};font-size:14px;">☯️ 《${f.name}》</span>
                  ${sizeBadge}
                  ${typeBadge}
                </div>
                ${actionBtn}
              </div>

              <div style="display:flex;gap:12px;align-items:flex-start;">
                <!-- 迷你 5x3 站位縮略圖 -->
                <div style="flex-shrink:0;">
                  ${renderMiniGrid(f.slots)}
                </div>
                <!-- 陣法描述與光環 -->
                <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:4px;">
                  <div style="font-size:var(--font-xs);color:#cbd5e1;line-height:1.4;">${f.description || ''}</div>
                  ${f.passiveAura ? `<div style="font-size:var(--font-xs);color:#6ee7b7;line-height:1.3;">${f.passiveAura}</div>` : ''}
                </div>
              </div>

              ${reqClassesHtml}
              ${ultHtml}
            </div>
          `;
        }).join('')}
      </div>

      <!-- 分頁導航控制列 -->
      <div style="display:flex;justify-content:center;align-items:center;gap:12px;padding:8px 0;margin-top:6px;">
        <button class="act-btn btn-sm" onclick="window.changeFormationPage(-1)" ${safePage === 0 ? 'disabled style="opacity:0.5;cursor:not-allowed;"' : ''}>
          ◀ 上一頁
        </button>
        <span style="font-size:var(--font-xs);color:#94a3b8;">
          第 <span style="color:#e2e8f0;font-weight:bold;">${safePage + 1}</span> / ${totalPages} 頁 (共 ${filteredList.length} 個陣法)
        </span>
        <button class="act-btn btn-sm" onclick="window.changeFormationPage(1)" ${safePage >= totalPages - 1 ? 'disabled style="opacity:0.5;cursor:not-allowed;"' : ''}>
          下一頁 ▶
        </button>
      </div>
    </div>
  `;
}

/**
 * 渲染全隊共有的道門陣法奧義與站位配置視圖
 */
export function renderTeamFormationView(party) {
  const isSymbols = Boolean(party.formationName && party.formationName.includes('四象'));
  const energy = party.formationEnergy || 0;
  const energyPct = Math.min(100, Math.max(0, energy));
  const ultName = party.ultimateSkillName || (isSymbols ? '四象封魔印' : '百鬼噬心');
  const inBattle = Boolean(drpgState.lastBattle && drpgState.lastBattle.inBattle);
  const canCast = Boolean(party.canCastUltimate || (energy >= 100 && inBattle));

  // 依存活與站位劃分：前衛、中衛、後衛與陣亡重傷者
  const frontMembers = [];
  const middleMembers = [];
  const backMembers = [];
  const deadMembers = [];

  // 判斷當前是「5×3 戰術站位盤」還是「道門陣法典籍庫」
  const viewMode = drpgState.formationViewMode || 'TACTICAL';
  if (viewMode === 'LIBRARY') {
    return renderFormationLibraryView(party);
  }

  // --- 模式 A: 浪漫沙加式 5×3 戰術陣盤視圖 ---
  const isFormationActive = Boolean(
    party.formationActive !== false &&
    party.formationName &&
    party.formationName !== '無' &&
    !party.formationName.includes('已崩解') &&
    !party.formationName.includes('未生效')
  );
  const isBroken = Boolean(party.formationName && party.formationName.includes('已崩解'));
  const isInactive = !isFormationActive;

  // 構建 3 列 (Row 0: FRONT, Row 1: MIDDLE, Row 2: BACK) × 5 行 (Col 0..4) 矩陣
  const gridMatrix = [
    [null, null, null, null, null], // Row 0: 前衛 FRONT
    [null, null, null, null, null], // Row 1: 中衛 MIDDLE
    [null, null, null, null, null]  // Row 2: 後衛 BACK
  ];

  (party.members || []).forEach((mem, idx) => {
    const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
    if (!isAlive) {
      deadMembers.push({ mem, idx });
    }

    // 若陣法未生效，角色都放到前衛 (Row 0)
    let gy = 0;
    let gx = Math.min(idx, 4);

    if (isFormationActive) {
      gy = (typeof mem.gridY === 'number' && mem.gridY >= 0 && mem.gridY < 3)
        ? mem.gridY
        : (mem.row === 'FRONT' ? 0 : (mem.row === 'MIDDLE' ? 1 : 2));
      gx = (typeof mem.gridX === 'number' && mem.gridX >= 0 && mem.gridX < 5)
        ? mem.gridX
        : Math.min(idx, 4);
    }

    // 碰撞保險：若該格已被佔用，找同排最近空位
    if (gridMatrix[gy][gx]) {
      let found = false;
      for (let c = 0; c < 5; c++) {
        if (!gridMatrix[gy][c]) {
          gx = c;
          found = true;
          break;
        }
      }
      if (!found) {
        // 全部排滿則塞到任意空位
        outer: for (let r = 0; r < 3; r++) {
          for (let c = 0; c < 5; c++) {
            if (!gridMatrix[r][c]) {
              gy = r;
              gx = c;
              break outer;
            }
          }
        }
      }
    }

    gridMatrix[gy][gx] = { mem, idx, isAlive };
  });

  const rowMeta = [
    { label: '⚔️ 前排線 (Front Row)', color: '#f87171', desc: '承受單體打擊與近戰反擊第一線' },
    { label: '☯️ 中排線 (Middle Row)', color: '#c084fc', desc: '核心陣眼樞紐、半攻半守機動策應' },
    { label: '🏹 後排線 (Back Row)', color: '#60a5fa', desc: '遠程道術與治癒援護，受前排雙層掩護' }
  ];

  // 渲染單個精簡孔位卡片 (依陣法與順序排布，僅姓名與生效加成，不佔空間)
  const renderSlimCell = (cellData) => {
    if (!cellData) {
      return `
        <div style="border:1px dashed #263346;border-radius:6px;min-height:52px;display:flex;align-items:center;justify-content:center;color:#475569;font-size:var(--font-xs);background:rgba(15,23,42,0.3);">
          <span style="opacity:0.4;">(空位)</span>
        </div>
      `;
    }

    const { mem, idx, isAlive } = cellData;
    const hasSlotBonus = Boolean(isAlive && isFormationActive && mem.formationSlotActive && mem.formationSlotBonus);
    const borderColor = !isAlive ? '#ef4444' : (hasSlotBonus ? '#38bdf8' : '#334155');

    return `
      <div style="background:#141b27;border:1px solid ${borderColor};border-radius:6px;padding:6px 10px;display:flex;flex-direction:column;justify-content:center;min-height:52px;box-shadow:0 2px 6px rgba(0,0,0,0.4);position:relative;gap:3px;${!isAlive ? 'opacity:0.75;' : ''}">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span style="font-weight:bold;font-size:var(--font-xs);color:${isAlive ? '#f1f5f9' : '#94a3b8'};overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="#${idx + 1} ${mem.name} (${mem.className || '道友'})">
            #${idx + 1} ${mem.name}
          </span>
          ${!isAlive ? '<span style="font-size:var(--font-xs);color:#ef4444;font-weight:bold;">💀 陣亡</span>' : ''}
        </div>
        ${hasSlotBonus ? `
          <div style="font-size:var(--font-xs);line-height:1.2;color:#6ee7b7;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${mem.formationSlotBonus}">
            💠 ${mem.formationSlotBonus}
          </div>
        ` : ''}
      </div>
    `;
  };

  return `
    <div class="team-formation-container" style="display:flex;flex-direction:column;gap:14px;">
      <!-- 1. 當前陣法光環與典籍庫切換條 -->
      <div style="background:rgba(30,27,75,0.7);border:1px solid ${isInactive ? '#ef4444' : '#6366f1'};border-radius:6px;padding:10px 14px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;">
        <div style="display:flex;align-items:center;gap:10px;font-size:var(--font-xs);">
          <span style="font-weight:bold;color:${isInactive ? '#f87171' : '#c084fc'};font-size:15px;">☯️ 【${party.formationName || '五行混元陣'}】</span>
          ${isInactive
            ? (isBroken
                ? '<span style="font-size:var(--font-xs);background:#991b1b;color:#fecaca;padding:2px 8px;border-radius:4px;font-weight:bold;">⚠️ 陣法崩解失效</span>'
                : '<span style="font-size:var(--font-xs);background:#991b1b;color:#fecaca;padding:2px 8px;border-radius:4px;font-weight:bold;">⚠️ 陣法未生效</span>')
            : '<span style="font-size:var(--font-xs);background:#4338ca;color:#e0e7ff;padding:2px 8px;border-radius:4px;font-weight:bold;">常駐運轉中</span>'}
          <span style="color:#cbd5e1;font-size:var(--font-xs);">
            ${isInactive
              ? '<span style="color:#f87171;font-weight:bold;">隊伍存活人數或配置不符陣法要求，陣法加成無法生效，全員自動歸於前排。請點擊右側「📚 切換陣法」選擇適配陣法！</span>'
              : '全隊受陣形靈威加持，陣眼與孔位契合時可爆發專屬屬性增幅。'}
          </span>
        </div>
        <button class="act-btn" onclick="window.toggleFormationViewMode('LIBRARY')" style="background:#0284c7;color:#fff;padding:6px 14px;font-size:var(--font-xs);border:none;border-radius:4px;cursor:pointer;font-weight:bold;box-shadow:0 2px 6px rgba(2,132,199,0.4);">
          📚 切換陣法 (典籍庫)
        </button>
      </div>

      <!-- 2. 浪漫沙加式 5×3 戰術站位陣盤 -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
          <div>
            <div style="font-size:15px;font-weight:bold;color:#e2e8f0;">🛡️ 隊伍戰鬥站位編排 (浪漫沙加式 5×3 戰陣盤)</div>
            <div style="font-size:var(--font-xs);color:#94a3b8;margin-top:2px;">依陣法孔位與隊伍順序自動排布。陣法未生效時全員自動歸於前排。</div>
          </div>
          <div style="font-size:var(--font-xs);color:#cbd5e1;background:#0f172a;padding:4px 10px;border-radius:4px;">
            總人數：${(party.members || []).length} / 5 人
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:12px;">
          ${rowMeta.map((row, rIdx) => `
            <div>
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
                <span style="font-size:var(--font-xs);font-weight:bold;color:${row.color};">${row.label}</span>
                <span style="font-size:var(--font-xs);color:#94a3b8;">${row.desc}</span>
              </div>
              <div style="display:grid;grid-template-columns:repeat(5, 1fr);gap:8px;">
                ${gridMatrix[rIdx].map(cell => renderSlimCell(cell)).join('')}
              </div>
            </div>
          `).join('')}
        </div>

        ${deadMembers.length > 0 ? `
        <!-- 陣亡重傷待援區 -->
        <div style="border-top:1px dashed #ef4444;margin-top:14px;padding-top:10px;display:flex;align-items:center;gap:8px;">
          <span style="font-size:var(--font-xs);font-weight:bold;color:#ef4444;">💀 陣亡重傷待援：</span>
          <span style="font-size:var(--font-xs);color:#fca5a5;">
            ${deadMembers.map(d => `#${d.idx + 1} ${d.mem.name}`).join('、')} (暫離陣法，請丹修施救或切換為 ${party.members.length - deadMembers.length} 人陣法)
          </span>
        </div>
        ` : ''}
      </div>
    </div>
  `;
}
