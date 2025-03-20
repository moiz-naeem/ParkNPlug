package server;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.concurrent.Executors;
import java.sql.SQLException;
import javax.net.ssl.*;
import java.io.*;


public class Server {
    public static void main(String[] args) throws Exception {
        // if (args.length < 3) {
        //     System.err.println("Usage: java Server <keystorePath> <keystorePassword> <dbPath>");
        //     System.exit(1);
        // }

        String keystorePath = args[0];
        String keystorePassword = args[1];
        String dbPath = "./MessageDB.db";

        try {
            MessageDB database = MessageDB.getInstance();
            database.open(dbPath);

            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            SSLContext sslContext = createSSLContext(keystorePath, keystorePassword);


            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    SSLContext c = getSSLContext();
                    SSLParameters sslParameters = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslParameters);
                }
            });

            UserAuthenticator authenticator = new UserAuthenticator();
            server.createContext("/registration", new RegistrationHandler(authenticator));
            server.createContext("/datarecord", new MessageHandler(database,authenticator));
			server.createContext("/info", new MessageHandler(database,authenticator));
			server.createContext("/search", new SearchHandler(database,authenticator));


			server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.println("Server started at port 8001");

        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            System.exit(1);
        }
    }

    private static SSLContext createSSLContext(String keystorePath, String keystorePassword)
            throws Exception {
        char[] password = keystorePassword.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keystorePath), password);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, password);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        return ssl;
    }
}
