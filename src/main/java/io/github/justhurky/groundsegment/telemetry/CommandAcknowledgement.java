package io.github.justhurky.groundsegment.telemetry;

import java.nio.ByteBuffer;
import java.util.UUID;

public record CommandAcknowledgement(UUID commandId, boolean executed, int resultCode) {

    public static final int PAYLOAD_BYTES = 17;
    public static final int RESULT_CODE_EXECUTED = 0;

    public static CommandAcknowledgement decode(byte[] payload) {
        if (payload.length != PAYLOAD_BYTES) {
            throw new FrameFormatException("A command acknowledgement payload must be exactly "
                    + PAYLOAD_BYTES + " bytes, got " + payload.length);
        }
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        UUID commandId = new UUID(buffer.getLong(), buffer.getLong());
        int resultCode = Byte.toUnsignedInt(buffer.get());
        return new CommandAcknowledgement(commandId, resultCode == RESULT_CODE_EXECUTED, resultCode);
    }

    public byte[] encode() {
        return ByteBuffer.allocate(PAYLOAD_BYTES)
                .putLong(commandId.getMostSignificantBits())
                .putLong(commandId.getLeastSignificantBits())
                .put((byte) resultCode)
                .array();
    }
}
