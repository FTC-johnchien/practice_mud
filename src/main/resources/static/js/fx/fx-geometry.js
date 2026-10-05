/**
 * 戰鬥特效幾何計算工具 (FX Geometry Helper)
 * 負責將任意卡片 DOM 座標換算為相對於 FX Layer 的像素座標
 */

export function getUnitRect(el, layer) {
  if (!el || !layer) {
    return { x: 0, y: 0, w: 0, h: 0 };
  }
  const elRect = el.getBoundingClientRect();
  const layerRect = layer.getBoundingClientRect();

  return {
    x: elRect.left - layerRect.left,
    y: elRect.top - layerRect.top,
    w: elRect.width,
    h: elRect.height
  };
}

export function getUnionRect(elements, layer) {
  if (!elements || elements.length === 0 || !layer) {
    return { x: 0, y: 0, w: 0, h: 0 };
  }
  const rects = elements.map(el => getUnitRect(el, layer)).filter(r => r.w > 0 && r.h > 0);
  if (rects.length === 0) {
    return { x: 0, y: 0, w: 0, h: 0 };
  }

  let minX = Infinity;
  let minY = Infinity;
  let maxX = -Infinity;
  let maxY = -Infinity;

  rects.forEach(r => {
    if (r.x < minX) minX = r.x;
    if (r.y < minY) minY = r.y;
    if (r.x + r.w > maxX) maxX = r.x + r.w;
    if (r.y + r.h > maxY) maxY = r.y + r.h;
  });

  return {
    x: minX,
    y: minY,
    w: Math.max(0, maxX - minX),
    h: Math.max(0, maxY - minY)
  };
}
