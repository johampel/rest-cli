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
package de.hipphampel.restcli.env;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.Pair;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@QuarkusTest
class EnvironmentRepositoryTest {

  @Inject
  EnvironmentRepository repository;
  @Inject
  ObjectMapper objectMapper;

  Path baseDir;
  Path envDir;

  @BeforeEach
  void beforeEach(@TempDir Path baseDir) {
    this.baseDir = baseDir;
    this.envDir = baseDir.resolve(EnvironmentRepository.ENVIRONMENT_DIR);
  }

  @ParameterizedTest
  @CsvSource({
      "_empty,   true",
      "notfound, false",
      "exists,   true"
  })
  void existsEnvironment(String name, boolean exists) throws IOException {
    sampleConfig("exists", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));

    assertThat(repository.existsEnvironment(baseDir, name)).isEqualTo(exists);
  }

  @Test
  void listEnvironments_noParent() throws IOException {
    sampleConfig("root", "null", Map.of("a", "b"), Map.of("c", List.of("d")));
    sampleConfig("parent", "root", Map.of("e", "f"), Map.of("g", List.of("h")));
    sampleConfig("config1", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));
    sampleConfig("config2", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));

    assertThat(repository.listEnvironments(baseDir)).containsExactlyInAnyOrder("root", "parent", "config1", "config2");
  }

  @ParameterizedTest
  @CsvSource({
      ",        '[]'",
      "unknown, '[]'",
      "root,    '[parent]'",
      "parent,  '[config1, config2]'",
      "config1, '[]'",
  })
  void listEnvironments_withParent(String parent, String expected) throws IOException {
    sampleConfig("root", "null", Map.of("a", "b"), Map.of("c", List.of("d")));
    sampleConfig("parent", "root", Map.of("e", "f"), Map.of("g", List.of("h")));
    sampleConfig("config1", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));
    sampleConfig("config2", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));

    assertThat(repository.listEnvironments(baseDir, parent).stream().sorted().toList()).hasToString(expected);
  }

  @Test
  void createTransientEnvironment_noParent() {
    Environment environment = repository.createTransientEnvironment("foo", null);

    assertThat(environment).isEqualTo(new Environment(
        null,
        "foo",
        EnvironmentConfig.EMPTY,
        EnvironmentConfig.EMPTY));
  }

  @Test
  void createTransientEnvironment_withParent() throws IOException {
    sampleConfig("root", null, Map.of("a", "b"), Map.of("c", List.of("d")));
    sampleConfig("parent", "root", Map.of("e", "f"), Map.of("g", List.of("h")));
    Environment parent = repository.getEnvironment(baseDir, "parent").orElseThrow();

    Environment environment = repository.createTransientEnvironment("foo", parent);

    assertThat(environment).isEqualTo(new Environment(
        "parent",
        "foo",
        new EnvironmentConfig(
            "root",
            parent.getVariables(),
            parent.getHeaders(),
            null),
        new EnvironmentConfig("parent", Map.of(), Map.of(), null)));
  }

  @Test
  void storeEnvironment_noParent_new_ok() {
    Environment environment = repository.createTransientEnvironment("test", null);
    environment.setLocalVariables(Map.of("1", "2"));
    assertThat(repository.existsEnvironment(baseDir, "test")).isFalse();

    repository.storeEnvironment(baseDir, environment, false);

    assertThat(repository.getEnvironment(baseDir, "test")).contains(environment);
    assertThat(Files.exists(repository.getPath(baseDir, "test"))).isTrue();
  }

  @Test
  void storeEnvironment_withParent_new_ok() {
    Environment parent = repository.createTransientEnvironment("parent", null);
    repository.storeEnvironment(baseDir, parent, false);
    Environment environment = repository.createTransientEnvironment("test", parent);
    environment.setLocalVariables(Map.of("1", "2"));
    assertThat(repository.existsEnvironment(baseDir, "test")).isFalse();

    repository.storeEnvironment(baseDir, environment, false);

    assertThat(repository.getEnvironment(baseDir, "test")).contains(environment);
    assertThat(Files.exists(repository.getPath(baseDir, "test"))).isTrue();
  }

  @Test
  void storeEnvironment_update_ok() {
    Environment environment = repository.createTransientEnvironment("test", null);
    environment.setLocalVariables(Map.of("1", "2"));
    repository.storeEnvironment(baseDir, environment, false);

    environment.setLocalVariables(Map.of("1", "2", "3", "4"));
    repository.storeEnvironment(baseDir, environment, false);

    assertThat(repository.getEnvironment(baseDir, "test")).contains(environment);
    assertThat(Files.exists(repository.getPath(baseDir, "test"))).isTrue();
  }

  @Test
  void storeEnvironment_rename_ok() {
    Environment environment = repository.createTransientEnvironment("test", null);
    environment.setLocalVariables(Map.of("1", "2"));
    repository.storeEnvironment(baseDir, environment, false);

    environment.setName("renamed");
    repository.storeEnvironment(baseDir, environment, false);

    assertThat(repository.getEnvironment(baseDir, "test")).isEmpty();
    assertThat(repository.getEnvironment(baseDir, "renamed")).contains(environment);
    assertThat(Files.exists(repository.getPath(baseDir, "test"))).isFalse();
    assertThat(Files.exists(repository.getPath(baseDir, "renamed"))).isTrue();
  }

  @Test
  void storeEnvironment_rename_forced_ok() {
    repository.storeEnvironment(baseDir, repository.createTransientEnvironment("existing", null), false);
    Environment environment = repository.createTransientEnvironment("test", null);
    environment.setLocalVariables(Map.of("1", "2"));
    repository.storeEnvironment(baseDir, repository.createTransientEnvironment("existing", null), false);
    repository.storeEnvironment(baseDir, environment, false);
    assertThat(Files.exists(repository.getPath(baseDir, "test"))).isTrue();
    assertThat(Files.exists(repository.getPath(baseDir, "existing"))).isTrue();

    environment.setName("existing");
    repository.storeEnvironment(baseDir, environment, true);

    assertThat(repository.getEnvironment(baseDir, "test")).isEmpty();
    assertThat(repository.getEnvironment(baseDir, "existing")).contains(environment);
    assertThat(Files.exists(repository.getPath(baseDir, "test"))).isFalse();
    assertThat(Files.exists(repository.getPath(baseDir, "existing"))).isTrue();
  }

  @Test
  void storeEnvironment_rename_noForced_fail() {
    Environment existing = repository.createTransientEnvironment("existing", null);
    repository.storeEnvironment(baseDir, existing, false);
    Environment environment = repository.createTransientEnvironment("test", null);
    environment.setLocalVariables(Map.of("1", "2"));
    repository.storeEnvironment(baseDir, repository.createTransientEnvironment("existing", null), false);
    repository.storeEnvironment(baseDir, environment, false);
    assertThat(Files.exists(repository.getPath(baseDir, "test"))).isTrue();
    assertThat(Files.exists(repository.getPath(baseDir, "existing"))).isTrue();

    environment.setName("existing");
    assertThatThrownBy(() -> repository.storeEnvironment(baseDir, environment, false))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Cannot overwrite the already existing environment \"existing\".");

    environment.setName("test");
    assertThat(repository.getEnvironment(baseDir, "test")).contains(environment);
    assertThat(repository.getEnvironment(baseDir, "existing")).contains(existing);
    assertThat(Files.exists(repository.getPath(baseDir, "test"))).isTrue();
    assertThat(Files.exists(repository.getPath(baseDir, "existing"))).isTrue();
  }

  @Test
  void storeEnvironment_rename_adaptChildren_ok() {
    Environment parent = repository.createTransientEnvironment("parent", null);
    repository.storeEnvironment(baseDir, parent, false);
    Environment child1 = repository.createTransientEnvironment("child1", parent);
    repository.storeEnvironment(baseDir, child1, false);
    Environment child2 = repository.createTransientEnvironment("child2", parent);
    repository.storeEnvironment(baseDir, child2, false);

    parent.setName("renamed");
    repository.storeEnvironment(baseDir, parent, false);

    assertThat(repository.getEnvironment(baseDir, "child1").orElseThrow().getParent()).isEqualTo("renamed");
    assertThat(repository.getEnvironment(baseDir, "child2").orElseThrow().getParent()).isEqualTo("renamed");
  }

  @Test
  void storeEnvironment_reparent_ok() {
    Environment root = repository.createTransientEnvironment("root", null);
    repository.storeEnvironment(baseDir, root, false);
    Environment child = repository.createTransientEnvironment("child", root);
    repository.storeEnvironment(baseDir, child, false);
    Environment test = repository.createTransientEnvironment("test", root);
    repository.storeEnvironment(baseDir, test, false);

    test.setParent("child");
    repository.storeEnvironment(baseDir, test, false);

    assertThat(repository.getEnvironment(baseDir, "test").orElseThrow().getParent()).isEqualTo("child");

  }

  @Test
  void storeEnvironment_reparent_fail() {
    Environment test = repository.createTransientEnvironment("test", null);
    repository.storeEnvironment(baseDir, test, false);
    Environment parent = repository.createTransientEnvironment("parent", test);
    repository.storeEnvironment(baseDir, parent, false);
    Environment child = repository.createTransientEnvironment("child", parent);
    repository.storeEnvironment(baseDir, child, false);

    test.setParent("child");
    assertThatThrownBy(() -> repository.storeEnvironment(baseDir, test, true))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Environment \"test\" has an inheritance loop, path is [parent, test, child].");
  }

  @Test
  void getEnvironment_ok() throws IOException {
    sampleConfig("root", null, Map.of("a", "b"), Map.of("c", List.of("d")));
    sampleConfig("parent", "root", Map.of("e", "f"), Map.of("g", List.of("h")));
    EnvironmentConfig config = sampleConfig("config", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));

    assertThat(repository.getEnvironment(baseDir, "config")).contains(new Environment(
        "parent",
        "config",
        repository.getMergedConfig(baseDir, "parent", new HashSet<>()),
        config
    ));
  }

  @Test
  void getEnvironment_empty() {
    assertThat(repository.getEnvironment(baseDir, "_empty")).contains(Environment.empty());
  }

  @Test
  void getEnvironment_notFound() {
    assertThat(repository.getEnvironment(baseDir, "notFound")).isEmpty();
  }

  @Test
  void getEnvironment_corruptLoop() throws IOException {
    sampleConfig("root", "config", Map.of("a", "b"), Map.of("c", List.of("d")));
    sampleConfig("parent", "root", Map.of("e", "f"), Map.of("g", List.of("h")));
    sampleConfig("config", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));
    assertThatThrownBy(() -> repository.getEnvironment(baseDir, "config"))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Environment \"config\" has an inheritance loop, path is [config, parent, root].");
  }

  @Test
  void getEnvironment_badParent() throws IOException {
    sampleConfig("config", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));
    assertThatThrownBy(() -> repository.getEnvironment(baseDir, "config"))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("No such environment \"parent\", path is [config, parent].");
  }

  @Test
  void deleteEnvironment_noForce_ok() throws IOException {
    EnvironmentConfig parent = sampleConfig("parent", null, Map.of("e", "f"), Map.of("g", List.of("h")));
    EnvironmentConfig config = sampleConfig("config", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));

    assertThat(Files.exists(envDir.resolve("config"))).isTrue();

    repository.deleteEnvironment(baseDir, "config", false);
    assertThat(Files.exists(envDir.resolve("config"))).isFalse();
  }

  @Test
  void deleteEnvironment_force_ok() throws IOException {
    EnvironmentConfig parent = sampleConfig("parent", null, Map.of("e", "f"), Map.of("g", List.of("h")));
    EnvironmentConfig config = sampleConfig("config", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));

    assertThat(Files.exists(envDir.resolve("parent"))).isTrue();

    repository.deleteEnvironment(baseDir, "parent", true);
    assertThat(Files.exists(envDir.resolve("parent"))).isFalse();
  }

  @Test
  void deleteEnvironment_noForce_fail() throws IOException {
    EnvironmentConfig parent = sampleConfig("parent", null, Map.of("e", "f"), Map.of("g", List.of("h")));
    EnvironmentConfig config = sampleConfig("config", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));

    assertThat(Files.exists(envDir.resolve("parent"))).isTrue();

    assertThatThrownBy(() -> repository.deleteEnvironment(baseDir, "parent", false))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Environment \"parent\" cannot be deleted, since it is in use.");
    assertThat(Files.exists(envDir.resolve("parent"))).isTrue();
  }

  @Test
  void deleteConfig_ok() throws IOException {
    sampleConfig("config", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));
    assertThat(Files.exists(envDir.resolve("config"))).isTrue();

    repository.deleteConfig(baseDir, "config");
    assertThat(Files.exists(envDir.resolve("config"))).isFalse();
  }

  @Test
  void deleteConfig_notFound() {
    assertThatThrownBy(() -> repository.deleteConfig(baseDir, "notFound"))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Failed to delete environment \"notFound\".");
  }

  @Test
  void streamConfigs() throws IOException {
    EnvironmentConfig root = sampleConfig("root", null, Map.of("a", "b"), Map.of("c", List.of("d")));
    EnvironmentConfig parent = sampleConfig("parent", "root", Map.of("e", "f"), Map.of("g", List.of("h")));
    EnvironmentConfig config = sampleConfig("config", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));

    assertThat(repository.streamConfigs(baseDir)).containsExactlyInAnyOrder(
        new Pair<>("root", root),
        new Pair<>("parent", parent),
        new Pair<>("config", config)
    );
  }

  @Test
  void storeConfig() {
    EnvironmentConfig config = new EnvironmentConfig("parent", Map.of("i", "j"), Map.of("k", List.of("l")), 4711L);
    assertThat(repository.getConfig(baseDir, "config")).isEmpty();

    repository.storeConfig(baseDir, "config", config);
    assertThat(repository.getConfig(baseDir, "config")).contains(config);
  }

  @Test
  void getMergedConfig_ok() throws IOException {
    sampleConfig("root", null, Map.of("a", "b"), Map.of("c", List.of("d")));
    sampleConfig("parent", "root", Map.of("e", "f"), Map.of("g", List.of("h")));
    EnvironmentConfig child = sampleConfig("config", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));

    assertThat(repository.getMergedConfig(baseDir, "config", new LinkedHashSet<>())).isEqualTo(
        new EnvironmentConfig(
            "parent",
            Map.of("a", "b", "e", "f", "i", "j"),
            Map.of("c", List.of("d"), "g", List.of("h"), "k", List.of("l")),
            child.requestTimeout()
        )
    );
  }

  @Test
  void getMergedConfig_recursive() throws IOException {
    sampleConfig("root", "config", Map.of("a", "b"), Map.of("c", List.of("d")));
    sampleConfig("parent", "root", Map.of("e", "f"), Map.of("g", List.of("h")));
    sampleConfig("config", "parent", Map.of("i", "j"), Map.of("k", List.of("l")));

    assertThatThrownBy(() -> repository.getMergedConfig(baseDir, "config", new LinkedHashSet<>()))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Environment \"config\" has an inheritance loop, path is [config, parent, root].");
  }

  @Test
  void getMergedConfig_notFound() throws IOException {
    assertThatThrownBy(() -> repository.getMergedConfig(baseDir, "config", new LinkedHashSet<>()))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("No such environment \"config\".");
  }

  @Test
  void getConfig_ok() throws IOException {
    EnvironmentConfig config = sampleConfig("foo", "bar", Map.of("a", "b"), Map.of("c", List.of("d")));
    assertThat(repository.getConfig(baseDir, "foo")).contains(config);
  }

  @Test
  void getConfig_notFound() {
    assertThat(repository.getConfig(baseDir, "notFound")).isEmpty();
  }

  @Test
  void getPath() {
    assertThat(repository.getPath(baseDir, "someName")).isEqualTo(envDir.resolve("someName"));
  }

  EnvironmentConfig sampleConfig(String name, String parent, Map<String, Object> variables, Map<String, List<String>> headers)
      throws IOException {
    EnvironmentConfig config = new EnvironmentConfig(parent, variables, headers, new Random().nextLong());
    if (!Files.exists(envDir)) {
      Files.createDirectories(envDir);
    }
    objectMapper.writeValue(envDir.resolve(name).toFile(), config);
    return config;
  }

}
