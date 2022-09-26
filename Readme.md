# Apigee Base64 Encoder/Decoder

This directory contains the Java source code and pom.xml file required to build a Java callout for Apigee that
base64-encodes a message payload, or base64-decodes a message payload.

For encoding and decoding, it uses the [Base64](https://docs.oracle.com/javase/8/docs/api/java/util/Base64.html) class from Java8.  This class performs MIME-compliant Base64 encoding and decoding.
See also [RFC 2045](https://www.ietf.org/rfc/rfc2045.txt).

There are multiple ways to base64-encode and decode a thing in Apigee:

* use the base64decode static function in Message templates
* use a python callout and the base64 module
* use a JS callout and a JS polyfill base64 module

...but those ways treat the decoded thing as a _string_.  That won't work if the encoded thing
is a PDF file, an image (JPG or PNG, etc), or some other binary octet-stream that cannot be encoded as a string.


This callout does not treat the result of decoding as a string, and it does not
treat the source for encoding as a string. You can base64-decode from a base64
string into an octet-stream, and you can base64-encode from an octet-stream into
a string.

That's that main reason you may want to use this callout, in lieu of
those builtin capabilities.

Another reason is that this callout supports encoding & decoding via base64url.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## Using this policy

You do not need to build the source code in order to use the policy in Apigee.
All you need is the built JAR, and the appropriate configuration for the policy.
If you want to build it, feel free.  The instructions are at the bottom of this readme.


1. copy the jar file, available in  target/apigee-custom-base64-20210409.jar , if you have built the jar, or in [the repo](bundle/apiproxy/resources/java/apigee-custom-base64-20210409.jar) if you have not, to your apiproxy/resources/java directory. You can do this offline, or using the graphical Proxy Editor in the Apigee Admin UI.

2. include an XML file for the Java callout policy in your
   apiproxy/resources/policies directory. It should look
   like this:

   ```xml
    <JavaCallout name='Java-Base64-1'>
        ...
      <ClassName>com.google.apigee.callouts.Base64Callout</ClassName>
      <ResourceURL>java://apigee-custom-base64-20210409.jar</ResourceURL>
    </JavaCallout>
   ```

3. use the Apigee UI, or a command-line tool like
   [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js/blob/master/examples/importAndDeploy.js) or
   [apigeetool](https://github.com/apigee/apigeetool-node)
   or similar to
   import the proxy into an Apigee organization, and then deploy the proxy .
   Eg, `./importAndDeploy.js -n -v -o ${ORG} -e ${ENV} -d bundle/`

4. Use a client to generate and send http requests to the proxy you just deployed . Eg,
   ```
   # Apigee Edge
   endpoint=https://${ORG}-${ENV}.apigee.net
   # Apigee X/hybrid
   endpoint=https://your-custom-domain.apis.net
   
   curl -i -X GET -H accept-encoding:base64 \
     $endpoint/myproxy/t1
   ```

   More examples follow below.


## Notes on Usage

There is one callout class, com.google.apigee.edgecallouts.Base64Callout

It encodes the message content into Base64 format, or decodes Base64-encoded message content.

If you place it in the request flow, it will operate on the request content.
If you attach the policy to the response flow, it will operate on the response content.



## Configuring the Callout

An example for encoding:

```xml
<JavaCallout name='Java-Base64Encode'>
  <Properties>
    <Property name='action'>encode</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.Base64Callout</ClassName>
  <ResourceURL>java://apigee-custom-base64-20210409.jar</ResourceURL>
</JavaCallout>
```

The result of an encode will be like this:

```
iVBORw0KGgoAAAANSUhEUgAAAKEAAABRAQMAAACADVTsAAAABlBM....
```

A decoding example:

```xml
<JavaCallout name='Java-Base64Decode'>
  <Properties>
    <Property name='action'>decode</Property>
    <Property name='encoding'>mime</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.Base64Callout</ClassName>
  <ResourceURL>java://apigee-custom-base64-20210409.jar</ResourceURL>
</JavaCallout>
```


These are the available configuration properties:

| property name     | status    | description                                              |
| ----------------- |-----------|----------------------------------------------------------|
| `action`            | Required  | valid values: `encode`, `decode`                       |
| `encoding`          | Optional  | `mime` or `url`.  Default: none (regular base64).      |

The action determines what the Callout will do.

When decoding, the `encoding` property is required when the string is Base64url-encoded or MIME formatted (with line breaks).
When encoding, the `encoding` property is optional. If you want base64url, you need to specify 'url' here.


## Example API Proxy

You can find an example proxy bundle that uses the policy, [here in this repo](bundle/apiproxy).
The example proxy has two ways of working:

1. with a GET to /t1, retrieve an image from imgur and encode it
2. with a POST to /t2, accept a binary file and either encode or decode it

You must deploy the proxy before you can invoke it.

For case 1, invoke it like this:

```
  curl -i -X GET -H accept-encoding:base64 \
    $endpoint/base64-encoder/t1
```

This request uses a non-standard value for accept-encoding.
You will notice that the response shows Content-type:image/png, but the response also includes a "non-standard" value for the content-encoding header.
```
Content-Encoding: Base64
```

To get line breaks when encoding, pass the linelength query param:

```
  curl -i -X GET -H accept-encoding:base64 \
    $endpoint/base64-encoder/t1?encoding=mime
```

And the result looks like this:

```
iVBORw0KGgoAAAANSUhEUgAAAKEAAABRAQMAAACADVTsAAAABlBMVEUiIiL///9ehyAxAAABrElE
QVR4Xu3QL2/bQBgG8NdRlrnMNqxu1eVAahCQVAEF03STbsuBSFVZYEBBoJ2RjZ0Hljuy6IZaUlUl
pfsKRUmZP4JTNJixkEm7nJu/Mxlot0l7JJOfXj06P/D3xvkBQH/lqoEC7WVvzqM0k/f4+Gat2nt7
ppqeCjCbiJX6HmN7vnca4LLc0BljH/yZ0ZejDQXGlA9GmYSthoumVw1wZ6PByxjrpxmeZq0hbMcD
XPCHGVB4hHCAkgUKrrNSulawelP...

```

To just get un-encoded content (pass-through), invoke it like this:
```
  curl -i -X GET $endpoint/base64-encoder/t1
```

For case 2, to encode, post a binary file.  Maybe like this:

```
curl -i -X POST \
  -H content-type:application/octet-stream \
  --data-binary @/Users/someone/Downloads/Logs_512px.png  \
  $endpoint/base64-encoder/t2?action=encode
```

You will see a base64 string returned. The linelength query param also works here.


To decode, save that base64 string into a file, and invoke it like this:
```
curl -i -X POST -o output.png \
   -H content-type:application/octet-stream \
   --data-binary @/Users/someone/Downloads/Logs_512px.png.b64 \
   $endpoint/base64-encoder/t2?action=decode
```



## Building

Building from source requires Java 1.8, and Maven 3.5.

1. unpack (if you can read this, you've already done that).

2. Before building _the first time_, configure the build on your machine by loading the Apigee jars into your local cache:
  ```
  ./buildsetup.sh
  ```

3. Build with maven.
  ```
  mvn clean package
  ```
  This will build the jar and also run all the tests, and copy the jar to the resource directory in the sample apiproxy bundle.


## Runtime Dependencies

None beyond the built-in Apigee expressions v1.0 and  message-flow v1.0 jars.


## License

This material is Copyright (c) 2017-2022 Google LLC.
and is licensed under the [Apache 2.0 License](LICENSE). This includes the Java code as well as the API Proxy configuration.

## Bugs

* The code does not decode or encode in streaming mode. There is a buffer created that stores the entire byte array of the result of the encoding or decoding. For this reason you should not use this callout for extremely large payloads.
