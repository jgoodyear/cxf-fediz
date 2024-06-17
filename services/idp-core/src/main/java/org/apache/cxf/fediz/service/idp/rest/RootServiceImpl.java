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

package org.apache.cxf.fediz.service.idp.rest;

import java.net.URI;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;


public class RootServiceImpl implements RootService {

    public RootServiceImpl() {
    }

    public Response head(UriInfo uriInfo) {
        UriBuilder absolute = uriInfo.getBaseUriBuilder();
        URI claimUrl = absolute.clone().path("claims").build();
        URI idpUrl = absolute.clone().path("idps").build();
        URI applicationUrl = absolute.clone().path("applications").build();
        URI trustedIdpUrl = absolute.clone().path("trusted-idps").build();
        URI rolesUrl = absolute.clone().path("roles").build();
        URI entitlementsUrl = absolute.clone().path("entitlements").build();
        jakarta.ws.rs.core.Link claims = jakarta.ws.rs.core.Link.fromUri(claimUrl).rel("claims")
            .type("application/xml").build();
        jakarta.ws.rs.core.Link idps = jakarta.ws.rs.core.Link.fromUri(idpUrl).rel("idps")
            .type("application/xml").build();
        jakarta.ws.rs.core.Link applications = jakarta.ws.rs.core.Link.fromUri(applicationUrl).rel("applications")
            .type("application/xml").build();
        jakarta.ws.rs.core.Link trustedIdps = jakarta.ws.rs.core.Link.fromUri(trustedIdpUrl).rel("trusted-idps")
            .type("application/xml").build();
        jakarta.ws.rs.core.Link roles = jakarta.ws.rs.core.Link.fromUri(rolesUrl).rel("roles")
            .type("application/xml").build();
        jakarta.ws.rs.core.Link entitlements = jakarta.ws.rs.core.Link.fromUri(entitlementsUrl).rel("entitlements")
            .type("application/xml").build();

        Response.ResponseBuilder builder = Response.ok().links(
            claims, idps, applications, trustedIdps, roles, entitlements);
        return builder.build();
    }

}
