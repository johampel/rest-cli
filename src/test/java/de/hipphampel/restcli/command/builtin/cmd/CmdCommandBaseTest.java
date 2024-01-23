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

import de.hipphampel.restcli.cli.commandline.CommandLine;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.config.BodyConfig;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.CommandConfigTree;
import de.hipphampel.restcli.command.config.ParameterConfig;
import de.hipphampel.restcli.command.config.ParameterConfig.Style;
import de.hipphampel.restcli.command.config.ParameterListConfig;
import de.hipphampel.restcli.command.config.RestCommandConfig;
import de.hipphampel.restcli.io.InputStreamProviderConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class CmdCommandBaseTest extends CommandTestBase {

  CmdCommandBase command;

  private CommandConfig parentConfig;
  private CommandConfig aliasConfig;
  private CommandConfig httpConfig;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    command = new  CmdCommandBase("ignore", "ignore", new CommandLineSpec(false), Map.of()) {
      @Override
      public boolean execute(CommandContext context, CommandLine commandLine) {
        return false;
      }
    };
    command.commandRepository = commandRepository;
    command.commandConfigRepository = commandConfigRepository;

    parentConfig = new CommandConfig();
    parentConfig.setType(Type.Parent);
    parentConfig.setSynopsis("Original synopsis1");
    parentConfig.setDescriptions(Map.of(HelpSection.DESCRIPTION, "Original description1"));
    storeCommand(CommandAddress.fromString("a-parent"), parentConfig);

    aliasConfig = new CommandConfig();
    aliasConfig.setType(Type.Alias);
    aliasConfig.setSynopsis("Original synopsis3");
    aliasConfig.setDescriptions(Map.of(HelpSection.DESCRIPTION, "Original description3"));
    aliasConfig.setAliasConfig(List.of("the", "alias"));
    storeCommand(CommandAddress.fromString("a-parent/an-alias"), aliasConfig);

    httpConfig = new CommandConfig();
    httpConfig.setType(Type.Http);
    httpConfig.setAliasConfig(null);
    httpConfig.setSynopsis("Original synopsis4");
    httpConfig.setDescriptions(Map.of(HelpSection.DESCRIPTION, "Original description4"));
    httpConfig.setRestConfig(new RestCommandConfig()
        .setMethod("GET")
        .setBaseUri("https://example.com")
        .setHeaders(Map.of("foo", List.of("bar")))
        .setQueryParameters(Map.of("id", "4711"))
        .setParameters(new ParameterListConfig(List.of(
            new ParameterConfig(Style.RequiredPositional, "name", "id", null)
        )))
        .setBody(new BodyConfig(InputStreamProviderConfig.fromString("foo"), null)));

    storeCommand(CommandAddress.fromString("a-parent/a-http"), httpConfig);
  }

  @Test
  void getCommandConfigTree_withChildren() {
    CommandConfigTree tree = command.getCommandConfigTree(context, CommandAddress.fromString("a-parent"), false);
    CommandConfigTree expected = new CommandConfigTree()
        .setConfig(parentConfig)
        .setSubCommands(Map.of(
            "an-alias", new CommandConfigTree().setConfig(aliasConfig).setSubCommands(Map.of()),
            "a-http", new CommandConfigTree().setConfig(httpConfig).setSubCommands(Map.of())
        ));
    assertThat(tree).isEqualTo(expected);
  }

  @Test
  void getCommandConfigTree_noChildren() {
    CommandConfigTree tree = command.getCommandConfigTree(context, CommandAddress.fromString("a-parent"), true);
    CommandConfigTree expected = new CommandConfigTree()
        .setConfig(parentConfig)
        .setSubCommands(Map.of());
    assertThat(tree).isEqualTo(expected);
  }
}
