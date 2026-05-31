package com.sparklink.service.impl;

import com.sparklink.dto.ActivityAiRequest;
import com.sparklink.dto.AiRecommendRequest;
import com.sparklink.dto.PartnerAiRequest;
import com.sparklink.entity.Activity;
import com.sparklink.entity.Partner;
import com.sparklink.mapper.ActivityMapper;
import com.sparklink.mapper.PartnerMapper;
import com.sparklink.mapper.UserMapper;
import com.sparklink.memory.service.MemoryFacade;
import com.sparklink.service.RecommendFeedbackService;
import com.sparklink.vo.AiRecommendVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiServiceMemoryHookTest {

    @Mock
    private PartnerMapper partnerMapper;
    @Mock
    private ActivityMapper activityMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RecommendFeedbackService recommendFeedbackService;
    @Mock
    private ChatModel chatModel;
    @Mock
    private ObjectProvider<ChatModel> chatModelProvider;
    @Mock
    private MemoryFacade memoryFacade;

    @Test
    void recommend_shouldRecallMergeAndEnqueueMemoryHooks() {
        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        AiServiceImpl aiService = new AiServiceImpl(
                partnerMapper,
                activityMapper,
                userMapper,
                recommendFeedbackService,
                chatModelProvider,
                memoryFacade
        );
        AiRecommendRequest request = new AiRecommendRequest();
        request.setUserId(10086L);
        request.setPartnerLimit(1);
        request.setActivityLimit(1);

        Partner partner = new Partner();
        partner.setId(11L);
        partner.setUserId(20001L);
        partner.setStatus(0);
        partner.setScope(1);
        partner.setType(5);
        partner.setTitle("周末羽毛球");
        partner.setContent("来打球");
        partner.setPlanTime(LocalDateTime.now().plusDays(1));
        when(partnerMapper.selectList(any())).thenReturn(List.of(partner));

        Activity activity = new Activity();
        activity.setId(22L);
        activity.setUserId(30001L);
        activity.setStatus(1);
        activity.setCategory("运动");
        activity.setTitle("同城羽毛球局");
        activity.setDescription("友好局");
        activity.setStartTime(LocalDateTime.now().plusDays(2));
        activity.setLatitude(BigDecimal.valueOf(39.9));
        activity.setLongitude(BigDecimal.valueOf(116.3));
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));

        when(userMapper.selectById(10086L)).thenReturn(null);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());
        when(recommendFeedbackService.findNegativeTargetIds(any(), anyInt())).thenReturn(java.util.Set.of());
        when(recommendFeedbackService.countNegativeByPartnerType(any())).thenReturn(Map.of());
        when(recommendFeedbackService.countNegativeByActivityCategory(any())).thenReturn(Map.of());

        when(memoryFacade.recallForPrompt(eq(10086L), anyString())).thenReturn(null);
        when(memoryFacade.mergePrompt(anyString(), isNull())).thenReturn("merged-rerank-prompt");
        when(chatModel.call("merged-rerank-prompt")).thenReturn("[{\"kind\":\"partner\",\"id\":11,\"score\":88}]");

        AiRecommendVO result = aiService.recommend(request);

        InOrder ordered = inOrder(memoryFacade, chatModel);
        ordered.verify(memoryFacade).recallForPrompt(eq(10086L), anyString());
        ordered.verify(memoryFacade).mergePrompt(anyString(), isNull());
        ordered.verify(chatModel).call("merged-rerank-prompt");
        ordered.verify(memoryFacade).enqueueConversation(eq(10086L), anyString(), eq("[{\"kind\":\"partner\",\"id\":11,\"score\":88}]"));
        assertNotNull(result);
        assertEquals(1, result.getPartners().size());
    }

    @Test
    void activityAndPartnerTextService_shouldUseMemoryHooksWithRequestUserId() {
        ActivityAiTextService activityAiTextService = new ActivityAiTextService(chatModel, memoryFacade);
        PartnerAiTextService partnerAiTextService = new PartnerAiTextService(chatModel, memoryFacade);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute("userId", 9527L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        try {
            ActivityAiRequest activityReq = new ActivityAiRequest();
            activityReq.setTitle("夜跑");
            activityReq.setCategory("运动");
            activityReq.setCurrentDesc("约跑步");
            when(memoryFacade.recallForPrompt(eq(9527L), anyString())).thenReturn(null);
            when(memoryFacade.mergePrompt(anyString(), isNull())).thenAnswer(invocation -> invocation.getArgument(0));
            when(chatModel.call(anyString())).thenReturn("活动文案");

            String activityText = activityAiTextService.enhanceDescription(activityReq);
            assertEquals("活动文案", activityText);
            verify(memoryFacade).enqueueConversation(eq(9527L), anyString(), eq("活动文案"));

            PartnerAiRequest partnerReq = new PartnerAiRequest();
            partnerReq.setTitle("找球搭子");
            partnerReq.setTypeName("运动");
            partnerReq.setPreference("友好");
            String partnerText = partnerAiTextService.enhanceDescription(partnerReq);
            assertEquals("活动文案", partnerText);
            verify(memoryFacade, times(2)).enqueueConversation(eq(9527L), anyString(), eq("活动文案"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void recommend_shouldFailOpenWhenMemoryHooksThrow() {
        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        AiServiceImpl aiService = new AiServiceImpl(
                partnerMapper,
                activityMapper,
                userMapper,
                recommendFeedbackService,
                chatModelProvider,
                memoryFacade
        );
        AiRecommendRequest request = new AiRecommendRequest();
        request.setUserId(10010L);
        request.setPartnerLimit(1);
        request.setActivityLimit(1);

        Partner partner = new Partner();
        partner.setId(77L);
        partner.setUserId(20001L);
        partner.setStatus(0);
        partner.setScope(1);
        partner.setType(5);
        partner.setTitle("跑步搭子");
        partner.setContent("晨跑");
        partner.setPlanTime(LocalDateTime.now().plusDays(1));
        when(partnerMapper.selectList(any())).thenReturn(List.of(partner));

        Activity activity = new Activity();
        activity.setId(88L);
        activity.setUserId(30001L);
        activity.setStatus(1);
        activity.setCategory("运动");
        activity.setTitle("周末跑步局");
        activity.setDescription("轻松跑");
        activity.setStartTime(LocalDateTime.now().plusDays(1));
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));

        when(userMapper.selectById(10010L)).thenReturn(null);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());
        when(recommendFeedbackService.findNegativeTargetIds(any(), anyInt())).thenReturn(java.util.Set.of());
        when(recommendFeedbackService.countNegativeByPartnerType(any())).thenReturn(Map.of());
        when(recommendFeedbackService.countNegativeByActivityCategory(any())).thenReturn(Map.of());

        when(memoryFacade.recallForPrompt(eq(10010L), anyString())).thenThrow(new RuntimeException("recall error"));
        when(chatModel.call(anyString())).thenReturn("[{\"kind\":\"partner\",\"id\":77,\"score\":86}]");

        AiRecommendVO result = aiService.recommend(request);
        assertNotNull(result);
        assertTrue(result.getPartners() != null);
    }

    @Test
    void recommend_shouldFailOpenWhenMergePromptThrows() {
        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        AiServiceImpl aiService = new AiServiceImpl(
                partnerMapper,
                activityMapper,
                userMapper,
                recommendFeedbackService,
                chatModelProvider,
                memoryFacade
        );
        AiRecommendRequest request = new AiRecommendRequest();
        request.setUserId(10020L);
        request.setPartnerLimit(1);
        request.setActivityLimit(1);

        Partner partner = new Partner();
        partner.setId(78L);
        partner.setUserId(20002L);
        partner.setStatus(0);
        partner.setScope(1);
        partner.setType(6);
        partner.setTitle("电影搭子");
        partner.setContent("周末看电影");
        partner.setPlanTime(LocalDateTime.now().plusDays(1));
        when(partnerMapper.selectList(any())).thenReturn(List.of(partner));

        Activity activity = new Activity();
        activity.setId(89L);
        activity.setUserId(30002L);
        activity.setStatus(1);
        activity.setCategory("娱乐");
        activity.setTitle("电影局");
        activity.setDescription("轻松交流");
        activity.setStartTime(LocalDateTime.now().plusDays(1));
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));

        when(userMapper.selectById(10020L)).thenReturn(null);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());
        when(recommendFeedbackService.findNegativeTargetIds(any(), anyInt())).thenReturn(java.util.Set.of());
        when(recommendFeedbackService.countNegativeByPartnerType(any())).thenReturn(Map.of());
        when(recommendFeedbackService.countNegativeByActivityCategory(any())).thenReturn(Map.of());

        when(memoryFacade.recallForPrompt(eq(10020L), anyString())).thenReturn(null);
        when(memoryFacade.mergePrompt(anyString(), isNull())).thenThrow(new RuntimeException("merge failed"));
        when(chatModel.call(anyString())).thenReturn("[{\"kind\":\"partner\",\"id\":78,\"score\":85}]");

        AiRecommendVO result = aiService.recommend(request);
        assertNotNull(result);
        assertNotNull(result.getPartners());
    }

    @Test
    void recommend_shouldFailOpenWhenEnqueueThrows() {
        when(chatModelProvider.getIfAvailable()).thenReturn(chatModel);
        AiServiceImpl aiService = new AiServiceImpl(
                partnerMapper,
                activityMapper,
                userMapper,
                recommendFeedbackService,
                chatModelProvider,
                memoryFacade
        );
        AiRecommendRequest request = new AiRecommendRequest();
        request.setUserId(10030L);
        request.setPartnerLimit(1);
        request.setActivityLimit(1);

        Partner partner = new Partner();
        partner.setId(79L);
        partner.setUserId(20003L);
        partner.setStatus(0);
        partner.setScope(1);
        partner.setType(7);
        partner.setTitle("咖啡搭子");
        partner.setContent("周末喝咖啡");
        partner.setPlanTime(LocalDateTime.now().plusDays(1));
        when(partnerMapper.selectList(any())).thenReturn(List.of(partner));

        Activity activity = new Activity();
        activity.setId(90L);
        activity.setUserId(30003L);
        activity.setStatus(1);
        activity.setCategory("社交");
        activity.setTitle("咖啡交流会");
        activity.setDescription("轻松闲聊");
        activity.setStartTime(LocalDateTime.now().plusDays(1));
        when(activityMapper.selectList(any())).thenReturn(List.of(activity));

        when(userMapper.selectById(10030L)).thenReturn(null);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of());
        when(recommendFeedbackService.findNegativeTargetIds(any(), anyInt())).thenReturn(java.util.Set.of());
        when(recommendFeedbackService.countNegativeByPartnerType(any())).thenReturn(Map.of());
        when(recommendFeedbackService.countNegativeByActivityCategory(any())).thenReturn(Map.of());

        when(memoryFacade.recallForPrompt(eq(10030L), anyString())).thenReturn(null);
        when(memoryFacade.mergePrompt(anyString(), isNull())).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatModel.call(anyString())).thenReturn("[{\"kind\":\"partner\",\"id\":79,\"score\":90}]");
        doThrow(new RuntimeException("enqueue failed"))
                .when(memoryFacade).enqueueConversation(eq(10030L), anyString(), anyString());

        AiRecommendVO result = aiService.recommend(request);
        assertNotNull(result);
        assertNotNull(result.getActivities());
    }
}
