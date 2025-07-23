package com.frankies.bootcamp.utils;

import org.wildfly.security.auth.server.IdentityCredentials;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStore.CredentialSourceProtectionParameter;
import org.wildfly.security.credential.store.CredentialStore.ProtectionParameter;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.credential.store.WildFlyElytronCredentialStoreProvider;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.WildFlyElytronPasswordProvider;
import org.wildfly.security.password.interfaces.ClearPassword;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
//NOTE, used the following example to popuplate the creds into the bin folder of wildfly(Remeber to flush to save):
//https://github.com/wildfly-security-incubator/elytron-examples/blob/main/credential-store/src/main/java/org/wildfly/security/examples/CredentialStoreExample.java
public class WildflyUtils {
    private static final Provider CREDENTIAL_STORE_PROVIDER = new WildFlyElytronCredentialStoreProvider();
    private static final Provider PASSWORD_PROVIDER = new WildFlyElytronPasswordProvider();

    static {
        Security.addProvider(PASSWORD_PROVIDER);
    }

    public String giveMeAPass(String aliasToFind) throws CredentialStoreException, NoSuchAlgorithmException {
        CredentialStore store = initCredentialStore();
        ClearPassword pass = store.retrieve(aliasToFind, PasswordCredential.class).getPassword(ClearPassword.class);
        return String.valueOf(pass.getPassword());
    }

    private CredentialStore initCredentialStore() throws CredentialStoreException, NoSuchAlgorithmException {
        Password storePassword = ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, "StorePassword".toCharArray());
        ProtectionParameter protectionParameter = new CredentialSourceProtectionParameter(IdentityCredentials.NONE.withCredential(new PasswordCredential(storePassword)));
        // Get an instance of the CredentialStore
        CredentialStore credentialStore = CredentialStore.getInstance("KeyStoreCredentialStore", CREDENTIAL_STORE_PROVIDER);
        // Configure and Initialise the CredentialStore
        Map<String, String> configuration = new HashMap<>();
        configuration.put("location", "mystore.cs");
        configuration.put("create", "true");

        credentialStore.initialize(configuration, protectionParameter);

//        for (String alias : credentialStore.getAliases()) {
//            System.out.println(" - "  + alias);
//        }
        return credentialStore;
    }

    public static String escape(String s) {
        StringBuilder builder = new StringBuilder();
        boolean previousWasASpace = false;
        for( char c : s.toCharArray() ) {
            if( c == ' ' ) {
                if( previousWasASpace ) {
                    builder.append("&nbsp;");
                    previousWasASpace = false;
                    continue;
                }
                previousWasASpace = true;
            } else {
                previousWasASpace = false;
            }
            switch(c) {
//                case '<': builder.append("&lt;"); break;
//                case '>': builder.append("&gt;"); break;
                case '&': builder.append("&amp;"); break;
                case '"': builder.append("&quot;"); break;
                case '\n': builder.append("<br>"); break;
                // We need Tab support here, because we print StackTraces as HTML
                case '\t': builder.append("&nbsp; &nbsp; &nbsp;"); break;
                default:
                    if( c < 128 ) {
                        builder.append(c);
                    } else {
                        builder.append("&#").append((int)c).append(";");
                    }
            }
        }
        return builder.toString();
    }
}
