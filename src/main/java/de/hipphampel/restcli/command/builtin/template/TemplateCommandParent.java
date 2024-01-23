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
package de.hipphampel.restcli.command.builtin.template;

import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandUtils;
import de.hipphampel.restcli.command.HelpSection;
import de.hipphampel.restcli.command.builtin.BuiltinParentCommand;
import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
@Unremovable
public class TemplateCommandParent extends BuiltinParentCommand {

  public static final String NAME = "template";

  public TemplateCommandParent() {
    super(
        CommandAddress.fromString(NAME),
        "Collection of commands to manage output templates.",
        Map.of(HelpSection.DESCRIPTION,
            CommandUtils.helpSection("""
                Output templates can be applied to the response of HTTP responses and allow to render according output for the response.
                          
                `template` is a collection of sub commands and has no functionality apart from grouping the commands.
                See the following list for the available sub commands."""),
            HelpSection.FURTHER_INFOS,
            CommandUtils.helpSection("""
                For more information about output templates, type `${applicationName} help :templates`.
                """)));
  }
}
