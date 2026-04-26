-- MySQL dump 10.13  Distrib 8.0.45, for Linux (aarch64)
--
-- Host: localhost    Database: sparklink
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `t_activity`
--

DROP TABLE IF EXISTS `t_activity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `description` text NOT NULL,
  `type` tinyint NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `register_end_time` datetime DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `location` varchar(200) DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL,
  `longitude` decimal(10,7) DEFAULT NULL,
  `fee` decimal(10,2) DEFAULT '0.00',
  `max_participants` int DEFAULT '0',
  `current_participants` int DEFAULT '0',
  `cover_image` varchar(500) DEFAULT NULL,
  `images` text COMMENT 'å¤šå›¾JSONæ•°ç»„ï¼Œé¦–å¼ ä¸ºå°é¢',
  `status` tinyint DEFAULT '0',
  `view_count` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_activity_participant`
--

DROP TABLE IF EXISTS `t_activity_participant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_activity_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL COMMENT 'æ´»åŠ¨ID',
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `status` int DEFAULT '0' COMMENT 'çŠ¶æ€(0-å·²æŠ¥å/1-å·²ç­¾åˆ°/2-å·²å–æ¶ˆ)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æ´»åŠ¨å‚ä¸Žè€…è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_activity_registration`
--

DROP TABLE IF EXISTS `t_activity_registration`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_activity_registration` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `message` varchar(500) DEFAULT NULL,
  `status` tinyint DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `deleted` tinyint DEFAULT '0',
  `check_in_time` datetime DEFAULT NULL COMMENT 'ç­¾åˆ°æ—¶é—´',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=72 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_activity_remind`
--

