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
package de.hipphampel.restcli;

import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.io.InputStreamProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class TestUtils {

  public static List<String> stringToList(String listStr) {
    return listStr == null ? List.of() : stringToList(listStr, ";");
  }

  public static List<String> stringToList(String listStr, String delimiter) {
    return Arrays.asList(listStr.split(delimiter));
  }

  public static void assertInputStreamProvider(InputStreamProvider inputStreamProvider, String expected) {
    try (InputStream in = inputStreamProvider.open(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo(expected);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static String getResourceContent(String location) throws IOException {
    try (InputStream in = TestUtils.class.getResourceAsStream(location); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      return out.toString(StandardCharsets.UTF_8);
    }
  }

}
