#!/usr/bin/env sh

set -eu

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
build_dir=$(mktemp -d "${TMPDIR:-/tmp}/getciphers-smoketest.XXXXXX")

cleanup() {
    status=$?
    trap - 0
    rm -rf "$build_dir"
    if [ "$status" -eq 0 ]; then
        echo 'Smoke test passed.'
    else
        echo 'Smoke test failed.' >&2
    fi
    exit "$status"
}

trap cleanup 0
trap 'exit 1' HUP INT TERM

run() {
    java -cp "$build_dir" GetCiphers "$@" > "$build_dir/stdout" 2> "$build_dir/stderr"
}

expect_error() {
    status=0
    run "$@" || status=$?
    test "$status" -eq 1
    test ! -s "$build_dir/stdout"
    grep -F 'Error:' "$build_dir/stderr" > /dev/null
    grep -F 'Usage: java GetCiphers [TLS_PROTOCOL]' "$build_dir/stderr" > /dev/null
}

echo 'Compiling GetCiphers.java and discovery tests...'
javac -Xlint:all -d "$build_dir" "$script_dir/GetCiphers.java" "$script_dir/tests/KemDiscoveryTest.java" "$script_dir/tests/NamedGroupsTest.java"

echo 'Checking successful runtime inspection...'
run
test ! -s "$build_dir/stderr"
for section in 'Java Runtime' 'Supported TLS Protocols' 'Default TLS Protocols' \
    'Supported TLS Named Groups' 'Default TLS Named Groups' 'Supported Cipher Suites' 'Default Cipher Suites' 'Supported KEMs (algorithm @ provider)' 'Security Providers'; do
    grep -F "$section" "$build_dir/stdout" > /dev/null
done
if grep -F 'Cipher Suites For Requested Context' "$build_dir/stdout" > /dev/null; then
    echo 'Unexpected derived cipher compatibility section.' >&2
    exit 1
fi

echo 'Checking help output...'
for option in -h --help help; do
    run "$option"
    test ! -s "$build_dir/stderr"
    grep -F 'Usage: java GetCiphers [TLS_PROTOCOL]' "$build_dir/stdout" > /dev/null
done

echo 'Checking invalid TLS context and excess arguments...'
expect_error invalid-tls-context
grep -F 'Error: TLS protocol or context' "$build_dir/stderr" > /dev/null
expect_error TLS extra

echo 'Checking protocol-name capitalization...'
# TLSv1.2 is available on the minimum supported JDK (Java 8).
run TLSv1.2
test ! -s "$build_dir/stderr"
sed '/^  requested:/d' "$build_dir/stdout" > "$build_dir/uppercase"
run tlsv1.2
test ! -s "$build_dir/stderr"
sed '/^  requested:/d' "$build_dir/stdout" > "$build_dir/lowercase"
cmp "$build_dir/uppercase" "$build_dir/lowercase"

echo 'Checking KEM discovery, provider attribution, aliases, and empty results...'
java -cp "$build_dir" KemDiscoveryTest

echo 'Checking TLS named groups, preference order, and unavailable results...'
java -cp "$build_dir" NamedGroupsTest
