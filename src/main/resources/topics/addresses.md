**Addresses**

>

Certain entities like commands or output templates have so called "addresses" that uniquely identify them in ${applicationName}.

This topic explains the syntax of these addresses.

>

**Command Address**

A command address is intended to identify a command. The address is simply a path to this command through the command tree, each command
separated by a `/` sign. For example the `exp` command of the parent command `env` has the address
`env/get`. With `foo/bar/baz` we would address the `baz` command which is a sub-command of `bar`, which in turn is the
sub-command of `foo`.

The special empty path (so an empty string) refers to ${applicationName} itself.

>

**Output Templates**

Output template belong either to a specific command or are globally defined. The general syntax of output template addresses is

> `<name>@<command-address>`

Whereas `<name>` is the name of the output template and `<command-address>` the address of the owning command, if any.

So `foo@` refers to the global output template named `foo`, and `foo@bar/baz` to the output template named `foo` that belongs to
the command `baz` which is a sub-command of `bar`.

