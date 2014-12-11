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
package org.sonatype.nexus.scheduling.internal;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.nexus.scheduling.Task;
import org.sonatype.nexus.scheduling.TaskConfiguration;
import org.sonatype.nexus.scheduling.TaskFactory;
import org.sonatype.nexus.scheduling.TaskInfo;
import org.sonatype.nexus.scheduling.schedule.Now;
import org.sonatype.nexus.scheduling.schedule.Schedule;
import org.sonatype.nexus.scheduling.spi.TaskExecutorSPI;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.eclipse.sisu.Priority;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple SPI using ThreadPoolExecutor that supports only simple execution of background tasks, but not scheduling.
 *
 * @since 3.0
 */
@Singleton
@Named
// TODO: I want this implementation to be last, see DefaultNexusTaskScheduler#getScheduler method
@Priority(1000) // be last, sorta fallback? (and used in tests)
public class ThreadPoolTaskExecutorSPI
    extends ComponentSupport
    implements TaskExecutorSPI
{
  private final TaskFactory taskFactory;

  private final ThreadPoolExecutor executorService;

  private final ConcurrentMap<String, ThreadPoolTaskInfo<?>> tasks;

  @Inject
  public ThreadPoolTaskExecutorSPI(final TaskFactory taskFactory)
  {
    this.taskFactory = checkNotNull(taskFactory);
    this.executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(15);
    this.tasks = Maps.newConcurrentMap();
  }

  private class ThreadPoolTaskInfo<T>
      implements TaskInfo<T>, Callable<T>
  {
    private Task<T> task;

    private Schedule schedule;

    private Future<T> future;

    private Date runStarted;

    private long runDuration;

    private volatile EndState endState;

    public void itRuns(final Task<T> task, final Schedule schedule, final Future<T> future) {
      this.task = task;
      this.schedule = schedule;
      this.future = future;
      this.runStarted = new Date();
      this.runDuration = 0;
      this.endState = null;
    }

    @Override
    public String getId() {
      return getConfiguration().getId();
    }

    @Override
    public String getName() {
      return getConfiguration().getName();
    }

    @Override
    public TaskConfiguration getConfiguration() {
      return task.taskConfiguration();
    }

    @Override
    public Schedule getSchedule() {
      return schedule;
    }

    @Override
    public String getMessage() {
      return getConfiguration().getMessage();
    }

    @Override
    public CurrentState<T> getCurrentState() {
      return new CurrentState<T>()
      {
        @Override
        public State getState() {
          if (endState != null) {
            return State.DONE;
          }
          else {
            return State.RUNNING;
          }
        }

        @Nullable
        @Override
        public Date getNextRun() {
          return null;
        }

        @Nullable
        @Override
        public Date getRunStarted() {
          if (endState != null) {
            return null;
          }
          else {
            return runStarted;
          }
        }

        @Nullable
        @Override
        public RunState getRunState() {
          if (endState != null) {
            return null;
          }
          else {
            return RunState.RUNNING;
          }
        }

        @Nullable
        @Override
        public Future getFuture() {
          return future;
        }
      };
    }

    @Nullable
    @Override
    public LastRunState getLastRunState() {
      if (endState != null) {
        return new LastRunState()
        {
          @Override
          public EndState getEndState() {
            return endState;
          }

          @Override
          public Date getRunStarted() {
            return runStarted;
          }

          @Override
          public long getRunDuration() {
            return runDuration;
          }
        };
      }
      return null;
    }

    @Override
    public TaskInfo<T> runNow() {
      throw new UnsupportedOperationException("Only once executing tasks are supported");
    }

    @Override
    public boolean remove() {
      return future.cancel(true);
    }

    @Override
    public T call() throws Exception {
      final long now = System.currentTimeMillis();
      EndState endState = null;
      log.info("Task started: {} : {}", getConfiguration().getTypeName(), getName());
      try {
        T result = task.call();
        endState = EndState.OK;
        log.info("Task ended: {} : {}", getConfiguration().getTypeName(), getName());
        return result;
      }
      catch (Exception e) {
        endState = EndState.FAILED;
        log.info("Task failed: {} : {}", getConfiguration().getTypeName(), getName(), e);
        throw e;
      }
      finally {
        tasks.remove(getId());
        this.runDuration = System.currentTimeMillis() - now;
        this.endState = endState;
      }
    }
  }

  @Override
  public <T> TaskInfo<T> getTaskById(final String id) {
    return (TaskInfo<T>) tasks.get(id);
  }

  @Override
  public List<TaskInfo<?>> listsTasks() {
    return Lists.<TaskInfo<?>>newArrayList(tasks.values());
  }

  @Override
  public synchronized <T> TaskInfo<T> scheduleTask(final TaskConfiguration taskConfiguration, final Schedule schedule) {
    checkNotNull(taskConfiguration);
    checkArgument(schedule instanceof Now, "Only 'now' schedule is supported");
    final Task<T> task = taskFactory.createTaskInstance(taskConfiguration);
    final ThreadPoolTaskInfo<T> taskInfo;
    if (tasks.containsKey(taskConfiguration.getId())) {
      taskInfo = (ThreadPoolTaskInfo<T>) tasks.get(taskConfiguration.getId());
      taskInfo.getCurrentState().getFuture().cancel(true);
    }
    else {
      taskInfo = new ThreadPoolTaskInfo<>();
    }
    final Future<T> future = executorService.submit(taskInfo);
    taskInfo.itRuns(task, schedule, future);
    tasks.put(task.getId(), taskInfo);
    return taskInfo;
  }

  @Override
  public <T> TaskInfo<T> rescheduleTask(final String id, final Schedule schedule) {
    throw new UnsupportedOperationException("Only once executing tasks are supported");
  }

  @Override
  public int getRunningTaskCount() {
    return tasks.size();
  }
}
