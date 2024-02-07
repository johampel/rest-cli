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
package de.hipphampel.restcli.cli.commandline;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.exception.UsageException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ValidatorsTest {

  private static final Positional ARG = positional("<arg>").build();

  @ParameterizedTest
  @CsvSource({
      "'',  'Value \"\" for \"<arg>\" is not a positive long value.'",
      "'-', 'Value \"-\" for \"<arg>\" is not a positive long value.'",
      "'a', 'Value \"a\" for \"<arg>\" is not a positive long value.'",
      "'0', 'Value \"0\" for \"<arg>\" is not a positive long value.'",
      "'1', ",
  })
  void POSITIVE_LONG_VALIDATOR(String value, String expectedException) {
    try {
      Validators.POSITIVE_LONG_VALIDATOR.accept(ARG, value);
      assertThat(expectedException).isNull();
    } catch (UsageException ue) {
      assertThat(ue).hasMessage(expectedException);
    }
  }

  @ParameterizedTest
  @CsvSource({
      "'',      'Value \"\" passed for \"<arg>\" is not a valid command name.'",
      "'abcd',  ",
      "'ab/cd', 'Value \"ab/cd\" passed for \"<arg>\" is not a valid command name.'",
      "'ab+cd', 'Value \"ab+cd\" passed for \"<arg>\" is not a valid command name.'",
  })
  void COMMAND_NAME_VALIDATOR(String value, String expectedException) {
    try {
      Validators.COMMAND_NAME_VALIDATOR.accept(ARG, value);
      assertThat(expectedException).isNull();
    } catch (UsageException ue) {
      assertThat(ue).hasMessage(expectedException);
    }
  }

  @ParameterizedTest
  @CsvSource({
      "'',      ",
      "'abcd',  ",
      "'ab/cd', ",
      "'ab+cd', 'Value \"ab+cd\" passed for \"<arg>\" is not a valid command address.'",
  })
  void COMMAND_ADDRESS_VALIDATOR(String value, String expectedException) {
    try {
      Validators.COMMAND_ADDRESS_VALIDATOR.accept(ARG, value);
      assertThat(expectedException).isNull();
    } catch (UsageException ue) {
      assertThat(ue).hasMessage(expectedException);
    }
  }

  @ParameterizedTest
  @CsvSource({
      "'',      'Value \"\" passed for \"<arg>\" is not a valid output template address.'",
      "'abcd@', ",
      "'ab@cd', ",
      "'ab+cd', 'Value \"ab+cd\" passed for \"<arg>\" is not a valid output template address.'",
  })
  void TEMPLATE_ADDRESS_VALIDATOR(String value, String expectedException) {
    try {
      Validators.TEMPLATE_ADDRESS_VALIDATOR.accept(ARG, value);
      assertThat(expectedException).isNull();
    } catch (UsageException ue) {
      assertThat(ue).hasMessage(expectedException);
    }
  }

  @ParameterizedTest
  @CsvSource({
      "'',      'Key value pair \"\" passed for \"<arg>\" must not be empty.'",
      "'abcd',  ",
      "'ab=cd', ",
      "'=cd',   'Key value pair \"=cd\" passed for \"<arg>\" has no key part.'",
  })
  void KEY_OPT_VALUE_VALIDATOR(String value, String expectedException) {
    try {
      Validators.KEY_OPT_VALUE_VALIDATOR.accept(ARG, value);
      assertThat(expectedException).isNull();
    } catch (UsageException ue) {
      assertThat(ue).hasMessage(expectedException);
    }
  }

  @ParameterizedTest
  @CsvSource({
      "'',      'Key value pair \"\" passed for \"<arg>\" must not be empty.'",
      "'abcd',  'Key value pair \"abcd\" passed for \"<arg>\" has no `=` sign.'",
      "'ab=cd', ",
      "'=cd',   'Key value pair \"=cd\" passed for \"<arg>\" has no key part.'",
  })
  void KEY_VALUE_VALIDATOR(String value, String expectedException) {
    try {
      Validators.KEY_VALUE_VALIDATOR.accept(ARG, value);
      assertThat(expectedException).isNull();
    } catch (UsageException ue) {
      assertThat(ue).hasMessage(expectedException);
    }
  }

}
