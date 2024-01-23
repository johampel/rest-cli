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
package de.hipphampel.restcli.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClosableInputStreamProviderTest {

  @ParameterizedTest
  @CsvSource({
      "  100, java.io.FileInputStream",
      "10000, java.io.ByteArrayInputStream"
  })
  void ofStream(int maxMemorySize, Class<?> expectedImpl) throws IOException {
    String content = "0123456789".repeat(1000); // 10000 characters

    try (ClosableInputStreamProvider isp = ClosableInputStreamProvider.ofStream(
        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), maxMemorySize)) {
      try (InputStream in1 = isp.open(); ByteArrayOutputStream out1 = new ByteArrayOutputStream()) {
        assertThat(in1).isInstanceOf(expectedImpl);
        try (InputStream in2 = isp.open(); ByteArrayOutputStream out2 = new ByteArrayOutputStream()) {
          assertThat(in2).isInstanceOf(expectedImpl);
          in2.transferTo(out2);
          assertThat(out2.toString(StandardCharsets.UTF_8)).isEqualTo(content);
        }
        in1.transferTo(out1);
        assertThat(out1.toString(StandardCharsets.UTF_8)).isEqualTo(content);
      }
    }
  }
}
