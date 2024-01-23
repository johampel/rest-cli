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

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateModelTest {


  @Test
  void get_defined() {
    TemplateModel model = new TemplateModel(Map.of("abc", "def"));
    assertThat(model.get("abc")).isEqualTo("def");
  }

  @Test
  void get_undefined() {
    AtomicReference<String> ref = new AtomicReference<>();
    TemplateModel model = new TemplateModel(Map.of(), null, key -> {
      ref.set(UUID.randomUUID().toString());
      return ref.get();
    });

    Object value = model.get("abc");
    assertThat(value).isNotNull().isEqualTo(ref.get()).isSameAs(model.get("abc"));
    assertThat(model.get("def")).isNotNull().isNotSameAs(value);
  }
}
