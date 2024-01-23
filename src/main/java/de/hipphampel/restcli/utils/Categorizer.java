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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

public class Categorizer<K extends Comparable<K>, V> {

  private final NavigableMap<K, Category<K, V>> values;
  private V defaultValue;

  public Categorizer() {
    this.values = new TreeMap<>();
    this.defaultValue = null;
  }

  public V defaultValue() {
    return this.defaultValue;
  }

  public Categorizer<K, V> defaultValue(V defaultCategory) {
    this.defaultValue = defaultCategory;
    return this;
  }

  public Categorizer<K, V> add(K lower, K upper, V value) {

    Category<K, V> newCategory = new Category<>(new Range<>(lower, upper), value);

    K lowerKey = values.lowerKey(lower);
    if (lowerKey == null && !values.isEmpty()) {
      lowerKey = values.firstKey();
    }

    if (lowerKey != null) {
      K upperKey = values.ceilingKey(upper);
      if (upperKey == null) {
        upperKey = values.lastKey();
      }

      NavigableMap<K, Category<K, V>> affectedMap = values.subMap(lowerKey, true, upperKey, true);
      List<Category<K, V>> affectedValues = new ArrayList<>(affectedMap.values());
      affectedMap.clear();
      affectedValues.forEach(affected ->
          affected.range().exclude(newCategory.range)
              .forEach(r -> values.put(r.lower(), new Category<>(r, affected.value))));
    }

    values.put(lower, newCategory);
    return this;
  }

  public Optional<V> get(K key) {
    Entry<K, Category<K, V>> category = values.floorEntry(key);
    if (category == null || !category.getValue().range().contains(key)) {
      return Optional.ofNullable(defaultValue());
    }

    return Optional.of(category.getValue().value);
  }


  record Category<K extends Comparable<K>, V>(Range<K> range, V value) {

    public Category {
      Objects.requireNonNull(value);
      Objects.requireNonNull(range);
    }
  }
}
