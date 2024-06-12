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

package org.apache.cxf.fediz.core.samlsso;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.cxf.fediz.common.SecurityTestUtil;
import org.apache.cxf.fediz.core.RequestState;
import org.apache.cxf.fediz.core.config.FedizConfigurator;
import org.apache.cxf.fediz.core.config.FedizContext;
import org.apache.cxf.fediz.core.processor.FedizProcessor;
import org.apache.cxf.fediz.core.processor.RedirectionResponse;
import org.apache.cxf.fediz.core.processor.SAMLProcessorImpl;
import org.apache.cxf.fediz.core.util.DOMUtils;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.dom.WSConstants;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.LogoutRequest;

import org.easymock.EasyMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;

/**
 * Some tests for creating SAMLRequests using the SAMLProcessorImpl
 */
public class SAMLRequestTest {
    static final String TEST_USER = "alice";
    static final String TEST_REQUEST_URL = "https://localhost/fedizhelloworld/";
    static final String TEST_REQUEST_URI = "/fedizhelloworld";
    static final String TEST_IDP_ISSUER = "http://url_to_the_issuer";
    static final String TEST_CLIENT_ADDRESS = "https://127.0.0.1";

    private static final String CONFIG_FILE = "fediz_test_config_saml.xml";

    private static FedizConfigurator configurator;
    private static DocumentBuilderFactory docBuilderFactory;

