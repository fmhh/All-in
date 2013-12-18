allin-cmd: SoapUI Project
======

### Introduction

SoapUI is an open source web service testing application for service-oriented architectures (SOA). Its functionality covers web service inspection, invoking, development, simulation and mocking, functional testing, load and compliance testing. SoapUI has been given a number of awards.
You can get SoapUI (Windows/Mac/Linux) at http://sourceforge.net/projects/soapui/

A SoapUI Project has been created that contains example requests to invoke:
* TSA Signature Request
* Organization Signature Request
* OnDemand Signature Request

### Automatic Regression Test

This SoapUI Project contains a Test Suite for Automatic Regression Test against the AIS Service.
It supports SOAP as well as RESTful (XML/JSON) interface.

##### Custom Properties:

| Property Variable | Description |
| :------------- | :------------- |
${#TestSuite#AP_ID}|ClaimedIdentity Customer ID
${#TestSuite#STATIC_ID}|ClaimedIdentity Key ID for Static Keys
${#TestSuite#ONDEMAND_QUALIFIED}|ClaimedIdentity Key ID for OnDemand Keys and enforced MID Auth
${#TestSuite#ONDEMAND_ADVANCED}|ClaimedIdentity Key ID for OnDemand Keys and optional MID Auth
${#TestSuite#MSISDN}|The Mobile Subscriber Number
${#TestSuite#SHA224}|URI For SHA-224 Algorithm
${#TestSuite#SHA256}|URI For SHA-256 Algorithm
${#TestSuite#SHA384}|URI For SHA-384 Algorithm
${#TestSuite#SHA512}|URI For SHA-512 Algorithm
${#TestSuite#DIGEST_224}|Base64 encoded binary hash (SHA-224) value of any document
${#TestSuite#DIGEST_256}|Base64 encoded binary hash (SHA-256) value of any document
${#TestSuite#DIGEST_384}|Base64 encoded binary hash (SHA-384) value of any document
${#TestSuite#DIGEST_512}|Base64 encoded binary hash (SHA-512) value of any document
${#TestSuite#tmp_ResponseID}|Temporary Variable
${#TestSuite#tmp_RequestID_RawReq}|Temporary Variable
${#TestSuite#tmp_RequestID_Resp}|Temporary Variable

### Known issues

n/a
