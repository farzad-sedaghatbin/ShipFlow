package com.github.farzadsedaghatbin.shipflow.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import org.junit.jupiter.api.Test;

class Ed25519LicensesTest {

  @Test
  void signThenVerify_WithMatchingKey_Succeeds() {
    KeyPair keyPair = Ed25519Licenses.generateKeyPair();
    byte[] data = "hello licence".getBytes(StandardCharsets.UTF_8);

    byte[] signature = Ed25519Licenses.sign(data, keyPair.getPrivate());

    assertThat(Ed25519Licenses.verify(data, signature, keyPair.getPublic())).isTrue();
  }

  @Test
  void verify_WithWrongKey_Fails() {
    KeyPair keyPair = Ed25519Licenses.generateKeyPair();
    KeyPair otherKeyPair = Ed25519Licenses.generateKeyPair();
    byte[] data = "hello licence".getBytes(StandardCharsets.UTF_8);

    byte[] signature = Ed25519Licenses.sign(data, keyPair.getPrivate());

    assertThat(Ed25519Licenses.verify(data, signature, otherKeyPair.getPublic())).isFalse();
  }

  @Test
  void verify_WithTamperedData_Fails() {
    KeyPair keyPair = Ed25519Licenses.generateKeyPair();
    byte[] data = "hello licence".getBytes(StandardCharsets.UTF_8);
    byte[] tampered = "hello licenSe".getBytes(StandardCharsets.UTF_8);

    byte[] signature = Ed25519Licenses.sign(data, keyPair.getPrivate());

    assertThat(Ed25519Licenses.verify(tampered, signature, keyPair.getPublic())).isFalse();
  }

  @Test
  void verify_WithGarbageSignatureBytes_ReturnsFalseNotThrows() {
    KeyPair keyPair = Ed25519Licenses.generateKeyPair();
    byte[] data = "hello licence".getBytes(StandardCharsets.UTF_8);

    assertThat(Ed25519Licenses.verify(data, new byte[] {1, 2, 3}, keyPair.getPublic())).isFalse();
  }

  @Test
  void encodeThenDecodePublicKey_RoundTrips() {
    KeyPair keyPair = Ed25519Licenses.generateKeyPair();

    String encoded = Ed25519Licenses.encodePublicKey(keyPair.getPublic());
    PublicKey decoded = Ed25519Licenses.decodePublicKey(encoded);

    assertThat(decoded).isEqualTo(keyPair.getPublic());
  }

  @Test
  void decodePublicKey_InvalidBase64_ThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> Ed25519Licenses.decodePublicKey("not-valid-base64!!"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
