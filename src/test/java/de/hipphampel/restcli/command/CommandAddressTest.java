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
package de.hipphampel.restcli.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CommandAddressTest {

  @Test
  void root() {
    assertThat(CommandAddress.ROOT.isRoot()).isTrue();
    assertThat(CommandAddress.ROOT.child("abc").isRoot()).isFalse();
  }

  @Test
  void child_str() {
    CommandAddress address = CommandAddress.ROOT;

    address = address.child("abc");
    assertThat(address).isEqualTo(new CommandAddress(CommandAddress.ROOT, "abc"));

    address = address.child("def");
    assertThat(address).isEqualTo(new CommandAddress(new CommandAddress(CommandAddress.ROOT, "abc"), "def"));
  }

  @Test
  void child_addr() {
    CommandAddress other = CommandAddress.fromString("uvw/xyz");

    CommandAddress address = CommandAddress.ROOT;
    assertThat(address.child(CommandAddress.ROOT)).isEqualTo(address);
    assertThat(address.child(other)).isEqualTo(other);

    address = address.child("abc");
    assertThat(address.child(CommandAddress.ROOT)).isEqualTo(address);
    assertThat(address.child(other)).isEqualTo(address.child("uvw").child("xyz"));
  }

  @ParameterizedTest
  @CsvSource({
      "''",
      "abc",
      "abc/def",
      "abc/def/ghi",
  })
  void toString(String str) {
    assertThat(CommandAddress.fromString(str)).hasToString(str);
  }

  @ParameterizedTest
  @CsvSource({
      "'',            '.'",
      "'abc',         './abc'",
      "'abc/def',     './abc/def'",
      "'abc/def/ghi', './abc/def/ghi'",
  })
  void toPath(String str, Path path) {
    assertThat(CommandAddress.fromString(str).toPath()).isEqualTo(path);
  }

  @ParameterizedTest
  @CsvSource({
      "'/',    '\"/\" is not a valid command address string'",
      "'abc/', '\"abc/\" is not a valid command address string'",
      "'/abc', '\"/abc\" is not a valid command address string'",
      "'a//c', '\"\" is not a valid command address name'",
  })
  void fromString_failed(String str, String message) {
    assertThatThrownBy(() -> CommandAddress.fromString(str))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(message);
  }

  @Test
  void fromString_ok() {
    assertThat(CommandAddress.fromString(null)).isNull();
    assertThat(CommandAddress.fromString("")).isEqualTo(CommandAddress.ROOT);
    assertThat(CommandAddress.fromString("abc")).isEqualTo(CommandAddress.ROOT.child("abc"));
    assertThat(CommandAddress.fromString("abc/def")).isEqualTo(CommandAddress.ROOT.child("abc").child("def"));
    assertThat(CommandAddress.fromString("abc/def/ghi")).isEqualTo(CommandAddress.ROOT.child("abc").child("def").child("ghi"));
  }

  @ParameterizedTest
  @CsvSource({
      ",          false",
      "'',        false",
      "'abc',     true",
      "'abc-123', true",
      "'-abc123', false",
      "'abc.123', false",
      "'abc123',  true",
      "'123-abc', false",
      "'123abc',  false",
  })
  void isValidCommandName(String name, boolean expected) {
    assertThat(CommandAddress.isValidCommandName(name)).isEqualTo(expected);
  }
}
