package communication;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;

class SSL {
    private static final char[] passphrase = "123456".toCharArray();
    private static final String type = "JKS";
    private static final String algorithm = "SunX509";
    private static final String protocol = "TLS";
    private static final String trustStoreFile = "communication/truststore";
    private static final String serverKeyStoreFile = "communication/server.keys";
    private static final String clientKeyStoreFile = "communication/client.keys";

    public static SSLServerSocket generateSSLServerSocket(int serverPort) throws IOException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContext sslContext = generateSSLContext(serverKeyStoreFile, trustStoreFile);
        SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
        SSLServerSocket receivingSocket = (SSLServerSocket) ssf.createServerSocket(serverPort);
        receivingSocket.setNeedClientAuth(true);
        return receivingSocket;
    }

    public static SSLSocket generateSSLSocket(String hostName, int hostPort) throws IOException, UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        SSLContext sslContext = generateSSLContext(clientKeyStoreFile, trustStoreFile);
        SSLSocketFactory ssf = sslContext.getSocketFactory();
        SSLSocket serverConnectionSocket = (SSLSocket) ssf.createSocket(hostName, hostPort);
        serverConnectionSocket.setNeedClientAuth(true);
        return serverConnectionSocket;
    }

    private static SSLContext generateSSLContext(String keyStoreFile, String trustStoreFile) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        // First initialize the key and trust material
        KeyStore ksKeys = KeyStore.getInstance(type);
        ksKeys.load(new FileInputStream(keyStoreFile), passphrase);
        KeyStore ksTrust = KeyStore.getInstance(type);
        ksTrust.load(new FileInputStream(trustStoreFile), passphrase);

        // KeyManagers decide which key material to use
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ksKeys, passphrase);

        // TrustManagers decide whether to allow connections
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
        tmf.init(ksTrust);

        SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return sslContext;
    }
}
