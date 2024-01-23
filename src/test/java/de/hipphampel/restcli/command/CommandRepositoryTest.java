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


import static de.hipphampel.restcli.TestUtils.stringToList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.hipphampel.restcli.command.CommandRepository.CustomRegistryEntry;
import de.hipphampel.restcli.command.CommandRepository.RegistryEntry;
import de.hipphampel.restcli.command.builtin.BuiltinCommand;
import de.hipphampel.restcli.command.builtin.BuiltinParentCommand;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.CommandConfigRepository;
import de.hipphampel.restcli.command.custom.CustomCommand;
import de.hipphampel.restcli.utils.Pair;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class CommandRepositoryTest {

  @Inject
  CommandConfigRepository configRepository;

  @Inject
  CommandRepository repository;

  Path rootDir;

  @BeforeEach
  void beforeEach(@TempDir Path rootDir) {
    this.rootDir = rootDir;
    repository.getBuiltins().clear();
  }

  @Test
  void getCommandInfo_found() {
    mockCustom(CommandAddress.fromString("test"), "foo1", true);
    mockCustom(CommandAddress.fromString("test/foo1"), "foo2", false);
    mockCustom(CommandAddress.fromString("test/foo2"), "foo3", false);
    mockCustom(CommandAddress.fromString("test/foo3"), "foo4", false);

    assertThat(repository.getCommandInfo(rootDir, CommandAddress.fromString("test"))).contains(
        new CommandInfo(
            CommandAddress.fromString("test"),
            true,
            false,
            "foo1",
            List.of(
                CommandAddress.fromString("test/foo1"),
                CommandAddress.fromString("test/foo2"),
                CommandAddress.fromString("test/foo3")
            )));
  }

  @Test
  void getCommandInfo_notFound() {
    assertThat(repository.getCommandInfo(null, CommandAddress.fromString("notFound"))).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
      "'not_found',             '',        'not_found'",
      "'foo/bar/baz/not_found', 'foo/bar', 'baz'",
      "'foo/bar',               'foo/bar', ",
      "'foo/not_found',         'foo',     'not_found'",
  })
  void getLongestExistingCommandAddress(String address, String existing, String child) {
    mockCustom(CommandAddress.fromString("foo"), "foo1", true);
    mockCustom(CommandAddress.fromString("foo/bar"), "foo2", false);

    assertThat(repository.getLongestExistingCommandAddress(rootDir, CommandAddress.fromString(address)))
        .isEqualTo(new Pair<>(CommandAddress.fromString(existing), Optional.ofNullable(child)));
  }

  @Test
  void getBuiltinRegistryEntry_notFound() {
    mockCustom(CommandAddress.fromString("custom"), "foo", false);

    assertThat(repository.getBuiltinRegistryEntry(CommandAddress.fromString("custom"))).isEmpty();
    assertThat(repository.getBuiltinRegistryEntry(CommandAddress.fromString("notFound"))).isEmpty();
  }

  @Test
  void getBuiltinRegistryEntry_found() {
    BuiltinCommand command = mockBuiltin(CommandAddress.fromString("builtin"), "foo1", true);
    mockBuiltin(CommandAddress.fromString("builtin/foo1"), "foo2", false);

    RegistryEntry entry = repository.getBuiltinRegistryEntry(CommandAddress.fromString("builtin")).orElseThrow();

    assertThat(entry.getChildren()).isEmpty();
    assertThat(entry.allowsChildren()).isTrue();
    assertThat(entry.getSynopsis()).isEqualTo("foo1");
    assertThat(entry.createCommand(repository)).isSameAs(command);
  }

  @Test
  void getCustomRegistryEntry_found() {
    mockCustom(CommandAddress.fromString("custom"), "foo1", true);
    mockCustom(CommandAddress.fromString("custom/child"), "foo2", false);

    RegistryEntry entry = repository.getCustomRegistryEntry(rootDir, CommandAddress.fromString("custom")).orElseThrow();

    assertThat(entry.getChildren()).isEmpty();
    assertThat(entry.allowsChildren()).isTrue();
    assertThat(entry.getSynopsis()).isEqualTo("foo1");
    assertThat(entry.createCommand(repository)).isInstanceOf(CustomCommand.class);
    assertThat(entry.createCommand(repository).address()).isEqualTo(CommandAddress.fromString("custom"));
  }

  @Test
  void getCustomRegistryEntry_notFound() {
    mockBuiltin(CommandAddress.fromString("builtin"), "foo2", true);
    mockCustom(CommandAddress.fromString("custom"), "foo", false);

    assertThat(repository.getCustomRegistryEntry(null, CommandAddress.fromString("builtin"))).isEmpty();
    assertThat(repository.getCustomRegistryEntry(null, CommandAddress.fromString("custom"))).isEmpty();
    assertThat(repository.getCustomRegistryEntry(rootDir, CommandAddress.ROOT)).isEmpty();
    assertThat(repository.getCustomRegistryEntry(null, CommandAddress.fromString("foo"))).isEmpty();
    assertThat(repository.getCustomRegistryEntry(rootDir, CommandAddress.fromString("foo"))).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({
      "true,  'test',  'test/foo1;test/foo2;test/foo3'",
      "false, 'test',  'test/foo1;test/foo2'",
      "true,  'other', ",
      "false, 'other', ",
      "true,  '',      'test;other'",
      "false, '',      'test'",
      "true,  'bad',   ",
      "false, 'bad',   ",
  })
  void addChildren(boolean withPath, String address, String expected) {
    // Arrange
    CustomRegistryEntry entry = new CustomRegistryEntry(CommandAddress.ROOT, new CommandConfig());
    mockBuiltin(CommandAddress.fromString("test"), "parent", true);
    mockBuiltin(CommandAddress.fromString("test/foo1"), "foo1", false);
    mockBuiltin(CommandAddress.fromString("test/foo2"), "foo2", true);
    mockCustom(CommandAddress.fromString("test/foo3"), "foo3", false);
    mockBuiltin(CommandAddress.fromString("test/foo2/bar"), "bar", false);
    mockCustom(CommandAddress.fromString("other"), "bar", false);

    // Act
    repository.addChildren(withPath ? rootDir : null, CommandAddress.fromString(address), entry);

    // Act
    assertThat(entry.getChildren()).containsAnyElementsOf(
        stringToList(expected).stream().map(CommandAddress::fromString).toList()
    );
  }

  @Test
  void fillBuiltinMapIfRequired() {
    assertThat(repository.getBuiltins()).isEmpty();

    repository.fillBuiltinMapIfRequired();

    assertThat(repository.getBuiltins()).isNotEmpty();
  }

  private BuiltinCommand mockBuiltin(CommandAddress address, String synopsis, boolean parent) {
    BuiltinCommand command = parent ? mock(BuiltinParentCommand.class) : mock(BuiltinCommand.class);
    when(command.address()).thenReturn(address);
    when(command.synopsis()).thenReturn(synopsis);
    repository.getBuiltins().put(address, command);
    return command;
  }

  private CommandConfig mockCustom(CommandAddress address, String synopsis, boolean parent) {
    CommandConfig config = new CommandConfig();
    config.setSynopsis(synopsis);
    config.setType(parent? Type.Parent:Type.Http);
    configRepository.store(rootDir, address, config);
    return config;
  }
}
