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
package org.apache.cxf.fediz.service.idp.service.jpa;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.apache.openjpa.persistence.jdbc.Index;


@Entity(name = "Application")
public class ApplicationEntity {

    @Id
    private int id;

    @Index(unique = true)
    @NotNull
    private String realm;  //wtrealm, whr

    //Could be read from Metadata, RoleDescriptor protocolSupportEnumeration=
    // "http://docs.oa14sis-open.org/wsfed/federation/200706"
    // Metadata could provide more than one but one must be chosen
    @NotNull
    @ApplicationProtocolSupported
    private String protocol;

    // Public key only
    // Could be read from Metadata, md:KeyDescriptor, use="encryption"
    private String encryptionCertificate;

    // Certificate for Signature verification
    private String validatingCertificate;

    // Could be read from Metadata, fed:ClaimTypesRequested
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationClaimEntity> requestedClaims = new ArrayList<>();

    //Could be read from Metadata, ServiceDisplayName
    //usage for list of application where user is logged in
    @NotNull
    private String serviceDisplayName;

    //Could be read from Metadata, ServiceDescription
    //usage for list of application where user is logged in
    private String serviceDescription;

    //Could be read from Metadata, RoleDescriptor
    //fed:ApplicationServiceType, fed:SecurityTokenServiceType
    private String role;

    // Not in Metadata, configured in IDP or passed in wreq parameter
    @NotNull
    private String tokenType;

    // Not in Metadata, configured in IDP or passed in wreq parameter
    @Min(value = 1)
    private int lifeTime;

    // Request audience restriction in token for this application (default is true)
    private boolean enableAppliesTo = true;

    // WS-Policy Namespace in SignIn Response
    private String policyNamespace;

    private String passiveRequestorEndpoint;

    // A regular expression constraint on the passiveRequestorEndpoint
    private String passiveRequestorEndpointConstraint;

    private String logoutEndpoint;

    // A regular expression constraint on the logoutEndpoint
    private String logoutEndpointConstraint;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getEncryptionCertificate() {
        return encryptionCertificate;
    }

    public void setEncryptionCertificate(String encryptionCertificate) {
        this.encryptionCertificate = encryptionCertificate;
    }

    public List<ApplicationClaimEntity> getRequestedClaims() {
        return requestedClaims;
    }

    public void setRequestedClaims(List<ApplicationClaimEntity> requestedClaims) {
        this.requestedClaims = requestedClaims;
    }

    public String getServiceDisplayName() {
        return serviceDisplayName;
    }

    public void setServiceDisplayName(String serviceDisplayName) {
        this.serviceDisplayName = serviceDisplayName;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public int getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    public String getPolicyNamespace() {
        return policyNamespace;
    }

    public void setPolicyNamespace(String policyNamespace) {
        this.policyNamespace = policyNamespace;
    }

    public String getPassiveRequestorEndpoint() {
        return passiveRequestorEndpoint;
    }

    public void setPassiveRequestorEndpoint(String passiveRequestorEndpoint) {
        this.passiveRequestorEndpoint = passiveRequestorEndpoint;
    }

    public String getPassiveRequestorEndpointConstraint() {
        return passiveRequestorEndpointConstraint;
    }

    public void setPassiveRequestorEndpointConstraint(String passiveRequestorEndpointConstraint) {
        this.passiveRequestorEndpointConstraint = passiveRequestorEndpointConstraint;
    }

    public String getValidatingCertificate() {
        return validatingCertificate;
    }

    public void setValidatingCertificate(String validatingCertificate) {
        this.validatingCertificate = validatingCertificate;
    }

    public boolean isEnableAppliesTo() {
        return enableAppliesTo;
    }

    public void setEnableAppliesTo(boolean enableAppliesTo) {
        this.enableAppliesTo = enableAppliesTo;
    }

    public String getLogoutEndpoint() {
        return logoutEndpoint;
    }

    public void setLogoutEndpoint(String logoutEndpoint) {
        this.logoutEndpoint = logoutEndpoint;
    }

    public String getLogoutEndpointConstraint() {
        return logoutEndpointConstraint;
    }

    public void setLogoutEndpointConstraint(String logoutEndpointConstraint) {
        this.logoutEndpointConstraint = logoutEndpointConstraint;
    }
}
