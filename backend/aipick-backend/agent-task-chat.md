# 用户间私信功能开发任务

## 项目路径
/Users/yanleishi/AI/project/OpenClaw/backend/aipick-backend

## 需求
实现用户间私信功能的后端接口

## 需要开发的接口

1. **GET /api/chat/history/{userId}**
   - 获取与指定用户的聊天记录
   - 支持分页（pageNum, pageSize）
   - 返回：消息列表（id, content, senderId, receiverId, createTime, isRead）

2. **POST /api/chat/send**
   - 发送私信
   - 请求参数：receiverId（接收者ID）, content（消息内容）
   - 返回：消息ID、发送时间

3. **GET /api/chat/unread/count**
   - 获取未读消息总数
   - 返回：未读消息数量

4. **PUT /api/chat/read/{userId}**
   - 标记与某用户的聊天记录为已读
   - 参数：userId（对方用户ID）

## 数据库表
检查是否有 t_chat_message 表，如果没有需要创建：
```sql
CREATE TABLE t_chat_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  sender_id BIGINT NOT NULL COMMENT '发送者ID',
  receiver_id BIGINT NOT NULL COMMENT '接收者ID',
  content VARCHAR(500) NOT NULL COMMENT '消息内容',
  is_read TINYINT DEFAULT 0 COMMENT '是否已读 0-未读 1-已读',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0
);
```

## 需要创建的文件
- ChatController.java - 在 /src/main/java/com/aipick/controller/ 下创建或修改
- ChatService.java / ChatServiceImpl.java - 在 /src/main/java/com/aipick/service/ 和 /src/main/java/com/aipick/service/impl/ 下创建或修改
- ChatMessageMapper.java - 在 /src/main/java/com/aipick/mapper/ 下创建或修改
- UserChatMessage.java（实体类） - 在 /src/main/java/com/aipick/entity/ 下创建，注意区分现有的 AI ChatMessage 实体
- ChatMessageRequest.java / ChatMessageResponse.java（DTO） - 在 /src/main/java/com/aipick/dto/ 下创建

## 注意事项
1. 项目中已存在 ChatController.java、ChatService.java、ChatMessage.java 和 ChatMessageMapper.java，但这些是用于 AI 对话功能的
2. 用户间私信功能需要新建实体类（如 UserChatMessage.java）和新的 Mapper（如 UserChatMessageMapper.java）
3. 参考现有的 FollowController.java 和 FollowServiceImpl.java 的代码风格
4. 使用 X-User-Id header 获取当前登录用户ID
5. 所有接口路径以 /api 开头（Spring Boot 已配置 context-path: /api）
6. 使用 MyBatis-Plus 进行数据库操作
7. 使用 BaseEntity 作为实体基类
8. 使用 Result 类统一返回结果

## 现有代码参考

### BaseEntity.java
```java
package com.aipick.common;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import java.io.Serializable;
import java.time.LocalDateTime;

public class BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableLogic
    private Integer deleted;
    // getters and setters...
}
```

### Result.java
```java
package com.aipick.common;
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    public static <T> Result<T> success(T data) { return new Result<>(0, "操作成功", data); }
    public static <T> Result<T> error(String message) { return new Result<>(500, message); }
    // ...
}
```
