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

import jakarta.servlet.http.Cookie;
import org.apache.cxf.fediz.service.idp.domain.Idp;
import org.apache.cxf.fediz.service.idp.util.WebUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.webflow.execution.RequestContext;

/**
 * This class is responsible to process Home Realm Discovery Service Expression.
 */
@Component
public class ProcessHRDSExpressionAction {

    private static final String IDP_CONFIG = "idpConfig";

    private static final Logger LOG = LoggerFactory.getLogger(ProcessHRDSExpressionAction.class);

    @Autowired
    private HomeRealmReminder homeRealmReminder;

    public String submit(RequestContext context, String homeRealm) {
        // Check if home realm is known already
        Cookie homeRealmCookie = homeRealmReminder.readCookie(context);
        if (homeRealmCookie != null) {
            LOG.debug("Home Realm Cookie set: {}", homeRealmCookie);
            return homeRealmCookie.getValue();
        }

        // Check if custom HRDS is defined
        Idp idpConfig = (Idp)WebUtils.getAttributeFromFlowScope(context, IDP_CONFIG);
        String hrds = idpConfig.getHrds();

        if (hrds != null) {
            LOG.debug("HomeRealmDiscoveryService EL: {}", hrds);
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(hrds);
            String result = exp.getValue(context, String.class);
            LOG.info("Realm resolved by HomeRealmDiscoveryService: {}", result);
            return result;
        }

        // Return home realm parameter unchanged
        LOG.debug("No custom homeRealm handling, using home realm parameter as provided in request: {}", homeRealm);
        return homeRealm;
    }
}
