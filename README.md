# GetCiphers-java

Small dependency-free Java utility that prints the TLS protocols, named groups, cipher suites, and registered key encapsulation mechanisms (KEMs) supported by the Java runtime that runs it.

This is useful when you need to compare TLS support across JDK versions, vendors, containers, or hosts.

## Usage

Requires a JDK (Java 8 or newer) to compile. Compile and run with the Java version you want to inspect:

```sh
javac GetCiphers.java
java GetCiphers
```

For example, to inspect a Java 27 installation in a sibling directory, run the source directly with that runtime:

```sh
../jdk-27/bin/java GetCiphers.java
```

By default the tool uses `SSLContext.getInstance("TLS")`. You can pass a specific TLS protocol if you want to inspect that context:

```sh
java GetCiphers TLSv1.3
java GetCiphers TLSv1.2
```

The output includes:

- Java runtime details
- Supported TLS protocols
- Default TLS protocols
- Supported and default TLS named groups (when reported by the runtime)
- Supported cipher suites
- Default cipher suites
- Registered KEM algorithms and their providers
- Installed security providers

## Reading the output

`Supported` means the Java security provider knows how to handle that protocol or cipher suite.

`Default` means Java enables it for normal SSL/TLS use unless an application, JVM property, or security policy changes the settings. For most operational checks, the default lists are the most important ones.

The protocol and cipher lists come directly from the selected `SSLContext`. A version-specific context does not necessarily restrict every reported value to that exact TLS version. The tool does not infer per-version cipher compatibility from suite names. SCSV signaling values may appear in the raw lists; they are not negotiable cipher suites.

`Supported TLS Named Groups` and `Default TLS Named Groups` show the groups reported by the selected context's supported and default SSL parameters. Unlike the alphabetically sorted cipher lists, these lists retain the provider's preference order (most preferred first). They include hybrid groups such as `X25519MLKEM768` when the provider reports them; there is no hard-coded group list or name-based filtering. Runtime configuration, including `jdk.tls.namedGroups`, can affect what is reported. The two lists may be identical. In particular, `Supported TLS Named Groups` is the list returned through the context's supported SSL parameters, not an exhaustive inventory of every group the implementation could enable through explicit configuration. A group's absence from these lists alone does not establish that the implementation lacks it. These lists do not prove that a handshake with a particular peer will succeed.

The [named-group API](https://docs.oracle.com/en/java/javase/26/docs/api/java.base/javax/net/ssl/SSLParameters.html#getNamedGroups()) was introduced in Java 20. Reflection preserves Java 8 compatibility: older runtimes show an explicit unavailable message. If a provider returns `null`, the utility says the list is not reported and provider defaults apply. An actual empty array displays `(0)`; it disables named-group negotiation for those parameters. A provider error is reported as unavailable without suppressing the rest of the report.

`Supported KEMs (algorithm @ provider)` lists registered `KEM` services from all installed security providers, sorted by algorithm and provider. The count is the number of algorithm/provider pairs; aliases are not listed separately. A count of zero means no KEM services are registered. Java's standard [KEM API was introduced in Java 21](https://openjdk.org/jeps/452), but this utility uses the older provider registry API and can still run on older JDKs.

KEM registrations do not establish which TLS named groups (including hybrid post-quantum groups) are supported or enabled. This section is independent of the requested TLS context and does not test encapsulation or TLS handshakes.

If too many arguments are supplied, or the requested protocol or context is unavailable in the selected Java runtime, the tool prints an error and usage information to standard error and exits with status 1.

It is normal for modern JDKs to list old protocols such as `SSLv3`, `TLSv1`, or `TLSv1.1` under supported protocols while not enabling them by default.

The security provider list helps explain platform differences. For example, Windows JDKs may include `SunMSCAPI`, which integrates with Windows cryptographic services and certificate stores.

## Example: hybrid key exchange on Java 27

A run with Oracle Java 27 reported `X25519MLKEM768` first in both the supported and default TLS named-group lists. Its registered KEM list still contained only:

```text
DHKEM @ SunJCE
ML-KEM @ SunJCE
ML-KEM-1024 @ SunJCE
ML-KEM-512 @ SunJCE
ML-KEM-768 @ SunJCE
```

This is expected: the hybrid TLS group combines X25519 key agreement with ML-KEM-768. It is exposed by SunJSSE as a TLS named group and need not appear as a separate SunJCE KEM registration. Its first position in the default list indicates the provider's highest preference for that context; it does not mean every connection will negotiate it.

Use the named-group sections to inspect the TLS hybrid groups reported by the context, and the KEM section to inspect registered standalone cryptographic services. Exact entries and counts depend on the JDK build, providers, and configuration; this example is an observed result, not an expected-output fixture for every Java 27 installation.

## Notes

`jrunscript` was deprecated for removal in [JDK 24](https://www.oracle.com/java/technologies/javase/24-relnote-issues.html) and removed in [JDK 26](https://docs.oracle.com/en/java/javase/26/migrate/removed-tools-components.html).

This source intentionally avoids build tooling and third-party dependencies so it can be copied or run directly on the Java runtime being inspected.

## Smoke test

Run the dependency-free smoke test with:

```sh
./smoketest.sh
```

The smoke test uses a temporary build directory and can be invoked from any working directory. It checks successful output, help and error handling, protocol-name capitalization, KEM discovery with synthetic providers (including no KEM registrations), and named-group reporting, including preference order, hybrid names, empty lists, and unavailable information.
