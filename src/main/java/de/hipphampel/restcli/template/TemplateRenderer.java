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

import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.io.InputStreamProvider;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

@ApplicationScoped
public class TemplateRenderer {

  private final Configuration configuration;

  public TemplateRenderer() {
    this.configuration = new Configuration(Configuration.VERSION_2_3_22);
    this.configuration.setNumberFormat("c");
    this.configuration.setObjectWrapper(new BeansWrapperBuilder(this.configuration.getIncompatibleImprovements()).build());
  }

  public String render(String template, TemplateModel model) {
    try (StringReader templateReader = new StringReader(template)) {
      return render(templateReader, model);
    }
  }

  public String render(InputStream templateStream, TemplateModel model) {
    InputStreamReader templateReader = new InputStreamReader(templateStream, StandardCharsets.UTF_8);
    return render(templateReader, model);
  }

  public String render(InputStreamProvider templateStreamProvider, TemplateModel model) {
    try (InputStream templateStream = templateStreamProvider.open()) {
      InputStreamReader templateReader = new InputStreamReader(templateStream, StandardCharsets.UTF_8);
      return render(templateReader, model);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to render template.", ioe);
    }
  }

  public String render(Reader templateReader, TemplateModel model) {
    try (StringWriter out = new StringWriter()) {
      return render(templateReader, model, out).toString();
    } catch (IOException e) {
      throw new ExecutionException("Failed to render template.", e);
    }
  }


  public <T extends Writer> T render(InputStreamProvider templateStreamProvider, TemplateModel model, T output) {
    try (InputStream templateStream = templateStreamProvider.open()) {
      InputStreamReader templateReader = new InputStreamReader(templateStream, StandardCharsets.UTF_8);
      return render(templateReader, model, output);
    } catch (IOException ioe) {
      throw new ExecutionException("Failed to render template.", ioe);
    }
  }

  public <T extends Writer> T render(Reader templateReader, TemplateModel model, T output) {
    try {
      Template template = new Template(null, templateReader, configuration);
      template.process(model, output);
      return output;
    } catch (TemplateException | IOException e) {
      throw new ExecutionException("Failed to render template.", e);
    }
  }
}
