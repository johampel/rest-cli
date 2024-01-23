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

import de.hipphampel.restcli.command.HelpSection;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RegisterForReflection
public class CommandConfig {

  public enum Type {
    Parent,
    Alias,
    Http
  }

  private Type type;
  private String synopsis;
  private Map<HelpSection, String> descriptions = new HashMap<>();
  private RestCommandConfig restConfig;
  private List<String> aliasConfig;

  public Type getType() {
    return type;
  }

  public CommandConfig setType(Type type) {
    this.type = Objects.requireNonNull(type);
    return this;
  }

  public String getSynopsis() {
    return synopsis;
  }

  public CommandConfig setSynopsis(String synopsis) {
    this.synopsis = synopsis;
    return this;
  }

  public Map<HelpSection, String> getDescriptions() {
    return descriptions;
  }

  public CommandConfig setDescriptions(Map<HelpSection, String> descriptions) {
    this.descriptions = Objects.requireNonNull(descriptions);
    return this;
  }

  public RestCommandConfig getRestConfig() {
    return restConfig;
  }

  public CommandConfig setRestConfig(RestCommandConfig restConfig) {
    this.restConfig = restConfig;
    return this;
  }

  public List<String> getAliasConfig() {
    return aliasConfig;
  }

  public CommandConfig setAliasConfig(List<String> aliasConfig) {
    this.aliasConfig = aliasConfig;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CommandConfig that = (CommandConfig) o;
    return type == that.type && Objects.equals(synopsis, that.synopsis) && Objects.equals(descriptions, that.descriptions)
        && Objects.equals(restConfig, that.restConfig) && Objects.equals(aliasConfig, that.aliasConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, synopsis, descriptions, restConfig, aliasConfig);
  }

  @Override
  public String toString() {
    return "CommandConfig{" +
        "type=" + type +
        ", synopsis='" + synopsis + '\'' +
        ", descriptions=" + descriptions +
        ", restConfig=" + restConfig +
        ", aliasConfig=" + aliasConfig +
        '}';
  }
}
