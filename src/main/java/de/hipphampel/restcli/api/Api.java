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


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.command.CommandContext;
import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.rest.BodyAndHeaders;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;

@RegisterForReflection
public class Api {

  private final CommandContext context;
  private final ObjectMapper objectMapper;
  private final Transformer transformer;

  public Api(CommandContext context, ObjectMapper objectMapper, Transformer transformer) {
    this.context = Objects.requireNonNull(context);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.transformer = Objects.requireNonNull(transformer);
  }


  public String beautify(BodyAndHeaders<?> body) throws IOException {
    // First try JSON
    try {
      return beautifyJson(body.getJsonBody());
    } catch (Exception e) {
      // Fall through...
    }

    // Next try XML
    try {
      return beautifyXml(body.getXmlBody());
    } catch (Exception e) {
      // Fall through...
    }
    return body.getStringBody();
  }

  String beautifyJson(Object value) throws JsonProcessingException {
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
  }

  String beautifyXml(Document value) throws TransformerException, IOException {
    try (StringWriter out = new StringWriter()) {
      transformer.transform(new DOMSource(value), new StreamResult(out));
      return out.toString();
    }
  }

  public Object jq(Object value, String path) {
    return JsonPath.read(value, path);
  }

  public String url_encode(String str) {
    return URLEncoder.encode(str, StandardCharsets.UTF_8);
  }

  public String sh(String... args) throws IOException, InterruptedException {
    Process process = new ProcessBuilder(args).start();
    String stdout;
    String stderr;
    try (InputStream in = process.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      stdout = out.toString(StandardCharsets.UTF_8);
    }
    try (InputStream in = process.getErrorStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      stderr = out.toString(StandardCharsets.UTF_8);
    }
    if (process.waitFor() != 0) {
      throw new ExecutionException("Command %s failed: %s".formatted(String.join(" ", args), stderr));
    }
    return stdout;
  }


  public String call(String... args) throws IOException {
    try (StringWriter out = new StringWriter(); StringWriter err = new StringWriter()) {
      boolean result = context.commandInvoker().invokeAliasCommand(
          context,
          List.of(args),
          new Output(out).withOutputWidth(-1).withStyles(false),
          new Output(err).withOutputWidth(-1).withStyles(false));
      if (!result) {
        throw new ExecutionException("sub command failed: %s.".formatted(err.toString()));
      }
      return out.toString();
    }
  }
}
