# AI-Pick 安全方案

> 版本：v1.0 | 日期：2026-03-05

---

## 一、接口安全

### 1.1 认证授权

**JWT 认证**
- 登录成功后返回 JWT Token
- Token 有效期：7 天（可配置）
- 刷新机制：Token 过期前自动刷新
- 存储方式：前端本地存储

```java
// JWT 配置
@Configuration
public class JwtConfig {
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration; // 7天 = 604800000ms
    
    // 生成 Token
    public String generateToken(Long userId) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
            .compact();
    }
    
    // 验证 Token
    public Long getUserIdFromToken(String token) {
        return Long.parseLong(Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject());
    }
}
```

### 1.2 接口限流

**限流策略**
| 接口 | 限流规则 |
|------|----------|
| 发送验证码 | 60次/手机号/分钟 |
| 登录 | 10次/手机号/分钟 |
| 发送消息 | 60次/用户/分钟 |
| 发布搭子 | 10次/用户/小时 |
| 发布活动 | 5次/用户/小时 |

```java
// 限流注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    int value() default 60;      // 默认60次
    int timeWindow() default 60; // 时间窗口（秒）
}

// 使用示例
@RateLimit(value = 10, timeWindow = 60)
@PostMapping("/login")
public Result login(@RequestBody LoginRequest request) { ... }
```

### 1.3 签名验签

**高敏感接口**（充值、提现、修改密码）需要签名：
```
签名算法：HMAC-SHA256
签名参数：timestamp + nonce + body + secret
```

```java
// 签名验证
public boolean verifySign(SignRequest request, String sign) {
    String data = request.getTimestamp() + request.getNonce() + request.getBody();
    String expectedSign = HmacSHA256(data, secretKey);
    return expectedSign.equals(sign);
}
```

---

## 二、数据安全

### 2.1 敏感数据加密

**加密字段**
| 字段 | 加密方式 | 说明 |
|------|----------|------|
| phone | AES | 手机号 |
| realName | AES | 真实姓名 |
| idCard | AES | 身份证号 |
| payPassword | BCrypt | 支付密码 |

```java
// 手机号加密存储
public String encryptPhone(String phone) {
    return AESUtil.encrypt(phone, encryptionKey);
}

public String decryptPhone(String encryptedPhone) {
    return AESUtil.decrypt(encryptedPhone, encryptionKey);
}
```

### 2.2 数据脱敏

**返回给前端的敏感数据**
| 字段 | 脱敏规则 | 示例 |
|------|----------|------|
| phone | 3-7位隐藏 | 138****5678 |
| idCard | 3-14位隐藏 | 110101****5678****01 |
| nickname | 敏感词过滤 | 过滤违规昵称 |
| message | 敏感词过滤 | 过滤违规内容 |

```java
// 手机号脱敏
public String maskPhone(String phone) {
    if (phone == null || phone.length() < 11) return phone;
    return phone.substring(0, 3) + "****" + phone.substring(7);
}

// 敏感词过滤
public String filterSensitiveWords(String content) {
    for (String word : sensitiveWords) {
        content = content.replaceAll(word, "**");
    }
    return content;
}
```

### 2.3 传输安全

- 全站 HTTPS
- API 使用 TLS 1.2+
- 证书配置 Let's Encrypt 或付费证书

---

## 三、接口安全

### 3.1 SQL 注入防护

- 使用 MyBatis-Plus 参数绑定，禁止拼接 SQL
- 启用 Wall 防火墙

```xml
<!-- MyBatis 使用 #{} 参数绑定 -->
<select id="selectByCondition">
    SELECT * FROM user
    <where>
        <if test="nickname != null and nickname != ''">
            AND nickname LIKE CONCAT('%', #{nickname}, '%')
        </if>
    </where>
</select>
```

### 3.2 XSS 防护

- 请求参数过滤特殊字符
- 输出转义 HTML 特殊字符

```java
// XSS 过滤
public String filterXss(String content) {
    if (content == null) return null;
    return content.replaceAll("<", "&lt;")
                  .replaceAll(">", "&gt;")
                  .replaceAll("\"", "&quot;")
                  .replaceAll("'", "&#x27;");
}
```

### 3.3 CORS 配置

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://aipick.example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## 四、业务安全

### 4.1 内容审核

**AI 内容审核**
- 用户发布内容（搭子描述、活动描述、聊天消息）
- 接入阿里云内容审核服务
- 敏感内容自动拦截

```java
// 内容审核服务
public boolean checkContent(String content, String type) {
    // 调用阿里云内容审核 API
    ContentModerationResult result = aliyunModeration.check(content, type);
    return result.isPass();
}
```

### 4.2 风控规则

| 场景 | 风控规则 |
|------|----------|
| 频繁登录 | 同一IP 1小时超过50次 |
| 批量注册 | 同一IP 24小时超过10个账号 |
| 频繁发送消息 | 1分钟超过60条 |
| 异常行为 | 新账号大量添加好友 |

### 4.3 举报机制

- 用户可举报搭子/活动/消息
- 举报内容人工审核
- 多次举报自动封禁

---

## 五、权限控制

### 5.1 接口权限

```java
// 需要登录
@PreAuthorize("@authService.isLogin()")
@PostMapping("/partner")
public Result<PartnerVO> createPartner(...) { ... }

// 需要特定角色
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/user/{id}")
public Result deleteUser(...) { ... }
```

### 5.2 数据权限

```java
// 只能查看自己的数据
@PreAuthorize("#userId == authentication.userId")
@GetMapping("/profile/{userId}")
public Result getProfile(@PathVariable Long userId) { ... }
```

---

## 六、安全配置

### 6.1 请求头安全

```java
@Configuration
public class SecurityHeadersConfig {
    @Bean
    public FilterRegistrationBean<HeaderSecurityFilter> headerSecurityFilter() {
        FilterRegistrationBean<HeaderSecurityFilter> registration = new FilterRegistrationBean<>();
        registration.addInitParameter("X-Content-Type-Options", "nosniff");
        registration.addInitParameter("X-Frame-Options", "DENY");
        registration.addInitParameter("X-XSS-Protection", "1; mode=block");
        registration.addInitParameter("Strict-Transport-Security", "max-age=31536000");
        return registration;
    }
}
```

### 6.2 请求体大小限制

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB      # 单个文件大小
      max-request-size: 20MB     # 请求总大小
```

---

## 七、审计日志

### 7.1 日志记录

| 操作 | 记录内容 |
|------|----------|
| 登录 | 用户ID、IP、时间、结果 |
| 修改密码 | 用户ID、IP、时间 |
| 资金操作 | 用户ID、金额、IP、时间 |
| 管理操作 | 管理员ID、操作内容、时间 |

```java
// 操作日志
@Log(operationType = "USER_LOGIN", description = "用户登录")
@PostMapping("/login")
public Result login(...) { ... }
```

---

## 八、应急响应

### 8.1 预案流程

```
发现安全事件
    ↓
评估影响范围
    ↓
启动应急预案
    ↓
止损（封IP、冻结账号）
    ↓
排查原因
    ↓
修复漏洞
    ↓
复盘总结
```

### 8.2 联系方式

| 角色 | 职责 |
|------|------|
| 安全负责人 | 统筹应急响应 |
| 开发团队 | 漏洞修复 |
| 运维团队 | 基础设施安全 |
| 客服 | 用户反馈处理 |

---

*文档版本：v1.0*
*最后更新：2026-03-05*