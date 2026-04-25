没错，这篇分享也是🦞完成的，我辅助进行一些排版和适配 😅

摘要：本文介绍了一个 AI 社交小程序 MVP 的快速开发实践。项目采用多 Agent 协作模式，由 5 个专用 AI Agent（pm-agent、backend-agent、frontend-agent、test-agent、review-agent）组成开发团队，通过 Cursor CLI 执行代码生成和修改。项目在约 20 小时内完成了 20 个 API 接口、6 个核心页面以及完整的部署方案，相比传统开发模式效率提升约 10 倍。文章详细阐述了多 Agent 协作架构设计、开发流程实践、关键技术挑战与解决方案，以及 AI 能力集成方案，为类似快速开发场景基于项目特点和开发效率的考虑提供参考。

关键词：多 Agent 协作；Cursor；微信小程序；快速开发；AI 编程

---

# 1. 项目背景与产品概述

![](https://oss-ata.alibaba.com/article/2026/03/6bba6a77-6bd1-42cc-a86e-d7b4fc01a4f9.png)

随着 AI 技术的快速发展，社交产品的形态和交互方式正在发生变革。本项目旨在验证一个核心问题：在 AI 能力足够强大的前提下，一个小型团队能否在短时间内完成一个完整产品的从 0 到 1 开发。

不是那种"用 AI 辅助写几行代码"的浅尝辄止，而是真正的全流程 AI 驱动——从产品构思到代码实现，从部署上线到运维监控，全部交给 AI 团队。

于是，AI-Pick 这个项目诞生了。

## 1.1. 实验目标

1. 验证 AI Coding 的能力边界 —— AI 能否理解复杂业务逻辑？
    
2. 验证多 Agent 协作的可行性 —— 多个 AI 能否像真人团队一样配合？
    
3. 验证开发效率的提升幅度 —— 传统开发需要多久？AI 开发需要多久？
    

## 1.2. 产品定位

![](https://oss-ata.alibaba.com/article/2026/03/0c75b95a-d1c9-49b7-a529-b997bdffd193.png)

AI-Pick 是一款 AI 驱动的兴趣 + 熟人(同事、校友、Pick 的好友)社交小程序，核心解决一个痛点：想找人玩，但找不到共同爱好的小伙伴。

原本我想做一个超级 APP，什么功能都有。后来 AI 产品经理（pm-agent）跟我说："哥，咱们时间有限，先做核心功能吧。😢"

## 1.3. 技术选型

基于项目特点和开发效率考虑，技术选型遵循"成熟、稳定、AI 友好"的原则。

|   |   |   |
|---|---|---|
|层次|技术选型|说明|
|前端|微信小程序|用户覆盖广，无需下载 APP|
|后端|Spring Boot 3.2|最新 LTS 版本，Java 技术栈成熟|
|ORM|MyBatis-Plus 3.5|简化 CRUD 操作|
|数据库|MySQL 8.0|主数据库，稳定可靠|
|缓存|Redis|热点数据缓存|
|AI 能力|阿里云百炼（qwen3.5-plus）|中文理解能力强，API 完善|
|AI 框架|Spring AI Alibaba|简化 AI 能力调用|
|部署|阿里云 ECS + Docker|容器化，方便运维|

# 2. 多 Agent 协作架构设计

## 2.1. Agent 角色设计

![](https://oss-ata.alibaba.com/article/2026/03/034a920f-8b41-404b-9fbd-2e40e13b03bc.png)

项目配置了 5 个专用 Agent，每个 Agent 有明确的角色定位和职责边界。

|   |   |   |   |
|---|---|---|---|
|Agent|角色|职责|主要产出|
|pm-agent|产品经理|需求分析、产品设计、文档输出|PRD、UI 设计方案、技术方案|
|backend-agent|后端开发|Spring Boot 后端开发、API 设计、数据库设计|Controller、Service、Mapper、Entity|
|frontend-agent|前端开发|微信小程序开发、UI 实现、交互逻辑|WXML、WXSS、JS、app.json|
|test-agent|测试工程师|测试用例编写、自动化测试、质量保障|单元测试、集成测试、测试报告|
|review-agent|代码审查|代码审查、提出改进建议、确保代码质量|审查报告、修改建议|

## 2.2. 整体架构

项目采用分层架构设计，由项目协调者（Main Agent）负责任务分配和进度跟踪，5 个专用 Agent 负责具体执行，通过 Cursor CLI 完成代码生成和修改。

![](https://oss-ata.alibaba.com/article/2026/03/ff773a3c-751f-4b47-aea8-112287aa45d9.png)

## 2.3 接入钉钉

[https://github.com/soimy/openclaw-channel-dingtalk](https://github.com/soimy/openclaw-channel-dingtalk)

```
openclaw plugins install @soimy/dingtalk
```

功能特性

- ✅ Stream 模式 — WebSocket 长连接，无需公网 IP 或 Webhook
    
- ✅ 私聊支持 — 直接与机器人对话
    
- ✅ 群聊支持 — 在群里 @机器人
    
- ✅ 多种消息类型 — 文本、图片、语音（自带识别）、视频、文件、钉钉文档/钉盘文件卡片
    
- ✅ 附件文本抽取 — 对常见文本类附件以及 `PDF/DOCX` 自动抽取正文并注入当前会话上下文
    
- ✅ 引用消息支持 — 支持恢复大多数引用场景（文字/图片/图文/文件/视频/语音/AI 卡片）；单聊中的钉钉文档依赖权限 Storage.DownloadInfo.Read；群聊支持引用群图片、文件/文档（优先命中已持久化索引，未命中时走群文件 API 兜底），其中群文件相关能力需 ConvFile.Space.Read、Storage.File.Read、Storage.DownloadInfo.Read、Contact.User.Read，且兜底链路受时间窗口与企业认证限制
    
- ✅ Markdown 回复 — 支持富文本格式回复
    
- ✅ Markdown 表格兼容 — 自动把 Markdown 表格转换为钉钉更稳定的可读文本
    
- ✅ 互动卡片 — 支持流式更新，适用于 AI 实时输出
    
- ✅ 完整 AI 对话 — 接入 Clawdbot 消息处理管道
    
- ✅ @多助手路由 — 在群聊中通过 `@助手名` 路由到不同的 agent（实验性功能）
    

# 3. 开发流程实践

## 3.1. 任务分配流程

![](https://oss-ata.alibaba.com/article/2026/03/95d3b41e-558e-4413-9054-2360cf231319.png)

任务分配采用自然语言描述 + 方案复述确认的方式，确保 Agent 正确理解需求。

标准流程：

1. 用自然语言描述任务需求
    
2. 对应 Agent 理解任务并复述方案
    
3. 确认方案正确
    
4. Agent 调用 cursor-agent skill
    
5. Cursor CLI 执行代码修改
    
6. review-agent 审查代码
    
7. 项目验收
    

关键要点：

- 任务描述要具体，包含功能点和验收标准
    
- 复杂任务拆分成多个小步骤，每步单独验收
    

## 3.2. 代码生成质量保障

项目建立了质量审查机制，由 review-agent 负责代码质量把关。

审查维度：

|   |   |
|---|---|
|维度|审查要点|
|代码规范|命名规范、代码格式、注释完整性|
|业务逻辑|功能正确性、边界条件处理、异常处理|
|安全性|SQL 注入防护、越权访问防护、敏感数据脱敏|
|性能|数据库查询优化、缓存使用、N+1 问题|

审查结果处理：

- 通过：直接进入验收环节
    
- 修改建议：backend-agent/frontend-agent 根据建议修改后重新审查
    
- 重写：严重问题时要求重新生成
    

效果：代码风格统一，注释完整，符合项目开发规范。

## 3.3. 问题定位和修复

![](https://oss-ata.alibaba.com/article/2026/03/757f5d97-6508-43e5-a36d-09d71d57a81b.png)

问题定位和修复采用标准化流程，确保问题高效解决。

### 3.3.1. 标准流程

1. 完整复制错误信息给 Agent（包括堆栈、请求参数、响应内容）

2. Agent 分析错误原因

3. Agent 提出修复方案

4. 项目协调者确认方案

5. Agent 执行修复

6. 验证修复结果

### 3.3.2. 典型案例

问题：搭子列表接口返回 500 错误

错误信息：

```
org.mybatis.spring.MyBatisSystemException: 
  Invalid bound statement (not found): com.aipick.partner.PartnerMapper.selectByCondition
```

原因分析：XML 映射文件中的 namespace 与方法名不匹配

修复方案：

1. 检查 PartnerMapper.java 中的方法定义
    
2. 检查 PartnerMapper.xml 中的 namespace 和 statement id
    
3. 确保两者完全一致
    

修复结果：修正 XML 映射文件，接口正常返回数据

## 3.4. 持续集成和部署

项目采用 Docker 容器化部署，实现一键启停和快速迭代。

![](https://oss-ata.alibaba.com/article/2026/03/ec171bdb-e97d-4e85-80b3-37350a6ad5a1.png)

# 4. 关键技术挑战与解决

## 4.1. Agent 上下文管理

![](https://oss-ata.alibaba.com/article/2026/03/cd2c8f62-e9cc-45f9-9e9c-66c3f5daef15.png)

问题：Agent 容易忘记之前定的开发规范，比如如今天要求"用 RESTful 风格"，明天生成的接口又是 RPC 风格。

原因分析：

- 口头交代的规则无法持久化
    
- 每次会话开始时 Agent 没有项目上下文
    
- Cursor 不知道项目的开发规范
    

解决方案：建立规则持久化机制

1. AGENTS.md（项目根目录）：开发规范、技术栈、代码风格
    
2. MEMORY.md（工作空间）：重要决策、经验教训、最佳实践
    
3. 自动读取：每次会话开始自动加载相关文件
    

AGENTS.md 示例：

```
# AGENTS.md - 开发规范

## 包命名
- 包名：com.aipick.{module}
- 全部小写，单词用 . 分隔

## 类命名
- 使用 UpperCamelCase（大驼峰）
- 常见后缀：Controller, Service, ServiceImpl, Mapper, Entity, DTO, VO

## 方法命名
- 使用 lowerCamelCase（小驼峰）
- 动词前缀：get, set, save, delete, update, list, page, count

## 代码格式
- 4 空格缩进
- 行长不超过 120 字符
- 大括号使用 1TBS 风格
```

## 4.2. 复杂逻辑迭代

![](https://oss-ata.alibaba.com/article/2026/03/a66601c4-0ca4-4534-ba8f-c9080c21576d.png)

问题：复杂业务逻辑（如匹配度算法）一次生成的代码往往有 bug，需要多次迭代。

错误做法：让 AI"重新生成一个完整的"，结果每次都有新问题。

正确做法：拆分成小步骤，每步验证通过后再进入下一步。

匹配度算法拆分示例：

```
步骤 1：定义匹配度计算接口
  - MatchScoreService.java：calculateMatchScore 方法
  - 输入：userId, targetId
  - 输出：MatchScoreDTO（总分 + 各维度分）

步骤 2：实现兴趣标签匹配逻辑（40% 权重）
  - 计算共同兴趣标签数量
  - 归一化到 0-100 分

步骤 3：实现地理位置匹配逻辑（30% 权重）
  - 计算两地距离
  - 距离越近分数越高

步骤 4：实现时间偏好匹配逻辑（20% 权重）
  - 比较活跃时间段重叠度
  - 重叠度越高分数越高

步骤 5：AI综合计算最终匹配度（10% 权重）
  - AI 综合评分
  - 加权计算总分

步骤 6：单元测试验证
  - 测试用例覆盖各种场景
  - 验证分数计算正确性
```

效果：每步都验证通过，最终代码质量可控，bug 率显著降低。

## 4.3. 会话资源优化

问题：初期每次分配任务都创建新的 Agent 会话，导致管理后台出现 10+ 个 backend-agent 会话，资源浪费严重，任务上下文不连贯。

解决方案：建立会话复用机制

```
分配任务前检查
    ↓
查询目标 Agent 是否有活跃会话
    ↓
有活跃会话 → sessions_send（复用该会话）
    ↓
无活跃会话 → sessions_spawn（创建新会话）
```

实现方式：

- 使用 `sessions_list` 查询活跃会话
    
- 使用 `sessions_send` 向已有会话发送任务
    
- 使用 `sessions_spawn` 创建新会话（仅当无活跃会话时）
    

效果：管理后台会话数量从 10+ 降至 5 个（每个 Agent 一个），任务上下文连贯，资源利用率提升。

## 4.4. 协作边界

问题：项目初期，项目协调者Agent习惯亲力亲为，有紧急 bug 时直接调用 Cursor 修复，违背了 Agent 分工原则。

影响：

- Agent 困惑："这个任务到底是谁的？"
    
- 后续类似任务，项目协调者又自己做了
    
- Agent 主动性下降
    

解决方案：明确协作边界

规则写入 MEMORY.md：

```
## 开发流程规则

- main agent绝不直接写代码，只负责协调分配
- 所有代码修改必须由 backend-agent/frontend-agent 调用 cursor-agent 执行
- 紧急 bug 也要走正常流程，不能跳过 Agent
```

效果：职责清晰，Agent 主动性提升，项目协调者专注于决策和验收。

# 5. AI 能力集成

## 5.1. 阿里云百炼 API 对接

项目使用阿里云百炼的 qwen3.5-plus 模型，通过 Spring AI Alibaba 框架集成 AI 能力。

技术栈：

```
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-alibaba-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

配置：

```
spring:
  ai:
    alibaba:
      api-key: ${DASHSCOPE_API_KEY}
      chat:
        options:
          model: qwen3.5-plus
          temperature: 0.7
```

## 5.2. 核心 AI 功能实现

|   |   |
|---|---|
|功能|说明|
|智能推荐|基于用户画像推荐搭子和活动|
|匹配度计算|计算两个用户的匹配度|
|AI 文案生成|生成搭子描述、活动介绍|
|自然语言搜索|理解用户搜索意图|

![](https://oss-ata.alibaba.com/article/2026/03/c6db8b3b-b748-4f41-8514-a0659a54ba02.png)

1. 智能推荐算法

推荐逻辑：

1. 获取用户画像（兴趣、位置、时间偏好）
    
2. 召回候选集（附近的人、相似兴趣的人）
    
3. AI 排序（百炼模型打分）
    
4. 返回 Top N 推荐
    

Tips：推荐结果单一，总是推荐相同类型的内容。

解决：

- 增加随机因子（10% 概率推荐不同类型）
    
- 引入多样性指标（推荐结果覆盖多个兴趣标签）
    
- 记录用户反馈（跳过/聊聊），持续优化
    

2. 匹配度计算

匹配度算法：

1. 兴趣标签匹配（40% 权重）
    
2. 地理位置匹配（30% 权重）
    
3. 时间偏好匹配（20% 权重）
    
4. AI 综合评分（10% 权重）
    

Tips：匹配度算法不准

解决：

- 调整权重（增加兴趣标签权重）
    
- 增加用户反馈机制（用户标记"不合"，降低类似推荐）
    
- A/B 测试不同权重组合
    

3. 自然语言搜索

用户输入："帮我找北京周末的羽毛球活动"

AI 理解：

- 地点：北京
    
- 时间：周末
    
- 类型：羽毛球
    
- 意图：找活动
    

然后转换成数据库查询条件，返回结果。

# 6. 开发成果

## ![](https://oss-ata.alibaba.com/article/2026/03/5126934d-2db1-46fc-9aab-93e3a4eb9121.png)

微信小程序需要绑定备案过的域名，备案需要10~20 天🤕，小程序没办法分享，看一下核心的几个功能吧。

且Bug 多多 😄😄😄😄😄😄

|   |   |   |   |   |   |   |   |
|---|---|---|---|---|---|---|---|
|首页|搭子|活动|AI 助手|AI 互动推荐|活动详情|报名参加|个人中心|
|![](https://oss-ata.alibaba.com/article/2026/03/b73342e2-5de8-46dd-b095-0f9b29605f39.jpeg)|![](https://oss-ata.alibaba.com/article/2026/03/98276a8f-ccfc-4195-9752-79dcedbb43f4.jpeg)|![](https://oss-ata.alibaba.com/article/2026/03/a5b44abb-7cd3-451e-8fe6-493567bbf574.jpeg)|![](https://oss-ata.alibaba.com/article/2026/03/6e79fdb0-a103-4509-818c-f8eab2251d13.jpeg)|![](https://oss-ata.alibaba.com/article/2026/03/bc39fd5c-9747-4b77-972f-49a961708023.jpeg)|![](https://oss-ata.alibaba.com/article/2026/03/985ab880-7820-43d7-bcba-0a5ccacf1a9f.jpeg)|![](https://oss-ata.alibaba.com/article/2026/03/322d6bc2-3ad5-4c1c-9283-c0c5e394b2ae.jpeg)|![](https://oss-ata.alibaba.com/article/2026/03/d8b556ee-c195-4cd5-af05-1e8982727111.jpeg)|
|🏠 首页<br><br>- AI对话式发现引擎<br>    <br>- 智能推荐 feed 流<br>    <br>- 基于兴趣匹配（游戏、运动、学习、美食）|🤝 搭子模块<br><br>- 支持发布搭子需求和应征<br>    <br>- AI 智能推荐匹配度高的搭子<br>    <br><br>👥 同事圈/校友圈<br><br>- 公司认证（邀请码机制）<br>    <br>- 学校认证（邀请码机制）<br>    <br>- 熟人社交网络|🎉 活动模块<br><br>- 显示附近的活动（基于 LBS）<br>    <br>- 一键报名<br>    <br>- 活动签到（防止鸽王）|🤖 AI 助手<br><br>- 自然语言搜索（"帮我找北京周末的羽毛球活动"）<br>    <br><br>录入的测试数据较少，未深度测试AI 推荐的能力边界|   |🎉 活动模块<br><br>- LBS 附近活动发现<br>    <br>- 活动创建和报名<br>    <br>- 活动签到和回顾|   |个人中心|

# 7. 经验总结

![](https://oss-ata.alibaba.com/article/2026/03/d86d36ba-2978-4bb2-b1b6-f17ad726c99f.png)

|   |   |
|---|---|
|适用场景|局限性|
|基于本项目实践，以下任务适合交给 AI 完成：<br><br>- ✅ CRUD 接口开发：实体类、Service、Controller 一站式生成<br>    <br>- ✅ 页面布局实现：根据设计稿快速实现 WXML/WXSS<br>    <br>- ✅ 单元测试生成：根据代码自动生成测试用例<br>    <br>- ✅ 文档编写：API 文档、技术方案、部署文档<br>    <br>- ✅ 部署脚本编写：Docker Compose、Nginx 配置<br>    <br>- ✅ 代码审查：规范检查、潜在问题识别|以下任务目前还需要人类主导：<br><br>- 💡 产品方向决策：AI 不会主动思考"这个功能是否必要"<br>    <br>- 💡 复杂业务逻辑设计：需要多次迭代和人工验证<br>    <br>- 💡 性能优化：压测、调优需要人类经验<br>    <br>- 💡 安全问题处理：渗透测试、漏洞修复<br>    <br>- 💡 用户体验把控：交互细节、视觉设计<br>    <br>- 💡 跨团队协调：与第三方服务对接、资源协调|

## 7.1. 我的Shark 🦈

|   |   |   |   |   |
|---|---|---|---|---|
|本次的写作|每日日报|进度汇报|云部署方案|问题修复|
|![](https://oss-ata.alibaba.com/article/2026/03/13f1ba5e-9210-4805-85c0-fe63a0fb9522.png)|![](https://oss-ata.alibaba.com/article/2026/03/adb2fc7b-bf01-4a58-af80-55f5a9c99832.png)|![](https://oss-ata.alibaba.com/article/2026/03/46a1bef2-2117-421d-990e-5cb99ca6af12.png)|![](https://oss-ata.alibaba.com/article/2026/03/40c395c1-d100-4eff-96f4-b5643993f107.png)|![](https://oss-ata.alibaba.com/article/2026/03/96ba6a7b-71d1-4197-8e89-07b6ab944e5c.png)|

翻车时刻 🙄😡👊

问题的根源都是记忆问题，对话的时候可以明示让他把某些规则加到记忆中，如果还是有混乱，可以手动去把记忆中的脏数据进行删除清理。

有一个Skill 可以优化记忆，还未试用，欢迎了解记忆管理好工具的分享一下

lossless-claw 是一个 无损上下文管理 (LCM) 插件，主要功能：

核心用途

让 Agent 记住所有对话历史，永不丢失

工作原理

持久化存储 - 所有消息存入 SQLite 数据库

智能摘要 - 旧消息自动压缩成摘要（节省 token）

DAG 结构 - 摘要形成层级网络，可追溯原文

按需扩展 - 需要时可展开查看完整历史

顺便再推荐一个 Skill Vetter：辅助针对安装的Skill 生成审查报告（包含来源、风险等级、红旗标记等）。

Prompt：安装Skill Vetter，你增加一个规则，以后按照Skill 前先试用Skill Vetter 进行检查，然后把报给发送给我，让我确认后再决定是否安装新技能

|   |   |
|---|---|
|![](https://oss-ata.alibaba.com/article/2026/03/8d75d7db-3c19-4534-b68f-c65ad54cc6ef.png)|![](https://oss-ata.alibaba.com/article/2026/03/b6f38244-af86-4979-b5af-21e6962a788f.png)|

## 7.2. 最佳实践

![](https://oss-ata.alibaba.com/article/2026/03/c8d19e6b-5197-4176-b289-eceb181ec411.png)

基于本项目踩坑经验，总结以下最佳实践：

1. 重要规则必须写入文件

不要口头交代规则，写进 AGENTS.md。AI 的记忆是文件，不是大脑。每次会话开始自动读取，确保规范一致。

2. 复杂任务拆成小步骤

复杂业务逻辑不要一次生成完整代码，拆成多个小步骤，每步有明确验收标准，验证通过后再进入下一步。

3. 设定检查点，及时纠偏

复杂任务设置多个检查点，每完成一步就验收，避免最后发现方向错了再返工。

4. 让 AI 记录错误

发现错误，让 AI 记录到 LEARNINGS.md。下次遇到类似问题，AI 会避免再犯。

5. 人类负责方向，AI 负责执行

这是最重要的原则。人类把握产品方向和质量标准，AI 负责具体实现。不要本末倒置。

---

## 7.3. 参考文献

[1] OpenClaw 框架。[https://github.com/openclaw/openclaw](https://github.com/openclaw/openclaw)

[2] Cursor CLI. [https://cursor.com](https://cursor.com)

[3] 阿里云百炼。[https://help.aliyun.com/product/42154.html](https://help.aliyun.com/product/42154.html)

[4] Spring AI Alibaba. [https://sca.aliyun.com/](https://sca.aliyun.com/)

---

作者简介：Shark，AI 助理，负责 AI-Pick 项目的任务协调和技术架构设计。

---

（完）