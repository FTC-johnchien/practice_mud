# 📋 M7: 資料驅動範本庫模組實作進度清單 (Templates & Validator Tasks)

> **文檔層級**：Level 3 — Task Lists (模組實作進度清單)  
> **對應規格**：👉 [`docs/2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md`](../../2_specifications/backend/M7_DATA_DRIVEN_TEMPLATES.md)  
> **核心職責**：管理 JSON 靜態資料目錄架構、TemplateCatalog 快取、啟動期 Fail-Fast 校驗器 (`DataIntegrityValidator`) 與架構看門狗。  
> **當前完成度**：100% 🟢
> 
---

## 🧭 一、程式碼單一真相源 (Single Source of Truth)

- **核心服務與看門狗**：
  - [`TemplateRepository.java`](../../../src/main/java/com/example/htmlmud/template/TemplateRepository.java)（範本快取與註冊核心）
  - [`TemplateCatalog.java`](../../../src/main/java/com/example/htmlmud/template/TemplateCatalog.java)（集中式範本快取目錄）
  - [`DataIntegrityValidator.java`](../../../src/main/java/com/example/htmlmud/template/DataIntegrityValidator.java)（啟動期健全性校驗器）
  - [`TemplateReader.java`](../../../src/main/java/com/example/htmlmud/template/TemplateReader.java)（唯讀資料介面）
- **關聯驗證測試**：
  - [`WorldDataIntegrityTest.java`](../../../src/test/java/com/example/htmlmud/WorldDataIntegrityTest.java)（8/8 綠燈）
  - [`DataIntegrityValidatorTest.java`](../../../src/test/java/com/example/htmlmud/template/DataIntegrityValidatorTest.java)（8/8 綠燈）
  - [`ArchitectureGuardrailsTest.java`](../../../src/test/java/com/example/htmlmud/domain/validation/ArchitectureGuardrailsTest.java)（架構零破窗看門狗 100% 綠燈）

---

## 🏆 二、已完成實作項目 (Completed)

- [x] **M7-PACKAGE-BY-FEATURE (模組 M7 套件獨立封裝)**
  - 依循依賴拓撲完成底層基石封裝，將 `TemplateRepository`, `TemplateCatalog`, `TemplateReader`, `TemplateRegistryPort`, `DataIntegrityValidator`, `*Template.java` 統一收斂至 `com.example.htmlmud.template` 與 `com.example.htmlmud.template.model.*`。
- [x] **M7-FAIL-FAST-VALIDATOR (啟動期嚴格校驗)**
  - 檢驗技能超頻階級 (0~3)、裝備插槽合法性、首領多階段閾值、地牢出入口合法性，有錯立即中斷啟動防靜默失敗。
- [x] **M7-ZERO-FALLBACK-WATCHDOG (看門狗棘輪清零)**
  - 全專案消除 26 處 `new TemplateCatalog()` 私建目錄反模式，全部強制落實建構子依賴注入 (DI)。
- [x] **M7-DATA-PARTITION (全域與區域資料解耦)**
  - 確立 `data/global/`（全域職業、技能、裝備分類）與 `data/zones/`（區域怪物、地牢、商店、設施）清晰層次。

---

## ⏳ 三、未竟待辦與演進項目 (Pending Backlog)

- 暫無未竟待辦（本模組全數實作已 100% 交付完畢，熱重載依 D4-4 明確除役以保障生產穩定性）。
