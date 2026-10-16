package io.github.justhurky.groundsegment.support;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TestFrames {

    public static final Instant CDS_EPOCH = LocalDate.of(1958, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    public static final int PRIMARY_HEADER_BYTES = 6;
    public static final int SECONDARY_HEADER_BYTES = 6;
    public static final int UNSEGMENTED = 3;

    private TestFrames() {
    }

    public static byte[] telemetryFrame(int apid, int sequenceCount, Instant generatedAt, byte[] payload) {
        return frame(0, 0, 1, apid, sequenceCount, generatedAt, payload, 0);
    }

    public static byte[] frame(int packetVersion, int packetType, int secondaryHeaderFlag, int apid,
                               int sequenceCount, Instant generatedAt, byte[] payload, int dataLengthOffset) {
        int identification = (packetVersion << 13) | (packetType << 12) | (secondaryHeaderFlag << 11)
                | (apid & 0x07FF);
        int sequenceWord = (UNSEGMENTED << 14) | (sequenceCount & 0x3FFF);
        int dataLength = SECONDARY_HEADER_BYTES + payload.length - 1 + dataLengthOffset;

        long sinceEpoch = Duration.between(CDS_EPOCH, generatedAt).toMillis();
        int days = (int) (sinceEpoch / Duration.ofDays(1).toMillis());
        long millisOfDay = sinceEpoch % Duration.ofDays(1).toMillis();

        return ByteBuffer.allocate(PRIMARY_HEADER_BYTES + SECONDARY_HEADER_BYTES + payload.length)
                .order(ByteOrder.BIG_ENDIAN)
                .putShort((short) identification)
                .putShort((short) sequenceWord)
                .putShort((short) dataLength)
                .putShort((short) days)
                .putInt((int) millisOfDay)
                .put(payload)
                .array();
    }

    public static byte[] samplePayload(Map<Integer, Double> samples) {
        ByteBuffer buffer = ByteBuffer.allocate(samples.size() * 9).order(ByteOrder.BIG_ENDIAN);
        samples.forEach((slot, value) -> buffer.put((byte) slot.intValue()).putDouble(value));
        return buffer.array();
    }

    public static Map<Integer, Double> samples(int firstSlot, double firstValue) {
        Map<Integer, Double> samples = new LinkedHashMap<>();
        samples.put(firstSlot, firstValue);
        return samples;
    }
}
