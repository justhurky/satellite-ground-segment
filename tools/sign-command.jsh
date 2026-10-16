import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

String requireEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) {
        throw new IllegalArgumentException("Environment variable " + name + " is required");
    }
    return value;
}

String canonicalParameters(String raw) {
    if (raw == null || raw.isBlank()) {
        return "";
    }
    Map<String, String> sorted = new TreeMap<>();
    for (String pair : raw.split(";")) {
        int separator = pair.indexOf('=');
        sorted.put(pair.substring(0, separator), pair.substring(separator + 1));
    }
    StringBuilder builder = new StringBuilder();
    sorted.forEach((key, value) -> {
        if (builder.length() > 0) {
            builder.append(';');
        }
        builder.append(key).append('=').append(value);
    });
    return builder.toString();
}

String spacecraftId = requireEnv("GS_SPACECRAFT_ID");
String opcode = requireEnv("GS_OPCODE").trim().toUpperCase().replace('-', '_');
String criticality = requireEnv("GS_CRITICALITY").trim().toUpperCase();
String nonce = requireEnv("GS_NONCE");
String requestedAt = requireEnv("GS_REQUESTED_AT");
String parameters = canonicalParameters(System.getenv("GS_PARAMETERS"));
byte[] privateKey = Base64.getDecoder().decode(requireEnv("GS_PRIVATE_KEY"));

long requestedAtMillis = java.time.Instant.parse(requestedAt).toEpochMilli();
String canonical = String.join("\n", spacecraftId, opcode, criticality, nonce,
        Long.toString(requestedAtMillis), parameters);

Signature signer = Signature.getInstance("Ed25519");
signer.initSign(KeyFactory.getInstance("Ed25519").generatePrivate(new PKCS8EncodedKeySpec(privateKey)));
signer.update(canonical.getBytes(StandardCharsets.UTF_8));
String signature = Base64.getEncoder().encodeToString(signer.sign());

System.out.println("canonical form:");
System.out.println(canonical.replace("\n", " | "));
System.out.println();
System.out.println("signatureBase64: " + signature);

/exit
