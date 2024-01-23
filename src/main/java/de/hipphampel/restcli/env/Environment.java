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

import de.hipphampel.restcli.utils.ChangeDetector;
import de.hipphampel.restcli.utils.CollectionUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Environment {

  public static final String EMPTY = "_empty";
  private final ChangeDetector<String> parent;
  private final ChangeDetector<String> name;
  private final EnvironmentConfig parentConfig;
  private EnvironmentConfig localConfig;
  private Map<String, Object> variables;
  private Map<String, List<String>> headers;

  public static Environment empty() {
    return new Environment(null, Environment.EMPTY, EnvironmentConfig.EMPTY, EnvironmentConfig.EMPTY);
  }

  public Environment(String parent, String name, EnvironmentConfig parentConfig, EnvironmentConfig localConfig) {
    this.parent = new ChangeDetector<>(parent);
    this.name = new ChangeDetector<>(name);
    this.parentConfig = Objects.requireNonNull(parentConfig);
    this.localConfig = Objects.requireNonNull(localConfig);
    recalculate();
  }

  public Environment(String parent, String name, EnvironmentConfig parentConfig) {
    this(parent, name, parentConfig, EnvironmentConfig.EMPTY);
  }

  public String getName() {
    return this.name.get();
  }

  public String getOriginalName() {
    return this.name.getOriginal();
  }

  public void setName(String name) {
    this.name.set(name);
  }

  boolean isNamedChanged() {
    return this.name.isChanged();
  }

  public String getParent() {
    return this.parent.get();
  }

  public void setParent(String parent) {
    this.localConfig = new EnvironmentConfig(
        parent,
        localConfig.variables(),
        localConfig.headers(),
        localConfig.requestTimeout());
    this.parent.set(parent);
  }

  boolean isParentChanged() {
    return this.parent.isChanged();
  }

  public EnvironmentConfig getLocalConfig() {
    return localConfig;
  }

  public EnvironmentConfig getParentConfig() {
    return new EnvironmentConfig(
        null,
        parentConfig.variables(),
        parentConfig.headers(),
        parentConfig.requestTimeout());
  }

  public Map<String, Object> getVariables() {
    return Collections.unmodifiableMap(this.variables);
  }

  public Object getLocalVariable(String name) {
    return localConfig.variables().get(name);
  }

  public Map<String, Object> getLocalVariables() {
    return Collections.unmodifiableMap(this.localConfig.variables());
  }

  public Environment setLocalVariables(Map<String, Object> localVariables) {
    this.localConfig = new EnvironmentConfig(
        parent.get(),
        localVariables,
        localConfig.headers(),
        localConfig.requestTimeout());
    return recalculate();
  }

  public Environment setLocalVariable(String key, Object value) {
    Map<String, Object> localVariables = new HashMap<>(getLocalVariables());
    localVariables.put(key, value);
    return setLocalVariables(localVariables);
  }

  public List<String> getHeader(String name) {
    return headers.get(name);
  }

  public Map<String, List<String>> getHeaders() {
    return Collections.unmodifiableMap(this.headers);
  }

  public List<String> getLocalHeader(String name) {
    return localConfig.headers().get(name);
  }

  public Map<String, List<String>> getLocalHeaders() {
    return Collections.unmodifiableMap(this.localConfig.headers());
  }

  public Environment setLocalHeader(String name, List<String> value) {
    Map<String, List<String>> localHeaders = new HashMap<>(getLocalHeaders());
    localHeaders.put(name, value);
    return setLocalHeaders(localHeaders);
  }

  public Environment setLocalHeaders(Map<String, List<String>> localHeaders) {
    this.localConfig = new EnvironmentConfig(
        parent.get(),
        localConfig.variables(),
        localHeaders,
        localConfig.requestTimeout());
    return recalculate();
  }

  public Long getRequestTimeout() {
    return localConfig.requestTimeout() == null && parentConfig != null ? parentConfig.requestTimeout() : localConfig.requestTimeout();
  }

  public Environment setRequestTimeout(Long requestTimeout) {
    this.localConfig = new EnvironmentConfig(
        parent.get(),
        localConfig.variables(),
        localConfig.headers(),
        requestTimeout);
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
    Environment that = (Environment) o;
    return Objects.equals(parent, that.parent) && Objects.equals(name, that.name) && Objects.equals(parentConfig, that.parentConfig)
        && Objects.equals(localConfig, that.localConfig);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parent, name, parentConfig, localConfig);
  }

  @Override
  public String toString() {
    return "Environment{" +
        "parent=" + parent +
        ", name=" + name +
        ", parentConfig=" + parentConfig +
        ", localConfig=" + localConfig +
        '}';
  }

  Environment recalculate() {
    this.variables = CollectionUtils.mergeVariables(parentConfig.variables(), localConfig.variables());
    this.headers = CollectionUtils.mergeHeaders(parentConfig.headers(), localConfig.headers());
    return this;
  }


}
