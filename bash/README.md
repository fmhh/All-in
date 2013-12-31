allin-cmd: bash scripts
============

bash command line scripts to invoke:
* Timestamp Request (RFC3161): allin-timestamp.sh
* Signing Request (RFC5652): allin-sign.sh

### Usage

```
Usage: ./allin-timestamp.sh <options> digest method pkcs7
  -t value  - message type (SOAP, XML, JSON), default SOAP
  -v        - verbose output
  -d        - debug mode
  digest    - digest/hash to be signed
  method    - digest method (SHA256, SHA384, SHA512)
  pkcs7     - output file with PKCS#7 (Crytographic Message Syntax)

  Examples ./allin-timestamp.sh GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s
           ./allin-timestamp.sh -t JSON -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s
```

```
Usage: ./allin-sign.sh <options> digest method pkcs7 [dn] [[msisdn]] [[msg]] [[lang]]
  -t value   - message type (SOAP, XML, JSON), default SOAP
  -v         - verbose output
  -d         - debug mode
  digest     - digest/hash to be signed
  method     - digest method (SHA256, SHA384, SHA512)
  pkcs7      - output file with PKCS#7 (Crytographic Message Syntax)
  [dn]       - optional distinguished name for on-demand certificate signing
  [[msisdn]] - optional Mobile ID authentication when [dn] is present
  [[msg]]    - optional Mobile ID message when [dn] is present
  [[lang]]   - optional Mobile ID language (en, de, fr, it) when [dn] is present

  Example ./allin-sign.sh -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s
          ./allin-sign.sh -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH'
          ./allin-sign.sh -v -t JSON GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH'
          ./allin-sign.sh -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH' +41792080350
          ./allin-sign.sh -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'myserver.com: Sign?' en
```

### Configuration

The files `mycert.crt`and `mycert.key` are placeholders without any valid content. Be sure to adjust them with your client certificate content in order to connect to the Mobile ID service.

Each script contains a configuration section on the top where at least following variables are relevant:

 * AP_ID: Identification provided by Swisscom to each customer in order to use the related signature service

### How to create a digest/hash to be signed

To create the digest/hash to be signed, here some examples with openssl:
```
  openssl dgst -binary -sha256 myfile.txt | base64
  openssl dgst -binary -sha512 myfile.txt | base64
```

### Results

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

### PKCS#7 output file

<TODO>

### Known issues

**OS X 10.x: Requests always fail with MSS error 104: _Wrong SSL credentials_.**

The `curl` shipped with OS X uses their own Secure Transport engine, which broke the --cert option, see: http://curl.haxx.se/mail/archive-2013-10/0036.html

Install curl from Mac Ports `sudo port install curl` or home-brew: `brew install curl && brew link --force curl`.
