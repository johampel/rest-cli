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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.io.InputStreamProvider;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

@QuarkusTest
class RequestTest {

  @Inject
  ObjectMapper objectMapper;

  @Inject
  DocumentBuilderFactory documentBuilderFactory;

  @Test
  void toHttpRequest() throws URISyntaxException {
    Request request = new Request(
        objectMapper,
        documentBuilderFactory,
        "FOO1",
        "http://FOO2",
        Map.of(
            "FOO3", List.of("FOO4, FOO5"),
            "FOO6", List.of("FOO7, FOO8")),
        InputStreamProvider.ofString("FOO9"),
        Duration.of(1, ChronoUnit.DAYS),
        true);

    HttpRequest httpRequest = request.toHttpRequest();

    ByteArraySubscriber subscriber = new ByteArraySubscriber();
    assertThat(httpRequest).isNotNull();
    assertThat(httpRequest.method()).isEqualTo("FOO1");
    assertThat(httpRequest.uri()).isEqualTo(new URI("http://FOO2"));
    assertThat(httpRequest.headers()).isEqualTo(HttpHeaders.of(
        Map.of(
            "FOO3", List.of("FOO4, FOO5"),
            "FOO6", List.of("FOO7, FOO8")),
        (a, b) -> true
    ));
    assertThat(httpRequest.timeout()).contains(Duration.of(1, ChronoUnit.DAYS));
    assertThat(httpRequest.expectContinue()).isTrue();
    httpRequest.bodyPublisher().orElseThrow().subscribe(subscriber);
    assertThat(subscriber.getByteArray()).isEqualTo("FOO9".getBytes(StandardCharsets.UTF_8));
  }

  static class ByteArraySubscriber implements Subscriber<ByteBuffer> {

    private Subscription subscription;
    private final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
      this.subscription.request(1);
    }

    @Override
    public void onNext(ByteBuffer item) {
      try {
        byte[] array = new byte[item.remaining()];
        item.get(array);
        byteArray.write(array);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      this.subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
      throw new RuntimeException(throwable);
    }

    @Override
    public void onComplete() {
    }

    public byte[] getByteArray() {
      return byteArray.toByteArray();
    }
  }

}
