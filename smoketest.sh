#!/usr/bin/env sh

set -eu

cleanup() {
    status=$?
    rm -f GetCiphers.class

    if [ "$status" -eq 0 ]; then
        echo 'Smoke test passed.'
    else
        echo 'Smoke test failed.' >&2
    fi

    exit "$status"
}

trap cleanup EXIT

echo 'Compiling GetCiphers.java...'
javac -Xlint:all GetCiphers.java

echo 'Checking help output...'
java GetCiphers --help | grep -F 'Usage: java GetCiphers [TLS_PROTOCOL]' > /dev/null

echo 'Checking invalid TLS context handling...'
if error_output=$(java GetCiphers invalid-tls-context 2>&1); then
    echo 'Expected an unavailable TLS context to fail.' >&2
    exit 1
fi

printf '%s\n' "$error_output" | grep -F 'Error: TLS protocol or context' > /dev/null
