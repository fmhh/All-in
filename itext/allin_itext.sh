#!/bin/sh
# allin_itext.sh - 1.0
#
# Java launcher script that handles the environment related elements
#
# License: GNU General Public License version 3 or later; see LICENSE.md
# Author: Swisscom (Schweiz AG)
#
# Change Log:
#  1.0 20.12.2013: Initial version

# Error function
error()
{
  [ "$VERBOSE" = "1" -o "$DEBUG" = "1" ] && echo "$@" >&2
  exit 1
}

# Check command line
DEBUG=
VERBOSE=
while getopts "dv" opt; do
  case $opt in
    d) DEBUG=1 ;;
    v) VERBOSE=1 ;;
  esac
done

# Get the path of the script
PWD=$(dirname $0)

# Check the dependencies
for cmd in java; do
  hash $cmd &> /dev/null
  if [ $? -eq 1 ]; then error "Dependency error: '$cmd' not found" ; fi
done

# Java options
JAVA_OPTS="-Xmx350M"
JAVA_CP="$PWD:$PWD/bcprov-jdk15on-150.jar:$PWD/itextpdf-5.4.5.jar:$PWD/jsr305-2.0.2.jar"
JAVA_DEBUG="-Xdebug"

# Proxy options (can be http/socks; check the Java documentation)
PROXY_TYPE=
PROXY_HOST=
PROXY_PORT=

if [ "$PROXY_TYPE" = "http" ] ; then
  JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost=$PROXY_HOST -Dhttp.proxyPort=$PROXY_PORT"
fi
if [ "$PROXY_TYPE" = "socks" ] ; then
  JAVA_OPTS="$JAVA_OPTS -DsocksProxyHost=$PROXY_HOST -DsocksProxyPort=$PROXY_PORT"
fi

# Debug mode
if [ "$DEBUG" != "" ]; then
  JAVA_OPTS="$JAVA_DEBUG $JAVA_OPTS"
  set -x
fi

# Launch
java $JAVA_OPTS -cp $JAVA_CP allin_itext $1 $2 $3 $4 $5 $6 $7 $8 $9

#==========================================================
