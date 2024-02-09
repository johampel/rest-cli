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

import de.hipphampel.restcli.cli.Output;
import de.hipphampel.restcli.io.InputStreamProvider;
import de.hipphampel.restcli.template.TemplateModel;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.Objects;

public record RequestContext(
    HttpClient client,
    TemplateModel templateModel,
    OutputFormat format,
    Output out,
    Output err,
    boolean interactive) {

  public RequestContext(
      HttpClient client,
      TemplateModel templateModel,
      OutputFormat format,
      Output out,
      Output err,
      boolean interactive) {
    this.client = Objects.requireNonNull(client);
    this.templateModel = Objects.requireNonNull(templateModel);
    this.format = Objects.requireNonNull(format);
    this.out = Objects.requireNonNull(out);
    this.err = Objects.requireNonNull(err);
    this.interactive = interactive;
  }

  public record OutputFormat(InputStreamProvider format, Map<String, String> parameters) {

    public OutputFormat(InputStreamProvider format, Map<String, String> parameters) {
      this.format = Objects.requireNonNull(format);
      this.parameters = Objects.requireNonNull(parameters);
    }
  }
}
