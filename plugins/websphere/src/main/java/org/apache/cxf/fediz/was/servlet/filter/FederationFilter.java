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
package org.apache.cxf.fediz.was.servlet.filter;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.security.auth.Subject;

import org.w3c.dom.Element;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import org.apache.cxf.fediz.core.SecurityTokenThreadLocal;
import org.apache.cxf.fediz.core.processor.FedizResponse;
import org.apache.cxf.fediz.was.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add security token to thread local
 */
public class FederationFilter extends HttpServlet implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(FederationFilter.class);
    private static final long serialVersionUID = 5732969318462358728L;

    public FederationFilter() {
        super();
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /*
     * (non-Java-doc)
     * @see jakarta.servlet.Filter#doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException {
        try {
            Subject subject = WSSubject.getCallerSubject();
            if (subject != null) {
                FedizResponse fedResponse = getCachedFederationResponse(subject);
                LOG.info("Security token found for user: {}", fedResponse.getUsername());
                Element el = fedResponse.getToken();
                if (el != null) {
                    SecurityTokenThreadLocal.setToken(el);
                    LOG.debug("Setting Security Token to SecurityTokenThreadLocal");
                }
            }
            chain.doFilter(request, response);
        } catch (WSSecurityException e) {
            LOG.warn("No caller Subject/Principal found in request.");
            chain.doFilter(request, response);
        } finally {
            SecurityTokenThreadLocal.setToken(null);
        }
    }

    private FedizResponse getCachedFederationResponse(Subject subject) {
        Iterator<?> i = subject.getPublicCredentials().iterator();
        while (i.hasNext()) {
            Object o = i.next();
            if (o instanceof Hashtable) {
                Map<?, ?> table = (Hashtable<?, ?>)o;
                return (FedizResponse)table.get(Constants.SUBJECT_TOKEN_KEY);
            }
        }
        return null;
    }
}
