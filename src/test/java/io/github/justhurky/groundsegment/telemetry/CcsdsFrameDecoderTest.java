package io.github.justhurky.groundsegment.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.justhurky.groundsegment.support.TestFrames;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CcsdsFrameDecoderTest {

    private static final Instant GENERATED_AT = Instant.parse("2026-03-14T09:26:53.589Z");

    private final CcsdsFrameDecoder decoder = new CcsdsFrameDecoder();

    @Test
    void decodesThePrimaryAndSecondaryHeader() {
        byte[] payload = TestFrames.samplePayload(Map.of(1, 27.4));
        byte[] frame = TestFrames.telemetryFrame(100, 4096, GENERATED_AT, payload);

        DecodedFrame decoded = decoder.decode(frame);

        assertThat(decoded.packetVersion()).isZero();
        assertThat(decoded.apid()).isEqualTo(100);
        assertThat(decoded.sequenceCount()).isEqualTo(4096);
        assertThat(decoded.sequenceFlags()).isEqualTo(TestFrames.UNSEGMENTED);
        assertThat(decoded.generatedAt()).isEqualTo(GENERATED_AT);
        assertThat(decoded.payload()).isEqualTo(payload);
    }

    @Test
    void masksTheApidToElevenBits() {
        byte[] frame = TestFrames.telemetryFrame(2047, 0, GENERATED_AT, new byte[9]);

        assertThat(decoder.decode(frame).apid()).isEqualTo(2047);
    }

    @Test
    void rejectsAFrameShorterThanItsHeaders() {
        assertThatThrownBy(() -> decoder.decode(new byte[8]))
                .isInstanceOf(FrameFormatException.class)
                .hasMessageContaining("header bytes");
    }

    @Test
    void rejectsAnUnsupportedPacketVersion() {
        byte[] frame = TestFrames.frame(1, 0, 1, 100, 0, GENERATED_AT, new byte[9], 0);

        assertThatThrownBy(() -> decoder.decode(frame))
                .isInstanceOf(FrameFormatException.class)
                .hasMessageContaining("Packet version");
    }

    @Test
    void rejectsATelecommandPacketOnTheTelemetryPath() {
        byte[] frame = TestFrames.frame(0, 1, 1, 100, 0, GENERATED_AT, new byte[9], 0);

        assertThatThrownBy(() -> decoder.decode(frame))
                .isInstanceOf(FrameFormatException.class)
                .hasMessageContaining("Only telemetry packets");
    }

    @Test
    void rejectsAFrameWithoutASecondaryHeader() {
        byte[] frame = TestFrames.frame(0, 0, 0, 100, 0, GENERATED_AT, new byte[9], 0);

        assertThatThrownBy(() -> decoder.decode(frame))
                .isInstanceOf(FrameFormatException.class)
                .hasMessageContaining("secondary header is mandatory");
    }

    @Test
    void rejectsAFrameWhoseDeclaredLengthDisagreesWithTheReceivedLength() {
        byte[] frame = TestFrames.frame(0, 0, 1, 100, 0, GENERATED_AT, new byte[9], 7);

        assertThatThrownBy(() -> decoder.decode(frame))
                .isInstanceOf(FrameFormatException.class)
                .hasMessageContaining("Declared data length");
    }

    @Test
    void rejectsATruncatedFrame() {
        byte[] frame = TestFrames.telemetryFrame(100, 0, GENERATED_AT, new byte[9]);

        assertThatThrownBy(() -> decoder.decode(Arrays.copyOf(frame, frame.length - 2)))
                .isInstanceOf(FrameFormatException.class);
    }

    @Test
    void decodesEveryParameterSampleInOrder() {
        Map<Integer, Double> expected = new LinkedHashMap<>();
        expected.put(1, 27.4);
        expected.put(2, -3.5);
        expected.put(3, 61.125);
        byte[] frame = TestFrames.telemetryFrame(100, 1, GENERATED_AT, TestFrames.samplePayload(expected));

        assertThat(decoder.decodeSamples(decoder.decode(frame).payload()))
                .containsExactly(
                        new ParameterSample(1, 27.4),
                        new ParameterSample(2, -3.5),
                        new ParameterSample(3, 61.125));
    }

    @Test
    void rejectsAPayloadThatIsNotAWholeNumberOfSamples() {
        assertThatThrownBy(() -> decoder.decodeSamples(new byte[14]))
                .isInstanceOf(FrameFormatException.class)
                .hasMessageContaining("not a multiple");
    }

    @Test
    void acceptsAnEmptyPayload() {
        assertThat(decoder.decodeSamples(new byte[0])).isEmpty();
    }
}
