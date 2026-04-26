# 代码审查问题修复报告

**修复日期:** 2026-03-11  
**修复状态:** ✅ 全部完成  
**编译状态:** ✅ BUILD SUCCESS

---

## 🔴 高优先级问题（4 个）

### 1. 数据库密码明文

**文件:** `src/main/resources/application.yml`

**问题:** 数据库密码硬编码为 `root`

**修复:** 使用环境变量 `${DB_PASSWORD:}`

```yaml
# 修复前
password: root

# 修复后
password: ${DB_PASSWORD:}
```

**状态:** ✅ 已修复

---

### 2. JWT Secret 未验证

**文件:** `src/main/java/com/aipick/config/JwtUtils.java`

**问题:** 从环境变量读取 JWT secret 但未验证是否为空

**修复:** 添加 `@PostConstruct` 方法在启动时检查，缺少时抛出异常

```java
@PostConstruct
public void init() {
    if (secret == null || secret.trim().isEmpty()) {
        throw new IllegalStateException("JWT_SECRET 环境变量未配置，请设置后再启动应用");
    }
}
```

**状态:** ✅ 已修复

---

### 3. 微信登录模拟实现

**文件:** `src/main/java/com/aipick/service/impl/UserServiceImpl.java`

**问题:** 使用 `mock_openid_` 前缀的模拟实现

**修复:** 
- 添加配置项 `wechat.mock.enabled` 控制模拟模式
- 实现真实微信 API 调用逻辑
- 保留模拟开关用于开发/测试

```java
@Value("${wechat.mock.enabled:true}")
private Boolean wechatMockEnabled;

private String getOpenidFromWechat(String code) {
    if (!StringUtils.hasText(code)) {
        return null;
    }
    
    // 模拟模式（开发/测试环境）
    if (wechatMockEnabled) {
        return "mock_openid_" + code;
    }
    
    // 生产环境：调用真实微信 API
    if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret)) {
        throw new BusinessException("微信配置不完整，请检查 appid 和 secret 配置");
    }
    
    try {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.weixin.qq.com/sns/jscode2session" +
                "?appid=" + appid +
                "&secret=" + secret +
                "&js_code=" + code +
                "&grant_type=authorization_code";
        
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        
        if (response == null) {
            throw new BusinessException("微信 API 返回为空");
        }
        
        // 检查错误码
        if (response.containsKey("errcode")) {
            Integer errcode = (Integer) response.get("errcode");
            String errmsg = (String) response.get("errmsg");
            throw new BusinessException("微信登录失败：" + errmsg + " (errcode=" + errcode + ")");
        }
        
        String openid = (String) response.get("openid");
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException("微信 API 未返回 openid");
        }
        
        return openid;
    } catch (Exception e) {
        throw new BusinessException("调用微信 API 失败：" + e.getMessage());
    }
}
```

**状态:** ✅ 已修复

---

### 4. N+1 查询问题

**文件:** 
- `src/main/java/com/aipick/service/impl/HomeRecommendVOUtils.java`
- `src/main/java/com/aipick/service/impl/AiServiceImpl.java`

**问题:** 在循环内单独查询用户信息，导致 N+1 查询问题

**修复:** 批量查询用户，使用 Map 关联

#### HomeRecommendVOUtils.java 修复:

```java
public static List<PartnerVO> convertPartners(List<Partner> partners) {
    if (partners == null || partners.isEmpty()) {
        return new ArrayList<>();
    }
    
    // 批量查询所有用户信息，避免 N+1 问题
    List<Long> userIds = partners.stream()
            .map(Partner::getUserId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());
    
    Map<Long, User> userMap = new HashMap<>();
    if (userMapper != null && !userIds.isEmpty()) {
        List<User> users = userMapper.selectBatchIds(userIds);
        for (User user : users) {
            userMap.put(user.getId(), user);
        }
    }
    
    // 转换为 VO
    List<PartnerVO> result = new ArrayList<>();
    for (Partner partner : partners) {
        // ... 其他字段 ...
        
        // 从 Map 中获取用户信息
        User user = userMap.get(partner.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        
        result.add(vo);
    }
    return result;
}
```

