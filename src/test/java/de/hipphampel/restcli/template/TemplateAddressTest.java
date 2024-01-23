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
package de.hipphampel.restcli.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.hipphampel.restcli.command.CommandAddress;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TemplateAddressTest {

  @ParameterizedTest
  @CsvSource({
      "'',      'Invalid template address \"\".'",
      "'@',     'Invalid template address \"@\".'",
      "abc,     'Invalid template address \"abc\".'",
      "'@abc',  'Invalid template address \"@abc\".'",
      "'abc@/', 'Invalid template address \"abc@/\".'",
  })
  void fromString_failed(String str, String message) {
    assertThatThrownBy(() -> TemplateAddress.fromString(str))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(message);
  }

  @ParameterizedTest
  @CsvSource({
      "abc@,         '',        abc",
      "abc@def/ghi , 'def/ghi', abc"
  })
  void fromString_ok(String in, String address, String name) {
    assertThat(TemplateAddress.fromString(in)).isEqualTo(
        new TemplateAddress(CommandAddress.fromString(address), name));
  }

}
