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
package org.sonatype.nexus.quartz;

import javax.inject.Inject;
import javax.inject.Named;

import org.sonatype.nexus.plugin.PluginIdentity;

import org.eclipse.sisu.EagerSingleton;
import org.jetbrains.annotations.NonNls;

/**
 * Quartz plugin.
 *
 * @since 3.0
 */
@Named
@EagerSingleton
public class QuartzPlugin
    extends PluginIdentity
{
  /**
   * Prefix for ID-like things.
   */
  @NonNls
  public static final String ID_PREFIX = "quartz";

  /**
   * Expected groupId for plugin artifact.
   */
  @NonNls
  public static final String GROUP_ID = "org.sonatype.nexus.plugins";

  /**
   * Expected artifactId for plugin artifact.
   */
  @NonNls
  public static final String ARTIFACT_ID = "nexus-" + ID_PREFIX + "-plugin";

  /**
   * Prefix for @Named configuration.
   */
  public static final String CONFIG_PREFIX = "${" + ID_PREFIX;

  /**
   * Scheduler capability ID.
   */
  public static final String CAPABILITY_ID = ID_PREFIX + ".scheduler";

  /**
   * Capabilities tag.
   */
  public static final String CAPABILITY_CATEGORY_TAG = "Quartz";

  /**
   * Name of the KV store used by plugin.
   */
  public static final String STORE_NAME = "nexusquartz";

  @Inject
  public QuartzPlugin() throws Exception {
    super(GROUP_ID, ARTIFACT_ID);
  }
}