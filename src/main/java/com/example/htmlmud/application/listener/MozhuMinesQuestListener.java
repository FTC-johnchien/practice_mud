package com.example.htmlmud.application.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.event.MobEvents;
import com.example.htmlmud.domain.service.WorldManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 墨竹山·道祖是克蘇魯 劇情任務監聽器
 * 當擊倒畸變大師兄·宋天衡時，觸發「現代思維邏輯錨點格式化」與「秦劍師黑話玉牌」劇情事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MozhuMinesQuestListener {

  private final WorldManager worldManager;

  @Async
  @EventListener
  public void onBossDefeated(MobEvents.MobDead event) {
    if (event.mobId() != null && event.mobId().contains("boss_song_tianheng")) {
      log.info("【墨竹山·道祖是克蘇魯】畸變大師兄·宋天衡已被擊倒！觸發現代思維錨點格式化劇情！Killer: {}", event.killerId());

      if (event.killerId() != null) {
        worldManager.findPlayerActor(event.killerId()).ifPresent(player -> {
          player.reply("\n================================================================================");
          player.reply("【虛月崩解 · 現代思維錨點格式化】");
          player.reply("================================================================================");
          player.reply("畸變大師兄轟然倒地，胸腔碎裂的虛月骨瘤瘋狂噴湧黑紫煞氣，四周石壁的血肉菌絲劇烈抽搐！");
          player.reply("天道異化低語直灌你的靈魂深處：「太素無常……萬法皆腐……同化……歸一……」");
          player.reply("【理智警告】你的道心正遭受極限衝擊！你立刻啟動穿越者的現代唯物邏輯作為理性錨點：");
          player.reply("  1. [程式邏輯錨點] 「萬物皆可封裝，記憶體洩漏終將被 Garbage Collector 標記回收！」");
          player.reply("  2. [物理法則錨點] 「光速 299,792,458 m/s，熱力學第二定律不可逆，封閉系統熵增恆定！」");
          player.reply("  3. [唯物哲學錨點] 「物質決定意識，一切瘋狂皆是神經遞質被外力雜質污染的生化反應！」");
          player.reply("--------------------------------------------------------------------------------");
          player.reply("「格式化執行完畢……理智錨點穩固，異化煞氣已被你的現代認知強行降維封裝！」");
          player.reply("背後的黑曜骨鐮緩緩退入肩胛，虛月的癲狂低語在你腦海中化作一片白雜訊。");
          player.reply("\n一道清脆的破空聲響起——古老通風豎井上方，一名身著墨家劍袍的冷峻修士飄然而落。");
          player.reply("「墨竹山外門劍閣·秦劍師」打量著你手中的黑曜骨鐮，眉頭微挑：");
          player.reply("「這名雜役……你這波『底層邏輯』的閉環相當激進啊。大師兄這點架構缺陷，硬生生被你重構掉了？」");
          player.reply("秦劍師隨手擲下一枚沉甸甸的玉牌：");
          player.reply("「拿著這塊長老玉牌，去外門劍閣找望舒真人報到。墨竹山如今各處都在抓『抓手』，正缺你這種懂『賦能』的人才。」");
          player.reply("【系統提示】你獲得了戰利品：[mozhu_mines:black_obsidian_scythe] 與 [mozhu_mines:elder_token]！");
          player.reply("【出口解鎖】北方的古老通風豎井已開啟，可通往外界！\n");
        });
      }
    }
  }
}
