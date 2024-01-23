**Input sources**

>

_Specifying the source of data:_
>

Input data, for example for request bodies, might come from different kind of sources, like:

- from a string literal, or
- from some file stored locally on the file system, or
- from a location a URL points to, or
- from the input stream ${applicationName} reads from ("stdin"), or
- from some resource bundled in the binary of ${applicationName}

`${applicationName}` has a special notation to specify the different input sources, as the following table shows:

| Kind of source                                                           | Notation                          |
|--------------------------------------------------------------------------|-----------------------------------|
| The `stdin` of ${applicationName}                                        | `@`                               |
| A file from the local file system, e.g. `some.txt`                       | `@some.txt` or `path:some.txt`    | 
| A URL to an external resource, e.g. `http://example.com`                 | `url:http://example.com`          |
| An internal resource bundled in ${applicationName}, e.g. `some.resource` | `builtin:some.resource`           |
| A string literal, e.g. `some text`                                       | `some text` or `string:some text` |  

So the different sources are distinguished by their prefixes; in case of string literals you may omit the prefix
`string` in case the string does not start with `@`, `path:`, `url:`, or `builtin:`.

>
_Placeholder replacements:_
>

In addition, you may prefix the input source with a `%` sign to instruct `${applicationName}` to perform a replacement
of placeholders in the content of the input source. These placeholder usually come from the current environment
or the command line parameters. If you - for example have the input source `%@some.txt`, it actually reads the content
of the file `some.txt` and replaces all placeholders found in that file. 

For details about placeholder you might type `${applicationName} help :templates`



