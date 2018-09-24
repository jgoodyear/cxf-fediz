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

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.cxf.fediz.systests.common.AbstractTests;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;

/**
 * Some tests for SAML SSO with the Spring (4) plugin, invoking on the Fediz IdP configured for SAML SSO.
 */
public class SpringTest extends AbstractTests {

    static String idpHttpsPort;
    static String rpHttpsPort;

    private static Tomcat idpServer;
    private static Tomcat rpServer;

    @BeforeClass
    public static void init() throws Exception {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");

        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");

        System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire", "debug");

        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");

        idpHttpsPort = System.getProperty("idp.https.port");
        Assert.assertNotNull("Property 'idp.https.port' null", idpHttpsPort);
        rpHttpsPort = System.getProperty("rp.spring.https.port");
        Assert.assertNotNull("Property 'rp.spring.https.port' null", rpHttpsPort);

        idpServer = startServer(true, idpHttpsPort);
        rpServer = startServer(false, rpHttpsPort);
    }

    @AfterClass
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

    private static Tomcat startServer(boolean idp, String port)
        throws ServletException, LifecycleException, IOException {
        Tomcat server = new Tomcat();
        server.setPort(0);
        String currentDir = new File(".").getCanonicalPath();
        String baseDir = currentDir + File.separator + "target";
        server.setBaseDir(baseDir);

        if (idp) {
            server.getHost().setAppBase("tomcat/idp/webapps");
        } else {
            server.getHost().setAppBase("tomcat/rp/webapps");
        }
        server.getHost().setAutoDeploy(true);
        server.getHost().setDeployOnStartup(true);

        Connector httpsConnector = new Connector();
        httpsConnector.setPort(Integer.parseInt(port));
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");
        httpsConnector.setAttribute("keyAlias", "mytomidpkey");
        httpsConnector.setAttribute("keystorePass", "tompass");
        httpsConnector.setAttribute("keystoreFile", "test-classes/server.jks");
        httpsConnector.setAttribute("truststorePass", "tompass");
        httpsConnector.setAttribute("truststoreFile", "test-classes/server.jks");
        httpsConnector.setAttribute("clientAuth", "want");
        // httpsConnector.setAttribute("clientAuth", "false");
        httpsConnector.setAttribute("sslProtocol", "TLS");
        httpsConnector.setAttribute("SSLEnabled", true);

        server.getService().addConnector(httpsConnector);

        if (idp) {
            File stsWebapp = new File(baseDir + File.separator + server.getHost().getAppBase(), "fediz-idp-sts");
            server.addWebapp("/fediz-idp-sts", stsWebapp.getAbsolutePath());

            File idpWebapp = new File(baseDir + File.separator + server.getHost().getAppBase(), "fediz-idp");
            server.addWebapp("/fediz-idp", idpWebapp.getAbsolutePath());
        } else {
            File rpWebapp = new File(baseDir + File.separator + server.getHost().getAppBase(),
                                     "fediz-systests-webapps-spring");
            server.addWebapp("/fedizhelloworldspring", rpWebapp.getAbsolutePath());
        }

        server.start();

        return server;
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
        return "fedizhelloworldspring";
    }

    @Override
    protected boolean isWSFederation() {
        return false;
    }

    @Ignore("This tests is currently failing on Spring")
    @Override
    public void testConcurrentRequests() throws Exception {
        // super.testConcurrentRequests();
    }

    @Ignore("This tests is currently failing on Spring")
    public void testRPLogout() throws Exception {
        //
    }

    @Override
    @org.junit.Test
    public void testCSRFAttack() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/j_spring_fediz_security_check";
        csrfAttackTest(url);
    }

    @Override
    @org.junit.Test
    public void testCSRFAttack2() throws Exception {
        String url = "https://localhost:" + getRpHttpsPort() + "/" + getServletContextName()
            + "/j_spring_fediz_security_check";
        csrfAttackTest2(url);
    }

}