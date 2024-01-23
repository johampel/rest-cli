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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Arrays;
import java.util.Objects;

@RegisterForReflection
public record InputStreamProviderConfig(Type type, String value, boolean interpolate) {

  public enum Type {
    string,
    path,
    url,
    builtin,
    stdin,
  }

  @JsonCreator
  public InputStreamProviderConfig(
      @JsonProperty("type") Type type,
      @JsonProperty("value") String value,
      @JsonProperty("interpolate") boolean interpolate) {
    this.type = Objects.requireNonNull(type);
    this.value = Objects.requireNonNull(value);
    this.interpolate = interpolate;
  }

  public static InputStreamProviderConfig fromString(String str) {
    if (str == null) {
      return null;
    }
    boolean interpolate = str.startsWith("%");
    if (interpolate) {
      str = str.substring(1);
    }
    if (Objects.equals("@", str)) {
      return new InputStreamProviderConfig(Type.stdin, "", interpolate);
    } else if (str.startsWith("@")) {
      return new InputStreamProviderConfig(Type.path, str.substring(1), interpolate);
    }

    int pos = str.indexOf(':');
    if (pos != -1) {
      String typeStr = str.substring(0, pos);
      Type type = Arrays.stream(Type.values())
          .filter(t -> Objects.equals(typeStr, t.name()))
          .findFirst()
          .orElse(null);
      if (type != null) {
        return new InputStreamProviderConfig(type, str.substring(pos + 1), interpolate);
      } else {
        return new InputStreamProviderConfig(Type.string, str, interpolate);
      }
    } else {
      return new InputStreamProviderConfig(Type.string, str, interpolate);
    }
  }

  @Override
  public String toString() {
    return (interpolate ? "%" : "") + type + ":" + value;
  }
}
