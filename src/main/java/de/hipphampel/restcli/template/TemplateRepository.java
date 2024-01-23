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
package de.hipphampel.restcli.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.utils.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationScoped
public class TemplateRepository {

  private static final String TEMPLATE_DIR = "templates";
  private static final String SUFFIX = ".json";
  private static final Set<TemplateAddress> BUILTINS = Set.of(
      TemplateAddress.fromString("default@"));

  @Inject
  ObjectMapper objectMapper;


  public void storeTemplate(Path rootDir, Template template, boolean replaceAllowed) {
    Path path = getTemplatePath(rootDir, template.address());
    FileUtils.createDirectoryIfNotExists(path.getParent());
    if (existsTemplate(rootDir, template.address()) && !replaceAllowed) {
      throw new ExecutionException("Template \"%s\" for command \"%s\" already exists.".formatted(template.name(), template.command()));
    }
    try {
      objectMapper.writeValue(path.toFile(), template.config());
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to store template \"%s\" for command \"%s\".".formatted(template.name(), template.command()),
          ioe);
    }
  }

  public boolean isBuiltin(Path rootDir, TemplateAddress address) {
    return BUILTINS.contains(address) && !existsTemplatePath(getTemplatePath(rootDir, address));
  }

  public Optional<Template> getTemplate(Path rootDir, TemplateAddress address) {
    Path path = getTemplatePath(rootDir, address);
    if (!existsTemplatePath(path)) {
      if (isBuiltin(address)) {
        return Optional.of(loadBuiltin(address));
      } else {
        return Optional.empty();
      }
    }
    try {
      return Optional.of(new Template(
          address,
          objectMapper.readValue(path.toFile(), TemplateConfig.class),
          false));
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to read template \"%s\" for command \"%s\".".formatted(address.name(), address.command()),
          ioe);
    }
  }

  public boolean existsTemplate(Path rootDir, TemplateAddress address) {
    Path path = getTemplatePath(rootDir, address);
    return existsTemplatePath(path) || isBuiltin(address);
  }

  public List<TemplateAddress> getAllTemplates(Path rootDir) {
    if (!Files.exists(rootDir.resolve(TEMPLATE_DIR))) {
      return List.of();
    }
    return collectTemplates(rootDir.resolve(TEMPLATE_DIR), CommandAddress.ROOT)
        .sorted(Comparator.comparing(TemplateAddress::toString))
        .toList();
  }

  Stream<TemplateAddress> collectTemplates(Path directory, CommandAddress commandAddress) {
    try (Stream<Path> child = Files.list(directory)) {
      List<Path> children = child.toList();
      return children.stream()
          .flatMap(path ->
              existsTemplatePath(path) ? Stream.of(new TemplateAddress(commandAddress, extractTemplateName(path).orElseThrow())) :
                  collectTemplates(path, commandAddress.child(path.getFileName().toString())));
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to read all output templates: %s.".formatted(ioe.getMessage()), ioe);
    }
  }

  public List<TemplateAddress> getTemplatesForCommand(Path rootDir, CommandAddress commandAddress) {
    Path dir = getTemplateDir(rootDir, commandAddress);
    if (!Files.isDirectory(dir)) {
      return BUILTINS.stream()
          .filter(address -> Objects.equals(address.command(), commandAddress))
          .map(TemplateAddress::name)
          .sorted()
          .map(name -> new TemplateAddress(commandAddress, name))
          .toList();
    }

    try (Stream<Path> entries = Files.list(dir)) {
      Stream<String> builtins = BUILTINS.stream()
          .filter(address -> Objects.equals(address.command(), commandAddress))
          .map(TemplateAddress::name);
      Stream<String> custom = entries
          .filter(TemplateRepository::existsTemplatePath)
          .map(TemplateRepository::extractTemplateName)
          .filter(Optional::isPresent)
          .map(Optional::get);
      return Stream.concat(builtins, custom)
          .distinct()
          .sorted()
          .map(name -> new TemplateAddress(commandAddress, name))
          .toList();
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to get template for command \"%s\".".formatted(commandAddress), ioe);
    }
  }

  public List<TemplateAddress> getEffectiveTemplates(Path rootDir, CommandAddress commandAddress) {
    if (!Files.isDirectory(getTemplateDir(rootDir, commandAddress)) && !commandAddress.equals(CommandAddress.ROOT)) {
      return List.of();
    }
    return getCommandAddressStream(commandAddress)
        .flatMap(address -> getTemplatesForCommand(rootDir, address).stream())
        .collect(Collectors.groupingBy(TemplateAddress::name))
        .entrySet().stream()
        .sorted(Entry.comparingByKey())
        .map(e -> e.getValue().stream().max(Comparator.comparing(address -> address.toString().length())))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  public boolean deleteTemplate(Path rootDir, TemplateAddress address) {
    Path path = getTemplatePath(rootDir, address);
    if (!Files.exists(path)) {
      return false;
    }
    try {
      Files.deleteIfExists(path);
      return true;
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to delete template \"%s\" for command \"%s\".".formatted(address.name(), address.command()),
          ioe);
    }
  }

  public void deleteTemplatesForCommand(Path rootDir, CommandAddress commandAddress, boolean recursive) {
    if (recursive) {
      FileUtils.deleteRecursively(getTemplateDir(rootDir, commandAddress), !CommandAddress.ROOT.equals(commandAddress));
    } else {
      getTemplatesForCommand(rootDir, commandAddress).forEach(address -> deleteTemplate(rootDir, address));
    }
  }

  public Optional<TemplateAddress> getEffectiveAddress(Path rootDir, CommandAddress commandAddress, String nameOrAddress) {
    if (TemplateAddress.isValueTemplateAddress(nameOrAddress)) {
      return getEffectiveAddress(rootDir, TemplateAddress.fromString(nameOrAddress));
    } else {
      return getEffectiveAddress(rootDir, new TemplateAddress(commandAddress, nameOrAddress));
    }
  }

  public Optional<TemplateAddress> getEffectiveAddress(Path rootDir, TemplateAddress address) {
    return getCommandAddressStream(address.command())
        .map(commandAddress -> new TemplateAddress(commandAddress, address.name()))
        .filter(hit -> existsTemplate(rootDir, hit))
        .findFirst();
  }

  Template loadBuiltin(TemplateAddress address) {
    try (InputStream in = TemplateRepository.class.getResourceAsStream("/templates/%s.json".formatted(address.name()))) {
      return new Template(
          address,
          objectMapper.readValue(in, TemplateConfig.class),
          true);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to read builtin template \"%s\".".formatted(address));
    }
  }

  static Path getTemplatePath(Path rootDir, TemplateAddress address) {
    return getTemplateDir(rootDir, address.command()).resolve(address.name() + SUFFIX);
  }

  static boolean existsTemplatePath(Path path) {
    return Files.isRegularFile(path) && extractTemplateName(path).isPresent();
  }

  static Optional<String> extractTemplateName(Path path) {
    String name = path.getFileName().toString();
    if (!name.endsWith(SUFFIX)) {
      return Optional.empty();
    }
    return Optional.of(name.substring(0, name.length() - SUFFIX.length()));
  }

  static Path getTemplateDir(Path rootDir, CommandAddress address) {
    return rootDir.resolve(TEMPLATE_DIR).resolve(address.toPath());
  }

  static Stream<CommandAddress> getCommandAddressStream(CommandAddress address) {
    if (address.isRoot()) {
      return Stream.of(address);
    } else {
      return Stream.concat(Stream.of(address), getCommandAddressStream(address.parent()));
    }
  }

  static boolean isBuiltin(TemplateAddress address) {
    return BUILTINS.contains(address);
  }
}
