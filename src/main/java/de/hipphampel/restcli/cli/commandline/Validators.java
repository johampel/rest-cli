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
package de.hipphampel.restcli.cli.commandline;

import de.hipphampel.restcli.cli.commandline.CommandLineSpec.Positional;
import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.exception.UsageException;
import de.hipphampel.restcli.template.TemplateAddress;
import java.util.function.BiConsumer;

public class Validators {

  private Validators() {

  }

  public static final BiConsumer<Positional, String> POSITIVE_LONG_VALIDATOR = (positional, value) -> {
    try {
      if (Long.parseLong(value) <= 0) {
        throw new Exception();
      }
    } catch (Exception e) {
      throw new UsageException("Value \"%s\" for \"%s\" is not a positive long value.".formatted(
          value, positional.name()));
    }
  };

  public static final BiConsumer<Positional, String> COMMAND_ADDRESS_VALIDATOR = (positional, value) -> {
    try {
      CommandAddress.fromString(value);
    } catch (Exception e) {
      throw new UsageException("Value \"%s\" passed for \"%s\" is not a valid command address.".formatted(value, positional.name()));
    }
  };

  public static final BiConsumer<Positional, String> TEMPLATE_ADDRESS_VALIDATOR = (positional, value) -> {
    try {
      TemplateAddress.fromString(value);
    } catch (Exception e) {
      throw new UsageException(
          "Value \"%s\" passed for \"%s\" is not a valid output template address.".formatted(value, positional.name()));
    }
  };

  public static final BiConsumer<Positional, String> KEY_OPT_VALUE_VALIDATOR = (positional, value) -> {
    if (value.isEmpty()) {
      throw new UsageException("Key value pair \"%s\" passed for \"%s\" must not be empty.".formatted(value, positional.name()));
    } else if (value.startsWith("=")) {
      throw new UsageException("Key value pair \"%s\" passed for \"%s\" has no key part.".formatted(value, positional.name()));
    }
  };

  public static final BiConsumer<Positional, String> KEY_VALUE_VALIDATOR = (positional, value) -> {
    if (value.isEmpty()) {
      throw new UsageException("Key value pair \"%s\" passed for \"%s\" must not be empty.".formatted(value, positional.name()));
    } else if (value.startsWith("=")) {
      throw new UsageException("Key value pair \"%s\" passed for \"%s\" has no key part.".formatted(value, positional.name()));
    } else if (!value.contains("=")) {
      throw new UsageException("Key value pair \"%s\" passed for \"%s\" has no `=` sign.".formatted(value, positional.name()));
    }
  };

}
