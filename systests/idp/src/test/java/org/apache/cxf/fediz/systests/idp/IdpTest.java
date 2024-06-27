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

package org.apache.cxf.fediz.systests.idp;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.UnexpectedPage;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;
import com.gargoylesoftware.htmlunit.xml.XmlPage;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.fediz.core.FederationConstants;
import org.apache.cxf.fediz.core.util.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.XMLSignature;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Some tests invoking directly on the IdP
 */
public class IdpTest {

    static String idpHttpsPort;
    static String rpHttpsPort;

    private static Tomcat idpServer;

    @BeforeAll
    public static void init() throws Exception {

        idpHttpsPort = System.getProperty("idp.https.port");
        Assertions.assertNotNull("Property 'idp.https.port' null", idpHttpsPort);
        rpHttpsPort = System.getProperty("rp.https.port");
        Assertions.assertNotNull("Property 'rp.https.port' null", rpHttpsPort);

        idpServer = startServer(idpHttpsPort);

        WSSConfig.init();
    }

    private static Tomcat startServer(String port) throws LifecycleException {
        Tomcat server = new Tomcat();
        server.setPort(0);
        final Path targetDir = Paths.get("target").toAbsolutePath();
        server.setBaseDir(targetDir.toString());

        server.getHost().setAppBase("tomcat/idp/webapps");
        server.getHost().setAutoDeploy(true);
        server.getHost().setDeployOnStartup(true);

        Connector httpsConnector = new Connector();
        httpsConnector.setPort(Integer.parseInt(port));
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");
        httpsConnector.setProperty("keyAlias", "mytomidpkey");
        httpsConnector.setProperty("keystorePass", "tompass");
        httpsConnector.setProperty("keystoreFile", "test-classes/server.jks");
        httpsConnector.setProperty("truststorePass", "tompass");
        httpsConnector.setProperty("truststoreFile", "test-classes/server.jks");
        httpsConnector.setProperty("clientAuth", "want");
        //httpsConnector.setProperty("clientAuth", "false");
        httpsConnector.setProperty("sslProtocol", "TLS");
        httpsConnector.setProperty("SSLEnabled", "true");

        server.getService().addConnector(httpsConnector);

        Path stsWebapp = targetDir.resolve(server.getHost().getAppBase()).resolve("fediz-idp-sts");
        server.addWebapp("/fediz-idp-sts", stsWebapp.toString());

        Path idpWebapp = targetDir.resolve(server.getHost().getAppBase()).resolve("fediz-idp");
        server.addWebapp("/fediz-idp", idpWebapp.toString());

        server.start();

        return server;
    }

    @AfterAll
    public static void cleanup() throws Exception {
        shutdownServer(idpServer);
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
    public void testSuccessfulInvokeOnIdP() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage = webClient.getPage(url);
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
    }

