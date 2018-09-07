/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity;

import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.Entity;
import org.w3c.dom.Element;

import javax.inject.Named;

@Named("POLICY")
public class PolicyEntity implements Entity {
    private final String name;
    private final String id;
    private final String guid;
    private final String parentFolderId;
    private final String policy;
    private Element policyXML;

    public PolicyEntity(final String name, final String id, final String guid, final String parentFolderId, Element policyXML, String policy) {
        this.name = name;
        this.id = id;
        this.guid = guid;
        this.parentFolderId = parentFolderId == null || parentFolderId.isEmpty() ? null : parentFolderId;
        this.policyXML = policyXML;
        this.policy = policy;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getGuid() {
        return guid;
    }

    public Element getPolicyXML() {
        return policyXML;
    }

    public void setPolicyXML(Element policyXML) {
        this.policyXML = policyXML;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getFolderId() {
        return parentFolderId;
    }


    public String getPolicy() {
        return policy;
    }
}
