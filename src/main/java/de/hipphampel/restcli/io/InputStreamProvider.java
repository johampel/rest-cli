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
package de.hipphampel.restcli.io;

import de.hipphampel.restcli.exception.ExecutionException;
import de.hipphampel.restcli.template.TemplateModel;
import de.hipphampel.restcli.template.TemplateRenderer;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public sealed interface InputStreamProvider extends Openable {

  static InputStreamProvider ofString(String str) {
    return new SupplierInputStreamProvider(() -> new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8)));
  }

  static InputStreamProvider ofURL(URL url) {
    return new SupplierInputStreamProvider(() -> {
      try {
        return url.openStream();
      } catch (IOException e) {
        throw new ExecutionException("Failed to open \"%s\" for reading.".formatted(url), e);
      }
    });
  }

  static InputStreamProvider ofBuiltin(String location) {
    return new SupplierInputStreamProvider(() -> InputStreamProvider.class.getResourceAsStream(location));
  }

  static InputStreamProvider ofPath(Path path) {
    return new SupplierInputStreamProvider(() -> {
      try {
        return new FileInputStream(path.toFile());
      } catch (IOException e) {
        throw new ExecutionException("Failed to open \"%s\" for reading.".formatted(path), e);
      }
    });
  }

  static InputStreamProvider interpolated(InputStreamProvider source, TemplateRenderer renderer, TemplateModel model) {
    return new SupplierInputStreamProvider(() -> {
      String rendered = renderer.render(source, model);
      return new ByteArrayInputStream(rendered.getBytes(StandardCharsets.UTF_8));
    });
  }

  record SupplierInputStreamProvider(Supplier<InputStream> supplier) implements InputStreamProvider {

    public SupplierInputStreamProvider(Supplier<InputStream> supplier) {
      this.supplier = Objects.requireNonNull(supplier);
    }

    @Override
    public InputStream open() {
      return supplier.get();
    }
  }
}
