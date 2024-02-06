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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class CommandLine extends Values {

  private final Map<Option, List<Subset>> subsets = new HashMap<>();

  public boolean hasSubset(Option option) {
    return subsets.containsKey(option);
  }

  public Subset addSubset(Option option) {
    List<Subset> list = this.subsets.computeIfAbsent(option, ignore -> new ArrayList<>());
    Subset subset = new Subset();
    list.add(subset);
    return subset;
  }

  public Optional<Subset> getSubset(Option option) {
    return getSubsets(option).stream().findFirst();
  }

  public List<Subset> getSubsets(Option option) {
    return this.subsets.getOrDefault(option, List.of());
  }

  public class Subset extends Values {

    @Override
    public void addOption(Option option) {
      super.addOption(option);
      CommandLine.this.addOption(option);
    }

    @Override
    public void addValues(Positional key, List<String> values) {
      super.addValues(key, values);
      CommandLine.this.addValues(key, values);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    CommandLine that = (CommandLine) o;
    return Objects.equals(subsets, that.subsets);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), subsets);
  }
}
