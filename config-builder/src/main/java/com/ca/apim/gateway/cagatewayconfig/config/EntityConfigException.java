/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.config;

public class EntityConfigException extends RuntimeException {

    public EntityConfigException(String message) {
        super(message);
    }

    public EntityConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
