package com.example.htmlmud.application.dto;

import com.example.htmlmud.domain.actor.impl.Player;

/**
 * 統一的遊戲請求物件
 * 
 * @param player 發起請求的玩家實體
 * @param commandText 原始輸入字串 (如 "go north" 或 "kill goblin")
 * @param source 來源標記 (可選，如 "WEB", "MOBILE")
 */
public record GameRequest(Player player, String commandText, String source) {
}
