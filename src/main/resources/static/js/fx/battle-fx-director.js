import { spawnFloatingText } from './floating-text.js';
import { getUnitRect } from './fx-geometry.js';

/**
 * 戰鬥動態表現指揮官 (Battle FX Director)
 * 負責消費 BATTLE_EVENTS，排程時間軸播放出手微位移、受擊反應 class 與浮動飄字
 */
class BattleFxDirector {
  constructor() {
    this.lastSeq = 0;
    this.currentBattleId = null;
    this.timers = new Set();
  }

  get layer() {
    return document.getElementById('battle-fx-layer');
  }

  /**
   * 批次接收戰鬥事件並排程入隊
   * @param {string} battleId 戰鬥場次 ID
   * @param {Array} events 事件清單
   */
  enqueue(battleId, events = []) {
    if (!events || events.length === 0) return;
    if (document.hidden) return; // 處於背景分頁時靜默略過，防止積壓爆發

    // 切換戰鬥場次時重置序號計數器
    if (this.currentBattleId !== battleId) {
      this.clear();
      this.currentBattleId = battleId;
    }

    // 依序號去重與排序
    const freshEvents = events
      .filter(e => e && e.seq > this.lastSeq)
      .sort((a, b) => a.seq - b.seq);

    if (freshEvents.length === 0) return;
    this.lastSeq = freshEvents[freshEvents.length - 1].seq;

    // 將同一個 tick (約 500ms) 的事件錯開播放 (間隔 60~100ms)
    const stepMs = Math.min(100, Math.max(40, Math.floor(400 / freshEvents.length)));
    freshEvents.forEach((ev, idx) => {
      this.later(() => this.playEvent(ev), idx * stepMs);
    });
  }

  /**
   * 播放單一戰鬥事件
   */
  playEvent(ev) {
    if (!ev || !this.layer) return;

    // 1. 出手者短促突進動態 (Actor Animation)
    if (ev.actor) {
      const actorEl = this.findUnitEl(ev.actor);
      if (actorEl) {
        const actorAnimClass = (ev.actor.side === 'ENEMY') ? 'actor-lunge-down' : 'actor-lunge-up';
        this.flashClass(actorEl, actorAnimClass, 260);
      }
    }

    // 2. 目標命中受擊反應與浮動文字 (Hits)
    const hits = ev.hits || [];
    hits.forEach((hit, hitIdx) => {
      this.later(() => {
        const targetEl = this.findUnitEl(hit.target);
        if (!targetEl) return;

        const rect = getUnitRect(targetEl, this.layer);
        const outcome = hit.outcome || 'HIT';

        // 觸發受擊/防禦動態 class
        const reactCls = this.resolveReactionClass(outcome);
        if (reactCls) {
          this.flashClass(targetEl, reactCls, 400);
        }

        // 生成浮動數字
        spawnFloatingText(this.layer, rect, hit, ev);
      }, 100 + hitIdx * 50);
    });
  }

  /**
   * 依照單位參照查找現存 DOM 卡片
   */
  findUnitEl(ref) {
    if (!ref) return null;
    if (ref.side === 'ENEMY') {
      if (ref.id) {
        const elById = document.querySelector(`.enemy-card[data-unit-id="${CSS.escape(ref.id)}"]`);
        if (elById) return elById;
      }
      return document.querySelector(`.enemy-card[data-enemy-index="${ref.index}"]`);
    } else {
      if (ref.id) {
        const elById = document.querySelector(`.battle-party-grid-card[data-member-id="${CSS.escape(ref.id)}"]`);
        if (elById) return elById;
      }
      return document.querySelector(`.battle-party-grid-card[data-member-idx="${ref.index}"]`);
    }
  }

  resolveReactionClass(outcome) {
    return {
      HIT: 'react-hit',
      CRIT: 'react-crit',
      BLOCKED: 'react-block',
      PARRIED: 'react-parry',
      DODGED: 'react-dodge',
      MISS: null,
      ABSORBED: 'react-block'
    }[outcome] || 'react-hit';
  }

  /**
   * 安全瞬時附加並移除動畫 class
   */
  flashClass(el, cls, ms) {
    if (!el || !cls) return;
    el.classList.remove(cls);
    void el.offsetWidth; // 強制重繪觸發動畫
    el.classList.add(cls);
    this.later(() => {
      if (el) el.classList.remove(cls);
    }, ms);
  }

  later(fn, delayMs) {
    const timer = setTimeout(() => {
      this.timers.delete(timer);
      fn();
    }, delayMs);
    this.timers.add(timer);
  }

  /**
   * 清場重置
   */
  clear() {
    this.timers.forEach(clearTimeout);
    this.timers.clear();
    if (this.layer) {
      this.layer.replaceChildren();
    }
    this.lastSeq = 0;
  }
}

export const fxDirector = new BattleFxDirector();
