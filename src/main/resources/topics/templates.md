**Templates**

>

(Output) templates are used by `${applicationName}` to format responses (and also requests). Templates for responses are managed by the 
`template` sub command, which in turn has further sub commands to create, delete, modify or examine output templates.

Whenever an HTTP request is executed, its response is passed to a template, which formats the response to generate the final output. But the
same is true for the request: whenever you create an HTTP command in `${applicationName}`, the different parts of the request, such as the URI,
header or query values are finally templates. 
 
>

Technically, templates are realized with the Freemarker library, so the syntax is the same as described there: for simple templates it is 
sufficient to know that a variable named `var` is referenced by `${r"${var}"}`; there is also a way to call methods and apply directives. 
For a complete list of features of Freemarker, please refer to its homepage  (see https://freemarker.apache.org/index.html)  

In the following section we describe the aspects specific to `${applicationName}`, especially, which variables functions are available in 
the templates and where we can use templates.

>

**Request Templates**

When talking about "request templates" we basically mean custom HTTP commands. Certain parts of the HTTP commands are templates, meaning that
the values that you specify there are not fixed but interpolated using the templating mechanism. For example, the request URL might be the 
template `${r"http://${host}/foo"}`; when executing the request, the variable reference `${r"${host}"}` is replaced with the concrete value.

The values for the variables are taken from the following places:

- From the variables defines on the command itself: When creating a HTTP command, you may define parameters and each parameter is bound to a 
  variable. When invoking the command, the parameter value is mapped to the corresponding variable
- If not found as variable in the command itself, `${applicationName}` tries to get the value from the current environment. 

Beside the variables, a template might contain also API calls. API calls are method calls to the internal API of `${applicationName}`, type
`${applicationName} help :api` to learn more about it. The API is available via the special variable `_`.

>

**Response Templates/Output Templates**

Response or output templates are managed by the `template` command and its sub-commands. An output template has the following properties:

- A description. This is shown if you type `${applicationName} template help <your-template>`
- A list of parameters, typically some flags that can be referenced within the template.
- A text block containing the template content.

Within the template content the following variables are available:

- All parameters defined by the template
- `_env` contains the settings/variables of the currently active environment.
- `_request` contains the original HTTP request. It has the following properties:
  - `method`: The HTTP method.
  - `uri`: The HTTP URI.
  - `hasBody`: A flag indicating, whether there is a request body.
  - `stringBody`: The request body formatted as plain string.
  - `jsonBody`: The request body formatted as a JSON object.
  - `xmlBody`: The request body formatted as a XML document.
  - `headers`: The request headers, formatted a map of list of strings.
- `_response` contains the original HTTP response. It has the following properties:
  - `statusCode`: The Status code. 
  - `hasBody`: A flag indicating, whether there is a response body.
  - `stringBody`: The response body formatted as plain string.
  - `jsonBody`: The response body formatted as a JSON object.
  - `xmlBody`: The response body formatted as a XML document.
  - `headers`: The response headers, formatted a map of list of strings.