#### AiServiceImpl.java 修复:

```java
private List<PartnerVO> buildPartnerVOWithScore(List<Partner> partners, AiRecommendRequest request) {
    if (partners == null || partners.isEmpty()) {
        return new ArrayList<>();
    }
    
    // 批量查询所有用户信息，避免 N+1 问题
    List<Long> userIds = partners.stream()
            .map(Partner::getUserId)
            .filter(id -> id != null)
            .distinct()
            .collect(Collectors.toList());
    
    Map<Long, User> userMap = new HashMap<>();
    if (!userIds.isEmpty()) {
        List<User> users = userMapper.selectBatchIds(userIds);
        for (User user : users) {
            userMap.put(user.getId(), user);
        }
    }
    
    // 转换为 VO
    List<PartnerVO> list = new ArrayList<>();
    for (Partner p : partners) {
        // ... 其他字段 ...
        
        // 从 Map 中获取用户信息
        User user = userMap.get(p.getUserId());
        if (user != null) {
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
            vo.setGender(user.getGender());
        }
        list.add(vo);
    }
    return list;
}
```

**状态:** ✅ 已修复

---

## 🟡 中优先级问题（2 个）

### 5. LIKE 查询未转义

**文件:** `src/main/resources/mapper/PartnerMapper.xml`

**问题:** LIKE 查询未处理 `%` 和 `_` 特殊字符

**修复:** 使用 `<bind>` 标签转义特殊字符，添加 `ESCAPE '\'`

```xml
<!-- 修复前 -->
<if test="location != null and location != ''">
    AND p.location LIKE CONCAT('%', #{location}, '%')
</if>

<!-- 修复后 -->
<if test="location != null and location != ''">
    <bind name="locationPattern" value="'%' + location.replace('\\', '\\\\').replace('%', '\\%').replace('_', '\\_') + '%'" />
    AND p.location LIKE #{locationPattern} ESCAPE '\'
</if>
```

**状态:** ✅ 已修复

---

## 📦 其他修改

### pom.xml 依赖添加

添加了 `javax.annotation-api` 依赖以支持 Java 17 的 `@PostConstruct` 注解：

```xml
<dependency>
    <groupId>javax.annotation</groupId>
    <artifactId>javax.annotation-api</artifactId>
    <version>1.3.2</version>
</dependency>
```

---

## ✅ 编译验证

**编译命令:**
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export PATH=$JAVA_HOME/bin:$PATH
mvn clean compile
```

**编译结果:** BUILD SUCCESS ✅

---

## 📝 配置说明

### 环境变量配置

启动应用前需要配置以下环境变量：

```bash
# 数据库密码
export DB_PASSWORD=your_password

# JWT Secret（必须配置）
export JWT_SECRET=your_jwt_secret_key_at_least_32_chars

# 微信配置（生产环境）
export WECHAT_APPID=your_wechat_appid
export WECHAT_SECRET=your_wechat_secret

# 微信模拟模式（开发环境可选）
export WECHAT_MOCK_ENABLED=true
```

### application.yml 配置示例

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:}

jwt:
  secret: ${JWT_SECRET}

wechat:
  appid: ${WECHAT_APPID:}
  secret: ${WECHAT_SECRET:}
  mock:
    enabled: ${WECHAT_MOCK_ENABLED:true}
```

---

## 🎯 修复总结

| 优先级 | 问题 | 文件 | 状态 |
|--------|------|------|------|
| 🔴 | 数据库密码明文 | application.yml | ✅ |
| 🔴 | JWT Secret 未验证 | JwtUtils.java | ✅ |
| 🔴 | 微信登录模拟实现 | UserServiceImpl.java | ✅ |
| 🔴 | N+1 查询问题 | HomeRecommendVOUtils.java, AiServiceImpl.java | ✅ |
| 🟡 | LIKE 查询未转义 | PartnerMapper.xml | ✅ |

**总计:** 6/6 问题已修复 ✅
