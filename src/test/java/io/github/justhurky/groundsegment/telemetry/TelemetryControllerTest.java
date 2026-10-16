package io.github.justhurky.groundsegment.telemetry;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.justhurky.groundsegment.common.error.ResourceNotFoundException;
import io.github.justhurky.groundsegment.identity.JwtService;
import io.github.justhurky.groundsegment.telemetry.dto.FrameIngestResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TelemetryController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TelemetryControllerTest {

    private static final UUID SPACECRAFT_ID = UUID.fromString("c8e1f0ec-1b2c-4d2a-9a7f-1d2e3f4a5b6c");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TelemetryIngestService ingestService;

    @MockitoBean
    private TelemetryQueryService queryService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void passesAWellFormedIngestRequestToTheService() throws Exception {
        Instant now = Instant.parse("2026-03-14T09:26:53Z");
        given(ingestService.ingest(any())).willReturn(new FrameIngestResponse(
                7L, SPACECRAFT_ID, 100, 1, now, now, 2, 0, List.of(), null));

        mockMvc.perform(post("/api/v1/telemetry/frames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "spacecraftId": "%s",
                                  "groundStationCode": "KIR",
                                  "frameBase64": "CGQAAAAAAAAAAAAA",
                                  "authenticationTagBase64": "dGFn"
                                }
                                """.formatted(SPACECRAFT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.frameId").value(7))
                .andExpect(jsonPath("$.apid").value(100));
    }

    @Test
    void rejectsAnIngestRequestWithoutASpacecraftId() throws Exception {
        mockMvc.perform(post("/api/v1/telemetry/frames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groundStationCode": "KIR",
                                  "frameBase64": "CGQAAAAAAAAAAAAA",
                                  "authenticationTagBase64": "dGFn"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations.spacecraftId").exists());

        verifyNoInteractions(ingestService);
    }

    @Test
    void rejectsAnIngestRequestWithAnEmptyFrame() throws Exception {
        mockMvc.perform(post("/api/v1/telemetry/frames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "spacecraftId": "%s",
                                  "groundStationCode": "KIR",
                                  "frameBase64": "",
                                  "authenticationTagBase64": "dGFn"
                                }
                                """.formatted(SPACECRAFT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations.frameBase64").exists());
    }

    @Test
    void rejectsAnIngestRequestWithAnUnknownField() throws Exception {
        mockMvc.perform(post("/api/v1/telemetry/frames")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "spacecraftId": "%s",
                                  "groundStationCode": "KIR",
                                  "frameBase64": "CGQ=",
                                  "authenticationTagBase64": "dGFn",
                                  "bypassAuthentication": true
                                }
                                """.formatted(SPACECRAFT_ID)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ingestService);
    }

    @Test
    void rejectsAPageSizeBeyondTheAllowedMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/spacecraft/{id}/telemetry/frames", SPACECRAFT_ID)
                        .param("size", "5000"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(queryService);
    }

    @Test
    void translatesAMissingFrameIntoNotFound() throws Exception {
        given(queryService.samplesOf(99L)).willThrow(new ResourceNotFoundException("telemetry-frame", 99L));

        mockMvc.perform(get("/api/v1/telemetry/frames/{id}/samples", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @TestConfiguration
    static class SliceConfiguration {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-03-14T09:26:53Z"), ZoneOffset.UTC);
        }
    }
}
