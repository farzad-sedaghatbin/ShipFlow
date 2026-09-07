package com.github.farzadsedaghatbin.shipflow.license;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Minimal Ed25519 sign/verify helpers, using only the JDK's built-in Ed25519
 * support ({@code java.security.Signature} / {@code KeyPairGenerator} with
 * algorithm {@code "Ed25519"}, available since JDK 15) — deliberately no
 * BouncyCastle or other third-party crypto library, per the fixed licensing
 * design.
 *
 * <p>Used by {@link LicenseService} to verify a configured licence's signature,
 * and by tests to build ad hoc signed licences with a throwaway keypair.
 */
public final class Ed25519Licenses {

  private static final String ALGORITHM = "Ed25519";

  private Ed25519Licenses() {}

  /** Generate a fresh Ed25519 keypair — for tests / the (out-of-scope) private issuer tool. */
  public static KeyPair generateKeyPair() {
    try {
      return KeyPairGenerator.getInstance(ALGORITHM).generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("JDK Ed25519 support is unavailable", e);
    }
  }

  /** Sign {@code data} with {@code privateKey}, returning the raw (non-base64) signature bytes. */
  public static byte[] sign(byte[] data, PrivateKey privateKey) {
    try {
      var signer = java.security.Signature.getInstance(ALGORITHM);
      signer.initSign(privateKey);
      signer.update(data);
      return signer.sign();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Failed to sign licence payload", e);
    }
  }

  /**
   * Verify {@code signature} over {@code data} with {@code publicKey}. Returns {@code false}
   * (never throws) on any malformed input or verification failure — callers should treat any
   * failure uniformly as "not a valid licence," not distinguish the reason.
   */
  public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey) {
    try {
      var verifier = java.security.Signature.getInstance(ALGORITHM);
      verifier.initVerify(publicKey);
      verifier.update(data);
      return verifier.verify(signature);
    } catch (GeneralSecurityException e) {
      return false;
    }
  }

  /** Decode a base64-encoded X.509 SubjectPublicKeyInfo (DER) Ed25519 public key. */
  public static PublicKey decodePublicKey(String base64X509) {
    try {
      byte[] der = Base64.getDecoder().decode(base64X509);
      return KeyFactory.getInstance(ALGORITHM).generatePublic(new X509EncodedKeySpec(der));
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid Ed25519 public key (expected base64 X.509 SubjectPublicKeyInfo)", e);
    }
  }

  /** Encode an Ed25519 public key as base64 X.509 SubjectPublicKeyInfo (DER) — the {@code app.license.public-key} format. */
  public static String encodePublicKey(PublicKey publicKey) {
    return Base64.getEncoder().encodeToString(publicKey.getEncoded());
  }
}
