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
package de.hipphampel.restcli.cli.format;

import static de.hipphampel.restcli.cli.format.FormatTestBase.assertFormat;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class InlineTest {

  @ParameterizedTest
  @CsvSource({
      "'',      false",
      "'\n \t', true",
      "' a ',   false"
  })
  void isWhitespace(String chars, boolean expected) {
    assertThat(new Inline(chars, -1).isWhitespace()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'',  true",
      "' ', false",
      "'a', false"
  })
  void isEmpty(String chars, boolean expected) {
    assertThat(new Inline(chars, -1).isEmpty()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'',    0",
      "'a',   1",
      "'abc', 3"
  })
  void getVisibleWidth(String chars, int expected) {
    assertThat(new Inline(chars, -1).getVisibleWidth()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "'a b c',  0, false, 'a b c'",
      "'a b c', 15, false, 'a b c'",
      "'a b c',  0, true,  'a b c'",
      "'a b c', 15, true,  '\033[0;1;3;4;9ma b c\033[0m'",
  })
  void appendTo(String chars, int style, boolean withStyles, String expected) {
    Inline inline = new Inline(chars, style);
    assertFormat(inline, withStyles, expected);
  }

}
