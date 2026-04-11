---
article: OpenClaw与Cursor联动开发实践.md
preset_primary: system-design
density: balanced
style_primary: blueprint
image_count: 5
language: zh
---

## 配图大纲

| ID | 类型 | 风格 | 插入位置 | 文件名 |
|----|------|------|----------|--------|
| 01 | framework | blueprint | §2.1 整体架构后 | 01-framework-multi-agent-cursor.png |
| 02 | flowchart | notion | §3.1 任务分配流程后 | 02-flowchart-task-handoff.png |
| 03 | framework | blueprint | §9.2 MemOS Cloud 插件架构后 | 03-framework-memos-plugin.png |
| 04 | comparison | vector-illustration | §9.4.1 两者定位差异后 | 04-comparison-memory-systems.png |
| 05 | infographic | blueprint | §6.2 效率对比后 | 05-infographic-efficiency-comparison.png |

## Illustration 1

**Position**: 第 2 章 §2.1 整体架构  
**Purpose**: 将 ASCII 架构示意图转为可读的系统框架图  
**Visual Content**: Main Agent → 五类专用 Agent（pm/backend/frontend/test/review）→ Cursor CLI → 代码产物层  
**Filename**: 01-framework-multi-agent-cursor.png  

## Illustration 2

**Position**: 第 3 章 §3.1 任务分配流程  
**Purpose**: 标准化协作四步可视化  
**Visual Content**: 任务描述 → 方案复述 → 确认执行 → 审查验收（中文短标签）  
**Filename**: 02-flowchart-task-handoff.png  

## Illustration 3

**Position**: 第 9 章 §9.2 MemOS Cloud 插件架构  
**Purpose**: 云端记忆与插件、多 Agent 的关系  
**Visual Content**: MemOS Cloud（存储/语义搜索/图谱）→ HTTPS → 插件（自动回忆/捕获/钩子）→ 三个 agents  
**Filename**: 03-framework-memos-plugin.png  

## Illustration 4

**Position**: §9.4.1 两者定位差异  
**Purpose**: OpenClaw 本地记忆 vs MemOS Cloud 并排对比  
**Visual Content**: 两列表格视觉化：存储位置、检索方式、共享范围、主要用途（取自文中表格）  
**Filename**: 04-comparison-memory-systems.png  

## Illustration 5

**Position**: §6.2 效率对比  
**Purpose**: 突出「约 10 倍」效率叙事  
**Visual Content**: 传统开发 vs AI 开发：时间 6–7 周 vs 20+ 小时，人力对比条形或对比卡  
**Filename**: 05-infographic-efficiency-comparison.png  
