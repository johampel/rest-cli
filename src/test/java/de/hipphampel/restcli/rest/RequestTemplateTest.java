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
package de.hipphampel.restcli.rest;

import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.io.InputStreamProvider;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RequestTemplateTest {


  @Test
  void test() {
    RequestTemplate template = new RequestTemplate()
        .method("foo1")
        .baseUri("http://foo2")
        .queryParameters(Map.of("foo3", "foo4"))
        .headers(Map.of("foo5", List.of("foo6")))
        .requestBody(InputStreamProvider.ofString("foo7"))
        .timeout(Duration.of(1, ChronoUnit.SECONDS))
        .expectContinue(true);

    assertThat(template.method()).isEqualTo("foo1");
    assertThat(template.baseUri()).isEqualTo("http://foo2");
    assertThat(template.queryParameters()).isEqualTo(Map.of("foo3", "foo4"));
    assertThat(template.headers()).isEqualTo(Map.of("foo5", List.of("foo6")));
    assertThat(template.timeout()).isEqualTo(Duration.of(1, ChronoUnit.SECONDS));
    assertThat(template.expectContinue()).isTrue();
    assertThat(template.requestBody()).isInstanceOf(InputStreamProvider.class);
  }

}
