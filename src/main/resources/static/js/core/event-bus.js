/**
 * 輕量事件總線 (EventBus)
 * 用於解耦各面板組件間的直接依賴與跨模組事件通知
 */
class EventBus {
  constructor() {
    this.events = new Map();
  }

  /**
   * 監聽事件
   * @param {string} event 事件名稱
   * @param {Function} handler 回呼函式
   */
  on(event, handler) {
    if (!this.events.has(event)) {
      this.events.set(event, new Set());
    }
    this.events.get(event).add(handler);
    return () => this.off(event, handler);
  }

  /**
   * 取消監聽
   * @param {string} event 事件名稱
   * @param {Function} handler 回呼函式
   */
  off(event, handler) {
    if (this.events.has(event)) {
      this.events.get(event).delete(handler);
      if (this.events.get(event).size === 0) {
        this.events.delete(event);
      }
    }
  }

  /**
   * 發布事件
   * @param {string} event 事件名稱
   * @param {any} data 事件附帶資料
   */
  emit(event, data) {
    if (this.events.has(event)) {
      this.events.get(event).forEach(handler => {
        try {
          handler(data);
        } catch (err) {
          console.error(`[EventBus] Error in handler for event '${event}':`, err);
        }
      });
    }
  }
}

export const eventBus = new EventBus();
