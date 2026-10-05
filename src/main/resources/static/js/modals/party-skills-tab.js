import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';
import { getMemberSlotItem, getPassiveSkillDisplayName } from './party-equipment-tab.js';

/**
 * 武學法術與心法典籍子模組 (Party Skills & Spellbook Tab)
 * 負責渲染成員武學法術書 (Spellbook)、功法挑選器 (Skill Picker) 與技能清單 (Skills View)
 */

const drpgState = store.getState();
const send = (cmd, silent) => sendCmd(cmd, silent);

function getRenderPartyModal() {
  return window.renderPartyModal;
}

function getRenderPaginationBar() {
  return window.renderPaginationBar || ((cur, tot, cnt, fn) => '');
}

export function setSkillsTab(tab) {
  drpgState.skillsTab = tab;
  drpgState.skillsPage = 1;
  const render = getRenderPartyModal();
  if (render) render();
}

export function changeSkillsPage(delta) {
  drpgState.skillsPage = Math.max(1, (drpgState.skillsPage || 1) + delta);
  const render = getRenderPartyModal();
  if (render) render();
}

export function openSkillPicker(memberIdx, category, categoryLabel) {
  drpgState.skillPicker = {
    memberIdx,
    category,
    categoryLabel,
    selectedSkillId: null,
    page: 1
  };
  const render = getRenderPartyModal();
  if (render) render();
}

export function closeSkillPicker() {
  drpgState.skillPicker = null;
  const render = getRenderPartyModal();
  if (render) render();
}

export function selectSkillPickerItem(skillId) {
  if (drpgState.skillPicker) {
    drpgState.skillPicker.selectedSkillId = skillId;
    const render = getRenderPartyModal();
    if (render) render();
  }
}

export function changeSkillPickerPage(delta) {
  if (drpgState.skillPicker) {
    drpgState.skillPicker.page = Math.max(1, (drpgState.skillPicker.page || 1) + delta);
    const render = getRenderPartyModal();
    if (render) render();
  }
}

export function confirmEquipSkill(memberIdx, skillId, category) {
  if (!skillId) return;
  send(`party enable ${memberIdx} ${skillId} ${category || ''}`);
  drpgState.skillPicker = null;
}

export function confirmUnequipSkill(memberIdx, category) {
  send(`party enable ${memberIdx} none ${category || ''}`);
  drpgState.skillPicker = null;
}

/**
 * 渲染成員技能與道法典籍 (Skills View - 單頁上限 20 筆)
 */
export function renderMemberSkillsView(m, memberIdx, party, skillTab) {
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

  const paginationBar = getRenderPaginationBar()(curPage, totalPages, items.length, 'window.changeSkillsPage');

  return `
    <div style="background:#1e293b;border:1px solid #334155;border-radius:8px;padding:16px;flex:1;display:flex;flex-direction:column;">
      <div style="display:grid;grid-template-columns:repeat(auto-fill, minmax(460px, 1fr));gap:12px;flex:1;align-content:start;">
        ${gridHtml}
      </div>
      ${paginationBar}
    </div>
  `;
}

/**
 * 功法與套路挑選子視圖 (Skill Picker Sub-view)
 * 支援兵刃套路依手持武器類型動態過濾、心法分類過濾、20 筆單頁分頁與還原預設
 */
export function renderSkillPickerView(party) {
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

  const paginationBar = getRenderPaginationBar()(curPage, totalPages, candidates.length, 'window.changeSkillPickerPage');

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
        ${paginationBar}
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
export function renderMemberSpellbook(m, memberIdx) {
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
        onclick="window.switchSpellbookTab(${memberIdx}, '${t.key}')" title="${t.label}">
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
            <button class="spellbook-page-btn" onclick="window.switchSpellbookPage(${memberIdx}, -1)" ${curPage <= 1 ? 'disabled' : ''}>◀ 上一頁</button>
            <span>第 ${curPage} 頁 / 共 ${totalPages} 頁 (共 ${items.length} 條目)</span>
            <button class="spellbook-page-btn" onclick="window.switchSpellbookPage(${memberIdx}, 1)" ${curPage >= totalPages ? 'disabled' : ''}>下一頁 ▶</button>
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

export function switchSpellbookTab(memberIdx, tab) {
  if (!drpgState.spellbookState) drpgState.spellbookState = {};
  drpgState.spellbookState[memberIdx] = { tab: tab, page: 1 };
  const render = getRenderPartyModal();
  if (render) render();
}

export function switchSpellbookPage(memberIdx, delta) {
  if (!drpgState.spellbookState) drpgState.spellbookState = {};
  if (!drpgState.spellbookState[memberIdx]) drpgState.spellbookState[memberIdx] = { tab: 'STANCES', page: 1 };
  drpgState.spellbookState[memberIdx].page = Math.max(1, (drpgState.spellbookState[memberIdx].page || 1) + delta);
  const render = getRenderPartyModal();
  if (render) render();
}
