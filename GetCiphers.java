import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public final class GetCiphers {
    /*
     * "TLS" asks JSSE for a general-purpose TLS context. On modern JDKs
     * that context can still report old protocols as supported, while only
     * enabling secure current protocols by default.
     */
    private static final String DEFAULT_PROTOCOL = "TLS";

    private GetCiphers() {
    }

    public static void main(String[] args) {
        if (args.length > 1) {
            printError("Expected at most one TLS protocol or context argument.");
            return;
        }
        if (args.length == 1 && isHelp(args[0])) {
            printUsage();
            return;
        }

        String protocol = args.length == 1 ? args[0] : DEFAULT_PROTOCOL;

        SSLContext sslContext;
        try {
            /*
             * SSLContext is provided by JSSE. Initializing it with null arguments
             * makes Java use the default key managers, trust managers, and source
             * of randomness for this runtime.
             */
            sslContext = SSLContext.getInstance(protocol);
            sslContext.init(null, null, null);
        } catch (NoSuchAlgorithmException e) {
            printError("TLS protocol or context '" + protocol + "' is not available in this Java runtime.");
            return;
        } catch (KeyManagementException e) {
            printError("Could not initialize TLS context '" + protocol + "'.");
            return;
        }

        /*
         * Supported values are everything the provider knows how to handle.
         * Default values are what Java enables unless an application overrides
         * the SSL/TLS settings.
         */
        SSLParameters supported = sslContext.getSupportedSSLParameters();
        SSLParameters defaults = sslContext.getDefaultSSLParameters();

        printRuntimeInfo(protocol, sslContext);
        printSection("Supported TLS Protocols", supported.getProtocols());
        printSection("Default TLS Protocols", defaults.getProtocols());
        printNamedGroups("Supported TLS Named Groups", supported, System.out);
        printNamedGroups("Default TLS Named Groups", defaults, System.out);
        printSection("Supported Cipher Suites", supported.getCipherSuites());
        printSection("Default Cipher Suites", defaults.getCipherSuites());
        printSection("Supported KEMs (algorithm @ provider)", supportedKems(Security.getProviders()));
        printProviders();
    }

    private static boolean isHelp(String arg) {
        return "-h".equals(arg) || "--help".equals(arg) || "help".equalsIgnoreCase(arg);
    }

    private static void printUsage() {
        printUsage(System.out);
    }

    private static void printUsage(PrintStream output) {
        output.println("Usage: java GetCiphers [TLS_PROTOCOL]");
        output.println();
        output.println("Examples:");
        output.println("  java GetCiphers");
        output.println("  java GetCiphers TLSv1.3");
        output.println("  java GetCiphers TLSv1.2");
    }

    private static void printError(String message) {
        System.err.println("Error: " + message);
        System.err.println();
        printUsage(System.err);
        System.exit(1);
    }

    private static void printRuntimeInfo(String requestedProtocol, SSLContext sslContext) {
        System.out.println("Java Runtime");
        System.out.println("  java.version: " + System.getProperty("java.version"));
        System.out.println("  java.vendor:  " + System.getProperty("java.vendor"));
        System.out.println("  java.home:    " + System.getProperty("java.home"));
        System.out.println("  os.name:      " + System.getProperty("os.name"));
        System.out.println("  requested:    " + requestedProtocol);
        System.out.println("  provider:     " + sslContext.getProvider().getName());
        System.out.println();
    }

    private static void printSection(String title, String[] values) {
        Collection<String> sortedValues = sorted(values);
        System.out.println(title + " (" + sortedValues.size() + ")");
        for (String value : sortedValues) {
            System.out.println("  " + value);
        }
        System.out.println();
    }

    private static Collection<String> sorted(String[] values) {
        /*
         * A TreeSet gives stable alphabetical output and removes duplicates if
         * a provider ever returns the same value more than once.
         */
        Set<String> sortedValues = new TreeSet<String>();
        sortedValues.addAll(Arrays.asList(values));
        return sortedValues;
    }

    static void printNamedGroups(String title, SSLParameters parameters, PrintStream output) {
        // Reflection keeps compilation and execution compatible with Java 8.
        // Preserve the provider's preference order instead of sorting these lists.
        try {
            String[] groups = (String[]) SSLParameters.class.getMethod("getNamedGroups").invoke(parameters);
            if (groups == null) {
                output.println(title);
                output.println("  Not reported by this provider; provider defaults apply.");
            } else {
                output.println(title + " (" + groups.length + ")");
                for (String group : groups) {
                    output.println("  " + group);
                }
            }
        } catch (NoSuchMethodException e) {
            output.println(title);
            output.println("  Unavailable: this Java runtime lacks SSLParameters.getNamedGroups() (Java 20+).");
        } catch (IllegalAccessException e) {
            output.println(title);
            output.println("  Unavailable: cannot access SSLParameters.getNamedGroups().");
        } catch (InvocationTargetException e) {
            output.println(title);
            output.println("  Unavailable: provider could not report named groups ("
                    + e.getCause().getClass().getSimpleName() + ").");
        }
        output.println();
    }

    static String[] supportedKems(Provider[] providers) {
        // Inspect registrations without linking to javax.crypto.KEM (Java 21+).
        // These are JCA services, not TLS named groups or handshake guarantees.
        Set<String> kems = new TreeSet<String>();
        for (Provider provider : providers) {
            for (Provider.Service service : provider.getServices()) {
                if ("KEM".equalsIgnoreCase(service.getType())) {
                    kems.add(service.getAlgorithm() + " @ " + provider.getName());
                }
            }
        }
        return kems.toArray(new String[0]);
    }

    private static void printProviders() {
        /*
         * Providers explain where algorithms come from. Platform-specific
         * providers, such as SunMSCAPI on Windows, are useful when comparing
         * the same JDK version across operating systems.
         */
        Provider[] providers = Security.getProviders();

        System.out.println("Security Providers (" + providers.length + ")");
        for (Provider provider : providers) {
            System.out.println("  " + provider);
        }
    }
}
