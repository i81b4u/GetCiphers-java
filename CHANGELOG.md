# Changelog

## 2026091101

### Added

- Supported and default TLS named-group sections using the selected context's
  SSL parameters. Lists preserve provider preference order and include hybrid
  groups when reported by the provider.
- Java 8-compatible reflective access to the Java 20 named-group API, with
  distinct output for a missing API, an unreported list, an empty list, and a
  provider failure.
- Named-group tests for hybrid names, preference order, empty and null results,
  provider failures, and comparison with the installed provider's reported lists.
- A supported KEM section listing registered algorithm/provider pairs from all
  installed Java security providers, in alphabetical order. Discovery uses the
  provider registry without requiring the Java 21 KEM API; runtimes with no KEM
  registrations display a count of zero.
- Dependency-free tests for KEM discovery, provider attribution, alias exclusion,
  sorting, and empty results.
- Smoke-test coverage for successful runtime inspection, all help options,
  incorrect argument counts, and equivalent protocol-name capitalization.
- README guidance on Java 8 or newer, KEM output, and the distinction between
  registered KEM services and TLS named groups or successful handshakes.

### Documentation

- Clarified that reported TLS named groups are not an exhaustive implementation
  inventory and that supported and default lists may be identical.
- Added an Oracle Java 27 example explaining hybrid TLS groups versus standalone
  KEM registrations, preference order, and direct source-file execution.

### Fixed

- Removed the derived `Cipher Suites For Requested Context` section, which could
  incorrectly list TLS 1.2-only suites for older protocols and produced different
  results for differently capitalized protocol names. Supported and default
  cipher lists still come directly from JSSE.
- Excess command-line arguments now produce an error and usage on standard error
  and exit with status 1. Explicit help continues to exit successfully.
- Section counts now reflect the deduplicated entries actually printed.
- Smoke tests compile into an isolated temporary directory, preserve existing
  class files, and resolve sources relative to the script so they can run from
  another working directory.
- Corrected the README's `jrunscript` history: deprecated for removal in JDK 24
  and removed in JDK 26.

### Validation

- Expanded smoke tests passed on OpenJDK 26.0.2.
- User-provided Oracle Java 27 output confirmed source-file execution and
  `X25519MLKEM768` appearing first in both reported TLS named-group lists while
  the standalone KEM registrations remained unchanged. This was a manual run,
  not a full smoke-test run on Java 27.
- Production and test sources compiled against the Java 8 API with `--release 8`;
  KEM discovery tests also passed using those class files on Java 26. Execution
  on an actual Java 8 runtime has not been tested.
- Verified invocation from another working directory, source paths containing
  spaces, and preservation of a pre-existing class file.
