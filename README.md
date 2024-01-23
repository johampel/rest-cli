# rest-cli

A command line utility to manage and run HTTP requests, with the focus on JSON based REST APIs.

## How to build

This is a Java 17 based [https://quarkus.io/](Quarkus) application. In order to built the final executable run:

```shell script
./mvnw package -Pnative
```

## Quick start

We assume that you have built the application via the `mvnw` command shown above and copied the resulting executable as `restcli` somehwere 
in your `PATH`.

We do our first steps by accessing the [Swagger Petshop](https://github.com/swagger-api/swagger-petstore) REST API, the OpenAPI 
Specification is available at https://petstore3.swagger.io/api/v3/openapi.json.

The main objective of this utility is to manage and run HTTP requests. In the following we import the OpenAPI specification of
the Petshop and make them available in `restcli`:

```shell script
openapi_spec=https://petstore3.swagger.io/api/v3/openapi.json
restcli cmd openapi pet --base-uri https://petstore3.swagger.io/api/v3 url:${openapi_spec}
```

The command above should have imported the OpenAPI specification and created according sub-commands in `restcli` under the name `pet`. 
When issuing the following command, you should see them: 
```shell script
restcli cmd tree pet --beautify --details
```
The output should be something like:

```
pet (custom, parent)
├── addPet (custom, http): POST https://petstore3.swagger.io/api/v3/pet
├── createUser (custom, http): POST https://petstore3.swagger.io/api/v3/user
├── createUsersWithListInput (custom, http): POST https://petstore3.swagger.io/api/v3/user/createWithList
├── deleteOrder (custom, http): DELETE https://petstore3.swagger.io/api/v3/store/order/${order_id}
├── deletePet (custom, http): DELETE https://petstore3.swagger.io/api/v3/pet/${pet_id}
├── deleteUser (custom, http): DELETE https://petstore3.swagger.io/api/v3/user/${username}
├── findPetsByStatus (custom, http): GET https://petstore3.swagger.io/api/v3/pet/findByStatus
├── findPetsByTags (custom, http): GET https://petstore3.swagger.io/api/v3/pet/findByTags
├── getInventory (custom, http): GET https://petstore3.swagger.io/api/v3/store/inventory
├── getOrderById (custom, http): GET https://petstore3.swagger.io/api/v3/store/order/${order_id}
├── getPetById (custom, http): GET https://petstore3.swagger.io/api/v3/pet/${pet_id}
├── getUserByName (custom, http): GET https://petstore3.swagger.io/api/v3/user/${username}
├── loginUser (custom, http): GET https://petstore3.swagger.io/api/v3/user/login
├── logoutUser (custom, http): GET https://petstore3.swagger.io/api/v3/user/logout
├── placeOrder (custom, http): POST https://petstore3.swagger.io/api/v3/store/order
├── updatePet (custom, http): PUT https://petstore3.swagger.io/api/v3/pet
├── updatePetWithForm (custom, http): POST https://petstore3.swagger.io/api/v3/pet/${pet_id}
├── updateUser (custom, http): PUT https://petstore3.swagger.io/api/v3/user/${username}
└── uploadFile (custom, http): POST https://petstore3.swagger.io/api/v3/pet/${pet_id}/uploadImage
```

Now you can simply call one of the commands, lile `findPetsByStatus`:

```shell script
restcli pet findPetsByStatus --status available
```
The output will be something like:
```
<ArrayList>
  <item>
    <id>60</id>
    <category>
      <id>1</id>
      <name>lion</name>
    </category>
    <name>lion</name>
    <photoUrls>
      <photoUrl>string</photoUrl>
    </photoUrls>
    <tags>
      <tag>
        <id>0</id>
        <name>string</name>
      </tag>
    </tags>
    <status>available</status>
  </item>
  ...
```

Note that in this example we access an external service and the we have no control over the pets available in the shop, so the return values
might differ when you try it.

Although the commands work pretty ok for this specific service, there are two shortcomings we want to work around:

1. The commands as they are a strongly bound to the public petshop server. What happens, if we want to host the petshop service on a different
   server, let's say on `localhost`?
2. The petshop service does not talk only XML, but also JSON. So how to achieve that the commands are able to talk
   JSON as well?

The first issue can be addressed by so-called environments. Instead of providing a hardcoded service name when we import the OpenAPI specification,
we could provide a variable for the server instead:

```shell script
restcli cmd openapi pet --base-uri '${petshop_server}' url:${openapi_spec} --replace --force
```

The options `--replace` and `--force` are required to replace our previous version of the commands. More interesting is the part 
`--base-uri '${petshop_server}'`. Please obey that `${petshop_sever}` is __not__ a shell variable, therefore it is quoted with a 
single quote.

The variable `petshop_server` can be defined in a so called environment:

```shell script
restcli env new remote -vpetshop_server=https://petstore3.swagger.io/api/v3
```

Creates an environment named `remote`, initially having the variable `petshop_server` set to the address `https://petstore3.swagger.io/api/v3`.
(You may add further variables, as well as headers and other setting in the environment by calling `restcli env mod`).

Now you are able to run the command above in the `remote` environment, for example with this line:

```shell script
restcli --environment remote pet findPetsByStatus --status available
```

If you want to be the `remote` environment to be the default one, just change the configuration:

```shell script
restcli cfg set environment=remote
```

And then the original command line works again:

```shell script
restcli pet findPetsByStatus --status available
```

The second issue is the media type of the response: By default, the commands return the content in the first media type found in the
OpenAPI specification. If the REST enpoint supports more than one media type, you may add an option to the commands to select the media type:

```shell script
restcli cmd openapi pet --base-uri '${petshop_server}' url:${openapi_spec} --replace --force --accept-option a
```

The additional `--accept-option a` instructs `restcli` to add for each command the option `-a` in case that the command supports more than
one media type for the responses. So now it is legal to call:

```shell script
restcli pet findPetsByStatus --status available -a application/json
```
The result is a JSON document. In a similar way, you may add a flag for the request body to support different media types there.

Just call `restcli help cmd openapi` to learn more.







