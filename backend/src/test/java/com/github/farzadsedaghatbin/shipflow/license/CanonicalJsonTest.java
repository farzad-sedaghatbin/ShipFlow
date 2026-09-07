package com.github.farzadsedaghatbin.shipflow.license;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class CanonicalJsonTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private String canonicalize(String json) throws Exception {
    JsonNode node = mapper.readTree(json);
    return new String(CanonicalJson.canonicalBytes(node), StandardCharsets.UTF_8);
  }

  @Test
  void sortsObjectKeysAlphabetically() throws Exception {
    assertThat(canonicalize("{\"b\":1,\"a\":2,\"c\":3}")).isEqualTo("{\"a\":2,\"b\":1,\"c\":3}");
  }

  @Test
  void sortsNestedObjectKeysRecursively() throws Exception {
    assertThat(canonicalize("{\"z\":{\"y\":1,\"x\":2},\"a\":1}")).isEqualTo("{\"a\":1,\"z\":{\"x\":2,\"y\":1}}");
  }

  @Test
  void preservesArrayElementOrder() throws Exception {
    assertThat(canonicalize("{\"a\":[3,1,2]}")).isEqualTo("{\"a\":[3,1,2]}");
  }

  @Test
  void sortsObjectKeysInsideArrayElements() throws Exception {
    assertThat(canonicalize("{\"a\":[{\"b\":1,\"a\":2}]}")).isEqualTo("{\"a\":[{\"a\":2,\"b\":1}]}");
  }

  @Test
  void producesNoInsignificantWhitespace() throws Exception {
    String result = canonicalize("{ \"a\" : 1 ,   \"b\" :   2 }");

    assertThat(result).doesNotContain(" ").doesNotContain("\n").doesNotContain("\t");
    assertThat(result).isEqualTo("{\"a\":1,\"b\":2}");
  }

  @Test
  void isDeterministicRegardlessOfInputKeyOrder() throws Exception {
    assertThat(canonicalize("{\"a\":1,\"b\":2}")).isEqualTo(canonicalize("{\"b\":2,\"a\":1}"));
  }

  @Test
  void handlesNullAndScalarTypes() throws Exception {
    assertThat(canonicalize("{\"n\":null,\"b\":true,\"s\":\"hi\",\"i\":5}"))
        .isEqualTo("{\"b\":true,\"i\":5,\"n\":null,\"s\":\"hi\"}");
  }

  @Test
  void escapesStringsConsistentlyWithJacksonDefaults() throws Exception {
    assertThat(canonicalize("{\"s\":\"a\\\"b\\\\c\\nd\"}")).isEqualTo("{\"s\":\"a\\\"b\\\\c\\nd\"}");
  }

  @Test
  void topLevelArrayIsSupported() throws Exception {
    assertThat(canonicalize("[{\"b\":1,\"a\":2},3]")).isEqualTo("[{\"a\":2,\"b\":1},3]");
  }
}
