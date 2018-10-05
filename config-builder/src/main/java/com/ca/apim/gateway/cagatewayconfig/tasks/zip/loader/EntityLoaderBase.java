/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.tasks.zip.loader;

import com.ca.apim.gateway.cagatewayconfig.tasks.zip.beans.Bundle;
import com.ca.apim.gateway.cagatewayconfig.util.json.JsonTools;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.MapType;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base loader for all entities with information stored in yaml or json files.
 *
 * @param <B> type of entity
 */
public abstract class EntityLoaderBase<B> implements EntityLoader {

    private final JsonTools jsonTools;

    EntityLoaderBase(final JsonTools jsonTools) {
        this.jsonTools = jsonTools;
    }

    @Override
    public void load(final Bundle bundle, final File rootDir) {
        final File entitiesFile = jsonTools.getDocumentFileFromConfigDir(rootDir, getFileName());
        if (entitiesFile == null) {
            // no file
            return;
        }
        final String fileType = jsonTools.getTypeFromFile(entitiesFile);
        final MapType type = jsonTools.getObjectMapper(fileType).getTypeFactory().constructMapType(LinkedHashMap.class, String.class, this.getBeanClass());

        final Map<String, B> entities = jsonTools.readDocumentFile(entitiesFile, type);
        if (entities != null) {
            putToBundle(bundle, entities);
        }
    }

    @Override
    public void load(Bundle bundle, String fullName, String value) {
        int extensionIndex = fullName.lastIndexOf('.');
        String valueType = null;
        String name;
        if (extensionIndex > 0) {
            valueType = jsonTools.getTypeFromExtension(fullName.substring(extensionIndex + 1));
        }
        if (valueType == null) {
            valueType = getDefaultValueType();
            name = fullName;
        } else {
            name = fullName.substring(0, extensionIndex);
        }

        final JavaType type = jsonTools.getObjectMapper(valueType).getTypeFactory().constructType(this.getBeanClass());

        B entity = jsonTools.readStream(IOUtils.toInputStream(value, Charset.defaultCharset()), valueType, type);
        putToBundle(bundle, ImmutableMap.<String, B>builder().put(name, entity).build());
    }

    /**
     * Returns the default value type for this entity. For example JSON, YAML, etc...
     *
     * @return The default value type
     */
    protected String getDefaultValueType() {
        return JsonTools.JSON;
    }

    /**
     * @return the class of the bean loaded by the concrete implementation of this loader
     */
    protected abstract Class<B> getBeanClass();

    /**
     * @return the file name specified for this type of entity
     */
    protected abstract String getFileName();

    /**
     * Put the entities to the bundle.
     *
     * @param bundle      the bundle to add
     * @param entitiesMap non-null map of entities read from the json/yaml files
     */
    protected abstract void putToBundle(final Bundle bundle, @NotNull final Map<String, B> entitiesMap);

}
