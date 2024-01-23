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

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;
import java.util.Objects;

@RegisterForReflection
public class CommandConfigTree {

  private CommandConfig config;
  private Map<String, CommandConfigTree> subCommands;

  public CommandConfig getConfig() {
    return config;
  }

  public CommandConfigTree setConfig(CommandConfig config) {
    this.config = config;
    return this;
  }

  public Map<String, CommandConfigTree> getSubCommands() {
    return subCommands;
  }

  public CommandConfigTree setSubCommands(Map<String, CommandConfigTree> subCommands) {
    this.subCommands = subCommands;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CommandConfigTree that = (CommandConfigTree) o;
    return Objects.equals(config, that.config) && Objects.equals(subCommands, that.subCommands);
  }

  @Override
  public int hashCode() {
    return Objects.hash(config, subCommands);
  }

  @Override
  public String toString() {
    return "CommandConfigTree{" +
        "config=" + config +
        ", subCommands=" + subCommands +
        '}';
  }
}
