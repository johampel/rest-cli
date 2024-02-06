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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.hipphampel.restcli.io.Openable;
import de.hipphampel.restcli.utils.Pair;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@RegisterForReflection
public abstract class BodyAndHeaders<T extends Openable> {

  private final ObjectMapper objectMapper;
  private final DocumentBuilderFactory documentBuilderFactory;

  public BodyAndHeaders(ObjectMapper objectMapper, DocumentBuilderFactory documentBuilderFactory) {
    this.objectMapper = objectMapper;
    this.documentBuilderFactory = documentBuilderFactory;
  }

  public abstract Map<String, List<String>> getHeaders();

  public List<String> headerKeys() {
    return new ArrayList<>(getHeaders().keySet());
  }

  @JsonIgnore
  public abstract T getBody();

  public boolean hasBody() {
    return getBody() != null;
  }

  public String getStringBody() throws IOException {
    try (InputStream in = getBody().open(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      in.transferTo(out);
      return out.toString(StandardCharsets.UTF_8);
    }
  }

  @JsonIgnore
  public Object getJsonBody() throws IOException {
    try (InputStream in = getBody().open()) {
      return objectMapper.readValue(in, Object.class);
    }
  }

  @JsonIgnore
  public Document getXmlBody() throws IOException {
    try (InputStream in = getBody().open()) {
      return documentBuilderFactory.newDocumentBuilder().parse(in);
    } catch (ParserConfigurationException | SAXException e) {
      throw new IOException(e);
    }
  }
}
