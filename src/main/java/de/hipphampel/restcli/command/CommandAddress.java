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
package de.hipphampel.restcli.command;

import java.nio.file.Path;

public record CommandAddress(CommandAddress parent, String name) {

  public static final CommandAddress ROOT = new CommandAddress(null, null);

  public CommandAddress {
    if (parent != null && name == null) {
      throw new IllegalArgumentException("Non root CommandAddress must have a not empty name");
    } else if (parent == null && name != null) {
      throw new IllegalArgumentException("Root CommandAddress must not have a name");
    } else if (parent != null && !isValidCommandName(name)) {
      throw new IllegalArgumentException("\"%s\" is not a valid command address name".formatted(name));
    }
  }

  public boolean isRoot() {
    return name == null;
  }

  public CommandAddress child(String child) {
    return new CommandAddress(this, child);
  }

  public CommandAddress child(CommandAddress child) {
    if (child.isRoot()) {
      return this;
    } else {
      return child(child.parent).child(child.name);
    }
  }

  @Override
  public String toString() {
    if (parent == null) {
      return "";
    } else if (parent.name == null) {
      return name;
    } else {
      return parent + "/" + name;
    }
  }

  public Path toPath() {
    if (name == null) {
      return Path.of(".");
    } else if (parent != null) {
      return parent.toPath().resolve(name);
    } else {
      return Path.of(name);
    }
  }

  public static CommandAddress fromString(String str) {
    if (str == null) {
      return null;
    }

    if (str.startsWith("/") || str.endsWith("/")) {
      throw new IllegalArgumentException("\"%s\" is not a valid command address string".formatted(str));
    }

    CommandAddress address = ROOT;
    if (str.isEmpty()) {
      return address;
    }

    String[] components = str.split("/");
    for (String component : components) {
      address = address.child(component);
    }
    return address;
  }

  public static boolean isValidCommandName(String name) {
    if (name == null || name.isEmpty() || !Character.isJavaIdentifierStart(name.charAt(0))) {
      return false;
    }
    for (int i = 1; i < name.length(); i++) {
      char ch = name.charAt(i);
      if (ch!='-' && !Character.isJavaIdentifierPart(ch)) {
        return false;
      }
    }
    return true;
  }
}
