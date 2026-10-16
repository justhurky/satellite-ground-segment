import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

String requireEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("Environment variable " + name + " is required");
    }
    return value;
}

String optionalEnv(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
}

byte[] encodeSamples(String samples) {
    if (samples.isBlank()) {
        return new byte[0];
    }
    String[] entries = samples.split(";");
    ByteBuffer buffer = ByteBuffer.allocate(entries.length * 9).order(ByteOrder.BIG_ENDIAN);
    for (String entry : entries) {
        String[] parts = entry.split(":");
        buffer.put((byte) Integer.parseInt(parts[0].trim()));
        buffer.putDouble(Double.parseDouble(parts[1].trim()));
    }
    return buffer.array();
}

byte[] encodeFrame(int apid, int sequenceCount, Instant generatedAt, byte[] payload) {
    Instant cdsEpoch = LocalDate.of(1958, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant();
    long sinceEpoch = Duration.between(cdsEpoch, generatedAt).toMillis();
    int days = (int) (sinceEpoch / Duration.ofDays(1).toMillis());
    long millisOfDay = sinceEpoch % Duration.ofDays(1).toMillis();
    int identification = (1 << 11) | (apid & 0x07FF);
    int sequenceWord = (3 << 14) | (sequenceCount & 0x3FFF);
    int dataLength = 6 + payload.length - 1;
    ByteBuffer buffer = ByteBuffer.allocate(12 + payload.length).order(ByteOrder.BIG_ENDIAN);
    buffer.putShort((short) identification);
    buffer.putShort((short) sequenceWord);
    buffer.putShort((short) dataLength);
    buffer.putShort((short) days);
    buffer.putInt((int) millisOfDay);
    buffer.put(payload);
    return buffer.array();
}

String authenticationTag(byte[] key, byte[] frame) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(key, "HmacSHA256"));
    return Base64.getEncoder().encodeToString(mac.doFinal(frame));
}

int apid = Integer.parseInt(requireEnv("GS_APID"));
int sequenceCount = Integer.parseInt(requireEnv("GS_SEQUENCE"));
Instant generatedAt = Instant.parse(optionalEnv("GS_GENERATED_AT", Instant.now().toString()));
byte[] telemetryKey = Base64.getDecoder().decode(requireEnv("GS_TELEMETRY_KEY"));
byte[] payload = encodeSamples(optionalEnv("GS_SAMPLES", ""));
byte[] frame = encodeFrame(apid, sequenceCount, generatedAt, payload);

System.out.println("frameBase64: " + Base64.getEncoder().encodeToString(frame));
System.out.println("authenticationTagBase64: " + authenticationTag(telemetryKey, frame));
System.out.println("generatedAt: " + generatedAt);

/exit
