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
package org.sonatype.nexus.component.services.internal.storage;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.sonatype.nexus.orient.DatabaseInstance;
import org.sonatype.nexus.orient.DatabaseManager;

import static com.google.common.base.Preconditions.checkNotNull;

public class ComponentMetadataDatabase
{
  public static final String NAME = "componentMetadata";

  /**
   * Shared {@code componentMetadata} database instance provider.
   *
   * @since 3.0
   */
  @Named(NAME)
  @Singleton
  public static class ProviderImpl
      implements Provider<DatabaseInstance>
  {
    private final DatabaseManager databaseManager;

    @Inject
    public ProviderImpl(final DatabaseManager databaseManager) {
      this.databaseManager = checkNotNull(databaseManager);
    }

    @Override
    public DatabaseInstance get() {
      return databaseManager.instance(NAME);
    }
  }
}
