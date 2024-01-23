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
package de.hipphampel.restcli.cli.commandline;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Option;
import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class Values {

  private final Set<Option> options = new HashSet<>();
  private final Map<Positional, List<String>> values = new HashMap<>();

  public void addOption(Option option) {
    this.options.add(option);
  }

  public boolean hasOption(Option option) {
    return this.options.contains(option);
  }

  public final void addValue(Positional key, String value) {
    addValues(key, List.of(value));
  }

  public void addValues(Positional key, List<String> values) {
    List<String> list = this.values.computeIfAbsent(key, ignore -> new ArrayList<>());
    list.addAll(values);
  }

  public void setValues(Positional key, List<String> values) {
    this.values.put(key, new ArrayList<>(values));
  }

  public boolean hasValues(Positional key) {
    return this.values.containsKey(key);
  }

  public Optional<String> getValue(Positional key) {
    return getValues(key).stream().findFirst();
  }

  public List<String> getValues(Positional key) {
    return this.values.getOrDefault(key, List.of());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Values values1 = (Values) o;
    return Objects.equals(options, values1.options) && Objects.equals(values, values1.values);
  }

  @Override
  public int hashCode() {
    return Objects.hash(options, values);
  }

  @Override
  public String toString() {
    return "Values{" +
        "options=" + options +
        ", values=" + values +
        '}';
  }
}
