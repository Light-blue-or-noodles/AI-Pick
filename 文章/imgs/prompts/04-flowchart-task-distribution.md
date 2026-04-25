---
type: flowchart
style: warm
aspect: 16:9
---

# ZONES

## Top Zone (8%): Title
- Text: "多 Agent 协作任务分发流程"
- Style: Hand-drawn title with network icons

## Middle Zone (75%): Horizontal Flow

**Stage 1: 需求/任务**
- Icon: Document with lightbulb
- Label: "需求/任务"
- Character: pm-agent (robot with clipboard)

**Arrow →** "分解任务"

**Stage 2: 开发 Agent**
- Characters: 
  - backend-agent (blue gear robot)
  - frontend-agent (green paintbrush robot)
- Label: "backend-agent / frontend-agent"
- Action: "调用 cursor-agent"

**Arrow →** "开发完成"

**Stage 3: 代码审查**
- Character: review-agent (red checkmark robot)
- Label: "review-agent"
- Action: "代码审查"

**Branch:**
- Path A (通过): Arrow down → "审查通过"
- Path B (不通过): Arrow up → "返回修复" → Loop back to Stage 2

**Arrow →** "测试"

**Stage 4: 测试验证**
- Character: test-agent (yellow magnifying glass robot)
- Label: "test-agent"
- Action: "测试验证"

**Arrow →** "测试通过"

**Stage 5: 任务完成**
- Icon: Trophy/checkmark
- Label: "任务完成"
- Character: All agents celebrating

## Bottom Zone (12%): Legend
- Color-coded agent icons with labels
- Text: "循环迭代 · 质量门禁"

## Side Zone (5%): Status indicators
- Checkmarks for completed stages
- Warning icons for review stage

# LABELS

All labels in Chinese (Simplified):
- 需求/任务 (Requirements/Task)
- 分解任务 (Decompose task)
- backend-agent / frontend-agent
- 调用 cursor-agent (Call cursor-agent)
- 开发完成 (Development complete)
- review-agent
- 代码审查 (Code review)
- 审查通过 (Review passed)
- 审查不通过 (Review failed)
- 返回修复 (Return to fix)
- test-agent
- 测试验证 (Test verification)
- 测试通过 (Test passed)
- 任务完成 (Task complete)

# COLORS

- Background: Cream/beige paper texture
- Stage 1 (pm): #FFB347 (orange)
- Stage 2 (dev): #87CEEB (sky blue) + #90EE90 (green)
- Stage 3 (review): #CD5C5C (red)
- Stage 4 (test): #F0E68C (khaki)
- Stage 5 (complete): #98D8C8 (mint)
- Text: #333333 (dark gray)
- Arrows: #666666 (gray)
- Success path: #90EE90 (green)
- Failure path: #FFA07A (light salmon)
- Accents: Gold stars, sparkles

# STYLE

- Style: Hand-drawn flowchart, warm and friendly
- Aesthetic: Children's book meets technical diagram
- Layout: Horizontal left-to-right flow
- Characters: Cute robots at each stage
- Lines: Hand-drawn, slightly imperfect
- Arrows: Bold, hand-drawn style with labels
- Containers: Soft rounded rectangles
- Branch: Clear visual distinction for pass/fail
- Texture: Paper/canvas background
- Decorations: Stars, sparkles, small clouds
- Reference: Similar to the provided example images with warm colors and playful style

# ASPECT
- Ratio: 16:9
- Orientation: Landscape
- Safe zones: Keep 10% margin on all sides
