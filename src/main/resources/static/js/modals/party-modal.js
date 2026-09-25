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
          <label style="font-size:12px;color:#94a3b8;">優先級：</label>
          <input type="number" id="t-builder-prio" value="${nextPriority}" min="1" max="99" style="width:55px;" />

          <label style="font-size:12px;color:#94a3b8;">觸發條件：</label>
          <select id="t-builder-cond" onchange="handleTacticsCondChange()">
            <option value="ALLY_HP_LESS_THAN">隊友氣血低於 (%)</option>
            <option value="SELF_HP_LESS_THAN">自身氣血低於 (%)</option>
            <option value="RESOURCE_GTE">自身資源 >= (點/怒氣/連擊)</option>
            <option value="ENEMY_COUNT_GTE">敵方存活數量 >= (體)</option>
            <option value="ENEMY_IS_BOSS">敵方存在首領 (Boss)</option>
            <option value="ALWAYS">無條件施展 (必定觸發)</option>
          </select>

          <label id="t-builder-val-label" style="font-size:12px;color:#94a3b8;">閥值：</label>
          <input type="number" id="t-builder-val" value="50" min="0" max="9999" style="width:65px;" />
        </div>
        <div class="tactics-builder-row">
          <label style="font-size:12px;color:#94a3b8;">目標：</label>
          <select id="t-builder-target">
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

          <label style="font-size:12px;color:#94a3b8;">執行武學：</label>
          <select id="t-builder-skill">
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
        <div style="border:1px dashed #263346;border-radius:6px;min-height:68px;display:flex;align-items:center;justify-content:center;color:#475569;font-size:11px;background:rgba(15,23,42,0.3);">
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
          <span style="font-weight:bold;font-size:12px;color:${isAlive ? '#f1f5f9' : '#94a3b8'};overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="#${idx + 1} ${mem.name} (${mem.className || '道友'})">
            #${idx + 1} ${mem.name}
          </span>
          <span style="font-size:10px;color:${rowColor};font-weight:bold;flex-shrink:0;">[${rowLabel}]</span>
        </div>
        <div style="font-size:10px;line-height:1.2;margin:3px 0;color:${!isAlive ? '#ef4444' : (slotActive ? '#6ee7b7' : '#facc15')};overflow:hidden;text-overflow:ellipsis;white-space:nowrap;" title="${mem.formationSlotBonus || mem.formationSlotName || '自由衛位'}">
          ${!isAlive ? '💀 陣亡離陣' : (mem.formationSlotBonus ? `💠 ${mem.formationSlotBonus}` : (mem.formationSlotName || '自由策應'))}
        </div>
        <div style="display:flex;justify-content:space-between;align-items:center;margin-top:2px;">
          <button class="act-btn btn-sm" onclick="send('formation switch ${idx}')" title="循環切換至${nextRow}" style="padding:1px 6px;font-size:10px;background:#1e293b;color:#cbd5e1;border:1px solid #475569;border-radius:3px;">
            🔄 調至${nextRow}
          </button>
          ${!isAlive ? '<span style="font-size:9px;color:#ef4444;font-weight:bold;">離陣</span>' : (slotActive ? '<span style="font-size:9px;color:#34d399;">✓生效</span>' : '<span style="font-size:9px;color:#facc15;">⚠未配</span>')}
        </div>
      </div>
    `;
  };

  return `
    <div class="team-formation-container" style="display:flex;flex-direction:column;gap:14px;">
      <!-- 1. 當前陣法光環與典籍庫切換條 -->
      <div style="background:rgba(30,27,75,0.7);border:1px solid ${isBroken ? '#ef4444' : '#6366f1'};border-radius:6px;padding:10px 14px;display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px;">
        <div style="display:flex;align-items:center;gap:10px;font-size:12px;">
          <span style="font-weight:bold;color:${isBroken ? '#f87171' : '#c084fc'};font-size:15px;">☯️ 【${party.formationName || '五行混元陣'}】</span>
          ${isBroken
            ? '<span style="font-size:11px;background:#991b1b;color:#fecaca;padding:2px 8px;border-radius:4px;font-weight:bold;">⚠️ 陣法崩解失效</span>'
            : '<span style="font-size:11px;background:#4338ca;color:#e0e7ff;padding:2px 8px;border-radius:4px;font-weight:bold;">常駐運轉中</span>'}
          <span style="color:#cbd5e1;font-size:11px;">
            ${isBroken
              ? '<span style="color:#f87171;font-weight:bold;">因人員陣亡導致人數不符，請點擊右側「📚 切換陣法」選擇適配存活人數的陣法！</span>'
              : '全隊受陣形靈威加持，陣眼與孔位契合時可爆發專屬屬性增幅。'}
          </span>
        </div>
        <button class="act-btn" onclick="window.toggleFormationViewMode('LIBRARY')" style="background:#0284c7;color:#fff;padding:6px 14px;font-size:12px;border:none;border-radius:4px;cursor:pointer;font-weight:bold;box-shadow:0 2px 6px rgba(2,132,199,0.4);">
          📚 切換陣法 (典籍庫)
        </button>
      </div>

      <!-- 2. 浪漫沙加式 5×3 戰術站位陣盤 -->
      <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;">
        <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px;">
          <div>
            <div style="font-size:15px;font-weight:bold;color:#e2e8f0;">🛡️ 隊伍戰鬥站位編排 (浪漫沙加式 5×3 戰陣盤)</div>
            <div style="font-size:11px;color:#94a3b8;margin-top:2px;">依陣法 (X, Y) 幾何座標精準排布。點擊「🔄 調位」可循環切換【前衛 ➜ 中衛 ➜ 後衛】。</div>
          </div>
          <div style="font-size:11px;color:#cbd5e1;background:#0f172a;padding:4px 10px;border-radius:4px;">
            總人數：${(party.members || []).length} / 5 人
          </div>
        </div>

        <div style="display:flex;flex-direction:column;gap:12px;">
          ${rowMeta.map((row, rIdx) => `
            <div>
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;">
                <span style="font-size:12px;font-weight:bold;color:${row.color};">${row.label}</span>
                <span style="font-size:10px;color:#94a3b8;">${row.desc}</span>
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
          <span style="font-size:12px;font-weight:bold;color:#ef4444;">💀 陣亡重傷待援：</span>
          <span style="font-size:11px;color:#fca5a5;">
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
  const PAGE_SIZE = 6;

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
            <span style="font-size:11px;color:#94a3b8;font-weight:normal;">- 典藏太古修仙陣圖與凡世軍道殺陣</span>
          </div>
          <div style="font-size:11px;color:#94a3b8;margin-top:2px;">
            當前出戰人數：${(party.members || []).length} 人 | 存活人數：${(party.members || []).filter(m => (m.alive !== undefined ? m.alive : m.hp > 0)).length} 人
          </div>
        </div>
        <button class="act-btn" onclick="window.toggleFormationViewMode('TACTICAL')" style="background:#0284c7;color:#fff;padding:6px 14px;font-size:12px;border:none;border-radius:4px;cursor:pointer;font-weight:bold;">
          🛡️ 返回 5×3 戰陣盤
        </button>
      </div>

      <!-- 人數分類頁籤 (2 / 3 / 4 / 5 人分類) -->
      <div style="display:flex;gap:8px;border-bottom:1px solid #334155;padding-bottom:8px;flex-wrap:wrap;">
        <button class="act-btn btn-sm ${currentFilter === 'ALL' ? 'active' : ''}" onclick="window.setFormationFilterSize('ALL')" style="padding:4px 12px;font-size:12px;background:${currentFilter === 'ALL' ? '#38bdf8' : '#1e293b'};color:${currentFilter === 'ALL' ? '#0f172a' : '#cbd5e1'};border:1px solid #475569;font-weight:bold;border-radius:4px;">
          全部 (${countAll})
        </button>
        <button class="act-btn btn-sm ${currentFilter === 5 ? 'active' : ''}" onclick="window.setFormationFilterSize(5)" style="padding:4px 12px;font-size:12px;background:${currentFilter === 5 ? '#38bdf8' : '#1e293b'};color:${currentFilter === 5 ? '#0f172a' : '#cbd5e1'};border:1px solid #475569;font-weight:bold;border-radius:4px;">
          5人陣法 (${count5})
        </button>
        <button class="act-btn btn-sm ${currentFilter === 4 ? 'active' : ''}" onclick="window.setFormationFilterSize(4)" style="padding:4px 12px;font-size:12px;background:${currentFilter === 4 ? '#38bdf8' : '#1e293b'};color:${currentFilter === 4 ? '#0f172a' : '#cbd5e1'};border:1px solid #475569;font-weight:bold;border-radius:4px;">
          4人陣法 (${count4})
        </button>
        <button class="act-btn btn-sm ${currentFilter === 3 ? 'active' : ''}" onclick="window.setFormationFilterSize(3)" style="padding:4px 12px;font-size:12px;background:${currentFilter === 3 ? '#38bdf8' : '#1e293b'};color:${currentFilter === 3 ? '#0f172a' : '#cbd5e1'};border:1px solid #475569;font-weight:bold;border-radius:4px;">
          3人陣法 (${count3})
        </button>
        <button class="act-btn btn-sm ${currentFilter === 2 ? 'active' : ''}" onclick="window.setFormationFilterSize(2)" style="padding:4px 12px;font-size:12px;background:${currentFilter === 2 ? '#38bdf8' : '#1e293b'};color:${currentFilter === 2 ? '#0f172a' : '#cbd5e1'};border:1px solid #475569;font-weight:bold;border-radius:4px;">
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
            actionBtn = '<span style="font-size:11px;color:#f87171;font-weight:bold;background:rgba(239,68,68,0.2);padding:2px 8px;border-radius:4px;border:1px solid #ef4444;">⚠️ 陣法已崩解</span>';
          } else if (isCurrent) {
            actionBtn = '<span style="font-size:11px;color:#34d399;font-weight:bold;background:rgba(52,211,153,0.15);padding:2px 8px;border-radius:4px;border:1px solid #34d399;">✔ 當前運轉中</span>';
          } else if (isSelectable) {
            actionBtn = `<button class="act-btn btn-sm" onclick="send('formation equip ${f.id}')" style="padding:4px 12px;font-size:11px;background:#0284c7;color:#fff;font-weight:bold;">結成此陣</button>`;
          } else {
            actionBtn = `<span style="font-size:11px;color:#ef4444;background:rgba(239,68,68,0.15);border:1px solid #ef4444;padding:2px 8px;border-radius:4px;" title="${f.lockReason || '隊伍條件不符'}">🔒 ${f.lockReason || '不可選'}</span>`;
          }

          const typeBadge = f.basic
            ? '<span style="font-size:10px;background:#059669;color:#ecfdf5;padding:1px 6px;border-radius:3px;">基本陣法</span>'
            : '<span style="font-size:10px;background:#7c3aed;color:#ede9fe;padding:1px 6px;border-radius:3px;">進階陣法</span>';

          const sizeBadge = `<span style="font-size:10px;background:#334155;color:#cbd5e1;padding:1px 6px;border-radius:3px;">${f.requiredPartySize}人陣</span>`;

          const reqClassesHtml = (f.requiredClasses && f.requiredClasses.length > 0)
            ? `<div style="font-size:11px;color:#fbbf24;display:flex;align-items:center;gap:4px;">
                 <span>⚠️ 需職業：</span>
                 <span>${f.requiredClasses.map(formatFormationClassName).join('、')}</span>
               </div>`
            : '';

          const ultHtml = f.ultimateSkillName
            ? `<div style="font-size:11px;color:#facc15;">⚡ 專屬奧義：【${f.ultimateSkillName}】</div>`
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
                  <div style="font-size:11px;color:#cbd5e1;line-height:1.4;">${f.description || ''}</div>
                  ${f.passiveAura ? `<div style="font-size:11px;color:#6ee7b7;line-height:1.3;">${f.passiveAura}</div>` : ''}
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
        <span style="font-size:12px;color:#94a3b8;">
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

function renderPartyModal() {
  const modal = document.getElementById('party-modal');
  if (!modal || modal.classList.contains('hidden')) return;

  const party = drpgState.lastParty;
  const formInfoEl = document.getElementById('party-modal-formation-info');
  const memberTabsEl = document.getElementById('party-modal-member-tabs');
  const subTabsEl = document.getElementById('party-modal-sub-tabs');
  const membersListEl = document.getElementById('party-modal-members-list');

  if (!party || !party.members || party.members.length === 0) {
    if (membersListEl) membersListEl.innerHTML = '<div style="color:#94a3b8;padding:20px;text-align:center;">尚未載入小隊資料，請稍候...</div>';
    return;
  }

  // 1. 確保選中狀態正常
  const isFormationMode = (drpgState.selectedModalMemberIdx === 'FORMATION');
  if (!isFormationMode) {
    if (typeof drpgState.selectedModalMemberIdx !== 'number' || drpgState.selectedModalMemberIdx >= party.members.length || drpgState.selectedModalMemberIdx < 0) {
      drpgState.selectedModalMemberIdx = 0;
    }
  }
  if (!drpgState.partyModalSubTab) {
    drpgState.partyModalSubTab = 'EQUIP';
  }

  const selIdx = drpgState.selectedModalMemberIdx;
  const m = (!isFormationMode && party.members[selIdx]) ? party.members[selIdx] : party.members[0];

  // 2. 渲染陣法與靈威狀態
  if (formInfoEl) {
    const ultBtn = party.canCastUltimate
      ? `<button class="act-btn btn-ult" onclick="send('formation cast')" style="padding:2px 8px;font-size:11px;">⚡ 施展陣法奧義【${party.ultimateSkillName}】</button>`
      : `<span style="color:#94a3b8;font-size:11px;">奧義【${party.ultimateSkillName || '無'}】(充能 ${party.formationEnergy || 0}/100)</span>`;

    formInfoEl.innerHTML = `
      <div style="display:flex;align-items:center;gap:10px;">
        <span>☯️ 當前道門陣法：<strong style="color:#38bdf8;">${party.formationName || '四象辟邪陣'}</strong></span>
        <button class="act-btn" onclick="send('formation toggle')" style="padding:2px 8px;font-size:11px;">切換陣法 (F)</button>
      </div>
      <div style="display:flex;align-items:center;gap:10px;">
        <span>⚡ 靈威：${party.formationEnergy || 0}/100</span>
        ${ultBtn}
      </div>
    `;
  }

  // 3. 渲染頂部隊員切換頁籤列
  if (memberTabsEl) {
    const memberBtns = party.members.map((mem, idx) => {
      const isSel = (!isFormationMode && selIdx === idx);
      const isAlive = (mem.alive !== undefined) ? mem.alive : (mem.hp > 0);
      const rowBadge = mem.row === 'FRONT' ? '前衛' : '後衛';
      const hpPct = Math.min(100, Math.max(0, (mem.hp / mem.maxHp) * 100));
      return `
        <button class="party-member-tab-btn ${isSel ? 'active' : ''}" type="button" onclick="selectPartyModalMember(${idx})" title="#${idx + 1} ${mem.name} (${mem.className || '道友'}) - ${mem.row === 'FRONT' ? '前衛' : (mem.row === 'MIDDLE' ? '中衛' : '後衛')}">
          <span style="font-weight:bold;">#${idx + 1} ${mem.name}</span>
          ${(idx === 0 && mem.freeStatPoints > 0) ? `<span class="hud-free-points-pill" style="margin-left:2px; font-size:9px; padding:0 3px;">+${mem.freeStatPoints}</span>` : ''}
        </button>
      `;
    }).join('');

    const formationBtn = `
      <button class="party-member-tab-btn ${isFormationMode ? 'active' : ''}" type="button" onclick="selectPartyFormationTab()" style="${isFormationMode ? 'border-color:#a855f7; box-shadow:0 -2px 10px rgba(168,85,247,0.3);' : 'border-left:2px solid #a855f7;'}" title="查看與調配全隊陣法站位">
        <span style="font-weight:bold; color:#c084fc;">☯️ 全隊陣法</span>
      </button>
    `;

    memberTabsEl.innerHTML = memberBtns + formationBtn;
  }

  // 4. 若為全隊陣法模式，直接渲染陣法奧義視圖
  if (isFormationMode) {
    if (subTabsEl) subTabsEl.style.display = 'none';
    if (membersListEl) {
      membersListEl.innerHTML = renderTeamFormationView(party);
    }
    return;
  }

  if (subTabsEl) subTabsEl.style.display = 'flex';

  // 5. 渲染子分頁切換列 (屬性裝備 / 武學法術 / 戰術方針)
  if (subTabsEl) {
    const activeSub = drpgState.partyModalSubTab;
    const tacticsCount = (m.tactics && m.tactics.length > 0) ? `(${m.tactics.length})` : '';
    subTabsEl.innerHTML = `
      <button class="party-sub-tab-btn ${activeSub === 'EQUIP' ? 'active' : ''}" type="button" onclick="switchPartyModalSubTab('EQUIP')">
        🛡️ 屬性與裝備
      </button>
      <button class="party-sub-tab-btn ${activeSub === 'SPELLBOOK' ? 'active' : ''}" type="button" onclick="switchPartyModalSubTab('SPELLBOOK')">
        📖 武學法術
      </button>
      <button class="party-sub-tab-btn ${activeSub === 'TACTICS' ? 'active' : ''}" type="button" onclick="switchPartyModalSubTab('TACTICS')">
        🎯 戰術方針 (Gambit AI) ${tacticsCount}
      </button>
    `;
  }

  if (!membersListEl) return;
  membersListEl.innerHTML = '';

  // 6. 依子頁籤渲染內容
  if (drpgState.partyModalSubTab === 'SPELLBOOK') {
    membersListEl.innerHTML = renderMemberSpellbook(m, selIdx);
    return;
  }

  if (drpgState.partyModalSubTab === 'TACTICS') {
    membersListEl.innerHTML = renderMemberTactics(m, selIdx);
    return;
  }

  // 預設 'EQUIP' 屬性與裝備
  const allSlots = [
    { key: 'MAIN_HAND', alias: 'weapon', label: '主手武器', icon: '🗡️' },
    { key: 'OFF_HAND', alias: 'shield', label: '副手防具', icon: '🛡️' },
    { key: 'HEAD', alias: 'head', label: '頭部盔甲', icon: '👑' },
    { key: 'BODY', alias: 'armor', label: '身軀道袍', icon: '🥋' },
    { key: 'FEET', alias: 'feet', label: '靴履護具', icon: '👢' },
    { key: 'ACCESSORY_1', alias: 'acc1', label: '本命法寶', icon: '💍' },
    { key: 'ACCESSORY_2', alias: 'acc2', label: '輔佐靈寶', icon: '📿' }
  ];

  const rowBadge = m.row === 'FRONT' ? '前衛' : '後衛';
  const rowClass = `badge-${m.row.toLowerCase()}`;

  const resType = m.resourceType || 'MP';
  const curRes = m.currentResource !== undefined ? m.currentResource : m.mp;
  const maxRes = m.maxResource !== undefined ? m.maxResource : m.maxMp;
  let resLabel = `MP: ${curRes}/${maxRes}`;
  if (resType === 'SP' || resType === 'RAGE' || resType === 'COMBO' || resType === 'STAMINA' || resType === 'FORCE' || resType === 'ENERGY') {
    resLabel = `戰氣: ${curRes}/${maxRes}`;
  }

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
        <div class="equip-slot-box has-item" title="${item.description || ''}">
          <div class="equip-slot-title">
            <span>${slot.icon} ${slot.label}</span>
            <button class="unequip-mini-btn" onclick="send('item unequip ${slot.alias} ${selIdx}')" title="卸下放回行囊">✕ 卸下</button>
          </div>
          <div class="equip-slot-name">${item.icon || '📦'} ${item.name}</div>
          <div class="equip-slot-stats">${statsStr}</div>
        </div>
      `;
    } else {
      equipSlotsHtml += `
        <div class="equip-slot-box empty">
          <div class="equip-slot-title">
            <span>${slot.icon} ${slot.label}</span>
          </div>
          <div class="equip-slot-name" style="color:#64748b;font-weight:normal;">(未穿戴)</div>
          <div class="equip-slot-act">
            <button class="item-act-mini-btn" onclick="toggleBagDrawer(true)" title="開啟行囊挑選裝備穿戴">🎒 挑選</button>
          </div>
        </div>
      `;
    }
  }

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
        <div class="free-points-banner">
          <div style="display:flex;align-items:center;gap:6px;">
            <span>⭐</span>
            <span><strong>道胎未定・造化充盈</strong>：尚有 <strong style="font-size:15px;color:#fde047;">${freePoints}</strong> 點自由修為點數！</span>
          </div>
          <span style="font-size:11px;color:#fef3c7;">(點擊下方屬性右側 [+1] 按鈕即刻分配)</span>
        </div>
      `;
    } else {
      freePointsBannerHtml = `
        <div style="font-size:11px;color:#94a3b8;display:flex;justify-content:space-between;padding:2px 4px;">
          <span>⭐ 自由修為點數：0 點</span>
          <span style="color:#64748b;">(主角每升一級額外獲贈 2 點自由分配點數)</span>
        </div>
      `;
    }
  } else {
    freePointsBannerHtml = `
      <div style="font-size:11px;color:#94a3b8;display:flex;justify-content:space-between;padding:2px 4px;">
        <span>🏷️ 成長模式：【職業範本自適應】</span>
        <span style="color:#64748b;">(同伴升級自動提升五維，無需手動微操)</span>
      </div>
    `;
  }

  const card = document.createElement('div');
  card.className = 'party-detail-card';
  card.innerHTML = `
    <div class="party-detail-header">
      <div class="party-detail-name-wrap">
        <span style="color:#38bdf8;font-weight:bold;font-size:16px;">#${selIdx + 1} ${m.name}</span>
        <span class="member-level-badge" style="font-size:11px;background:#0f172a;border:1px solid #eab308;padding:1px 6px;border-radius:4px;">Lv.${level}</span>
        <span class="member-row ${rowClass}">[${rowBadge}]</span>
        ${m.className ? `<span class="member-class-badge" title="${m.classDescription || ''}" style="background:#1e293b;border:1px solid #38bdf8;color:#7dd3fc;padding:2px 8px;border-radius:4px;font-size:11px;">🏷️ ${m.className}</span>` : ''}
        <button class="item-act-mini-btn" onclick="send('party switch ${selIdx}')" title="切換前排/後排站位" style="font-size:11px;">🔄 站位切換 (${rowBadge})</button>
      </div>
      <span class="party-detail-role" style="font-size:12px;color:#94a3b8;">${m.roleTitle}</span>
    </div>

    <!-- 1. 修為境界與 EXP 進度條 -->
    <div class="party-detail-exp-card">
      <div class="party-detail-exp-header">
        <div class="party-detail-exp-title">
          <span>✨ 境界修為</span>
          <strong style="color:#fde047;">Lv.${level}</strong>
        </div>
        <div class="party-detail-exp-val">EXP: ${exp} / ${nextExp} (${expPct}%)</div>
      </div>
      <div class="party-detail-exp-bar" title="晉升下一級所需修為：${exp}/${nextExp} (${expPct}%)">
        <div class="party-detail-exp-fill" style="width:${expPct}%;"></div>
      </div>
    </div>

    <!-- 2. 自由點數提示橫幅 -->
    ${freePointsBannerHtml}

    <!-- 3. 五維先天道基屬性網格 -->
    <div style="font-size:13px;color:#cbd5e1;font-weight:bold;margin-top:2px;display:flex;justify-content:space-between;align-items:center;">
      <span>☯️ 五維先天道基：</span>
      ${isLeader && freePoints > 0 ? `<span style="font-size:11px;color:#facc15;">請點擊右側 [+1] 分配點數</span>` : ''}
    </div>
    <div class="party-detail-stats-grid">
      <div class="stat-item-box">
        <div class="stat-item-header">
          <span class="stat-item-label">💪 力量 STR</span>
          ${renderStatAddBtn('str', '力量')}
        </div>
        <div class="stat-item-val">${strVal}</div>
        <div class="stat-item-desc">物理傷害、負重、招架</div>
      </div>

      <div class="stat-item-box">
        <div class="stat-item-header">
          <span class="stat-item-label">🫀 根骨 CON</span>
          ${renderStatAddBtn('con', '根骨')}
        </div>
        <div class="stat-item-val">${conVal}</div>
        <div class="stat-item-desc">血量上限 (+10/點)、減傷</div>
      </div>

      <div class="stat-item-box">
        <div class="stat-item-header">
          <span class="stat-item-label">⚡ 靈巧 DEX</span>
          ${renderStatAddBtn('dex', '靈巧')}
        </div>
        <div class="stat-item-val">${dexVal}</div>
        <div class="stat-item-desc">暴擊率、命中、身法閃避</div>
      </div>

      <div class="stat-item-box">
        <div class="stat-item-header">
          <span class="stat-item-label">🧠 悟性 INT</span>
          ${renderStatAddBtn('int', '悟性')}
        </div>
        <div class="stat-item-val">${intVal}</div>
        <div class="stat-item-desc">真元上限 (+8/點)、法術</div>
      </div>

      <div class="stat-item-box">
        <div class="stat-item-header">
          <span class="stat-item-label">🧘 定力 WIS</span>
          ${renderStatAddBtn('wis', '定力')}
        </div>
        <div class="stat-item-val">${wisVal}</div>
        <div class="stat-item-desc">治療增幅、法力恢復、抗性</div>
      </div>
    </div>

    <!-- 4. 當前實時動態條 (HP / MP / SAN) -->
    <div class="party-detail-bars" style="background:rgba(15,23,42,0.6);padding:10px;border-radius:6px;">
      <div style="font-size:12px;color:#f87171;font-weight:bold;">❤️ 氣血 HP: ${m.hp}/${m.maxHp}</div>
      <div style="font-size:12px;color:#60a5fa;font-weight:bold;">⚡ ${resLabel}</div>
      <div style="font-size:12px;color:#34d399;font-weight:bold;">🧘 道心 SAN: ${m.san}/${m.maxSan} (${m.sanityStatus || '心境平穩'})</div>
    </div>

    <!-- 5. 裝備槽位 (5 基礎 + 2 飾品) -->
    <div style="font-size:13px;color:#cbd5e1;font-weight:bold;margin-top:4px;">🛡️ 裝備槽位 (5 基礎 + 2 飾品)：</div>
    <div class="party-detail-equip-grid">
      ${equipSlotsHtml}
    </div>
  `;
  membersListEl.appendChild(card);
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
      actBtnHtml = '<span style="font-size:9px;color:#60a5fa;">戰鬥快捷施展</span>';
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
      <div style="margin-bottom:10px;padding:8px 12px;background:rgba(15,23,42,0.7);border:1px solid rgba(148,163,184,0.25);border-radius:6px;display:flex;gap:12px;font-size:11px;justify-content:space-around;">
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
        <span style="font-size:10px;color:#94a3b8;">${tabs.find(t=>t.key===curTab)?.label || ''}</span>
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
      <div style="margin-top:12px;padding:8px 14px;background:rgba(99,102,241,0.15);border:1px solid rgba(99,102,241,0.3);border-radius:6px;display:flex;align-items:center;justify-content:space-between;font-size:11px;color:#c7d2fe;">
        <span>☯️ <strong>全隊陣法奧義</strong>屬於全隊共有，不局限於個人武學。請前往全隊陣法面板進行切換與站位調配。</span>
        <button class="act-btn btn-sm" onclick="selectPartyFormationTab()" style="padding:3px 10px;font-size:11px;background:#4f46e5;color:#fff;">前往全隊陣法 ➔</button>
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
  window.switchSpellbookTab = switchSpellbookTab;
  window.switchSpellbookPage = switchSpellbookPage;
  window.toggleFormationViewMode = toggleFormationViewMode;
  window.setFormationFilterSize = setFormationFilterSize;
  window.changeFormationPage = changeFormationPage;
}

export {
  triggerPartyAction,
  togglePartyModal,
  openPartyModal,
  closePartyModal,
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
  switchSpellbookTab,
  switchSpellbookPage,
  toggleFormationViewMode,
  setFormationFilterSize,
  changeFormationPage
};
