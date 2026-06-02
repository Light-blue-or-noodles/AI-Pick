package com.sparklink.knowledge.client;

import com.sparklink.knowledge.config.DifyKnowledgeProperties;
import com.sparklink.knowledge.model.KnowledgeRetrieveRequest;
import com.sparklink.knowledge.model.KnowledgeRetrieveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DifyKnowledgeClientTest {

    private MockRestServiceServer mockServer;
    private DifyKnowledgeClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();

        DifyKnowledgeProperties properties = new DifyKnowledgeProperties();
        properties.setBaseUrl("https://api.dify.test/v1");
        properties.setApiKey("dify-secret");
        properties.setDatasetId("dataset-001");

        DifyKnowledgeProperties.Retrieve retrieve = properties.getRetrieve();
        retrieve.setTopK(8);
        retrieve.setThresholdEnabled(true);
        retrieve.setScoreThreshold(0.66D);
        retrieve.setSearchMethod("hybrid_search");
        retrieve.setRerankingEnable(true);

        client = new DifyKnowledgeClient(restTemplate, properties);
    }

    @Test
    void shouldSendRequestWithExpectedPathHeaderAndBody() {
        String tooLongQuery = "q".repeat(300);
        String expectedTruncatedQuery = tooLongQuery.substring(0, 250);
        mockServer.expect(requestTo("https://api.dify.test/v1/datasets/dataset-001/retrieve"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer dify-secret"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.query").value(expectedTruncatedQuery))
                .andExpect(jsonPath("$.retrieval_model.top_k").value(8))
                .andExpect(jsonPath("$.retrieval_model.score_threshold_enabled").value(true))
                .andExpect(jsonPath("$.retrieval_model.score_threshold").value(0.66D))
                .andExpect(jsonPath("$.retrieval_model.search_method").value("hybrid_search"))
                .andExpect(jsonPath("$.retrieval_model.reranking_enable").value(true))
                .andRespond(withSuccess("{\"records\":[]}", MediaType.APPLICATION_JSON));

        KnowledgeRetrieveRequest request = new KnowledgeRetrieveRequest();
        request.setUserId(1001L);
        request.setQuery(tooLongQuery);
        client.retrieve(request);

        mockServer.verify();
    }

    @Test
    void shouldParseRecordsIntoKnowledgeResult() {
        mockServer.expect(requestTo("https://api.dify.test/v1/datasets/dataset-001/retrieve"))
                .andRespond(withSuccess("""
                        {
                          "records": [
                            {
                              "segment": {
                                "id": "seg-1",
                                "document_id": "doc-1",
                                "content": "知识片段1"
                              },
                              "score": 0.91
                            },
                            {
                              "segment": {
                                "id": "seg-2",
                                "document_id": "doc-2",
                                "content": "知识片段2"
                              },
                              "score": 0.82
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        KnowledgeRetrieveRequest request = new KnowledgeRetrieveRequest();
        request.setQuery("测试查询");
        KnowledgeRetrieveResult result = client.retrieve(request);

        assertTrue(result.isHit());
        assertEquals(2, result.getRecords().size());
        assertEquals("seg-1", result.getRecords().get(0).getSegmentId());
        assertEquals("doc-1", result.getRecords().get(0).getDocumentId());
        assertEquals("知识片段1", result.getRecords().get(0).getContent());
        assertEquals(0.91D, result.getRecords().get(0).getScore(), 0.000001D);
    }

    @Test
    void shouldReturnEmptyHitWhenNoRecords() {
        mockServer.expect(requestTo("https://api.dify.test/v1/datasets/dataset-001/retrieve"))
                .andRespond(withSuccess("{\"records\":[]}", MediaType.APPLICATION_JSON));

        KnowledgeRetrieveRequest request = new KnowledgeRetrieveRequest();
        request.setQuery("无结果查询");
        KnowledgeRetrieveResult result = client.retrieve(request);

        assertFalse(result.isHit());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void shouldFilterMalformedRecordsAndMarkNotHit() {
        mockServer.expect(requestTo("https://api.dify.test/v1/datasets/dataset-001/retrieve"))
                .andRespond(withSuccess("""
                        {
                          "records": [
                            {"score": 0.91},
                            {"segment": {"id":"seg-2","document_id":"doc-2","content":""}, "score": 0.82},
                            {"segment": {"id":"seg-3","document_id":"doc-3","content":"   "}, "score": 0.81}
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        KnowledgeRetrieveRequest request = new KnowledgeRetrieveRequest();
        request.setQuery("脏数据");

        KnowledgeRetrieveResult result = client.retrieve(request);
        assertFalse(result.isHit());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void shouldThrowParseErrorWhenJsonInvalid() {
        mockServer.expect(requestTo("https://api.dify.test/v1/datasets/dataset-001/retrieve"))
                .andRespond(withSuccess("{invalid}", MediaType.APPLICATION_JSON));
        KnowledgeRetrieveRequest request = new KnowledgeRetrieveRequest();
        request.setQuery("非法JSON");

        KnowledgeClientException ex = assertThrows(KnowledgeClientException.class, () -> client.retrieve(request));
        assertEquals(KnowledgeClientException.ErrorType.PARSE_ERROR, ex.getErrorType());
    }

    @Test
    void shouldThrowParseErrorWhenRecordsIsNotArray() {
        mockServer.expect(requestTo("https://api.dify.test/v1/datasets/dataset-001/retrieve"))
                .andRespond(withSuccess("{\"records\":{}}", MediaType.APPLICATION_JSON));
        KnowledgeRetrieveRequest request = new KnowledgeRetrieveRequest();
        request.setQuery("records 非数组");

        KnowledgeClientException ex = assertThrows(KnowledgeClientException.class, () -> client.retrieve(request));
        assertEquals(KnowledgeClientException.ErrorType.PARSE_ERROR, ex.getErrorType());
    }

    @Test
    void shouldMapHttp4xxToKnowledgeClientException() {
        mockServer.expect(requestTo("https://api.dify.test/v1/datasets/dataset-001/retrieve"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("bad request").contentType(MediaType.TEXT_PLAIN));
        KnowledgeRetrieveRequest request = new KnowledgeRetrieveRequest();
        request.setQuery("http error");

        KnowledgeClientException ex = assertThrows(KnowledgeClientException.class, () -> client.retrieve(request));
        assertEquals(KnowledgeClientException.ErrorType.HTTP_ERROR, ex.getErrorType());
    }

    @Test
    void shouldMapTimeoutToKnowledgeClientException() {
        mockServer.expect(requestTo("https://api.dify.test/v1/datasets/dataset-001/retrieve"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));
        KnowledgeRetrieveRequest request = new KnowledgeRetrieveRequest();
        request.setQuery("timeout");

        KnowledgeClientException ex = assertThrows(KnowledgeClientException.class, () -> client.retrieve(request));
        assertEquals(KnowledgeClientException.ErrorType.HTTP_ERROR, ex.getErrorType());
    }

    @Test
    void shouldUseSameAuthorizationHeaderWithOrWithoutBearerPrefix() {
        DifyKnowledgeProperties prefixProps = newProperties("Bearer dify-secret");
        RestTemplate rtPrefix = new RestTemplate();
        MockRestServiceServer serverPrefix = MockRestServiceServer.bindTo(rtPrefix).build();
        DifyKnowledgeClient clientWithPrefix = new DifyKnowledgeClient(rtPrefix, prefixProps);

        serverPrefix.expect(requestTo("https://api.dify.test/v1/datasets/dataset-001/retrieve"))
                .andExpect(header("Authorization", "Bearer dify-secret"))
                .andRespond(withSuccess("{\"records\":[]}", MediaType.APPLICATION_JSON));
        KnowledgeRetrieveRequest request = new KnowledgeRetrieveRequest();
        request.setQuery("prefix");
        clientWithPrefix.retrieve(request);
        serverPrefix.verify();

        DifyKnowledgeProperties plainProps = newProperties("dify-secret");
        RestTemplate rtPlain = new RestTemplate();
        MockRestServiceServer serverPlain = MockRestServiceServer.bindTo(rtPlain).build();
        DifyKnowledgeClient clientWithoutPrefix = new DifyKnowledgeClient(rtPlain, plainProps);

        serverPlain.expect(requestTo("https://api.dify.test/v1/datasets/dataset-001/retrieve"))
                .andExpect(header("Authorization", "Bearer dify-secret"))
                .andRespond(withSuccess("{\"records\":[]}", MediaType.APPLICATION_JSON));
        request.setQuery("plain");
        clientWithoutPrefix.retrieve(request);
        serverPlain.verify();
    }

    private DifyKnowledgeProperties newProperties(String apiKey) {
        DifyKnowledgeProperties properties = new DifyKnowledgeProperties();
        properties.setBaseUrl("https://api.dify.test/v1");
        properties.setApiKey(apiKey);
        properties.setDatasetId("dataset-001");
        DifyKnowledgeProperties.Retrieve retrieve = properties.getRetrieve();
        retrieve.setTopK(8);
        retrieve.setThresholdEnabled(true);
        retrieve.setScoreThreshold(0.66D);
        retrieve.setSearchMethod("hybrid_search");
        retrieve.setRerankingEnable(true);
        return properties;
    }
}
