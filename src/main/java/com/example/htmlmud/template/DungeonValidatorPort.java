package com.example.htmlmud.template;

import java.util.List;

/**
 * 地牢資料拓撲校驗連接埠 (Port)
 * 供 DataIntegrityValidator 於開機期校驗地牢地塊、首領與掉落物，反轉 template 對 dungeon 模組的依賴。
 */
public interface DungeonValidatorPort {

  /**
   * 執行地牢資料完整性校驗
   *
   * @param templateReader 範本讀取器
   * @param errors 錯誤訊息收集清單
   * @param warnings 警告訊息收集清單
   * @return 校驗的地牢樓層總數
   */
  int validateDungeons(TemplateReader templateReader, List<String> errors, List<String> warnings);
}
