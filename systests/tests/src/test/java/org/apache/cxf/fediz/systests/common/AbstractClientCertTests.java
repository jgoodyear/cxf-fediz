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

import java.net.URL;
import java.util.ArrayList;

import org.apache.cxf.fediz.core.ClaimTypes;
import org.apache.wss4j.dom.engine.WSSConfig;
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

import org.junit.jupiter.api.Assertions;

public abstract class AbstractClientCertTests {

    static {
        WSSConfig.init();
    }

    public AbstractClientCertTests() {
        super();
    }

    public abstract String getServletContextName();

    public abstract String getIdpHttpsPort();

    public abstract String getRpHttpsPort();

    @org.junit.jupiter.api.Test
    public void testClientAuthentication() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";

        final WebClient webClient = new WebClient();
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setSSLClientCertificate(
            this.getClass().getClassLoader().getResource("alice_client.jks"), "storepass", "jks");

        webClient.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());

        final HtmlForm form = idpPage.getFormByName("signinresponseform");
        final HtmlSubmitInput button = form.getInputByName("_eventId_submit");

        // Test the Subject Confirmation method here
        DomNodeList<DomElement> results = idpPage.getElementsByTagName("input");

        String wresult = null;
        for (DomElement result : results) {
            if ("wresult".equals(result.getAttributeNS(null, "name"))) {
                wresult = result.getAttributeNS(null, "value");
                break;
            }
        }
        Assertions.assertTrue(wresult != null
            && wresult.contains("urn:oasis:names:tc:SAML:2.0:cm:holder-of-key"));

        final HtmlPage rpPage = button.click();
        Assertions.assertTrue("WS Federation Systests Examples".equals(rpPage.getTitleText())
                          || "WS Federation Systests Spring Examples".equals(rpPage.getTitleText()));

        final String bodyTextContent = rpPage.getBody().getTextContent();
        String user = "alice";
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

        webClient.close();
    }

    @org.junit.jupiter.api.Test
    public void testDifferentClientCertificate() throws Exception {
        // Get the initial wresult from the IdP
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName() + "/secure/fedservlet";

        CookieManager cookieManager = new CookieManager();
        final WebClient webClient = new WebClient();
        webClient.setCookieManager(cookieManager);
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setSSLClientCertificate(
            this.getClass().getClassLoader().getResource("alice_client.jks"), "storepass", "jks");

        webClient.getOptions().setJavaScriptEnabled(false);
        final HtmlPage idpPage = webClient.getPage(url);
        webClient.getOptions().setJavaScriptEnabled(true);
        Assertions.assertEquals("IDP SignIn Response Form", idpPage.getTitleText());

        // Test the Subject Confirmation method here
        DomNodeList<DomElement> results = idpPage.getElementsByTagName("input");

        String wresult = null;
        String wa = "wsignin1.0";
        String wctx = null;
        String wtrealm = null;
        for (DomElement result : results) {
            if ("wresult".equals(result.getAttributeNS(null, "name"))) {
                wresult = result.getAttributeNS(null, "value");
            } else if ("wctx".equals(result.getAttributeNS(null, "name"))) {
                wctx = result.getAttributeNS(null, "value");
            } else if ("wtrealm".equals(result.getAttributeNS(null, "name"))) {
                wtrealm = result.getAttributeNS(null, "value");
            }
        }
        Assertions.assertTrue(wctx != null && wtrealm != null);
        Assertions.assertTrue(wresult != null
            && wresult.contains("urn:oasis:names:tc:SAML:2.0:cm:holder-of-key"));
        webClient.close();

        // Now invoke on the RP using the saved parameters above, but a different client cert!
        final WebClient webClient2 = new WebClient();
        webClient2.setCookieManager(cookieManager);
        webClient2.getOptions().setUseInsecureSSL(true);
        webClient2.getOptions().setSSLClientCertificate(
            this.getClass().getClassLoader().getResource("server.jks"), "tompass", "jks");

        WebRequest request = new WebRequest(new URL(url), HttpMethod.POST);

        request.setRequestParameters(new ArrayList<NameValuePair>());
        request.getRequestParameters().add(new NameValuePair("wctx", wctx));
        request.getRequestParameters().add(new NameValuePair("wa", wa));
        request.getRequestParameters().add(new NameValuePair("wtrealm", wtrealm));
        request.getRequestParameters().add(new NameValuePair("wresult", wresult));

        try {
            webClient2.getPage(request);
            Assertions.fail("Exception expected");
        } catch (FailingHttpStatusCodeException ex) {
            // expected
            Assertions.assertTrue(401 == ex.getStatusCode() || 403 == ex.getStatusCode());
        }

        webClient2.close();
    }

}
