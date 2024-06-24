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

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import org.apache.cxf.fediz.service.idp.domain.FederationType;
import org.apache.cxf.fediz.service.idp.domain.TrustType;
import org.apache.openjpa.persistence.jdbc.Index;


@Entity(name = "TrustedIDP")
public class TrustedIdpEntity {

    @Id
    private int id;

    //@Column(name = "REALM", nullable = true, length = FIELD_LENGTH)
    @Index(unique = true)
    @NotNull
    private String realm;  //wtrealm, whr

    private String issuer;  //Validation of issuer name in SAMLResponse

    // Should tokens be cached from trusted IDPs
    // to avoid redirection to the trusted IDP again for next SignIn request
    private boolean cacheTokens;

    //Could be read from Metadata, PassiveRequestorEndpoint
    @NotNull
    private String url;

    //Could be read from Metadata, md:KeyDescriptor, use="signing"
    //Store certificate in DB or filesystem, provide options?
    private String certificate;

    //Direct trust (signing cert imported), Indirect trust (CA certs imported, subject configured)
    @Enumerated(EnumType.STRING)
    private TrustType trustType;

    //Could be read from Metadata, RoleDescriptor protocolSupportEnumeration=
    // "http://docs.oasis-open.org/wsfed/federation/200706"
    // Metadata could provide more than one but one must be chosen
    @TrustedIdpProtocolSupported
    private String protocol;

    //FederateIdentity, FederateClaims
    @Enumerated(EnumType.STRING)
    private FederationType federationType;

    //optional (to provide a list of IDPs)
    @NotNull
    private String name;

    //optional (to provide a list of IDPs)
    private String description;

    //optional (to provide a list of IDPs)
    private String logo;

    // Additional (possibly protocol specific parameters)
    @ElementCollection
    @MapKeyColumn(name = "name")
    @Column(name = "value")
    @CollectionTable(name = "trusted_idp_parameters", joinColumns = @JoinColumn(name = "trusted_idp_id"))
    private Map<String, String> parameters = new HashMap<>();


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public boolean isCacheTokens() {
        return cacheTokens;
    }

    public void setCacheTokens(boolean cacheTokens) {
        this.cacheTokens = cacheTokens;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public FederationType getFederationType() {
        return federationType;
    }

    public void setFederationType(FederationType federationType) {
        this.federationType = federationType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public TrustType getTrustType() {
        return trustType;
    }

    public void setTrustType(TrustType trustType) {
        this.trustType = trustType;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

}
