/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.checkstyle.resource;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Map;

import org.codehaus.plexus.resource.DefaultResourceManager;
import org.codehaus.plexus.resource.PlexusResource;
import org.codehaus.plexus.resource.loader.ResourceLoader;
import org.codehaus.plexus.resource.loader.ResourceNotFoundException;
import org.codehaus.plexus.resource.loader.ThreadContextClasspathResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * License resource manager, to avoid defaulting license to maven-checkstyle-plugin's own license.
 *
 * @since 2.12
 */
@Named("license")
public class LicenseResourceManager extends DefaultResourceManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseResourceManager.class);

    private final Map<String, ResourceLoader> resourceLoaders;

    @Inject
    public LicenseResourceManager(Map<String, ResourceLoader> resourceLoaders) {
        super(resourceLoaders);
        this.resourceLoaders = resourceLoaders;
    }

    @Override
    public void addSearchPath(String id, String path) {
        ResourceLoader loader = resourceLoaders.get(id);

        if (loader == null) {
            throw new IllegalArgumentException("unknown resource loader: " + id);
        }

        loader.addSearchPath(path);
    }

    @Override
    public PlexusResource getResource(String name) throws ResourceNotFoundException {
        for (ResourceLoader resourceLoader : resourceLoaders.values()) {
            if (resourceLoader instanceof ThreadContextClasspathResourceLoader
                    && !"config/maven-header.txt".equals(name)) {
                // MCHECKSTYLE-219: Don't load the license from the plugin
                // classloader, only allow config/maven-header.txt
                continue;
            }

            try {
                PlexusResource resource = resourceLoader.getResource(name);

                LOGGER.debug("The resource '" + name + "' was found as " + resource.getName() + ".");

                return resource;
            } catch (ResourceNotFoundException e) {
                LOGGER.debug("The resource '" + name + "' was not found with resourceLoader "
                        + resourceLoader.getClass().getName() + ".");
            }
        }

        throw new ResourceNotFoundException(name);
    }
}
