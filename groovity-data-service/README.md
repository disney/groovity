# Groovity Data Service

The groovity-data module provides a universal factory library for storing and retrieving user-defined data types from various data sources.  The groovity-data-service module provides a universal REST api on top of the groovity-data factory; it will work with any user-defined data-types, providing CRUD via JSON and XML using standard REST semantics.  It also supports multipart form processing for simple integration with web forms, and supports dynamic include, exclude, collapse, promote and expand filters when retrieving data by GET.  It has full support for groovity-data Attachments, so it can be used to store and retrieve binary attachments to models like images and thumbnails.  It also allows use of custom security policies to authorize service requests.

* [Example Project](#example)
* [Pointers and expand](#pointer)
* [Attachments](#attach)
* [Security](#security)
* [Options](#options)

## <a name="example"></a>Example Project

Here's a working example.  Start with a new project pom; it will contain the usual groovity build plugin and the groovity-data-service dependency

```
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<groupId>com.sample</groupId>
	<artifactId>sample-app</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<packaging>war</packaging>
	<name>Sample data service</name>
	<properties>
		<groovity.version>2.0.0</groovity.version>
	</properties>
	<build>
		<plugins>
			<plugin>
				<groupId>com.disney.groovity</groupId>
				<artifactId>groovity-maven-plugin</artifactId>
				<version>${groovity.version}</version>
				<executions>
					<execution>
						<id>groovityTest</id>
						<goals>
							<goal>test</goal>
						</goals>
					</execution>
					<execution>
						<id>groovityPackage</id>
						<goals>
							<goal>package</goal>
						</goals>
					</execution>
					<execution>
						<id>default-cli</id>
						<goals>
							<goal>run</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
	<dependencies>
		<dependency>
			<groupId>com.disney.groovity</groupId>
			<artifactId>groovity-data-service</artifactId>
			<version>${groovity.version}</version>
		</dependency>
	</dependencies>

</project>

```

Then create one or more data types; for our example we will define a reusable trait first (can be anywhere, but we used `/data/traits/temperature.grvt`)

```
trait HasTemperature{
	static enum Temperature{ cold, cool, room, warm, hot }

	Temperature temperature
}
```

Then we'll define a type that uses that trait and defines a data source with default configuration

**/data/types/drink.grvt**

```
static conf=[
	source: 'file',
	'file.basedir': 'target/drinks'
]

class Drink implements DataModel, Stored, HasTemperature{
	String name
	double caffeine
	boolean opaque
}

new Drink()
```

Then we can start up our local environment

> mvn groovity:run

And begin storing and retrieving data via REST!  Assume all PUT/POST requests have these headers unless explicitly marked multipart/form-data:

```
Accept: application/json
Content-Type: application/json
```

We'll start by creating a drink:

>  POST http://localhost:9880/data/drink

```
REQUEST
{
  "name": "milk", 
  "opaque": true, 
  "temperature": "cold"
}

RESPONSE 201 Location: http://localhost:9880/data/drink/drink/57ac5477-df44-43c4-96da-77ad1e7a27af
{
    "caffeine": 0,
    "name": "milk",
    "opaque": true,
    "pointer": {
        "id": "57ac5477-df44-43c4-96da-77ad1e7a27af",
        "type": "drink"
    },
    "temperature": "cold"
}

```

We get back a 201 location header with the URL of our new drink, and the response also contains the pointer in the JSON body.  We can call the URL to validate that the response is the same as we got from our initial POST:

> GET http://localhost:9880/data/drink/57ac5477-df44-43c4-96da-77ad1e7a27af

```
RESPONSE 200 OK
{
    "caffeine": 0,
    "name": "milk",
    "opaque": true,
    "pointer": {
        "id": "57ac5477-df44-43c4-96da-77ad1e7a27af",
        "type": "drink"
    },
    "temperature": "cold"
}
```

Also check in the target directory for the `drinks` folder and the file inside - that's our data storage location, until someone bothers to configure 

```
/data/types/drink/file.basedir={someOtherValue}
```

We can update one or more fields using form-data

> POST http://localhost:9880/data/drink/57ac5477-df44-43c4-96da-77ad1e7a27af

```
REQUEST multipart/form-data
	temperature=hot
	
RESPONSE 200 OK
{
    "caffeine": 0,
    "name": "milk",
    "opaque": true,
    "pointer": {
        "id": "57ac5477-df44-43c4-96da-77ad1e7a27af",
        "type": "drink"
    },
    "temperature": "hot"
}
```

Since we define an enum for temperature, we get natural validation and error messaging if we input a bad value

> POST http://localhost:9880/data/drink/57ac5477-df44-43c4-96da-77ad1e7a27af

```
REQUEST multipart/form-data
	temperature=scalding
	
RESPONSE 400 Bad Request
{
    "message": "RuntimeException: Error setting property temperature on Drink to value type java.lang.String: IllegalArgumentException: No enum constant HasTemperature.Temperature.scalding",
    "reason": "Bad Request",
    "status": 400,
    "uri": "/data/drink/57ac5477-df44-43c4-96da-77ad1e7a27af"
}
```

We can also update the entire document or selected fields using JSON PUT or POST respectively

> POST http://localhost:9880/data/drink/57ac5477-df44-43c4-96da-77ad1e7a27af

```
REQUEST
{
	"caffeine": 0.001,
    "temperature": "cool"
}
	
RESPONSE 200 OK
{
    "caffeine": 0.001,
    "name": "milk",
    "opaque": true,
    "pointer": {
        "id": "57ac5477-df44-43c4-96da-77ad1e7a27af",
        "type": "drink"
    },
    "temperature": "cool"
}
```

And we can use filters to dynamically sculpt the data when retrieving for our needs

> GET http://localhost:9880/data/drink/57ac5477-df44-43c4-96da-77ad1e7a27af?include=name&include=temperature&include=pointer&promote=pointer.id

```
RESPONSE 200 OK
{
    "name": "milk",
    "pointer": "57ac5477-df44-43c4-96da-77ad1e7a27af",
    "temperature": "cool"
}
```

Finally we can delete with standard HTTP DELETE method

> DELETE http://localhost:9880/data/drink/57ac5477-df44-43c4-96da-77ad1e7a27af

```
RESPONSE 200 OK
```

After deleting, subsequent attempts to GET or DELETE that ID will result in a 404

> GET http://localhost:9880/data/drink/57ac5477-df44-43c4-96da-77ad1e7a27af

```
RESPONSE 404 Not Found
{
    "reason": "Not Found",
    "status": 404,
    "uri": "/data/drink/57ac5477-df44-43c4-96da-77ad1e7a27af"
}
```

## <a name="pointer"></a>Pointers and expand

Let's add the ability for a drink to call out its ingredients; we'll add a new class inside our data type script to model a relationship to 
another drink or ingredient along with a quantity to be applied in this context.

```
class DrinkPart{
	Pointer ingredient
	Integer quantity
}
```

And then we can add a field to our main Drink class

```
DrinkPart[] parts
```

Now we can create a drink that has milk in it

> POST http://localhost:9880/data/drink

```
REQUEST
{
	"name": "white russian",
	"caffeine": 0.2,
	"opaque": true,
	"temperature": "cold",
	"parts": [
		{
			"ingredient": {
				"id": "57ac5477-df44-43c4-96da-77ad1e7a27af",
				"type": "drink"
			},
			"quantity": 3
        }
    ]
}

RESPONSE 201 Location: http://localhost:9880/data/drink/81e8a694-9d11-4b3c-abf8-4990bda7803d
{
    "caffeine": 0.2,
    "name": "white russian",
    "opaque": true,
    "parts": [
        {
            "ingredient": {
                "id": "57ac5477-df44-43c4-96da-77ad1e7a27af",
                "type": "drink"
            },
            "quantity": 3
        }
    ],
    "pointer": {
        "id": "81e8a694-9d11-4b3c-abf8-4990bda7803d",
        "type": "drink"
    },
    "temperature": "cold"
}
```

A follow up GET will have identical response to what we got back from the POST; however we can use the `expand` filter to automatically dereference pointers during serialization; this saves having to make several follow up requests to dereference pointers, at the expense of a larger response.

> GET http://localhost:9880/data/drink/81e8a694-9d11-4b3c-abf8-4990bda7803d?expand=parts.ingredient

```
RESPONSE 200 OK
{
    "caffeine": 0.2,
    "name": "white russian",
    "opaque": true,
    "parts": [
        {
            "ingredient": {
                "caffeine": 0.001,
                "name": "milk",
                "opaque": true,
                "parts": null,
                "pointer": {
                    "id": "57ac5477-df44-43c4-96da-77ad1e7a27af",
                    "type": "drink"
                },
                "temperature": "cool"
            },
            "quantity": 3
        }
    ],
    "pointer": {
        "id": "81e8a694-9d11-4b3c-abf8-4990bda7803d",
        "type": "drink"
    },
    "temperature": "cold"
}
```

The expand filter is also capable of following pointers that might be moved across several data lifecycles.  The supported approach is one where each lifecycle is modeled as a separate data type whose name is composed of a common logical base name followed by an underscore followed by the lifecycle name.  For example the type `post_editing` would reflect the `editing` lifecycle stage of the logical `post` type.  If an object of type `post_editing`  has a property with a Pointer of type `image`, the expands filter will look for a type `image_editing` to satisfy the reference if the type `image` cannot be found.  This allows the logical reference to `image` to remain meaningful when the post is copied to `post_live`; for the `post_live` object the expands filter would fall back on the type `image_live` to dereference the pointer if `image` does not exist.  This way pointers cannot point across lifecycle boundaries but can travel between them, assuming IDs remain consistent across lifecycles.

## <a name="attach"></a>Attachments

Let's say we want to be able to store an image to preview each drink.  Attachments are a special form of Model that can produce an InputStream of some raw data, and carry the name, MIME type, modified date, length and md5 hash of the data with them, along with any user-supplied attributes.  Some data stores, such as the File data store, know how to store and retrieve Attachments along with DataModels.  When a model with Attachments is saved in a data store that doesn't support binary payloads, such as ElasticSearch, all the metadata of the attachment is saved and the binary payload dropped.  This makes it easy to re-use attachment traits as they degrade properly across all data sources, as long as data flows from sources that can handle binary data to those that don't.

Attachments can appear anywhere in a model including inside collections; a single DataModel can hold an unlimited number of Attachments at any depth in the model hierarchy.  However, the name of each Attachment must be unique within the DataModel than contains it; in other words, while the DataModel may be a complex nested set of objects with Attachments, the Attachment namespace is a flat set for the entire DataModel (that is one instance of a data type).  So some care should be taken in complex models in naming the attachments to avoid collisions.

To add an attachment to our model is simple, we can add a single field to the Drink class

```
	Attachment image
```

There are two ways for us to upload attachment binary data into a DataModel using the Data Service REST API.  One way is to first store the model with attachment metadata; in this case we have to specify the `name`, can specify the `contentType`, and have chosen to add a custom metadata element `copyright`.

> POST http://localhost:9880/data/drink

```
REQUEST
{
  "name": "manhattan",
  "image": {
	"contentType":"image/jpeg",
	"name":"manhattan.jpg",
	"copyright": "Yours Truly"
  }
}
RESPONSE 201 Location: http://localhost:9880/data/drink/8853741c-b538-4caf-b919-69e48f05a424
{
    "caffeine": 0,
    "image": {
        "name": "manhattan.jpg",
        "contentType": "image/jpeg",
        "length": null,
        "md5": null,
        "modified": null,
        "copyright": "Yours Truly"
    },
    "name": "manhattan",
    "opaque": false,
    "parts": null,
    "pointer": {
        "id": "8853741c-b538-4caf-b919-69e48f05a424",
        "type": "drink"
    },
    "temperature": null
}
```

Now that a "slot" for the attachment has been created, we can use the REST API to PUT the raw binary data by name

> PUT http://localhost:9880/data/drink/8853741c-b538-4caf-b919-69e48f05a424/attachments/manhattan.jpg

```
REQUEST Content-Type: image/jpeg
  RAW DATA 1234
RESPONSE
{
    "caffeine": 0,
    "image": {
        "name": "manhattan.jpg",
        "contentType": "image/jpeg",
        "length": 4,
        "md5": "mZZTXgclinu/2LEyQ1xZYg==",
        "modified": 1538086144281,
        "copyright": "Yours Truly"
    },
    "name": "manhattan",
    "opaque": false,
    "parts": null,
    "pointer": {
        "id": "8853741c-b538-4caf-b919-69e48f05a424",
        "type": "drink"
    },
    "temperature": null
}
```

Notice that our custom copyright metadata element survived along with the name, though the length, hash and modified date were automatically updated.  And we can then get the raw data back by name

> GET http://localhost:9880/data/drink/8853741c-b538-4caf-b919-69e48f05a424/attachments/manhattan.jpg

As an alternative, we can use multipart/form-data to upload attachments.  For example, we can fill out a web form to fill out several text fields and populate a file input for the image at the same time.

> POST http://localhost:9880/data/drink

```
REQUEST multipart/form-data
	image=file(lemonade-stand.jpg)
	name=lemonade
	temperature=cold
	image.copyright=Yours Truly

RESPONSE 201 Location: http://localhost:9880/data/drink/0fd6b8c6-336f-47ca-9085-3ca35cc13603
{
    "caffeine": 0,
    "image": {
        "name": "lemonade-stand.jpg",
        "contentType": "image/jpeg",
        "length": 260539,
        "md5": "J5J3+L6X8oSjB62IJQQwVw==",
        "modified": 1538086672389,
        "copyright": "Yours Truly"
    },
    "name": "lemonade",
    "opaque": false,
    "parts": null,
    "pointer": {
        "id": "0fd6b8c6-336f-47ca-9085-3ca35cc13603",
        "type": "drink"
    },
    "temperature": "cold"
}
```

Notice that we are able to target nested fields in our form by using dot syntax in the field name.  And we can retrieve the attachment by the original filename

> GET http://localhost:9880/data/drink/0fd6b8c6-336f-47ca-9085-3ca35cc13603/attachments/lemonade-stand.jpg

Let's say we want to change the image on an existing DataModel; we can either use PUT on the `/data/{type}/{id}/attachments/{filename}` as shown above, or we can use multipart POST to update just the image

> POST http://localhost:9880/data/drink/0fd6b8c6-336f-47ca-9085-3ca35cc13603

```
REQUEST multipart/form-data
	image=file(lemonade-stand-new.jpg)

RESPONSE 200 OK
{
    "caffeine": 0,
    "image": {
        "name": "lemonade-stand-new.jpg",
        "contentType": "image/jpeg",
        "length": 383266,
        "md5": "NoXwvprN1ca7X6LXXiL6wA==",
        "modified": 1538086963970,
        "copyright": "Yours Truly"
    },
    "name": "lemonade",
    "opaque": false,
    "parts": null,
    "pointer": {
        "id": "0fd6b8c6-336f-47ca-9085-3ca35cc13603",
        "type": "drink"
    },
    "temperature": "cold"
}
```

Notice in this case the name of the attachment was changed to reflect the new original filename, so to get the picture now we have to call

> GET http://localhost:9880/data/drink/0fd6b8c6-336f-47ca-9085-3ca35cc13603/attachments/lemonade-stand-new.jpg

## <a name="security"></a>Security

Groovity data service provides an extension point for plugging in a custom security policy for authentication, authorization and CORS.  The default policy is permissive, allowing open access to all methods and types from any CORS origin.

A security policy is a script that defines a web block with `auth` and `cors` components to override the behavior of data service REST endpoints.  Let's say we want to implement a policy that restricts the CORS origin and prevents DELETEs.  We have to configure the system property `groovity.data.service.policy` to point to the script we want to use as our policy.  This self-contained script registers itself as a policy during initialization and implements our authorization logic:

```
/* /samplePolicy.grvt */

//self-register this script to override groovity.data.service.policy

static init(){
	System.setProperty('groovity.data.service.policy', getClassLoader().scriptName)
}

static web=[
	// custom authentication and authorization logic, could also configure digest, signature or basic auth
	auth:{ req->
		boolean authn = true, authz = true
		String[] path = req.URI.split('/')
		if(path.length > 2 && path[1] == 'data'){
			// if policy is applied to data, check the method
			// type is available in path[2] if logic is type dependent
			if(req.method=='DELETE'){
				authz = false
			}
		}
		[authenticated: authn, authorized: authz]
	},
	cors:[
		//restrict CORS origin
		origins:['*.mydomain.com']
	]
]

```

Now when we try to access the API from a different domain in a web browser we will get blocked by CORS, and when we try to execute a `DELETE` we will get back a 403 response

> DELETE http://localhost:9880/data/drink/0fd6b8c6-336f-47ca-9085-3ca35cc13603

```
{
    "message": "You are not authorized to access this URL",
    "reason": "Forbidden",
    "status": 403,
    "uri": "/data/drink/0fd6b8c6-336f-47ca-9085-3ca35cc13603"
}
```

For more information on configuring authentication and CORS, visit the [Groovity Servlet Security page](https://github.com/disney/groovity/wiki/Servlet#security)

## <a name="options"></a>Options

Groovity data service has a default `2MB` buffer limit on dynamic API responses; this means that when retrieving data models, any response under 2 MB will have automatically have the Content-Length and ETag headers calculated, and also gets automatic support for conditional If-None-Match requests.  You can raise or lower this threshold by setting the configuration variable `groovity.data.service.buffer`.  This setting has no impact on serving Attachments; the length and hash of attachments are pre-computed and ETags and conditional requests are always generated and supported for Attachments of any size.