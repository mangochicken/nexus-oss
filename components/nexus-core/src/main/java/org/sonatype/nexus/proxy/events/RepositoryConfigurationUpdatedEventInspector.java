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
package org.sonatype.nexus.proxy.events;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.events.Asynchronous;
import org.sonatype.nexus.events.EventSubscriber;
import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.tasks.ExpireCacheTask;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;

/**
 * Event inspector listening for configuration changes to expire caches when Local or Remote URL changed of the
 * repository.
 */
@Named
@Singleton
public class RepositoryConfigurationUpdatedEventInspector
    extends ComponentSupport
    implements EventSubscriber, Asynchronous
{
  private final TaskScheduler nexusTaskExecutor;

  @Inject
  public RepositoryConfigurationUpdatedEventInspector(final TaskScheduler nexusTaskExecutor)
  {
    this.nexusTaskExecutor = nexusTaskExecutor;
  }

  @Subscribe
  @AllowConcurrentEvents
  public void inspect(final RepositoryConfigurationUpdatedEvent event) {
    if (event.isLocalUrlChanged() || event.isRemoteUrlChanged()) {
      String taskName = null;
      String logMessage = null;

      if (event.isLocalUrlChanged() && event.isRemoteUrlChanged()) {
        // both changed
        taskName = "Local and Remote URLs changed, repositoryId=" + event.getRepository().getId() + ".";

        logMessage =
            "The Local and Remote URL of repository \"" + event.getRepository().getName() + "\" (id="
                + event.getRepository().getId() + ") has been changed, expiring its caches.";

      }
      else if (!event.isLocalUrlChanged() && event.isRemoteUrlChanged()) {
        // remote URL changed
        taskName = "Remote URL changed, repositoryId=" + event.getRepository().getId() + ".";

        logMessage =
            "The Remote URL of repository \"" + event.getRepository().getName() + "\" (id="
                + event.getRepository().getId() + ") has been changed, expiring its caches.";
      }
      else if (event.isLocalUrlChanged() && !event.isRemoteUrlChanged()) {
        // local URL changed
        taskName = "Local URL changed, repositoryId=" + event.getRepository().getId() + ".";

        logMessage =
            "The Local URL of repository \"" + event.getRepository().getName() + "\" (id="
                + event.getRepository().getId() + ") has been changed, expiring its caches.";
      }

      final TaskConfiguration taskConfiguration = nexusTaskExecutor.createTaskConfigurationInstance(ExpireCacheTask.class);
      taskConfiguration.setRepositoryId(event.getRepository().getId());
      taskConfiguration.setName(taskName);
      nexusTaskExecutor.submit(taskConfiguration);
      log.info(logMessage);
    }
  }
}
