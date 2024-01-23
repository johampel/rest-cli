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
package de.hipphampel.restcli.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.condition.OS.LINUX;
import static org.junit.jupiter.api.condition.OS.MAC;
import static org.junit.jupiter.api.condition.OS.WINDOWS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.rest.Response;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.io.TempDir;

@QuarkusTest
class ApiTest extends CommandTestBase {

  @Inject
  ApiFactory apiFactory;
  @Inject
  DocumentBuilderFactory documentBuilderFactory;
  @Inject
  ObjectMapper objectMapper;
  Api api;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    api = apiFactory.createApi(context);
  }

  @Test
  void beautifyJson() throws IOException {
    HttpResponse<InputStream> httpResponse = mock(HttpResponse.class);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream("""
        {"red":123,"green":456,"blue":789}
        """.getBytes(StandardCharsets.UTF_8)));
    try (Response response = new Response(documentBuilderFactory, objectMapper, httpResponse, 10000)) {
      assertThat(api.beautifyJson(response.getJsonBody())).isEqualTo("""
          {
            "red" : 123,
            "green" : 456,
            "blue" : 789
          }""");
    }
  }

  @Test
  void beautifyXml() throws IOException, TransformerException {
    HttpResponse<InputStream> httpResponse = mock(HttpResponse.class);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream("""
        <foo><red>123</red><green>456</green><blue>789</blue></foo>
        """.getBytes(StandardCharsets.UTF_8)));
    try (Response response = new Response(documentBuilderFactory, objectMapper, httpResponse, 10000)) {
      assertThat(api.beautifyXml(response.getXmlBody())).isEqualTo("""
          <?xml version="1.0" encoding="UTF-8" standalone="no"?>
          <foo>
            <red>123</red>
            <green>456</green>
            <blue>789</blue>
          </foo>
          """);
    }
  }

  @Test
  void beautify_JsonBody() throws IOException {
    HttpResponse<InputStream> httpResponse = mock(HttpResponse.class);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream("""
        {"red":123,"green":456,"blue":789}
        """.getBytes(StandardCharsets.UTF_8)));
    try (Response response = new Response(documentBuilderFactory, objectMapper, httpResponse, 10000)) {
      assertThat(api.beautify(response)).isEqualTo("""
          {
            "red" : 123,
            "green" : 456,
            "blue" : 789
          }""");
    }
  }

  @Test
  void beautify_XmlBody() throws IOException {
    HttpResponse<InputStream> httpResponse = mock(HttpResponse.class);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream("""
        <foo><red>123</red><green>456</green><blue>789</blue></foo>
        """.getBytes(StandardCharsets.UTF_8)));
    try (Response response = new Response(documentBuilderFactory, objectMapper, httpResponse, 10000)) {
      assertThat(api.beautify(response)).isEqualTo("""
          <?xml version="1.0" encoding="UTF-8" standalone="no"?>
          <foo>
            <red>123</red>
            <green>456</green>
            <blue>789</blue>
          </foo>
          """);
    }
  }

  @Test
  void jq() throws IOException {
    HttpResponse<InputStream> httpResponse = mock(HttpResponse.class);
    when(httpResponse.body()).thenReturn(new ByteArrayInputStream("""
        [
          {"id": 1 },
          {"id": 2 },
          {"id": 4 }
        ]
        """.getBytes(StandardCharsets.UTF_8)));
    try (Response response = new Response(documentBuilderFactory, objectMapper, httpResponse, 10000)) {
      assertThat(api.jq(response, "$.[*].id")).isEqualTo(List.of(1, 2, 4));
    }
  }

  @Test
  @EnabledOnOs({LINUX, MAC})
  void sh_unix() throws IOException, InterruptedException {
    assertThat(api.sh("./mvnw", "help:evaluate", "-Dexpression=project.groupId", "-q", "-DforceStdout"))
        .isEqualTo("de.hipphampel");
  }

  @Test
  @EnabledOnOs({WINDOWS})
  void sh_windows() throws IOException, InterruptedException {
    assertThat(api.sh(".\\mvnw.cmd", "help:evaluate", "-Dexpression=project.groupId", "-q", "-DforceStdout"))
        .isEqualTo("de.hipphampel");
  }

  @Test
  void call() throws IOException {
    assertThat(api.call("cmd", "tree", "cfg", "-db"))
        .isEqualTo("""
            cfg (builtin, parent)
            ├── get (builtin)
            └── set (builtin)
            """);
  }
}
