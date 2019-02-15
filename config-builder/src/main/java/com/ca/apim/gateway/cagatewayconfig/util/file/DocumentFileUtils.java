/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.util.file;

import com.ca.apim.gateway.cagatewayconfig.util.xml.DocumentTools;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.ca.apim.gateway.cagatewayconfig.util.file.FileUtils.closeQuietly;

public class DocumentFileUtils {

    public static final DocumentFileUtils INSTANCE = new DocumentFileUtils(DocumentTools.INSTANCE);
    private final DocumentTools documentTools;

    private DocumentFileUtils(DocumentTools documentTools) {
        this.documentTools = documentTools;
    }

    public void createFile(Element element, Path path) {
        createFile(element, path, false);
    }

    public void createFile(Element element, Path path, boolean addNamespace) {
        OutputStream fos = null;
        try {
            fos = Files.newOutputStream(path);
            documentTools.printXML(element, fos, addNamespace);
        } catch (IOException e) {
            throw new DocumentFileUtilsException("Error writing to file '" + path + "': " + e.getMessage(), e);
        } finally {
            closeQuietly(fos);
        }
    }
}
