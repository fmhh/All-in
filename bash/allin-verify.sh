#!/bin/sh
# allin-verify.sh
#
# Script to verify the integrity of a file with a detached PKCS#7 signature.
#
# Dependencies: openssl, base64, sed
#
# License: GNU General Public License version 3 or later; see LICENSE.md
# Author: Swisscom (Schweiz) AG

# Error function
error()
{
  [ "$VERBOSE" = "1" -o "$DEBUG" = "1" ] && echo "$@" >&2
  exit 1
}

# Check command line
DEBUG=
VERBOSE=
while getopts "dv" opt; do                      # Parse the options
  case $opt in
    d) DEBUG=1 ;;                               # Debug
    v) VERBOSE=1 ;;                             # Verbose
  esac
done
shift $((OPTIND-1))                             # Remove the options

if [ $# -lt 2 ]; then                           # Parse the rest of the arguments
  echo "Usage: $0 <options> pkcs7"
  echo "  -v         - verbose output"
  echo "  -d         - debug mode"
  echo "  file       - file to verify"
  echo "  pkcs7      - file containing the detached PKCS#7 signature"
  echo
  echo "  Example $0 -v myfile.txt myfile.p7s"
  echo
  exit 1
fi

PWD=$(dirname $0)                               # Get the Path of the script

# Check the dependencies
for cmd in openssl base64 sed; do
  hash $cmd &> /dev/null
  if [ $? -eq 1 ]; then error "Dependency error: '$cmd' not found" ; fi
done

SIG_CA=$PWD/allin-ca.crt                        # Signature CA certificate/chain file
TMP=$(mktemp -u /tmp/_tmp.XXXXXX)               # Temporary file

# File to verify and the results
FILE=$1                                         # File to verify
SIG=$2                                          # File containing the detached signature
RES_CERT_SUBJ=""                                # Certificate subject used in the signature
RES_CERT_STATUS=""                              # Certificate revocation status
RES_SIG_STATUS=""                               # Verification status of the signed digest

# Check existence of needed files
[ -r "${FILE}" ]   || error "File to verify ($FILE) missing or not readable"
[ -r "${SIG}" ]    || error "Signature file ($SIG) missing or not readable"
[ -r "${SIG_CA}" ] || error "CA certificate/chain file ($SIG_CA) missing or not readable"

# Start verification by assuming all fine
RC=0
RES_SIG_STATUS="success"

# Extract the signers certificate
openssl pkcs7 -inform pem -in $SIG -out $TMP.certificates.pem -print_certs > /dev/null 2>&1
[ -s "${TMP}.certificates.pem" ] || error "Unable to extract signers certificate from signature"
RES_CERT_SUBJ=$(openssl x509 -subject -nameopt utf8 -nameopt sep_comma_plus -noout -in $TMP.certificates.pem)
RES_CERT_ISSUER=$(openssl x509 -issuer -nameopt utf8 -nameopt sep_comma_plus -noout -in $TMP.certificates.pem)
RES_CERT_START=$(openssl x509 -startdate -noout -in $TMP.certificates.pem)
RES_CERT_END=$(openssl x509 -enddate -noout -in $TMP.certificates.pem)

# Get OCSP uri from the signers certificate and verify the revocation status
OCSP_URL=$(openssl x509 -in $TMP.certificates.pem -ocsp_uri -noout)

# Verify the revocation status over OCSP
if [ -n "$OCSP_URL" ]; then
  openssl ocsp -CAfile $SIG_CA -issuer $SIG_CA -nonce -out $TMP.certificates.check -url $OCSP_URL -cert $TMP.certificates.pem > /dev/null 2>&1
  OCSP_ERR=$?                                   # Keep related errorlevel
  if [ "$OCSP_ERR" = "0" ]; then                # Revocation check completed
    RES_CERT_STATUS=$(sed -n -e 's/.*.certificates.pem: //p' $TMP.certificates.check)
   else                                         # -> check not ok
    RES_CERT_STATUS="failed, status $OCSP_ERR"    # Details for OCSP verification
    RC=1                                          # Failure in verification
    RES_SIG_STATUS="failed"                       # Details of verification
  fi
fi

# Verify the detached signature against original file
#  -noverify: don't verify signers certificate to avoid expired certificate error for OnDemand
if [ "$DEBUG" = "" ]; then
  openssl smime -verify -inform pem -in $SIG -content $FILE -out $TMP.sig -CAfile $SIG_CA -noverify -purpose any > /dev/null 2>&1
 else
  openssl smime -verify -inform pem -in $SIG -content $FILE -out $TMP.sig -CAfile $SIG_CA -noverify -purpose any
fi
VERIFY_ERR=$?                                   # Keep the related errorlevel
if [ "$VERIFY_ERR" != "0" ]; then               # Verification error
  RC=1                                            # Failure in verification
  RES_SIG_STATUS="failed"                         # Details of verification
fi

if [ "$VERBOSE" = "1" ]; then                   # Verbose details
  echo "Signature verification of $SIG on $FILE:"
  echo " Signed by    : $RES_CERT_SUBJ"
  echo "                $RES_CERT_ISSUER"
  echo "                validity= $RES_CERT_START $RES_CERT_END"
  echo "                ocsp check= $RES_CERT_STATUS"
  echo " Verification : $RES_SIG_STATUS with exit $RC"
fi

# Cleanups if not DEBUG mode
if [ "$DEBUG" = "" ]; then
  [ -f "$TMP" ] && rm $TMP
  [ -f "$TMP.certificates.pem" ] && rm $TMP.certificates.pem
  [ -f "$TMP.certificates.check" ] && rm $TMP.certificates.check
  [ -f "$TMP.sig" ] && rm $TMP.sig
fi

exit $RC

#==========================================================
