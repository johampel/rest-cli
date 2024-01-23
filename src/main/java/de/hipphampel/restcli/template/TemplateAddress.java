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

import de.hipphampel.restcli.command.CommandAddress;
import java.util.Objects;

public record TemplateAddress(CommandAddress command, String name) {

  public TemplateAddress(CommandAddress command, String name) {
    this.command = Objects.requireNonNull(command);
    this.name = Objects.requireNonNull(name);
    if (!isValidTemplateName(name)) {
      throw new IllegalArgumentException("\"%s\" is not a valid template name.".formatted(name));
    }
  }

  @Override
  public String toString() {
    return name + "@" + command;
  }

  public static TemplateAddress fromString(String str) {
    int pos = str.indexOf("@");
    if (pos == -1) {
      throw new IllegalArgumentException("Invalid template address \"%s\".".formatted(str));
    }
    try {
      CommandAddress address = CommandAddress.fromString(str.substring(pos + 1));
      String name = str.substring(0, pos);
      return new TemplateAddress(address, name);
    } catch (IllegalArgumentException iae) {
      throw new IllegalArgumentException("Invalid template address \"%s\".".formatted(str));
    }
  }

  public static boolean isValueTemplateAddress(String address) {
    try {
      fromString(address);
      return true;
    }
    catch(IllegalArgumentException iae) {
      return false;
    }
  }

  public static boolean isValidTemplateName(String name) {
    if (name == null || name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) {
      return false;
    }
    for (int i = 1; i < name.length(); i++) {
      if (!Character.isJavaIdentifierPart(name.charAt(i))) {
        return false;
      }
    }
    return true;
  }

}
