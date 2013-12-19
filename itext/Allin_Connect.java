/**
 * Created:
 * 03.12.13 KW49 14:51
 * </p>
 * Last Modification:
 * 13.12.13 KW 50 14:21
 * </p>
 * **********************************************************************************
 * Class to connect to a server using certificates                                  *
 * **********************************************************************************
 */

import javax.annotation.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class allin_connect {

    private boolean _debug;
    private String _url;
    private String _privateKey;
    private String _serverCert;
    private String _clientCert;
    private String _keyStore;
    private String _trustStore;
    private String _keyStorePass;
    private String _trustStorePass;
    private int _timeout;

    /**
     * Constructor
     * @param url
     * @param privateKey
     * @param serverCert
     * @param clientCert
     * @param keyStoreName
     * @param trustStoreName
     * @param keyStorePass
     * @param trustStorePass
     * @param timeout in ms
     * @param debug
     */
    public allin_connect(@Nonnull String url, @Nonnull String privateKey, @Nonnull String serverCert, @Nonnull String clientCert,
                         @Nonnull String keyStoreName, @Nonnull String trustStoreName,
                         @Nonnull String keyStorePass, @Nonnull String trustStorePass, int timeout, boolean debug) {
        this._url = url;
        this._privateKey = privateKey;
        this._serverCert = serverCert;
        this._clientCert = clientCert;
        this._keyStore = keyStoreName;
        this._trustStore = trustStoreName;
        this._keyStorePass = keyStorePass;
        this._trustStorePass = trustStorePass;
        this._timeout = timeout;
        this._debug = debug;
    }

    @Nullable
    public URLConnection getConnection() {
        System.setProperty("javax.net.ssl.keyStore", _keyStore);
        System.setProperty("javax.net.ssl.trustStore", _trustStore);
        System.setProperty("javax.net.ssl.keyStorePassword", _keyStorePass);
        System.setProperty("javax.net.ssl.trustStorePassword", _trustStorePass);

        try {
            KeyManager[] keyManagers = createKeyManagers(_keyStore, _keyStorePass, _clientCert);
            TrustManager[] trustManagers = createTrustManagers(_trustStore, _trustStorePass);
            SSLSocketFactory factory = initItAll(keyManagers, trustManagers);
            URLConnection con = createConnectionObject(_url, factory);
            con.setConnectTimeout(_timeout);

            return con;

        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        return null;
    }

    private URLConnection createConnectionObject(@Nonnull String urlString,@Nonnull  SSLSocketFactory sslSocketFactory) throws IOException {
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        }
        return connection;
    }

    private SSLSocketFactory initItAll(@Nonnull KeyManager[] keyManagers,@Nonnull  TrustManager[] trustManagers)
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, trustManagers, null);
        SSLSocketFactory socketFactory = context.getSocketFactory();
        return socketFactory;
    }

    private KeyManager[] createKeyManagers(@Nonnull String keyStoreFileName,@Nonnull  String keyStorePassword,@Nonnull  String alias)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException {
        java.io.InputStream inputStream = new java.io.FileInputStream(keyStoreFileName);
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(inputStream, keyStorePassword == null ? null : keyStorePassword.toCharArray());
        if (_debug) {
            printKeystoreInfo(keyStore);
        }

        KeyManager[] managers;
        if (alias != null) {
            managers =
                    new KeyManager[]{new allin_connect(_url, _privateKey, _serverCert, _clientCert, _keyStore, _trustStore,
                            _keyStorePass, _trustStorePass, _timeout, _debug).new AliasKeyManager(keyStore, alias, keyStorePassword,
                            _privateKey, _serverCert, _clientCert)};
        } else {
            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, keyStorePassword == null ? null : keyStorePassword.toCharArray());
            managers = keyManagerFactory.getKeyManagers();
        }
        return managers;
    }

    private TrustManager[] createTrustManagers(@Nonnull String trustStoreFileName,@Nonnull  String trustStorePassword)
            throws KeyStoreException, NoSuchAlgorithmException, IOException, java.security.cert.CertificateException {
        java.io.InputStream inputStream = new java.io.FileInputStream(trustStoreFileName);
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(inputStream, trustStorePassword == null ? null : trustStorePassword.toCharArray());
        if (_debug) {
            printKeystoreInfo(trustStore);
        }
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        return trustManagerFactory.getTrustManagers();
    }

    private static void printKeystoreInfo(@Nonnull KeyStore keystore) throws KeyStoreException {
        System.out.println("Provider : " + keystore.getProvider().getName());
        System.out.println("Type : " + keystore.getType());
        System.out.println("Size : " + keystore.size());

        Enumeration en = keystore.aliases();
        while (en.hasMoreElements()) {
            System.out.println("Alias: " + en.nextElement());
        }
    }

    private class AliasKeyManager implements X509KeyManager {

        private KeyStore _ks;
        private String _alias;
        private String _password;
        private String _privateKeyName;
        private String _clientCert;
        private String _serverCert;

        public AliasKeyManager(@Nonnull KeyStore ks,@Nonnull  String alias,@Nonnull  String password,@Nonnull  String privateKeyName,
                               @Nonnull String serverCert,@Nonnull  String clientCert) {
            this._ks = ks;
            this._alias = alias;
            this._password = password;
            this._privateKeyName = privateKeyName;
            this._serverCert = serverCert;
            this._clientCert = clientCert;
        }

        @Nullable
        public String chooseClientAlias(String[] str, Principal[] principal, Socket socket) {
            return _alias;
        }

        @Nullable
        public String chooseServerAlias(String str, Principal[] principal, Socket socket) {
            return _serverCert;
        }

        @Nullable
        public X509Certificate[] getCertificateChain(String alias) {
            try {
                java.security.cert.Certificate[] certificates = this._ks.getCertificateChain(alias);
                if (certificates == null) {
                    certificates = new java.security.cert.Certificate[]{this._ks.getCertificate(_clientCert)};
                }
                X509Certificate[] x509Certificates = new X509Certificate[certificates.length];
                System.arraycopy(certificates, 0, x509Certificates, 0, certificates.length);
                return x509Certificates;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public String[] getClientAliases(String str, Principal[] principal) {
            return new String[]{_alias};
        }

        @Nullable
        public PrivateKey getPrivateKey(String alias) {
            try {
                return (PrivateKey) _ks.getKey(_privateKeyName, _password == null ? null : _password.toCharArray());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public String[] getServerAliases(String str, Principal[] principal) {
            return new String[]{_alias};
        }
    }
}