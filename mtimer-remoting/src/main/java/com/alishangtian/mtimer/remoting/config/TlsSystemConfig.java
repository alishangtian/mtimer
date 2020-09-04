package com.alishangtian.mtimer.remoting.config;

import com.alishangtian.mtimer.remoting.common.TlsMode;
import io.netty.handler.ssl.SslContext;

/**
 * @Desc TlsSystemConfig
 * @Time 2020/08/30
 * @Author alishangtian
 */
public class TlsSystemConfig {
    public static final String TLS_SERVER_MODE = "tls.server.mode";
    public static final String TLS_ENABLE = "tls.enable";
    public static final String TLS_CONFIG_FILE = "tls.config.file";
    public static final String TLS_TEST_MODE_ENABLE = "tls.test.mode.enable";

    public static final String TLS_SERVER_NEED_CLIENT_AUTH = "tls.server.need.client.auth";
    public static final String TLS_SERVER_KEYPATH = "tls.server.keyPath";
    public static final String TLS_SERVER_KEYPASSWORD = "tls.server.keyPassword";
    public static final String TLS_SERVER_CERTPATH = "tls.server.certPath";
    public static final String TLS_SERVER_AUTHCLIENT = "tls.server.authClient";
    public static final String TLS_SERVER_TRUSTCERTPATH = "tls.server.trustCertPath";

    public static final String TLS_CLIENT_KEYPATH = "tls.client.keyPath";
    public static final String TLS_CLIENT_KEYPASSWORD = "tls.client.keyPassword";
    public static final String TLS_CLIENT_CERTPATH = "tls.client.certPath";
    public static final String TLS_CLIENT_AUTHSERVER = "tls.client.authServer";
    public static final String TLS_CLIENT_TRUSTCERTPATH = "tls.client.trustCertPath";


    /**
     * To determine whether use SSL in client-side, include SDK client and BrokerOuterAPI
     */
    public static boolean tlsEnable = Boolean.parseBoolean(System.getProperty(TLS_ENABLE, "false"));

    /**
     * To determine whether use test mode when initialize TLS context
     */
    public static boolean tlsTestModeEnable = Boolean.parseBoolean(System.getProperty(TLS_TEST_MODE_ENABLE, "true"));

    /**
     * Indicates the state of the {@link javax.net.ssl.SSLEngine} with respect to client authentication.
     * This configuration item really only applies when building the server-side {@link SslContext},
     * and can be set to none, require or optional.
     */
    public static String tlsServerNeedClientAuth = System.getProperty(TLS_SERVER_NEED_CLIENT_AUTH, "none");
    /**
     * The store path of server-side private key
     */
    public static String tlsServerKeyPath = System.getProperty(TLS_SERVER_KEYPATH, null);

    /**
     * The  password of the server-side private key
     */
    public static String tlsServerKeyPassword = System.getProperty(TLS_SERVER_KEYPASSWORD, null);

    /**
     * The store path of server-side X.509 certificate chain in PEM format
     */
    public static String tlsServerCertPath = System.getProperty(TLS_SERVER_CERTPATH, null);

    /**
     * To determine whether verify the client endpoint's certificate strictly
     */
    public static boolean tlsServerAuthClient = Boolean.parseBoolean(System.getProperty(TLS_SERVER_AUTHCLIENT, "false"));

    /**
     * The store path of trusted certificates for verifying the client endpoint's certificate
     */
    public static String tlsServerTrustCertPath = System.getProperty(TLS_SERVER_TRUSTCERTPATH, null);

    /**
     * The store path of client-side private key
     */
    public static String tlsClientKeyPath = System.getProperty(TLS_CLIENT_KEYPATH, null);

    /**
     * The  password of the client-side private key
     */
    public static String tlsClientKeyPassword = System.getProperty(TLS_CLIENT_KEYPASSWORD, null);

    /**
     * The store path of client-side X.509 certificate chain in PEM format
     */
    public static String tlsClientCertPath = System.getProperty(TLS_CLIENT_CERTPATH, null);

    /**
     * To determine whether verify the server endpoint's certificate strictly
     */
    public static boolean tlsClientAuthServer = Boolean.parseBoolean(System.getProperty(TLS_CLIENT_AUTHSERVER, "false"));

    /**
     * The store path of trusted certificates for verifying the server endpoint's certificate
     */
    public static String tlsClientTrustCertPath = System.getProperty(TLS_CLIENT_TRUSTCERTPATH, null);

    /**
     * For server, three SSL modes are supported: disabled, permissive and enforcing.
     * For client, use {@link TlsSystemConfig#tlsEnable} to determine whether use SSL.
     * <ol>
     *     <li><strong>disabled:</strong> SSL is not supported; any incoming SSL handshake will be rejected, causing connection closed.</li>
     *     <li><strong>permissive:</strong> SSL is optional, aka, server in this mode can serve client connections with or without SSL;</li>
     *     <li><strong>enforcing:</strong> SSL is required, aka, non SSL connection will be rejected.</li>
     * </ol>
     */
    public static TlsMode tlsMode = TlsMode.parse(System.getProperty(TLS_SERVER_MODE, "permissive"));

    /**
     * A config file to store the above TLS related configurations,
     * except {@link TlsSystemConfig#tlsMode} and {@link TlsSystemConfig#tlsEnable}
     */
    public static String tlsConfigFile = System.getProperty(TLS_CONFIG_FILE, "/etc/rocketmq/tls.properties");
}
