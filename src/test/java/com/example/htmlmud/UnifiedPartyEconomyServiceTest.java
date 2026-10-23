package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.item.UnifiedPartyEconomyService;

/**
 * 模組 M6 小隊共享經濟服務單元測試 (UnifiedPartyEconomyServiceTest)
 */
class UnifiedPartyEconomyServiceTest {

  private final UnifiedPartyEconomyService economyService = new UnifiedPartyEconomyService();

  @Test
  @DisplayName("M6-ECONOMY: 驗證小隊金庫餘額查詢與 null 安全防禦")
  void testBalanceQueryAndNullSafety() {
    assertThat(economyService.getBalance(null)).isEqualTo(0);
    assertThat(economyService.canAfford(null, 100)).isFalse();

    Party party = new Party();
    party.setCoin(500);
    assertThat(economyService.getBalance(party)).isEqualTo(500);
    assertThat(economyService.canAfford(party, 500)).isTrue();
    assertThat(economyService.canAfford(party, 501)).isFalse();
    assertThat(economyService.canAfford(party, -10)).isFalse();
  }

  @Test
  @DisplayName("M6-ECONOMY: 驗證小隊金庫靈石入帳流水線")
  void testAddCoinFlow() {
    Party party = new Party();
    party.setCoin(100);

    economyService.addCoin(party, 50);
    assertThat(party.getCoin()).isEqualTo(150);

    // 負數或 0 不應異動餘額
    economyService.addCoin(party, 0);
    assertThat(party.getCoin()).isEqualTo(150);
    economyService.addCoin(party, -20);
    assertThat(party.getCoin()).isEqualTo(150);
  }

  @Test
  @DisplayName("M6-ECONOMY: 驗證小隊金庫扣款與超額阻斷")
  void testSpendCoinFlow() {
    Party party = new Party();
    party.setCoin(200);

    boolean spent = economyService.spendCoin(party, 80);
    assertThat(spent).isTrue();
    assertThat(party.getCoin()).isEqualTo(120);

    // 扣款 0 視為合法不扣款
    boolean spentZero = economyService.spendCoin(party, 0);
    assertThat(spentZero).isTrue();
    assertThat(party.getCoin()).isEqualTo(120);

    // 超額扣款應嚴格阻斷
    boolean overspent = economyService.spendCoin(party, 150);
    assertThat(overspent).isFalse();
    assertThat(party.getCoin()).isEqualTo(120);

    // 負數扣款應阻斷
    boolean negativeSpent = economyService.spendCoin(party, -10);
    assertThat(negativeSpent).isFalse();
    assertThat(party.getCoin()).isEqualTo(120);
  }
}
