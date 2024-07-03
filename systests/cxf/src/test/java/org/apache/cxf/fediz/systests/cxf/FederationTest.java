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

package org.apache.cxf.fediz.systests.cxf;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.fediz.systests.common.AbstractTests;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.htmlunit.CookieManager;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSubmitInput;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

/**
 * A test for WS-Federation using the CXF plugin (deployed in Tomcat).
 */
public class FederationTest extends AbstractTests {

    static String idpHttpsPort;
    static String rpHttpsPort;

    private static Tomcat idpServer;
    private static Tomcat rpServer;

    @BeforeAll
    public static void init() throws Exception {
        idpHttpsPort = System.getProperty("idp.https.port");
        Assertions.assertNotNull("Property 'idp.https.port' null", idpHttpsPort);
        rpHttpsPort = System.getProperty("rp.https.port");
        Assertions.assertNotNull("Property 'rp.https.port' null", rpHttpsPort);

        initIdp();
        initRp();
    }

    private static void initIdp() throws LifecycleException {
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

        idpServer.addWebapp("/fediz-idp", "fediz-idp");
        idpServer.addWebapp("/fediz-idp-sts", "fediz-idp-sts");


        idpServer.start();
    }

    private static void initRp() throws LifecycleException {
        rpServer = new Tomcat();
        rpServer.setPort(0);
        final Path targetDir = Paths.get("target").toAbsolutePath();
        rpServer.setBaseDir(targetDir.toString());

        rpServer.getHost().setAppBase("tomcat/rp/webapps");
        rpServer.getHost().setAutoDeploy(true);
        rpServer.getHost().setDeployOnStartup(true);

        Connector httpsConnector = new Connector();
        httpsConnector.setPort(Integer.parseInt(rpHttpsPort));
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


        rpServer.getService().addConnector(httpsConnector);

        rpServer.addWebapp("/fedizhelloworld", "cxfWebapp");
        rpServer.addWebapp("/fedizhelloworldnoreqvalidation", "cxfWebapp");

        rpServer.start();
    }

    @AfterAll
    public static void cleanup() throws Exception {
        try {
            shutdownServer(idpServer);
        } finally {
            shutdownServer(rpServer);
        }
    }

    private static void shutdownServer(Tomcat server) throws LifecycleException {
        if (server != null && server.getServer() != null
            && server.getServer().getState() != LifecycleState.DESTROYED) {
            if (server.getServer().getState() != LifecycleState.STOPPED) {
                server.stop();
            }
            server.destroy();
        }
    }

    public String getIdpHttpsPort() {
        return idpHttpsPort;
    }

    public String getRpHttpsPort() {
        return rpHttpsPort;
    }

    public String getServletContextName() {
        return "fedizhelloworld";
    }

    @org.junit.jupiter.api.Test
    public void testNoRequestValidation() throws Exception {

        String url = "https://localhost:" + getRpHttpsPort() + "/fedizhelloworldnoreqvalidation/secure/fedservlet";
        String user = "alice";
        String password = "ecila";

        // Get the initial token
        CookieManager cookieManager = new CookieManager();
        final WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());

        // Parse the form to remove the context
        DomNodeList<DomElement> results = idpPage.getElementsByTagName("input");

        for (DomElement result : results) {
            if (getContextName().equals(result.getAttributeNS(null, "name"))) {
                result.setAttributeNS(null, "value", "");
            }
        }

        // Invoke back on the RP

        final HtmlForm form = idpPage.getFormByName(getLoginFormName());
        final HtmlSubmitInput button = form.getInputByName("_eventId_submit");

        final HtmlPage rpPage = button.click();
        Assertions.assertTrue("WS Federation Systests Examples".equals(rpPage.getTitleText())
                          || "WS Federation Systests Spring Examples".equals(rpPage.getTitleText()));

        webClient.close();

    }
}
