allin-cmd
============

All-in command line tools

## bash

Contains a script to invoke the:
* TSA Signature Request
* Organization Signature Request
* OnDemand Signature Request

```
Usage: ./allin-tsa.sh <args> digest method pkcs7
  -t value  - message type (SOAP, XML, JSON), default SOAP
  -v        - verbose output
  -d        - debug mode
  digest    - digest/hash to be signed
  method    - digest method (SHA256, SHA384, SHA512)
  pkcs7     - output file with PKCS#7 (Crytographic Message Syntax)

  Examples ./allin-tsa.sh GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s
           ./allin-tsa.sh -t JSON -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s
```

```
Usage: ./allin-org.sh <args> digest method pkcs7
  -t value  - message type (SOAP, XML, JSON), default SOAP
  -v        - verbose output
  -d        - debug mode
  digest    - digest/hash to be signed
  method    - digest method (SHA256, SHA384, SHA512)
  pkcs7     - output file with PKCS#7 (Crytographic Message Syntax)

  Examples ./allin-org.sh GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s
           ./allin-org.sh -t JSON -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s
```

```
Usage: ./allin-ondemand.sh <args> digest method pkcs7 dn <msisdn> <msg> <lang>
  -t value  - message type (SOAP, XML, JSON), default SOAP
  -v        - verbose output
  -d        - debug mode
  digest    - digest/hash to be signed
  method    - digest method (SHA256, SHA384, SHA512)
  pkcs7     - output file with PKCS#7 (Crytographic Message Syntax)
  dn        - distinguished name in the ondemand certificate
  <msisdn>  - optional Mobile ID step-up
  <msg>     - optional Mobile ID message
  <lang>    - optional Mobile ID language element (en, de, fr, it)

  Example ./allin-ondemand.sh -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH'
          ./allin-ondemand.sh -v -t JSON GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH'
          ./allin-ondemand.sh -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH' +41792080350
          ./allin-ondemand.sh -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en
```


The files `mycert.crt`and `mycert.key` are placeholders without any valid content. Be sure to adjust them with your client certificate content in order to connect to the Mobile ID service.

To create the digest/hast to be signed, here some examples with openssl:
```
  openssl dgst -binary -sha256 myfile.txt | base64
  openssl dgst -binary -sha512 myfile.txt | base64
```

Refer to the "All-In - SOAP client reference guide" document from Swisscom for more details.


Example of verbose outputs:
```
OK on GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= with following details:
 Signer subject : subject= /CN=Hans Muster/O=ACME/C=CH
 Result major   : urn:oasis:names:tc:dss:1.0:resultmajor:Success with exit 0
```

```
FAILED on GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= with following details:
 Result major   : urn:oasis:names:tc:dss:1.0:resultmajor:RequesterError with exit 1
 Result minor   : urn:com:swisscom:dss:1.0:resultminor:InsufficientData
 Result message : MSISDN
```


## iText

Contains source code and a Java command line tool to digitally sign a PDF with iText.

````
Usage: java allin-itext <args> signature pdftosign signedpdf <dn> <msisdn> <msg> <lang>
-v        - verbose output
-d        - debug mode
signature - tsa, static, ondemand
pdftosign - PDF to be signed
signedpdf - signed PDF
<dn>      - optional distinguished name in ondemand
<msisdn>  - optional Mobile ID step-up in ondemand
<msg>     - optional Mobile ID message, mandatory if msisdn is set
<lang>    - optional Mobile ID language element (en, de, fr, it), mandatory if msisdn is set

Example: java allin-itext -v tsa sample.pdf signed.pdf
         java allin-itext -v static sample.pdf signed.pdf
         java allin-itext -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH'
         java allin-itext -v ondemand sample.pdf signed.pdf 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en
```

### Dependencies

To run this java example you need to download the following files from given sources:

1: http://mvnrepository.com/artifact/com.google.code.findbugs/jsr305

Version 2.0.2 is successfully tested

2: http://sourceforge.net/projects/itext

Version 5.4.5 is successfully tested

3: http://www.bouncycastle.org/latest_releases.html

bcprov-jdk15on-150.jar is successfully tested

### Compiling

After downloading this files you need to compile the java sources (maybe you have to change the location of jar-files):

`javac -cp .:bcprov-jdk15on-150.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar allin_itext.java`

Now you can run the program: `java -cp .:bcprov-jdk15on-150.jar:itextpdf-5.4.5.jar:jsr305-2.0.2.jar allin_itext`

### Configuration

Refer to `allin_itext.cfg` configuration file for related settings.

## Known issues

**OS X 10.x: Requests always fail with MSS error 104: _Wrong SSL credentials_.**

The `curl` shipped with OS X uses their own Secure Transport engine, which broke the --cert option, see: http://curl.haxx.se/mail/archive-2013-10/0036.html

Install curl from Mac Ports `sudo port install curl` or home-brew: `brew install curl && brew link --force curl`.
