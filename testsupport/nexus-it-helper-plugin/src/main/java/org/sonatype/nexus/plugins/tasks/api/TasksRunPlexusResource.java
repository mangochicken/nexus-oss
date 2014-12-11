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
package org.sonatype.nexus.plugins.tasks.api;

import java.util.Map;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.TaskScheduler;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.plexus.rest.resource.AbstractPlexusResource;
import org.sonatype.plexus.rest.resource.PathProtectionDescriptor;

import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.ResourceException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Run a specified task by type, using task parameters provided as query parameters.
 *
 * @since 2.6
 */
@Named
@Singleton
public class TasksRunPlexusResource
    extends AbstractPlexusResource
{

  private static final String RESOURCE_URI = "/tasks/run/{taskType}";

  private final TaskScheduler nexusScheduler;

  @Inject
  public TasksRunPlexusResource(final TaskScheduler nexusScheduler)
  {
    this.nexusScheduler = checkNotNull(nexusScheduler);
  }

  @Override
  public String getResourceUri() {
    return RESOURCE_URI;
  }

  @Override
  public boolean isModifiable() {
    return true;
  }

  @Override
  public PathProtectionDescriptor getResourceProtection() {
    return new PathProtectionDescriptor("/tasks/run/*/**", "anon");
  }

  @Override
  public Object getPayloadInstance() {
    return null;
  }

  @Override
  public Object post(final Context context, final Request request, final Response response, final Object payload)
      throws ResourceException
  {
    final String taskType = request.getAttributes().get("taskType").toString();

    if (taskType != null) {
      try {
        TaskConfiguration taskInstance = nexusScheduler.createTaskConfigurationInstance(taskType);

        final Map<String, String> valuesMap = request.getResourceRef().getQueryAsForm().getValuesMap();
        if (valuesMap != null && !valuesMap.isEmpty()) {
          for (Map.Entry<String, String> entry : valuesMap.entrySet()) {
            taskInstance.setString(entry.getKey(), entry.getValue());
          }
        }
        final TaskInfo<?> scheduledTask = nexusScheduler.submit(taskInstance);
        final Future<?> future = scheduledTask.getCurrentState().getFuture();
        if (future != null) {
          future.get();
        }
      }
      catch (Exception e) {
        throw new ResourceException(e);
      }
    }
    return null;
  }

}
