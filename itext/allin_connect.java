/**
 * Created:
 * 03.12.13 KW49 14:51
 * </p>
 * Last Modification:
 * 20.12.13 15:04
 * </p>
 * **********************************************************************************
 * Class to connect to a server using certificates                                  *
 * **********************************************************************************
 */

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class allin_connect {

    private boolean _debug;
    private String _url;
    private String _privateKey;
    private String _serverCert;
    private String _clientCert;
    private int _timeout;

    /**
     * Constructor
     *
     * @param url
     * @param privateKey
     * @param serverCert
     * @param clientCert
     * @param timeout    in ms
     * @param debug
     */
    public allin_connect(@Nonnull String url, @Nonnull String privateKey, @Nonnull String serverCert, @Nonnull String clientCert,
                         int timeout, boolean debug) {
        this._url = url;
        this._privateKey = privateKey;
        this._serverCert = serverCert;
        this._clientCert = clientCert;
        this._timeout = timeout;
        this._debug = debug;

        Security.addProvider(new BouncyCastleProvider());
    }

    @Nullable
    public URLConnection getConnection() {

        try {
            KeyManager[] keyManagers = createKeyManagers(_clientCert);
            TrustManager[] trustManagers = null;//createTrustManagers(_serverCert);
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

    private URLConnection createConnectionObject(@Nonnull String urlString, @Nonnull SSLSocketFactory sslSocketFactory) throws IOException {
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
        }
        return connection;
    }

    private SSLSocketFactory initItAll(@Nonnull KeyManager[] keyManagers, @Nonnull TrustManager[] trustManagers)
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, trustManagers, null);
        SSLSocketFactory socketFactory = context.getSocketFactory();
        return socketFactory;
    }

    private KeyManager[] createKeyManagers(@Nonnull String alias)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException {

        KeyManager[] managers = new KeyManager[]{new allin_connect(_url, _privateKey, _serverCert, _clientCert, _timeout,
                _debug).new AliasKeyManager(alias, _privateKey, _serverCert)};

        return managers;
    }

    @Nullable
    private TrustManager[] createTrustManagers(String alias)
            throws KeyStoreException, NoSuchAlgorithmException, IOException, java.security.cert.CertificateException {
        return null;
    }

    private class AliasKeyManager implements X509KeyManager {

        private String _alias;
        private String _privateKeyName;
        private String _serverCert;

        public AliasKeyManager(@Nonnull String alias, @Nonnull String privateKeyName,
                               @Nonnull String serverCert) {
            this._alias = alias;
            this._privateKeyName = privateKeyName;
            this._serverCert = serverCert;
        }

        public String chooseClientAlias(String[] str, Principal[] principal, Socket socket) {
            return _alias;
        }

        public String chooseServerAlias(String str, Principal[] principal, Socket socket) {
            return _serverCert;
        }

        @Nullable
        public X509Certificate[] getCertificateChain(String clientCertFilePath) {
            try {
                CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                Certificate certificate = certificateFactory.generateCertificate(new FileInputStream(clientCertFilePath));
                return new X509Certificate[]{(X509Certificate) certificate};
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public String[] getClientAliases(String str, Principal[] principal) {
            return new String[]{_alias};
        }

        @Nullable
        public PrivateKey getPrivateKey(String privateKeyPath) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(_privateKeyName));
                PEMKeyPair pemKeyPair = (PEMKeyPair) new PEMParser(br).readObject();
                PrivateKeyInfo privateKeyInfo = pemKeyPair.getPrivateKeyInfo();
                JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter();
                java.security.PrivateKey privateKey = jcaPEMKeyConverter.getPrivateKey(privateKeyInfo);
                return privateKey;
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