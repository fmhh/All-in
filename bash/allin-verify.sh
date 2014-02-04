#!/bin/sh
# allin-verify.sh
#
# Script to verify the integrity of a file with a detached PKCS#7 signature.
#
# Dependencies: openssl, sed, awk
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
for cmd in openssl sed awk; do
  hash $cmd &> /dev/null
  if [ $? -eq 1 ]; then error "Dependency error: '$cmd' not found" ; fi
done

SIG_CA=$PWD/allin-ca.crt                        # Signature CA certificate/chain file
TMP=$(mktemp -u /tmp/_tmp.XXXXXX)               # Temporary file

# File to verify and the results
FILE=$1                                         # File to verify
SIG=$2                                          # File containing the detached signature

# Check existence of needed files
[ -r "${FILE}" ]   || error "File to verify ($FILE) missing or not readable"
[ -r "${SIG}" ]    || error "Signature file ($SIG) missing or not readable"
[ -r "${SIG_CA}" ] || error "CA certificate/chain file ($SIG_CA) missing or not readable"

# Verify the detached signature against original file
#  -noverify: don't verify signers certificate to avoid expired certificate error for OnDemand
#  stdout and stderr to a file as the -out will not contain the details about the verification itself
openssl smime -verify -inform pem -in $SIG -content $FILE -out $TMP.sig -CAfile $SIG_CA -noverify -purpose any 1> $TMP.verify 2>&1

RC=$?                                           # Keep the related errorlevel
if [ "$RC" = "0" ]; then                        # Verification ok
  # Extract the certificates in the signature
  openssl pkcs7 -inform pem -in $SIG -out $TMP.certs.pem -print_certs > /dev/null 2>&1
  [ -s "${TMP}.certs.pem" ] || error "Unable to extract the certificates in the signature"
  # Split the certificate list into separate files
  cat $TMP.certs.pem | awk -v c=-1 '/-----BEGIN CERTIFICATE-----/{inc=1;c++}
                                    inc {print > ("TMP.certs.level" c ".pem")}
                                    /---END CERTIFICATE-----/{inc=0}'
  # Signers certificate is in level0
  [ -s "TMP.certs.level0.pem" ] || error "Unable to extract signers certificate from the list"
  RES_CERT_SUBJ=$(openssl x509 -subject -nameopt utf8 -nameopt sep_comma_plus -noout -in TMP.certs.level0.pem)
  RES_CERT_ISSUER=$(openssl x509 -issuer -nameopt utf8 -nameopt sep_comma_plus -noout -in TMP.certs.level0.pem)
  RES_CERT_START=$(openssl x509 -startdate -noout -in TMP.certs.level0.pem)
  RES_CERT_END=$(openssl x509 -enddate -noout -in TMP.certs.level0.pem)

  # Get OCSP uri from the signers certificate and verify the revocation status
  OCSP_URL=$(openssl x509 -in TMP.certs.level0.pem -ocsp_uri -noout)

  # Verify the revocation status over OCSP
  if [ -n "$OCSP_URL" ]; then
    openssl ocsp -CAfile $SIG_CA -issuer TMP.certs.level1.pem -nonce -out $TMP.certs.check -url $OCSP_URL -cert TMP.certs.level0.pem > /dev/null 2>&1
    OCSP_ERR=$?                                   # Keep related errorlevel
    if [ "$OCSP_ERR" = "0" ]; then                # Revocation check completed
      RES_CERT_STATUS=$(sed -n -e 's/.*.certs.level0.pem: //p' $TMP.certs.check)
     else                                         # -> check not ok
      RES_CERT_STATUS="error, status $OCSP_ERR"    # Details for OCSP verification
    fi
   else
    RES_CERT_STATUS="No OCSP information found in the signers certificate"
  fi

  if [ "$VERBOSE" = "1" ]; then                   # Verbose details
    echo "OK on $SIG with following details:"
    echo " Signed by    : $RES_CERT_SUBJ"
    echo "                $RES_CERT_ISSUER"
    echo "                validity= $RES_CERT_START $RES_CERT_END"
    echo " OCSP check   : $RES_CERT_STATUS"
  fi
 else                                           # -> verification failure
  if [ "$VERBOSE" = "1" ]; then                   # Verbose details
    echo "FAILED on $SIG with following details:"
    [ -f "$TMP.verify" ] && cat $TMP.verify
  fi
fi

# Cleanups if not DEBUG mode
if [ ! -n "$DEBUG" ]; then
  [ -f "$TMP" ] && rm $TMP
  [ -f "$TMP.certs.pem" ] && rm $TMP.certs.pem
  ## TODO REMOVE ALL LEVEL certs
  [ -f "$TMP.certs.check" ] && rm $TMP.certs.check
  [ -f "$TMP.sig" ] && rm $TMP.sig
  [ -f "$TMP.verify" ] && rm $TMP.verify
fi

exit $RC

#==========================================================
