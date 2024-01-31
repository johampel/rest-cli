**The internal API**

The internal API is a set of functions that can be used from within templates (see `${applicationName} help :templates` for more information). 
So the API is available with as the variable `_` for the response and request template. Typically, you write something like
`${r"${_.func(args...)}"}` in order to call the API function `func` witht the arguments `args...`. The following API functions are known:

>

`beautify(<response>)`

>Typically called as `${r"${_.beautify(_response)}"}` this function beautifies JSON or XML bodies. In case that the body
is neither XML nor JSON, this function simply returns the body as it is; otherwise it applies some formatting to the
document to have indents and linebreaks at those places that it looks more nicely.

`call([<global-options>] <command> [<args>...])`

>Invokes a command of ${applicationName}. Optionally, similar to alias commands, you may pass as
global options one of `-o | --output-parameter`, `-f | --format`, or `-t | --template` (type `${applicationName) help`
for more information about them). `<command>` and `<args>` are the command and arguments to be executed.


`sh(<args>...)`

>Invokes a shell command consisting of the given `<args>` and returns its standard output. For example, in order to 
produce a directory listing of `/` on a UNIX platform, you may call `${r"${_.call("}"ls, "/")}`.