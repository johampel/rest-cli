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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RangeTest {

  @Test
  void ctor_fail() {
    assertThatThrownBy(() -> new Range<>(null, 1)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Range<>(1, null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Range<>(1, 0)).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @CsvSource({
      "0, 0,  0, false",
      "0, 3, -1, false",
      "0, 3,  0, true",
      "0, 3,  1, true",
      "0, 3,  2, true",
      "0, 3,  3, false",
  })
  void contains(int lower, int upper, int value, boolean expected) {
    assertThat(new Range<>(lower, upper).contains(value)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "0, 0, true",
      "0, 1, false"
  })
  void isEmpty(int lower, int upper, boolean expected) {
    assertThat(new Range<>(lower, upper).isEmpty()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "5, 15,  0,  5,   , ",
      "5, 15, 15, 20,   , ",
      "5, 15,  0, 10,  5, 10",
      "5, 15,  5, 15,  5, 15",
      "5, 15,  6, 9,   6,  9",
      "5, 15, 10, 20, 10, 15",
  })
  void intersect(int lower1, int upper1, int lower2, int upper2, Integer expectedLower, Integer expectedUpper) {
    Optional<Range<Integer>> expected = Optional.ofNullable(expectedLower).map(ign -> new Range<>(expectedLower, expectedUpper));

    assertThat(new Range<>(lower1, upper1).intersect(new Range<>(lower2, upper2))).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "5, 15,  0,  5,  5, 15,   ,",
      "5, 15, 15, 20,  5, 15,   ,",
      "5, 15,  5, 15,   ,   ,   ,",
      "5, 15,  0, 10, 10, 15,   ,",
      "5, 15, 10, 20, 5,  10,   ,",
      "5, 15,  8, 11, 5,   8, 11, 15",
  })
  void exclude(int lower1, int upper1, int lower2, int upper2, Integer expectedLower1, Integer expectedUpper1, Integer expectedLower2,
      Integer expectedUpper2) {
    List<Range<Integer>> expected = new ArrayList<>();
    Optional.ofNullable(expectedLower1).map(ign -> new Range<>(expectedLower1, expectedUpper1)).ifPresent(expected::add);
    Optional.ofNullable(expectedLower2).map(ign -> new Range<>(expectedLower2, expectedUpper2)).ifPresent(expected::add);

    assertThat(new Range<>(lower1, upper1).exclude(new Range<>(lower2, upper2))).isEqualTo(expected);
  }
}
