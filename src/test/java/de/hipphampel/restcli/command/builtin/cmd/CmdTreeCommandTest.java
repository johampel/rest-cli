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
package de.hipphampel.restcli.command.builtin.cmd;

import static org.assertj.core.api.Assertions.assertThat;

import de.hipphampel.restcli.command.CommandAddress;
import de.hipphampel.restcli.command.CommandTestBase;
import de.hipphampel.restcli.command.config.CommandConfig;
import de.hipphampel.restcli.command.config.CommandConfig.Type;
import de.hipphampel.restcli.command.config.RestCommandConfig;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@QuarkusTest
class CmdTreeCommandTest extends CommandTestBase {

  @Inject
  CmdTreeCommand command;

  @BeforeEach
  protected void beforeEach(@TempDir Path rootDir) throws IOException {
    super.beforeEach(rootDir);
    CommandConfig config = new CommandConfig();

    config.setType(Type.Parent);
    storeCommand(CommandAddress.fromString("a-parent"), config);
    config.setType(Type.Parent);
    storeCommand(CommandAddress.fromString("a-parent/child"), config);

    config.setType(Type.Alias);
    config.setAliasConfig(List.of("the", "alias"));
    storeCommand(CommandAddress.fromString("an-alias"), config);

    config.setType(Type.Http);
    config.setAliasConfig(null);
    config.setRestConfig(new RestCommandConfig().setMethod("GET").setBaseUri("https://example.com"));
    storeCommand(CommandAddress.fromString("a-http"), config);
  }

  @ParameterizedTest
  @MethodSource("execute_data")
  void execute(List<String> args, boolean expectedResult, String expectedOut, String expectedErr) throws IOException {
    assertThat(commandInvoker.invokeCommand(context, command.address(), args)).isEqualTo(expectedResult);
    assertOutput(expectedOut, expectedErr);
  }

  static Stream<Arguments> execute_data() {
    return Stream.of(
        // No arguments
        Arguments.of(
            List.of(),
            true,
            """
                test-app
                    a-http
                    a-parent
                        child
                    an-alias
                    cfg
                        get
                        set
                    cmd
                        alias
                        cp
                        exp
                        group
                        http
                        imp
                        mod
                        mv
                        openapi
                        rm
                        tree
                    env
                        cp
                        exp
                        get
                        imp
                        ls
                        mod
                        mv
                        new
                        rm
                    help
                    http
                    template
                        cp
                        exp
                        get
                        help
                        imp
                        ls
                        mod
                        mv
                        new
                        rm
                """,
            ""),
        // All options
        Arguments.of(
            List.of("-dba"),
            true,
            """
                test-app (builtin, parent)
                ├── a-http (custom, http): GET https://example.com
                ├── a-parent (custom, parent)
                │   └── a-parent/child (custom, parent)
                ├── an-alias (custom, alias): the alias
                ├── cfg (builtin, parent)
                │   ├── cfg/get (builtin)
                │   └── cfg/set (builtin)
                ├── cmd (builtin, parent)
                │   ├── cmd/alias (builtin)
                │   ├── cmd/cp (builtin)
                │   ├── cmd/exp (builtin)
                │   ├── cmd/group (builtin)
                │   ├── cmd/http (builtin)
                │   ├── cmd/imp (builtin)
                │   ├── cmd/mod (builtin)
                │   ├── cmd/mv (builtin)
                │   ├── cmd/openapi (builtin)
                │   ├── cmd/rm (builtin)
                │   └── cmd/tree (builtin)
                ├── env (builtin, parent)
                │   ├── env/cp (builtin)
                │   ├── env/exp (builtin)
                │   ├── env/get (builtin)
                │   ├── env/imp (builtin)
                │   ├── env/ls (builtin)
                │   ├── env/mod (builtin)
                │   ├── env/mv (builtin)
                │   ├── env/new (builtin)
                │   └── env/rm (builtin)
                ├── help (builtin)
                ├── http (builtin)
                └── template (builtin, parent)
                    ├── template/cp (builtin)
                    ├── template/exp (builtin)
                    ├── template/get (builtin)
                    ├── template/help (builtin)
                    ├── template/imp (builtin)
                    ├── template/ls (builtin)
                    ├── template/mod (builtin)
                    ├── template/mv (builtin)
                    ├── template/new (builtin)
                    └── template/rm (builtin)
                """,
            ""),
        // Unknown command
        Arguments.of(
            List.of("unknown"),
            false,
            "",
            """
                *** error test-app: Cannot find command "unknown".
                """),
        // Known command
        Arguments.of(
            List.of("env"),
            true,
            """
                env
                    cp
                    exp
                    get
                    imp
                    ls
                    mod
                    mv
                    new
                    rm
                """,
            "")
    );
  }

  @Test
  void showHelp() {
    command.showHelp(context, context.out());
    assertOutput("""
            tree - Shows a tree of the available commands.

            Usage
              tree [-d|--details] [-b|--beautify] [-a|--address]
                   [<address>]

            Description
              Displays the available commands in a tree. Depending on
              the options, the entire command tree is shown or only a
              sub-tree starting with a root command. Also, there are
              some options to customize the output.

            Arguments and options
              <address>
                  The command address to start with (= the root of the
                  tree), see also section "Further Infos:" for details
                  concerning the syntax. If omitted, the tree for all
                  commands is shown.
              -a | --address
                  If specified, the complete addresses of the commands
                  are printed. Without this option just their names are
                  shown.
              -b | --beautify
                  If this option is specified, some beautfications on
                  the output are done to visualize the tree structure.
                  Without this option the tree structure is visualized
                  just be indentation.
              -d | --details
                  If specified, additional information about the
                  commands are shown, without this option just the name
                  or address.

            Further infos
              Command addresses:
              A command address uniquely identifies a command in
              test-app. It is basically a path the command, so `foo/bar`
              refers to the command `bar` that is a child command of
              `foo`. The special empty path (``) refers to the root
              command that represents the application itself. For
              details, please type `test-app help :addresses`.
            """,
        "");
  }

}