DROP TABLE IF EXISTS `t_activity_remind`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_activity_remind` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `activity_id` bigint NOT NULL COMMENT 'æ´»åŠ¨ID',
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `remind_time` datetime NOT NULL COMMENT 'æé†’æ—¶é—´',
  `is_reminded` tinyint DEFAULT '0' COMMENT 'æ˜¯å¦å·²æé†’ 0-æœªæé†’ 1-å·²æé†’',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `deleted` tinyint DEFAULT '0' COMMENT 'åˆ é™¤æ ‡è®° 0-æ­£å¸¸ 1-åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_activity_id` (`activity_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_remind_time` (`remind_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æ´»åŠ¨æé†’è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_chat_message`
--

DROP TABLE IF EXISTS `t_chat_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_chat_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `session_id` varchar(50) NOT NULL COMMENT 'ä¼šè¯ID',
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `type` tinyint NOT NULL COMMENT 'æ¶ˆæ¯ç±»åž‹ 1-ç”¨æˆ· 2-AI',
  `content` text NOT NULL COMMENT 'æ¶ˆæ¯å†…å®¹',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `create_by` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `update_by` bigint DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `deleted` tinyint DEFAULT '0' COMMENT 'åˆ é™¤æ ‡è®° 0-æ­£å¸¸ 1-åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=86 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AIå¯¹è¯æ¶ˆæ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_company`
--

DROP TABLE IF EXISTS `t_company`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_company` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL COMMENT 'å…¬å¸åç§°',
  `invite_code` varchar(20) DEFAULT NULL COMMENT 'é‚€è¯·ç ',
  `member_count` int DEFAULT '0' COMMENT 'æˆå‘˜æ•°é‡',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å…¬å¸è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_conversation`
--

DROP TABLE IF EXISTS `t_conversation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_conversation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `user_id1` bigint NOT NULL COMMENT 'ç”¨æˆ·1 ID',
  `user_id2` bigint NOT NULL COMMENT 'ç”¨æˆ·2 ID',
  `last_message_id` bigint DEFAULT NULL COMMENT 'æœ€åŽä¸€æ¡æ¶ˆæ¯ID',
  `unread_count1` int DEFAULT '0' COMMENT 'æœªè¯»æ•°é‡ï¼ˆç”¨æˆ·1ï¼‰',
  `unread_count2` int DEFAULT '0' COMMENT 'æœªè¯»æ•°é‡ï¼ˆç”¨æˆ·2ï¼‰',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `create_by` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `update_by` bigint DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `deleted` tinyint DEFAULT '0' COMMENT 'åˆ é™¤æ ‡è®° 0-æ­£å¸¸ 1-åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_user_id1` (`user_id1`),
  KEY `idx_user_id2` (`user_id2`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ç”¨æˆ·ä¼šè¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_favorite`
--

DROP TABLE IF EXISTS `t_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `user_id` bigint NOT NULL COMMENT 'ç”¨æˆ·ID',
  `target_type` tinyint NOT NULL COMMENT 'ç›®æ ‡ç±»åž‹ 1-æ´»åŠ¨ 2-æ­å­',
  `target_id` bigint NOT NULL COMMENT 'ç›®æ ‡IDï¼ˆæ´»åŠ¨IDæˆ–æ­å­IDï¼‰',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `create_by` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `update_by` bigint DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `deleted` tinyint DEFAULT '0' COMMENT 'åˆ é™¤æ ‡è®° 0-æ­£å¸¸ 1-åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_target` (`user_id`,`target_type`,`target_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_target_type` (`target_type`),
  KEY `idx_target_id` (`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æ”¶è—è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_follow`
--

DROP TABLE IF EXISTS `t_follow`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `user_id` bigint NOT NULL COMMENT 'å…³æ³¨è€…ID',
  `follow_user_id` bigint NOT NULL COMMENT 'è¢«å…³æ³¨è€…ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `create_by` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `update_by` bigint DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `deleted` tinyint DEFAULT '0' COMMENT 'åˆ é™¤æ ‡è®° 0-æ­£å¸¸ 1-åˆ é™¤',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_follow` (`user_id`,`follow_user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_follow_user_id` (`follow_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å…³æ³¨è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_message`
--

DROP TABLE IF EXISTS `t_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL COMMENT 'å‘é€è€…ID',
  `to_user_id` bigint NOT NULL COMMENT 'æŽ¥æ”¶è€…ID',
  `content` text COMMENT 'æ¶ˆæ¯å†…å®¹',
  `type` int DEFAULT '0' COMMENT 'æ¶ˆæ¯ç±»åž‹(0-æ–‡æœ¬/1-å›¾ç‰‡/2-è¯­éŸ³)',
  `is_read` tinyint DEFAULT '0' COMMENT 'æ˜¯å¦å·²è¯»',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_from_user_id` (`from_user_id`),
  KEY `idx_to_user_id` (`to_user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='æ¶ˆæ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_partner`
--

DROP TABLE IF EXISTS `t_partner`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_partner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) NOT NULL,
  `content` text NOT NULL,
  `preference` varchar(512) DEFAULT NULL COMMENT '搭子偏好，逗号分隔标签',
  `scope` tinyint NOT NULL DEFAULT '1' COMMENT '可见范围 1公开 2同事 3校友',
  `type` tinyint NOT NULL,
  `target_count` int DEFAULT '2',
  `current_count` int DEFAULT '0',
  `location` varchar(200) DEFAULT NULL,
  `latitude` decimal(10,7) DEFAULT NULL COMMENT 'çº¬åº¦ GCJ-02',
  `longitude` decimal(11,7) DEFAULT NULL COMMENT 'ç»åº¦ GCJ-02',
  `plan_time` datetime DEFAULT NULL,
  `plan_end_time` datetime DEFAULT NULL COMMENT '计划/活动结束时间',
  `cover_image` varchar(500) DEFAULT NULL,
  `status` tinyint DEFAULT '0',
  `view_count` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_partner_apply`
--

DROP TABLE IF EXISTS `t_partner_apply`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_partner_apply` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `partner_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `message` varchar(500) DEFAULT NULL,
  `status` tinyint DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_partner_id` (`partner_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_school`
--

DROP TABLE IF EXISTS `t_school`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_school` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) DEFAULT NULL COMMENT 'å­¦æ ¡åç§°',
  `invite_code` varchar(20) DEFAULT NULL COMMENT 'é‚€è¯·ç ',
  `member_count` int DEFAULT '0' COMMENT 'æˆå‘˜æ•°é‡',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='å­¦æ ¡è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_user`
--

DROP TABLE IF EXISTS `t_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `nickname` varchar(50) DEFAULT NULL,
  `avatar` varchar(500) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `gender` tinyint DEFAULT '0',
  `bio` varchar(500) DEFAULT NULL,
  `status` tinyint DEFAULT '0',
  `openid` varchar(100) DEFAULT NULL,
  `company_name` varchar(100) DEFAULT NULL COMMENT 'å…¬å¸åç§°',
  `school_name` varchar(100) DEFAULT NULL COMMENT 'å­¦æ ¡åç§°',
  `birthday` varchar(20) DEFAULT NULL COMMENT 'ç”Ÿæ—¥ yyyy-MM-dd',
  `tags` varchar(500) DEFAULT NULL COMMENT 'å…´è¶£æ ‡ç­¾ JSON æ•°ç»„',
  `location` varchar(200) DEFAULT NULL COMMENT '常驻/当前位置(城市或lat,lon GCJ-02)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `create_by` bigint DEFAULT NULL,
  `update_by` bigint DEFAULT NULL,
  `deleted` tinyint DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `idx_username` (`username`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `t_user_message`
--

DROP TABLE IF EXISTS `t_user_message`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `t_user_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ä¸»é”®ID',
  `conversation_id` bigint NOT NULL COMMENT 'ä¼šè¯ID',
  `sender_id` bigint NOT NULL COMMENT 'å‘é€è€…ID',
  `receiver_id` bigint NOT NULL COMMENT 'æŽ¥æ”¶è€…ID',
  `type` varchar(20) DEFAULT 'text' COMMENT 'æ¶ˆæ¯ç±»åž‹ text/image/location',
  `content` text NOT NULL COMMENT 'æ¶ˆæ¯å†…å®¹',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT 'çº¬åº¦',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT 'ç»åº¦',
  `is_read` tinyint DEFAULT '0' COMMENT 'æ˜¯å¦å·²è¯» 0-æœªè¯» 1-å·²è¯»',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT 'åˆ›å»ºæ—¶é—´',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'æ›´æ–°æ—¶é—´',
  `create_by` bigint DEFAULT NULL COMMENT 'åˆ›å»ºäºº',
  `update_by` bigint DEFAULT NULL COMMENT 'æ›´æ–°äºº',
  `deleted` tinyint DEFAULT '0' COMMENT 'åˆ é™¤æ ‡è®° 0-æ­£å¸¸ 1-åˆ é™¤',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_receiver_id` (`receiver_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='ç”¨æˆ·ç§ä¿¡æ¶ˆæ¯è¡¨';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-26 10:05:24
