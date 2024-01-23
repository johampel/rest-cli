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

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

@RegisterForReflection
public class TemplateModel extends AbstractMap<String, Object> {

  private final Map<String, Object> values;
  private final Object api;
  private final Function<String, Object> undefinedVariableCallback;
  private final Map<String, Object> computedValues;

  public TemplateModel(Map<String, Object> values, Object api, Function<String, Object> undefinedVariableCallback) {
    this.values = Objects.requireNonNull(values);
    this.api = api;
    this.computedValues = new HashMap<>();
    this.undefinedVariableCallback = Objects.requireNonNull(undefinedVariableCallback);
  }

  public TemplateModel(Map<String, Object> values, Object api) {
    this(values, api, key -> null);
  }

  public TemplateModel(Map<String, Object> values) {
    this(values, null, key -> null);
  }

  public Object get_() {
    return api;
  }

  @Override
  public Object get(Object key) {
    Object value = values.get(key);
    if (value == null) {
      value = computedValues.computeIfAbsent(String.valueOf(key), var -> undefinedVariableCallback.apply(String.valueOf(var)));
    }
    return value;
  }

  @NotNull
  @Override
  public Set<Entry<String, Object>> entrySet() {
    return values.entrySet();
  }
}
