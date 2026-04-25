---
type: mixed
density: per-section
style: hand-drawn-warm
image_count: 10
language: zh-CN
---

## Article Illustration Outline

**Article**: OpenClaw与Cursor联动开发实践-v2.md
**Analysis Date**: 2026-04-15
**Style**: hand-drawn-warm (based on user reference image, warm paper texture + cartoon technical doodle style)

---

## Illustration 1: Agent Architecture Framework
**Position**: §2.1 Agent 系统架构
**Purpose**: Visualize the three-layer Agent architecture (Main Agent → Specialized Agents → Tool Agents)
**Type**: framework
**Visual Content**: 
- Top layer: Main Agent (coordinator robot with crown)
- Middle layer: 5 specialized agents (pm, backend, frontend, test, review) as cute robots
- Bottom layer: Tool agents (cursor-agent, browser, test)
- Flow arrows showing task distribution
**Filename**: 01-framework-agent-architecture.png
**Reference**: Replace ASCII diagram in §2.1

---

## Illustration 2: Memory System Layers
**Position**: §2.2 记忆系统设计
**Purpose**: Show three-layer memory system hierarchy
**Type**: framework
**Visual Content**:
- Top: Project-level memory (AGENTS.md) - orange, building icon
- Middle: Agent-level memory (MEMORY.md) - blue, robot icon
- Bottom: Cloud memory (MemOS Cloud) - green, cloud icon
- Arrows showing read/sync flow
**Filename**: 02-framework-memory-system.png
**Reference**: Replace ASCII diagram in §2.2

---

## Illustration 3: Collaboration Flowchart
**Position**: §3.2 OpenClaw 与 Cursor 的协作模型
**Purpose**: Show 6-step collaboration flow from user to code
**Type**: flowchart
**Visual Content**:
- Step 1: User sends task (human figure)
- Step 2: Main Agent understands (robot)
- Step 3: backend/frontend-agent reads MEMORY.md (robot)
- Step 4: cursor-agent builds CLI command (robot with wand)
- Step 5: Cursor CLI generates code (computer)
- Step 6: Code repository (checkmark)
- Vertical flow with connecting arrows
**Filename**: 03-flowchart-collaboration-model.png
**Reference**: Replace ASCII diagram in §3.2

---

## Illustration 4: Task Distribution Flow
**Position**: §4.2 多 Agent 协作流程
**Purpose**: Show horizontal workflow with review pass/fail branches
**Type**: flowchart
**Visual Content**:
- Stage 1: pm-agent (requirements)
- Stage 2: dev agents (coding)
- Stage 3: review-agent (review with pass/fail branches)
- Stage 4: test-agent (testing)
- Stage 5: Complete (trophy)
- Horizontal layout with color-coded agents
**Filename**: 04-flowchart-task-distribution.png
**Reference**: Replace ASCII diagram in §4.2

---

## Illustration 5: Evolution Timeline
**Position**: §8.1 从混乱到规范：四阶段演进
**Purpose**: Show 4-stage evolution from chaos to intelligence
**Type**: timeline
**Visual Content**:
- Phase 1 (Red): Direct coding - chaos, 40% standard rate
- Phase 2 (Yellow): cursor-agent - tool phase, 60% standard rate
- Phase 3 (Green): AGENTS.md - standardization, 90% standard rate
- Phase 4 (Blue): MemOS Cloud - intelligence, 95% standard rate
- Timeline with dates and metrics
- Status indicators: ❌ → ⚠️ → ✅ → ⭐
**Filename**: 05-timeline-evolution.png
**Reference**: New illustration for §8.1

---

## Illustration 6: Multi-Agent Task Handoff
**Position**: §4.1.3 指令传递与结果回调机制
**Purpose**: Visualize task handoff chain before cursor-agent execution
**Type**: flowchart
**Filename**: 02-flowchart-task-handoff.png

---

## Illustration 7: MemOS Plugin Architecture
**Position**: §6.2 MemOS Cloud 插件架构
**Purpose**: Show MemOS Cloud platform and plugin capabilities mapped to multi-agents
**Type**: framework
**Filename**: 03-framework-memos-plugin.png

---

## Illustration 8: Memory Systems Comparison
**Position**: §7.2 详细对比分析
**Purpose**: Compare OpenClaw local memory and MemOS Cloud memory at a glance
**Type**: comparison
**Filename**: 04-comparison-memory-systems.png

---

## Illustration 9: Efficiency Comparison Infographic
**Position**: §8.3 演进历程数据总结
**Purpose**: Emphasize efficiency gain and manpower change with visual contrast
**Type**: infographic
**Filename**: 05-infographic-efficiency-comparison.png

---

## Illustration 10: Multi-Agent + Cursor Layered Framework
**Position**: §3.1 Cursor Agent 模式解析
**Purpose**: Supplement chapter-level architecture perspective and role relationships
**Type**: framework
**Filename**: 01-framework-multi-agent-cursor.png

---

## Summary

Total illustrations: 10
- Framework: 4
- Flowchart: 3
- Timeline: 1
- Comparison: 1
- Infographic: 1

All illustrations use warm style with:
- Hand-drawn aesthetic
- Cute robot characters
- Warm color palette (oranges, blues, greens)
- Paper texture background
- Chinese labels
