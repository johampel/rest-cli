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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.hipphampel.restcli.exception.ExecutionException;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TemplateRendererTest {

  @Inject
  TemplateRenderer renderer;

  @Test
  void render_Success() {
    TemplateModel model = new TemplateModel(Map.of(
        "abc", "def",
        "ghi", "jkl"
    ));
    assertThat(renderer.render("${abc}123${ghi}", model)).isEqualTo("def123jkl");
  }


  @Test
  void render_int() {
    TemplateModel model = new TemplateModel(Map.of(
        "abc", 4711
    ));
    assertThat(renderer.render("${abc}", model)).isEqualTo("4711");
  }

  @Test
  void render_BadPlaceholder() {
    TemplateModel model = new TemplateModel(Map.of(
    ));
    assertThatThrownBy(() -> renderer.render("${bad}", model))
        .isInstanceOf(ExecutionException.class)
        .hasMessage("Reference to unknown variable \"bad\". Consider to start the application with the `--interactive` option.");
  }

  @Test
  void render_ApiCall() {
    TestApi api = new TestApi();
    TemplateModel model = new TemplateModel(Map.of(
        "a", "1",
        "b", "2"
    ), api);
    assertThat(renderer.render("${_.add(\"${a}\", \"${b}\")}", model)).isEqualTo("3");
  }

  @RegisterForReflection
  public static class TestApi {

    public String add(String a, String b) {
      return String.valueOf(Integer.parseInt(a) + Integer.parseInt(b));
    }
  }
}
