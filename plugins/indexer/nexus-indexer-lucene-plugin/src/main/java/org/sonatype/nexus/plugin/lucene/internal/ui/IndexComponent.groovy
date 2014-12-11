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

package org.sonatype.nexus.plugin.lucene.internal.ui

import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.TaskConfiguration

import com.softwarementors.extjs.djn.config.annotations.DirectAction
import com.softwarementors.extjs.djn.config.annotations.DirectMethod
import org.apache.shiro.authz.annotation.RequiresAuthentication
import org.apache.shiro.authz.annotation.RequiresPermissions
import org.hibernate.validator.constraints.NotEmpty
import org.sonatype.nexus.extdirect.DirectComponent
import org.sonatype.nexus.extdirect.DirectComponentSupport
import org.sonatype.nexus.index.tasks.RepairIndexTask
import org.sonatype.nexus.index.tasks.UpdateIndexTask
import org.sonatype.nexus.proxy.maven.MavenRepository
import org.sonatype.nexus.proxy.registry.RepositoryRegistry
import org.sonatype.nexus.proxy.repository.Repository
import org.sonatype.nexus.validation.Validate

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Lucene Index {@link DirectComponent}.
 *
 * @since 3.0
 */
@Named
@Singleton
@DirectAction(action = 'indexerLucene_Index')
class IndexComponent
extends DirectComponentSupport
{

  @Inject
  @Named("protected")
  RepositoryRegistry protectedRepositoryRegistry

  @Inject
  TaskScheduler nexusScheduler

  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:index:delete')
  @Validate
  void repair(final @NotEmpty(message = '[id] may not be empty') String id,
              final String path)
  {
    Repository repository = protectedRepositoryRegistry.getRepositoryWithFacet(id, MavenRepository)

    TaskConfiguration task = nexusScheduler.createTaskConfigurationInstance(RepairIndexTask.class)
    task.setRepositoryId(id)
    task.setPath(path ?: '/')
    task.setName("Repair ${repository.name} index")

    nexusScheduler.submit(task)
  }

  @DirectMethod
  @RequiresAuthentication
  @RequiresPermissions('nexus:index:delete')
  @Validate
  void update(final @NotEmpty(message = '[id] may not be empty') String id,
              final String path)
  {
    Repository repository = protectedRepositoryRegistry.getRepositoryWithFacet(id, MavenRepository)

    TaskConfiguration task = nexusScheduler.createTaskConfigurationInstance(UpdateIndexTask)
    task.setRepositoryId(id)
    task.setPath(path ?: '/')
    task.setName("Update ${repository.name} index")

    nexusScheduler.submit(task)
  }

}
