#!/bin/sh
# allin-ondemand.sh - 1.2
#
# Script using curl to invoke Swisscom All-in signing service: OnDemand
# Dependencies: curl, openssl, base64, sed, date, xmllint, tr, python
#
# License: GNU General Public License version 3 or later; see LICENSE.md
# Author: Swisscom (Schweiz AG)
#
# Change Log:
#  1.0 26.11.2013: Initial version
#  1.1 05.12.2013: Added support for RESTful interface
#  1.2 19.12.2013: Removed SHA224

######################################################################
# User configurable options
######################################################################

# AP_ID used to identify to Allin (provided by Swisscom)
AP_ID=cartel.ch:OnDemand-Advanced

######################################################################
# There should be no need to change anything below
######################################################################

# Error function
error()
{
  [ "$VERBOSE" = "1" -o "$DEBUG" = "1" ] && echo "$@" >&2
  exit 1
}

# Check command line
MSGTYPE=SOAP                                    # Default is SOAP
DEBUG=
VERBOSE=
while getopts "dvt:" opt; do
  case $opt in
    t) MSGTYPE=$OPTARG ;;
    d) DEBUG=1 ;;
    v) VERBOSE=1 ;; 
  esac
done

shift $((OPTIND-1))                             # Remove the options

if [ $# -lt 4 ]; then                           # Parse the rest of the arguments
  echo "Usage: $0 <args> digest method pkcs7 dn <msisdn> <msg> <lang>"
  echo "  -t value  - message type (SOAP, XML, JSON), default SOAP"
  echo "  -v        - verbose output"
  echo "  -d        - debug mode"
  echo "  digest    - digest/hash to be signed"
  echo "  method    - digest method (SHA256, SHA384, SHA512)"
  echo "  pkcs7     - output file with PKCS#7 (Crytographic Message Syntax)"
  echo "  dn        - distinguished name in the ondemand certificate"
  echo "  <msisdn>  - optional Mobile ID step-up"
  echo "  <msg>     - optional Mobile ID message"
  echo "  <lang>    - optional Mobile ID language element (en, de, fr, it)"
  echo
  echo "  Example $0 -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH'"
  echo "          $0 -v -t JSON GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH'"
  echo "          $0 -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH' +41792080350"
  echo "          $0 -v GcXfOzOP8GsBu7odeT1w3GnMedppEWvngCQ7Ef1IBMA= SHA256 result.p7s 'cn=Hans Muster,o=ACME,c=CH' +41792080350 'service.com: Sign?' en"
  echo 
  exit 1
fi

PWD=$(dirname $0)                               # Get the Path of the script

# Check the dependencies
for cmd in curl openssl base64 sed date xmllint tr python; do
  hash $cmd &> /dev/null
  if [ $? -eq 1 ]; then error "Dependency error: '$cmd' not found" ; fi
done

# Swisscom Mobile ID credentials
CERT_FILE=$PWD/mycert.crt                       # The certificate that is allowed to access the service
CERT_KEY=$PWD/mycert.key                        # The related key of the certificate

# Swisscom SDCS elements
SSL_CA=$PWD/allin-ssl.crt                       # Bag file for SSL server connection

# Create temporary request
INSTANT=$(date +%Y-%m-%dT%H:%M:%S.%N%:z)        # Define instant and request id
REQUESTID=ALLIN.TEST.$INSTANT
TMP=$(mktemp -u /tmp/_tmp.XXXXXX)                  # Request goes here
TIMEOUT_CON=90                                  # Timeout of the client connection

# Hash and digests
DIGEST_VALUE=$1                                 # Hash to be signed
DIGEST_METHOD=$2                                # Digest method
case "$DIGEST_METHOD" in
  SHA256)
    DIGEST_ALGO='http://www.w3.org/2001/04/xmlenc#sha256' ;;
  SHA384)
    DIGEST_ALGO='http://www.w3.org/2001/04/xmldsig-more#sha384' ;;
  SHA512)
    DIGEST_ALGO='http://www.w3.org/2001/04/xmlenc#sha512' ;;
  *)
    error "Unsupported digest method $DIGEST_METHOD, check with $0" ;;
esac

# Target file
PKCS7_RESULT=$3
[ -f "$PKCS7_RESULT" ] && error "Target file $PKCS7_RESULT already exists"

