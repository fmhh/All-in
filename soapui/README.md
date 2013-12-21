allin-cmd: SoapUI Project
======

#### Introduction

SoapUI is an open source web service testing application for service-oriented architectures (SOA). Its functionality covers web service inspection, invoking, development, simulation and mocking, functional testing, load and compliance testing. SoapUI has been given a number of awards.

A SoapUI Project has been created that contains example requests to invoke:
* TSA Signature Request
* Organization Signature Request
* OnDemand Signature Request

#### Configuration

* AP_ID: Each customer will need their own AP_ID. This identification is provided by Swisscom.
* SSL KeyStore: A certificate is required for the SSL handshake. This certificate is provided by Swisscom.

#### Instructions

* Download & Install SoapUI (Windows/Mac/Linux) from http://sourceforge.net/projects/soapui
* Checkout the complete GitHub Repository (including the `services` folder which contains the `*.wadl`/`*.wsdl` files)
* Import the `allin-soapui-project.xml` file (File > Import Project)
* Configure the SSL KeyStore (File > Preferences > SSL Settings)
* [optional] Configure the Proxy (File > Preferences > Proxy Settings)
* Configure the Test Suite Properties (Select "Automatic Regression Test Suite" > "Custom Properties")
* Double click the "Automatic Regression Test Suite" and run it


#### Regression Test Suite

This SoapUI Project contains a Test Suite for Automatic Regression Test against the AIS Service.
It supports SOAP as well as RESTful (XML/JSON) interface.

###### Test Suite Properties:

| Property | Description |
| :------------- | :------------- |
${#TestSuite#AP_ID}|Your ClaimedIdentity Customer ID (AP_ID)
${#TestSuite#STATIC_ID}|Your ClaimedIdentity Key ID for Static Keys
${#TestSuite#ONDEMAND_QUALIFIED}|Your ClaimedIdentity Key ID for OnDemand Keys and enforced MID Auth
${#TestSuite#ONDEMAND_ADVANCED}|Your ClaimedIdentity Key ID for OnDemand Keys and optional MID Auth
${#TestSuite#MSISDN}|The Mobile Subscriber Number
${#TestSuite#SHA256}|URI For SHA-256 Algorithm
${#TestSuite#SHA384}|URI For SHA-384 Algorithm
${#TestSuite#SHA512}|URI For SHA-512 Algorithm
${#TestSuite#DIGEST_256}|Base64 encoded binary hash (SHA-256) value of any document
${#TestSuite#DIGEST_384}|Base64 encoded binary hash (SHA-384) value of any document
${#TestSuite#DIGEST_512}|Base64 encoded binary hash (SHA-512) value of any document
${#TestSuite#_tmp}|This property is used for temporary session data only


