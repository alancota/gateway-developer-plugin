/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayexport.tasks.explode.loader;

import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.Entity;
import org.w3c.dom.Element;

import static com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.EntityUtils.getEntityType;

public interface EntityLoader<E extends Entity> {

    E load(final Element element);

    Class<E> entityClass();

    default String entityType() {
        return getEntityType(entityClass());
    }
}
