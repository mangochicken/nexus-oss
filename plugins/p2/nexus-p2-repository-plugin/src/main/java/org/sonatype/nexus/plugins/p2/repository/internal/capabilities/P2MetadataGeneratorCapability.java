/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2014 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.plugins.p2.repository.internal.capabilities;

import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.capability.Condition;
import org.sonatype.nexus.capability.Tag;
import org.sonatype.nexus.capability.Taggable;
import org.sonatype.nexus.capability.support.CapabilitySupport;
import org.sonatype.nexus.capability.support.condition.RepositoryConditions;
import org.sonatype.nexus.plugins.p2.repository.P2MetadataGenerator;
import org.sonatype.nexus.plugins.p2.repository.P2MetadataGeneratorConfiguration;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.capability.Tag.repositoryTag;
import static org.sonatype.nexus.capability.Tag.tags;
import static org.sonatype.nexus.plugins.p2.repository.internal.capabilities.P2MetadataGeneratorCapabilityDescriptor.TYPE_ID;

@Named(TYPE_ID)
public class P2MetadataGeneratorCapability
    extends CapabilitySupport<P2MetadataGeneratorConfiguration>
    implements Taggable
{

  private final P2MetadataGenerator service;

  private final RepositoryRegistry repositoryRegistry;

  @Inject
  public P2MetadataGeneratorCapability(final P2MetadataGenerator service,
                                       final RepositoryRegistry repositoryRegistry)
  {
    this.service = checkNotNull(service);
    this.repositoryRegistry = checkNotNull(repositoryRegistry);
  }

  @Override
  protected P2MetadataGeneratorConfiguration createConfig(final Map<String, String> properties) throws Exception {
    return new P2MetadataGeneratorConfiguration(properties);
  }

  @Override
  protected String renderDescription() {
    if (isConfigured()) {
      try {
        return repositoryRegistry.getRepository(getConfig().repositoryId()).getName();
      }
      catch (NoSuchRepositoryException e) {
        return getConfig().repositoryId();
      }
    }
    return null;
  }

  @Override
  public void onActivate() {
    service.addConfiguration(getConfig());
  }

  @Override
  public void onPassivate() {
    service.removeConfiguration(getConfig());
  }

  @Override
  public Condition activationCondition() {
    return conditions().logical().and(
        conditions().repository().repositoryIsInService(new RepositoryConditions.RepositoryId()
        {
          @Override
          public String get() {
            return isConfigured() ? getConfig().repositoryId() : null;
          }
        }),
        conditions().capabilities().passivateCapabilityDuringUpdate()
    );
  }

  @Override
  public Condition validityCondition() {
    return conditions().repository().repositoryExists(new RepositoryConditions.RepositoryId()
    {
      @Override
      public String get() {
        return isConfigured() ? getConfig().repositoryId() : null;
      }
    });
  }

  @Override
  public Set<Tag> getTags() {
    return tags(repositoryTag(renderDescription()));
  }

}
