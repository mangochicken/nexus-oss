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
package org.sonatype.nexus.testsuite.task.nexus533;

import java.util.Calendar;
import java.util.Date;

import org.sonatype.nexus.integrationtests.AbstractNexusIntegrationTest;
import org.sonatype.nexus.rest.model.ScheduledServiceBaseResource;
import org.sonatype.nexus.rest.model.ScheduledServiceListResource;
import org.sonatype.nexus.rest.model.ScheduledServiceOnceResource;
import org.sonatype.nexus.test.utils.TaskScheduleUtil;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Assert;
import org.junit.Test;
import org.restlet.data.Status;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractNexusTasksIntegrationIT<E extends ScheduledServiceBaseResource>
    extends AbstractNexusIntegrationTest
{

  public abstract E getTaskScheduled();

  @Test
  public void doTest()
      throws Exception
  {
    scheduleTasks();
    updateTasks();
    changeScheduling();
    deleteTasks();
  }

  public void scheduleTasks()
      throws Exception
  {
    Status status = TaskScheduleUtil.create(getTaskScheduled());
    Assert.assertTrue(status.isSuccess());

    assertTasks();
  }

  protected void assertTasks()
      throws Exception
  {
    E scheduledTask = getTaskScheduled();

    ScheduledServiceListResource task = TaskScheduleUtil.getTask(scheduledTask.getName());
    assertThat(task.getName(), equalTo(scheduledTask.getName()));
    // Note: FQCN typeId accepted, but "canonical" is the ID from TaskDescriptor and that is now class.simpleName
    assertThat(scheduledTask.getTypeId(), endsWith(task.getTypeId()));
  }

  public void updateTasks()
      throws Exception
  {
    E scheduledTask = getTaskScheduled();
    ScheduledServiceListResource task = TaskScheduleUtil.getTask(scheduledTask.getName());

    scheduledTask.setId(task.getId());
    updateTask(scheduledTask);
    Status status = TaskScheduleUtil.update(scheduledTask);
    Assert.assertTrue(status.isSuccess());

    assertTasks();
  }

  public abstract void updateTask(E scheduledTask);

  public void changeScheduling()
      throws Exception
  {
    E scheduledTask = getTaskScheduled();
    ScheduledServiceListResource task = TaskScheduleUtil.getTask(scheduledTask.getName());

    // if we have a manual task we can't change the schedule to be manual
    // again
    if (!task.getSchedule().equals("manual")) {

      ScheduledServiceBaseResource taskManual = new ScheduledServiceBaseResource();
      taskManual.setId(task.getId());
      taskManual.setName(scheduledTask.getName());
      taskManual.setEnabled(true);
      taskManual.setTypeId(scheduledTask.getTypeId());
      taskManual.setProperties(scheduledTask.getProperties());
      taskManual.setSchedule("manual");

      Status status = TaskScheduleUtil.update(taskManual);
      Assert.assertTrue(status.isSuccess());

    }
    else {
      ScheduledServiceOnceResource updatedTask = new ScheduledServiceOnceResource();
      updatedTask.setId(task.getId());
      updatedTask.setName(scheduledTask.getName());
      updatedTask.setEnabled(task.isEnabled());
      updatedTask.setTypeId(scheduledTask.getTypeId());
      updatedTask.setProperties(scheduledTask.getProperties());
      updatedTask.setSchedule("once");
      Date startDate = DateUtils.addDays(new Date(), 10);
      startDate = DateUtils.round(startDate, Calendar.DAY_OF_MONTH);
      updatedTask.setStartDate(String.valueOf(startDate.getTime()));
      updatedTask.setStartTime("03:30");

      Status status = TaskScheduleUtil.update(updatedTask);
      Assert.assertTrue(status.isSuccess());
    }

    assertTasks();
  }

  public void deleteTasks()
      throws Exception
  {
    ScheduledServiceListResource task = TaskScheduleUtil.getTask(getTaskScheduled().getName());
    Status status = TaskScheduleUtil.deleteTask(task.getId());
    Assert.assertTrue(status.isSuccess());

    // delete is not working, see NEXUS-572
    // This is not true anymore, since NEXUS-3977, cancel() does NOT remove task from config, as this IT originally
    // checked,
    // that is left to DefaultScheduledTask upon exiting from call() method.
    // Hence, this IT is failing now, since it checks for removal from config, that does not happen immediately (but
    // sometime in the future)

    // This is more correct to do
    TaskScheduleUtil.waitForAllTasksToStop();

    // Configuration nexusConfig = getNexusConfigUtil().getNexusConfig();
    // Assert.assertTrue( nexusConfig.getTasks().isEmpty() );
  }

}
