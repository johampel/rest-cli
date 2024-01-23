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

public class HelpSnippets {

  public static final String FURTHER_INFOS_INPUT_SOURCE = """ 
      *Input source:*
            
      An input source is basically a string literal, which represents either a string, refers to a file, an URL, or a builtin resource.
      Detailed information can be obtained by typing `${applicationName} help :input-sources`.
      """;

  public static final String FURTHER_INFOS_TEMPLATE_ADDRESS = """ 
      *Template addresses:*

      A template address is intended to uniquely identify a template. A template address has the general format `<name>@<command>`, whereas
      the `<name>` is the local name of the template and `<command>` the address of the owning command.
      For details, please type `${applicationName} help :addresses`.
      """;
  public static final String FURTHER_INFOS_TEMPLATE_NAME = """ 
      *Template names:*

      A template can be referenced without the owning command in case it is referenced from within a command context. If so, the template
      name resolves to the best matching template address according to the currently executed command. For example, given the command with 
      address `foo/bar` and the template name `name`, it refers to the first template address that exists in the list. `name@foo/bar`, 
      `name@foo` or `name@`.
      """;

  public static final String FURTHER_INFOS_COMMAND_ADDRESS = """ 
      *Command addresses:*

      A command address uniquely identifies a command in ${applicationName}. It is basically a path the command, so
      `foo/bar` refers to the command `bar` that is a child command of `foo`. The special empty path (``) refers to the root command that 
      represents the application itself. For details, please type `${applicationName} help :addresses`.
      """;


}
