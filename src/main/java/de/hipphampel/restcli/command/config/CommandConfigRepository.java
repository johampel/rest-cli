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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@ApplicationScoped
public class CommandConfigRepository {

  private static final String CONFIG_NAME = "command.json";
  private static final String COMMAND_DIR = "commands";

  @Inject
  ObjectMapper objectMapper;

  public Optional<Path> getPath(Path rootDir, CommandAddress address) {
    return Optional.ofNullable(rootDir)
        .flatMap(dir -> Optional.ofNullable(address).map(addr -> getDescriptorDir(dir, address)))
        .map(path -> path.resolve(CONFIG_NAME));
  }

  public Optional<Path> getValidatedPath(Path rootDir, CommandAddress address) {
    try {
      Optional<Path> path = getPath(rootDir, address);
      path.ifPresent(ignore -> load(rootDir, address));
      return path;
    } catch (ExecutionException ee) {
      return Optional.empty();
    }
  }

  public List<CommandAddress> getChildren(Path rootDir, CommandAddress address) {
    if (rootDir == null || address == null) {
      return List.of();
    }

    Path dir = getDescriptorDir(rootDir, address);
    if (!Files.isReadable(dir) || !Files.isDirectory(dir)) {
      return List.of();
    }

    try (Stream<Path> children = Files.list(dir)) {
      return children
          .filter(Files::isDirectory)
          .map(Path::getFileName)
          .map(Path::toString)
          .filter(CommandAddress::isValidCommandName)
          .map(address::child)
          .filter(child -> getPath(rootDir, child)
              .filter(path -> Files.isRegularFile(path) && Files.isReadable(path))
              .isPresent())
          .toList();
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to read directory \"%s\".".formatted(dir), ioe);
    }
  }

  public CommandConfig load(Path rootDir, CommandAddress address) {
    Path path = getDescriptorDir(rootDir, address).resolve(CONFIG_NAME);
    try {
      return objectMapper.readValue(path.toFile(), CommandConfig.class);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to load configuration for command \"%s\".".formatted(address), ioe);
    }
  }

  public void store(Path rootDir, CommandAddress address, CommandConfig config) {
    Path path = getDescriptorDir(rootDir, address).resolve(CONFIG_NAME);
    FileUtils.createDirectoryIfNotExists(path.getParent());
    try {
      objectMapper.writeValue(path.toFile(), config);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to store the configuration for command \"%s\".".formatted(address), ioe);
    }
  }

  public void store(Path rootDir, CommandAddress address, CommandConfigTree configTree) {
    if (address.isRoot()) {
      throw new ExecutionException("Cannot store at root address.");
    }

    Path configDir = getDescriptorDir(rootDir, address);
    FileUtils.deleteRecursively(configDir, true);

    store(rootDir, address, configTree.getConfig());
    if (configTree.getSubCommands() != null) {
      for (Map.Entry<String, CommandConfigTree> entry : configTree.getSubCommands().entrySet()) {
        store(rootDir, address.child(entry.getKey()), entry.getValue());
      }
    }
  }

  public void delete(Path rootDir, CommandAddress address, boolean recursive) {
    if (!getChildren(rootDir, address).isEmpty() && !recursive) {
      throw new ExecutionException("Command \"%s\" cannot be deleted since it has at least one sub-command.".formatted(address));
    }

    Path configDir = getDescriptorDir(rootDir, address);

    if (address.isRoot()) {
      FileUtils.deleteRecursively(configDir.resolve(CONFIG_NAME), false);
    } else {
      FileUtils.deleteRecursively(configDir, true);
    }
  }

  public void move(Path rootDir, CommandAddress address, CommandAddress newAddress) {
    if (Objects.equals(address, newAddress)) {
      return;
    }
    Path newPath = getDescriptorDir(rootDir, newAddress);
    if (Files.exists(newPath)) {
      throw new ExecutionException("Command \"%s\" already exists.".formatted(newAddress));
    }
    Path path = getDescriptorDir(rootDir, address);
    if (!Files.exists(path)) {
      throw new ExecutionException("Command \"%s\" not exists.".formatted(address));
    }
    try {
      Files.move(path, newPath);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to move command \"%s\" to \"%s\".".formatted(address, newAddress), ioe);
    }
  }

  public Path getDescriptorDir(Path rootPath, CommandAddress address) {
    return rootPath.resolve(COMMAND_DIR).resolve(address.toPath());
  }
}