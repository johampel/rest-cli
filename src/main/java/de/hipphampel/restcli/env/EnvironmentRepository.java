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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.CollectionUtils;
import de.hipphampel.restcli.utils.FileUtils;
import de.hipphampel.restcli.utils.Pair;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@ApplicationScoped
public class EnvironmentRepository {

  static final String ENVIRONMENT_DIR = "environments";

  @Inject
  ObjectMapper objectMapper;

  public boolean existsEnvironment(Path rootDir, String environmentName) {
    Path path = getPath(rootDir, environmentName);
    return Environment.EMPTY.equals(environmentName) || (Files.isRegularFile(path) && Files.isReadable(path));
  }

  public Optional<Environment> getEnvironment(Path rootDir, String environmentName) {
    if (Objects.equals(Environment.EMPTY, environmentName)) {
      return Optional.of(Environment.empty());
    }
    return getConfig(rootDir, environmentName)
        .map(config -> new Environment(
            config.parent(),
            environmentName,
            getMergedConfig(rootDir, config.parent(), new LinkedHashSet<>(List.of(environmentName))),
            config));
  }

  public List<String> listEnvironments(Path rootDir) {
    try (Stream<Path> listing = Files.list(getPath(rootDir, "."))) {
      return listing
          .filter(path -> Files.isRegularFile(path) && Files.isReadable(path))
          .map(path -> path.getFileName().toString())
          .toList();
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to list environments.", ioe);
    }
  }

  public List<String> listEnvironments(Path rootDir, String parent) {
    return streamConfigs(rootDir)
        .filter(p -> Objects.equals(p.second().parent(), parent))
        .map(Pair::first)
        .toList();
  }

  public Environment createTransientEnvironment(String name, Environment parent) {
    return new Environment(
        parent == null ? null : parent.getName(),
        Objects.requireNonNull(name),
        parent == null ? EnvironmentConfig.EMPTY : new EnvironmentConfig(
            parent.getParent(),
            parent.getVariables(),
            parent.getHeaders(),
            null),
        parent == null ? EnvironmentConfig.EMPTY : new EnvironmentConfig(parent.getName(), Map.of(), Map.of(), null));
  }

  public void storeEnvironment(Path rootDir, Environment environment, boolean force) {
    if (environment.isNamedChanged()) {
      if (!force && existsEnvironment(rootDir, environment.getName())) {
        throw new ExecutionException("Cannot overwrite the already existing environment \"%s\".".formatted(environment.getName()));
      }
    }

    getMergedConfig(rootDir, environment.getParent(),
        new HashSet<>(List.of(environment.getName()))); // implicitly validates the inheritance path

    storeConfig(rootDir, environment.getName(), environment.getLocalConfig());

    if (environment.isNamedChanged()) {
      deleteConfig(rootDir, environment.getOriginalName());
      String oldName = environment.getOriginalName();
      streamConfigs(rootDir)
          .filter(entry -> Objects.equals(entry.second().parent(), oldName))
          .forEach(entry -> storeConfig(
              rootDir,
              entry.first(),
              new EnvironmentConfig(
                  environment.getName(),
                  entry.second().variables(),
                  entry.second().headers(),
                  entry.second().requestTimeout())));
    }
  }

  public void deleteEnvironment(Path rootDir, String environmentName, boolean force) {
    if (!force && streamConfigs(rootDir).anyMatch(config -> Objects.equals(config.second().parent(), environmentName))) {
      throw new ExecutionException("Environment \"%s\" cannot be deleted, since it is in use.".formatted(environmentName));
    }
    deleteConfig(rootDir, environmentName);
  }

  void deleteConfig(Path rootDir, String environmentName) {
    Path path = getPath(rootDir, environmentName);
    try {
      Files.delete(path);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to delete environment \"%s\".".formatted(environmentName), ioe);
    }
  }

  public Stream<Pair<String, EnvironmentConfig>> streamConfigs(Path rootDir) {
    return listEnvironments(rootDir).stream()
        .flatMap(name -> getConfig(rootDir, name).stream()
            .map(config -> new Pair<>(name, config)));
  }

  void storeConfig(Path rootDir, String environmentName, EnvironmentConfig config) {
    Path path = getPath(rootDir, environmentName);
    try {
      Files.write(path, objectMapper.writeValueAsBytes(config));
    } catch (IOException e) {
      throw new ExecutionException("Failed to store environment \"%s\".".formatted(environmentName));
    }
  }

  EnvironmentConfig getMergedConfig(Path rootDir, String environmentName, Set<String> recursionGuard) {
    if (environmentName == null) {
      return EnvironmentConfig.EMPTY;
    }
    if (!recursionGuard.add(environmentName)) {
      throw new ExecutionException("Environment \"%s\" has an inheritance loop, path is %s.".formatted(environmentName, recursionGuard));
    }

    return getConfig(rootDir, environmentName)
        .map(config -> {
          if (config.parent() != null) {
            EnvironmentConfig parentConfig = getMergedConfig(rootDir, config.parent(), recursionGuard);
            return new EnvironmentConfig(
                config.parent(),
                CollectionUtils.mergeVariables(parentConfig.variables(), config.variables()),
                CollectionUtils.mergeHeaders(parentConfig.headers(), config.headers()),
                config.requestTimeout() != null ? config.requestTimeout() : parentConfig.requestTimeout());
          } else {
            return config;
          }
        })
        .orElseThrow(() -> recursionGuard.size() == 1 ? new ExecutionException("No such environment \"%s\".".formatted(environmentName))
            : new ExecutionException("No such environment \"%s\", path is %s.".formatted(environmentName, recursionGuard)));
  }

  public Optional<EnvironmentConfig> getConfig(Path rootDir, String environmentName) {
    Path path = getPath(rootDir, environmentName);
    try {
      return Optional.of(objectMapper.readValue(Files.readAllBytes(path), EnvironmentConfig.class));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  Path getPath(Path rootDir, String environmentName) {
    Path envDir = rootDir.resolve(ENVIRONMENT_DIR);
    FileUtils.createDirectoryIfNotExists(envDir);
    return envDir.resolve(environmentName);
  }
}
