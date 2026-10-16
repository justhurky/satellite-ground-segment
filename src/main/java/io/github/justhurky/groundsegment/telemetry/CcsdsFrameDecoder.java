package io.github.justhurky.groundsegment.telemetry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CcsdsFrameDecoder {

    public static final int PRIMARY_HEADER_BYTES = 6;
    public static final int SECONDARY_HEADER_BYTES = 6;
    public static final int SAMPLE_BYTES = 9;
    public static final int SUPPORTED_PACKET_VERSION = 0;
    public static final int TELEMETRY_PACKET_TYPE = 0;
    public static final int APID_MASK = 0x07FF;
    public static final int SEQUENCE_COUNT_MASK = 0x3FFF;

    private static final Instant CDS_EPOCH =
            LocalDate.of(1958, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    private static final long MILLIS_PER_DAY = Duration.ofDays(1).toMillis();

    public DecodedFrame decode(byte[] frame) {
        if (frame.length < PRIMARY_HEADER_BYTES + SECONDARY_HEADER_BYTES) {
            throw new FrameFormatException("A frame must carry at least "
                    + (PRIMARY_HEADER_BYTES + SECONDARY_HEADER_BYTES) + " header bytes, got " + frame.length);
        }
        ByteBuffer buffer = ByteBuffer.wrap(frame).order(ByteOrder.BIG_ENDIAN);

        int identification = Short.toUnsignedInt(buffer.getShort());
        int packetVersion = identification >>> 13;
        int packetType = (identification >>> 12) & 0x01;
        int secondaryHeaderFlag = (identification >>> 11) & 0x01;
        int apid = identification & APID_MASK;

        int sequenceWord = Short.toUnsignedInt(buffer.getShort());
        int sequenceFlags = sequenceWord >>> 14;
        int sequenceCount = sequenceWord & SEQUENCE_COUNT_MASK;

        int declaredDataLength = Short.toUnsignedInt(buffer.getShort());

        if (packetVersion != SUPPORTED_PACKET_VERSION) {
            throw new FrameFormatException("Packet version " + packetVersion + " is not supported");
        }
        if (packetType != TELEMETRY_PACKET_TYPE) {
            throw new FrameFormatException("Only telemetry packets are accepted on this endpoint");
        }
        if (secondaryHeaderFlag != 1) {
            throw new FrameFormatException("A CDS secondary header is mandatory for telemetry frames");
        }
        int actualDataLength = frame.length - PRIMARY_HEADER_BYTES - 1;
        if (declaredDataLength != actualDataLength) {
            throw new FrameFormatException("Declared data length " + declaredDataLength
                    + " does not match the received length " + actualDataLength);
        }

        int daysSinceEpoch = Short.toUnsignedInt(buffer.getShort());
        long millisOfDay = Integer.toUnsignedLong(buffer.getInt());
        if (millisOfDay >= MILLIS_PER_DAY) {
            throw new FrameFormatException("CDS time field carries " + millisOfDay
                    + " milliseconds of day, which is out of range");
        }
        Instant generatedAt = CDS_EPOCH.plus(Duration.ofDays(daysSinceEpoch)).plusMillis(millisOfDay);

        byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);

        return new DecodedFrame(packetVersion, apid, sequenceFlags, sequenceCount, generatedAt, payload);
    }

    public List<ParameterSample> decodeSamples(byte[] payload) {
        if (payload.length % SAMPLE_BYTES != 0) {
            throw new FrameFormatException("Telemetry payload length " + payload.length
                    + " is not a multiple of the " + SAMPLE_BYTES + " byte sample record");
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload).order(ByteOrder.BIG_ENDIAN);
        List<ParameterSample> samples = new ArrayList<>(payload.length / SAMPLE_BYTES);
        while (buffer.hasRemaining()) {
            int parameterSlot = Byte.toUnsignedInt(buffer.get());
            double value = buffer.getDouble();
            samples.add(new ParameterSample(parameterSlot, value));
        }
        return samples;
    }
}
