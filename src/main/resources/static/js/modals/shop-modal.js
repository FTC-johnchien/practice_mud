import { store } from '../core/state-store.js';
import { sendCmd } from '../core/cmd-dispatcher.js';

/**
 * 貨棧交易彈窗組件 (Shop Modal Component)
 * 負責渲染城鎮商店商品清單、購買數量調節、庫存售罄判定與交易派發
 */

const drpgState = store.getState();
const send = (cmd, silent) => sendCmd(cmd, silent);

/**
 * 開啟貨棧交易模態視窗 (Shop Modal)
 */
function openShopModal(data) {
  if (!data) return;
  drpgState.lastShopCatalog = data;

  const modal = document.getElementById('shop-modal');
  if (!modal) return;

  const titleEl = document.getElementById('shop-modal-title-text');
  if (titleEl) {
    titleEl.innerText = `🏪【${data.shopName || '貨棧櫃檯'}】`;
  }

  const coinEl = document.getElementById('shop-modal-coin-val');
  if (coinEl) {
    coinEl.innerText = `${data.playerCoin || 0} 靈石`;
  }

  // 若彈窗已經開啟，動態刷新各商品的剩餘庫存與購買按鈕狀態，而不重置整個 DOM 避免干擾輸入
  if (drpgState.isShopModalOpen && !modal.classList.contains('hidden')) {
    if (data.goods && data.goods.length > 0) {
      data.goods.forEach(item => {
        const row = document.getElementById(`shop-item-${item.id}`);
        if (!row) return;
        const stockEl = row.querySelector('.shop-item-stock');
        if (stockEl) {
          if (item.stock !== undefined && item.stock >= 0) {
            stockEl.style.color = item.stock > 0 ? '#38bdf8' : '#ef4444';
            stockEl.innerText = `(庫存: ${item.stock})`;
          } else {
            stockEl.style.color = '#10b981';
            stockEl.innerText = '(充足)';
          }
        }
        const buyBtn = row.querySelector('.shop-buy-btn');
        const qtyInput = document.getElementById(`shop-qty-${item.id}`);
        const isOutOfStock = (item.stock !== undefined && item.stock === 0);
        if (qtyInput && item.stock !== undefined && item.stock >= 0) {
          qtyInput.max = item.stock;
          if (parseInt(qtyInput.value) > item.stock) {
            qtyInput.value = Math.max(1, item.stock);
          }
        }
        if (buyBtn) {
          if (isOutOfStock) {
            buyBtn.disabled = true;
            buyBtn.style.opacity = '0.5';
            buyBtn.style.cursor = 'not-allowed';
            buyBtn.innerText = '❌ 售罄';
          } else {
            buyBtn.disabled = false;
            buyBtn.style.opacity = '1';
            buyBtn.style.cursor = 'pointer';
            buyBtn.innerText = '🛒 購買';
          }
        }
      });
    }
    return;
  }

  drpgState.isShopModalOpen = true;
  const goodsContainer = document.getElementById('shop-modal-goods-list');
  if (goodsContainer) {
    let goodsHtml = '';
    if (data.goods && data.goods.length > 0) {
      data.goods.forEach(item => {
        const isOutOfStock = (item.stock !== undefined && item.stock === 0);
        const stockHtml = (item.stock !== undefined && item.stock >= 0)
          ? `<span class="shop-item-stock" style="color: ${item.stock > 0 ? '#38bdf8' : '#ef4444'}; margin-left: 8px; font-size: 0.85em;">(庫存: ${item.stock})</span>`
          : `<span class="shop-item-stock" style="color: #10b981; margin-left: 8px; font-size: 0.85em;">(充足)</span>`;
        const buyBtnText = isOutOfStock ? '❌ 售罄' : '🛒 購買';
        const buyBtnDisabled = isOutOfStock ? 'disabled style="opacity: 0.5; cursor: not-allowed;"' : '';
        const maxQty = (item.stock !== undefined && item.stock > 0) ? item.stock : 99;

        goodsHtml += `
          <div class="shop-item-row" id="shop-item-${item.id}">
            <div class="shop-item-info">
              <span class="shop-item-badge">#${item.index}</span>
              <span class="shop-item-name">${item.name}</span>
              <span class="shop-item-price">💰 ${item.price} 靈石</span>
              ${stockHtml}
            </div>
            <div class="shop-item-desc">${item.description || ''}</div>
            <div class="shop-item-actions">
              <div class="shop-qty-picker">
                <button class="qty-btn" type="button" onclick="adjustShopQty('${item.id}', -1)">-</button>
                <input type="number" id="shop-qty-${item.id}" class="shop-qty-input" value="1" min="1" max="${maxQty}" />
                <button class="qty-btn" type="button" onclick="adjustShopQty('${item.id}', 1)">+</button>
              </div>
              <button class="shop-buy-btn" type="button" ${buyBtnDisabled} onclick="triggerShopBuy('${item.id}')">${buyBtnText}</button>
            </div>
          </div>
        `;
      });
    } else {
      goodsHtml = '<div style="color: #94a3b8; padding: 20px; text-align: center;">貨架空空如也，掌櫃尚在進貨中...</div>';
    }
    goodsContainer.innerHTML = goodsHtml;
  }

  modal.classList.remove('hidden');
}

/**
 * 開關貨棧交易視窗
 */
function toggleShopModal(show) {
  const modal = document.getElementById('shop-modal');
  if (!modal) return;
  if (show === undefined) {
    drpgState.isShopModalOpen = !drpgState.isShopModalOpen;
  } else {
    drpgState.isShopModalOpen = !!show;
  }
  if (drpgState.isShopModalOpen) {
    modal.classList.remove('hidden');
  } else {
    modal.classList.add('hidden');
    if (document.activeElement) {
      document.activeElement.blur();
    }
  }
}

function renderShopCatalogInLog(data) {
  openShopModal(data);
}

function adjustShopQty(itemId, delta) {
  const input = document.getElementById(`shop-qty-${itemId}`);
  if (!input) return;
  let val = parseInt(input.value) || 1;
  val = Math.max(1, Math.min(99, val + delta));
  input.value = val;
}

function triggerShopBuy(itemId) {
  const input = document.getElementById(`shop-qty-${itemId}`);
  const qty = input ? (parseInt(input.value) || 1) : 1;
  send(`buy ${itemId} ${qty}`);
}



// 相容掛載至 window 供 HTML 內聯事件呼叫
if (typeof window !== 'undefined') {
  window.openShopModal = openShopModal;
  window.toggleShopModal = toggleShopModal;
  window.renderShopCatalogInLog = renderShopCatalogInLog;
  window.adjustShopQty = adjustShopQty;
  window.triggerShopBuy = triggerShopBuy;
}

export {
  openShopModal,
  toggleShopModal,
  renderShopCatalogInLog,
  adjustShopQty,
  triggerShopBuy
};
