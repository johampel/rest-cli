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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CategorizerTest {


  private Categorizer<Integer, String> categorizer;

  @BeforeEach
  void beforeEach() {
    categorizer = new Categorizer<>();
  }

  @Test
  void get_noDefault() {
    categorizer.add(1, 3, "1-3");
    assertThat(categorizer.get(0)).isEmpty();
    assertThat(categorizer.get(1)).contains("1-3");
    assertThat(categorizer.get(2)).contains("1-3");
    assertThat(categorizer.get(3)).isEmpty();
  }

  @Test
  void get_withDefault() {
    categorizer
        .defaultValue("default")
        .add(1, 3, "1-3");
    assertThat(categorizer.get(0)).contains("default");
    assertThat(categorizer.get(1)).contains("1-3");
    assertThat(categorizer.get(2)).contains("1-3");
    assertThat(categorizer.get(3)).contains("default");
  }

  @Test
  void add_nonOverlapping() {
    categorizer.add(3, 5, "3-5");
    categorizer.add(1, 3, "1-3");
    categorizer.add(5, 7, "5-7");
    assertThat(categorizer.get(0)).isEmpty();
    assertThat(categorizer.get(1)).contains("1-3");
    assertThat(categorizer.get(2)).contains("1-3");
    assertThat(categorizer.get(3)).contains("3-5");
    assertThat(categorizer.get(4)).contains("3-5");
    assertThat(categorizer.get(5)).contains("5-7");
    assertThat(categorizer.get(6)).contains("5-7");
    assertThat(categorizer.get(7)).isEmpty();
  }

  @Test
  void add_overlapping() {
    categorizer.add(1, 20, "0-20");
    categorizer.add(5, 15, "5-15");
    categorizer.add(3, 7, "3-7");
    categorizer.add(13, 17, "13-17");
    categorizer.add(9, 11, "9-11");
    assertThat(categorizer.get(0)).isEmpty();
    assertThat(categorizer.get(1)).contains("0-20");
    assertThat(categorizer.get(2)).contains("0-20");
    assertThat(categorizer.get(3)).contains("3-7");
    assertThat(categorizer.get(4)).contains("3-7");
    assertThat(categorizer.get(5)).contains("3-7");
    assertThat(categorizer.get(6)).contains("3-7");
    assertThat(categorizer.get(7)).contains("5-15");
    assertThat(categorizer.get(8)).contains("5-15");
    assertThat(categorizer.get(9)).contains("9-11");
    assertThat(categorizer.get(10)).contains("9-11");
    assertThat(categorizer.get(11)).contains("5-15");
    assertThat(categorizer.get(12)).contains("5-15");
    assertThat(categorizer.get(13)).contains("13-17");
    assertThat(categorizer.get(14)).contains("13-17");
    assertThat(categorizer.get(15)).contains("13-17");
    assertThat(categorizer.get(16)).contains("13-17");
    assertThat(categorizer.get(17)).contains("0-20");
    assertThat(categorizer.get(18)).contains("0-20");
    assertThat(categorizer.get(19)).contains("0-20");
    assertThat(categorizer.get(20)).isEmpty();
  }
}
