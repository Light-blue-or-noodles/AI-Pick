# AI-Pick 内容安全审核配置指南

## 混合审核策略

本项目采用混合审核策略，兼顾成本和效果：

| 内容类型 | 审核策略 | 说明 |
|---------|---------|------|
| **文本** | 本地敏感词库 → 阿里云复核 | 先用本地 Trie 树快速过滤，疑似违规再调阿里云 API |
| **图片** | 阿里云内容安全 API | 必须调用阿里云 API 进行图片审核 |

---

## 本地敏感词库

### 词库位置
```
src/main/resources/sensitive/sensitive_words.txt
```

### 支持的检测规则
- ✅ **精确匹配**：完整词汇匹配
- ✅ **前缀匹配**：以敏感词开头
- ✅ **后缀匹配**：以敏感词结尾
- ✅ **中间匹配**：包含敏感词
- ✅ **变体识别**：自动处理大小写、特殊符号干扰

### 词库格式
```
# 以#开头的行为注释
# 每行一个词或短语

# 政治敏感
反党
反动
...

# 色情低俗
色情
...
```

---

## 阿里云内容安全配置

### 1. 申请服务

访问以下链接申请阿里云内容安全服务：

- **产品首页**：https://www.aliyun.com/product/lvwang
- **开通服务**：登录阿里云控制台 → 搜索"内容安全" → 立即开通
- **获取 AccessKey**：https://ram.console.aliyun.com/manage/ak

### 2. 配置环境变量

在服务器上配置以下环境变量：

```bash
# 启用阿里云内容安全
export ALIYUN_CONTENT_SECURITY_ENABLED=true

# AccessKey（从阿里云控制台获取）
export ALIYUN_ACCESS_KEY_ID=your-access-key-id
export ALIYUN_ACCESS_KEY_SECRET=your-access-key-secret

# 地域（默认杭州）
export ALIYUN_REGION=cn-hangzhou

# 文本复核策略：本地检测通过后是否调用阿里云复核
export ALIYUN_TEXT_REVIEW_ON_SUSPICIOUS=true

# 审核阈值（0-100，分数越高越严格）
export ALIYUN_IMAGE_REVIEW_THRESHOLD=60
export ALIYUN_TEXT_REVIEW_THRESHOLD=60
```

### 3. 重启服务

```bash
cd ~/AI/project/OpenClaw/backend/aipick-backend
./restart.sh
```

---

## API 接口

### 1. 检测文本内容

```http
POST /api/content-security/check-text
Content-Type: application/json

{
  "text": "待检测的文本内容"
}
```

响应示例：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "passed": false,
    "checkType": "sensitive",
    "message": "内容包含敏感词: 赌博",
    "sensitiveWord": "赌博"
  }
}
```

### 2. 检测图片内容

```http
POST /api/content-security/check-image
Content-Type: application/json

{
  "imageUrl": "https://example.com/image.jpg"
}
```

### 3. 批量检测敏感词

```http
POST /api/content-security/batch-check
Content-Type: application/json

{
  "texts": ["文本1", "文本2", "文本3"]
}
```

### 4. 获取配置状态

```http
GET /api/content-security/status
```

---

## 业务代码中使用

### 方式一：使用工具类（推荐）

```java
import com.aipick.util.ContentSecurityUtil;

// 检查文本是否安全
boolean isSafe = ContentSecurityUtil.isTextSafe("待检测文本");

// 验证文本，未通过时抛出异常
try {
    ContentSecurityUtil.validateText(content, "发布内容");
} catch (IllegalArgumentException e) {
    // 处理违规内容
}

// 检查图片
boolean imageSafe = ContentSecurityUtil.isImageSafe(imageUrl);
```

### 方式二：注入 Service

```java
import com.aipick.service.ContentSecurityService;

@Autowired
private ContentSecurityService contentSecurityService;

public void publishContent(String text, String imageUrl) {
    // 检查文本
    ContentSecurityService.ContentCheckResult textResult = 
        contentSecurityService.checkText(text);
    if (!textResult.isPassed()) {
        throw new IllegalArgumentException("文本违规: " + textResult.getMessage());
    }
    
    // 检查图片
    ContentSecurityService.ContentModerationResult imageResult = 
        contentSecurityService.checkImage(imageUrl);
    if (!imageResult.isPassed()) {
        throw new IllegalArgumentException("图片违规: " + imageResult.getLabel());
    }
}
```

---

## 计费说明

### 阿里云内容安全收费标准

| 服务类型 | 单价 | 说明 |
|---------|------|------|
| 文本审核 | 约 ¥0.0005-0.001/条 | 按调用次数计费 |
| 图片审核 | 约 ¥0.001-0.003/张 | 按调用次数计费 |
| 视频审核 | 按分钟计费 | 约 ¥0.01-0.05/分钟 |

### 免费额度
- 新用户通常有 **每月几千次** 的免费调用额度
- 具体以阿里云官方说明为准

### 成本优化建议
1. **优先使用本地敏感词库**（已配置，零成本）
2. **文本内容**：本地检测通过后再调用阿里云复核
3. **图片内容**：必须走阿里云 API，但可设置合理阈值避免过度审核

---

## 注意事项

1. **AccessKey 安全**：务必使用环境变量配置，不要提交到代码仓库
2. **敏感词维护**：定期更新本地敏感词库，保持时效性
3. **误报处理**：阿里云 API 可能产生误报，可根据业务调整阈值
4. **日志记录**：审核结果会记录到日志，便于后续分析和优化

---

## 相关链接

- [阿里云内容安全产品页](https://www.aliyun.com/product/lvwang)
- [阿里云内容安全文档](https://help.aliyun.com/document_detail/28427.html)
- [阿里云 AccessKey 管理](https://ram.console.aliyun.com/manage/ak)
- [阿里云计费详情](https://www.aliyun.com/price/product#/lvwang/detail)
