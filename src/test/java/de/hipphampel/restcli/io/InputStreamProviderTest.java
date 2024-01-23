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

import static de.hipphampel.restcli.TestUtils.assertInputStreamProvider;
import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.io.InputStreamProviderConfig.Type;
import java.net.URL;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InputStreamProviderTest {

  @Test
  void ofString() {
    InputStreamProvider provider = InputStreamProvider.ofString("äöüabc");

    assertInputStreamProvider(provider, "äöüabc");
  }

  @Test
  void ofUrl() {
    URL url = InputStreamProviderTest.class.getResource("/testdata/test.txt");
    InputStreamProvider provider = InputStreamProvider.ofURL(url);

    assertInputStreamProvider(provider, "Testdata ${abc}");
  }

  @Test
  void ofPath() {
    Path path = Path.of(InputStreamProviderTest.class.getResource("/testdata/test.txt").getFile());
    InputStreamProvider provider = InputStreamProvider.ofPath(path);

    assertInputStreamProvider(provider, "Testdata ${abc}");
  }

  @ParameterizedTest
  @CsvSource({
      "string, 'foo',                false, 'string:foo'",
      "path,   'the/path',           false, 'path:the/path'",
      "url,    'http://example.com', false, 'url:http://example.com'",
      "string, 'foo',                true,  '%string:foo'",
      "path,   'the/path',           true,  '%path:the/path'",
      "url,    'http://example.com', true,  '%url:http://example.com'",
  })
  void toString(Type type, String value, boolean interpolate, String expected) {
    assertThat(new InputStreamProviderConfig(type, value,interpolate)).hasToString(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'string:foo',                    string,  'foo',                   false",
      "'foo',                           string,  'foo',                   false",
      "'string and other data',         string,  'string and other data', false",
      "'path:the/path',                 path,    'the/path',              false",
      "'url:http://example.com',        url,     'http://example.com',    false",
      "'builtin:/templates/test.ftl',   builtin, '/templates/test.ftl',   false",
      "'%string:foo',                   string,  'foo',                   true",
      "'%foo',                          string,  'foo',                   true",
      "'%string and other data',        string,  'string and other data', true",
      "'%path:the/path',                path,    'the/path',              true",
      "'%url:http://example.com',       url,     'http://example.com',    true",
      "'%builtin:/templates/test.ftl',  builtin, '/templates/test.ftl',   true",
  })
  void fromString(String str, Type type, String value, boolean interpolate) {
    assertThat(InputStreamProviderConfig.fromString(str)).isEqualTo(new InputStreamProviderConfig(type, value, interpolate));
  }
}
