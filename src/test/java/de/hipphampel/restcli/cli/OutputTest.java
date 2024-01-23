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
package de.hipphampel.restcli.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OutputTest {

  private StringWriter buffer;
  private Output output;

  @BeforeEach
  void beforeEach() {
    this.buffer = new StringWriter();
    this.output = new Output(buffer);
  }

  @Test
  void chars() {
    assertThat(output.chars("%s is not expanded")).isSameAs(output);
    assertOutput("""
        %s is not expanded""");
  }

  @Test
  void charsf() {
    assertThat(output.charsf("%s is expanded", "The placeholder")).isSameAs(output);
    assertOutput("""
        The placeholder is expanded""");
  }

  @Test
  void line() {
    assertThat(output.line("%s is not expanded")).isSameAs(output);
    assertOutput("""
        %s is not expanded
        """);
  }

  @Test
  void linef() {
    assertThat(output.linef("%s is expanded", "The placeholder")).isSameAs(output);
    assertOutput("""
        The placeholder is expanded
        """);
  }

  @Test
  void newline() {
    assertThat(output.newline()).isSameAs(output);
    assertOutput(System.lineSeparator());
  }


  @ParameterizedTest
  @CsvSource({
      "false, -1, 'This is a small %s\n'",
      "false, 10, 'This is a\nsmall %s\n'",
      "true,  -1, '\033[0;1mThis\033[0m \033[0;3mis\033[0m a small %s\n'",
      "true,  10, '\033[0;1mThis\033[0m \033[0;3mis\033[0m a\nsmall %s\n'",
  })
  void markdown(boolean withStyles, int requestedWidth, String expected) {
    String input = "**This** *is* a small %s";
    output = output.withOutputWidth(requestedWidth).withStyles(withStyles);
    assertThat(output.markdown(input)).isSameAs(output);
    assertOutput(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "false, -1, 'This is a small paragraph\n'",
      "false, 10, 'This is a\nsmall\nparagraph\n'",
      "true,  -1, '\033[0;1mThis\033[0m \033[0;3mis\033[0m a small paragraph\n'",
      "true,  10, '\033[0;1mThis\033[0m \033[0;3mis\033[0m a\nsmall\nparagraph\n'",
  })
  void markdownf(boolean withStyles, int requestedWidth, String expected) {
    String input = "**This** *is* a small %s";
    output = output.withOutputWidth(requestedWidth).withStyles(withStyles);
    assertThat(output.markdownf(input, "paragraph")).isSameAs(output);
    assertOutput(expected);
  }

  void assertOutput(String expected) {

    assertThat(buffer).hasToString(expected);
  }
}
