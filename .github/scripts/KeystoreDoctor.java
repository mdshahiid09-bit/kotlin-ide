import java.io.*;
import java.security.*;
import java.util.*;

public class KeystoreDoctor {
    public static void main(String[] args) {
        String ksPath = System.getenv("KEYSTORE_FILE");
        String storePass = System.getenv("KEYSTORE_PASSWORD");
        String alias = System.getenv("KEY_ALIAS");
        String keyPass = System.getenv("KEY_PASSWORD");
        String githubEnv = System.getenv("GITHUB_ENV");

        if (ksPath == null || storePass == null) {
            System.err.println("Missing required environment variables");
            System.exit(1);
        }

        File ksFile = new File(ksPath.trim());
        storePass = storePass.trim();
        alias = alias != null ? alias.trim() : "";
        keyPass = keyPass != null ? keyPass.trim() : storePass;

        System.out.println("=== KeystoreDoctor Diagnostics ===");
        System.out.println("Keystore file: " + ksFile.getAbsolutePath() + " (size: " + ksFile.length() + " bytes)");
        System.out.println("Target alias: '" + alias + "'");

        String[] types = new String[] { "PKCS12", "JKS", KeyStore.getDefaultType() };
        KeyStore ks = null;
        String workingType = null;
        String workingStorePass = null;

        List<String> storePassCandidates = new ArrayList<>();
        storePassCandidates.add(storePass);
        if (!storePassCandidates.contains(keyPass)) {
            storePassCandidates.add(keyPass);
        }
        if (storePass.startsWith("\"") && storePass.endsWith("\"")) {
            storePassCandidates.add(storePass.substring(1, storePass.length() - 1));
        }
        if (keyPass.startsWith("\"") && keyPass.endsWith("\"")) {
            storePassCandidates.add(keyPass.substring(1, keyPass.length() - 1));
        }
        storePassCandidates.add("");

        for (String type : types) {
            for (String sp : storePassCandidates) {
                try {
                    KeyStore testKs = KeyStore.getInstance(type);
                    try (FileInputStream fis = new FileInputStream(ksFile)) {
                        testKs.load(fis, sp.toCharArray());
                        ks = testKs;
                        workingType = type;
                        workingStorePass = sp;
                        System.out.println("SUCCESS: Loaded keystore with type: " + type + " (pass candidate index: " + storePassCandidates.indexOf(sp) + ")");
                        break;
                    }
                } catch (Exception e) {
                    System.out.println("Type " + type + " failed: " + e.getClass().getName() + " - " + e.getMessage());
                    if (e.getCause() != null) {
                        System.out.println("  Cause: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
                    }
                }
            }
            if (ks != null) break;
        }

        if (ks == null) {
            System.err.println("FATAL: Could not unlock keystore with any candidate password.");
            System.exit(1);
        }

        List<String> foundAliases = new ArrayList<>();
        try {
            Enumeration<String> e = ks.aliases();
            while (e.hasMoreElements()) {
                String a = e.nextElement();
                boolean isKey = ks.isKeyEntry(a);
                foundAliases.add(a);
                System.out.println("Found entry: '" + a + "' (isKey=" + isKey + ")");
            }
        } catch (Exception e) {
            System.err.println("Error reading aliases: " + e.getMessage());
        }

        String resolvedAlias = alias;
        if (!foundAliases.contains(alias)) {
            for (String a : foundAliases) {
                if (a.equalsIgnoreCase(alias)) {
                    System.out.println("Matched alias case-insensitively: '" + a + "'");
                    resolvedAlias = a;
                    break;
                }
            }
            if (!foundAliases.contains(resolvedAlias) && !foundAliases.isEmpty()) {
                resolvedAlias = foundAliases.get(0);
                System.out.println("Falling back to first available alias: '" + resolvedAlias + "'");
            }
        }

        List<String> keyPassCandidates = new ArrayList<>();
        keyPassCandidates.add(keyPass);
        if (!keyPassCandidates.contains(workingStorePass)) {
            keyPassCandidates.add(workingStorePass);
        }
        if (keyPass.startsWith("\"") && keyPass.endsWith("\"")) {
            keyPassCandidates.add(keyPass.substring(1, keyPass.length() - 1));
        }

        String resolvedKeyPass = null;
        for (String kp : keyPassCandidates) {
            try {
                Key key = ks.getKey(resolvedAlias, kp.toCharArray());
                if (key != null) {
                    System.out.println("SUCCESS: Unlocked private key for alias '" + resolvedAlias + "'!");
                    resolvedKeyPass = kp;
                    break;
                }
            } catch (Exception e) {
                System.out.println("Key password candidate failed: " + e.getClass().getName() + " - " + e.getMessage());
                if (e.getCause() != null) {
                    System.out.println("  Cause: " + e.getCause().getClass().getName() + " - " + e.getCause().getMessage());
                }
            }
        }

        if (resolvedKeyPass == null) {
            System.err.println("FATAL: Could not unlock private key with key password candidates.");
            System.exit(1);
        }

        if (githubEnv != null) {
            try (FileWriter fw = new FileWriter(githubEnv, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                pw.println("RESOLVED_KEYSTORE_PASSWORD=" + workingStorePass);
                pw.println("RESOLVED_KEY_ALIAS=" + resolvedAlias);
                pw.println("RESOLVED_KEY_PASSWORD=" + resolvedKeyPass);
                System.out.println("Exported resolved credentials to GITHUB_ENV.");
            } catch (Exception e) {
                System.err.println("Error writing to GITHUB_ENV: " + e.getMessage());
            }
        }
    }
}
