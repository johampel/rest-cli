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
package de.hipphampel.restcli.command.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.hipphampel.restcli.command.config.ParameterConfig.Style;
import de.hipphampel.restcli.exception.ExecutionException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ParameterConfigListTest {

  @Test
  void setParameters_ok() {
    ParameterConfig param1 = new ParameterConfig(Style.MultiOption, "<abc>", "mno", List.of("-a", "--ah"));
    ParameterConfig param2 = new ParameterConfig(Style.SingleOption, "<def>", "pqr", List.of("-b", "--be"));
    ParameterConfig param3 = new ParameterConfig(Style.RequiredPositional, "<ghi>", "stu", null);
    ParameterConfig param4 = new ParameterConfig(Style.RequiredMultiPositional, "<jkl>", "vwx", null);

    ParameterListConfig list = new ParameterListConfig(List.of(param1, param2, param3, param4));

    assertThat(list.getParameters()).containsExactly(param1, param2, param3, param4);
    assertThat(list.getCommandLineSpec().usageString()).isEqualTo("[-a|--ah <abc>]... [-b|--be <def>] <ghi> <jkl>...");
    assertThat(list.getConfigMap()).isEqualTo(Map.of(
        list.getCommandLineSpec().params().options().get(0), param1,
        list.getCommandLineSpec().params().options().get(1), param2,
        list.getCommandLineSpec().params().positionals().get(0), param3,
        list.getCommandLineSpec().params().positionals().get(1), param4));
    assertThat(list.getPositionalMap()).isEqualTo(Map.of(
        param1, list.getCommandLineSpec().params().options().get(0).params().positionals().get(0),
        param2, list.getCommandLineSpec().params().options().get(1).params().positionals().get(0),
        param3, list.getCommandLineSpec().params().positionals().get(0),
        param4, list.getCommandLineSpec().params().positionals().get(1)));
  }

  @Test
  void setParameters_duplicateVariable() {
    ParameterConfig param1 = new ParameterConfig(Style.RequiredPositional, "<ghi>", "same", null);
    ParameterConfig param2 = new ParameterConfig(Style.RequiredPositional, "<jkl>", "same", null);

    assertThatThrownBy(() -> new ParameterListConfig(List.of(param1, param2)))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Variable \"same\" bound twice in parameter list.");
  }

  @Test
  void setParameters_badCommandLine() {
    ParameterConfig param1 = new ParameterConfig(Style.RequiredMultiPositional, "<ghi>", "abc", null);
    ParameterConfig param2 = new ParameterConfig(Style.RequiredMultiPositional, "<jkl>", "def", null);

    assertThatThrownBy(() -> new ParameterListConfig(List.of(param1, param2)))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Positional \"<ghi>\" must be fixed sized, since further positionals follow.");
  }
}
