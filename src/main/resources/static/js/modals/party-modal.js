import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';

/**
 * 角色裝備、武學法術書與戰術方針彈窗 (Party Modal Component)
 * 負責渲染成員 5+2 裝備欄位、WoW 經典三 Tab 法術書典籍、Gambit 戰術方針 AI 配置與全隊陣法站位
 */

const drpgState = store.getState();
const send = (cmd, silent) => sendCmd(cmd, silent);

function triggerPartyAction() {
  const list = document.getElementById('party-members-list');
  if (list) {
    list.classList.remove('party-pulse');
    void list.offsetWidth;
    list.classList.add('party-pulse');
  }
  togglePartyModal();
  send('party');
}

/**
 * 開啟 / 關閉小隊 5+2 裝備與隊伍編制管理面板
 */
function togglePartyModal(forceOpen) {
  const modal = document.getElementById('party-modal');
  if (!modal) return;

  if (forceOpen === undefined) {
    drpgState.isPartyModalOpen = !drpgState.isPartyModalOpen;
  } else {
    drpgState.isPartyModalOpen = !!forceOpen;
  }

  if (drpgState.isPartyModalOpen) {
    modal.classList.remove('hidden');
    renderPartyModal();
  } else {
    modal.classList.add('hidden');
    drpgState.equipPicker = null;
    drpgState.skillPicker = null;
    if (document.activeElement) {
      document.activeElement.blur();
    }
    // 父子視窗生命週期聯動：關閉狀態主視窗時，一併關閉由此開啟的子視窗 (如公共行囊)
    if (window.toggleBagDrawer && store.get('isBagDrawerOpen')) {
      window.toggleBagDrawer(false);
    }
  }
}

function openPartyModal(memberIdx) {
  if (typeof memberIdx === 'number') {
    drpgState.selectedModalMemberIdx = memberIdx;
  }
  togglePartyModal(true);
}

function closePartyModal() {
  if (drpgState.equipPicker) {
    drpgState.equipPicker = null;
    renderPartyModal();
    return;
  }
  if (drpgState.skillPicker) {
    drpgState.skillPicker = null;
    renderPartyModal();
    return;
  }
  togglePartyModal(false);
  if (document.activeElement) {
    document.activeElement.blur();
  }
}

function selectPartyModalMember(idx) {
  drpgState.selectedModalMemberIdx = idx;
  drpgState.isAddingTactics = false;
  renderPartyModal();
}

function selectPartyFormationTab() {
  drpgState.selectedModalMemberIdx = 'FORMATION';
  drpgState.isAddingTactics = false;
  renderPartyModal();
}

function switchPartyModalSubTab(tab) {
  drpgState.partyModalSubTab = tab;
  drpgState.isAddingTactics = false;
  renderPartyModal();
}

function toggleAddTacticsForm(show) {
  drpgState.isAddingTactics = (show !== undefined) ? !!show : !drpgState.isAddingTactics;
  renderPartyModal();
}

function handleTacticsCondChange() {
  const condEl = document.getElementById('t-builder-cond');
  const valEl = document.getElementById('t-builder-val');
  const valLabel = document.getElementById('t-builder-val-label');
  if (!condEl || !valEl) return;
  const cond = condEl.value;
  if (cond === 'ALWAYS' || cond === 'ENEMY_IS_BOSS') {
    valEl.style.display = 'none';
    if (valLabel) valLabel.style.display = 'none';
  } else {
    valEl.style.display = 'inline-block';
    if (valLabel) valLabel.style.display = 'inline-block';
    if (cond === 'ALLY_HP_LESS_THAN' || cond === 'SELF_HP_LESS_THAN') {
      if (valLabel) valLabel.innerText = '氣血 (%):';
      valEl.value = 50;
    } else if (cond === 'RESOURCE_GTE') {
      if (valLabel) valLabel.innerText = '資源 (點):';
      valEl.value = 30;
    } else if (cond === 'ENEMY_COUNT_GTE') {
      if (valLabel) valLabel.innerText = '數量 (體):';
      valEl.value = 2;
    }
  }
}

function submitAddTactics(memberIdx) {
  const prioEl = document.getElementById('t-builder-prio');
  const condEl = document.getElementById('t-builder-cond');
  const valEl = document.getElementById('t-builder-val');
  const targetEl = document.getElementById('t-builder-target');
  const skillEl = document.getElementById('t-builder-skill');

  if (!prioEl || !condEl || !targetEl || !skillEl) return;
  const prio = parseInt(prioEl.value) || 1;
  const cond = condEl.value;
  const val = parseInt(valEl ? valEl.value : 0) || 0;
  const target = targetEl.value;
  const skillId = skillEl.value;

  send(`party tactics ${memberIdx} add ${prio} ${cond} ${val} ${target} ${skillId}`);
  drpgState.isAddingTactics = false;
}

function renderMemberTactics(m, idx) {
  if (idx === 0) {
    return `
      <div class="tactics-leader-box">
        <div class="tactics-leader-icon">👑</div>
        <div class="tactics-leader-title">隊長（道友親自操控）</div>
        <div class="tactics-leader-desc">
          主角為問道隊伍之核心領袖，戰鬥中所有普通攻擊、絕技道法、陣法奧義與行囊靈藥均由道友在戰場中即時親自下達指令，享有 100% 自由決策權，無需設定自動戰術方針。
        </div>
        <div class="tactics-leader-tip">
          💡 提示：點擊上方頁籤切換至同伴（如「鐵牛」、「凌霜」），即可為同伴設定專屬的 Gambit 戰鬥 AI 方針！
        </div>
      </div>
    `;
  }

  const tacticsList = m.tactics || [];
  const nextPriority = tacticsList.length > 0
    ? Math.max(...tacticsList.map(r => r.priority)) + 1
    : 1;

  let builderHtml = '';
  if (drpgState.isAddingTactics) {
    let skillOptionsHtml = `<option value="basic_attack">🗡️ 基礎普攻 (${m.basicSkillName || '普通攻擊'})</option>`;
    if (m.skills && m.skills.length > 0) {
      m.skills.forEach(s => {
        skillOptionsHtml += `<option value="${s.id}">⚡ ${s.name} (${s.costDescription || (s.costValue ? s.costValue + '消耗' : '絕技')})</option>`;
      });
    }
    if (m.availableStances && m.availableStances.length > 0) {
      m.availableStances.forEach(st => {
        skillOptionsHtml += `<option value="${st.skillId}">⚔️ ${st.skillName} (套路)</option>`;
      });
    }

    const partyMembers = (window.currentPartyData && window.currentPartyData.members) ? window.currentPartyData.members : [];
    let memberOptionsHtml = '';
    if (partyMembers && partyMembers.length > 0) {
      partyMembers.forEach((pm, pidx) => {
        const role = pidx === 0 ? '👑 隊長' : (pm.row === 'FRONT' ? '🛡️ 前衛' : '🏹 後衛');
        memberOptionsHtml += `<option value="MEMBER_${pidx + 1}">指定 #${pidx + 1} ${pm.name} (${role})</option>`;
      });
    }

    builderHtml = `
      <div class="tactics-builder-card">
        <div class="tactics-builder-title">➕ 新增戰術方針規則 (Gambit Rule)</div>
        <div class="tactics-builder-row">
          <label style="font-size:13px;color:#94a3b8;">優先級：</label>
          <input type="number" id="t-builder-prio" value="${nextPriority}" min="1" max="99" style="width:55px;font-size:13px;" />

          <label style="font-size:13px;color:#94a3b8;">觸發條件：</label>
          <select id="t-builder-cond" onchange="handleTacticsCondChange()" style="font-size:13px;">
            <option value="ALLY_HP_LESS_THAN">隊友氣血低於 (%)</option>
            <option value="SELF_HP_LESS_THAN">自身氣血低於 (%)</option>
            <option value="RESOURCE_GTE">自身資源 >= (點/怒氣/連擊)</option>
            <option value="ENEMY_COUNT_GTE">敵方存活數量 >= (體)</option>
            <option value="ENEMY_IS_BOSS">敵方存在首領 (Boss)</option>
            <option value="ALWAYS">無條件施展 (必定觸發)</option>
          </select>

          <label id="t-builder-val-label" style="font-size:13px;color:#94a3b8;">閥值：</label>
          <input type="number" id="t-builder-val" value="50" min="0" max="9999" style="width:65px;font-size:13px;" />
        </div>
        <div class="tactics-builder-row">
          <label style="font-size:13px;color:#94a3b8;">目標：</label>
          <select id="t-builder-target" style="font-size:13px;">
            <option value="FRONT_ROW_ALLY">前衛肉盾 (Tank)</option>
            <option value="LOWEST_HP_ALLY">氣血最低隊友</option>
            <option value="LEADER">小隊隊長</option>
            <option value="SELF">自身</option>
            ${memberOptionsHtml}
            <option value="BACK_ROW_ALLY">後衛隊友</option>
            <option value="CURRENT_ENEMY">當前集火目標</option>
            <option value="ALL_ENEMIES">全體敵怪</option>
            <option value="ALL_ALLIES">全體隊友</option>
          </select>

          <label style="font-size:13px;color:#94a3b8;">執行武學：</label>
          <select id="t-builder-skill" style="font-size:13px;">
            ${skillOptionsHtml}
          </select>
        </div>
        <div style="display:flex;gap:8px;margin-top:6px;">
          <button class="act-btn btn-green" type="button" onclick="submitAddTactics(${idx})">💾 確定新增規則</button>
          <button class="act-btn" type="button" onclick="toggleAddTacticsForm(false)">✕ 取消</button>
        </div>
      </div>
    `;
  }

  let rulesHtml = '';
  if (tacticsList.length === 0) {
    rulesHtml = '<div class="tactics-empty-hint">尚無設定戰術方針。該同伴在戰鬥中將默認執行基礎套路普通攻擊。</div>';
  } else {
    const sorted = [...tacticsList].sort((a, b) => a.priority - b.priority);
    rulesHtml = `
      <div class="tactics-rule-list">
        ${sorted.map(r => {
          const isAlways = (r.condition === 'ALWAYS');
          const isBoss = (r.condition === 'ENEMY_IS_BOSS');
          const valDisplay = (!isAlways && !isBoss) ? ` ${r.conditionValue}` : '';
          return `
            <div class="tactics-rule-row ${r.enabled ? 'enabled' : 'disabled'}">
              <div class="tactics-prio-badge">#${r.priority}</div>
              <div class="tactics-rule-desc">
                <span class="tactics-cond-tag">${r.conditionLabel}${valDisplay}</span>
                <span class="tactics-arrow">➜</span>
                <span class="tactics-target-tag">對 ${r.targetLabel}</span>
                <span class="tactics-arrow">➜</span>
                <span class="tactics-skill-tag">施展【${r.skillName}】</span>
              </div>
              <div class="tactics-rule-actions">
                <button class="act-btn btn-sm ${r.enabled ? 'btn-green' : 'btn-gray'}" type="button"
                  onclick="send('party tactics ${idx} toggle ${r.priority}')" title="點擊啟用或停用此規則">
                  ${r.enabled ? '🟢 啟用中' : '⚪ 已停用'}
                </button>
                <button class="act-btn btn-sm btn-red" type="button"
                  onclick="send('party tactics ${idx} delete ${r.priority}')" title="刪除此規則">
                  🗑️ 刪除
                </button>
              </div>
            </div>
          `;
        }).join('')}
      </div>
    `;
  }

  return `
    <div class="tactics-container">
      <div class="tactics-header-banner">
        <div>
          <div class="tactics-banner-title">🎯 同伴戰鬥方針設定 (Gambit AI)</div>
          <div class="tactics-banner-desc">戰鬥中輪到該同伴行動時，將依優先級序號 (#1, #2, #3...) 由上至下判定條件，首條滿足者即刻施展。</div>
        </div>
        <div class="tactics-banner-actions">
          <button class="act-btn btn-blue btn-sm" type="button" onclick="toggleAddTacticsForm(true)">➕ 新增方針</button>
          <button class="act-btn btn-sm" type="button" onclick="send('party tactics ${idx} reset')">🔄 重置預設</button>
          <button class="act-btn btn-red btn-sm" type="button" onclick="send('party tactics ${idx} clear')">🗑️ 清空方針</button>
        </div>
      </div>
      ${builderHtml}
      ${rulesHtml}
    </div>
  `;
}

/**
 * 渲染全隊共有的道門陣法奧義與站位配置視圖
 */
