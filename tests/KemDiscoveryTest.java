import java.security.Provider;
import java.util.Arrays;
import java.util.Collections;

public final class KemDiscoveryTest {
    private KemDiscoveryTest() {
    }

    private static final class TestProvider extends Provider {
        private static final long serialVersionUID = 1L;

        // The numeric constructor preserves Java 8 compatibility.
        @SuppressWarnings("deprecation")
        TestProvider(String name, boolean withKems) {
            super(name, 1.0, "Synthetic discovery-only provider");
            putService(new Service(this, "Cipher", "NotAKem", "unused.Cipher",
                    null, null));
            if (withKems) {
                putService(new Service(this, "KEM", "Z-KEM", "unused.Kem",
                        Collections.singletonList("Z-Alias"), null));
                putService(new Service(this, "KEM", "A-KEM", "unused.Kem",
                        null, null));
            }
        }
    }

    public static void main(String[] args) {
        assertKems(new String[0], new Provider[0]);
        assertKems(new String[0], new Provider[] {new TestProvider("Empty", false)});
        assertKems(new String[] {
                "A-KEM @ First", "A-KEM @ Second", "Z-KEM @ First", "Z-KEM @ Second"
        }, new Provider[] {new TestProvider("Second", true), new TestProvider("First", true)});
    }

    private static void assertKems(String[] expected, Provider[] providers) {
        String[] actual = GetCiphers.supportedKems(providers);
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("Expected " + Arrays.toString(expected)
                    + ", got " + Arrays.toString(actual));
        }
    }
}