    @org.junit.jupiter.api.Test
    public void testSuccessfulSSOInvokeOnIdP() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.addRequestHeader("Authorization", "Basic "
            + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8)));

        //
        // First invocation
        //

        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage idpPage = webClient.getPage(url);
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

        //
        // Second invocation - change the credentials to make sure the session is set up correctly
        //

        webClient.removeRequestHeader("Authorization");
        webClient.addRequestHeader("Authorization", "Basic "
            + Base64.getEncoder().encodeToString(("mallory" + ":" + password).getBytes(StandardCharsets.UTF_8)));

        webClient.getOptions().setJavaScriptEnabled(false);
        idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());

        // Parse the form to get the token (wresult)
        results = idpPage.getElementsByTagName("input");

        wresult = null;
        for (DomElement result : results) {
            if ("wresult".equals(result.getAttributeNS(null, "name"))) {
                wresult = result.getAttributeNS(null, "value");
                break;
            }
        }

        Assertions.assertNotNull(wresult);

        webClient.close();
    }

    @Test
    public void testIdPMetadata() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort()
            + "/fediz-idp/FederationMetadata/2007-06/FederationMetadata.xml";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setSSLClientCertificate(
            this.getClass().getClassLoader().getResource("client.jks"), "storepass", "jks");

        final XmlPage rpPage = webClient.getPage(url);
        final String xmlContent = rpPage.asXml();
        Assertions.assertTrue(xmlContent.startsWith("<md:EntityDescriptor"));

        // Now validate the Signature
        Document doc = rpPage.getXmlDocument();

        doc.getDocumentElement().setIdAttributeNS(null, "ID", true);

        Node signatureNode =
            DOMUtils.getChild(doc.getDocumentElement(), "Signature");
        Assertions.assertNotNull(signatureNode);

        XMLSignature signature = new XMLSignature((Element)signatureNode, "");
        KeyInfo ki = signature.getKeyInfo();
        Assertions.assertNotNull(ki);
        Assertions.assertNotNull(ki.getX509Certificate());

        Assertions.assertTrue(signature.checkSignatureValue(ki.getX509Certificate()));

        webClient.close();
    }

    @Test
    public void testIdPMetadataDefault() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort()
            + "/fediz-idp/metadata";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setSSLClientCertificate(
            this.getClass().getClassLoader().getResource("client.jks"), "storepass", "jks");

        final XmlPage rpPage = webClient.getPage(url);
        final String xmlContent = rpPage.asXml();
        Assertions.assertTrue(xmlContent.startsWith("<md:EntityDescriptor"));

        // Now validate the Signature
        Document doc = rpPage.getXmlDocument();

        doc.getDocumentElement().setIdAttributeNS(null, "ID", true);

        Node signatureNode =
            DOMUtils.getChild(doc.getDocumentElement(), "Signature");
        Assertions.assertNotNull(signatureNode);

        XMLSignature signature = new XMLSignature((Element)signatureNode, "");
        KeyInfo ki = signature.getKeyInfo();
        Assertions.assertNotNull(ki);
        Assertions.assertNotNull(ki.getX509Certificate());

        Assertions.assertTrue(signature.checkSignatureValue(ki.getX509Certificate()));

        webClient.close();
    }

    @Test
    public void testIdPServiceMetadata() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort()
            + "/fediz-idp/metadata/urn:org:apache:cxf:fediz:idp:realm-B";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setSSLClientCertificate(
            this.getClass().getClassLoader().getResource("client.jks"), "storepass", "jks");

        final XmlPage rpPage = webClient.getPage(url);
        final String xmlContent = rpPage.asXml();
        Assertions.assertTrue(xmlContent.startsWith("<md:EntityDescriptor"));

        // Now validate the Signature
        Document doc = rpPage.getXmlDocument();

        doc.getDocumentElement().setIdAttributeNS(null, "ID", true);

        Node signatureNode =
            DOMUtils.getChild(doc.getDocumentElement(), "Signature");
        Assertions.assertNotNull(signatureNode);

        XMLSignature signature = new XMLSignature((Element)signatureNode, "");
        KeyInfo ki = signature.getKeyInfo();
        Assertions.assertNotNull(ki);
        Assertions.assertNotNull(ki.getX509Certificate());

        Assertions.assertTrue(signature.checkSignatureValue(ki.getX509Certificate()));

        webClient.close();
    }

    // Send an unknown wreq value
    @org.junit.jupiter.api.Test
    public void testBadWReq() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String testWReq =
            "<RequestSecurityToken xmlns=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">"
            + "<TokenType>http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV3.0</TokenType>"
            + "</RequestSecurityToken>";
        url += "&wreq=" + URLEncoder.encode(testWReq, "UTF-8");

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreq value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    // Send an entity expansion attack for the wreq value
    @org.junit.jupiter.api.Test
    public void testEntityExpansionWReq() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String entity = IOUtils.toString(getClass().getResourceAsStream("/entity_wreq.xml"));
        String validWreq =
            "<RequestSecurityToken xmlns=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">"
            + "<TokenType>&m;http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</TokenType>"
            + "</RequestSecurityToken>";

        url += "&wreq=" + URLEncoder.encode(entity + validWreq, "UTF-8");

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreq value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    // Send an entity expansion attack for the wreq value
    @org.junit.jupiter.api.Test
    public void testEntityExpansionWReq2() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String entity = IOUtils.toString(getClass().getResourceAsStream("/entity_wreq2.xml"));
        String validWreq =
            "<RequestSecurityToken xmlns=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">"
            + "<TokenType>&m;http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</TokenType>"
            + "</RequestSecurityToken>";

        url += "&wreq=" + URLEncoder.encode(entity + validWreq, "UTF-8");

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);

        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreq value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    // Send an malformed wreq value
    @org.junit.jupiter.api.Test
    public void testMalformedWReq() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String testWReq =
            "<RequestSecurityToken xmlns=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">"
            + "<TokenTypehttp://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</TokenType>"
            + "</RequestSecurityToken>";
        url += "&wreq=" + URLEncoder.encode(testWReq, "UTF-8");

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreq value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    // Send an unknown wa value
    @org.junit.jupiter.api.Test
    public void testBadWa() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin2.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wa value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    // Send an unknown whr value
    @org.junit.jupiter.api.Test
    public void testBadWHR() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A-xyz";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad whr value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 500);
        }

        webClient.close();
    }

    // Send an unknown wtrealm value
    @org.junit.jupiter.api.Test
    public void testBadWtRealm() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld-xyz";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wtrealm value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    // Send an malformed wreply value
    @org.junit.jupiter.api.Test
    public void testMalformedWReply() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "/localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreply value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    // Send a bad wreply value
    @org.junit.jupiter.api.Test
    public void testBadWReply() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://www.apache.org:" + getRpHttpsPort() + "/"
            + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreply value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    @org.junit.jupiter.api.Test
    public void testValidWReplyWrongApplication() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld2";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreply value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    @org.junit.jupiter.api.Test
    public void testWReplyExactMatchingSuccess() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld3";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getPage(url);

        webClient.close();
    }

    @org.junit.jupiter.api.Test
    public void testWReplyExactMatchingFailure() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld3";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet/blah";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreply value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    @org.junit.jupiter.api.Test
    public void testNoEndpointAddressOrConstraint() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld4";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        // This is an error in the IdP
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreply value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    // Send a bad wreply value. This will pass the reg ex validation but fail the commons-validator
    // validation
    @org.junit.jupiter.api.Test
    public void testWReplyWithDoubleSlashes() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure//fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreply value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    // Send a query parameter that's too big
    @org.junit.jupiter.api.Test
    public void testLargeQueryParameterRejected() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?"
            + "wa=wsignin1.0"
            + "&whr=urn:org:apache:cxf:fediz:idp:realm-A"
            + "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";

        StringBuilder sb = new StringBuilder("https://localhost:").append(getRpHttpsPort()).append('/')
                .append(getServletContextName()).append("/secure/fedservlet");
        for (int i = 0; i < 100; i++) {
            sb.append("aaaaaaaaaa");
        }

        url += "&wreply=" + sb.toString();

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        try {
            webClient.getPage(url);
            Assertions.fail("Failure expected on a bad wreply value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    // Send a query parameter that's bigger than the accepted default, but is allowed by configuration
    @org.junit.jupiter.api.Test
    public void testLargeQueryParameterAccepted() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?"
            + "wa=wsignin1.0"
            + "&whr=urn:org:apache:cxf:fediz:idp:realm-A"
            + "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";

        StringBuilder sb = new StringBuilder("https://localhost:").append(getRpHttpsPort()).append('/')
            .append(getServletContextName()).append("/secure/fedservlet");
        for (int i = 0; i < 50; i++) {
            sb.append("aaaaaaaaaa");
        }

        url += "&wreply=" + sb.toString();

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        webClient.getPage(url);

        webClient.close();
    }

    @Test
    public void testIdPLogout() throws Exception {

        // 1. First let's login to the IdP
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());
        webClient.close();

        // 2. now we logout from IdP
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT;

        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        idpPage = webClient.getPage(idpLogoutUrl);

        Assertions.assertEquals("IDP SignOut Confirmation Response Page", idpPage.getTitleText());

        HtmlForm form = idpPage.getFormByName("signoutconfirmationresponseform");
        HtmlSubmitInput button = form.getInputByName("_eventId_submit");
        button.click();

        webClient.close();

        // 3. now we try to access the idp without authentication but with the existing cookies
        // to see if we are really logged out
        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        idpPage = webClient.getPage(url);

        Assertions.assertEquals(401, idpPage.getWebResponse().getStatusCode());

        webClient.close();
    }

    @Test
    public void testIdPLogoutCleanup() throws Exception {

        // 1. First let's login to the IdP
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());
        webClient.close();

        // 2. now we logout from IdP
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT_CLEANUP;

        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        idpPage = webClient.getPage(idpLogoutUrl);

        Assertions.assertEquals("IDP SignOut Response Page", idpPage.getTitleText());

        webClient.close();

        // 3. now we try to access the idp without authentication but with the existing cookies
        // to see if we are really logged out
        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        idpPage = webClient.getPage(url);

        Assertions.assertEquals(401, idpPage.getWebResponse().getStatusCode());

        webClient.close();
    }

    @Test
    public void testIdPLogoutCleanupWithBadWReply() throws Exception {

        // 1. First let's login to the IdP
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());
        webClient.close();

        // 2. now we logout from IdP using a bad wreply
        String badWReply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure//fedservlet";
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT_CLEANUP;
        idpLogoutUrl += "&wreply=" + badWReply;

        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        try {
            webClient.getPage(idpLogoutUrl);
            Assertions.fail("Failure expected on a bad wreply value");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();

        // 3. now we try to access the idp without authentication but with the existing cookies
        // to see if we are really logged out. Even though an error was thrown on a bad wreply, we should still
        // be logged out
        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        idpPage = webClient.getPage(url);

        Assertions.assertEquals(401, idpPage.getWebResponse().getStatusCode());

        webClient.close();
    }

    @Test
    public void testIdPLogoutWithWreplyConstraint() throws Exception {

        // 1. First let's login to the IdP
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());
        webClient.close();

        // 2. now we logout from IdP
        String logoutWReply = "https://localhost:12345";
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT + "&wreply=" + logoutWReply
            + "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";

        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        idpPage = webClient.getPage(idpLogoutUrl);

        Assertions.assertEquals("IDP SignOut Confirmation Response Page", idpPage.getTitleText());

        HtmlForm form = idpPage.getFormByName("signoutconfirmationresponseform");
        HtmlSubmitInput button = form.getInputByName("_eventId_submit");
        button.click();

        webClient.close();

        // 3. now we try to access the idp without authentication but with the existing cookies
        // to see if we are really logged out
        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        idpPage = webClient.getPage(url);

        Assertions.assertEquals(401, idpPage.getWebResponse().getStatusCode());

        webClient.close();
    }

    @Test
    public void testIdPLogoutWithWreplyBadAddress() throws Exception {

        // 1. First let's login to the IdP
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());
        webClient.close();

        // 2. now we logout from IdP
        String logoutWReply = "https://localhost:12345/badlogout";
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT + "&wreply=" + logoutWReply
            + "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";

        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        try {
            webClient.getPage(idpLogoutUrl);
            Assertions.fail("Failure expected on a non-matching wreply address");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    @Test
    public void testIdPLogoutWithNoRealm() throws Exception {

        // 1. First let's login to the IdP
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());
        webClient.close();

        // 2. now we logout from IdP
        String logoutWReply = "https://localhost:12345";
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT + "&wreply=" + logoutWReply;

        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        try {
            webClient.getPage(idpLogoutUrl);
            Assertions.fail("Failure expected on a non-matching wreply address");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    @Test
    public void testIdPLogoutWithWreplyAddress() throws Exception {

        // 1. First let's login to the IdP
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld3";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());
        webClient.close();

        // 2. now we logout from IdP
        String logoutWReply = "https://localhost:12345";
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT + "&wreply=" + logoutWReply
            + "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld3";

        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        idpPage = webClient.getPage(idpLogoutUrl);

        Assertions.assertEquals("IDP SignOut Confirmation Response Page", idpPage.getTitleText());

        HtmlForm form = idpPage.getFormByName("signoutconfirmationresponseform");
        HtmlSubmitInput button = form.getInputByName("_eventId_submit");
        button.click();

        webClient.close();

        // 3. now we try to access the idp without authentication but with the existing cookies
        // to see if we are really logged out
        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        idpPage = webClient.getPage(url);

        Assertions.assertEquals(401, idpPage.getWebResponse().getStatusCode());

        webClient.close();
    }

    @Test
    public void testIdPLogoutWithBadAddress() throws Exception {

        // 1. First let's login to the IdP
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld3";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());
        webClient.close();

        // 2. now we logout from IdP
        String logoutWReply = "https://localhost:12345/badlogout";
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT + "&wreply=" + logoutWReply
            + "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld3";

        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        try {
            webClient.getPage(idpLogoutUrl);
            Assertions.fail("Failure expected on a non-matching wreply address");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    @Test
    public void testIdPLogoutWithNoConfiguredConstraint() throws Exception {

        // 1. First let's login to the IdP
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?";
        url += "wa=wsignin1.0";
        url += "&whr=urn:org:apache:cxf:fediz:idp:realm-A";
        url += "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld2";
        String wreply = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure2/fedservlet";
        url += "&wreply=" + wreply;

        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());
        webClient.close();

        // 2. now we logout from IdP
        String logoutWReply = "https://localhost:12345";
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT + "&wreply=" + logoutWReply
            + "&wtrealm=urn:org:apache:cxf:fediz:fedizhelloworld2";

        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        try {
            webClient.getPage(idpLogoutUrl);
            Assertions.fail("Failure expected on a non-matching wreply address");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }

        webClient.close();
    }

    @org.junit.jupiter.api.Test
    public void testSwagger() throws Exception {
        String url = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/services/rs/swagger.json";

        String user = "alice";
        String password = "ecila";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        final UnexpectedPage swaggerPage = webClient.getPage(url);
        WebResponse response = swaggerPage.getWebResponse();
        Assertions.assertEquals("application/json", response.getContentType());
        String json = response.getContentAsString();
        Assertions.assertTrue(json.contains("Claims"));

        webClient.close();
    }

}
