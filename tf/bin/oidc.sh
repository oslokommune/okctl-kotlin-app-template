#!/bin/bash

set -e

debug_flag='true'
out_flag=''

while getopts 'do:' flag; do
    case "${flag}" in
        d) debug_flag='true' ;;
        o) out_flag="$OPTARG" ;;
    esac
done

debug() {
    if [ "$debug_flag" = 'true' ]; then
        echo "$1"
    fi
}

WELL_KNOWN="https://token.actions.githubusercontent.com/.well-known/openid-configuration"

debug "Querying $WELL_KNOWN for jwks_uri"
JWKS_URI=$(curl -s "$WELL_KNOWN" | jq .jwks_uri)
debug "Found jwks_uri $JWKS_URI"

SERVER_NAME=$(echo "$JWKS_URI" | perl -pe 's,.*?//(.*?)/.*,\1,p')
debug "Using $SERVER_NAME as servername"

# echo -n to make openssl s_client exit instead of waiting for user input
# openssl s_client gets all certificates, we only want the last one in the chain
CERTS=$(echo -n | openssl s_client -servername "$SERVER_NAME" -showcerts -connect "$SERVER_NAME":443 2> /dev/null)

# tac prints lines in reverse, now the cert we want is at the top
# sed print everything between and including the first END CERTIFICATE / BEGIN CERTIFICATE block
# reverse output again, now our cert is in the correct line order
CERT=$(echo "$CERTS" | tac | sed -n -e '/END/,/BEGIN/p; /BEGIN/q;' | tac)

debug "Certificate found"
debug "$CERT"

# openssl x509 fingerprint the cert
# grep print everything between = and EOL
# sed remove :
# tr convert uppercase to lowercase
FINGERPRINT=$(echo "$CERT" | openssl x509 -in - -fingerprint -noout | grep -oP '=\K.*$' | sed -e 's/://g' | tr '[:upper:]' '[:lower:]')

if [ "$debug_flag" = 'true' ]; then
    echo "Fingerprint: $FINGERPRINT"
else
    if [ -z "$out_flag" ]; then
        echo "$FINGERPRINT"
    else
        echo "$FINGERPRINT" > $out_flag
    fi
fi

exit 0