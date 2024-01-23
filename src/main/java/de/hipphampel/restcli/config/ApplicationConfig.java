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
package de.hipphampel.restcli.config;

import de.hipphampel.restcli.env.Environment;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Objects;

@RegisterForReflection
public class ApplicationConfig {

  private String environment;
  private int outputWidth;
  private boolean outputWithStyles;
  private String outputTemplate;
  private long requestTimeout;

  public ApplicationConfig() {
    this.environment = Environment.EMPTY;
    this.outputWidth = 80;
    this.outputWithStyles = true;
    this.outputTemplate = "default";
    this.requestTimeout = 30_000L;

  }

  public int getOutputWidth() {
    return outputWidth;
  }

  public ApplicationConfig setOutputWidth(int outputWidth) {
    this.outputWidth = outputWidth;
    return this;
  }

  public boolean isOutputWithStyles() {
    return outputWithStyles;
  }

  public ApplicationConfig setOutputWithStyles(boolean outputWithStyles) {
    this.outputWithStyles = outputWithStyles;
    return this;
  }

  public String getOutputTemplate() {
    return outputTemplate;
  }

  public ApplicationConfig setOutputTemplate(String outputTemplate) {
    this.outputTemplate = Objects.requireNonNull(outputTemplate);
    return this;
  }

  public long getRequestTimeout() {
    return requestTimeout;
  }

  public ApplicationConfig setRequestTimeout(long requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  public String getEnvironment() {
    return environment;
  }

  public void setEnvironment(String environment) {
    this.environment = environment;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApplicationConfig config = (ApplicationConfig) o;
    return outputWidth == config.outputWidth && outputWithStyles == config.outputWithStyles && requestTimeout == config.requestTimeout
        && Objects.equals(environment, config.environment) && Objects.equals(outputTemplate, config.outputTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environment, outputWidth, outputWithStyles, outputTemplate, requestTimeout);
  }
}
