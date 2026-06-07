import java.security.GeneralSecurityException;
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
     * "TLS" asks JSSE for the runtime's default TLS context. On modern JDKs
     * that context can still report old protocols as supported, while only
     * enabling secure current protocols by default.
     */
    private static final String DEFAULT_PROTOCOL = "TLS";

    private GetCiphers() {
    }

    public static void main(String[] args) throws GeneralSecurityException {
        if (args.length > 1 || (args.length == 1 && isHelp(args[0]))) {
            printUsage();
            return;
        }

        String protocol = args.length == 1 ? args[0] : DEFAULT_PROTOCOL;

        /*
         * SSLContext is provided by JSSE. Initializing it with null arguments
         * makes Java use the default key managers, trust managers, and source
         * of randomness for this runtime.
         */
        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(null, null, null);

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
        printSection("Supported Cipher Suites", supported.getCipherSuites());
        printSection("Default Cipher Suites", defaults.getCipherSuites());
        printProviders();
    }

    private static boolean isHelp(String arg) {
        return "-h".equals(arg) || "--help".equals(arg) || "help".equalsIgnoreCase(arg);
    }

    private static void printUsage() {
        System.out.println("Usage: java GetCiphers [TLS_PROTOCOL]");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java GetCiphers");
        System.out.println("  java GetCiphers TLSv1.3");
        System.out.println("  java GetCiphers TLSv1.2");
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
        System.out.println(title + " (" + values.length + ")");
        for (String value : sorted(values)) {
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
