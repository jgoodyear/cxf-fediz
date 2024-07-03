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

package org.apache.cxf.fediz.systests.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.fediz.core.ClaimTypes;
import org.apache.cxf.fediz.core.FederationConstants;
import org.apache.cxf.fediz.core.util.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.htmlunit.CookieManager;
import org.htmlunit.FailingHttpStatusCodeException;
import org.htmlunit.HttpMethod;
import org.htmlunit.WebClient;
import org.htmlunit.WebRequest;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.DomNodeList;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlSubmitInput;
import org.htmlunit.util.NameValuePair;
import org.htmlunit.xml.XmlPage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class AbstractTests {

    static {
        WSSConfig.init();
    }

    public abstract String getServletContextName();

    public abstract String getIdpHttpsPort();

    public abstract String getRpHttpsPort();

    protected boolean isWSFederation() {
        return true;
    }

    protected String getLoginFormName() {
        if (isWSFederation()) {
            return "signinresponseform";
        }
        return "samlsigninresponseform";
    }

    protected String getTokenName() {
        if (isWSFederation()) {
            return "wresult";
        }

        return "SAMLResponse";
    }

    protected String getContextName() {
        if (isWSFederation()) {
            return "wctx";
        }

        return "RelayState";
    }

    @Test
    public void testAlice() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet";
        String user = "alice";
        String password = "ecila";

        final String bodyTextContent =
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());

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

    @Test
    public void testAliceUser() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/user/fedservlet";
        String user = "alice";
        String password = "ecila";

        final String bodyTextContent =
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());

        Assertions.assertTrue(bodyTextContent.contains("userPrincipal=" + user), "Principal not " + user);
        Assertions.assertTrue(bodyTextContent.contains("role:Admin=false"),
                "User " + user + " does not have role Admin");
        Assertions.assertTrue(bodyTextContent.contains("role:Manager=false"),
                "User " + user + " does not have role Manager");
        Assertions.assertTrue(bodyTextContent.contains("role:User=true"),
                "User " + user + " must have role User");
    }

    @Test
    public void testAliceAdminNoAccess() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/admin/fedservlet";
        String user = "alice";
        String password = "ecila";

        try {
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());
            Assertions.fail("Exception expected");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 403);
        }
    }

    @Test
    public void testAliceManagerNoAccess() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/manager/fedservlet";
        String user = "alice";
        String password = "ecila";

        try {
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());
            Assertions.fail("Exception expected");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 403);
        }
    }

    @Test
    public void testAliceWrongPasswordNoAccess() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet";
        String user = "alice";
        String password = "alice";

        try {
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());
            Assertions.fail("Exception expected");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 401);
        }
    }

    @Test
    public void testBob() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet";
        String user = "bob";
        String password = "bob";

        final String bodyTextContent =
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());

        Assertions.assertTrue(bodyTextContent.contains("userPrincipal=" + user), "Principal not " + user);
        Assertions.assertTrue(bodyTextContent.contains("role:Admin=true"),
                "User " + user + " does not have role Admin");
        Assertions.assertTrue(bodyTextContent.contains("role:Manager=true"),
                "User " + user + " does not have role Manager");
        Assertions.assertTrue(bodyTextContent.contains("role:User=true"),
                "User " + user + " must have role User");

        String claim = ClaimTypes.FIRSTNAME.toString();
        Assertions.assertTrue(bodyTextContent.contains(claim + "=Bob"),
                "User " + user + " claim " + claim + " is not 'Bob'");
        claim = ClaimTypes.LASTNAME.toString();
        Assertions.assertTrue(bodyTextContent.contains(claim + "=Windsor"),
                "User " + user + " claim " + claim + " is not 'Windsor'");
        claim = ClaimTypes.EMAILADDRESS.toString();
        Assertions.assertTrue(bodyTextContent.contains(claim + "=bobwindsor@realma.org"),
                "User " + user + " claim " + claim + " is not 'bobwindsor@realma.org'");
    }

    @Test
    public void testBobUser() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/user/fedservlet";
        String user = "bob";
        String password = "bob";

        final String bodyTextContent =
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());

        Assertions.assertTrue(bodyTextContent.contains("userPrincipal=" + user), "Principal not " + user);
        Assertions.assertTrue(bodyTextContent.contains("role:Admin=true"),
                "User " + user + " does not have role Admin");
        Assertions.assertTrue(bodyTextContent.contains("role:Manager=true"),
                "User " + user + " does not have role Manager");
        Assertions.assertTrue(bodyTextContent.contains("role:User=true"),
                "User " + user + " must have role User");
    }

    @Test
    public void testBobManager() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/manager/fedservlet";
        String user = "bob";
        String password = "bob";

        final String bodyTextContent =
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());

        Assertions.assertTrue(bodyTextContent.contains("userPrincipal=" + user), "Principal not " + user);
        Assertions.assertTrue(bodyTextContent.contains("role:Admin=true"),
                "User " + user + " does not have role Admin");
        Assertions.assertTrue(bodyTextContent.contains("role:Manager=true"),
                "User " + user + " does not have role Manager");
        Assertions.assertTrue(bodyTextContent.contains("role:User=true"),
                "User " + user + " must have role User");
    }

    @Test
    public void testBobAdmin() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/admin/fedservlet";
        String user = "bob";
        String password = "bob";

        final String bodyTextContent =
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());

        Assertions.assertTrue(bodyTextContent.contains("userPrincipal=" + user), "Principal not " + user);
        Assertions.assertTrue(bodyTextContent.contains("role:Admin=true"),
                "User " + user + " does not have role Admin");
        Assertions.assertTrue(bodyTextContent.contains("role:Manager=true"),
                "User " + user + " does not have role Manager");
        Assertions.assertTrue(bodyTextContent.contains("role:User=true"),
                "User " + user + " must have role User");
    }

    @Test
    public void testTed() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet";
        String user = "ted";
        String password = "det";

        final String bodyTextContent =
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());

        Assertions.assertTrue(bodyTextContent.contains("userPrincipal=" + user), "Principal not " + user);
        Assertions.assertTrue(bodyTextContent.contains("role:Admin=false"),
                "User " + user + " does not have role Admin");
        Assertions.assertTrue(bodyTextContent.contains("role:Manager=false"),
                "User " + user + " does not have role Manager");
        Assertions.assertTrue(bodyTextContent.contains("role:User=false"),
                "User " + user + " must have role User");

        String claim = ClaimTypes.FIRSTNAME.toString();
        Assertions.assertTrue(bodyTextContent.contains(claim + "=Ted"),
                "User " + user + " claim " + claim + " is not 'Ted'");
        claim = ClaimTypes.LASTNAME.toString();
        Assertions.assertTrue(bodyTextContent.contains(claim + "=Cooper"),
                "User " + user + " claim " + claim + " is not 'Cooper'");
        claim = ClaimTypes.EMAILADDRESS.toString();
        Assertions.assertTrue(bodyTextContent.contains(claim + "=tcooper@realma.org"),
                "User " + user + " claim " + claim + " is not 'tcooper@realma.org'");
    }

    @Test
    public void testTedUserNoAccess() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/user/fedservlet";
        String user = "ted";
        String password = "det";

        try {
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());
            Assertions.fail("Exception expected");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 403);
        }
    }

    @Test
    public void testTedAdminNoAccess() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/admin/fedservlet";
        String user = "ted";
        String password = "det";

        try {
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());
            Assertions.fail("Exception expected");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 403);
        }
    }

    @Test
    public void testTedManagerNoAccess() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/manager/fedservlet";
        String user = "ted";
        String password = "det";

        try {
            HTTPTestUtils.login(url, user, password, getIdpHttpsPort(), getLoginFormName());
            Assertions.fail("Exception expected");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 403);
        }
    }

    @Test
    public void testRPMetadata() throws Exception {

        if (!isWSFederation()) {
            return;
        }

        String url = "https://localhost:" + getRpHttpsPort()
            + "/" + getServletContextName() + "/FederationMetadata/2007-06/FederationMetadata.xml";

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
    public void testRPLogout() throws Exception {

        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet";
        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        // 1. Login
        HTTPTestUtils.loginWithCookieManager(url, user, password, getIdpHttpsPort(), getLoginFormName(), cookieManager);

        // 2. Now we should have a cookie from the RP and IdP and should be able to do
        // subsequent requests without authenticate again. Lets test this first.
        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        final HtmlPage rpPage = webClient.getPage(url);
        Assertions.assertTrue("WS Federation Systests Examples".equals(rpPage.getTitleText())
                          || "WS Federation Systests Spring Examples".equals(rpPage.getTitleText()));

        // 3. now we logout from RP
        String rpLogoutUrl = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/logout";

        HTTPTestUtils.logout(rpLogoutUrl, cookieManager, isWSFederation());

        // 4. now we try to access the RP and idp without authentication but with the existing cookies
        // to see if we are really logged out

        webClient.close();
        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        final HtmlPage idpPage = webClient.getPage(url);

        Assertions.assertEquals(401, idpPage.getWebResponse().getStatusCode());

        webClient.close();
    }

    @Test
    public void testRPLogoutViaAction() throws Exception {

        if (!isWSFederation()) {
            return;
        }

        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet";
        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        // 1. Login
        HTTPTestUtils.loginWithCookieManager(url, user, password, getIdpHttpsPort(), getLoginFormName(), cookieManager);

        // 2. Now we should have a cookie from the RP and IdP and should be able to do
        // subsequent requests without authenticate again. Lets test this first.
        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        final HtmlPage rpPage = webClient.getPage(url);
        Assertions.assertTrue("WS Federation Systests Examples".equals(rpPage.getTitleText())
                          || "WS Federation Systests Spring Examples".equals(rpPage.getTitleText()));

        // 3. now we logout from RP
        String rpLogoutUrl = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet?wa=" + FederationConstants.ACTION_SIGNOUT;

        HTTPTestUtils.logout(rpLogoutUrl, cookieManager, isWSFederation());

        // 4. now we try to access the RP and idp without authentication but with the existing cookies
        // to see if we are really logged out

        webClient.close();
        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        final HtmlPage idpPage = webClient.getPage(url);

        Assertions.assertEquals(401, idpPage.getWebResponse().getStatusCode());

        webClient.close();
    }

    @Test
    public void testIdPLogout() throws Exception {

        if (!isWSFederation()) {
            return;
        }

        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet";
        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        // 1. Login
        HTTPTestUtils.loginWithCookieManager(url, user, password, getIdpHttpsPort(), getLoginFormName(), cookieManager);

        // 2. Now we should have a cookie from the RP and IdP and should be able to do
        // subsequent requests without authenticate again. Lets test this first.
        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        final HtmlPage rpPage = webClient.getPage(url);
        Assertions.assertTrue("WS Federation Systests Examples".equals(rpPage.getTitleText())
                          || "WS Federation Systests Spring Examples".equals(rpPage.getTitleText()));

        // 3. now we logout from IdP
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT; //todo logout url on idp?!?

        HTTPTestUtils.logout(idpLogoutUrl, cookieManager, isWSFederation());

        // 4. now we try to access the RP and idp without authentication but with the existing cookies
        // to see if we are really logged out

        webClient.close();
        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        final HtmlPage idpPage = webClient.getPage(url);

        Assertions.assertEquals(401, idpPage.getWebResponse().getStatusCode());

        webClient.close();
    }

    @Test
    public void testIdPLogoutCleanup() throws Exception {

        if (!isWSFederation()) {
            return;
        }

        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet";
        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        // 1. Login
        HTTPTestUtils.loginWithCookieManager(url, user, password, getIdpHttpsPort(), getLoginFormName(), cookieManager);

        // 2. Now we should have a cookie from the RP and IdP and should be able to do
        // subsequent requests without authenticate again. Lets test this first.
        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        final HtmlPage rpPage = webClient.getPage(url);
        Assertions.assertTrue("WS Federation Systests Examples".equals(rpPage.getTitleText())
                          || "WS Federation Systests Spring Examples".equals(rpPage.getTitleText()));

        // 3. now we logout from IdP
        String idpLogoutUrl = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation?wa="
            + FederationConstants.ACTION_SIGNOUT_CLEANUP;

        HTTPTestUtils.logoutCleanup(idpLogoutUrl, cookieManager);

        // 4. now we try to access the RP and idp without authentication but with the existing cookies
        // to see if we are really logged out

        webClient.close();
        webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        final HtmlPage idpPage = webClient.getPage(url);

        Assertions.assertEquals(401, idpPage.getWebResponse().getStatusCode());

        webClient.close();
    }

    @Test
    public void testAliceModifiedSignature() throws Exception {

        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/secure/fedservlet";
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

        // Parse the form to get the token (wresult)
        DomNodeList<DomElement> results = idpPage.getElementsByTagName("input");

        for (DomElement result : results) {
            if (getTokenName().equals(result.getAttributeNS(null, "name"))) {
                // Now modify the Signature
                String value = result.getAttributeNS(null, "value");
                if (value.contains("alice")) {
                    value = value.replace("alice", "bob");
                } else {
                    value = "H" + value;
                }
                result.setAttributeNS(null, "value", value);
            }
        }

        // Invoke back on the RP

        final HtmlForm form = idpPage.getFormByName(getLoginFormName());
        final HtmlSubmitInput button = form.getInputByName("_eventId_submit");

        try {
            button.click();
            Assertions.fail("Failure expected on a modified signature");
        } catch (FailingHttpStatusCodeException ex) {
            // expected
            Assertions.assertTrue(401 == ex.getStatusCode() || 403 == ex.getStatusCode());
        }

        webClient.close();
    }

    @Test
    public void testConcurrentRequests() throws Exception {

        String url1 = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        String url2 = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/test.html";
        String user = "bob";
        String password = "bob";

        // Get the initial token
        CookieManager cookieManager = new CookieManager();
        final WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage1 = webClient.getPage(url1);
        final HtmlPage idpPage2 = webClient.getPage(url2);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage1.getTitleText());
        Assertions.assertEquals("IDP SignIn Response Form", idpPage2.getTitleText());

        // Invoke back on the page1 RP
        final HtmlForm form = idpPage1.getFormByName(getLoginFormName());
        final HtmlSubmitInput button = form.getInputByName("_eventId_submit");
        final HtmlPage rpPage1 = button.click();
        Assertions.assertTrue("WS Federation Systests Examples".equals(rpPage1.getTitleText())
                          || "WS Federation Systests Spring Examples".equals(rpPage1.getTitleText()));

        String bodyTextContent1 = rpPage1.getBody().getTextContent();

        Assertions.assertTrue(bodyTextContent1.contains("userPrincipal=" + user), "Principal not " + user);

        // Invoke back on the page2 RP
        final HtmlForm form2 = idpPage2.getFormByName(getLoginFormName());
        final HtmlSubmitInput button2 = form2.getInputByName("_eventId_submit");
        final HtmlPage rpPage2 = button2.click();
        String bodyTextContent2 = rpPage2.getBody().getTextContent();

        Assertions.assertTrue(bodyTextContent2.contains("Secure Test"), "Unexpected content of RP page");

        webClient.close();
    }

    @org.junit.jupiter.api.Test
    public void testMaliciousRedirect() throws Exception {
        if (!isWSFederation()) {
            return;
        }

        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        String user = "alice";
        String password = "ecila";

        CookieManager cookieManager = new CookieManager();

        // 1. Login
        HTTPTestUtils.loginWithCookieManager(url, user, password, getIdpHttpsPort(), getLoginFormName(), cookieManager);

        // 2. Now we should have a cookie from the RP and IdP and should be able to do
        // subsequent requests without authenticate again. Lets test this first.
        WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        HtmlPage rpPage = webClient.getPage(url);
        Assertions.assertTrue("WS Federation Systests Examples".equals(rpPage.getTitleText())
                          || "WS Federation Systests Spring Examples".equals(rpPage.getTitleText()));

        // 3. Now a malicious user sends the client a URL with a bad "wreply" address to the IdP
        String maliciousURL = "https://www.apache.org/attack";
        String idpUrl
         = "https://localhost:" + getIdpHttpsPort() + "/fediz-idp/federation";
        idpUrl += "?wa=wsignin1.0&wreply=" + URLEncoder.encode(maliciousURL, "UTF-8");
        idpUrl += "&wtrealm=urn%3Aorg%3Aapache%3Acxf%3Afediz%3Afedizhelloworld";
        idpUrl += "&whr=urn%3Aorg%3Aapache%3Acxf%3Afediz%3Aidp%3Arealm-A";
        webClient.close();

        final WebClient webClient2 = new WebClient();
        webClient2.setCookieManager(cookieManager);
        webClient2.getOptions().setUseInsecureSSL(true);
        webClient2.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient2.getOptions().setJavaScriptEnabled(false);
        try {
            webClient2.getPage(idpUrl);
            Assertions.fail("Failure expected on a bad wreply address");
        } catch (FailingHttpStatusCodeException ex) {
            Assertions.assertEquals(ex.getStatusCode(), 400);
        }
        webClient2.close();
    }

    @Test
    public void testEntityExpansionAttack() throws Exception {

        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
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

        // Parse the form to get the token (wresult)
        DomNodeList<DomElement> results = idpPage.getElementsByTagName("input");

        String entity = getResourceAsString("/entity.xml");
        String reference = "&m;";

        for (DomElement result : results) {
            if (getTokenName().equals(result.getAttributeNS(null, "name"))) {
                // Now modify the Signature
                String value = result.getAttributeNS(null, "value");

                if (isWSFederation()) {
                    value = entity + value;
                    value = value.replace("alice", reference);
                    result.setAttributeNS(null, "value", value);
                } else {
                    // Decode response
                    byte[] deflatedToken = Base64Utility.decode(value);
                    InputStream inputStream = new ByteArrayInputStream(deflatedToken);

                    Document responseDoc = StaxUtils.read(new InputStreamReader(inputStream, "UTF-8"));

                    // Modify SignatureValue to include the entity
                    String signatureNamespace = "http://www.w3.org/2000/09/xmldsig#";
                    Node signatureValue =
                        responseDoc.getElementsByTagNameNS(signatureNamespace, "SignatureValue").item(0);
                    signatureValue.setTextContent(reference + signatureValue.getTextContent());

                    // Re-encode response
                    String responseMessage = DOM2Writer.nodeToString(responseDoc);
                    result.setAttributeNS(null, "value", Base64Utility.encode(
                            (entity + responseMessage).getBytes(StandardCharsets.UTF_8)));
                }
            }
        }

        // Invoke back on the RP

        final HtmlForm form = idpPage.getFormByName(getLoginFormName());
        final HtmlSubmitInput button = form.getInputByName("_eventId_submit");

        try {
            button.click();
            Assertions.fail("Failure expected on an entity expansion attack");
        } catch (FailingHttpStatusCodeException ex) {
            // expected
            Assertions.assertTrue(401 == ex.getStatusCode() || 403 == ex.getStatusCode());
        }

        webClient.close();
    }

    @Test
    public void testEntityExpansionAttack2() throws Exception {

        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
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

        // Parse the form to get the token (wresult)
        DomNodeList<DomElement> results = idpPage.getElementsByTagName("input");

        String entity = getResourceAsString("/entity2.xml");
        String reference = "&m;";

        for (DomElement result : results) {
            if (getTokenName().equals(result.getAttributeNS(null, "name"))) {
                // Now modify the Signature
                String value = result.getAttributeNS(null, "value");

                if (isWSFederation()) {
                    value = entity + value;
                    value = value.replace("alice", reference);
                    result.setAttributeNS(null, "value", value);
                } else {
                    // Decode response
                    byte[] deflatedToken = Base64Utility.decode(value);
                    InputStream inputStream = new ByteArrayInputStream(deflatedToken);

                    Document responseDoc = StaxUtils.read(new InputStreamReader(inputStream, "UTF-8"));

                    // Modify SignatureValue to include the entity
                    String signatureNamespace = "http://www.w3.org/2000/09/xmldsig#";
                    Node signatureValue =
                        responseDoc.getElementsByTagNameNS(signatureNamespace, "SignatureValue").item(0);
                    signatureValue.setTextContent(reference + signatureValue.getTextContent());

                    // Re-encode response
                    String responseMessage = DOM2Writer.nodeToString(responseDoc);
                    result.setAttributeNS(null, "value", Base64Utility.encode(
                            (entity + responseMessage).getBytes(StandardCharsets.UTF_8)));
                }
            }
        }

        // Invoke back on the RP

        final HtmlForm form = idpPage.getFormByName(getLoginFormName());
        final HtmlSubmitInput button = form.getInputByName("_eventId_submit");

        try {
            button.click();
            Assertions.fail("Failure expected on an entity expansion attack");
        } catch (FailingHttpStatusCodeException ex) {
            // expected
            Assertions.assertTrue(401 == ex.getStatusCode() || 403 == ex.getStatusCode());
        }

        webClient.close();
    }

    @org.junit.jupiter.api.Test
    public void testCSRFAttack() throws Exception {

        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        csrfAttackTest(url);
    }

    protected void csrfAttackTest(String rpURL) throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        String user = "alice";
        String password = "ecila";

        // 1. Log in as "alice"
        WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials(user, password));

        webClient.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());

        final HtmlForm form = idpPage.getFormByName(getLoginFormName());
        final HtmlSubmitInput button = form.getInputByName("_eventId_submit");

        final HtmlPage rpPage = button.click();
        Assertions.assertTrue("WS Federation Systests Examples".equals(rpPage.getTitleText())
                            || "WS Federation Systests Spring Examples".equals(rpPage.getTitleText()));


        // 2. Log in as "bob" using another WebClient
        WebClient webClient2 = new WebClient();
        webClient2.getOptions().setUseInsecureSSL(true);
        webClient2.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials("bob", "bob"));

        webClient2.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage2 = webClient2.getPage(url);
        webClient2.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage2.getTitleText());

        // 3. Now instead of clicking on the form, send the form via alice's WebClient instead

        // Send with context...
        WebRequest request = new WebRequest(new URL(rpURL), HttpMethod.POST);
        request.setRequestParameters(new ArrayList<NameValuePair>());

        DomNodeList<DomElement> results = idpPage2.getElementsByTagName("input");

        for (DomElement result : results) {
            if (isWSFederation()) {
                if ("wresult".equals(result.getAttributeNS(null, "name"))
                    || "wa".equals(result.getAttributeNS(null, "name"))
                    || "wctx".equals(result.getAttributeNS(null, "name"))) {
                    String value = result.getAttributeNS(null, "value");
                    request.getRequestParameters().add(new NameValuePair(result.getAttributeNS(null, "name"), value));
                }
            } else {
                if ("SAMLResponse".equals(result.getAttributeNS(null, "name"))
                    || "RelayState".equals(result.getAttributeNS(null, "name"))) {
                    String value = result.getAttributeNS(null, "value");
                    request.getRequestParameters().add(new NameValuePair(result.getAttributeNS(null, "name"), value));
                }
            }
        }

        try {
            webClient.getPage(request);
            Assertions.fail("Failure expected on a CSRF attack");
        } catch (FailingHttpStatusCodeException ex) {
            // expected
        }

        webClient.close();
        webClient2.close();

    }

    @org.junit.jupiter.api.Test
    public void testCSRFAttack2() throws Exception {

        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";
        csrfAttackTest2(url);
    }

    protected void csrfAttackTest2(String rpURL) throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";

        // 1. Log in as "bob" using another WebClient
        WebClient webClient2 = new WebClient();
        webClient2.getOptions().setUseInsecureSSL(true);
        webClient2.getCredentialsProvider().setCredentials(
            new AuthScope("localhost", Integer.parseInt(getIdpHttpsPort())),
            new UsernamePasswordCredentials("bob", "bob"));

        webClient2.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage2 = webClient2.getPage(url);
        webClient2.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage2.getTitleText());

        // 2. Now instead of clicking on the form, send the form via alice's WebClient instead

        // Send with context...
        WebRequest request = new WebRequest(new URL(rpURL), HttpMethod.POST);
        request.setRequestParameters(new ArrayList<NameValuePair>());

        DomNodeList<DomElement> results = idpPage2.getElementsByTagName("input");

        for (DomElement result : results) {
            if (isWSFederation()) {
                if ("wresult".equals(result.getAttributeNS(null, "name"))
                    || "wa".equals(result.getAttributeNS(null, "name"))
                    || "wctx".equals(result.getAttributeNS(null, "name"))) {
                    String value = result.getAttributeNS(null, "value");
                    request.getRequestParameters().add(new NameValuePair(result.getAttributeNS(null, "name"), value));
                }
            } else {
                if ("SAMLResponse".equals(result.getAttributeNS(null, "name"))
                    || "RelayState".equals(result.getAttributeNS(null, "name"))) {
                    String value = result.getAttributeNS(null, "value");
                    request.getRequestParameters().add(new NameValuePair(result.getAttributeNS(null, "name"), value));
                }
            }
        }

        WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);

        try {
            webClient.getPage(request);
            Assertions.fail("Failure expected on a CSRF attack");
        } catch (FailingHttpStatusCodeException ex) {
            // expected
        }

        webClient.close();
        webClient2.close();

    }

    private static String getResourceAsString(String location) throws IOException {
        try (InputStream is = AbstractTests.class.getResourceAsStream(location)) {
            byte[] content = new byte[is.available()];
            is.read(content);
            return new String(content, StandardCharsets.UTF_8);
        }
    }

}
