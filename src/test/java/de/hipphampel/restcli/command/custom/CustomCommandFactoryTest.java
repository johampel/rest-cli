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
package de.hipphampel.restcli.command.custom;

import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.cli.format.FormatBuilder;
import de.hipphampel.restcli.command.Command;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.RestCommandConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
public class CustomCommandFactoryTest extends CommandTestBase {

  @Inject
  CustomCommandFactory factory;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
  }

  @Test
  void parentCommand() {
    CommandConfig config = new CommandConfig();
    config.setType(Type.Parent);
    config.setSynopsis(UUID.randomUUID().toString());
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, UUID.randomUUID().toString()));

    Command command = factory.createCommand(CommandAddress.ROOT.child("test"), config);
    assertThat(command).isInstanceOf(CustomParentCommand.class);
    assertThat(command.name()).isEqualTo("test");
    assertThat(command.synopsis()).isEqualTo(config.getSynopsis());
    assertThat(command.helpSection(context, HelpSection.DESCRIPTION)).contains(
        FormatBuilder.buildFormat(config.getDescriptions().get(HelpSection.DESCRIPTION)));
  }

  @Test
  void aliasCommand() {
    CommandConfig config = new CommandConfig();
    config.setType(Type.Alias);
    config.setSynopsis(UUID.randomUUID().toString());
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, UUID.randomUUID().toString()));
    config.setAliasConfig(List.of("a", "b", "c"));

    Command command = factory.createCommand(CommandAddress.ROOT.child("test"), config);
    assertThat(command).isInstanceOf(CustomAliasCommand.class);
    assertThat(command.name()).isEqualTo("test");
    assertThat(command.synopsis()).isEqualTo(config.getSynopsis());
    assertThat(command.helpSection(context, HelpSection.DESCRIPTION)).contains(
        FormatBuilder.buildFormat(config.getDescriptions().get(HelpSection.DESCRIPTION)));
  }

  @Test
  void httpCommand() {
    CommandConfig config = new CommandConfig();
    config.setType(Type.Http);
    config.setSynopsis(UUID.randomUUID().toString());
    config.setDescriptions(Map.of(HelpSection.DESCRIPTION, UUID.randomUUID().toString()));
    config.setRestConfig(new RestCommandConfig());

    Command command = factory.createCommand(CommandAddress.ROOT.child("test"), config);
    assertThat(command).isInstanceOf(CustomHttpCommand.class);
    assertThat(command.name()).isEqualTo("test");
    assertThat(command.synopsis()).isEqualTo(config.getSynopsis());
    assertThat(command.helpSection(context, HelpSection.DESCRIPTION)).contains(
        FormatBuilder.buildFormat(config.getDescriptions().get(HelpSection.DESCRIPTION)));
  }

}
