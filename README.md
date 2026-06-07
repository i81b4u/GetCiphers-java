# GetCiphers-java

Small dependency-free Java utility that prints the TLS protocols and cipher suites supported by the Java runtime that runs it.

This is useful when you need to compare TLS support across JDK versions, vendors, containers, or hosts.

## Usage

Compile and run with the Java version you want to inspect:

```sh
javac GetCiphers.java
java GetCiphers
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
- Cipher suites for the requested protocol
- Supported cipher suites
- Default cipher suites
- Installed security providers

## Reading the output

`Supported` means the Java security provider knows how to handle that protocol or cipher suite.

`Default` means Java enables it for normal SSL/TLS use unless an application, JVM property, or security policy changes the settings. For most operational checks, the default lists are the most important ones.

`Cipher Suites For Requested Protocol` is a derived protocol-specific view. JSSE reports provider-wide cipher suite support from `SSLContext`, so this tool filters that list by the requested protocol family and removes SCSV signaling values that are not negotiable cipher suites.

It is normal for modern JDKs to list old protocols such as `SSLv3`, `TLSv1`, or `TLSv1.1` under supported protocols while not enabling them by default.

The security provider list helps explain platform differences. For example, Windows JDKs may include `SunMSCAPI`, which integrates with Windows cryptographic services and certificate stores.

## Notes

Older one-liners using `jrunscript` are no longer reliable because `jrunscript` was deprecated in Java 9 and removed in later JDK releases.

This source intentionally avoids build tooling and third-party dependencies so it can be copied or run directly on the Java runtime being inspected.