function renderTeamFormationView(party) {
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
  const isBroken = Boolean(party.formationName && party.formationName.includes('已崩解'));

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

    let gy = (typeof mem.gridY === 'number' && mem.gridY >= 0 && mem.gridY < 3)
      ? mem.gridY
      : (mem.row === 'FRONT' ? 0 : (mem.row === 'MIDDLE' ? 1 : 2));
    let gx = (typeof mem.gridX === 'number' && mem.gridX >= 0 && mem.gridX < 5)
      ? mem.gridX
      : Math.min(idx, 4);

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
    { label: '⚔️ 前衛線 (Front Row)', color: '#f87171', desc: '承受單體打擊與近戰反擊第一線' },
    { label: '☯️ 中衛線 (Middle Row)', color: '#c084fc', desc: '核心陣眼樞紐、半攻半守機動策應' },
    { label: '🏹 後衛線 (Back Row)', color: '#60a5fa', desc: '遠程道術與治癒援護，受前排雙層掩護' }
  ];

  // 渲染單個精簡孔位卡片 (精簡顯示：僅 #1 姓名 + 陣位效果 + 調位按鈕)
  const renderSlimCell = (cellData) => {
    if (!cellData) {
      return `
        <div style="border:1px dashed #263346;border-radius:6px;min-height:68px;display:flex;align-items:center;justify-content:center;color:#475569;font-size:var(--font-xs);background:rgba(15,23,42,0.3);">
          <span style="opacity:0.4;">(空位)</span>
        </div>
      `;
    }

    const { mem, idx, isAlive } = cellData;
    const rowLabel = mem.row === 'FRONT' ? '前衛' : (mem.row === 'MIDDLE' ? '中衛' : '後衛');
    const rowColor = mem.row === 'FRONT' ? '#f87171' : (mem.row === 'MIDDLE' ? '#c084fc' : '#60a5fa');
    const slotActive = Boolean(isAlive && mem.formationSlotActive);
    const nextRow = mem.row === 'FRONT' ? '中衛' : (mem.row === 'MIDDLE' ? '後衛' : '前衛');

    return `
      <div style="background:#141b27;border:1px solid ${isAlive ? (slotActive ? '#38bdf8' : '#eab308') : '#ef4444'};border-radius:6px;padding:6px 8px;display:flex;flex-direction:column;justify-content:space-between;min-height:68px;box-shadow:0 2px 6px rgba(0,0,0,0.4);position:relative;${!isAlive ? 'opacity:0.75;' : ''}">
        <div style="display:flex;justify-content:space-between;align-items:center;">
          <span style="font-weight:bold;font-size:var(--font-xs);color:${isAlive ? '#f1f5f9' : '#94a3b8'};overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="#${idx + 1} ${mem.name} (${mem.className || '道友'})">
            #${idx + 1} ${mem.name}
          </span>
          <span style="font-size:var(--font-xs);color:${rowColor};font-weight:bold;flex-shrink:0;">[${rowLabel}]</span>
        </div>
        <div style="font-size:var(--font-xs);line-height:1.2;margin:3px 0;color:${!isAlive ? '#ef4444' : (slotActive ? '#6ee7b7' : '#facc15')};overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${mem.formationSlotBonus || mem.formationSlotName || '自由衛位'}">
          ${!isAlive ? '💀 陣亡離陣' : (mem.formationSlotBonus ? `💠 ${mem.formationSlotBonus}` : (mem.formationSlotName || '自由策應'))}
        </div>
        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:2px;">
          <button class="act-btn btn-sm" onclick="send('formation switch ${idx}')" title="循環切換至${nextRow}" style="padding:1px 6px;font-size:var(--font-xs);background:#1e293b;color:#cbd5e1;border:1px solid #475569;border-radius:3px;">
            🔄 調至${nextRow}
          </button>
          ${!isAlive ? '<span style="font-size:var(--font-xs);color:#ef4444;font-weight:bold;">離陣</span>' : (slotActive ? '<span style="font-size:var(--font-xs);color:#34d399;">✓生效</span>' : '<span style="font-size:var(--font-xs);color:#facc15;">⚠未配</span>')}
        </div>
      </div>
    `;
  };

  return `
    <div class="team-formation-container" style="display:flex;flex-direction:column;gap:14px;">
      <!-- 1. 當前陣法光環與典籍庫切換條 -->
      <div style="background:rgba(30,27,75,0.7);border:1px solid ${isBroken ? '#ef4444' : '#6366f1'};border-radius:6px;padding:10px 14px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;">
        <div style="display:flex;align-items:center;gap:10px;font-size:var(--font-xs);">
          <span style="font-weight:bold;color:${isBroken ? '#f87171' : '#c084fc'};font-size:15px;">☯️ 【${party.formationName || '五行混元陣'}】</span>
          ${isBroken
            ? '<span style="font-size:var(--font-xs);background:#991b1b;color:#fecaca;padding:2px 8px;border-radius:4px;font-weight:bold;">⚠️ 陣法崩解失效</span>'
            : '<span style="font-size:var(--font-xs);background:#4338ca;color:#e0e7ff;padding:2px 8px;border-radius:4px;font-weight:bold;">常駐運轉中</span>'}
          <span style="color:#cbd5e1;font-size:var(--font-xs);">
            ${isBroken
              ? '<span style="color:#f87171;font-weight:bold;">因人員陣亡導致人數不符，請點擊右側「📚 切換陣法」選擇適配存活人數的陣法！</span>'
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
            <div style="font-size:var(--font-xs);color:#94a3b8;margin-top:2px;">依陣法 (X, Y) 幾何座標精準排布。點擊「🔄 調位」可循環切換【前衛 ➜ 中衛 ➜ 後衛】。</div>
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

/**
 * 模式 B: 道門陣法典籍庫 (專屬切換視窗，支援 2/3/4/5人頁籤與分頁)
 */
function renderFormationLibraryView(party) {
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

function formatFormationClassName(c) {
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

function toggleFormationViewMode(mode) {
  drpgState.formationViewMode = mode || (drpgState.formationViewMode === 'TACTICAL' ? 'LIBRARY' : 'TACTICAL');
  renderPartyModal();
}

function setFormationFilterSize(size) {
  drpgState.formationFilterSize = size;
  drpgState.formationPage = 0;
  renderPartyModal();
}

function changeFormationPage(delta) {
  drpgState.formationPage = Math.max(0, (drpgState.formationPage || 0) + delta);
  renderPartyModal();
}

function switchMainMenuTab(tab, subOption) {
  drpgState.mainMenuTab = tab || 'FORMATION';
  drpgState.equipPicker = null;
  drpgState.skillPicker = null;
  if (subOption) {
    if (tab === 'SYSTEM') drpgState.mainMenuSystemMode = subOption;
    if (tab === 'FORMATION') drpgState.formationViewMode = subOption;
    if (tab === 'ITEMS') drpgState.mainMenuItemsTab = subOption;
  }
  if (tab === 'SYSTEM') {
    send('saves quiet', true);
  }
  renderPartyModal();
}

function openMainMenu(tab, subOption) {
  drpgState.equipPicker = null;
  drpgState.skillPicker = null;
  if (tab) {
    drpgState.mainMenuTab = tab;
  }
  if (subOption) {
    if (tab === 'SYSTEM') drpgState.mainMenuSystemMode = subOption;
    if (tab === 'FORMATION') drpgState.formationViewMode = subOption;
    if (tab === 'ITEMS') drpgState.mainMenuItemsTab = subOption;
  }
  if (tab === 'SYSTEM') {
    send('saves quiet', true);
  }
  togglePartyModal(true);
}

function renderPartyModal() {
  const modal = document.getElementById('party-modal');
  if (!modal || modal.classList.contains('hidden')) return;

  const party = drpgState.lastParty;
  const subHeaderEl = document.getElementById('main-menu-sub-header');
  const contentEl = document.getElementById('main-menu-content');
  const titleTextEl = document.getElementById('main-menu-title-text');

  if (!party || !party.members || party.members.length === 0) {
    if (contentEl) contentEl.innerHTML = '<div style="color:#94a3b8;padding:40px;text-align:center;font-size:14px;">尚未載入小隊資料，請稍候...</div>';
    return;
  }

  // 1. 初始化當前選中的主選單 Tab
  if (!drpgState.mainMenuTab) {
    drpgState.mainMenuTab = 'FORMATION';
  }
  const currentTab = drpgState.mainMenuTab;

  // 2. 更新左側導覽列按鈕高亮
  const navBtns = document.querySelectorAll('#main-menu-nav .main-menu-nav-btn');
  navBtns.forEach(btn => {
    if (btn.getAttribute('data-menu') === currentTab) {
      btn.classList.add('active');
    } else {
      btn.classList.remove('active');
    }
  });

  // 3. 確保選中隊員索引有效
  if (typeof drpgState.selectedModalMemberIdx !== 'number' || drpgState.selectedModalMemberIdx >= party.members.length || drpgState.selectedModalMemberIdx < 0) {
    drpgState.selectedModalMemberIdx = 0;
  }
  const selIdx = drpgState.selectedModalMemberIdx;
  const m = party.members[selIdx] || party.members[0];

  // 4. 更新頂部標題
  const titleMap = {
    'ITEMS': '🎒 仙道總覽・公共行囊與道具 (Items)',
    'EQUIP': '🛡️ 仙道總覽・全隊 5+2 裝備與被動功法 (Equipment)',
    'SKILLS': '📖 仙道總覽・武學法術典籍 (Skills & Spells)',
    'CHARACTERS': '👤 仙道總覽・角色詳細道基面板 (Character Status)',
    'PARTY': '👥 仙道總覽・隊伍名冊編號順序 (Party Roster)',
    'FORMATION': '☯️ 仙道總覽・道門陣法與 5×3 戰陣盤 (Formations)',
    'TACTICS': '🎯 仙道總覽・同伴戰術方針 (Gambit AI)',
    'SYSTEM': '💾 仙道總覽・仙道命冊 (System Save / Load)'
  };
  if (titleTextEl) {
    titleTextEl.innerText = titleMap[currentTab] || '📜 仙道總覽・功能選單';
  }

  // 輔助函式：渲染隊員切換頁籤組 (字體嚴格保持 13~14px)
  const renderMemberTabButtons = () => {
    return party.members.map((mem, idx) => {
      const isSel = (selIdx === idx);
      const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
      const rowBadge = mem.row === 'FRONT' ? '前衛' : (mem.row === 'MIDDLE' ? '中衛' : '後衛');
      return `
        <button class="party-member-tab-btn ${isSel ? 'active' : ''}" type="button" onclick="selectPartyModalMember(${idx})"
          style="font-size:13px;padding:6px 12px;" title="#${idx + 1} ${mem.name} (${mem.className || '道友'}) - ${rowBadge}">
          <span style="font-weight:bold;">#${idx + 1} ${mem.name}</span>
          ${(idx === 0 && mem.freeStatPoints > 0) ? `<span class="hud-free-points-pill" style="margin-left:4px; font-size:var(--font-xs); padding:1px 5px;">+${mem.freeStatPoints}點</span>` : ''}
        </button>
      `;
    }).join('');
  };

  // 5. 依據 currentTab 路由渲染次級頁首與內容視口
  switch (currentTab) {
    case 'FORMATION': {
      const isLibrary = (drpgState.formationViewMode === 'LIBRARY');
      const ultBtn = party.canCastUltimate
        ? `<button class="act-btn btn-ult" onclick="send('formation cast')" style="padding:4px 12px;font-size:13px;">⚡ 施展陣法奧義【${party.ultimateSkillName}】</button>`
        : `<span style="color:#94a3b8;font-size:13px;">奧義【${party.ultimateSkillName || '無'}】(充能 ${party.formationEnergy || 0}/100)</span>`;

      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="act-btn ${!isLibrary ? 'btn-blue' : ''}" onclick="window.toggleFormationViewMode('BOARD')" style="font-size:13px;padding:5px 14px;">☯️ 5×3 戰陣盤</button>
            <button class="act-btn ${isLibrary ? 'btn-blue' : ''}" onclick="window.toggleFormationViewMode('LIBRARY')" style="font-size:13px;padding:5px 14px;">📚 陣法典籍庫</button>
          </div>
          <div style="display:flex;align-items:center;gap:12px;font-size:13px;color:#cbd5e1;">
            <span>當前道門陣法：<strong style="color:#38bdf8;">${party.formationName || '五行混元陣'}</strong></span>
            <span>⚡ 靈威：${party.formationEnergy || 0}/100</span>
            ${ultBtn}
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = isLibrary ? renderFormationLibraryView(party) : renderTeamFormationView(party);
      }
      break;
    }

    case 'SYSTEM': {
      const sysMode = drpgState.mainMenuSystemMode || 'load';
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="act-btn ${sysMode === 'load' ? 'btn-blue' : ''}" onclick="window.switchMainMenuTab('SYSTEM', 'load')" style="font-size:13px;padding:5px 14px;">📂 讀取存檔</button>
            <button class="act-btn ${sysMode === 'save' ? 'btn-blue' : ''}" onclick="window.switchMainMenuTab('SYSTEM', 'save')" style="font-size:13px;padding:5px 14px;">💾 覆蓋存檔</button>
          </div>
          <div style="font-size:13px;color:#94a3b8;">
            💡 支援 1 個即時自動存檔與 5 個手動命冊存檔槽位 (按 F5 快捷存檔)
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderSystemSlotsHtml(sysMode);
      }
      break;
    }

    case 'TACTICS': {
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:8px;overflow-x:auto;">
            ${renderMemberTabButtons()}
          </div>
          <div style="font-size:13px;color:#94a3b8;">
            🎯 FFXII Gambit 規則鏈：自上而下匹配並執行第一條符合條件的技能
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderMemberTactics(m, selIdx);
      }
      break;
    }

    case 'SKILLS': {
      const skillTab = drpgState.skillsTab || 'ACTIVE';
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:8px;overflow-x:auto;">
            ${renderMemberTabButtons()}
          </div>
          <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
            <button class="act-btn btn-sm ${skillTab === 'FIELD_USABLE' ? 'btn-blue' : ''}" onclick="window.setSkillsTab('FIELD_USABLE')" style="font-size:13px;padding:4px 12px;">🌿 可使用 (探索/回復)</button>
            <button class="act-btn btn-sm ${skillTab === 'ACTIVE' ? 'btn-blue' : ''}" onclick="window.setSkillsTab('ACTIVE')" style="font-size:13px;padding:4px 12px;">⚡ 主動絕技</button>
            <button class="act-btn btn-sm ${skillTab === 'ALL' ? 'btn-blue' : ''}" onclick="window.setSkillsTab('ALL')" style="font-size:13px;padding:4px 12px;">📖 全部道法</button>
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderMemberSkillsView(m, selIdx, party, skillTab);
      }
      break;
    }

    case 'CHARACTERS': {
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:8px;overflow-x:auto;">
            ${renderMemberTabButtons()}
          </div>
          <div style="font-size:13px;color:#94a3b8;">
            👤 角色道基天賦、五維六道與實時氣血狀態
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderMemberStatusDetailHtml(m, selIdx, party);
      }
      break;
    }

    case 'PARTY': {
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="font-size:14px;font-weight:bold;color:#f1f5f9;">
            👥 隊伍出戰名冊順序編號調整
          </div>
          <div style="font-size:13px;color:#94a3b8;">
            💡 提示：調整名冊僅改變出戰順序，與 5×3 戰陣盤上的站位座標完全解耦
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderPartyRosterHtml(party);
      }
      break;
    }

    case 'ITEMS': {
      const itemTab = drpgState.mainMenuItemsTab || 'USABLE';
      const secFilter = drpgState.itemsSecondaryFilter || 'ALL';
      if (subHeaderEl) {
        subHeaderEl.innerHTML = `
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="act-btn ${itemTab === 'USABLE' ? 'btn-blue' : ''}" onclick="window.switchMainMenuTab('ITEMS', 'USABLE')" style="font-size:13px;padding:5px 14px;">🧪 可使用 (回復/Buff)</button>
            <button class="act-btn ${itemTab === 'QUEST' ? 'btn-blue' : ''}" onclick="window.switchMainMenuTab('ITEMS', 'QUEST')" style="font-size:13px;padding:5px 14px;">📜 任務道具</button>
            <button class="act-btn ${itemTab === 'ALL' ? 'btn-blue' : ''}" onclick="window.switchMainMenuTab('ITEMS', 'ALL')" style="font-size:13px;padding:5px 14px;">📦 全部道具</button>
          </div>
          <div style="display:flex;align-items:center;gap:10px;">
            <button class="act-btn" onclick="toggleBagDrawer(true)" style="font-size:13px;padding:5px 12px;background:#334155;" title="展開右側獨立行囊抽屜">🎒 側欄行囊 (B)</button>
          </div>
        `;
      }
      if (contentEl) {
        contentEl.innerHTML = renderItemsListHtml(itemTab, secFilter, party);
      }
      break;
    }

    case 'EQUIP':
    default: {
      if (drpgState.equipPicker) {
        const pMemIdx = drpgState.equipPicker.memberIdx;
        const pMem = (party.members && party.members[pMemIdx]) ? party.members[pMemIdx] : m;
        if (subHeaderEl) {
          subHeaderEl.innerHTML = `
            <div style="display:flex;align-items:center;gap:12px;">
              <button class="act-btn btn-sm" onclick="window.closeEquipPicker()" style="font-size:13px;padding:4px 12px;background:#334155;">
                ◀ 返回全隊裝備總覽
              </button>
              <span style="font-size:15px;font-weight:bold;color:#f1f5f9;">
                🛡️ 裝備挑選與 Diff 比較 - #${pMemIdx + 1} ${pMem ? pMem.name : ''} 的【${drpgState.equipPicker.slotLabel}】
              </span>
            </div>
            <div style="font-size:13px;color:#94a3b8;">
              挑選合適法寶，比較攻防加成與武器功法相容性
            </div>
          `;
        }
        if (contentEl) {
          contentEl.innerHTML = renderEquipmentDiffPickerView(party);
        }
      } else if (drpgState.skillPicker) {
        const pMemIdx = drpgState.skillPicker.memberIdx;
        const pMem = (party.members && party.members[pMemIdx]) ? party.members[pMemIdx] : m;
        if (subHeaderEl) {
          subHeaderEl.innerHTML = `
            <div style="display:flex;align-items:center;gap:12px;">
              <button class="act-btn btn-sm" onclick="window.closeSkillPicker()" style="font-size:13px;padding:4px 12px;background:#334155;">
                ◀ 返回全隊裝備總覽
              </button>
              <span style="font-size:15px;font-weight:bold;color:#f1f5f9;">
                🧘 功法配置與挑選 - #${pMemIdx + 1} ${pMem ? pMem.name : ''} 的【${drpgState.skillPicker.categoryLabel}】
              </span>
            </div>
            <div style="font-size:13px;color:#94a3b8;">
              挑選合適的主修套路或常駐心法；未裝配時自動以角色基礎武學作為預設值
            </div>
          `;
        }
        if (contentEl) {
          contentEl.innerHTML = renderSkillPickerView(party);
        }
      } else {
        if (subHeaderEl) {
          subHeaderEl.innerHTML = `
            <div style="font-size:15px;font-weight:bold;color:#f1f5f9;display:flex;align-items:center;gap:8px;">
              <span>🛡️ 全隊裝備與功法一覽 (${(party.members || []).length} / 5 人)</span>
            </div>
            <div style="font-size:13px;color:#94a3b8;display:flex;align-items:center;gap:12px;">
              <span>💡 點擊裝備或功法槽位，即可展開專屬挑選與配置視窗</span>
              <button class="act-btn btn-sm" onclick="toggleBagDrawer(true)" style="font-size:var(--font-xs);padding:3px 10px;background:#1e293b;" title="開啟右側獨立行囊抽屜">🎒 側欄行囊 (B)</button>
            </div>
          `;
        }
        if (contentEl) {
          contentEl.innerHTML = renderTeamEquipmentOverview(party);
        }
      }
      break;
    }
  }
}

/**
 * 渲染系統存讀檔槽位清單 (System View)
 */
function renderSystemSlotsHtml(sysMode) {
  const slots = drpgState.saveSlots && drpgState.saveSlots.length > 0
    ? drpgState.saveSlots
    : [0, 1, 2, 3, 4, 5].map(id => ({ slotId: id, empty: true, title: id === 0 ? '自動存檔' : `存檔槽位 ${id}` }));

  return `
    <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(360px, 1fr));gap:14px;padding:4px 0;">
      ${slots.map(slot => {
        const isAuto = (slot.slotId === 0);
        return `
          <div class="slot-item-card ${isAuto ? 'is-autosave' : ''} ${slot.empty ? 'is-empty' : 'is-populated'}" style="margin:0;font-size:13px;">
            <div class="slot-info-col">
              <div class="slot-badge-row">
                <span class="slot-id-tag ${isAuto ? 'auto-tag' : ''}">${isAuto ? '⚡ 自動存檔' : `槽位 ${slot.slotId}`}</span>
                <span class="slot-title-text" style="font-size:14px;font-weight:bold;">${slot.title || (isAuto ? '自動存檔' : `存檔槽位 ${slot.slotId}`)}</span>
              </div>
              <div class="slot-meta-row" style="font-size:13px;margin:6px 0;">
                ${slot.empty
                  ? '<span style="color:#64748b;">-- 空無道痕 (未存檔) --</span>'
                  : `<span>👤 主角: <strong>${slot.protagonistName || '無名'}</strong></span>
                     <span>🏛️ <strong>${slot.floorName || '太陰古塚'}</strong></span>
                     <span>☯️ <strong>${slot.formationName || '四象辟邪陣'}</strong></span>
                     <span>🕒 <strong>${slot.savedAt || ''}</strong></span>`}
              </div>
              ${(!slot.empty && slot.memberNames && slot.memberNames.length > 0)
                ? `<div class="slot-members-row" style="font-size:var(--font-xs);color:#94a3b8;">👥 小隊隊容：${slot.memberNames.join('、')}</div>`
                : ''}
            </div>
            <div class="slot-actions-col">
              ${slot.empty
                ? (!isAuto ? `<button class="slot-btn slot-btn-save" onclick="window.triggerSaveSlot(${slot.slotId})" style="font-size:13px;">💾 存檔於此</button>` : '<span style="color:#64748b;font-size:var(--font-xs);">待觸發</span>')
                : `
                  <button class="slot-btn slot-btn-load" onclick="window.triggerLoadSlot(${slot.slotId})" style="font-size:13px;">${isAuto ? '📂 載入自動存檔' : '📂 載入此檔'}</button>
                  ${!isAuto ? `
                    <button class="slot-btn slot-btn-save" onclick="window.triggerSaveSlot(${slot.slotId})" style="font-size:13px;">💾 覆蓋存檔</button>
                    <button class="slot-btn slot-btn-del" onclick="window.triggerDeleteSlot(${slot.slotId})" title="刪除此存檔" style="font-size:13px;">🗑️</button>
                  ` : ''}
                `}
            </div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

/**
 * 渲染隊伍名冊調整畫面 (Party Roster View)
 */
function renderPartyRosterHtml(party) {
  const members = party.members || [];
  if (members.length === 0) {
    return '<div style="color:#94a3b8;padding:20px;text-align:center;">小隊尚無任何成員。</div>';
  }

  return `
    <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;">
      <div style="margin-bottom:14px;font-size:14px;color:#cbd5e1;">
        當前小隊編制 (${members.length} / 5 人)。點擊【▲ 上移】或【▼ 下移】調整隊員先發順位：
      </div>
      <div style="display:flex;flex-direction:column;gap:10px;">
        ${members.map((mem, idx) => {
          const isLeader = (idx === 0);
          const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
          const rowBadge = mem.row === 'FRONT' ? '前衛' : (mem.row === 'MIDDLE' ? '中衛' : '後衛');
          const rowColor = mem.row === 'FRONT' ? '#f87171' : (mem.row === 'MIDDLE' ? '#c084fc' : '#60a5fa');
          return `
            <div style="display:flex;justify-content:space-between;align-items:center;background:#0f172a;border:1px solid #334155;border-radius:6px;padding:12px 18px;">
              <div style="display:flex;align-items:center;gap:16px;">
                <span style="font-size:18px;font-weight:bold;color:${isLeader ? '#fde047' : '#38bdf8'};width:36px;">#${idx + 1}</span>
                <div>
                  <div style="font-size:15px;font-weight:bold;color:#f1f5f9;display:flex;align-items:center;gap:8px;">
                    ${isLeader ? '👑' : '👤'} ${mem.name}
                    <span style="font-size:13px;color:#94a3b8;font-weight:normal;">(${mem.roleTitle || '道友'})</span>
                    <span style="font-size:var(--font-xs);color:${rowColor};font-weight:bold;background:#1e293b;padding:2px 8px;border-radius:4px;">${rowBadge}</span>
                    ${!isAlive ? '<span style="font-size:var(--font-xs);color:#ef4444;font-weight:bold;background:#450a0a;padding:2px 8px;border-radius:4px;">💀 陣亡</span>' : ''}
                  </div>
                  <div style="font-size:13px;color:#64748b;margin-top:4px;">
                    境界 Lv.${mem.level || 1} ‧ 氣血 ${mem.hp}/${mem.maxHp} ‧ 真元/戰氣 ${mem.mp || 0}/${mem.maxMp || 0}
                  </div>
                </div>
              </div>
              <div style="display:flex;align-items:center;gap:10px;">
                <button class="act-btn btn-sm" onclick="send('party swap ${idx} ${idx - 1}')" ${idx === 0 ? 'disabled style="opacity:0.35;cursor:not-allowed;"' : ''} style="font-size:13px;padding:5px 12px;">
                  ▲ 上移
                </button>
                <button class="act-btn btn-sm" onclick="send('party swap ${idx} ${idx + 1}')" ${idx === members.length - 1 ? 'disabled style="opacity:0.35;cursor:not-allowed;"' : ''} style="font-size:13px;padding:5px 12px;">
                  ▼ 下移
                </button>
              </div>
            </div>
          `;
        }).join('')}
      </div>
    </div>
  `;
}

/**
 * 通用 20 筆單頁分頁元件 (Pagination Component)
 */
function renderPaginationBar(currentPage, totalPages, totalCount, onPageChangeFnName) {
  if (totalCount === 0) return '';
  const safeCurrent = Math.max(1, currentPage);
  const safeTotal = Math.max(1, totalPages);

  return `
    <div class="menu-pagination-bar" style="display:flex;justify-content:space-between;align-items:center;padding:10px 16px;background:#0b1120;border:1px solid #1e293b;border-radius:6px;margin-top:auto;">
      <div style="font-size:13px;color:#94a3b8;">
        共 <strong style="color:#e2e8f0;font-size:14px;">${totalCount}</strong> 筆項目 ‧ 每頁上限 20 筆
      </div>
      <div style="display:flex;align-items:center;gap:12px;">
        <button class="act-btn btn-sm" onclick="${onPageChangeFnName}(-1)" ${safeCurrent <= 1 ? 'disabled style="opacity:0.35;cursor:not-allowed;"' : ''} style="font-size:13px;padding:4px 12px;">
          ◀ 上一頁
        </button>
        <span style="font-size:13px;color:#cbd5e1;font-weight:bold;">
          第 ${safeCurrent} / ${safeTotal} 頁
        </span>
        <button class="act-btn btn-sm" onclick="${onPageChangeFnName}(1)" ${safeCurrent >= safeTotal ? 'disabled style="opacity:0.35;cursor:not-allowed;"' : ''} style="font-size:13px;padding:4px 12px;">
          下一頁 ▶
        </button>
      </div>
    </div>
  `;
}

function setItemsSecondaryFilter(filter) {
  drpgState.itemsSecondaryFilter = filter;
  drpgState.itemsPage = 1;
  renderPartyModal();
}

function changeItemsPage(delta) {
  drpgState.itemsPage = Math.max(1, (drpgState.itemsPage || 1) + delta);
  renderPartyModal();
}

function setSkillsTab(tab) {
  drpgState.skillsTab = tab;
  drpgState.skillsPage = 1;
  renderPartyModal();
}

function changeSkillsPage(delta) {
  drpgState.skillsPage = Math.max(1, (drpgState.skillsPage || 1) + delta);
  renderPartyModal();
}

/**
 * 渲染道具畫面 (Items View - 單頁上限 20 筆)
 */
function renderItemsListHtml(itemTab, secFilter, party) {
  const inv = (party && party.inventory) ? party.inventory : null;
  const allSlots = (inv && inv.slots) ? inv.slots : [];
  const currentSec = secFilter || 'ALL';

  // 1. 依據 itemTab 與 secFilter 進行多維度篩選
  const filtered = allSlots.filter(item => {
    if (!item) return false;
    if (itemTab === 'USABLE') {
      const isUsable = Boolean(item.consumable || item.effectType);
      if (!isUsable) return false;
      if (currentSec === 'HEAL') {
        return item.effectType === 'HEAL_HP' || item.effectType === 'RESTORE_SAN';
      }
      if (currentSec === 'BUFF') {
        return item.effectType === 'BUFF' || item.effectType === 'LEARN_SKILL' || (item.effectType !== 'HEAL_HP' && item.effectType !== 'RESTORE_SAN');
      }
      return true;
    } else if (itemTab === 'QUEST') {
      return item.itemType === 'QUEST' || item.subType === 'QUEST' || item.quality === 'QUEST';
    } else {
      // ALL
      if (currentSec === 'EQUIP') return Boolean(item.weapon || item.armor || item.equipment || item.itemType === 'WEAPON' || item.itemType === 'ARMOR' || item.itemType === 'SHIELD' || item.itemType === 'ACCESSORY');
      if (currentSec === 'CONSUMABLE') return Boolean(item.consumable);
      if (currentSec === 'QUEST') return item.itemType === 'QUEST' || item.subType === 'QUEST' || item.quality === 'QUEST';
      if (currentSec === 'MISC') return !item.weapon && !item.armor && !item.equipment && !item.consumable && item.itemType !== 'QUEST';
      return true;
    }
  });

  // 2. 20 筆單頁分頁計算
  const PAGE_SIZE = 20;
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const curPage = Math.min(Math.max(1, drpgState.itemsPage || 1), totalPages);
  drpgState.itemsPage = curPage;

  const startIdx = (curPage - 1) * PAGE_SIZE;
  const pagedItems = filtered.slice(startIdx, startIdx + PAGE_SIZE);

  // 3. 次級分類標籤列 (Chips)
  let chipsHtml = '';
  if (itemTab === 'USABLE') {
    chipsHtml = `
      <div style="display:flex;align-items:center;gap:8px;padding-bottom:10px;border-bottom:1px solid #1e293b;margin-bottom:12px;flex-wrap:wrap;">
        <span style="font-size:13px;color:#94a3b8;">子分類篩選：</span>
        <button class="act-btn btn-sm ${currentSec === 'ALL' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('ALL')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'ALL' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'ALL' ? '#0f172a' : '#cbd5e1'};">全部可使用</button>
        <button class="act-btn btn-sm ${currentSec === 'HEAL' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('HEAL')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'HEAL' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'HEAL' ? '#0f172a' : '#cbd5e1'};">🌿 氣血/道心回復</button>
        <button class="act-btn btn-sm ${currentSec === 'BUFF' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('BUFF')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'BUFF' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'BUFF' ? '#0f172a' : '#cbd5e1'};">⚡ 增益/丹道Buff</button>
      </div>
    `;
  } else if (itemTab === 'ALL') {
    chipsHtml = `
      <div style="display:flex;align-items:center;gap:8px;padding-bottom:10px;border-bottom:1px solid #1e293b;margin-bottom:12px;flex-wrap:wrap;">
        <span style="font-size:13px;color:#94a3b8;">品項篩選：</span>
        <button class="act-btn btn-sm ${currentSec === 'ALL' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('ALL')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'ALL' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'ALL' ? '#0f172a' : '#cbd5e1'};">全部 (${allSlots.length})</button>
        <button class="act-btn btn-sm ${currentSec === 'EQUIP' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('EQUIP')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'EQUIP' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'EQUIP' ? '#0f172a' : '#cbd5e1'};">🛡️ 裝備法寶</button>
        <button class="act-btn btn-sm ${currentSec === 'CONSUMABLE' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('CONSUMABLE')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'CONSUMABLE' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'CONSUMABLE' ? '#0f172a' : '#cbd5e1'};">🧪 消耗靈丹</button>
        <button class="act-btn btn-sm ${currentSec === 'QUEST' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('QUEST')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'QUEST' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'QUEST' ? '#0f172a' : '#cbd5e1'};">📜 任務信物</button>
        <button class="act-btn btn-sm ${currentSec === 'MISC' ? 'active' : ''}" onclick="window.setItemsSecondaryFilter('MISC')" style="font-size:13px;padding:3px 12px;background:${currentSec === 'MISC' ? '#38bdf8' : '#1e293b'};color:${currentSec === 'MISC' ? '#0f172a' : '#cbd5e1'};">📦 靈材雜項</button>
      </div>
    `;
  }

  // 4. 物品卡片渲染
  let itemsGridHtml = '';
  if (pagedItems.length === 0) {
    itemsGridHtml = `
      <div style="flex:1;display:flex;align-items:center;justify-content:center;color:#94a3b8;font-size:14px;padding:40px;border:1px dashed #334155;border-radius:6px;grid-column:1/-1;">
        🎒 此分類下尚無符合條件之靈物或道具。
      </div>
    `;
  } else {
    itemsGridHtml = pagedItems.map(item => {
      const q = (item.quality || 'COMMON').toUpperCase();
      let qColor = '#cbd5e1';
      let qBorder = '#334155';
      if (q === 'UNCOMMON') { qColor = '#34d399'; qBorder = '#059669'; }
      else if (q === 'RARE') { qColor = '#60a5fa'; qBorder = '#2563eb'; }
      else if (q === 'EPIC') { qColor = '#c084fc'; qBorder = '#7c3aed'; }
      else if (q === 'LEGENDARY') { qColor = '#fbbf24'; qBorder = '#d97706'; }
      else if (q === 'QUEST') { qColor = '#fde047'; qBorder = '#ca8a04'; }

      let effectDesc = '';
      if (item.weapon) {
        effectDesc = `<span style="color:#f87171;">🗡️ 攻 +${item.bonusMinDamage}~${item.bonusMaxDamage}</span>`;
      } else if (item.armor) {
        effectDesc = `<span style="color:#60a5fa;">🥋 防 +${item.bonusDefense}, 血 +${item.bonusHp}</span>`;
      } else if (item.effectType === 'HEAL_HP') {
        effectDesc = `<span style="color:#34d399;">🌿 服用回復 ${item.effectValue} HP</span>`;
      } else if (item.effectType === 'RESTORE_SAN') {
        effectDesc = `<span style="color:#c084fc;">📜 服用回復 ${item.effectValue} SAN (定神)</span>`;
      } else if (item.effectType === 'LEARN_SKILL') {
        effectDesc = `<span style="color:#fde047;">🧬 煉化領悟絕學【${item.grantedSkillName || '道種'}】</span>`;
      } else if (item.effectType === 'BUFF') {
        effectDesc = `<span style="color:#38bdf8;">⚡ 服用賦予專屬靈效加持</span>`;
      }

      // 操作按鍵 (支援點擊直接指定隊員)
      let actionButtons = '';
      const members = (party && party.members) ? party.members : [];
      if (item.consumable) {
        actionButtons = `
          <div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap;justify-content:flex-end;">
            <span style="font-size:var(--font-xs);color:#94a3b8;margin-right:2px;">服用給：</span>
            ${members.map((mem, mIdx) => {
              const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
              return `
                <button class="act-btn btn-sm btn-green" ${isAlive ? '' : 'disabled style="opacity:0.35;cursor:not-allowed;"'}
                  onclick="send('use ${item.slotId} ${mIdx}')" title="為 #${mIdx + 1} ${mem.name} 服用" style="font-size:var(--font-xs);padding:3px 7px;">
                  #${mIdx + 1} ${mem.name}
                </button>
              `;
            }).join('')}
          </div>
        `;
      } else if (item.weapon || item.armor || item.equipment || item.itemType === 'WEAPON' || item.itemType === 'ARMOR' || item.itemType === 'SHIELD' || item.itemType === 'ACCESSORY') {
        actionButtons = `
          <div style="display:flex;align-items:center;gap:4px;flex-wrap:wrap;justify-content:flex-end;">
            <span style="font-size:var(--font-xs);color:#94a3b8;margin-right:2px;">穿戴給：</span>
            ${members.map((mem, mIdx) => {
              const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
              return `
                <button class="act-btn btn-sm btn-blue" ${isAlive ? '' : 'disabled style="opacity:0.35;cursor:not-allowed;"'}
                  onclick="send('equip ${item.slotId} ${mIdx}')" title="為 #${mIdx + 1} ${mem.name} 穿戴" style="font-size:var(--font-xs);padding:3px 7px;">
                  #${mIdx + 1}
                </button>
              `;
            }).join('')}
          </div>
        `;
      } else if (item.itemType === 'QUEST' || item.subType === 'QUEST' || item.quality === 'QUEST') {
        actionButtons = `<span style="font-size:var(--font-xs);color:#fde047;background:rgba(253,224,71,0.15);border:1px solid #ca8a04;padding:2px 8px;border-radius:4px;">📜 機緣信物</span>`;
      }

      return `
        <div style="background:#0f172a;border:1px solid ${qBorder};border-radius:8px;padding:12px 14px;display:flex;gap:14px;align-items:center;box-shadow:0 2px 8px rgba(0,0,0,0.35);">
          <!-- 左側物品圖標 -->
          <div style="font-size:24px;width:44px;height:44px;background:#1e293b;border:1px solid #475569;border-radius:6px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
            ${item.icon || '📦'}
          </div>
          <!-- 中間文字資訊 -->
          <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:3px;">
            <div style="display:flex;align-items:center;gap:8px;">
              <span style="font-size:15px;font-weight:bold;color:${qColor};overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${item.name}</span>
              ${item.count > 1 ? `<span style="font-size:var(--font-xs);color:#cbd5e1;background:#1e293b;padding:1px 6px;border-radius:4px;font-weight:bold;flex-shrink:0;">×${item.count}</span>` : ''}
              <span style="font-size:var(--font-xs);color:#94a3b8;border:1px solid #334155;padding:1px 6px;border-radius:3px;flex-shrink:0;">${item.itemType || '道具'}</span>
            </div>
            ${effectDesc ? `<div style="font-size:13px;font-weight:500;">${effectDesc}</div>` : ''}
            <div style="font-size:var(--font-xs);color:#94a3b8;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${item.description || ''}">
              ${item.description || '無描述'}
            </div>
          </div>
          <!-- 右側操作按鈕 -->
          <div style="flex-shrink:0;">
            ${actionButtons}
          </div>
        </div>
      `;
    }).join('');
  }

  return `
    <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;flex:1;display:flex;flex-direction:column;">
      ${chipsHtml}
      <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(460px, 1fr));gap:12px;flex:1;align-content:start;">
        ${itemsGridHtml}
      </div>
      ${renderPaginationBar(curPage, totalPages, filtered.length, 'window.changeItemsPage')}
    </div>
  `;
}

/**
 * 渲染成員技能與道法典籍 (Skills View - 單頁上限 20 筆)
 */
function renderMemberSkillsView(m, memberIdx, party, skillTab) {
  const curTab = skillTab || 'ACTIVE';
  const stances = m.availableStances || [];
  const skills = m.skills || [];
  const passives = m.availablePassives || [];

  // 1. 依據 curTab 收集與分類技能
  let items = [];

  if (curTab === 'FIELD_USABLE') {
    // 探索可用：治療、道心平復、修煉或輔助絕學 (過濾 basic_*)
    skills.filter(s => s.id && !s.id.startsWith('basic_')).forEach(s => {
      const isHealOrSan = (s.description && (s.description.includes('氣血') || s.description.includes('道心') || s.description.includes('治療') || s.description.includes('清心') || s.description.includes('調息'))) || (s.category && (s.category === 'HEAL' || s.category === 'SUPPORT'));
      if (isHealOrSan) {
        items.push({
          id: s.id,
          name: s.name,
          icon: s.icon || '🌿',
          typeLabel: '探索道法',
          cost: s.costDescription || (s.costValue ? `${s.costValue} ${s.costType || 'MP'}` : '無消耗'),
          cooldown: s.cooldownMs ? `${(s.cooldownMs / 1000).toFixed(1)}秒` : '無調息',
          desc: s.description || '調息養元、穩定道心。',
          canFieldCast: true,
          badgeColor: '#34d399'
        });
      }
    });

    // 通用凝神吐納 (可作為基礎備選)
    items.push({
      id: 'rest_action',
      name: '凝神吐納 (通用)',
      icon: '🧘',
      typeLabel: '基礎調息',
      cost: '無消耗',
      cooldown: '即時',
      desc: '運轉五臟六腑真氣，緩慢調養氣血與道心 (可直接按鍵盤 R 鍵)。',
      canFieldCast: true,
      isGeneralRest: true,
      badgeColor: '#38bdf8'
    });
  } else if (curTab === 'ACTIVE') {
    // 主動絕學：主力武器套路 + 所有戰鬥絕技 (過濾 basic_*)
    stances.filter(st => st.skillId && !st.skillId.startsWith('basic_')).forEach(st => {
      items.push({
        id: st.skillId,
        name: st.skillName,
        icon: '⚔️',
        typeLabel: '主力套路',
        cost: '普攻無耗',
        cooldown: '1.5秒攻擊節奏',
        desc: st.description || '兵刃連招套路。',
        isStance: true,
        isCurrent: !!st.enabled,
        badgeColor: '#f59e0b'
      });
    });

    skills.filter(s => s.id && !s.id.startsWith('basic_')).forEach(s => {
      items.push({
        id: s.id,
        name: s.name,
        icon: s.icon || '⚡',
        typeLabel: s.category || '門派絕技',
        cost: s.costDescription || (s.costValue ? `${s.costValue} ${s.costType || 'MP'}` : '無消耗'),
        cooldown: s.cooldownMs ? `調息 ${(s.cooldownMs / 1000).toFixed(1)}秒` : '即時',
        desc: s.description || '威能莫測之道術絕招。',
        isSkill: true,
        available: !!s.available,
        badgeColor: '#38bdf8'
      });
    });
  } else {
    // ALL：全部道法 (絕學 + 套路 + 身法/招架/心法，過濾 basic_*)
    stances.filter(st => st.skillId && !st.skillId.startsWith('basic_')).forEach(st => {
      items.push({
        id: st.skillId,
        name: st.skillName,
        icon: '⚔️',
        typeLabel: '兵刃套路',
        cost: '普攻連攜',
        desc: st.description || '兵刃招式套路。',
        isStance: true,
        isCurrent: !!st.enabled,
        badgeColor: '#f59e0b'
      });
    });

    skills.filter(s => s.id && !s.id.startsWith('basic_')).forEach(s => {
      items.push({
        id: s.id,
        name: s.name,
        icon: s.icon || '⚡',
        typeLabel: '門派絕技',
        cost: s.costDescription || (s.costValue ? `${s.costValue} ${s.costType || 'MP'}` : '絕學'),
        desc: s.description || '主動道法絕技。',
        isSkill: true,
        badgeColor: '#38bdf8'
      });
    });

    passives.filter(p => p.skillId && !p.skillId.startsWith('basic_')).forEach(p => {
      let icon = '🧘';
      if (p.category === 'DODGE') icon = '💨';
      else if (p.category === 'PARRY') icon = '🛡️';
      else if (p.category === 'FORCE') icon = '🟣';

      items.push({
        id: p.skillId,
        name: p.skillName,
        icon: icon,
        category: p.category,
        typeLabel: p.categoryName || '常駐心法',
        cost: '被動常駐',
        desc: p.description || '常駐運轉之防禦或內功心法。',
        isPassive: true,
        isCurrent: !!p.isCurrentEnabled,
        badgeColor: '#c084fc'
      });
    });
  }

  // 2. 20 筆單頁分頁計算
  const PAGE_SIZE = 20;
  const totalPages = Math.max(1, Math.ceil(items.length / PAGE_SIZE));
  const curPage = Math.min(Math.max(1, drpgState.skillsPage || 1), totalPages);
  drpgState.skillsPage = curPage;

  const startIdx = (curPage - 1) * PAGE_SIZE;
  const pagedItems = items.slice(startIdx, startIdx + PAGE_SIZE);

  // 3. 網格卡片生成
  let gridHtml = '';
  if (pagedItems.length === 0) {
    gridHtml = '<div style="color:#94a3b8;padding:30px;grid-column:1/-1;text-align:center;">此分類下尚無已修習之進階武學或道法記錄。</div>';
  } else {
    gridHtml = pagedItems.map(it => {
      let actionBtn = '';
      if (it.canFieldCast) {
        if (it.isGeneralRest) {
          actionBtn = `<button class="act-btn btn-sm btn-green" onclick="window.triggerRestAction()" style="font-size:13px;padding:4px 12px;">🌿 調息吐納</button>`;
        } else {
          actionBtn = `<button class="act-btn btn-sm btn-blue" onclick="send('skill cast ${memberIdx} ${it.id} 0')" style="font-size:13px;padding:4px 12px;">⚡ 施展</button>`;
        }
      } else if (it.isStance) {
        if (it.isCurrent) {
          actionBtn = `
            <div style="display:flex;align-items:center;gap:6px;">
              <span style="font-size:var(--font-xs);color:#34d399;font-weight:bold;background:rgba(52,211,153,0.15);padding:2px 8px;border-radius:4px;border:1px solid #34d399;">✔ 當前主力</span>
              <button class="act-btn btn-sm" onclick="send('party enable ${memberIdx} none')" style="font-size:var(--font-xs);padding:2px 6px;background:rgba(239,68,68,0.2);color:#fca5a5;border:1px solid #ef4444;" title="卸下還原為基礎預設">✕ 卸下</button>
            </div>
          `;
        } else {
          actionBtn = `<button class="act-btn btn-sm" onclick="send('party enable ${memberIdx} ${it.id}')" style="font-size:var(--font-xs);padding:3px 10px;">⚔️ 設為主力</button>`;
        }
      } else if (it.isPassive) {
        if (it.isCurrent) {
          actionBtn = `
            <div style="display:flex;align-items:center;gap:6px;">
              <span style="font-size:var(--font-xs);color:#c084fc;font-weight:bold;background:rgba(192,132,252,0.15);padding:2px 8px;border-radius:4px;border:1px solid #c084fc;">✔ 運轉中</span>
              <button class="act-btn btn-sm" onclick="send('party enable ${memberIdx} none ${it.category || ''}')" style="font-size:var(--font-xs);padding:2px 6px;background:rgba(239,68,68,0.2);color:#fca5a5;border:1px solid #ef4444;" title="卸下還原為基礎預設">✕ 卸下</button>
            </div>
          `;
        } else {
          actionBtn = `<button class="act-btn btn-sm" onclick="send('party enable ${memberIdx} ${it.id}')" style="font-size:var(--font-xs);padding:3px 10px;">🧘 裝配心法</button>`;
        }
      } else {
        actionBtn = `<span style="font-size:var(--font-xs);color:#38bdf8;background:rgba(56,189,248,0.15);padding:2px 8px;border-radius:4px;">戰鬥絕技</span>`;
      }

      return `
        <div style="background:#0f172a;border:1px solid #334155;border-radius:8px;padding:12px 14px;display:flex;gap:14px;align-items:center;box-shadow:0 2px 8px rgba(0,0,0,0.35);">
          <!-- 左側圖示 -->
          <div style="font-size:24px;width:44px;height:44px;background:#1e293b;border:1px solid #475569;border-radius:6px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
            ${it.icon}
          </div>
          <!-- 中間說明 -->
          <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:3px;">
            <div style="display:flex;align-items:center;gap:8px;">
              <span style="font-size:15px;font-weight:bold;color:#f1f5f9;">${it.name}</span>
              <span style="font-size:var(--font-xs);color:${it.badgeColor || '#38bdf8'};border:1px solid #334155;padding:1px 6px;border-radius:3px;">${it.typeLabel}</span>
              ${it.cost ? `<span style="font-size:var(--font-xs);color:#e2e8f0;background:#1e293b;padding:1px 6px;border-radius:3px;">${it.cost}</span>` : ''}
              ${it.cooldown ? `<span style="font-size:var(--font-xs);color:#94a3b8;">${it.cooldown}</span>` : ''}
            </div>
            <div style="font-size:13px;color:#cbd5e1;line-height:1.4;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${it.desc}">
              ${it.desc}
            </div>
          </div>
          <!-- 右側動作 -->
          <div style="flex-shrink:0;">
            ${actionBtn}
          </div>
        </div>
      `;
    }).join('');
  }

  return `
    <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;flex:1;display:flex;flex-direction:column;">
      <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(460px, 1fr));gap:12px;flex:1;align-content:start;">
        ${gridHtml}
      </div>
      ${renderPaginationBar(curPage, totalPages, items.length, 'window.changeSkillsPage')}
    </div>
  `;
}

/**
 * 渲染角色詳細屬性道基面板 (Characters View)
 */
function renderMemberStatusDetailHtml(m, selIdx, party) {
  const isLeader = (m.id && (m.id === 'm-leader' || m.id.includes('leader'))) || selIdx === 0;
  const level = m.level || 1;
  const exp = m.exp || 0;
  const nextExp = m.nextLevelExp || 180;
  const expPct = Math.min(100, Math.max(0, Math.floor((exp / nextExp) * 100)));
  const freePoints = m.freeStatPoints || 0;

  const strVal = m.str || 5;
  const conVal = m.con || 5;
  const dexVal = m.dex || 5;
  const intVal = m.intStat !== undefined ? m.intStat : (m.intelligence || 5);
  const wisVal = m.wis || 5;

  const resType = m.resourceType || 'MP';
  const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
  const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;
  let resLabel = `MP: ${curRes}/${maxRes}`;
  if (resType === 'SP' || resType === 'RAGE' || resType === 'COMBO' || resType === 'STAMINA' || resType === 'FORCE' || resType === 'ENERGY') {
    resLabel = `戰氣: ${curRes}/${maxRes}`;
  }

  const renderStatAddBtn = (statKey, label) => {
    if (!isLeader) return '';
    if (freePoints > 0) {
      return `<button class="stat-add-btn" onclick="send('party stat add ${statKey} 1')" title="點擊投入 1 點自由修為點數提升【${label}】">+1</button>`;
    } else {
      return `<button class="stat-add-btn disabled" disabled title="無可用自由修為點數">+1</button>`;
    }
  };

  let freePointsBannerHtml = '';
  if (isLeader) {
    if (freePoints > 0) {
      freePointsBannerHtml = `
        <div class="free-points-banner" style="font-size:14px;">
          <div style="display:flex;align-items:center;gap:8px;">
            <span>⭐</span>
            <span><strong>道胎未定・造化充盈</strong>：尚有 <strong style="font-size:16px;color:#fde047;">${freePoints}</strong> 點自由修為點數！</span>
          </div>
          <span style="font-size:13px;color:#fef3c7;">(點擊下方屬性右側 [+1] 按鈕即刻分配)</span>
        </div>
      `;
    } else {
      freePointsBannerHtml = `
        <div style="font-size:13px;color:#94a3b8;display:flex;justify-content:space-between;padding:4px 6px;">
          <span>⭐ 自由修為點數：0 點</span>
          <span style="color:#64748b;">(主角每升一級額外獲贈 2 點自由分配點數)</span>
        </div>
      `;
    }
  } else {
    freePointsBannerHtml = `
      <div style="font-size:13px;color:#94a3b8;display:flex;justify-content:space-between;padding:4px 6px;">
        <span>🏷️ 成長模式：【職業範本自適應】</span>
        <span style="color:#64748b;">(同伴升級自動提升五維，無需手動微操)</span>
      </div>
    `;
  }

  return `
    <div class="party-detail-card" style="font-size:14px;">
      <div class="party-detail-header">
        <div class="party-detail-name-wrap">
          <span style="color:#38bdf8;font-weight:bold;font-size:18px;">#${selIdx + 1} ${m.name}</span>
          <span class="member-level-badge" style="font-size:13px;background:#0f172a;border:1px solid #eab308;padding:2px 8px;border-radius:4px;">Lv.${level}</span>
          ${m.className ? `<span class="member-class-badge" style="background:#1e293b;border:1px solid #38bdf8;color:#7dd3fc;padding:2px 8px;border-radius:4px;font-size:13px;">🏷️ ${m.className}</span>` : ''}
        </div>
        <span class="party-detail-role" style="font-size:13px;color:#94a3b8;">${m.roleTitle}</span>
      </div>

      <!-- 1. 修為境界與 EXP 進度條 -->
      <div class="party-detail-exp-card" style="margin:10px 0;">
        <div class="party-detail-exp-header">
          <div class="party-detail-exp-title">
            <span style="font-size:14px;">✨ 境界修為</span>
            <strong style="color:#fde047;font-size:14px;">Lv.${level}</strong>
          </div>
          <div class="party-detail-exp-val" style="font-size:13px;">EXP: ${exp} / ${nextExp} (${expPct}%)</div>
        </div>
        <div class="party-detail-exp-bar" style="height:12px;" title="晉升下一級所需修為：${exp}/${nextExp} (${expPct}%)">
          <div class="party-detail-exp-fill" style="width:${expPct}%;"></div>
        </div>
      </div>

      <!-- 2. 自由點數提示橫幅 -->
      ${freePointsBannerHtml}

      <!-- 3. 五維先天道基屬性網格 -->
      <div style="font-size:14px;color:#cbd5e1;font-weight:bold;margin:10px 0 6px 0;display:flex;justify-content:space-between;align-items:center;">
        <span>☯️ 五維先天道基：</span>
        ${isLeader && freePoints > 0 ? `<span style="font-size:13px;color:#facc15;">請點擊右側 [+1] 分配點數</span>` : ''}
      </div>
      <div class="party-detail-stats-grid">
        <div class="stat-item-box">
          <div class="stat-item-header">
            <span class="stat-item-label" style="font-size:14px;">💪 力量 STR</span>
            ${renderStatAddBtn('str', '力量')}
          </div>
          <div class="stat-item-val" style="font-size:20px;">${strVal}</div>
          <div class="stat-item-desc" style="font-size:var(--font-xs);">物理傷害、負重、招架</div>
        </div>

        <div class="stat-item-box">
          <div class="stat-item-header">
            <span class="stat-item-label" style="font-size:14px;">🫀 根骨 CON</span>
            ${renderStatAddBtn('con', '根骨')}
          </div>
          <div class="stat-item-val" style="font-size:20px;">${conVal}</div>
          <div class="stat-item-desc" style="font-size:var(--font-xs);">血量上限 (+10/點)、減傷</div>
        </div>

        <div class="stat-item-box">
          <div class="stat-item-header">
            <span class="stat-item-label" style="font-size:14px;">⚡ 靈巧 DEX</span>
            ${renderStatAddBtn('dex', '靈巧')}
          </div>
          <div class="stat-item-val" style="font-size:20px;">${dexVal}</div>
          <div class="stat-item-desc" style="font-size:var(--font-xs);">暴擊率、命中、身法閃避</div>
        </div>

        <div class="stat-item-box">
          <div class="stat-item-header">
            <span class="stat-item-label" style="font-size:14px;">🧠 悟性 INT</span>
            ${renderStatAddBtn('int', '悟性')}
          </div>
          <div class="stat-item-val" style="font-size:20px;">${intVal}</div>
          <div class="stat-item-desc" style="font-size:var(--font-xs);">真元上限 (+8/點)、法術</div>
        </div>

        <div class="stat-item-box">
          <div class="stat-item-header">
            <span class="stat-item-label" style="font-size:14px;">🧘 定力 WIS</span>
            ${renderStatAddBtn('wis', '定力')}
          </div>
          <div class="stat-item-val" style="font-size:20px;">${wisVal}</div>
          <div class="stat-item-desc" style="font-size:var(--font-xs);">治療增幅、法力恢復、抗性</div>
        </div>
      </div>

      <!-- 4. 當前實時動態條 (HP / MP / SAN) -->
      <div class="party-detail-bars" style="background:rgba(15,23,42,0.6);padding:12px;border-radius:6px;margin-top:12px;">
        <div style="font-size:14px;color:#f87171;font-weight:bold;margin-bottom:4px;">❤️ 氣血 HP: ${m.hp}/${m.maxHp}</div>
        <div style="font-size:14px;color:#60a5fa;font-weight:bold;margin-bottom:4px;">⚡ ${resLabel}</div>
        <div style="font-size:14px;color:#34d399;font-weight:bold;">🧘 道心 SAN: ${m.san}/${m.maxSan} (${m.sanityStatus || '心境平穩'})</div>
      </div>
    </div>
  `;
}

function getPassiveSkillDisplayName(m, category) {
  if (!m || !m.passiveSlots || !m.passiveSlots[category]) return null;
  const skillId = m.passiveSlots[category];
  if (!skillId || skillId.startsWith('basic_')) return null;
  if (m.availablePassives) {
    const found = m.availablePassives.find(p => p.skillId === skillId);
    if (found && found.skillName) return found.skillName;
  }
  return skillId;
}

/**
 * 渲染成員裝備與被動功法畫面 (Equipment View)
 */
function renderMemberEquipHtml(m, selIdx, party) {
  const allSlots = [
    { key: 'MAIN_HAND', alias: 'weapon', label: '主手武器', icon: '🗡️' },
    { key: 'OFF_HAND', alias: 'shield', label: '副手防具', icon: '🛡️' },
    { key: 'HEAD', alias: 'head', label: '頭部盔甲', icon: '👑' },
    { key: 'BODY', alias: 'armor', label: '身軀道袍', icon: '🥋' },
    { key: 'FEET', alias: 'feet', label: '靴履護具', icon: '👢' },
    { key: 'ACCESSORY_1', alias: 'acc1', label: '本命法寶', icon: '💍' },
    { key: 'ACCESSORY_2', alias: 'acc2', label: '輔佐靈寶', icon: '📿' }
  ];

  let equipSlotsHtml = '';
  for (const slot of allSlots) {
    let item = m.equipment ? m.equipment[slot.key] : null;
    if (!item && slot.key === 'MAIN_HAND' && m.equippedWeapon) item = m.equippedWeapon;
    if (!item && slot.key === 'BODY' && m.equippedArmor) item = m.equippedArmor;

    if (item) {
      let statsParts = [];
      if (item.bonusMinDamage || item.bonusMaxDamage) statsParts.push(`攻 ${item.bonusMinDamage}~${item.bonusMaxDamage}`);
      if (item.bonusDefense) statsParts.push(`防 +${item.bonusDefense}`);
      if (item.bonusHp) statsParts.push(`血 +${item.bonusHp}`);
      if (item.bonusSan) statsParts.push(`心 +${item.bonusSan}`);
      const statsStr = statsParts.length > 0 ? statsParts.join(' ') : '基礎裝備';

      equipSlotsHtml += `
        <div class="equip-slot-box has-item" title="${item.description || ''}" style="font-size:13px;">
          <div class="equip-slot-title">
            <span style="font-size:13px;">${slot.icon} ${slot.label}</span>
            <button class="unequip-mini-btn" onclick="send('item unequip ${slot.alias} ${selIdx}')" title="卸下放回行囊" style="font-size:var(--font-xs);">✕ 卸下</button>
          </div>
          <div class="equip-slot-name" style="font-size:14px;font-weight:bold;">${item.icon || '📦'} ${item.name}</div>
          <div class="equip-slot-stats" style="font-size:var(--font-xs);">${statsStr}</div>
        </div>
      `;
    } else {
      equipSlotsHtml += `
        <div class="equip-slot-box empty" style="font-size:13px;">
          <div class="equip-slot-title">
            <span style="font-size:13px;">${slot.icon} ${slot.label}</span>
          </div>
          <div class="equip-slot-name" style="color:#64748b;font-weight:normal;font-size:13px;">(未穿戴)</div>
          <div class="equip-slot-act">
            <button class="item-act-mini-btn" onclick="toggleBagDrawer(true)" title="開啟行囊挑選裝備穿戴" style="font-size:var(--font-xs);">🎒 挑選</button>
          </div>
        </div>
      `;
    }
  }

  // 被動功法欄位 (Stance, Parry, Dodge, Force)
  const passiveCategories = [
    { key: 'STANCE', label: '兵刃套路', icon: '⚔️', val: m.basicSkillName || null },
    { key: 'PARRY', label: '護身招架', icon: '🛡️', val: getPassiveSkillDisplayName(m, 'PARRY') },
    { key: 'DODGE', label: '靈動身法', icon: '💨', val: getPassiveSkillDisplayName(m, 'DODGE') },
    { key: 'FORCE', label: '玄門心法', icon: '🧘', val: getPassiveSkillDisplayName(m, 'FORCE') }
  ];

  let passivesHtml = passiveCategories.map(p => {
    if (p.val) {
      return `
        <div class="equip-slot-item-row" onclick="window.openSkillPicker(${selIdx}, '${p.key}', '${p.label}')" title="點擊更換功法" style="background:#0f172a;border:1px solid #334155;border-radius:6px;padding:8px 12px;display:flex;justify-content:space-between;align-items:center;cursor:pointer;">
          <span style="font-size:13px;color:#94a3b8;">${p.icon} ${p.label}</span>
          <div style="display:flex;align-items:center;gap:8px;">
            <span style="font-size:13px;color:#38bdf8;font-weight:bold;">${p.val}</span>
            <button class="act-btn btn-sm" onclick="event.stopPropagation();window.confirmUnequipSkill(${selIdx}, '${p.key}');" style="font-size:var(--font-xs);padding:2px 6px;background:rgba(239,68,68,0.2);color:#fca5a5;border:1px solid #ef4444;" title="卸下還原為預設">✕</button>
          </div>
        </div>
      `;
    } else {
      return `
        <div class="equip-slot-item-row is-empty" onclick="window.openSkillPicker(${selIdx}, '${p.key}', '${p.label}')" title="點擊挑選功法" style="background:#0f172a;border:1px dashed #334155;border-radius:6px;padding:8px 12px;display:flex;justify-content:space-between;align-items:center;cursor:pointer;">
          <span style="font-size:13px;color:#64748b;">${p.icon} ${p.label}</span>
          <span style="font-size:13px;color:#64748b;">(無) <span style="color:#94a3b8;">+ 選擇功法</span></span>
        </div>
      `;
    }
  }).join('');

  return `
    <div style="display:flex;flex-direction:column;gap:14px;">
      <!-- 1. 角色摘要條 -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:12px 18px;display:flex;justify-content:space-between;align-items:center;">
        <div style="display:flex;align-items:center;gap:12px;">
          <span style="font-size:18px;font-weight:bold;color:#38bdf8;">#${selIdx + 1} ${m.name}</span>
          <span style="font-size:13px;color:#cbd5e1;background:#0f172a;padding:2px 8px;border-radius:4px;">Lv.${m.level || 1} ${m.className || '道友'}</span>
          <span style="font-size:13px;color:#94a3b8;">氣血: ${m.hp}/${m.maxHp}</span>
        </div>
      </div>

      <!-- 2. 7 部位裝備槽位 (5 基礎 + 2 飾品) -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;">
        <div style="font-size:14px;color:#cbd5e1;font-weight:bold;margin-bottom:10px;">🛡️ 穿戴裝備欄 (7 槽位)：</div>
        <div class="party-detail-equip-grid">
          ${equipSlotsHtml}
        </div>
      </div>

      <!-- 3. 被動四槽位功法 -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;">
        <div style="font-size:14px;color:#cbd5e1;font-weight:bold;margin-bottom:10px;">⚡ 主修與被動套路功法 (4 部位)：</div>
        <div style="display:grid;grid-template-columns:repeat(auto-fit, minmax(220px, 1fr));gap:10px;">
          ${passivesHtml}
        </div>
      </div>
    </div>
  `;
}

/**
 * 裝備部位定義 (7 個部位)
 */
const EQUIP_SLOT_DEFS = [
  { key: 'MAIN_HAND', alias: 'weapon', label: '主手武器', icon: '🗡️' },
  { key: 'OFF_HAND', alias: 'shield', label: '副手防具', icon: '🛡️' },
  { key: 'HEAD', alias: 'head', label: '頭部盔甲', icon: '👑' },
  { key: 'BODY', alias: 'armor', label: '身軀道袍', icon: '🥋' },
  { key: 'FEET', alias: 'feet', label: '靴履護具', icon: '👢' },
  { key: 'ACCESSORY_1', alias: 'acc1', label: '本命法寶', icon: '💍' },
  { key: 'ACCESSORY_2', alias: 'acc2', label: '輔佐靈寶', icon: '📿' }
];

function getMemberSlotItem(m, slotKey) {
  if (m.equipment && m.equipment[slotKey]) return m.equipment[slotKey];
  if (slotKey === 'MAIN_HAND' && m.equippedWeapon) return m.equippedWeapon;
  if (slotKey === 'BODY' && m.equippedArmor) return m.equippedArmor;
  return null;
}

function isItemMatchingSlot(item, slotKey) {
  if (!item) return false;
  const eqSlot = (item.equipSlot || '').toUpperCase();
  const itType = (item.itemType || '').toUpperCase();
  const subType = (item.subType || '').toUpperCase();
  const name = item.name || '';

  switch (slotKey) {
    case 'MAIN_HAND':
      return item.weapon || itType === 'WEAPON' || eqSlot === 'MAIN_HAND' || subType === 'WEAPON';
    case 'OFF_HAND':
      return item.shield || itType === 'SHIELD' || eqSlot === 'OFF_HAND' || subType === 'SHIELD';
    case 'HEAD':
      return eqSlot === 'HEAD' || itType === 'HEAD' || subType === 'HEAD' || name.includes('冠') || name.includes('盔') || name.includes('帽');
    case 'BODY':
      return item.armor || itType === 'ARMOR' || eqSlot === 'BODY' || subType === 'ARMOR' || name.includes('甲') || name.includes('袍') || name.includes('衣');
    case 'FEET':
      return eqSlot === 'FEET' || itType === 'FEET' || subType === 'FEET' || name.includes('靴') || name.includes('履');
    case 'ACCESSORY_1':
    case 'ACCESSORY_2':
      return eqSlot === 'ACCESSORY_1' || eqSlot === 'ACCESSORY_2' || itType === 'ACCESSORY' || subType === 'ACCESSORY' || name.includes('戒') || name.includes('佩') || name.includes('鐲') || name.includes('珠');
    default:
      return false;
  }
}

function openEquipPicker(memberIdx, slotKey, slotAlias, slotLabel) {
  drpgState.equipPicker = {
    memberIdx,
    slotKey,
    slotAlias,
    slotLabel,
    selectedSlotId: null,
    page: 1
  };
  renderPartyModal();
}

function closeEquipPicker() {
  drpgState.equipPicker = null;
  renderPartyModal();
}

function selectEquipPickerItem(slotId) {
  if (drpgState.equipPicker) {
    drpgState.equipPicker.selectedSlotId = slotId;
    renderPartyModal();
  }
}

function changeEquipPickerPage(delta) {
  if (drpgState.equipPicker) {
    drpgState.equipPicker.page = Math.max(1, (drpgState.equipPicker.page || 1) + delta);
    renderPartyModal();
  }
}

function confirmEquipItem(slotId, memberIdx) {
  if (!slotId) return;
  send(`item equip ${slotId} ${memberIdx}`);
  drpgState.equipPicker = null;
}

function confirmUnequipItem(slotAlias, memberIdx) {
  if (!slotAlias) return;
  send(`item unequip ${slotAlias} ${memberIdx}`);
  drpgState.equipPicker = null;
}

function openSkillPicker(memberIdx, category, categoryLabel) {
  drpgState.skillPicker = {
    memberIdx,
    category,
    categoryLabel,
    selectedSkillId: null,
    page: 1
  };
  renderPartyModal();
}

function closeSkillPicker() {
  drpgState.skillPicker = null;
  renderPartyModal();
}

function selectSkillPickerItem(skillId) {
  if (drpgState.skillPicker) {
    drpgState.skillPicker.selectedSkillId = skillId;
    renderPartyModal();
  }
}

function changeSkillPickerPage(delta) {
  if (drpgState.skillPicker) {
    drpgState.skillPicker.page = Math.max(1, (drpgState.skillPicker.page || 1) + delta);
    renderPartyModal();
  }
}

function confirmEquipSkill(memberIdx, skillId, category) {
  if (!skillId) return;
  send(`party enable ${memberIdx} ${skillId} ${category || ''}`);
  drpgState.skillPicker = null;
}

function confirmUnequipSkill(memberIdx, category) {
  send(`party enable ${memberIdx} none ${category || ''}`);
  drpgState.skillPicker = null;
}

/**
 * 5 隊員直排全隊裝備總覽 (5-column vertical overview)
 */
function renderTeamEquipmentOverview(party) {
  const members = party.members || [];
  if (members.length === 0) {
    return '<div style="color:#94a3b8;padding:30px;text-align:center;font-size:14px;">隊伍中尚無任何成員。</div>';
  }

  const columnsHtml = members.map((m, idx) => {
    const isLeader = (idx === 0);
    const isAlive = (m.alive !== undefined ? m.alive : m.hp > 0);
    const rowBadge = m.row === 'FRONT' ? '前衛' : (m.row === 'MIDDLE' ? '中衛' : '後衛');
    const rowColor = m.row === 'FRONT' ? '#f87171' : (m.row === 'MIDDLE' ? '#c084fc' : '#60a5fa');

    // 計算累計加成數值
    let bonusMin = 0;
    let bonusMax = 0;
    let bonusDef = 0;
    let bonusHp = 0;
    let bonusSan = 0;
    const eqMap = m.equipment || {};
    Object.values(eqMap).forEach(it => {
      if (it) {
        bonusMin += (it.bonusMinDamage || 0);
        bonusMax += (it.bonusMaxDamage || 0);
        bonusDef += (it.bonusDefense || 0);
        bonusHp += (it.bonusHp || 0);
        bonusSan += (it.bonusSan || 0);
      }
    });
    if (bonusMin === 0 && bonusMax === 0 && m.equippedWeapon) {
      bonusMin += (m.equippedWeapon.bonusMinDamage || 0);
      bonusMax += (m.equippedWeapon.bonusMaxDamage || 0);
    }
    if (bonusDef === 0 && m.equippedArmor) {
      bonusDef += (m.equippedArmor.bonusDefense || 0);
    }

    // 7 個裝備槽位
    const equipRowsHtml = EQUIP_SLOT_DEFS.map(slot => {
      const item = getMemberSlotItem(m, slot.key);
      if (item) {
        let statsParts = [];
        if (item.bonusMinDamage || item.bonusMaxDamage) statsParts.push(`攻 +${item.bonusMinDamage}~${item.bonusMaxDamage}`);
        if (item.bonusDefense) statsParts.push(`防 +${item.bonusDefense}`);
        if (item.bonusHp) statsParts.push(`血 +${item.bonusHp}`);
        if (item.bonusSan) statsParts.push(`心 +${item.bonusSan}`);
        const statStr = statsParts.length > 0 ? statsParts.join(' ') : '裝備中';

        return `
          <div class="equip-slot-item-row" onclick="window.openEquipPicker(${idx}, '${slot.key}', '${slot.alias}', '${slot.label}')" title="點擊挑選更換或比較屬性">
            <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:2px;">
              <div style="display:flex;align-items:center;gap:6px;">
                <span style="font-size:13px;color:#94a3b8;">${slot.icon} ${slot.label}</span>
                <span style="font-size:13px;font-weight:bold;color:#f1f5f9;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${item.name}</span>
              </div>
              <div style="font-size:13px;color:#38bdf8;">${statStr}</div>
            </div>
            <button class="act-btn btn-sm" onclick="event.stopPropagation();send('item unequip ${slot.alias} ${idx}');" style="font-size:13px;padding:2px 8px;margin-left:6px;background:rgba(239,68,68,0.2);color:#fca5a5;border:1px solid #ef4444;" title="卸下放回行囊">
              ✕
            </button>
          </div>
        `;
      } else {
        return `
          <div class="equip-slot-item-row is-empty" onclick="window.openEquipPicker(${idx}, '${slot.key}', '${slot.alias}', '${slot.label}')" title="點擊挑選裝備穿戴">
            <span style="font-size:13px;color:#64748b;">${slot.icon} ${slot.label}</span>
            <span style="font-size:13px;color:#94a3b8;">+ 挑選裝備</span>
          </div>
        `;
      }
    }).join('');

    // 4 個被動功法槽位
    const passives = [
      { key: 'STANCE', icon: '⚔️', label: '兵刃套路', val: m.basicSkillName || null },
      { key: 'PARRY', icon: '🛡️', label: '護身招架', val: getPassiveSkillDisplayName(m, 'PARRY') },
      { key: 'DODGE', icon: '💨', label: '靈動身法', val: getPassiveSkillDisplayName(m, 'DODGE') },
      { key: 'FORCE', icon: '🟣', label: '玄門心法', val: getPassiveSkillDisplayName(m, 'FORCE') }
    ];

    const passivesHtml = passives.map(p => {
      if (p.val) {
        return `
          <div class="equip-slot-item-row" onclick="window.openSkillPicker(${idx}, '${p.key}', '${p.label}')" title="點擊更換功法">
            <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:2px;">
              <div style="display:flex;align-items:center;gap:6px;">
                <span style="font-size:13px;color:#94a3b8;">${p.icon} ${p.label}</span>
                <span style="font-size:13px;font-weight:bold;color:#38bdf8;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${p.val}</span>
              </div>
            </div>
            <button class="act-btn btn-sm" onclick="event.stopPropagation();window.confirmUnequipSkill(${idx}, '${p.key}');" style="font-size:13px;padding:2px 8px;margin-left:6px;background:rgba(239,68,68,0.2);color:#fca5a5;border:1px solid #ef4444;" title="卸下還原為基礎預設">
              ✕
            </button>
          </div>
        `;
      } else {
        return `
          <div class="equip-slot-item-row is-empty" onclick="window.openSkillPicker(${idx}, '${p.key}', '${p.label}')" title="點擊挑選功法">
            <span style="font-size:13px;color:#64748b;">${p.icon} ${p.label}</span>
            <span style="font-size:13px;color:#64748b;">(無) <span style="color:#94a3b8;">+ 選擇功法</span></span>
          </div>
        `;
      }
    }).join('');

    return `
      <div class="equip-member-col">
        <!-- 角色標頭 -->
        <div class="equip-member-header">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <span style="font-size:16px;font-weight:bold;color:${isLeader ? '#fde047' : '#38bdf8'};display:flex;align-items:center;gap:4px;">
              ${isLeader ? '👑' : '👤'} #${idx + 1} ${m.name}
            </span>
            <span style="font-size:13px;color:${rowColor};font-weight:bold;background:#1e293b;padding:2px 8px;border-radius:4px;">${rowBadge}</span>
          </div>
          <div style="display:flex;align-items:center;gap:8px;font-size:13px;color:#cbd5e1;">
            <span>Lv.${m.level || 1}</span>
            <span>${m.className || '道友'}</span>
            ${!isAlive ? '<span style="color:#ef4444;font-weight:bold;">(💀 陣亡)</span>' : ''}
          </div>
        </div>

        <!-- 屬性面板 (原始點數 + 裝備加成) 與 Buff -->
        <div class="equip-member-stats-box">
          <div style="display:flex;justify-content:space-between;font-size:13px;">
            <span style="color:#f87171;">❤️ 氣血: ${m.hp}/${m.maxHp}</span>
            <span style="color:#60a5fa;">⚡ 戰氣: ${m.currentResource !== undefined ? m.currentResource : m.mp}/${m.maxResource !== undefined ? m.maxResource : m.maxMp}</span>
          </div>
          <div style="display:flex;justify-content:space-between;border-top:1px solid #334155;padding-top:4px;font-size:13px;">
            <span style="color:#fde047;">⚔️ 攻 +${bonusMin}~${bonusMax}</span>
            <span style="color:#34d399;">🛡️ 防 +${bonusDef}</span>
          </div>
          <div style="font-size:13px;color:#94a3b8;border-top:1px solid #334155;padding-top:4px;display:flex;justify-content:space-between;">
            <span>力${m.str || 5}</span>
            <span>骨${m.con || 5}</span>
            <span>巧${m.dex || 5}</span>
            <span>悟${m.intStat !== undefined ? m.intStat : (m.intelligence || 5)}</span>
            <span>定${m.wis || 5}</span>
          </div>
          ${m.formationSlotBonus ? `
            <div style="font-size:13px;color:#6ee7b7;background:rgba(16,185,129,0.12);padding:3px 8px;border-radius:4px;margin-top:2px;">
              ☯️ 陣法: ${m.formationSlotBonus}
            </div>
          ` : ''}
        </div>

        <!-- 7 部位裝備欄 -->
        <div style="display:flex;flex-direction:column;gap:6px;">
          <div style="font-size:13px;font-weight:bold;color:#cbd5e1;display:flex;justify-content:space-between;align-items:center;">
            <span>🛡️ 裝備部位 (7 槽位)</span>
            <span style="font-size:13px;color:#64748b;">點擊換裝</span>
          </div>
          ${equipRowsHtml}
        </div>

        <!-- 4 部位被動功法 -->
        <div style="display:flex;flex-direction:column;gap:6px;margin-top:auto;">
          <div style="font-size:13px;font-weight:bold;color:#cbd5e1;display:flex;justify-content:space-between;align-items:center;">
            <span>🧘 主修功法 (4 部位)</span>
            <span style="font-size:13px;color:#64748b;">點擊設定</span>
          </div>
          ${passivesHtml}
        </div>
      </div>
    `;
  }).join('');

  return `
    <div class="equip-team-grid">
      ${columnsHtml}
    </div>
  `;
}

/**
 * 裝備挑選與 Diff 比較子視圖 (Sub-view Overlay)
 */
function renderEquipmentDiffPickerView(party) {
  const picker = drpgState.equipPicker;
  if (!picker) return '';

  const memberIdx = picker.memberIdx || 0;
  const m = (party.members && party.members[memberIdx]) ? party.members[memberIdx] : null;
  if (!m) return '<div style="color:#94a3b8;padding:20px;font-size:14px;">隊員資料異常。</div>';

  const slotKey = picker.slotKey;
  const slotAlias = picker.slotAlias;
  const slotLabel = picker.slotLabel;
  const curItem = getMemberSlotItem(m, slotKey);

  // 1. 從背包過濾出符合此槽位的裝備候選清單
  const inv = (party && party.inventory) ? party.inventory : null;
  const allSlots = (inv && inv.slots) ? inv.slots : [];
  const candidates = allSlots.filter(it => isItemMatchingSlot(it, slotKey));

  // 2. 如果尚未指定選取項，預設選取候選列表第 1 項
  if (!picker.selectedSlotId && candidates.length > 0) {
    picker.selectedSlotId = candidates[0].slotId;
  }

  // 3. 20 筆單頁分頁計算
  const PAGE_SIZE = 20;
  const totalPages = Math.max(1, Math.ceil(candidates.length / PAGE_SIZE));
  const curPage = Math.min(Math.max(1, picker.page || 1), totalPages);
  picker.page = curPage;

  const startIdx = (curPage - 1) * PAGE_SIZE;
  const pagedCandidates = candidates.slice(startIdx, startIdx + PAGE_SIZE);

  // 4. 左側候選列表 HTML
  let candidateListHtml = '';
  if (pagedCandidates.length === 0) {
    candidateListHtml = `
      <div style="padding:40px 20px;text-align:center;color:#94a3b8;display:flex;flex-direction:column;gap:12px;align-items:center;grid-column:1/-1;">
        <span style="font-size:36px;">🎒</span>
        <span style="font-size:15px;color:#e2e8f0;font-weight:bold;">行囊中尚無可穿戴的【${slotLabel}】</span>
        <span style="font-size:13px;color:#64748b;">可至古塚探勘擊殺妖邪獲取裝備戰利品。</span>
      </div>
    `;
  } else {
    candidateListHtml = pagedCandidates.map(it => {
      const isSelected = (it.slotId === picker.selectedSlotId);
      let statsParts = [];
      if (it.bonusMinDamage || it.bonusMaxDamage) statsParts.push(`攻 +${it.bonusMinDamage}~${it.bonusMaxDamage}`);
      if (it.bonusDefense) statsParts.push(`防 +${it.bonusDefense}`);
      if (it.bonusHp) statsParts.push(`血 +${it.bonusHp}`);
      if (it.bonusSan) statsParts.push(`心 +${it.bonusSan}`);
      const statsStr = statsParts.length > 0 ? statsParts.join(' ‧ ') : '基礎裝備';

      return `
        <div class="equip-candidate-card ${isSelected ? 'is-selected' : ''}" onclick="window.selectEquipPickerItem('${it.slotId}')">
          <div style="font-size:24px;width:40px;height:40px;background:#0b1120;border:1px solid #334155;border-radius:6px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
            ${it.icon || '📦'}
          </div>
          <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:2px;">
            <div style="display:flex;align-items:center;gap:6px;">
              <span style="font-size:14px;font-weight:bold;color:#f1f5f9;">${it.name}</span>
              ${it.count > 1 ? `<span style="font-size:13px;color:#94a3b8;">x${it.count}</span>` : ''}
              ${isSelected ? '<span style="font-size:13px;color:#38bdf8;background:rgba(56,189,248,0.15);padding:1px 6px;border-radius:3px;">比較中</span>' : ''}
            </div>
            <div style="font-size:13px;color:#38bdf8;">${statsStr}</div>
          </div>
          <button class="act-btn btn-sm btn-blue" onclick="event.stopPropagation();window.confirmEquipItem('${it.slotId}', ${memberIdx});" style="font-size:13px;padding:4px 12px;flex-shrink:0;">
            ✨ 穿戴
          </button>
        </div>
      `;
    }).join('');
  }

  // 5. 右側 Diff 比較面板
  const selectedCandidate = allSlots.find(s => s.slotId === picker.selectedSlotId);

  const renderDiffMetric = (label, curVal, candVal) => {
    const cVal = curVal || 0;
    const nVal = candVal || 0;
    const diff = nVal - cVal;
    let diffBadge = '';
    if (diff > 0) {
      diffBadge = `<span style="color:#34d399;font-weight:bold;background:rgba(52,211,153,0.15);padding:2px 8px;border-radius:4px;border:1px solid #34d399;font-size:13px;">+${diff} ▲ (提升)</span>`;
    } else if (diff < 0) {
      diffBadge = `<span style="color:#f87171;font-weight:bold;background:rgba(239,68,68,0.15);padding:2px 8px;border-radius:4px;border:1px solid #ef4444;font-size:13px;">${diff} ▼ (降低)</span>`;
    } else {
      diffBadge = `<span style="color:#94a3b8;background:#1e293b;padding:2px 8px;border-radius:4px;font-size:13px;">持平</span>`;
    }

    return `
      <div style="display:flex;justify-content:space-between;align-items:center;padding:8px 0;border-bottom:1px solid #1e293b;font-size:13px;">
        <span style="color:#cbd5e1;">${label}</span>
        <div style="display:flex;align-items:center;gap:12px;">
          <span style="color:#94a3b8;">${cVal} ➔ <strong style="color:#f1f5f9;font-size:14px;">${nVal}</strong></span>
          ${diffBadge}
        </div>
      </div>
    `;
  };

  // 武器功法套路相容性分析
  let stanceCheckHtml = '';
  if (slotKey === 'MAIN_HAND') {
    const currentStance = m.basicSkillName || '基礎套路';
    let isStanceMatch = false;
    let candWeaponType = '兵刃';

    if (selectedCandidate) {
      const wName = selectedCandidate.name || '';
      const wSub = (selectedCandidate.subType || '').toUpperCase();
      if (wName.includes('劍') || wSub.includes('SWORD')) candWeaponType = '劍類';
      else if (wName.includes('刀') || wSub.includes('BLADE')) candWeaponType = '刀類';
      else if (wName.includes('拳') || wName.includes('掌') || wName.includes('爪') || wSub.includes('FIST')) candWeaponType = '拳掌類';
      else if (wName.includes('槍') || wName.includes('矛') || wSub.includes('SPEAR')) candWeaponType = '長槍類';
      else if (wName.includes('棍') || wName.includes('杖') || wSub.includes('STAFF')) candWeaponType = '棍杖類';

      // 檢查當前套路是否符合
      if (candWeaponType === '劍類' && (currentStance.includes('劍') || currentStance.includes('sword'))) isStanceMatch = true;
      else if (candWeaponType === '刀類' && (currentStance.includes('刀') || currentStance.includes('blade'))) isStanceMatch = true;
      else if (candWeaponType === '拳掌類' && (currentStance.includes('拳') || currentStance.includes('掌') || currentStance.includes('fist'))) isStanceMatch = true;
      else if (candWeaponType === '長槍類' && (currentStance.includes('槍') || currentStance.includes('spear'))) isStanceMatch = true;
      else if (candWeaponType === '棍杖類' && (currentStance.includes('棍') || currentStance.includes('杖') || currentStance.includes('staff'))) isStanceMatch = true;
      else if (currentStance.includes('基礎') || currentStance.includes('basic')) isStanceMatch = true;
    }

    if (selectedCandidate) {
      if (isStanceMatch) {
        stanceCheckHtml = `
          <div style="background:rgba(16,185,129,0.12);border:1px solid #10b981;border-radius:6px;padding:12px 14px;display:flex;flex-direction:column;gap:4px;">
            <div style="font-size:13px;font-weight:bold;color:#34d399;display:flex;align-items:center;gap:6px;">
              <span>✔ 武器功法契合：【${candWeaponType}】</span>
            </div>
            <div style="font-size:13px;color:#a7f3d0;line-height:1.4;">
              此武器與當前主力套路【${currentStance}】契合，普通攻擊與連攜招式可享全額增幅！
            </div>
          </div>
        `;
      } else {
        stanceCheckHtml = `
          <div style="background:rgba(245,158,11,0.12);border:1px solid #f59e0b;border-radius:6px;padding:12px 14px;display:flex;flex-direction:column;gap:4px;">
            <div style="font-size:13px;font-weight:bold;color:#fbbf24;display:flex;align-items:center;gap:6px;">
              <span>⚠️ 武器功法切換提示：【${candWeaponType}】</span>
            </div>
            <div style="font-size:13px;color:#fde68a;line-height:1.4;">
              當前主力套路為【${currentStance}】。裝備此武器後，建議前往「技能&法術」分頁啟用對應兵刃套路，以發揮最大威力。
            </div>
          </div>
        `;
      }
    }
  }

  let diffPanelHtml = '';
  if (!selectedCandidate) {
    diffPanelHtml = `
      <div style="display:flex;flex-direction:column;gap:16px;">
        <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:16px;color:#94a3b8;text-align:center;font-size:13px;">
          請從左側點選一件候選裝備進行屬性比較
        </div>
        ${curItem ? `
          <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:14px;display:flex;flex-direction:column;gap:8px;">
            <div style="font-size:14px;font-weight:bold;color:#cbd5e1;">當前穿戴中：</div>
            <div style="font-size:15px;font-weight:bold;color:#f1f5f9;">${curItem.icon || '📦'} ${curItem.name}</div>
            <button class="act-btn btn-sm btn-red" onclick="window.confirmUnequipItem('${slotAlias}', ${memberIdx})" style="font-size:13px;padding:6px 14px;margin-top:8px;">
              ✕ 卸下當前裝備
            </button>
          </div>
        ` : ''}
      </div>
    `;
  } else {
    diffPanelHtml = `
      <div style="display:flex;flex-direction:column;gap:12px;">
        <!-- 當前 vs 候選頂部卡片對比 -->
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:10px;">
          <!-- 當前 -->
          <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:10px 12px;display:flex;flex-direction:column;gap:4px;">
            <span style="font-size:13px;color:#94a3b8;font-weight:bold;">【當前穿戴】</span>
            <span style="font-size:14px;font-weight:bold;color:${curItem ? '#f1f5f9' : '#64748b'};overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
              ${curItem ? `${curItem.icon || '📦'} ${curItem.name}` : '(未穿戴任何裝備)'}
            </span>
          </div>
          <!-- 候選 -->
          <div style="background:#1e293b;border:1px solid #38bdf8;border-radius:6px;padding:10px 12px;display:flex;flex-direction:column;gap:4px;">
            <span style="font-size:13px;color:#38bdf8;font-weight:bold;">【候選更換】</span>
            <span style="font-size:14px;font-weight:bold;color:#f1f5f9;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
              ${selectedCandidate.icon || '📦'} ${selectedCandidate.name}
            </span>
          </div>
        </div>

        <!-- 數值 Diff 條列 -->
        <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:10px 14px;">
          <div style="font-size:14px;font-weight:bold;color:#cbd5e1;margin-bottom:6px;">📊 屬性變化比較：</div>
          ${renderDiffMetric('物理攻擊 (最小)', curItem?.bonusMinDamage, selectedCandidate.bonusMinDamage)}
          ${renderDiffMetric('物理攻擊 (最大)', curItem?.bonusMaxDamage, selectedCandidate.bonusMaxDamage)}
          ${renderDiffMetric('護甲防禦', curItem?.bonusDefense, selectedCandidate.bonusDefense)}
          ${renderDiffMetric('氣血加成', curItem?.bonusHp, selectedCandidate.bonusHp)}
          ${renderDiffMetric('道心增幅', curItem?.bonusSan, selectedCandidate.bonusSan)}
        </div>

        <!-- 功法契合性提示 -->
        ${stanceCheckHtml}

        <!-- 物品描述與特殊道韻 -->
        ${(selectedCandidate.description || selectedCandidate.grantedSkillName) ? `
          <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:10px 14px;font-size:13px;color:#cbd5e1;line-height:1.4;">
            <div style="font-weight:bold;color:#94a3b8;margin-bottom:2px;">📜 法寶靈韻：</div>
            ${selectedCandidate.grantedSkillName ? `<div style="color:#fde047;font-weight:bold;margin-bottom:2px;">⚡ 附帶奧義：【${selectedCandidate.grantedSkillName}】</div>` : ''}
            <div>${selectedCandidate.description || ''}</div>
          </div>
        ` : ''}

        <!-- 操作按鈕列 -->
        <div style="display:flex;gap:10px;margin-top:6px;">
          <button class="act-btn btn-blue" onclick="window.confirmEquipItem('${selectedCandidate.slotId}', ${memberIdx})" style="flex:1;font-size:14px;font-weight:bold;padding:10px 16px;">
            ✨ 即刻確認穿戴
          </button>
          ${curItem ? `
            <button class="act-btn btn-red" onclick="window.confirmUnequipItem('${slotAlias}', ${memberIdx})" style="font-size:13px;padding:10px 14px;">
              ✕ 卸下當前
            </button>
          ` : ''}
        </div>
      </div>
    `;
  }

  return `
    <div class="equip-diff-view">
      <!-- 左側候選列表 -->
      <div class="equip-candidate-list-col">
        <div style="font-size:14px;font-weight:bold;color:#e2e8f0;margin-bottom:10px;display:flex;justify-content:space-between;align-items:center;">
          <span>🎒 行囊中可穿戴的【${slotLabel}】(${candidates.length})</span>
          <span style="font-size:13px;color:#94a3b8;">每頁上限 20 筆</span>
        </div>
        <div class="equip-candidate-grid">
          ${candidateListHtml}
        </div>
        ${renderPaginationBar(curPage, totalPages, candidates.length, 'window.changeEquipPickerPage')}
      </div>

      <!-- 右側 Diff 比較面板 -->
      <div class="equip-diff-panel-col">
        <div style="font-size:15px;font-weight:bold;color:#38bdf8;border-bottom:1px solid #334155;padding-bottom:8px;">
          ⚖️ 法寶數值比較與功法相容性
        </div>
        ${diffPanelHtml}
      </div>
    </div>
  `;
}

/**
 * 功法與套路挑選子視圖 (Skill Picker Sub-view)
 * 支援兵刃套路依手持武器類型動態過濾、心法分類過濾、20 筆單頁分頁與還原預設
 */
function renderSkillPickerView(party) {
  const picker = drpgState.skillPicker;
  if (!picker) return '';

  const memberIdx = picker.memberIdx || 0;
  const m = (party.members && party.members[memberIdx]) ? party.members[memberIdx] : null;
  if (!m) return '<div style="color:#94a3b8;padding:20px;font-size:14px;">隊員資料異常。</div>';

  const category = picker.category; // 'STANCE', 'PARRY', 'DODGE', 'FORCE'
  const categoryLabel = picker.categoryLabel;

  // 1. 取得主手武器資訊
  const mainWeapon = getMemberSlotItem(m, 'MAIN_HAND');
  const weaponName = mainWeapon ? mainWeapon.name : '空手';

  // 2. 候選功法收集 (徹底過濾 basic_*)
  let candidates = [];
  let curEquippedName = null;

  if (category === 'STANCE') {
    curEquippedName = m.basicSkillName || null;
    const rawStances = m.availableStances || [];
    candidates = rawStances.filter(st => st.skillId && !st.skillId.startsWith('basic_')).map(st => {
      let icon = '⚔️';
      const sId = (st.skillId || '').toLowerCase();
      if (sId.includes('blade')) icon = '🗡️';
      else if (sId.includes('axe')) icon = '🪓';
      else if (sId.includes('staff') || sId.includes('stick')) icon = '🦯';
      else if (sId.includes('fist') || sId.includes('unarmed')) icon = '👊';
      else if (sId.includes('spear')) icon = '🔱';
      return {
        id: st.skillId,
        name: st.skillName,
        icon: icon,
        typeLabel: '兵刃套路',
        cost: '自動普攻',
        desc: st.description || '隨兵刃揮舞自動施展之套路招式。',
        isCurrent: Boolean(st.enabled || (curEquippedName && curEquippedName === st.skillName))
      };
    });
  } else {
    // PARRY, DODGE, FORCE
    curEquippedName = getPassiveSkillDisplayName(m, category);
    const curEquippedId = (m.passiveSlots && m.passiveSlots[category]) || null;
    const rawPassives = m.availablePassives || [];
    candidates = rawPassives.filter(p => p.category === category && p.skillId && !p.skillId.startsWith('basic_')).map(p => {
      let icon = '🧘';
      if (p.category === 'DODGE') icon = '💨';
      else if (p.category === 'PARRY') icon = '🛡️';
      else if (p.category === 'FORCE') icon = '🟣';
      return {
        id: p.skillId,
        name: p.skillName,
        icon: icon,
        typeLabel: p.categoryName || categoryLabel,
        cost: '常駐運轉',
        desc: p.description || '常駐運轉之防禦或內功心法。',
        isCurrent: Boolean(p.isCurrentEnabled || (curEquippedId && curEquippedId === p.skillId) || (curEquippedName && curEquippedName === p.skillName))
      };
    });
  }

  // 3. 預設選中第一項
  if (!picker.selectedSkillId && candidates.length > 0) {
    picker.selectedSkillId = candidates[0].id;
  }

  // 4. 20 筆單頁分頁計算
  const PAGE_SIZE = 20;
  const totalPages = Math.max(1, Math.ceil(candidates.length / PAGE_SIZE));
  const curPage = Math.min(Math.max(1, picker.page || 1), totalPages);
  picker.page = curPage;

  const startIdx = (curPage - 1) * PAGE_SIZE;
  const pagedCandidates = candidates.slice(startIdx, startIdx + PAGE_SIZE);

  // 5. 左側候選列表 HTML
  let candidateListHtml = '';
  if (pagedCandidates.length === 0) {
    let emptyReason = '';
    if (category === 'STANCE') {
      emptyReason = `尚未習得契合主手武器【${weaponName}】之進階套路，將以基礎武學（預設）應戰。`;
    } else {
      emptyReason = `尚未領悟此類進階常駐心法，將以基礎心法（預設）運轉。`;
    }
    candidateListHtml = `
      <div style="padding:40px 20px;text-align:center;color:#94a3b8;display:flex;flex-direction:column;gap:12px;align-items:center;grid-column:1/-1;">
        <span style="font-size:36px;">📜</span>
        <span style="font-size:15px;color:#e2e8f0;font-weight:bold;">尚無可啟用的【${categoryLabel}】</span>
        <span style="font-size:13px;color:#64748b;">${emptyReason}</span>
        <span style="font-size:var(--font-xs);color:#475569;">可於秘境古塚中研讀武道秘笈以領悟新功法。</span>
      </div>
    `;
  } else {
    candidateListHtml = pagedCandidates.map(it => {
      const isSelected = (it.id === picker.selectedSkillId);
      return `
        <div class="equip-candidate-card ${isSelected ? 'is-selected' : ''}" onclick="window.selectSkillPickerItem('${it.id}')">
          <div style="font-size:24px;width:40px;height:40px;background:#0b1120;border:1px solid #334155;border-radius:6px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
            ${it.icon}
          </div>
          <div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:2px;">
            <div style="display:flex;align-items:center;gap:6px;">
              <span style="font-size:14px;font-weight:bold;color:#f1f5f9;">${it.name}</span>
              ${it.isCurrent ? '<span style="font-size:var(--font-xs);color:#34d399;background:rgba(52,211,153,0.15);padding:1px 6px;border-radius:3px;border:1px solid #34d399;">✔ 當前裝備</span>' : ''}
              ${isSelected ? '<span style="font-size:var(--font-xs);color:#38bdf8;background:rgba(56,189,248,0.15);padding:1px 6px;border-radius:3px;">選取中</span>' : ''}
            </div>
            <div style="font-size:var(--font-xs);color:#94a3b8;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${it.desc}</div>
          </div>
          ${it.isCurrent ? `
            <button class="act-btn btn-sm btn-red" onclick="event.stopPropagation();window.confirmUnequipSkill(${memberIdx}, '${category}');" style="font-size:var(--font-xs);padding:3px 10px;flex-shrink:0;">
              ✕ 卸下
            </button>
          ` : `
            <button class="act-btn btn-sm btn-blue" onclick="event.stopPropagation();window.confirmEquipSkill(${memberIdx}, '${it.id}', '${category}');" style="font-size:var(--font-xs);padding:3px 10px;flex-shrink:0;">
              ⚡ 啟用
            </button>
          `}
        </div>
      `;
    }).join('');
  }

  // 6. 右側詳情面板
  const selectedCandidate = candidates.find(c => c.id === picker.selectedSkillId);
  let detailPanelHtml = '';
  if (!selectedCandidate) {
    detailPanelHtml = `
      <div style="display:flex;flex-direction:column;gap:14px;">
        <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:16px;color:#94a3b8;text-align:center;font-size:13px;">
          ${candidates.length > 0 ? '請從左側點選一門功法查看詳情' : '當前分類無可選功法'}
        </div>
        ${curEquippedName ? `
          <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:14px;display:flex;flex-direction:column;gap:8px;">
            <div style="font-size:13px;font-weight:bold;color:#cbd5e1;">當前運轉功法：</div>
            <div style="font-size:15px;font-weight:bold;color:#38bdf8;">${curEquippedName}</div>
            <button class="act-btn btn-sm btn-red" onclick="window.confirmUnequipSkill(${memberIdx}, '${category}')" style="font-size:13px;padding:6px 14px;margin-top:6px;">
              ✕ 卸下還原為預設 (無)
            </button>
          </div>
        ` : `
          <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:14px;color:#64748b;font-size:13px;">
            當前狀態：(無) ‧ 系統預設運轉基礎武學。
          </div>
        `}
      </div>
    `;
  } else {
    detailPanelHtml = `
      <div style="display:flex;flex-direction:column;gap:12px;">
        <!-- 頂部功法卡片 -->
        <div style="background:#1e293b;border:1px solid #38bdf8;border-radius:6px;padding:12px;display:flex;align-items:center;gap:12px;">
          <div style="font-size:28px;width:48px;height:48px;background:#0b1120;border:1px solid #334155;border-radius:6px;display:flex;align-items:center;justify-content:center;flex-shrink:0;">
            ${selectedCandidate.icon}
          </div>
          <div style="flex:1;min-width:0;">
            <div style="font-size:16px;font-weight:bold;color:#f1f5f9;">${selectedCandidate.name}</div>
            <div style="font-size:var(--font-xs);color:#38bdf8;margin-top:2px;">${selectedCandidate.typeLabel} ‧ ${selectedCandidate.cost}</div>
          </div>
          ${selectedCandidate.isCurrent ? `
            <span style="font-size:var(--font-xs);color:#34d399;font-weight:bold;background:rgba(52,211,153,0.15);padding:3px 10px;border-radius:4px;border:1px solid #34d399;">✔ 運轉中</span>
          ` : ''}
        </div>

        <!-- 功法描述與口訣 -->
        <div style="background:#1e293b;border:1px solid #334155;border-radius:6px;padding:12px 14px;display:flex;flex-direction:column;gap:6px;">
          <div style="font-size:13px;font-weight:bold;color:#cbd5e1;">📜 功法要訣與效果：</div>
          <div style="font-size:13px;color:#cbd5e1;line-height:1.5;">${selectedCandidate.desc}</div>
        </div>

        <!-- 兵刃契合提示 (若為 STANCE) -->
        ${category === 'STANCE' ? `
          <div style="background:rgba(16,185,129,0.12);border:1px solid #10b981;border-radius:6px;padding:10px 14px;font-size:13px;color:#a7f3d0;line-height:1.4;">
            <span>✔ 當前手持兵刃【${weaponName}】，此套路已完全契合，啟用後自動取代普攻！</span>
          </div>
        ` : ''}

        <!-- 預設機制說明提示 -->
        <div style="background:rgba(15,23,42,0.8);border:1px dashed #475569;border-radius:6px;padding:10px 14px;font-size:var(--font-xs);color:#94a3b8;line-height:1.4;">
          <span>💡 提示：若未裝配任何進階功法，系統將自動以角色預設基礎武學（如基本招架、基本身法）運轉，畫面上顯示為「(無)」。</span>
        </div>

        <!-- 操作按鈕列 -->
        <div style="display:flex;gap:10px;margin-top:6px;">
          ${selectedCandidate.isCurrent ? `
            <button class="act-btn btn-red" onclick="window.confirmUnequipSkill(${memberIdx}, '${category}')" style="flex:1;font-size:14px;font-weight:bold;padding:10px 16px;">
              ✕ 卸下當前套路 (還原為預設)
            </button>
          ` : `
            <button class="act-btn btn-blue" onclick="window.confirmEquipSkill(${memberIdx}, '${selectedCandidate.id}', '${category}')" style="flex:1;font-size:14px;font-weight:bold;padding:10px 16px;">
              ✨ 即刻啟用此功法
            </button>
            ${curEquippedName ? `
              <button class="act-btn btn-red" onclick="window.confirmUnequipSkill(${memberIdx}, '${category}')" style="font-size:13px;padding:10px 14px;">
                ✕ 還原預設
              </button>
            ` : ''}
          `}
        </div>
      </div>
    `;
  }

  return `
    <div class="equip-diff-view">
      <!-- 左側候選列表 -->
      <div class="equip-candidate-list-col">
        <div style="font-size:14px;font-weight:bold;color:#e2e8f0;margin-bottom:10px;display:flex;justify-content:space-between;align-items:center;">
          <span>📖 可啟用的【${categoryLabel}】(${candidates.length})</span>
          <span style="font-size:13px;color:#94a3b8;">每頁上限 20 筆</span>
        </div>
        <div class="equip-candidate-grid">
          ${candidateListHtml}
        </div>
        ${renderPaginationBar(curPage, totalPages, candidates.length, 'window.changeSkillPickerPage')}
      </div>

      <!-- 右側詳情面板 -->
      <div class="equip-diff-panel-col">
        <div style="font-size:15px;font-weight:bold;color:#38bdf8;border-bottom:1px solid #334155;padding-bottom:8px;">
          ☯️ 功法詳情與運轉狀態
        </div>
        ${detailPanelHtml}
      </div>
    </div>
  `;
}

/**
 * 渲染單一成員的 WoW 經典修仙武學典籍 (Spellbook)
 * 陣法奧義已抽離至全隊陣法面板，此處專注於兵刃套路與門派絕技
 */
function renderMemberSpellbook(m, memberIdx) {
  if (!drpgState.spellbookState) {
    drpgState.spellbookState = {};
  }
  if (!drpgState.spellbookState[memberIdx]) {
    drpgState.spellbookState[memberIdx] = { tab: 'STANCES', page: 1 };
  }
  const curState = drpgState.spellbookState[memberIdx];
  if (curState.tab !== 'STANCES' && curState.tab !== 'SKILLS' && curState.tab !== 'PASSIVES') {
    curState.tab = 'STANCES';
  }
  const curTab = curState.tab || 'STANCES';
  let curPage = curState.page || 1;

  // 1. 整理各 Tab 項目 (兵刃套路、門派絕技、被動心法)
  const stances = m.availableStances || [];
  const skills = m.skills || [];
  const passives = m.availablePassives || [];

  const tabs = [
    { key: 'STANCES', label: '🗡️ 兵刃套路', count: stances.length > 0 ? stances.length : 1 },
    { key: 'SKILLS', label: '⚡ 門派絕技', count: skills.length },
    { key: 'PASSIVES', label: '🧘 被動心法', count: passives.length }
  ];

  let items = [];
  if (curTab === 'STANCES') {
    if (stances.length > 0) {
      items = stances.map(st => {
        let icon = '🗡️';
        const sId = (st.skillId || '').toLowerCase();
        if (sId.includes('blade')) icon = '⚔️';
        else if (sId.includes('axe')) icon = '🪓';
        else if (sId.includes('staff')) icon = '🦯';
        else if (sId.includes('hammer') || sId.includes('blunt')) icon = '🔨';
        else if (sId.includes('dagger')) icon = '⚡';
        else if (sId.includes('bow')) icon = '🏹';
        else if (sId.includes('fist') || sId.includes('unarmed')) icon = '👊';

        return {
          id: st.skillId,
          name: st.skillName,
          icon: icon,
          cost: '自動普攻',
          desc: st.description || '隨武器揮舞自動施展之套路招式。',
          isCurrent: !!st.isCurrentEnabled,
          canSwitch: !st.isCurrentEnabled
        };
      });
    } else {
      items.push({
        id: m.basicSkillId || 'basic_attack',
        name: m.basicSkillName || '基礎武學',
        icon: '🗡️',
        cost: '自動普攻',
        desc: '隨手施展之門派基礎套路。',
        isCurrent: true,
        canSwitch: false
      });
    }
  } else if (curTab === 'SKILLS') {
    if (skills.length > 0) {
      items = skills.map(s => ({
        id: s.id,
        name: s.name,
        icon: s.icon || '⚡',
        cost: s.costDescription || (s.costValue ? `${s.costValue} 消耗` : '無消耗'),
        desc: s.description || '引導煞氣或靈威爆發之奧義招式。',
        isCurrent: false,
        canSwitch: false
      }));
    } else {
      items.push({
        id: 'none',
        name: '暫無主動絕技',
        icon: '📜',
        cost: '專注平砍',
        desc: '該角色當前專注於武器套路平砍，未修習主動絕技。',
        isCurrent: false,
        canSwitch: false
      });
    }
  } else if (curTab === 'PASSIVES') {
    if (passives.length > 0) {
      items = passives.map(p => {
        let icon = '🧘';
        if (p.category === 'DODGE') icon = '💨';
        else if (p.category === 'PARRY') icon = '🛡️';
        else if (p.category === 'FORCE') icon = '🟣';

        return {
          id: p.skillId,
          name: p.skillName,
          icon: icon,
          cost: p.categoryName || '心法',
          desc: p.description || '常駐運轉之防禦或內功心法。',
          isCurrent: !!p.isCurrentEnabled,
          canSwitch: !p.isCurrentEnabled,
          category: p.category
        };
      });
    } else {
      items.push({
        id: 'none',
        name: '暫無被動心法',
        icon: '📜',
        cost: '無心法',
        desc: '該角色尚未領悟輕功、招架或內功心法。',
        isCurrent: false,
        canSwitch: false
      });
    }
  }

  // 分頁計算 (每頁 4 個條目，2x2 雙欄卡片佈局)
  const pageSize = 4;
  const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
  if (curPage > totalPages) curPage = totalPages;
  curState.page = curPage;

  const startIdx = (curPage - 1) * pageSize;
  const pageItems = items.slice(startIdx, startIdx + pageSize);

  // 雙欄網格條目 HTML
  const spellsGridHtml = pageItems.map(it => {
    const activeClass = it.isCurrent ? 'active-spell' : '';
    let actBtnHtml = '';
    if (it.isCurrent) {
      actBtnHtml = '<span class="spell-active-badge">✔ 參悟運轉中</span>';
    } else if (it.canSwitch) {
      const switchLabel = (curTab === 'PASSIVES') ? '⚡ 裝配心法' : '⚡ 啟用套路';
      const switchTitle = (curTab === 'PASSIVES') ? '裝配至常駐被動心法槽位' : '啟用為當前主力普攻套路';
      actBtnHtml = `<button class="spell-switch-btn" onclick="send('party enable ${memberIdx} ${it.id}')" title="${switchTitle}">${switchLabel}</button>`;
    } else if (curTab === 'SKILLS') {
      actBtnHtml = '<span style="font-size:var(--font-xs);color:#60a5fa;">戰鬥快捷施展</span>';
    }

    return `
      <div class="spell-card ${activeClass}" title="${it.desc}">
        <div class="spell-icon-box">${it.icon}</div>
        <div class="spell-info">
          <div class="spell-name-row">
            <span class="spell-name">${it.name}</span>
            <span class="spell-cost-badge">${it.cost}</span>
          </div>
          <div class="spell-desc">${it.desc}</div>
          <div class="spell-action-row">
            ${actBtnHtml}
          </div>
        </div>
      </div>
    `;
  }).join('');

  // 右側垂直 Tab 標籤列
  const tabsHtml = tabs.map(t => {
    const isSelected = (curTab === t.key);
    return `
      <button class="spellbook-tab-btn ${isSelected ? 'active' : ''}"
        onclick="switchSpellbookTab(${memberIdx}, '${t.key}')" title="${t.label}">
        ${t.label} (${t.count})
      </button>
    `;
  }).join('');

  let passiveSlotsBanner = '';
  if (curTab === 'PASSIVES') {
    const dodgeName = passives.find(p => p.category === 'DODGE' && p.isCurrentEnabled)?.skillName || m.passiveSlots?.DODGE || '未裝配';
    const parryName = passives.find(p => p.category === 'PARRY' && p.isCurrentEnabled)?.skillName || m.passiveSlots?.PARRY || '未裝配';
    const forceName = passives.find(p => p.category === 'FORCE' && p.isCurrentEnabled)?.skillName || m.passiveSlots?.FORCE || '未裝配';

    passiveSlotsBanner = `
      <div style="margin-bottom:10px;padding:8px 12px;background:rgba(15,23,42,0.7);border:1px solid rgba(148,163,184,0.25);border-radius:6px;display:flex;gap:12px;font-size:var(--font-xs);justify-content:space-around;">
        <span style="color:#38bdf8;">💨 <strong>輕功身法</strong>：${dodgeName}</span>
        <span style="color:#fbbf24;">🛡️ <strong>招架護體</strong>：${parryName}</span>
        <span style="color:#c084fc;">🟣 <strong>內功真元</strong>：${forceName}</span>
      </div>
    `;
  }

  return `
    <div class="wow-spellbook-container">
      <div class="wow-spellbook-header">
        <span class="wow-spellbook-title">📖 修仙武學典籍・道種法術書 (Spellbook)</span>
        <span style="font-size:var(--font-xs);color:#94a3b8;">${tabs.find(t=>t.key===curTab)?.label || ''}</span>
      </div>
      <div class="wow-spellbook-layout">
        <div class="wow-spellbook-page">
          ${passiveSlotsBanner}
          <div class="spell-grid">
            ${spellsGridHtml}
          </div>
          <div class="spellbook-pagination">
            <button class="spellbook-page-btn" onclick="switchSpellbookPage(${memberIdx}, -1)" ${curPage <= 1 ? 'disabled' : ''}>◀ 上一頁</button>
            <span>第 ${curPage} 頁 / 共 ${totalPages} 頁 (共 ${items.length} 條目)</span>
            <button class="spellbook-page-btn" onclick="switchSpellbookPage(${memberIdx}, 1)" ${curPage >= totalPages ? 'disabled' : ''}>下一頁 ▶</button>
          </div>
        </div>
        <div class="wow-spellbook-tabs">
          ${tabsHtml}
        </div>
      </div>
      <div style="margin-top:12px;padding:8px 14px;background:rgba(99,102,241,0.15);border:1px solid rgba(99,102,241,0.3);border-radius:6px;display:flex;align-items:center;justify-content:space-between;font-size:var(--font-xs);color:#c7d2fe;">
        <span>☯️ <strong>全隊陣法奧義</strong>屬於全隊共有，不局限於個人武學。請前往全隊陣法面板進行切換與站位調配。</span>
        <button class="act-btn btn-sm" onclick="selectPartyFormationTab()" style="padding:3px 10px;font-size:var(--font-xs);background:#4f46e5;color:#fff;">前往全隊陣法 ➔</button>
      </div>
    </div>
  `;
}

function switchSpellbookTab(memberIdx, tab) {
  if (!drpgState.spellbookState) drpgState.spellbookState = {};
  drpgState.spellbookState[memberIdx] = { tab: tab, page: 1 };
  renderPartyModal();
}

function switchSpellbookPage(memberIdx, delta) {
  if (!drpgState.spellbookState) drpgState.spellbookState = {};
  if (!drpgState.spellbookState[memberIdx]) drpgState.spellbookState[memberIdx] = { tab: 'STANCES', page: 1 };
  drpgState.spellbookState[memberIdx].page = Math.max(1, (drpgState.spellbookState[memberIdx].page || 1) + delta);
  renderPartyModal();
}


// 相容掛載至 window 供 HTML 內聯事件呼叫
if (typeof window !== 'undefined') {
  window.triggerPartyAction = triggerPartyAction;
  window.togglePartyModal = togglePartyModal;
  window.openPartyModal = openPartyModal;
  window.closePartyModal = closePartyModal;
  window.switchMainMenuTab = switchMainMenuTab;
  window.openMainMenu = openMainMenu;
  window.selectPartyModalMember = selectPartyModalMember;
  window.selectPartyFormationTab = selectPartyFormationTab;
  window.switchPartyModalSubTab = switchPartyModalSubTab;
  window.toggleAddTacticsForm = toggleAddTacticsForm;
  window.handleTacticsCondChange = handleTacticsCondChange;
  window.submitAddTactics = submitAddTactics;
  window.renderMemberTactics = renderMemberTactics;
  window.renderTeamFormationView = renderTeamFormationView;
  window.renderPartyModal = renderPartyModal;
  window.renderMemberSpellbook = renderMemberSpellbook;
  window.renderMemberSkillsView = renderMemberSkillsView;
  window.switchSpellbookTab = switchSpellbookTab;
  window.switchSpellbookPage = switchSpellbookPage;
  window.toggleFormationViewMode = toggleFormationViewMode;
  window.setFormationFilterSize = setFormationFilterSize;
  window.changeFormationPage = changeFormationPage;
  window.changeItemsPage = changeItemsPage;
  window.setItemsSecondaryFilter = setItemsSecondaryFilter;
  window.changeSkillsPage = changeSkillsPage;
  window.setSkillsTab = setSkillsTab;
  window.openEquipPicker = openEquipPicker;
  window.closeEquipPicker = closeEquipPicker;
  window.selectEquipPickerItem = selectEquipPickerItem;
  window.changeEquipPickerPage = changeEquipPickerPage;
  window.confirmEquipItem = confirmEquipItem;
  window.confirmUnequipItem = confirmUnequipItem;
  window.renderTeamEquipmentOverview = renderTeamEquipmentOverview;
  window.renderEquipmentDiffPickerView = renderEquipmentDiffPickerView;
  window.openSkillPicker = openSkillPicker;
  window.closeSkillPicker = closeSkillPicker;
  window.selectSkillPickerItem = selectSkillPickerItem;
  window.changeSkillPickerPage = changeSkillPickerPage;
  window.confirmEquipSkill = confirmEquipSkill;
  window.confirmUnequipSkill = confirmUnequipSkill;
  window.renderSkillPickerView = renderSkillPickerView;
}

export {
  triggerPartyAction,
  togglePartyModal,
  openPartyModal,
  closePartyModal,
  switchMainMenuTab,
  openMainMenu,
  selectPartyModalMember,
  selectPartyFormationTab,
  switchPartyModalSubTab,
  toggleAddTacticsForm,
  handleTacticsCondChange,
  submitAddTactics,
  renderMemberTactics,
  renderTeamFormationView,
  renderPartyModal,
  renderMemberSpellbook,
  renderMemberSkillsView,
  switchSpellbookTab,
  switchSpellbookPage,
  toggleFormationViewMode,
  setFormationFilterSize,
  changeFormationPage,
  changeItemsPage,
  setItemsSecondaryFilter,
  changeSkillsPage,
  setSkillsTab,
  openEquipPicker,
  closeEquipPicker,
  selectEquipPickerItem,
  changeEquipPickerPage,
  confirmEquipItem,
  confirmUnequipItem,
  renderTeamEquipmentOverview,
  renderEquipmentDiffPickerView,
  openSkillPicker,
  closeSkillPicker,
  selectSkillPickerItem,
  changeSkillPickerPage,
  confirmEquipSkill,
  confirmUnequipSkill,
  renderSkillPickerView
};
