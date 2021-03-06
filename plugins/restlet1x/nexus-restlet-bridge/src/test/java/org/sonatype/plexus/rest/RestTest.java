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
package org.sonatype.plexus.rest;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusTestCase;
import org.restlet.Application;
import org.restlet.Component;
import org.restlet.data.Protocol;

public class RestTest
    extends PlexusTestCase
{
  @Override
  protected void customizeContainerConfiguration(ContainerConfiguration configuration) {
    super.customizeContainerConfiguration(configuration);
    configuration.setAutoWiring(true);
    configuration.setClassPathScanning(PlexusConstants.SCANNING_CACHE);
  }

  public void testRest()
      throws Exception
  {
    Component component = new Component();
    component.getServers().add(Protocol.HTTP, 8182);
    PlexusRestletApplicationBridge app = (PlexusRestletApplicationBridge) getContainer().lookup(Application.class);
    component.getDefaultHost().attach(app);
    component.start();
    component.stop();
  }
}
