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

package org.apache.cxf.fediz.systests.custom;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.servlet.ServletException;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.fediz.core.ClaimTypes;
import org.apache.cxf.fediz.systests.common.HTTPTestUtils;
import org.apache.cxf.fediz.tomcat.FederationAuthenticator;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.WebClient;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlPage;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

/**
 * Some tests invoking directly on the IdP and sending custom parameters
 */
public class CustomParametersTest {

    private static final String IDP_HTTPS_PORT = System.getProperty("idp.https.port");
    private static final String RP_HTTPS_PORT = System.getProperty("rp.https.port");

    private static Tomcat idpServer;
    private static Tomcat rpServer;

    @BeforeAll
    public static void init() throws Exception {
        Assertions.assertNotNull("Property 'idp.https.port' null", IDP_HTTPS_PORT);
        Assertions.assertNotNull("Property 'rp.https.port' null", RP_HTTPS_PORT);

        idpServer = startServer(true, IDP_HTTPS_PORT);
        rpServer = startServer(false, RP_HTTPS_PORT);

        WSSConfig.init();
    }

    private static Tomcat startServer(boolean idp, String port)
        throws ServletException, LifecycleException, IOException {
        Tomcat server = new Tomcat();
        server.setPort(0);
        Path targetDir = Paths.get("target").toAbsolutePath();
        server.setBaseDir(targetDir.toString());

        server.getHost().setAutoDeploy(true);
        server.getHost().setDeployOnStartup(true);

//        Connector httpsConnector = new Connector();
//        httpsConnector.setPort(Integer.parseInt(port));
//        httpsConnector.setSecure(true);
//        httpsConnector.setScheme("https");
//        httpsConnector.setProperty("sslProtocol", "TLS");
//        httpsConnector.setProperty("SSLEnabled", "true");
////        httpsConnector.setProperty("keyAlias", "mytomidpkey");
//        httpsConnector.setProperty("keystorePass", "tompass");
//        httpsConnector.setProperty("keystoreFile", "test-classes/server.jks");
//
//        server.getService().addConnector(httpsConnector);
        Connector httpsConnector = new Connector();
        httpsConnector.setPort(Integer.parseInt(port));
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
        server.getService().addConnector(httpsConnector);
        //todo Need to adjust config as per below idp switch...

        if (idp) {
            server.getHost().setAppBase("tomcat/idp/webapps");

            httpsConnector.setProperty("truststorePass", "tompass");
            httpsConnector.setProperty("truststoreFile", "test-classes/server.jks");
            httpsConnector.setProperty("clientAuth", "want");

            Path stsWebapp = targetDir.resolve(server.getHost().getAppBase()).resolve("fediz-idp-sts");
            server.addWebapp("/fediz-idp-sts", stsWebapp.toString());

            Path idpWebapp = targetDir.resolve(server.getHost().getAppBase()).resolve("fediz-idp");
            server.addWebapp("/fediz-idp", idpWebapp.toString());
        } else {
            server.getHost().setAppBase("tomcat/rp/webapps");

            httpsConnector.setProperty("clientAuth", "false");

            Path rpWebapp = targetDir.resolve(server.getHost().getAppBase()).resolve("simpleWebapp");
            Context cxt = server.addWebapp("/fedizhelloworld", rpWebapp.toString());

            // Substitute the IDP port. Necessary if running the test in eclipse where port filtering doesn't seem
            // to work
            Path fedizConfig = targetDir.resolve("tomcat").resolve("fediz_config.xml");
            try (InputStream is = CustomParametersTest.class.getResourceAsStream("/fediz_config.xml")) {
                byte[] content = new byte[is.available()];
                is.read(content);
                Files.write(fedizConfig, new String(content).replace("${idp.https.port}", IDP_HTTPS_PORT).getBytes());
            }

            FederationAuthenticator fa = new FederationAuthenticator();
            fa.setConfigFile(fedizConfig.toString());
            cxt.getPipeline().addValve(fa);
        }

        server.start();

        return server;
    }

    @AfterAll
    public static void cleanup() {
        shutdownServer(idpServer);
        shutdownServer(rpServer);
    }

    private static void shutdownServer(Tomcat server) {
        try {
            if (server != null && server.getServer() != null
                && server.getServer().getState() != LifecycleState.DESTROYED) {
                if (server.getServer().getState() != LifecycleState.STOPPED) {
                    server.stop();
                }
                server.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getIdpHttpsPort() {
        return IDP_HTTPS_PORT;
    }

    public String getRpHttpsPort() {
        return RP_HTTPS_PORT;
    }

    public String getServletContextName() {
        return "fedizhelloworld";
    }

    // Test a custom parameter that gets passed through to the STS
    @org.junit.jupiter.api.Test
    public void testCustomParameter() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        // Successful test
        WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);

        String authUrl = url + "&auth_realm="
            + URLEncoder.encode("<realm xmlns=\"http://cxf.apache.org/custom\">custom-realm</realm>", "UTF-8");
        HtmlPage idpPage = webClient.getPage(authUrl);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());

        // Parse the form to get the token (wresult)
        DomNodeList<DomElement> results = idpPage.getElementsByTagName("input");

        String wresult = null;
        for (DomElement result : results) {
            if ("wresult".equals(result.getAttributeNS(null, "name"))) {
                wresult = result.getAttributeNS(null, "value");
                break;
            }
        }

        Assertions.assertNotNull(wresult);

        webClient.close();

        // Unsuccessful test
        webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        authUrl = url + "&auth_realm="
            + URLEncoder.encode("<realm xmlns=\"http://cxf.apache.org/custom\">unknown-realm</realm>", "UTF-8");
        try {
            webClient.getPage(authUrl);
            Assertions.fail("Failure expected on a bad auth_realm value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 401);
        }

        webClient.close();
    }

    @org.junit.jupiter.api.Test
    public void testCustomParameterViaRP() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/fedizhelloworld/secure/fedservlet";
        String user = "alice";
        String password = "ecila";

        final String bodyTextContent =
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), "signinresponseform");

        Assertions.assertTrue(bodyTextContent.contains("userPrincipal=" + user), "Principal not " + user);
        Assertions.assertTrue(bodyTextContent.contains("role:Admin=false"),
                "User " + user + " does not have role Admin");
        Assertions.assertTrue(bodyTextContent.contains("role:Manager=false"),
                "User " + user + " does not have role Manager");
        Assertions.assertTrue(bodyTextContent.contains("role:User=true"),
                "User " + user + " must have role User");

        String claim = ClaimTypes.FIRSTNAME.toString();
        Assertions.assertTrue(bodyTextContent.contains(claim + "=Alice"),
                "User " + user + " claim " + claim + " is not 'Alice'");
        claim = ClaimTypes.LASTNAME.toString();
        Assertions.assertTrue(bodyTextContent.contains(claim + "=Smith"),
                "User " + user + " claim " + claim + " is not 'Smith'");
        claim = ClaimTypes.EMAILADDRESS.toString();
        Assertions.assertTrue(bodyTextContent.contains(claim + "=alice@realma.org"),
                "User " + user + " claim " + claim + " is not 'alice@realma.org'");

    }

}
