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
package de.hipphampel.restcli.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RegisterForReflection
public record Range<K extends Comparable<K>>(K lower, K upper) {

  @JsonCreator
  public Range(@JsonProperty("lower") K lower, @JsonProperty("upper") K upper) {
    if (Objects.requireNonNull(lower).compareTo(upper) > 0) {
      throw new IllegalArgumentException("%s must not be greater than %s".formatted(lower, upper));
    }
    this.lower = lower;
    this.upper = upper;
  }

  public boolean contains(K value) {
    return value != null && lower.compareTo(value) <= 0 && value.compareTo(upper) < 0;
  }

  public boolean isEmpty() {
    return lower.compareTo(upper) >= 0;
  }

  public Optional<Range<K>> intersect(Range<K> other) {
    K min = lower.compareTo(other.lower) < 0 ? other.lower : lower;
    K max = upper.compareTo(other.upper) < 0 ? upper : other.upper;

    return min.compareTo(max) < 0 ? Optional.of(new Range<>(min, max)) : Optional.empty();
  }

  public List<Range<K>> exclude(Range<K> other) {
    if (lower.compareTo(other.upper) >= 0 || upper.compareTo(other.lower) <= 0) {
      return List.of(this);
    }

    List<Range<K>> result = new ArrayList<>();
    if (lower.compareTo(other.lower) < 0) {
      result.add(new Range<>(lower, other.lower));
    }
    if (other.upper.compareTo(upper) < 0) {
      result.add(new Range<>(other.upper, upper));
    }

    return result;
  }

}