# OnDemand distinguished name
ONDEMAND_DN=$4

# Optional step up with Mobile ID
MID=""                                          # MobileID step up by default off
MID_MSISDN=$5                                   # MSISDN
MID_MSG=$6                                      # Optional DTBS
[ "$MID_MSG" = "" ] && MID_MSG="Sign it?"
MID_LANG=$7                                     # Optional Language
[ "$MID_LANG" = "" ] && MID_LANG="EN"
if [ "$MID_MSISDN" != "" ]; then                # MobileID step up?
  case "$MSGTYPE" in
    SOAP|XML) 
      MID="<sc:StepUpAuthorisation>
               <sc:MobileID>
                   <sc:MSISDN>$MID_MSISDN</sc:MSISDN>
                   <sc:Message>$MID_MSG</sc:Message>
                   <sc:Language>$MID_LANG</sc:Language>
               </sc:MobileID>
           </sc:StepUpAuthorisation>" ;;
    JSON) 
      MID=',"sc.StepUpAuthorisation": {
                "sc.MobileID": {
                    "sc.MSISDN": "'$MID_MSISDN'",
                    "sc.Message": "'$MID_MSG'",
                    "sc.Language": "'$MID_LANG'"
                }
            }' ;;
  esac
fi

case "$MSGTYPE" in
  # MessageType is SOAP. Define the Request
  SOAP)
    REQ_SOAP='
    <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"                  
                   xmlns:ais="http://service.ais.swisscom.com/">
        <soap:Body>
            <ais:sign>
                <SignRequest RequestID="'$REQUESTID'" Profile="urn:com:swisscom:dss:v1.0"
                             xmlns="urn:oasis:names:tc:dss:1.0:core:schema"
                             xmlns:dsig="http://www.w3.org/2000/09/xmldsig#"   
                             xmlns:sc="urn:com:swisscom:dss:1.0:schema">
                    <OptionalInputs>
                        <ClaimedIdentity Format="urn:com:swisscom:dss:v1.0:entity">
                            <Name>'$AP_ID'</Name>
                        </ClaimedIdentity>
                        <SignatureType>urn:ietf:rfc:3369</SignatureType>
                        <AdditionalProfile>urn:com:swisscom:dss:v1.0:profiles:ondemandcertificate</AdditionalProfile>
                        <sc:CertificateRequest>
                            <sc:DistinguishedName>'$ONDEMAND_DN'</sc:DistinguishedName>
                            '$MID'
                        </sc:CertificateRequest>
                        <AddTimestamp Type="urn:ietf:rfc:3161"/>
                        <sc:AddOcspResponse Type="urn:ietf:rfc:2560"/>
                    </OptionalInputs>
                    <InputDocuments>
                        <DocumentHash>
                            <dsig:DigestMethod Algorithm="'$DIGEST_ALGO'"/>
                            <dsig:DigestValue>'$DIGEST_VALUE'</dsig:DigestValue>
                        </DocumentHash>
                    </InputDocuments>
                </SignRequest>
            </ais:sign>
        </soap:Body>
    </soap:Envelope>'
    # store into file
    echo "$REQ_SOAP" > $TMP.req ;;

  # MessageType is XML. Define the Request
  XML)
    REQ_XML='
    <SignRequest RequestID="'$REQUESTID'" Profile="urn:com:swisscom:dss:v1.0" 
                 xmlns="urn:oasis:names:tc:dss:1.0:core:schema"
                 xmlns:dsig="http://www.w3.org/2000/09/xmldsig#"   
                 xmlns:sc="urn:com:swisscom:dss:1.0:schema">
        <OptionalInputs>
            <ClaimedIdentity Format="urn:com:swisscom:dss:v1.0:entity">
                <Name>'$AP_ID'</Name>
            </ClaimedIdentity>
            <SignatureType>urn:ietf:rfc:3369</SignatureType>
            <AdditionalProfile>urn:com:swisscom:dss:v1.0:profiles:ondemandcertificate</AdditionalProfile>  
            <sc:CertificateRequest>
                <sc:DistinguishedName>'$ONDEMAND_DN'</sc:DistinguishedName>
                '$MID'
            </sc:CertificateRequest>
            <AddTimestamp Type="urn:ietf:rfc:3161"/>
            <sc:AddOcspResponse Type="urn:ietf:rfc:2560"/>
        </OptionalInputs>
        <InputDocuments>
            <DocumentHash>
                <dsig:DigestMethod Algorithm="'$DIGEST_ALGO'"/>
                <dsig:DigestValue>'$DIGEST_VALUE'</dsig:DigestValue>
            </DocumentHash>
        </InputDocuments>
    </SignRequest>'
    # store into file
    echo "$REQ_XML" > $TMP.req ;;
    
  # MessageType is JSON. Define the Request
  JSON)
    REQ_JSON='{
    "dss.SignRequest": {
        "@RequestID": "'$REQUESTID'",
        "@Profile": "urn:com:swisscom:dss:v1.0",
        "dss.OptionalInputs": {
            "dss.ClaimedIdentity": {
                "@Format": "urn:com:swisscom:dss:v1.0:entity",
                "dss.Name": "'$AP_ID'"
            },
            "dss.SignatureType": "urn:ietf:rfc:3369",
            "dss.AdditionalProfile": "urn:com:swisscom:dss:v1.0:profiles:ondemandcertificate", 
            "sc.CertificateRequest": {
                "sc.DistinguishedName": "'$ONDEMAND_DN'" 
                '$MID'
            },
            "dss.AddTimestamp": {"@Type": "urn:ietf:rfc:3161"},
            "sc.AddOcspResponse": {"@Type": "urn:ietf:rfc:2560"}
        },
        "dss.InputDocuments": {"dss.DocumentHash": {
            "xmldsig.DigestMethod": {"@Algorithm": "'$DIGEST_ALGO'"},
            "xmldsig.DigestValue": "'$DIGEST_VALUE'" 
        }}
    }}'
    # store into file
    echo "$REQ_JSON" > $TMP.req ;;
    
  # Unknown message type
  *)
    error "Unsupported message type $MSGTYPE, check with $0" ;;
    
