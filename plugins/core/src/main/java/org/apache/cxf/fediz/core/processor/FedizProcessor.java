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

package org.apache.cxf.fediz.core.processor;

import org.w3c.dom.Document;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.cxf.fediz.core.config.FedizContext;
import org.apache.cxf.fediz.core.exception.ProcessingException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;


public interface FedizProcessor {

    FedizResponse processRequest(
        FedizRequest request, FedizContext config
    ) throws ProcessingException;

    RedirectionResponse createSignInRequest(
        HttpServletRequest request, FedizContext config
    ) throws ProcessingException;

    RedirectionResponse createSignOutRequest(
        HttpServletRequest request, SamlAssertionWrapper token, FedizContext config
    ) throws ProcessingException;

    Document getMetaData(
        HttpServletRequest request, FedizContext config
    ) throws ProcessingException;

}
