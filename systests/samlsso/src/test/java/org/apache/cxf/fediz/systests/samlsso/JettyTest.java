/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.fediz.systests.samlsso;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.fediz.systests.common.AbstractTests;

import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;

/**
 * Some tests for SAML SSO with the Jetty (11) plugin, invoking on the Fediz IdP configured for SAML SSO.
 */
public class JettyTest extends AbstractTests {

    static String idpHttpsPort;
    static String rpHttpsPort;

    private static Tomcat idpServer;

    @BeforeAll
    public static void init() throws Exception {
        idpHttpsPort = System.getProperty("idp.https.port");
        Assertions.assertNotNull("Property 'idp.https.port' null", idpHttpsPort);

        rpHttpsPort = System.getProperty("rp.jetty.https.port");
        Assertions.assertNotNull("Property 'rp.jetty.https.port' null", rpHttpsPort);

        System.out.println("idpHttpsPort: " + System.getProperty("idp.https.port"));
        System.out.println("rpHttpsPort: " + System.getProperty("rp.jetty.https.port"));
        initIdp();

        JettyUtils.initRpServer("rp-server.xml");
    }

    @AfterAll
    public static void cleanup() throws Exception {
        if (idpServer != null && idpServer.getServer() != null
            && idpServer.getServer().getState() != LifecycleState.DESTROYED) {
            if (idpServer.getServer().getState() != LifecycleState.STOPPED) {
                idpServer.stop();
            }
            idpServer.destroy();
        }

        JettyUtils.stopRpServer();
    }

    private static void initIdp() throws Exception {
        idpServer = new Tomcat();
        idpServer.setPort(0);
        final Path targetDir = Paths.get("target").toAbsolutePath();
        idpServer.setBaseDir(targetDir.toString());

        idpServer.getHost().setAppBase("tomcat/idp/webapps");
        idpServer.getHost().setAutoDeploy(true);
        idpServer.getHost().setDeployOnStartup(true);

        Connector httpsConnector = new Connector();
        httpsConnector.setPort(Integer.parseInt(idpHttpsPort));
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");
        httpsConnector.setProperty("protocol", "https");
        httpsConnector.setProperty("SSLEnabled", "true");
        httpsConnector.setProperty("throwOnFailure", "true");
        httpsConnector.setThrowOnFailure(true);

        SSLHostConfig sslHostConfig = new SSLHostConfig();
        sslHostConfig.setSslProtocol("TLSv1.2");
        sslHostConfig.setTruststorePassword("storepass");
        sslHostConfig.setTruststoreFile("test-classes/clienttrust.jks");
        sslHostConfig.setProtocols("all");
        sslHostConfig.setTruststoreType("JKS");

        SSLHostConfigCertificate sslHostConfigCertificate = new SSLHostConfigCertificate(sslHostConfig,
                SSLHostConfigCertificate.Type.RSA);
        sslHostConfigCertificate.setCertificateKeyAlias("mytomidpkey");
        sslHostConfigCertificate.setCertificateKeystorePassword("tompass");
        sslHostConfigCertificate.setCertificateKeystoreFile("test-classes/server.jks");
        sslHostConfigCertificate.setCertificateKeystoreType("JKS");

        sslHostConfig.addCertificate(sslHostConfigCertificate);
        httpsConnector.addSslHostConfig(sslHostConfig);

        idpServer.getService().addConnector(httpsConnector);

        Path stsWebapp = targetDir.resolve(idpServer.getHost().getAppBase()).resolve("fediz-idp-sts");
        idpServer.addWebapp("/fediz-idp-sts", stsWebapp.toString());

        Path idpWebapp = targetDir.resolve(idpServer.getHost().getAppBase()).resolve("fediz-idp");
        idpServer.addWebapp("/fediz-idp", idpWebapp.toString());

        idpServer.start();
    }

    @Override
    public String getIdpHttpsPort() {
        return idpHttpsPort;
    }

    @Override
    public String getRpHttpsPort() {
        return rpHttpsPort;
    }

    @Override
    public String getServletContextName() {
        return "fedizhelloworldjetty";
    }

    @Override
    protected boolean isWSFederation() {
        return false;
    }

    @Disabled("This tests is currently failing on Jetty")
    @Override
    public void testConcurrentRequests() throws Exception {
        // super.testConcurrentRequests();
    }

    @Disabled("This tests is currently failing on Jetty")
    @Override
    public void testRPLogout() throws Exception {
        //
    }

}
