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
package de.hipphampel.restcli.utils;

import de.hipphampel.restcli.exception.ExecutionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionUtils {

  public static Map<String, Object> mergeVariables(Map<String, Object> parent, Map<String, Object> child) {
    child = child == null ? Map.of() : child;
    if (parent == null || parent.isEmpty()) {
      return child;
    }
    Map<String, Object> variables = new HashMap<>(parent);
    variables.putAll(child);
    return Collections.unmodifiableMap(variables);
  }

  public static Map<String, List<String>> fillHeaders(Map<String, List<String>> headers, List<KeyValue<String>> keyValues,
      boolean removeAllowed) {
    Map<String, List<String>> localHeaders = new HashMap<>();
    headers = new HashMap<>(headers);
    for (KeyValue<String> keyValue : keyValues) {
      String key = keyValue.key();
      if (!removeAllowed && !keyValue.hasValue()) {
        throw new ExecutionException("Removing headers (\"%s\") not allowed.".formatted(key));
      }

      if (keyValue.hasValue()) {
        localHeaders.computeIfAbsent(key, ignore -> new ArrayList<>()).add(keyValue.value());
      } else {
        headers.remove(key);
        localHeaders.remove(key);
      }
    }

    return mergeHeaders(headers, localHeaders);
  }

  public static Map<String, List<String>> mergeHeaders(Map<String, List<String>> parent, Map<String, List<String>> child) {
    child = child == null ? Map.of() : child;
    if (parent == null || parent.isEmpty()) {
      return child;
    }
    Map<String, List<String>> headers = new HashMap<>(parent);
    for (Map.Entry<String, List<String>> entry : child.entrySet()) {
      headers.compute(entry.getKey(), (headerName, parentHeaderValue) -> {
        List<String> headerValue = new ArrayList<>(entry.getValue());
        if (parentHeaderValue != null) {
          headerValue.addAll(parentHeaderValue);
        }
        return headerValue;
      });
    }
    return headers;
  }
}
