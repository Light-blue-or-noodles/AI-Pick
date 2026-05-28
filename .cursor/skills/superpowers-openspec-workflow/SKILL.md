---
name: superpowers-openspec-workflow
description: >-
  Delivers multi-milestone features using Superpowers for planning (brainstorming,
  writing-plans) and OpenSpec for contracts (proposal, design, specs, tasks) and
  implementation (/opsx:apply). Use when starting OpenSpec changes, writing M1/M2/M3
  plans, generating tasks from plans, applying changes, or when the user mentions
  Superpowers + OpenSpec, opsx:apply, or plan-before-tasks workflow in this repo.
---

# Superpowers + OpenSpec（OpenClaw 项目）

本仓库已采用该流程；完整说明见个人技能与项目文档。

## 快速路径（本仓库）

| 阶段 | 路径 / 命令 |
|------|-------------|
| 设计 | `docs/superpowers/specs/` |
| 计划 | `docs/superpowers/plans/` |
| OpenSpec | `openspec/changes/<change-id>/` |
| 人类可读总览 | `~/AI/Project/Superpowers-OpenSpec-协作流程.md` |

## 执行说明

**必须阅读**个人技能中的逐步流程（内容与本文一致）：

`~/.cursor/skills/superpowers-openspec-workflow/SKILL.md`

以及 [reference.md](reference.md)（与 personal skill 的 reference 相同结构）。

## 本仓库已验证 change

- `h5-mini-program-parity` — H5 与小程序 1:1 对齐（M1–M3 已实施，见 `openspec/changes/h5-mini-program-parity/tasks.md`）

## 原则（摘要）

1. plan → tasks → apply，不用 propose 跳过 plan。
2. apply 指令写明 plan 全路径。
3. 完成前 `npm run build` 与 UAT 清单。

详细步骤、提示词模板、FAQ → 见 `~/.cursor/skills/superpowers-openspec-workflow/SKILL.md`。
