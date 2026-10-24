# 🌲 十七專精技能樹與神通體系總覽 (Skill Trees Directory)

> **文檔層級**：Level 2 — Specifications (模組規劃層)  
> **上級指針**：👉 Level 1《[CYBERPUNK_CULTIVATION_SPEC.md](../../1_requirements/CYBERPUNK_CULTIVATION_SPEC.md)》  
> **對應模組**：👉 Level 2《[M5_SKILL_AND_PROGRESSION.md](../backend/M5_SKILL_AND_PROGRESSION.md)》與《[SKILL_SCHEMA.md](../data_schemas/SKILL_SCHEMA.md)》  
> **對應任務**：👉 Level 3《[M5_SKILL_AND_PROGRESSION_TASKS.md](../../3_task_lists/backend/M5_SKILL_AND_PROGRESSION_TASKS.md)》

---

## 🧭 一、四大歷史淵源與目錄架構

全專案技能體系採 **模組化獨立分檔治理 (Feature-by-Tree)**，劃分為四大歷史淵源與 17 棵專精樹。每份規格檔案詳述其四階主動戰技（煉氣、築基、金丹、元嬰大絕招）、常駐道骨/晶片插槽、數值公式、底層科技真相與連攜搭配：

```text
docs/2_specifications/skills/
├── README.md                              # 本導覽總索引
├── SYNERGY_AND_COMBOS.md                  # 合體技反應引擎、16雙星、8三才、5領域、15終結技
├── 1_cultivator/                          # ☯️ 東方修仙道統 (5 樹)
│   ├── TREE_01_GOLD_SWORD.md              # 庚金劍煞樹 (金/飛劍破甲/雷法變異)
│   ├── TREE_02_WOOD_LIFE.md               # 青帝靈木樹 (木/草木回春/風法劇毒)
│   ├── TREE_03_WATER_FROST.md             # 玄冥真水樹 (水/玄冰護盾/陰煞凍結)
│   ├── TREE_04_FIRE_FLAME.md              # 九天離火樹 (火/赤陽灼燒/熔岩爆破)
│   └── TREE_05_EARTH_SHIELD.md            # 后土鎮嶽樹 (土/厚土霸體/純陽辟邪)
├── 2_engineer/                            # 🏛️ 古代工程師類 (4 樹)
│   ├── TREE_06_GRAVITY.md                 # 天引重力樹 (引力定錨/黑洞聚怪)
│   ├── TREE_07_FRACTURE.md                # 神機裂解樹 (液壓超聲波/極限削韌破防)
│   ├── TREE_08_FORCEFIELD.md              # 太古金剛樹 (蜂巢力場/反震偏折護坦)
│   └── TREE_09_PLASMA_CORE.md             # 地核熔爐樹 (地熱等離子/熔岩地形)
├── 3_high_tech/                           # ⚡ 賽博高科技類 (4 樹)
│   ├── TREE_10_TIME_OVERCLOCK.md          # 時序超頻樹 (神經加速/子彈時間/GCD歸零)
│   ├── TREE_11_CYBER_WARFARE.md           # 協議逆亂樹 (電子戰/100%打斷/因果劫持)
│   ├── TREE_12_OPTICS_GLITCH.md           # 全息匿影樹 (頂點錯誤/分身/潛行暴擊)
│   └── TREE_13_KINETIC_RAILS.md           # 電磁動能樹 (高斯軌道/穿甲狙殺/真傷)
└── 4_cthulhu/                             # 🧬 克蘇魯外神類 (4 樹)
    ├── TREE_14_CHITIN_BLADE.md            # 強殖幾丁質樹 (外骨骼/骨刃切割/裂魂引爆)
    ├── TREE_15_BLOOD_SIPHON.md            # 黑山羊血裔樹 (嗜血寄生/全隊回哺/自殘狂化)
    ├── TREE_16_COSMIC_MADNESS.md          # 知見污染樹 (不可名狀囈語/恐懼混亂)
    └── TREE_17_NULL_VOID.md               # 虛無崩解樹 (空指針抹殺/記憶體洩漏/格式化)
```

---

## 📋 二、十七專精樹全覽索引表

