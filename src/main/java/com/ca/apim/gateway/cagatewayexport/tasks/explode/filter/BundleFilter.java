/*
 * Copyright (c) 2018 CA. All rights reserved.
 * This software may be modified and distributed under the terms
 * of the MIT license.  See the LICENSE file for details.
 */

package com.ca.apim.gateway.cagatewayexport.tasks.explode.filter;

import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.Bundle;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.Entity;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.*;
import com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.StoredPasswordEntity.Type;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.ListenPortEntity.DEFAULT_PORTS;
import static com.ca.apim.gateway.cagatewayexport.tasks.explode.bundle.entity.PrivateKeyEntity.SSL_DEFAULT_PRIVATE_KEY;

public class BundleFilter {
    private final Bundle bundle;

    public BundleFilter(Bundle bundle) {
        this.bundle = bundle;
    }

    public Bundle filter(String folderPath) {
        Bundle filteredBundle = new Bundle();

        //filter folders
        filterFolders(folderPath, bundle.getFolderTree()).forEach(filteredBundle::addEntity);

        //filter services
        filterServices(filteredBundle.getEntities(Folder.class), bundle.getEntities(ServiceEntity.class).values()).forEach(filteredBundle::addEntity);

        //filter policies
        filterPolicies(filteredBundle.getEntities(Folder.class), bundle.getEntities(PolicyEntity.class).values()).forEach(filteredBundle::addEntity);

        //filter encass
        filterEncasses(filteredBundle.getEntities(PolicyEntity.class), bundle.getEntities(EncassEntity.class).values()).forEach(filteredBundle::addEntity);

        //filter pbs
        filterPBS(filteredBundle.getEntities(PolicyEntity.class), bundle.getEntities(PolicyBackedServiceEntity.class).values()).forEach(filteredBundle::addEntity);

        //filter IDP
        filterIDP(bundle.getEntities(IdentityProviderEntity.class).values()).forEach(filteredBundle::addEntity);

        //filter cluster property
        filterStaticProperties(
                bundle.getEntities(ClusterProperty.class).values(),
                filterDependencies(ClusterProperty.class, bundle.getDependencies(), filteredBundle)
        ).forEach(filteredBundle::addEntity);

        // filter listen ports
        filterListenPorts(bundle.getEntities(ListenPortEntity.class).values()).forEach(filteredBundle::addEntity);

        // filter trusted certs
        bundle.getEntities(TrustedCertEntity.class).values().forEach(filteredBundle::addEntity);

        // filter stored passwords
        filterStoredPasswords(bundle.getEntities(StoredPasswordEntity.class).values()).forEach(filteredBundle::addEntity);

        // filtering jdbc connections using only the ones defined as policy dependencies
        filterJdbcConnections(
                bundle.getEntities(JdbcConnectionEntity.class).values(),
                filterDependencies(JdbcConnectionEntity.class, bundle.getDependencies(), filteredBundle)
        ).forEach(filteredBundle::addEntity);

        // filter private keys removing the default SSL one
        bundle.getEntities(PrivateKeyEntity.class).values().stream().filter(p -> !p.getName().equals(SSL_DEFAULT_PRIVATE_KEY)).forEach(filteredBundle::addEntity);

        filterParentFolders(folderPath, bundle.getFolderTree()).forEach(filteredBundle::addEntity);

        FolderTree folderTree = new FolderTree(filteredBundle.getEntities(Folder.class).values());
        filteredBundle.setFolderTree(folderTree);
        return filteredBundle;
    }

    private List<StoredPasswordEntity> filterStoredPasswords(Collection<StoredPasswordEntity> values) {
        return values.stream().filter(e -> e.isType(Type.PASSWORD)).collect(Collectors.toList());
    }

    private List<IdentityProviderEntity> filterIDP(Collection<IdentityProviderEntity> values) {
        return values.stream().filter(idp -> !IdentityProviderEntity.INTERNAL_IDP_ID.equals(idp.getId())).collect(Collectors.toList());
    }

    private List<ClusterProperty> filterStaticProperties(Collection<ClusterProperty> clusterProperties, Set<Dependency> filteredDependencies) {
        return clusterProperties.stream()
                .filter(c -> filteredDependencies.contains(new Dependency(c.getId(), ClusterProperty.class)))
                .collect(Collectors.toList());
    }

    private List<PolicyBackedServiceEntity> filterPBS(Map<String, PolicyEntity> policies, Collection<PolicyBackedServiceEntity> encasses) {
        return encasses.stream().filter(pbs -> pbs.getOperations().values().stream().anyMatch(policies::containsKey)).collect(Collectors.toList());
    }

    private List<EncassEntity> filterEncasses(Map<String, PolicyEntity> policies, Collection<EncassEntity> encasses) {
        return encasses.stream().filter(e -> policies.containsKey(e.getPolicyId())).collect(Collectors.toList());
    }

    private List<PolicyEntity> filterPolicies(Map<String, Folder> folders, Collection<PolicyEntity> policies) {
        return policies.stream().filter(p -> folders.containsKey(p.getFolderId())).collect(Collectors.toList());
    }

    private List<ServiceEntity> filterServices(Map<String, Folder> folders, Collection<ServiceEntity> services) {
        return services.stream().filter(s -> folders.containsKey(s.getFolderId())).collect(Collectors.toList());
    }

    private List<Folder> filterFolders(String folderPath, FolderTree folderTree) {
        return folderTree.stream().filter(f -> {
            Path path = folderTree.getPath(f);
            return ("/" + path.toString()).startsWith(folderPath);
        }).collect(Collectors.toList());
    }

    private List<Folder> filterParentFolders(String folderPath, FolderTree folderTree) {
        return folderTree.stream().filter(f -> {
            Path path = folderTree.getPath(f);
            return folderPath.startsWith("/" + path.toString());
        }).collect(Collectors.toList());
    }

    private List<ListenPortEntity> filterListenPorts(Collection<ListenPortEntity> listenPorts) {
        return listenPorts.stream().filter(l -> !DEFAULT_PORTS.contains(l.getPort())).collect(Collectors.toList());
    }

    private List<JdbcConnectionEntity> filterJdbcConnections(Collection<JdbcConnectionEntity> jdbcConnections, Set<Dependency> filteredDependencies) {
        return jdbcConnections.stream()
                .filter(c -> filteredDependencies.contains(new Dependency(c.getId(), JdbcConnectionEntity.class)))
                .collect(Collectors.toList());
    }

    private static <E extends Entity> Set<Dependency> filterDependencies(Class<E> dependencyType, Map<Dependency, List<Dependency>> dependencies, Bundle filteredBundle) {
        return dependencies.entrySet().stream()
                .filter(e -> filteredBundle.getEntities(e.getKey().getType()).get(e.getKey().getId()) != null)
                .flatMap(e -> e.getValue().stream())
                .filter(d -> d.getType() == dependencyType)
                .collect(Collectors.toSet());
    }
}