esac

# Check existence of needed files
[ -r "${SSL_CA}" ]    || error "CA certificate/chain file ($CERT_CA) missing or not readable"
[ -r "${CERT_KEY}" ]  || error "SSL key file ($CERT_KEY) missing or not readable"
[ -r "${CERT_FILE}" ] || error "SSL certificate file ($CERT_FILE) missing or not readable"

# Define cURL Options according to Message Type
case "$MSGTYPE" in
  SOAP)
    URL=https://ais.pre.swissdigicert.ch/DSS-Server/ws
    HEADER_ACCEPT="Accept: application/xml"
    HEADER_CONTENT_TYPE="Content-Type: text/xml; charset=utf-8"
    CURL_OPTIONS="--data" ;;
  XML)
    URL=https://ais.pre.swissdigicert.ch/DSS-Server/rs/v1.0/sign
    HEADER_ACCEPT="Accept: application/xml"
    HEADER_CONTENT_TYPE="Content-Type: application/xml"
    CURL_OPTIONS="--request POST --data" ;;
  JSON)
    URL=https://ais.pre.swissdigicert.ch/DSS-Server/rs/v1.0/sign
    HEADER_ACCEPT="Accept: application/json"
    HEADER_CONTENT_TYPE="Content-Type: application/json"
    CURL_OPTIONS="--request POST --data-binary" ;;
esac

# Call the service
http_code=$(curl --write-out '%{http_code}\n' --silent \
  $CURL_OPTIONS @$TMP.req \
  --header "${HEADER_ACCEPT}" --header "${HEADER_CONTENT_TYPE}" \
  --cert $CERT_FILE --cacert $SSL_CA --key $CERT_KEY \
  --output $TMP.rsp --trace-ascii $TMP.curl.log \
  --connect-timeout $TIMEOUT_CON \
  $URL)

# Results
export RC=$?

