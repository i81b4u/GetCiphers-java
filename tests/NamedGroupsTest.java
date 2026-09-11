import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public final class NamedGroupsTest {
    private NamedGroupsTest() {
    }

    public static final class TestParameters extends SSLParameters {
        private final String[] groups;
        private final boolean fail;

        TestParameters(String[] groups, boolean fail) {
            this.groups = groups;
            this.fail = fail;
        }

        // Overrides the Java 20+ method, while still compiling against Java 8.
        public String[] getNamedGroups() {
            if (fail) {
                throw new UnsupportedOperationException("Synthetic provider failure");
            }
            return groups;
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            SSLParameters.class.getMethod("getNamedGroups");
        } catch (NoSuchMethodException e) {
            assertOutput(new SSLParameters(), "Groups\n"
                    + "  Unavailable: this Java runtime lacks SSLParameters.getNamedGroups() (Java 20+).\n\n");
            return;
        }
        assertOutput(new TestParameters(new String[] {"X25519MLKEM768", "x25519", "secp256r1"}, false),
                "Groups (3)\n  X25519MLKEM768\n  x25519\n  secp256r1\n\n");
        assertOutput(new TestParameters(new String[0], false), "Groups (0)\n\n");
        assertOutput(new TestParameters(null, false),
                "Groups\n  Not reported by this provider; provider defaults apply.\n\n");
        assertOutput(new TestParameters(null, true),
                "Groups\n  Unavailable: provider could not report named groups (UnsupportedOperationException).\n\n");

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, null, null);
        assertProviderOutput(context.getSupportedSSLParameters());
        assertProviderOutput(context.getDefaultSSLParameters());
    }

    private static void assertProviderOutput(SSLParameters parameters) throws Exception {
        String[] groups = (String[]) SSLParameters.class.getMethod("getNamedGroups").invoke(parameters);
        StringBuilder expected = new StringBuilder("Groups");
        if (groups == null) {
            expected.append("\n  Not reported by this provider; provider defaults apply.\n");
        } else {
            expected.append(" (").append(groups.length).append(")\n");
            for (String group : groups) {
                expected.append("  ").append(group).append('\n');
            }
        }
        assertOutput(parameters, expected.append('\n').toString());
    }

    private static void assertOutput(SSLParameters parameters, String expected) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(bytes, true, "UTF-8");
        GetCiphers.printNamedGroups("Groups", parameters, output);
        String actual = bytes.toString("UTF-8").replace("\r\n", "\n");
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + "Actual: " + actual);
        }
    }
}
