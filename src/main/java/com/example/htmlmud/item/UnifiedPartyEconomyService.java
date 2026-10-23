package com.example.htmlmud.item;

import org.springframework.stereotype.Service;
import com.example.htmlmud.party.Party;
import lombok.extern.slf4j.Slf4j;

/**
 * 小隊大一統共享經濟服務 (M6-UNIFIED-ECONOMY)
 * 統一管理小隊共享靈石金庫 (Party.coin) 的查詢、出納、收支防禦與交易合法性校驗。
 */
@Slf4j
@Service
public class UnifiedPartyEconomyService {

  /**
   * 取得指定小隊的共享金庫餘額
   */
  public int getBalance(Party party) {
    return party != null ? party.getCoin() : 0;
  }

  /**
   * 檢核小隊是否具備足夠支付指定金額之靈石
   */
  public boolean canAfford(Party party, int amount) {
    return party != null && amount >= 0 && party.getCoin() >= amount;
  }

  /**
   * 向小隊金庫存入靈石 (戰利品繳獲、寶箱、販售道具)
   */
  public void addCoin(Party party, int amount) {
    if (party == null || amount <= 0) return;
    party.addCoin(amount);
    log.debug("小隊金庫入帳 {} 靈石，當前餘額: {}", amount, party.getCoin());
  }

  /**
   * 自小隊金庫扣除靈石 (商店購買、百煉重鑄、設施消耗)
   */
  public boolean spendCoin(Party party, int amount) {
    if (party == null || amount < 0) return false;
    if (amount == 0) return true;
    if (party.getCoin() < amount) return false;
    party.deductCoin(amount);
    log.debug("小隊金庫扣款 {} 靈石，剩餘: {}", amount, party.getCoin());
    return true;
  }
}
