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
package org.apache.cxf.fediz.cxf.plugin.state;

import java.io.Serializable;
import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.apache.cxf.fediz.core.Claim;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ResponseState implements Serializable {

    private static final long serialVersionUID = -3247188797004342462L;

    private String assertion;
    private String state;
    private String webAppContext;
    private String webAppDomain;
    private long createdAt;
    private long expiresAt;
    private List<String> roles;
    private String issuer;
    private List<Claim> claims;
    private String subject;

    public ResponseState() {

    }

    public ResponseState(String assertion,
                         String state,
                         String webAppContext,
                         String webAppDomain,
                         long createdAt,
                         long expiresAt) {
        this.assertion = assertion;
        this.state = state;
        this.webAppContext = webAppContext;
        this.webAppDomain = webAppDomain;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public String getState() {
        return state;
    }

    public String getWebAppContext() {
        return webAppContext;
    }

    public String getWebAppDomain() {
        return webAppDomain;
    }

    public String getAssertion() {
        return assertion;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<Claim> getClaims() {
        return claims;
    }

    public void setClaims(List<Claim> claims) {
        this.claims = claims;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}
