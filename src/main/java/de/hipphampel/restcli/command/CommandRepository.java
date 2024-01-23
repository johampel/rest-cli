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

import de.hipphampel.restcli.command.builtin.BuiltinCommand;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.CommandConfigRepository;
import de.hipphampel.restcli.command.custom.CustomCommandFactory;
import de.hipphampel.restcli.utils.Pair;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ApplicationScoped
public class CommandRepository {

  @Inject
  CommandConfigRepository configRepository;
  @Inject
  CustomCommandFactory customCommandFactory;

  private final Map<CommandAddress, BuiltinCommand> builtins = new HashMap<>();

  // Testing only
  Map<CommandAddress, BuiltinCommand> getBuiltins() {
    return builtins;
  }

  public Optional<Command> getCommand(Path rootDir, CommandAddress address) {
    return getRegistryEntry(rootDir, address)
        .map(entry -> entry.createCommand(this));
  }

  public Optional<CommandInfo> getCommandInfo(Path rootDir, CommandAddress address) {
    return getRegistryEntry(rootDir, address)
        .map(entry -> addChildren(rootDir, address, entry))
        .map(entry -> toCommandInfo(address, entry));
  }

  public Pair<CommandAddress, Optional<String>> getLongestExistingCommandAddress(Path rootDir, CommandAddress address) {
    String child = null;
    while (!address.isRoot() && getCommandInfo(rootDir, address).isEmpty()) {
      child = address.name();
      address = address.parent();
    }
    return new Pair<>(address, Optional.ofNullable(child));
  }

  private CommandInfo toCommandInfo(CommandAddress address, RegistryEntry registryEntry) {
    return new CommandInfo(
        address,
        registryEntry.allowsChildren(),
        registryEntry instanceof BuiltinRegistryEntry,
        registryEntry.getSynopsis(),
        new ArrayList<>(registryEntry.getChildren())
    );
  }

  Optional<RegistryEntry> getRegistryEntry(Path rootDir, CommandAddress address) {
    fillBuiltinMapIfRequired();
    return getBuiltinRegistryEntry(address)
        .or(() -> getCustomRegistryEntry(rootDir, address));
  }

  Optional<RegistryEntry> getBuiltinRegistryEntry(CommandAddress address) {
    return Optional.ofNullable(builtins.get(address))
        .map(BuiltinRegistryEntry::new);
  }

  Optional<RegistryEntry> getCustomRegistryEntry(Path rootDir, CommandAddress address) {
    return configRepository.getValidatedPath(rootDir, address)
        .map(path -> configRepository.load(rootDir, address))
        .map(config -> new CustomRegistryEntry(address, config));
  }

  RegistryEntry addChildren(Path rootDir, CommandAddress address, RegistryEntry entry) {
    if (rootDir != null) {
      entry.getChildren().addAll(configRepository.getChildren(rootDir, address));
    }
    builtins.keySet().stream()
        .filter(child -> Objects.equals(child.parent(), address))
        .forEach(child -> entry.getChildren().add(child));
    entry.children.sort(Comparator.comparing(CommandAddress::toString));
    return entry;
  }

  @SuppressWarnings("unchecked")
  void fillBuiltinMapIfRequired() {
    if (!builtins.isEmpty()) {
      return;
    }

    BeanManager beanManager = CDI.current().getBeanManager();
    beanManager.getBeans(BuiltinCommand.class).stream()
        .map(bean -> (Bean<BuiltinCommand>) bean)
        .map(bean -> beanManager.getReference(bean, Command.class, beanManager.createCreationalContext(bean)))
        .map(BuiltinCommand.class::cast)
        .forEach(bean -> builtins.put(bean.address(), bean));
  }

  CustomCommandFactory getCustomCommandFactory() {
    return customCommandFactory;
  }

  abstract static class RegistryEntry {

    private final List<CommandAddress> children = new ArrayList<>();

    abstract Command createCommand(CommandRepository repository);

    abstract String getSynopsis();

    List<CommandAddress> getChildren() {
      return children;
    }

    abstract boolean allowsChildren();
  }

  static class BuiltinRegistryEntry extends RegistryEntry {

    private final BuiltinCommand command;

    BuiltinRegistryEntry(BuiltinCommand command) {
      this.command = command;
    }

    @Override
    public Command createCommand(CommandRepository repository) {
      return command;
    }

    @Override
    public String getSynopsis() {
      return command.synopsis();
    }

    @Override
    public boolean allowsChildren() {
      return command instanceof ParentCommand;
    }
  }

  static class CustomRegistryEntry extends RegistryEntry {

    private final CommandAddress address;
    private final CommandConfig config;

    CustomRegistryEntry(CommandAddress address, CommandConfig config) {
      this.address = address;
      this.config = config;
    }

    @Override
    Command createCommand(CommandRepository repository) {
      return repository.getCustomCommandFactory().createCommand(address, config);
    }

    @Override
    String getSynopsis() {
      return config.getSynopsis();
    }

    @Override
    boolean allowsChildren() {
      return config.getType()== Type.Parent;
    }
  }
}
