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

package org.sonatype.nexus.maven.tasks;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.repository.ProxyRepository;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * @since 2.7.0
 */
@Named
@Singleton
public class UnusedSnapshotRemoverTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String DAYS_SINCE_LAST_REQUESTED_FIELD_ID = "daysSinceLastRequested";

  @Inject
  public UnusedSnapshotRemoverTaskDescriptor()
  {
    super(UnusedSnapshotRemoverTask.class, "Remove Unused Snapshots From Repository",
        new RepositoryCombobox(
            TaskConfiguration.REPOSITORY_ID_KEY,
            "Repository",
            "Select the Maven repository to remove unused snapshots.",
            FormField.MANDATORY
        ).includeAnEntryForAllRepositories()
            .includingAnyOfContentClasses(Maven2ContentClass.ID)
            .excludingAnyOfFacets(ProxyRepository.class),
        new NumberTextFormField(
            DAYS_SINCE_LAST_REQUESTED_FIELD_ID,
            "Days since last requested",
            "The job will purge all snapshots that were last time requested from Nexus before the specified number of days",
            FormField.MANDATORY
        )
    );
  }
}
