/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayconfig.util.gateway;

import com.ca.apim.gateway.cagatewayconfig.tasks.gw7.PackageBuildException;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The packager build a Gateway Deployment Package. A GW7 file is to be able to package bundles, aars, bootstrap
 * scripts, solution kits, etc... all into a single file. This helps make distributing and deploying solutions simpler,
 * a single file to download, a single volume to mount.
 * <p>
 * Here is an example of a GW7 file structure:
 * <pre>
 * opt/
 *   - docker/rc.d/
 *     - apply-environment.sh
 *   - SecureSpan/Gateway/
 *     - node/default/etc/bootstrap/bundle/
 *       - 1-my-bundle-1.0.00.req.bundle
 *       - custom-assertion.req.bundle
 *       - gateway-developer-example.req.bundle
 *       - helloworld.req.bundle
 *     - runtime/modules/
 *       - assertions/
 *         - Hello-World-Assertion-0.1.01.aar
 *       - lib/
 *         - custom-assertion-1.0.0.jar
 * </pre>
 * The above package will add:
 * <ul>
 * <li>a bootstrap script apply-environment.sh</li>
 * <li>4 bundle: 1-my-bundle-1.0.00.req.bundle, custom-assertion.req.bundle, gateway-developer-example.req.bundle, helloworld.req.bundle</li>
 * <li>a modular assertion: Hello-World-Assertion-0.1.01.aar</li>
 * <li>a custom assertion: custom-assertion-1.0.0.jar</li>
 * </ul>
 */
public class GW7Builder {
    public static final GW7Builder INSTANCE = new GW7Builder();

    public void buildPackage(OutputStream gw7FileOutputStream, Set<PackageFile> packageFiles) {
        try (TarArchiveOutputStream taos = new TarArchiveOutputStream(new GzipCompressorOutputStream(gw7FileOutputStream))) {
            writeFiles(taos, packageFiles);
        } catch (IOException e) {
            throw new PackageBuildException("Error building GW7 Package: " + e.getMessage(), e);
        }
    }

    private void writeFiles(TarArchiveOutputStream taos, Set<PackageFile> packageFiles) {
        for (PackageFile file : packageFiles) {
            try (InputStream inputStream = file.fileStreamSupplier.get()) {
                TarArchiveEntry tarEntry = new TarArchiveEntry(file.filePath);
                tarEntry.setSize(file.fileSize);
                taos.putArchiveEntry(tarEntry);
                IOUtils.copy(inputStream, taos);
                taos.closeArchiveEntry();
            } catch (IOException e) {
                throw new PackageBuildException("Error building GW7 Package. Error adding file: " + file.filePath + " Message: " + e.getMessage(), e);
            }
        }
    }

    public static class PackageFile {
        private final String filePath;
        private final long fileSize;
        private final Supplier<InputStream> fileStreamSupplier;

        public PackageFile(String filePath, long fileSize, Supplier<InputStream> fileStreamSupplier) {
            this.filePath = filePath;
            this.fileSize = fileSize;
            this.fileStreamSupplier = fileStreamSupplier;
        }
    }
}
