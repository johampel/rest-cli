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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;


class CollectionUtilsTest {

  @Test
  void mergeVariables_noParent() {
    Map<String, Object> child = Map.of("a", "b", "c", "d");

    assertThat(CollectionUtils.mergeVariables(null, child))
        .isSameAs(child)
        .isEqualTo(Map.of("a", "b", "c", "d"));
    assertThat(CollectionUtils.mergeVariables(Map.of(), child))
        .isSameAs(child)
        .isEqualTo(Map.of("a", "b", "c", "d"));
  }

  @Test
  void mergeVariables_withParent() {
    Map<String, Object> child = Map.of("a", "b", "c", "d");
    Map<String, Object> parent = Map.of("c", "b", "d", "e");

    assertThat(CollectionUtils.mergeVariables(parent, child))
        .isEqualTo(Map.of("a", "b", "c", "d", "d", "e"));
  }

  @Test
  void mergeHeaders_noParent() {
    Map<String, List<String>> child = Map.of("a", List.of("b1", "b2"), "c", List.of("d1", "d2"));

    assertThat(CollectionUtils.mergeHeaders(null, child))
        .isSameAs(child)
        .isEqualTo(Map.of("a", List.of("b1", "b2"), "c", List.of("d1", "d2")));
    assertThat(CollectionUtils.mergeHeaders(Map.of(), child))
        .isSameAs(child)
        .isEqualTo(Map.of("a", List.of("b1", "b2"), "c", List.of("d1", "d2")));
  }

  @Test
  void mergeHeaders_withParent() {
    Map<String, List<String>> child = Map.of("a", List.of("b1", "b2"), "c", List.of("d1", "d2"));
    Map<String, List<String>> parent = Map.of("c", List.of("b1", "b2"), "d", List.of("e1", "e2"));

    assertThat(CollectionUtils.mergeHeaders(parent, child))
        .isEqualTo(Map.of("a", List.of("b1", "b2"), "c", List.of("d1", "d2", "b1", "b2"), "d", List.of("e1", "e2")));
  }
}
