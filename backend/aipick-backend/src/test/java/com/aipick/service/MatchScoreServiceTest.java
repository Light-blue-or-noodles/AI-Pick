package com.aipick.service;

import com.aipick.dto.MatchScoreDTO;
import com.aipick.entity.User;
import com.aipick.mapper.UserMapper;
import com.aipick.service.impl.MatchScoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * MatchScoreService 单元测试
 * <p>
 * 匹配度各维度与实现一致：时间分为「重叠分钟数 / 全天 1440 分钟 × 100」。
 *
 * @author AI-Pick
 */
@ExtendWith(MockitoExtension.class)
class MatchScoreServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MatchScoreServiceImpl matchScoreService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setNickname("用户A");
        user1.setTags("[\"游戏\",\"运动\",\"音乐\"]");
        user1.setLocation("39.9042,116.4074");
        user1.setActiveTime("{\"start\":\"09:00\",\"end\":\"22:00\"}");

        user2 = new User();
        user2.setId(2L);
        user2.setNickname("用户B");
        user2.setTags("[\"游戏\",\"读书\",\"旅行\"]");
        user2.setLocation("39.9042,116.4074");
        user2.setActiveTime("{\"start\":\"10:00\",\"end\":\"21:00\"}");
    }

    @Test
    void testCalculateMatchScore_BothUsersExist() {
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(2L, result.getTargetId());
        assertNotNull(result.getTotalScore());
        assertTrue(result.getTotalScore() >= 0 && result.getTotalScore() <= 100);
    }

    @Test
    void testCalculateMatchScore_UserNotFound() {
        when(userMapper.selectById(1L)).thenReturn(null);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertNotNull(result);
        assertEquals(0, result.getTotalScore());
        assertEquals("用户不存在，无法计算匹配度", result.getReason());
    }

    @Test
    void testCalculateMatchScore_SameUser() {
        when(userMapper.selectById(1L)).thenReturn(user1);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 1L);

        assertNotNull(result);
        assertTrue(result.getInterestScore() > 0);
    }

    @Test
    void testInterestScore_FullMatch() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("[\"游戏\",\"运动\"]");

        User u2 = new User();
        u2.setId(2L);
        u2.setTags("[\"游戏\",\"运动\"]");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertEquals(100, result.getInterestScore());
    }

    @Test
    void testInterestScore_PartialMatch() {
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertTrue(result.getInterestScore() > 0 && result.getInterestScore() <= 50);
    }

    @Test
    void testInterestScore_NoMatch() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("[\"游戏\",\"运动\"]");

        User u2 = new User();
        u2.setId(2L);
        u2.setTags("[\"读书\",\"音乐\",\"电影\"]");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertEquals(0, result.getInterestScore());
    }

    @Test
    void testInterestScore_EmptyTags() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("");

        User u2 = new User();
        u2.setId(2L);
        u2.setTags("[\"游戏\"]");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertEquals(0, result.getInterestScore());
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

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

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

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

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

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertEquals(0, result.getLocationScore());
    }

    /**
     * 双方均为全天段时，重叠接近 24h，时间分应接近 100。
     */
    @Test
    void testTimeScore_FullOverlap() {
        User u1 = new User();
        u1.setId(1L);
        u1.setActiveTime("{\"start\":\"00:00\",\"end\":\"23:59\"}");

        User u2 = new User();
        u2.setId(2L);
        u2.setActiveTime("{\"start\":\"00:00\",\"end\":\"23:59\"}");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertEquals(100, result.getTimeScore());
    }

    /**
     * 相同 9:00–18:00 窗口：重叠 9h，9/24×100=37.5 → 四舍五入 38。
     */
    @Test
    void testTimeScore_SameWorkdayWindow() {
        User u1 = new User();
        u1.setId(1L);
        u1.setActiveTime("{\"start\":\"09:00\",\"end\":\"18:00\"}");

        User u2 = new User();
        u2.setId(2L);
        u2.setActiveTime("{\"start\":\"09:00\",\"end\":\"18:00\"}");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertEquals(38, result.getTimeScore());
    }

    @Test
    void testTimeScore_PartialOverlap() {
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertTrue(result.getTimeScore() > 0);
    }

    @Test
    void testTimeScore_NoOverlap() {
        User u1 = new User();
        u1.setId(1L);
        u1.setActiveTime("{\"start\":\"09:00\",\"end\":\"12:00\"}");

        User u2 = new User();
        u2.setId(2L);
        u2.setActiveTime("{\"start\":\"14:00\",\"end\":\"18:00\"}");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertEquals(0, result.getTimeScore());
    }

    @Test
    void testTimeScore_EmptyActiveTime() {
        User u1 = new User();
        u1.setId(1L);

        User u2 = new User();
        u2.setId(2L);
        u2.setActiveTime("{\"start\":\"09:00\",\"end\":\"18:00\"}");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertEquals(0, result.getTimeScore());
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

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertNotNull(result.getSuggestions());
        assertFalse(result.getSuggestions().isEmpty());
    }

    @Test
    void testSuggestions_AllGoodScores() {
        User u1 = new User();
        u1.setId(1L);
        u1.setTags("[\"游戏\",\"运动\"]");
        u1.setLocation("北京");
        u1.setActiveTime("{\"start\":\"00:00\",\"end\":\"23:59\"}");

        User u2 = new User();
        u2.setId(2L);
        u2.setTags("[\"游戏\",\"运动\"]");
        u2.setLocation("北京");
        u2.setActiveTime("{\"start\":\"00:00\",\"end\":\"23:59\"}");

        when(userMapper.selectById(1L)).thenReturn(u1);
        when(userMapper.selectById(2L)).thenReturn(u2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertTrue(result.getSuggestions().stream()
                .anyMatch(s -> s.contains("继续保持")));
    }

    @Test
    void testTotalScore_Boundary() {
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertTrue(result.getTotalScore() >= 0);
        assertTrue(result.getTotalScore() <= 100);
    }

    @Test
    void testReason_NotEmpty() {
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        MatchScoreDTO result = matchScoreService.calculateMatchScore(1L, 2L);

        assertNotNull(result.getReason());
        assertFalse(result.getReason().isEmpty());
    }
}
