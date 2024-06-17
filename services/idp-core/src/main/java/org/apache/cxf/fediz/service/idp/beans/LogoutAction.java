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
package org.apache.cxf.fediz.service.idp.beans;

import jakarta.servlet.http.HttpSession;
import org.apache.cxf.fediz.service.idp.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.webflow.execution.RequestContext;

/**
 * This class is responsible to clear security context and invalidate the IDP session.
 */
@Component
public class LogoutAction {

    private static final Logger LOG = LoggerFactory.getLogger(LogoutAction.class);

    public void submit(RequestContext requestContext) {
        SecurityContextHolder.clearContext();
        LOG.info("Security context has been cleared.");
        HttpSession session = WebUtils.getHttpSession(requestContext);
        session.invalidate();
        LOG.info("Session " + session.getId() + " has been invalidated.");
    }
}