if [ "$RC" = "0" -a "$http_code" = "200" ]; then
  case "$MSGTYPE" in
    SOAP|XML)
      # SOAP/XML Parse Result
      RES_MAJ=$(sed -n -e 's/.*<ResultMajor>\(.*\)<\/ResultMajor>.*/\1/p' $TMP.rsp)
      RES_MIN=$(sed -n -e 's/.*<ResultMinor>\(.*\)<\/ResultMinor>.*/\1/p' $TMP.rsp)
      RES_MSG=$(cat $TMP.rsp | tr '\n' ' ' | sed -n -e 's/.*<ResultMessage.*>\(.*\)<\/ResultMessage>.*/\1/p')
      sed -n -e 's/.*<Base64Signature.*>\(.*\)<\/Base64Signature>.*/\1/p' $TMP.rsp > $TMP.sig.base64 ;;
    JSON)
      # JSON Parse Result
      RES_MAJ=$(sed -n -e 's/^.*"dss.ResultMajor":"\([^"]*\)".*$/\1/p' $TMP.rsp)
      RES_MIN=$(sed -n -e 's/^.*"dss.ResultMinor":"\([^"]*\)".*$/\1/p' $TMP.rsp)
      RES_MSG=$(cat $TMP.rsp | sed 's/\\\//\//g' | sed 's/\\n/ /g' | sed -n -e 's/^.*"dss.ResultMessage":{\([^}]*\)}.*$/\1/p')
      sed -n -e 's/^.*"dss.Base64Signature":{"@Type":"urn:ietf:rfc:3369","$":"\([^"]*\)".*$/\1/p' $TMP.rsp | sed 's/\\//g' > $TMP.sig.base64 ;;
  esac 

  if [ -s "${TMP}.sig.base64" ]; then
    # Decode signature if present
    base64 --decode  $TMP.sig.base64 > $TMP.sig.der
    [ -s "${TMP}.sig.der" ] || error "Unable to decode Base64Signature"
    # Save PKCS7 content to target
    openssl pkcs7 -inform der -in $TMP.sig.der -out $PKCS7_RESULT
    # Extract the signers certificate
    openssl pkcs7 -inform der -in $TMP.sig.der -out $TMP.sig.certificates.pem -print_certs
    [ -s "${TMP}.sig.certificates.pem" ] || error "Unable to extract signers certificate from signature"
    RES_ID_CERT=$(openssl x509 -subject -noout -in $TMP.sig.certificates.pem)
  fi

  # Status and results
  if [ "$RES_MAJ" = "urn:oasis:names:tc:dss:1.0:resultmajor:Success" ]; then
    RC=0                                                # Ok
    if [ "$VERBOSE" = "1" ]; then                       # Verbose details
      echo "OK on $DIGEST_VALUE with following details:"
      echo " Signer subject : $RES_ID_CERT"
      echo " Result major   : $RES_MAJ with exit $RC"
    fi
   else
    RC=1                                                # Failure
    if [ "$VERBOSE" = "1" ]; then                       # Verbose details
      echo "FAILED on $DIGEST_VALUE with following details:"
      echo " Result major   : $RES_MAJ with exit $RC"
      echo " Result minor   : $RES_MIN"
      echo " Result message : $RES_MSG"
    fi
  fi
 else
  CURL_ERR=$RC                                          # Keep related error
  export RC=2                                           # Force returned error code
  if [ "$VERBOSE" = "1" ]; then                         # Verbose details
    echo "FAILED on $DIGEST_VALUE with following details:"
    echo " curl error : $CURL_ERR"
    echo " http error : $http_code"
  fi
fi

# Debug details
if [ "$DEBUG" != "" ]; then
  [ -f "$TMP.req" ] && echo ">>> $TMP.req <<<" && cat $TMP.req | ( [ "$MSGTYPE" != "JSON" ] && xmllint --format - || python -m json.tool )
  [ -f "$TMP.curl.log" ] && echo ">>> $TMP.curl.log <<<" && cat $TMP.curl.log | grep '==\|error'
  [ -f "$TMP.rsp" ] && echo ">>> $TMP.rsp <<<" && cat $TMP.rsp | ( [ "$MSGTYPE" != "JSON" ] && xmllint --format - || python -m json.tool ) 
  echo ""
fi

# Cleanups if not DEBUG mode
if [ "$DEBUG" = "" ]; then
  [ -f "$TMP.req" ] && rm $TMP.req
  [ -f "$TMP.curl.log" ] && rm $TMP.curl.log
  [ -f "$TMP.rsp" ] && rm $TMP.rsp
  [ -f "$TMP.sig.base64" ] && rm $TMP.sig.base64
  [ -f "$TMP.sig.der" ] && rm $TMP.sig.der
  [ -f "$TMP.sig.certificates.pem" ] && rm $TMP.sig.certificates.pem
fi

exit $RC

#==========================================================
