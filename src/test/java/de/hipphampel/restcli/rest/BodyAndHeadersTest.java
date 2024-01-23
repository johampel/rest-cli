/*
 * The MIT License
 * Copyright © ${year} Johannes Hampel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.hipphampel.restcli.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.io.InputStreamProvider;
import de.hipphampel.restcli.io.Openable;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;

@QuarkusTest
class BodyAndHeadersTest {

  @Inject
  ObjectMapper objectMapper;

  @Inject
  DocumentBuilderFactory documentBuilderFactory;

  private InputStreamProvider body;
  private Map<String, List<String>> headers;

  private BodyAndHeaders<Openable> underTest;

  @BeforeEach
  void beforeEach() {
    underTest = new BodyAndHeaders<>(objectMapper, documentBuilderFactory) {
      @Override
      public Map<String, List<String>> getHeaders() {
        return headers;
      }

      @Override
      public InputStreamProvider getBody() {
        return body;
      }
    };
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void hasBody(boolean hasBody) {
    this.body = hasBody ? InputStreamProvider.ofString("foo") : null;
    assertThat(underTest.hasBody()).isEqualTo(hasBody);
  }

  @Test
  void getStringBody() throws IOException {
    body = InputStreamProvider.ofString("Some content");
    assertThat(underTest.getStringBody()).isEqualTo("Some content");
  }

  @Test
  void getJsonBody() throws IOException {
    body = InputStreamProvider.ofString("{\"Some\":\"content\"}");
    assertThat(underTest.getJsonBody()).isEqualTo(Map.of("Some", "content"));
  }

  @Test
  void getXmlBody() throws IOException {
    body = InputStreamProvider.ofString("<Some>content</Some>");
    Document document = underTest.getXmlBody();
    assertThat(document).isNotNull();
    assertThat(document.getDocumentElement().getElementsByTagName("Some")).isNotNull();
  }
}
