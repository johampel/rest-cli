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

import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class CommandConfigRepositoryTest {

  private final String SAMPLE_STR1 = """
      {
        "type": "Parent",
        "synopsis": "synopsis1"
      }
      """;
  private final String SAMPLE_STR2 = """
      {
        "type": "Parent",
        "synopsis": "synopsis2"
      }
      """;
  private final String SAMPLE_STR3 = """
      {
        "type": "Parent",
        "synopsis": "synopsis3"
      }
      """;

  @Inject
  CommandConfigRepository repository;

  Path rootDir;

  @BeforeEach
  void beforeEach(@TempDir Path rootDir) {
    this.rootDir = rootDir;
  }

  @ParameterizedTest
  @CsvSource({
      "false,  ,        ",
      "false,  abc/def, ",
      "true,   ,        ",
      "true,   abc/def, commands/./abc/def/command.json",
  })
  void getPath(boolean rootDirProvided, String addressStr, String expectedStr) {
    CommandAddress address = Optional.ofNullable(addressStr).map(CommandAddress::fromString).orElse(null);
    Path expected = Optional.ofNullable(expectedStr).map(rootDir::resolve).orElse(null);

    assertThat(repository.getPath(rootDirProvided ? rootDir : null, address)).isEqualTo(Optional.ofNullable(expected));
  }

  @Test
  void getValidatedPath_fail() throws IOException {
    createSampleConfig(rootDir.resolve("def/command.json"), "garbage");

    assertThat(repository.getValidatedPath(rootDir, CommandAddress.fromString("not/found"))).isEmpty();
    assertThat(repository.getValidatedPath(rootDir, CommandAddress.fromString("def"))).isEmpty();
  }

  @Test
  void getValidatedPath_ok() throws IOException {
    createSampleConfig(rootDir.resolve("commands/def/command.json"), SAMPLE_STR1);

    assertThat(repository.getValidatedPath(rootDir, CommandAddress.fromString("def"))).contains(
        rootDir.resolve("commands/./def/command.json"));

  }

  @Test
  void getChildren_notFound() throws IOException {
    createSampleConfig(rootDir.resolve("def/foo.json"), SAMPLE_STR1);

    assertThat(repository.getChildren(null, null)).isEmpty();
    assertThat(repository.getChildren(null, CommandAddress.ROOT)).isEmpty();
    assertThat(repository.getChildren(rootDir, null)).isEmpty();
    assertThat(repository.getChildren(rootDir, CommandAddress.ROOT)).isEmpty();
    assertThat(repository.getChildren(rootDir, CommandAddress.fromString("abc"))).isEmpty();

  }

  @Test
  void getChildren_found() throws IOException {
    createSampleConfig(rootDir.resolve("commands/abc/command.json"), SAMPLE_STR1);
    createSampleConfig(rootDir.resolve("commands/abc/def/command.json"), SAMPLE_STR2);
    createSampleConfig(rootDir.resolve("commands/abc/ghi/command.json"), SAMPLE_STR3);

    assertThat(repository.getChildren(rootDir, CommandAddress.ROOT)).containsExactlyInAnyOrder(
        CommandAddress.fromString("abc")
    );
    assertThat(repository.getChildren(rootDir, CommandAddress.fromString("abc"))).containsExactlyInAnyOrder(
        CommandAddress.fromString("abc/def"),
        CommandAddress.fromString("abc/ghi")
    );
  }

  @Test
  void load_notFound() {
    CommandAddress address = CommandAddress.fromString("not/found");
    assertThatThrownBy(() -> repository.load(rootDir, address))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Failed to load configuration for command \"not/found\".")
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void load_ok() throws IOException {
    createSampleConfig(rootDir.resolve("commands/abc/def/command.json"), SAMPLE_STR2);
    CommandAddress address = CommandAddress.fromString("abc/def");
    CommandConfig config = repository.load(rootDir, address);

    assertThat(config).isNotNull();
    assertThat(config.getSynopsis()).isEqualTo("synopsis2");
  }


  @Test
  void store_config() {
    CommandAddress address = CommandAddress.fromString("abc/def");
    CommandConfig config = new CommandConfig();
    config.setSynopsis("foo");
    config.setType(Type.Parent);
    Path path = repository.getPath(rootDir, address).orElseThrow();

    assertThat(Files.exists(path)).isFalse();

    repository.store(rootDir, address, config);

    assertThat(Files.exists(path)).isTrue();
    assertThat(repository.load(rootDir, address)).isEqualTo(config);
  }

  @Test
  void store_tree() {
    CommandAddress address = CommandAddress.fromString("abc/def");
    CommandConfig config1 = new CommandConfig();
    config1.setSynopsis("foo");
    config1.setType(Type.Parent);
    CommandConfig config2 = new CommandConfig();
    config2.setSynopsis("bar");
    config2.setType(Type.Parent);
    CommandConfigTree subTree = new CommandConfigTree();
    subTree.setConfig(config2);
    CommandConfigTree tree = new CommandConfigTree();
    tree.setConfig(config1);
    tree.setSubCommands(Map.of("bar", subTree));
    Path path1 = repository.getPath(rootDir, address).orElseThrow();
    Path path2 = repository.getPath(rootDir, address.child("bar")).orElseThrow();

    assertThat(Files.exists(path1)).isFalse();
    assertThat(Files.exists(path2)).isFalse();

    repository.store(rootDir, address, tree);

    assertThat(Files.exists(path1)).isTrue();
    assertThat(Files.exists(path2)).isTrue();
    assertThat(repository.load(rootDir, address)).isEqualTo(config1);
    assertThat(repository.load(rootDir, address.child("bar"))).isEqualTo(config2);
  }

  @Test
  void delete_fail_hasChildren() throws IOException {
    createSampleConfig(rootDir.resolve("commands/abc/command.json"), SAMPLE_STR1);
    createSampleConfig(rootDir.resolve("commands/abc/def/command.json"), SAMPLE_STR2);
    createSampleConfig(rootDir.resolve("commands/abc/ghi/command.json"), SAMPLE_STR3);
    CommandAddress address = CommandAddress.fromString("abc");

    assertThatThrownBy(() -> repository.delete(rootDir, address, false))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Command \"abc\" cannot be deleted since it has at least one sub-command.");
    assertThat(repository.getValidatedPath(rootDir, address)).isPresent();
  }

  @Test
  void delete_ok_hasChildren() throws IOException {
    createSampleConfig(rootDir.resolve("commands/abc/command.json"), SAMPLE_STR1);
    createSampleConfig(rootDir.resolve("commands/abc/def/command.json"), SAMPLE_STR2);
    createSampleConfig(rootDir.resolve("commands/abc/ghi/command.json"), SAMPLE_STR3);
    CommandAddress address = CommandAddress.fromString("abc");

    repository.delete(rootDir, address, true);
    assertThat(Files.exists(repository.getDescriptorDir(rootDir, CommandAddress.ROOT))).isTrue();
    assertThat(Files.exists(repository.getDescriptorDir(rootDir, address))).isFalse();
  }

  @Test
  void delete_ok_noRoot() throws IOException {
    createSampleConfig(rootDir.resolve("commands/abc/command.json"), SAMPLE_STR1);
    CommandAddress address = CommandAddress.fromString("abc");

    repository.delete(rootDir, address, false);
    assertThat(Files.exists(repository.getDescriptorDir(rootDir, CommandAddress.ROOT))).isTrue();
    assertThat(Files.exists(repository.getDescriptorDir(rootDir, address))).isFalse();
  }

  @Test
  void delete_ok_root() throws IOException {
    Files.createDirectories(repository.getDescriptorDir(rootDir, CommandAddress.ROOT));
    repository.delete(rootDir, CommandAddress.ROOT, false);
    assertThat(Files.exists(repository.getDescriptorDir(rootDir, CommandAddress.ROOT))).isTrue();
  }

  @Test
  void move_ok() throws IOException {
    createSampleConfig(rootDir.resolve("commands/abc/command.json"), SAMPLE_STR1);
    createSampleConfig(rootDir.resolve("commands/abc/def/command.json"), SAMPLE_STR1);
    CommandAddress address = CommandAddress.fromString("abc");
    CommandAddress newAddress = CommandAddress.fromString("xyz");

    repository.move(rootDir, address, newAddress);
    assertThat(repository.getValidatedPath(rootDir, address)).isEmpty();
    assertThat(repository.getValidatedPath(rootDir, newAddress)).isPresent();
    assertThat(repository.getValidatedPath(rootDir, newAddress.child("def"))).isPresent();
  }

  @Test
  void move_fail_target_exists() throws IOException {
    createSampleConfig(rootDir.resolve("commands/abc/command.json"), SAMPLE_STR1);
    createSampleConfig(rootDir.resolve("commands/def/command.json"), SAMPLE_STR1);
    CommandAddress address = CommandAddress.fromString("abc");
    CommandAddress newAddress = CommandAddress.fromString("def");

    assertThatThrownBy(() -> repository.move(rootDir, address, newAddress))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Command \"def\" already exists.");
  }

  @Test
  void move_fail_source_not_exists() throws IOException {
    CommandAddress address = CommandAddress.fromString("abc");
    CommandAddress newAddress = CommandAddress.fromString("def");

    assertThatThrownBy(() -> repository.move(rootDir, address, newAddress))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Command \"abc\" not exists.");
  }

  private void createSampleConfig(Path path, String content) throws IOException {
    if (!Files.exists(path.getParent())) {
      Files.createDirectories(path.getParent());
    }
    Files.writeString(path, content);
  }
}
  