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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnvironmentTest {

  private final EnvironmentConfig childConfig = new EnvironmentConfig(
      "parent",
      Map.of("a", "b", "c", "d"),
      Map.of("a", List.of("b", "c"), "d", List.of("e", "f")),
      1000L);
  private final EnvironmentConfig parentConfig = new EnvironmentConfig(
      null,
      Map.of("c", "e", "f", "h"),
      Map.of("d", List.of("b", "c"), "c", List.of("a", "b")),
      2000L);

  private Environment environment;

  @BeforeEach
  void beforeEach() {
    environment = new Environment("parent", "name", parentConfig, childConfig);
  }

  @Test
  void name() {
    assertThat(environment.getName()).isEqualTo("name");
    assertThat(environment.getOriginalName()).isEqualTo("name");
    assertThat(environment.isNamedChanged()).isFalse();

    environment.setName("newName");

    assertThat(environment.getName()).isEqualTo("newName");
    assertThat(environment.getOriginalName()).isEqualTo("name");
    assertThat(environment.isNamedChanged()).isTrue();
  }

  @Test
  void parent() {
    assertThat(environment.getParent()).isEqualTo("parent");
    assertThat(environment.getLocalConfig().parent()).isEqualTo("parent");
    assertThat(environment.isParentChanged()).isFalse();

    environment.setParent("newParent");

    assertThat(environment.getParent()).isEqualTo("newParent");
    assertThat(environment.getLocalConfig().parent()).isEqualTo("newParent");
    assertThat(environment.isParentChanged()).isTrue();
  }

  @Test
  void variables() {
    assertThat(environment.getVariables()).isEqualTo(Map.of(
        "a", "b", "c", "d", "f", "h"
    ));
    assertThat(environment.getLocalVariables()).isEqualTo(Map.of(
        "a", "b", "c", "d"
    ));
    assertThat(environment.getLocalConfig()).isEqualTo(new EnvironmentConfig(
        environment.getParent(),
        environment.getLocalVariables(),
        environment.getLocalHeaders(),
        1000L));
    assertThat(environment.getParentConfig()).isEqualTo(new EnvironmentConfig(
        null,
        parentConfig.variables(),
        parentConfig.headers(),
        2000L));

    environment.setLocalVariables(Map.of("a", "1", "g", "2"));
    assertThat(environment.getVariables()).isEqualTo(Map.of(
        "a", "1", "c", "e", "f", "h", "g", "2"
    ));
    assertThat(environment.getLocalVariables()).isEqualTo(Map.of(
        "a", "1", "g", "2"
    ));
    assertThat(environment.getLocalConfig()).isEqualTo(new EnvironmentConfig(
        environment.getParent(),
        environment.getLocalVariables(),
        environment.getLocalHeaders(),
        1000L));
    assertThat(environment.getParentConfig()).isEqualTo(new EnvironmentConfig(
        null,
        parentConfig.variables(),
        parentConfig.headers(),
        2000L));
  }

  @Test
  void headers() {
    assertThat(environment.getHeaders()).isEqualTo(Map.of(
        "a", List.of("b", "c"), "c", List.of("a", "b"), "d", List.of("e", "f", "b", "c")

    ));
    assertThat(environment.getLocalHeaders()).isEqualTo(Map.of(
        "a", List.of("b", "c"), "d", List.of("e", "f")
    ));
    assertThat(environment.getLocalConfig()).isEqualTo(new EnvironmentConfig(
        environment.getParent(),
        environment.getLocalVariables(),
        environment.getLocalHeaders(),
        1000L));
    assertThat(environment.getParentConfig()).isEqualTo(new EnvironmentConfig(
        null,
        parentConfig.variables(),
        parentConfig.headers(),
        2000L));

    environment.setLocalHeaders(Map.of("d", List.of("1", "2"), "e", List.of("2", "3")));
    assertThat(environment.getHeaders()).isEqualTo(Map.of(
        "c", List.of("a", "b"), "d", List.of("1", "2", "b", "c"), "e", List.of("2", "3")
    ));
    assertThat(environment.getLocalHeaders()).isEqualTo(Map.of(
        "d", List.of("1", "2"), "e", List.of("2", "3")
    ));
    assertThat(environment.getLocalConfig()).isEqualTo(new EnvironmentConfig(
        environment.getParent(),
        environment.getLocalVariables(),
        environment.getLocalHeaders(),
        1000L));
    assertThat(environment.getParentConfig()).isEqualTo(new EnvironmentConfig(
        null,
        parentConfig.variables(),
        parentConfig.headers(),
        2000L));
  }
}
