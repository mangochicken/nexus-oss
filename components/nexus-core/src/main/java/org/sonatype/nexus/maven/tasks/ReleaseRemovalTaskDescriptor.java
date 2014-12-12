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

import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.formfields.FormField;
import org.sonatype.nexus.formfields.NumberTextFormField;
import org.sonatype.nexus.formfields.RepoTargetComboFormField;
import org.sonatype.nexus.formfields.RepositoryCombobox;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskDescriptorSupport;

/**
 * @since 2.5
 */
@Named
@Singleton
public class ReleaseRemovalTaskDescriptor
    extends TaskDescriptorSupport
{
  public static final String NUMBER_OF_VERSIONS_TO_KEEP_FIELD_ID = "numberOfVersionsToKeep";

  public static final String REPOSITORY_TARGET_FIELD_ID = "repositoryTarget";

  public ReleaseRemovalTaskDescriptor() {
    super(ReleaseRemovalTask.class, "Remove Releases From Repository",
        new RepositoryCombobox(
            TaskConfiguration.REPOSITORY_ID_KEY,
            "Repository",
            "Select Maven repository to remove releases.",
            FormField.MANDATORY
        ).includingAnyOfContentClasses(Maven2ContentClass.ID).excludingAnyOfFacets(GroupRepository.class),
        new NumberTextFormField(
            NUMBER_OF_VERSIONS_TO_KEEP_FIELD_ID, "Number to keep", "The number of versions for each GA to keep",
            FormField.MANDATORY),
        new RepoTargetComboFormField(REPOSITORY_TARGET_FIELD_ID, "Repository Target",
            "Select a repository target to apply", FormField.OPTIONAL)
    );
  }
}
