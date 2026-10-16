package io.github.justhurky.groundsegment.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.justhurky.groundsegment.identity.JwtAuthenticationFilter;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

public class MissionTestClient {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public MissionTestClient(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).get("accessToken").asText();
    }

    public JsonNode postJson(String path, String token, Object payload, int expectedStatus) throws Exception {
        return exchange(post(path), token, payload, expectedStatus);
    }

    public JsonNode putJson(String path, String token, Object payload, int expectedStatus) throws Exception {
        return exchange(put(path), token, payload, expectedStatus);
    }

    public JsonNode getJson(String path, String token, int expectedStatus) throws Exception {
        return exchange(get(path), token, null, expectedStatus);
    }

    private JsonNode exchange(MockHttpServletRequestBuilder request, String token, Object payload,
                              int expectedStatus) throws Exception {
        if (token != null) {
            request.header(JwtAuthenticationFilter.AUTHORIZATION_HEADER,
                    JwtAuthenticationFilter.BEARER_PREFIX + token);
        }
        if (payload != null) {
            request.contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(payload));
        }
        MvcResult result = mockMvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn();
        return body(result);
    }

    private JsonNode body(MvcResult result) throws Exception {
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        if (content.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(content);
    }
}
