import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';
import { escapeHtml } from '../core/ui-utils.js';

/**
 * 戰術方針子模組 (Party Tactics / Gambit AI Tab)
 * 負責渲染同伴 FFXII 式 Gambit 規則列表、條件判定與新增規則建構器
 */

const drpgState = store.getState();
const send = (cmd, silent) => sendCmd(cmd, silent);

function getRenderPartyModal() {
  return window.renderPartyModal;
}

export function toggleAddTacticsForm(show) {
  drpgState.isAddingTactics = (show !== undefined) ? !!show : !drpgState.isAddingTactics;
  const render = getRenderPartyModal();
  if (render) render();
}

export function handleTacticsCondChange() {
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

export function submitAddTactics(memberIdx) {
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
  const render = getRenderPartyModal();
  if (render) render();
}

export function renderMemberTactics(m, idx) {
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
        const role = pidx === 0 ? '👑 隊長' : (pm.className || '道友');
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
          <select id="t-builder-cond" onchange="window.handleTacticsCondChange()" style="font-size:13px;">
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
            <option value="FRONT_ROW_ALLY">前排隊友 (Tank)</option>
            <option value="LOWEST_HP_ALLY">氣血最低隊友</option>
            <option value="LEADER">小隊隊長</option>
            <option value="SELF">自身</option>
            ${memberOptionsHtml}
            <option value="BACK_ROW_ALLY">後排隊友</option>
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
          <button class="act-btn btn-green" type="button" onclick="window.submitAddTactics(${idx})">💾 確定新增規則</button>
          <button class="act-btn" type="button" onclick="window.toggleAddTacticsForm(false)">✕ 取消</button>
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
              <div class="tactics-prio-badge">#${escapeHtml(String(r.priority || ''))}</div>
              <div class="tactics-rule-desc">
                <span class="tactics-cond-tag">${escapeHtml(r.conditionLabel || '')}${escapeHtml(valDisplay)}</span>
                <span class="tactics-arrow">➜</span>
                <span class="tactics-target-tag">對 ${escapeHtml(r.targetLabel || '')}</span>
                <span class="tactics-arrow">➜</span>
                <span class="tactics-skill-tag">施展【${escapeHtml(r.skillName || '')}】</span>
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
          <button class="act-btn btn-blue btn-sm" type="button" onclick="window.toggleAddTacticsForm(true)">➕ 新增方針</button>
          <button class="act-btn btn-sm" type="button" onclick="send('party tactics ${idx} reset')">🔄 重置預設</button>
          <button class="act-btn btn-red btn-sm" type="button" onclick="send('party tactics ${idx} clear')">🗑️ 清空方針</button>
        </div>
      </div>
      ${builderHtml}
      ${rulesHtml}
    </div>
  `;
}
