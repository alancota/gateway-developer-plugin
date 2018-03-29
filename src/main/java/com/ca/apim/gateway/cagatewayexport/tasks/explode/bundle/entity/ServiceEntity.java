/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity;

import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.Entity;
import org.w3c.dom.Element;

public class ServiceEntity implements Entity {
    private final String name;
    private final String id;
    private final String parentFolderId;
    private final Element serviceDetailsElement;
    private final String policy;
    private final Element serviceXML;

    public ServiceEntity(final String name, final String id, final String parentFolderId, Element serviceXML, Element serviceDetailsElement, String policy) {
        this.name = name;
        this.id = id;
        this.parentFolderId = parentFolderId == null || parentFolderId.isEmpty() ? null : parentFolderId;
        this.serviceXML = serviceXML;
        this.serviceDetailsElement = serviceDetailsElement;
        this.policy = policy;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Element getXml() {
        return serviceXML;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getFolderId() {
        return parentFolderId;
    }

    public Element getServiceDetailsElement() {
        return serviceDetailsElement;
    }

    public String getPolicy() {
        return policy;
    }

}
