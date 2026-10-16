package io.github.justhurky.groundsegment.crypto;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;

public final class Ed25519Keys {

    public static final String ALGORITHM = "Ed25519";

    private Ed25519Keys() {
    }

    public static KeyPair generate() {
        try {
            return KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new KeyMaterialException("Unable to generate an " + ALGORITHM + " key pair", e);
        }
    }

    public static byte[] sign(byte[] pkcs8PrivateKey, byte[] message) {
        try {
            PrivateKey privateKey = KeyFactory.getInstance(ALGORITHM)
                    .generatePrivate(new PKCS8EncodedKeySpec(pkcs8PrivateKey));
            Signature signer = Signature.getInstance(ALGORITHM);
            signer.initSign(privateKey);
            signer.update(message);
            return signer.sign();
        } catch (GeneralSecurityException e) {
            throw new KeyMaterialException("Unable to sign with the supplied private key", e);
        }
    }
}
