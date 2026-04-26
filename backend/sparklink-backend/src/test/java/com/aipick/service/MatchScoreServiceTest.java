package com.aipick.service;

import com.sparklink.dto.MatchScoreDTO;
import com.sparklink.entity.Partner;
import com.sparklink.entity.User;
import com.sparklink.integration.DashScopeCompatClient;
import com.sparklink.mapper.PartnerMapper;
import com.sparklink.mapper.UserMapper;
import com.sparklink.service.impl.MatchScoreServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * MatchScoreService 单元测试
 * <p>
 * 有 partnerId 时：活动标签 30% + 发布者标签 10% + 活动位置 30% + AI 30%。无 partnerId 时：发布者标签 40% + 两用户位置 30% + AI 30%。时间维已移除，timeScore 恒为 0。
 */
@ExtendWith(MockitoExtension.class)
class MatchScoreServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PartnerMapper partnerMapper;

    @Mock
    private DashScopeCompatClient dashScopeCompatClient;

    private MatchScoreServiceImpl matchScoreService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        matchScoreService = new MatchScoreServiceImpl(
                userMapper, partnerMapper, new ObjectMapper(), dashScopeCompatClient);
        user1 = new User();
        user1.setId(1L);
        user1.setNickname("用户A");
        user1.setTags("[\"游戏\",\"运动\",\"音乐\"]");
        user1.setLocation("39.9042,116.4074");
        user1.setBio("喜欢户外");

        user2 = new User();
        user2.setId(2L);
        user2.setNickname("用户B");
        user2.setTags("[\"游戏\",\"读书\",\"旅行\"]");
        user2.setLocation("39.9042,116.4074");
    }

    @Test
    void testCalculateMatchScore_BothUsersExist() {
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(2L, result.getTargetId());
        assertNotNull(result.getTotalScore());
        assertTrue(result.getTotalScore() >= 0 && result.getTotalScore() <= 100);
        assertEquals(0, result.getTimeScore());
    }

    @Test
    void testCalculateMatchScore_UserNotFound() {
        when(userMapper.selectById(1L)).thenReturn(null);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertNotNull(result);
        assertEquals(0, result.getTotalScore().intValue());
        assertEquals("用户不存在，无法计算匹配度", result.getReason());
    }

    @Test
    void testCalculateMatchScore_SameUser() {
        when(userMapper.selectById(1L)).thenReturn(user1);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 1L, null);

        assertNotNull(result);
        assertTrue(result.getPublisherTagScore() > 0);
    }

    @Test
    void testPublisherTagScore_FullMatch() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("[\"游戏\",\"运动\"]");

        User u2 = new User();
        u2.setId(2L);
        u2.setTags("[\"游戏\",\"运动\"]");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertEquals(100, result.getPublisherTagScore());
        assertEquals(0, result.getActivityTagScore());
    }

    @Test
    void testPublisherTagScore_PartialMatch() {
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertTrue(result.getPublisherTagScore() > 0 && result.getPublisherTagScore() <= 100);
    }

    @Test
    void testPublisherTagScore_NoMatch() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("[\"游戏\",\"运动\"]");

        User u2 = new User();
        u2.setId(2L);
        u2.setTags("[\"读书\",\"音乐\",\"电影\"]");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertEquals(0, result.getPublisherTagScore());
    }

    @Test
    void testPublisherTagScore_EmptyTags() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("");

        User u2 = new User();
        u2.setId(2L);
        u2.setTags("[\"游戏\"]");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertEquals(0, result.getPublisherTagScore());
    }

    @Test
    void testLocationScore_SameLocation() {
        User u1 = new User();
        u1.setId(1L);
        u1.setLocation("北京");

        User u2 = new User();
        u2.setId(2L);
        u2.setLocation("北京");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertEquals(100, result.getLocationScore());
    }

    @Test
    void testLocationScore_DifferentLocation() {
        User u1 = new User();
        u1.setId(1L);
        u1.setLocation("北京");

        User u2 = new User();
        u2.setId(2L);
        u2.setLocation("上海");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertTrue(result.getLocationScore() < 100);
    }

    @Test
    void testLocationScore_EmptyLocation() {
        User u1 = new User();
        u1.setId(1L);

        User u2 = new User();
        u2.setId(2L);
        u2.setLocation("北京");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertEquals(0, result.getLocationScore());
    }

    @Test
    void testTimeScore_AlwaysZero() {
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);
        assertEquals(0, result.getTimeScore());
    }

    @Test
    void testWithPartner_ActivityAndPublisher() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("[\"游戏\",\"户外\"]");
        u1.setLocation("39.90,116.40");

        User u2 = new User();
        u2.setId(2L);
        u2.setTags("[\"游戏\",\"电影\"]");
        u2.setLocation("31.0,121.0");

        Partner p = new Partner();
        p.setId(10L);
        p.setUserId(2L);
        p.setPreference("游戏,户外,烧烤");
        p.setLatitude(39.91);
        p.setLongitude(116.41);

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);
        when(partnerMapper.selectById(10L)).thenReturn(p);

        MatchScoreDTO r = matchScoreService.calculateMatchScore(1L, 2L, 10L);
        assertNotNull(r);
        assertTrue(r.getActivityTagScore() > 0);
        assertTrue(r.getPublisherTagScore() > 0);
        assertTrue(r.getLocationScore() > 0);
    }

    @Test
    void testWithPartner_MismatchPublisher_DropsPartner() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("[\"A\"]");

        User u2 = new User();
        u2.setId(2L);
        u2.setTags("[\"A\"]");

        Partner p = new Partner();
        p.setId(99L);
        p.setUserId(99L);
        p.setPreference("x");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);
        when(partnerMapper.selectById(99L)).thenReturn(p);

        MatchScoreDTO r = matchScoreService.calculateMatchScore(1L, 2L, 99L);
        assertNotNull(r);
        assertEquals(0, r.getActivityTagScore());
    }

    @Test
    void testSuggestions_AllLowScores() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("");

        User u2 = new User();
        u2.setId(2L);

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertNotNull(result.getSuggestions());
        assertFalse(result.getSuggestions().isEmpty());
    }

    @Test
    void testSuggestions_AllGoodScores() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("[\"游戏\",\"运动\"]");
        u1.setLocation("北京");

        User u2 = new User();
        u2.setId(2L);
        u2.setTags("[\"游戏\",\"运动\"]");
        u2.setLocation("北京");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertTrue(result.getSuggestions().stream()
                .anyMatch(s -> s.contains("继续保持")));
    }

    @Test
    void testTotalScore_Boundary() {
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertTrue(result.getTotalScore() >= 0);
        assertTrue(result.getTotalScore() <= 100);
    }

    @Test
    void testReason_ContainsNoTimeRule() {
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L, null);

        assertNotNull(result.getReason());
        assertTrue(result.getReason().contains("AI 综合评分"));
    }
}
