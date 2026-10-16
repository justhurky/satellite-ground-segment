package io.github.justhurky.groundsegment.crypto;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import org.springframework.stereotype.Component;

@Component
public class CommandSignatureVerifier {

    public boolean verify(byte[] x509PublicKey, byte[] message, byte[] signature) {
        PublicKey publicKey = readPublicKey(x509PublicKey);
        try {
            Signature verifier = Signature.getInstance(Ed25519Keys.ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(message);
            return verifier.verify(signature);
        } catch (SignatureException e) {
            return false;
        } catch (GeneralSecurityException e) {
            throw new KeyMaterialException("Unable to verify an " + Ed25519Keys.ALGORITHM + " signature", e);
        }
    }

    private PublicKey readPublicKey(byte[] x509PublicKey) {
        try {
            return KeyFactory.getInstance(Ed25519Keys.ALGORITHM)
                    .generatePublic(new X509EncodedKeySpec(x509PublicKey));
        } catch (GeneralSecurityException e) {
            throw new KeyMaterialException("Stored command verification key is not a usable "
                    + Ed25519Keys.ALGORITHM + " public key", e);
        }
    }
}
