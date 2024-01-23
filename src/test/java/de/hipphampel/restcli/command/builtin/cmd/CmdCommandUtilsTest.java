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
package de.hipphampel.restcli.command.builtin.cmd;

import static de.hipphampel.restcli.cli.commandline.CommandLineSpec.positional;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_HEADER;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_LOAD_BODY;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_OPTION;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_POSITIONAL;
import static de.hipphampel.restcli.command.builtin.cmd.CmdCommandUtils.CMD_OPT_QUERY_PARAMETER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.hipphampel.restcli.TestUtils;
import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.config.BodyConfig;
import de.hipphampel.restcli.command.config.ParameterConfig;
import de.hipphampel.restcli.command.config.ParameterConfig.Style;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.exception.UsageException;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class CmdCommandUtilsTest extends CommandTestBase {

  @BeforeEach
  public void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @Test
  void collectDescriptions_ok() {
    CommandLineSpec commandLineSpec = new CommandLineSpec(true, CmdCommandUtils.CMD_OPT_DESCRIPTION);
    CommandLine commandLine = commandLineParser.parseCommandLine(commandLineSpec, List.of(
        "-dfoo",
        "-d", "string:bar", "--arguments",
        "--description", "--infos", "baz"
    ));
    Map<HelpSection, String> descriptions = CmdCommandUtils.collectDescriptions(context, commandLine, new HashMap<>());
    assertThat(descriptions).isEqualTo(Map.of(
        HelpSection.DESCRIPTION, "foo",
        HelpSection.ARGS_AND_OPTIONS, "bar",
        HelpSection.FURTHER_INFOS, "baz"
    ));
  }

  @Test
  void collectDescriptions_fail() {
    CommandLineSpec commandLineSpec = new CommandLineSpec(true, CmdCommandUtils.CMD_OPT_DESCRIPTION);
    CommandLine commandLine = commandLineParser.parseCommandLine(commandLineSpec, List.of(
        "-dfoo",
        "-d", "string:bar", "--main"
    ));
    assertThatThrownBy(() -> CmdCommandUtils.collectDescriptions(context, commandLine, new HashMap<>()))
        .isInstanceOf(UsageException.class)
        .hasMessage("Specified more than one description for the same section.");
  }

  @Test
  void collectQueryParameters_ok() {
    CommandLineSpec commandLineSpec = new CommandLineSpec(true, CMD_OPT_QUERY_PARAMETER);
    CommandLine commandLine = commandLineParser.parseCommandLine(commandLineSpec, List.of(
        "-qfoo=value1",
        "--query", "bar=value2"
    ));
    Map<String, String> parameters = CmdCommandUtils.collectQueryParameters(commandLine, new HashMap<>());
    assertThat(parameters).isEqualTo(Map.of(
        "foo", "value1",
        "bar", "value2"
    ));
  }

  @Test
  void collectQueryParameters_fail() {
    CommandLineSpec commandLineSpec = new CommandLineSpec(true, CMD_OPT_QUERY_PARAMETER);
    CommandLine commandLine = commandLineParser.parseCommandLine(commandLineSpec, List.of(
        "-qfoo=value1",
        "--query", "foo=value2"
    ));
    assertThatThrownBy(() -> CmdCommandUtils.collectQueryParameters(commandLine, new HashMap<>()))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Duplicate definition of query parameter \"foo\".");
  }


  @Test
  void collectHeaders() {
    CommandLineSpec commandLineSpec = new CommandLineSpec(true, CMD_OPT_HEADER);
    CommandLine commandLine = commandLineParser.parseCommandLine(commandLineSpec, List.of(
        "-hfoo=value1",
        "--header", "foo=value2",
        "-hbar=value3",
        "--header", "baz=value4"
    ));
    Map<String, List<String>> headers = CmdCommandUtils.collectHeaders(context, commandLine, new HashMap<>());
    assertThat(headers).isEqualTo(Map.of(
        "foo", List.of("value1", "value2"),
        "bar", List.of("value3"),
        "baz", List.of("value4")
    ));
  }

  @Test
  void collectParameters() {
    CommandLineSpec commandLineSpec = new CommandLineSpec(true, CMD_OPT_POSITIONAL, CMD_OPT_OPTION);
    CommandLine commandLine = commandLineParser.parseCommandLine(commandLineSpec, List.of(
        "-pname1=var1",
        "--option", "n=var2",
        "-oname2=var3"
    ));
    assertThat(CmdCommandUtils.collectParameters(commandLine, new ArrayList<>())).isEqualTo(List.of(
        new ParameterConfig(Style.RequiredPositional, "name1", "var1", List.of()),
        new ParameterConfig(Style.SingleOption, "n", "var2", List.of("-n")),
        new ParameterConfig(Style.SingleOption, "name2", "var3", List.of("--name2"))
    ));
  }

  @ParameterizedTest
  @CsvSource({
      "'abc',                             'abc'",
      "'%${abc}',                         '%${abc}'",
      "'%builtin:/templates/test.ftl',    '%builtin:/templates/test.ftl'",
      "'-l;%builtin:/templates/test.ftl', '%${a} Foo bar'",
  })
  void createBodyConfig(String args, String expected) {
    Positional bodyArg = positional("body").build();

    CommandLineSpec commandLineSpec = new CommandLineSpec(true, CMD_OPT_LOAD_BODY, bodyArg);
    CommandLine commandLine = commandLineParser.parseCommandLine(commandLineSpec, TestUtils.stringToList(args));

    BodyConfig body = CmdCommandUtils.createBodyConfig(context, commandLine, bodyArg);
    assertThat(body).isEqualTo(new BodyConfig(InputStreamProviderConfig.fromString(expected), null));
  }

}