    static {
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);
    }


    @BeforeAll
    public static void init() {
        getFederationConfigurator();
        Assertions.assertNotNull(configurator);
    }

    @AfterAll
    public static void cleanup() {
        SecurityTestUtil.cleanup();
    }


    private static FedizConfigurator getFederationConfigurator() {
        if (configurator != null) {
            return configurator;
        }
        try {
            configurator = new FedizConfigurator();
            final URL resource = Thread.currentThread().getContextClassLoader()
                    .getResource(CONFIG_FILE);
            File f = new File(resource.toURI());
            configurator.loadConfig(f);
            return configurator;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @org.junit.jupiter.api.Test
    public void createSAMLAuthnRequest() throws Exception {
        // Mock up a Request
        FedizContext config = getFederationConfigurator().getFedizContext("ROOT");

        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getRequestURL()).andReturn(new StringBuffer(TEST_REQUEST_URL)).times(1, 2);
        EasyMock.expect(req.getContextPath()).andReturn(TEST_REQUEST_URI);
        EasyMock.expect(req.getRequestURI()).andReturn(TEST_REQUEST_URI).times(1, 2);
        EasyMock.replay(req);

        FedizProcessor wfProc = new SAMLProcessorImpl();
        RedirectionResponse response = wfProc.createSignInRequest(req, config);

        String redirectionURL = response.getRedirectionURL();
        Assertions.assertTrue(redirectionURL.startsWith(TEST_IDP_ISSUER));
        Assertions.assertTrue(redirectionURL.contains("SAMLRequest="));
        Assertions.assertTrue(redirectionURL.contains("RelayState="));

        Map<String, String> headers = response.getHeaders();
        Assertions.assertNotNull(headers);
        Assertions.assertFalse(headers.isEmpty());
        Assertions.assertTrue("no-cache, no-store".equals(headers.get("Cache-Control")));
        Assertions.assertTrue("no-cache".equals(headers.get("Pragma")));
    }

    @org.junit.jupiter.api.Test
    public void testAuthnRelayState() throws Exception {
        // Mock up a Request
        FedizContext config = getFederationConfigurator().getFedizContext("ROOT");

        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getRequestURL()).andReturn(new StringBuffer(TEST_REQUEST_URL)).times(1, 2);
        EasyMock.expect(req.getContextPath()).andReturn(TEST_REQUEST_URI);
        EasyMock.expect(req.getRequestURI()).andReturn(TEST_REQUEST_URI).times(1, 2);
        EasyMock.replay(req);

        FedizProcessor wfProc = new SAMLProcessorImpl();
        RedirectionResponse response = wfProc.createSignInRequest(req, config);

        String redirectionURL = response.getRedirectionURL();
        String relayState =
            redirectionURL.substring(redirectionURL.indexOf("RelayState=") + "RelayState=".length());
        Assertions.assertNotNull(relayState);

        RequestState requestState = response.getRequestState();

        Assertions.assertEquals(TEST_IDP_ISSUER, requestState.getIdpServiceAddress());
        Assertions.assertEquals(TEST_REQUEST_URL, requestState.getIssuerId());
        Assertions.assertEquals(TEST_REQUEST_URL, requestState.getTargetAddress());
    }

    @org.junit.jupiter.api.Test
    public void testSAMLAuthnRequest() throws Exception {
        // Mock up a Request
        FedizContext config = getFederationConfigurator().getFedizContext("ROOT");

        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getRequestURL()).andReturn(new StringBuffer(TEST_REQUEST_URL)).times(1, 2);
        EasyMock.expect(req.getContextPath()).andReturn(TEST_REQUEST_URI);
        EasyMock.expect(req.getRequestURI()).andReturn(TEST_REQUEST_URI).times(1, 2);
        EasyMock.replay(req);

        FedizProcessor wfProc = new SAMLProcessorImpl();
        RedirectionResponse response = wfProc.createSignInRequest(req, config);

        String redirectionURL = response.getRedirectionURL();
        String samlRequest =
            redirectionURL.substring(redirectionURL.indexOf("SAMLRequest=") + "SAMLRequest=".length(),
                                     redirectionURL.indexOf("RelayState=") - 1);

        byte[] deflatedToken = Base64.getDecoder().decode(URLDecoder.decode(samlRequest, "UTF-8"));
        InputStream tokenStream = CompressionUtils.inflate(deflatedToken);

        Document requestDoc = DOMUtils.readXml(new InputStreamReader(tokenStream, StandardCharsets.UTF_8));
        AuthnRequest request =
            (AuthnRequest)OpenSAMLUtil.fromDom(requestDoc.getDocumentElement());

        Assertions.assertEquals(TEST_REQUEST_URL, request.getIssuer().getValue());
        Assertions.assertEquals(TEST_REQUEST_URL, request.getAssertionConsumerServiceURL());
        Assertions.assertEquals("2.0",  request.getVersion().toString());
    }

    @org.junit.jupiter.api.Test
    public void testSignedSAMLAuthnRequest() throws Exception {
        // Mock up a Request
        FedizContext config = getFederationConfigurator().getFedizContext("SIGNED_ROOT");

        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getRequestURL()).andReturn(new StringBuffer(TEST_REQUEST_URL)).times(1, 2);
        EasyMock.expect(req.getContextPath()).andReturn(TEST_REQUEST_URI);
        EasyMock.expect(req.getRequestURI()).andReturn(TEST_REQUEST_URI).times(1, 2);
        EasyMock.replay(req);

        FedizProcessor wfProc = new SAMLProcessorImpl();
        RedirectionResponse response = wfProc.createSignInRequest(req, config);

        String redirectionURL = response.getRedirectionURL();
        String signature =
            redirectionURL.substring(redirectionURL.indexOf("Signature=") + "Signature=".length());
        Assertions.assertTrue(signature != null && signature.length() > 0);
        String signatureAlg =
                redirectionURL.substring(redirectionURL.indexOf("SigAlg=") + "SigAlg=".length(),
                        redirectionURL.indexOf('&', redirectionURL.indexOf("SigAlg=")));
        Assertions.assertEquals(WSConstants.RSA_SHA1, URLDecoder.decode(signatureAlg, "UTF-8"));
    }

    @org.junit.jupiter.api.Test
    public void testSignedSAMLAuthnRequestSHA256() throws Exception {
        // Mock up a Request
        FedizContext config = getFederationConfigurator().getFedizContext("SIGNED_ROOT_SHA256");

        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getRequestURL()).andReturn(new StringBuffer(TEST_REQUEST_URL)).times(1, 2);
        EasyMock.expect(req.getContextPath()).andReturn(TEST_REQUEST_URI);
        EasyMock.expect(req.getRequestURI()).andReturn(TEST_REQUEST_URI).times(1, 2);
        EasyMock.replay(req);

        FedizProcessor wfProc = new SAMLProcessorImpl();
        RedirectionResponse response = wfProc.createSignInRequest(req, config);

        String redirectionURL = response.getRedirectionURL();
        String signature =
                redirectionURL.substring(redirectionURL.indexOf("Signature=") + "Signature=".length());
        Assertions.assertTrue(signature != null && signature.length() > 0);
        String signatureAlg =
                redirectionURL.substring(redirectionURL.indexOf("SigAlg=") + "SigAlg=".length(),
                        redirectionURL.indexOf('&', redirectionURL.indexOf("SigAlg=")));
        Assertions.assertEquals(WSConstants.RSA_SHA256, URLDecoder.decode(signatureAlg, "UTF-8"));
    }

    @org.junit.jupiter.api.Test
    public void createSAMLLogoutRequest() throws Exception {
        // Mock up a Request
        FedizContext config = getFederationConfigurator().getFedizContext("ROOT");

        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getRequestURL()).andReturn(new StringBuffer(TEST_REQUEST_URL)).times(1, 2);
        EasyMock.expect(req.getContextPath()).andReturn(TEST_REQUEST_URI);
        EasyMock.expect(req.getRequestURI()).andReturn(TEST_REQUEST_URI).times(1, 2);
        EasyMock.replay(req);

        FedizProcessor wfProc = new SAMLProcessorImpl();
        RedirectionResponse response = wfProc.createSignOutRequest(req, null, config);

        String redirectionURL = response.getRedirectionURL();
        String samlRequest =
            redirectionURL.substring(redirectionURL.indexOf("SAMLRequest=") + "SAMLRequest=".length(),
                                     redirectionURL.indexOf("RelayState=") - 1);

        byte[] deflatedToken = Base64.getDecoder().decode(URLDecoder.decode(samlRequest, "UTF-8"));
        InputStream tokenStream = CompressionUtils.inflate(deflatedToken);

        Document requestDoc = DOMUtils.readXml(new InputStreamReader(tokenStream, StandardCharsets.UTF_8));
        LogoutRequest request =
            (LogoutRequest)OpenSAMLUtil.fromDom(requestDoc.getDocumentElement());

        Assertions.assertEquals(TEST_REQUEST_URL, request.getIssuer().getValue());
    }

    @org.junit.jupiter.api.Test
    public void testSignedSAMLLogoutRequest() throws Exception {
        // Mock up a Request
        FedizContext config = getFederationConfigurator().getFedizContext("SIGNED_ROOT");

        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getRequestURL()).andReturn(new StringBuffer(TEST_REQUEST_URL)).times(1, 2);
        EasyMock.expect(req.getContextPath()).andReturn(TEST_REQUEST_URI);
        EasyMock.expect(req.getRequestURI()).andReturn(TEST_REQUEST_URI).times(1, 2);
        EasyMock.replay(req);

        FedizProcessor wfProc = new SAMLProcessorImpl();
        RedirectionResponse response = wfProc.createSignOutRequest(req, null, config);

        String redirectionURL = response.getRedirectionURL();
        String signature =
            redirectionURL.substring(redirectionURL.indexOf("Signature=") + "Signature=".length());
        Assertions.assertTrue(signature != null && signature.length() > 0);
    }

    @org.junit.jupiter.api.Test
    public void testCustomSAMLAuthnRequest() throws Exception {
        // Mock up a Request
        FedizContext config = getFederationConfigurator().getFedizContext("CUSTOM_REQUEST");

        HttpServletRequest req = EasyMock.createMock(HttpServletRequest.class);
        EasyMock.expect(req.getRequestURL()).andReturn(new StringBuffer(TEST_REQUEST_URL)).times(1, 2);
        EasyMock.expect(req.getContextPath()).andReturn(TEST_REQUEST_URI);
        EasyMock.expect(req.getRequestURI()).andReturn(TEST_REQUEST_URI).times(1, 2);
        EasyMock.replay(req);

        FedizProcessor wfProc = new SAMLProcessorImpl();
        RedirectionResponse response = wfProc.createSignInRequest(req, config);

        String redirectionURL = response.getRedirectionURL();
        String samlRequest =
            redirectionURL.substring(redirectionURL.indexOf("SAMLRequest=") + "SAMLRequest=".length(),
                                     redirectionURL.indexOf("RelayState=") - 1);

        byte[] deflatedToken = Base64.getDecoder().decode(URLDecoder.decode(samlRequest, "UTF-8"));
        InputStream tokenStream = CompressionUtils.inflate(deflatedToken);

        Document requestDoc = DOMUtils.readXml(new InputStreamReader(tokenStream, StandardCharsets.UTF_8));
        AuthnRequest request =
            (AuthnRequest)OpenSAMLUtil.fromDom(requestDoc.getDocumentElement());

        Assertions.assertEquals(TEST_REQUEST_URL, request.getIssuer().getValue());
        Assertions.assertEquals(TEST_REQUEST_URL, request.getAssertionConsumerServiceURL());
        Assertions.assertEquals("1.1",  request.getVersion().toString());
    }
}
