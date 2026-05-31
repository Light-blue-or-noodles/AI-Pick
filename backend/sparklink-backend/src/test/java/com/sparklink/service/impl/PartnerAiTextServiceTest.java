package com.sparklink.service.impl;

import com.sparklink.dto.PartnerAiRequest;
import com.sparklink.memory.service.MemoryFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PartnerAiTextServiceTest {

    @Mock
    private ChatModel chatModel;
    @Mock
    private MemoryFacade memoryFacade;

    @Test
    void enhanceDescription_shouldFailOpenWhenMemoryHooksThrow() {
        PartnerAiTextService service = new PartnerAiTextService(chatModel, memoryFacade);
        PartnerAiRequest request = new PartnerAiRequest();
        request.setTitle("找饭搭子");
        request.setTypeName("美食");
        request.setPreference("随和");

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setAttribute("userId", 10001L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));
        try {
            when(memoryFacade.recallForPrompt(eq(10001L), anyString())).thenThrow(new RuntimeException("recall failed"));
            when(memoryFacade.mergePrompt(anyString(), isNull())).thenThrow(new RuntimeException("merge failed"));
            doThrow(new RuntimeException("enqueue failed"))
                    .when(memoryFacade).enqueueConversation(eq(10001L), anyString(), anyString());
            when(chatModel.call(anyString())).thenReturn("搭子文案输出");

            String result = service.enhanceDescription(request);
            assertEquals("搭子文案输出", result);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}
