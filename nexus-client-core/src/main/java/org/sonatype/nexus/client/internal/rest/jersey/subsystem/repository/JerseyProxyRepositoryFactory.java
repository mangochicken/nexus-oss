/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.client.internal.rest.jersey.subsystem.repository;

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.client.core.spi.subsystem.repository.RepositoryFactory;
import org.sonatype.nexus.client.core.subsystem.repository.ProxyRepository;
import org.sonatype.nexus.client.core.subsystem.repository.Repository;
import org.sonatype.nexus.client.rest.jersey.JerseyNexusClient;
import org.sonatype.nexus.rest.model.RepositoryBaseResource;
import org.sonatype.nexus.rest.model.RepositoryProxyResource;

/**
 * {@link JerseyProxyRepository} factory.
 *
 * @since 2.3
 */
@Named
@Singleton
public class JerseyProxyRepositoryFactory
    implements RepositoryFactory<ProxyRepository>
{

    @Override
    public int canAdapt( final RepositoryBaseResource resource )
    {
        int score = 0;
        if ( resource instanceof RepositoryProxyResource )
        {
            score++;
        }
        if ( JerseyProxyRepository.REPO_TYPE.equals( resource.getRepoType() ) )
        {
            score++;
        }
        return score;
    }

    @Override
    public JerseyProxyRepository adapt( final JerseyNexusClient nexusClient,
                                        final RepositoryBaseResource resource )
    {
        return new JerseyProxyRepository( nexusClient, (RepositoryProxyResource) resource );
    }

    @Override
    public boolean canCreate( final Class<? extends Repository> type )
    {
        return ProxyRepository.class.equals( type );
    }

    @Override
    public JerseyProxyRepository create( final JerseyNexusClient nexusClient, final String id )
    {
        return new JerseyProxyRepository( nexusClient, id );
    }

}
