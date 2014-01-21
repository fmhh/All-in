/**
 * Created:
 * 03.12.13 KW49 14:51
 * </p>
 * Last Modification:
 * 21.01.2014 09:04
 * <p/>
 * Version:
 * 1.0.0
 * </p>
 * Copyright:
 * Copyright (C) 2013. All rights reserved.
 * </p>
 * License:
 * GNU General Public License version 2 or later; see LICENSE.md
 * </p>
 * Author:
 * Swisscom (Schweiz) AG
 * **********************************************************************************
 * Class to connect to a server using certificates                                  *
 * **********************************************************************************
 */

package swisscom.com.ais.itext;

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
import java.net.*;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collections;

public class allin_connect {

    boolean _debugMode = false;
    boolean _verboseMode = false;
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
                         int timeout, boolean debug, boolean verbose) {
        this._url = url;
        this._privateKey = privateKey;
        this._serverCert = serverCert;
        this._clientCert = clientCert;
        this._timeout = timeout;
        _debugMode = debug;
        _verboseMode = verbose;

        Security.addProvider(new BouncyCastleProvider());
    }

    @Nullable
    public URLConnection getConnection() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException,
            KeyStoreException, IOException, KeyManagementException {

        KeyManager[] keyManagers = createKeyManagers(_clientCert);
        TrustManager[] trustManagers = createTrustManagers(_serverCert);
        SSLSocketFactory factory = initItAll(keyManagers, trustManagers);
        URLConnection con = createConnectionObject(_url, factory);
        con.setConnectTimeout(_timeout);

        return con;
    }

    private URLConnection createConnectionObject(@Nonnull String urlString, @Nonnull SSLSocketFactory sslSocketFactory)
            throws IOException {

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
                _debugMode, _verboseMode).new AliasKeyManager(alias, _privateKey, _serverCert, _debugMode, _verboseMode)};

        return managers;
    }

    @Nullable
    private TrustManager[] createTrustManagers(String alias)
            throws KeyStoreException, NoSuchAlgorithmException, IOException, java.security.cert.CertificateException {

        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {

            X509Certificate[] trustedIssuers = null;

            @Override
            public void checkClientTrusted(final X509Certificate[] chain, final String authType) {
                //not relevant here
            }

            @Override
            public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {

                if (chain == null || chain.length < 2) {
                    throw new CertificateException("Error when validating server certificate");
                }

                X509Certificate certToVerify = chain[0];
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                CertPath cp = cf.generateCertPath(Arrays.asList(new X509Certificate[]{certToVerify}));

                TrustAnchor trustAnchor = new TrustAnchor(chain[1], null);

                CertPathValidator cpv = null;
                try {
                    cpv = CertPathValidator.getInstance("PKIX");

                    PKIXParameters pkixParams = new PKIXParameters(Collections.singleton(trustAnchor));
                    pkixParams.setRevocationEnabled(false);

                    CertPathValidatorResult validated = cpv.validate(cp, pkixParams);

                    if (validated == null) {
                        throw new CertificateException("Error when validating server certificate");
                    }

                    trustedIssuers = chain;

                } catch (Exception e) {
                    throw new CertificateException("Error when validating server certificate");
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return trustedIssuers;
            }
        }};

        return trustAllCerts;
    }

    private class AliasKeyManager implements X509KeyManager {

        private String _alias;
        private String _privateKeyName;
        private String _serverCert;
        private boolean _debugMode;
        private boolean _verboseMode;

        public AliasKeyManager(@Nonnull String alias, @Nonnull String privateKeyName,
                               @Nonnull String serverCert, boolean debugMode, boolean verboseMode) {

            this._alias = alias;
            this._privateKeyName = privateKeyName;
            this._serverCert = serverCert;
            this._debugMode = debugMode;
            this._verboseMode = verboseMode;
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
                if (_debugMode)
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

                //if we read a X509 key we will get immediately PrivatekeyInfo if key is a RSA key it is necessary to
                //create a PEMKeyPair first
                PrivateKeyInfo privateKeyInfo = null;
                PEMParser pemParser = null;
                try {
                    pemParser = new PEMParser(br);
                    privateKeyInfo = (PrivateKeyInfo) pemParser.readObject();
                } catch (Exception e) {
                    br.close();
                    br = new BufferedReader(new FileReader(_privateKeyName));
                    pemParser = new PEMParser(br);
                    PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
                    privateKeyInfo = pemKeyPair.getPrivateKeyInfo();
                }

                pemParser.close();
                br.close();

                JcaPEMKeyConverter jcaPEMKeyConverter = new JcaPEMKeyConverter();
                java.security.PrivateKey privateKey = jcaPEMKeyConverter.getPrivateKey(privateKeyInfo);

                return privateKey;

            } catch (Exception e) {
                if (_debugMode)
                    e.printStackTrace();

                return null;
            }
        }

        public String[] getServerAliases(String str, Principal[] principal) {
            return new String[]{_alias};
        }
    }
}