| # | 歷史淵源 | 專精技能樹名稱 | 戰術機制與定位 | 代表大絕招 (Tier 4) | 常駐插槽 | 規格連結 |
|---|---|---|---|---|---|:---:|
| 01 | ☯️ 修仙 | **【庚金劍煞樹】** | 飛劍破甲、金煞先手印記 (`PRIMER`) | 【萬劍歸宗·太白凌虛大殺劫】 | 【劍心通明骨】 | [查看規格](1_cultivator/TREE_01_GOLD_SWORD.md) |
| 02 | ☯️ 修仙 | **【青帝靈木樹】** | 草木回春、真菌流毒、風法變異 | 【萬象回春·太陰神樹降世生】 | 【靈泉不息心法】 | [查看規格](1_cultivator/TREE_02_WOOD_LIFE.md) |
| 03 | ☯️ 修仙 | **【玄冥真水樹】** | 極寒減速、水幕吸收護盾 (`BARRIER`) | 【翻江倒海·九幽天河覆地陣】 | 【寒玉冰魄心】 | [查看規格](1_cultivator/TREE_03_WATER_FROST.md) |
| 04 | ☯️ 修仙 | **【九天離火樹】** | 赤陽灼燒、範圍爆裂、熔岩穿甲 | 【九天焚世·金烏大日焚天劫】 | 【赤帝焚炎髓】 | [查看規格](1_cultivator/TREE_04_FIRE_FLAME.md) |
| 05 | ☯️ 修仙 | **【后土鎮嶽樹】** | 前排高削韌嘲諷、金剛霸體減傷 | 【八荒六合·九州神山定鼎引】 | 【大地同調脈】 | [查看規格](1_cultivator/TREE_05_EARTH_SHIELD.md) |
| 06 | 🏛️ 工程 | **【天引重力樹】** | 引力定錨、黑洞聚怪、行動條壓制 | 【太古黑洞·奇異點湮滅核】 | 【地心定錨骨】 | [查看規格](2_engineer/TREE_06_GRAVITY.md) |
| 07 | 🏛️ 工程 | **【神機裂解樹】** | 液壓超聲波、極限削韌破防 (`POISE_BREAK`)| 【地核共振·萬刃崩山破】 | 【破陣先鋒骨】 | [查看規格](2_engineer/TREE_07_FRACTURE.md) |
| 08 | 🏛️ 工程 | **【太古金剛樹】** | 蜂巢偏折、反震回彈、重坦霸體 | 【絕對防衛·造物主力場堡壘】 | 【受創充能晶片】 | [查看規格](2_engineer/TREE_08_FORCEFIELD.md) |
| 09 | 🏛️ 工程 | **【地核熔爐樹】** | 地熱等離子、貫穿射線、熔岩地形 | 【地心超新星·行星熔爐過載爆發】 | 【超導散熱髓】 | [查看規格](2_engineer/TREE_09_PLASMA_CORE.md) |
| 10 | ⚡ 科技 | **【時序超頻樹】** | 神經加速、子彈時間、身法 GCD 歸零 | 【子彈時間·天衍特工時停獵殺】 | 【神經突觸超頻晶片】 | [查看規格](3_high_tech/TREE_10_TIME_OVERCLOCK.md) |
| 11 | ⚡ 科技 | **【協議逆亂樹】** | 100% 唱條打斷 (`INTERRUPT`)、因果劫持 | 【天道宕機·全頻段電磁因果癱瘓】 | 【零日漏洞嗅探算法】 | [查看規格](3_high_tech/TREE_11_CYBER_WARFARE.md) |
| 12 | ⚡ 科技 | **【全息匿影樹】** | 頂點錯誤、全息分身仇恨、潛行暴擊 | 【虛實莫辨·九幽全息幻殺陣】 | 【光學折射迷彩塗層】 | [查看規格](3_high_tech/TREE_12_OPTICS_GLITCH.md) |
| 13 | ⚡ 科技 | **【電磁動能樹】** | 高斯軌道、真實穿甲貫穿、反器材狙殺 | 【天基軌道·上帝之杖天劫滅】 | 【弱點彈道輔助晶片】 | [查看規格](3_high_tech/TREE_13_KINETIC_RAILS.md) |
| 14 | 🧬 外神 | **【強殖幾丁質樹】** | 外骨骼化相、骨刃切割、裂魂引爆 (`DETONATOR`)| 【完全降臨·強殖阿修羅天魔相】 | 【幾丁外骨骼骨】 | [查看規格](4_cthulhu/TREE_14_CHITIN_BLADE.md) |
| 15 | 🧬 外神 | **【黑山羊血裔樹】** | 嗜血撕裂、氣血吞噬、全隊回哺、自殘狂化 | 【萬子千孫·母神卵鞘血海臨】 | 【貪婪血髓心法】 | [查看規格](4_cthulhu/TREE_15_BLOOD_SIPHON.md) |
| 16 | 🧬 外神 | **【知見污染樹】** | 不可名狀囈語、恐懼混亂、海馬體抹除 | 【不可名狀·盲目痴愚知見障】 | 【深淵平靜心】 | [查看規格](4_cthulhu/TREE_16_COSMIC_MADNESS.md) |
| 17 | 🧬 外神 | **【虛無崩解樹】** | 空指針斬殺、記憶體洩漏成長 DoT、格式化 | 【歸墟終末·格式化天劫】 | 【九幽輪迴·硬體冷重啟】 | [查看規格](4_cthulhu/TREE_17_NULL_VOID.md) |

---

## 💥 三、合體技與終結技架構指針

小隊連攜合體技與終結技規範完整收錄於 👉 [**`SYNERGY_AND_COMBOS.md`**](SYNERGY_AND_COMBOS.md)：
1. **法則反應引擎**（涵蓋水火蒸發、火木擴散等 10 大反應矩陣）。
2. **16 大傳奇雙星名招**（如太極水火、星火燎原、九霄紫電、星隕高斯炮）。
3. **8 大三才殺招**（如萬毒天雨、天星地煞、千星瞬影）。
4. **5 大四象天道領域展開**（太乙萬劍、造物神殿、天衍神念、黑山羊肉相、四極降魔）。
5. **15 大 5 人全隊動態終結技矩陣**（4 純系、6 雙曜、4 三才、1 萬象全窮盡分類）。
