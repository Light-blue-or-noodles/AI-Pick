---
type: flowchart
style: warm
aspect: 3:4
---

# ZONES

## Top Zone (8%): Title
- Text: "OpenClaw + Cursor 协作流程"
- Style: Hand-drawn title with arrow decorations

## Middle Zone (80%): Flow Steps

**Step 1: 用户发送任务**
- Character: Human figure with speech bubble
- Label: "用户"
- Action: "发送任务"
- Arrow: Down to Step 2

**Step 2: Main Agent 理解任务**
- Character: Robot with thinking bubble
- Label: "Main Agent"
- Action: "任务理解"
- Sub-actions: 识别类型、选择 Agent
- Arrow: Down to Step 3

**Step 3: 分发给专业 Agent**
- Character: Robot with clipboard
- Label: "backend-agent / frontend-agent"
- Action: "读取 MEMORY.md"
- Sub-actions: 确认规范、设计方案
- Arrow: Down to Step 4

**Step 4: 调用 cursor-agent**
- Character: Robot with magic wand
- Label: "cursor-agent Skill"
- Action: "构建 CLI 命令"
- Sub-actions: 打包上下文、调用 Cursor
- Arrow: Down to Step 5

**Step 5: Cursor CLI 生成代码**
- Character: Computer/terminal icon
- Label: "Cursor CLI"
- Action: "代码生成"
- Sub-actions: 读取上下文、生成文件
- Arrow: Down to Step 6

**Step 6: 返回结果**
- Character: Robot with checkmark
- Label: "代码仓库"
- Action: "生成/修改代码文件"
- Result: ✓ 完成

## Bottom Zone (10%): Summary
- Text: "任务自动分发 → 代码智能生成 → 结果自动返回"
- Style: Hand-drawn banner with stars

## Side Zone (2%): Flow indicators
- Vertical flow arrows connecting all steps
- Step numbers: ① ② ③ ④ ⑤ ⑥

# LABELS

All labels in Chinese (Simplified):
- 用户 (User)
- 发送任务 (Send task)
- Main Agent (Main Agent)
- 任务理解 (Task understanding)
- backend-agent / frontend-agent
- 读取 MEMORY.md (Read MEMORY.md)
- cursor-agent Skill
- 构建 CLI 命令 (Build CLI command)
- Cursor CLI
- 代码生成 (Code generation)
- 代码仓库 (Code repository)
- 完成 (Complete)

# COLORS

- Background: Cream/beige paper texture
- Step 1 (User): #FFB347 (orange)
- Step 2 (Main): #FF9F7A (coral)
- Step 3 (Agent): #87CEEB (sky blue)
- Step 4 (cursor): #DDA0DD (plum)
- Step 5 (Cursor): #98D8C8 (mint)
- Step 6 (Result): #90EE90 (light green)
- Text: #333333 (dark gray)
- Arrows: #666666 (gray)
- Accents: Gold stars, sparkles

# STYLE

- Style: Hand-drawn flowchart, warm and friendly
- Aesthetic: Children's book meets technical diagram
- Characters: Cute robots and human figures
- Lines: Hand-drawn, slightly imperfect
- Arrows: Bold, hand-drawn style
- Containers: Soft rounded rectangles
- Texture: Paper/canvas background
- Decorations: Stars, sparkles, small clouds
- Reference: Similar to the provided example images with warm colors and playful style

# ASPECT
- Ratio: 3:4
- Orientation: Portrait
- Safe zones: Keep 10% margin on all sides
