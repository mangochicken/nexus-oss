/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2013 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package core.routing;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.sonatype.nexus.client.core.exception.NexusClientBadRequestException;
import org.sonatype.nexus.client.core.subsystem.content.Content.Directive;
import org.sonatype.nexus.client.core.subsystem.content.Location;
import org.sonatype.nexus.client.core.subsystem.routing.DiscoveryConfiguration;

import com.google.common.primitives.Ints;

/**
 * Testing automatic routing publishing on group repositories.
 * 
 * @author cstamas
 */
public class WithGroupRepositoryIT
    extends ITSupport
{
    /**
     * This is a safety net, to have JUnit kill this test if it locks for any reason (remote scrape or such). This rule
     * will kill this test after 15 minutes.
     */
    @Rule
    public Timeout timeout = new Timeout( Ints.checkedCast( TimeUnit.MINUTES.toMillis( 15L ) ) );

    /**
     * Constructor.
     * 
     * @param nexusBundleCoordinates
     */
    public WithGroupRepositoryIT( final String nexusBundleCoordinates )
    {
        super( nexusBundleCoordinates );
    }

    // ==

    private final String REPO_ID = "public";

    private final Location PREFIX_FILE_LOCATION = Location.repositoryLocation( REPO_ID, "/.meta/prefixes.txt" );

    protected boolean exists( final Location location )
        throws IOException
    {
        return exists( location, Directive.GROUP_ONLY );
    }

    protected boolean noscrape( final Location location )
        throws IOException
    {
        return noscrape( location, Directive.GROUP_ONLY );
    }

    @Test
    public void groupMustHaveWLPublishedWhenAllMembersHaveIt()
        throws Exception
    {
        // wait for central
        routingTest().waitForAllRoutingUpdateJobToStop();
        // waitForWLDiscoveryOutcome( "central" );
        // waitForWLPublishingOutcomes( "central", REPO_ID );
        assertThat( exists( PREFIX_FILE_LOCATION ), is( true ) );
        assertThat( noscrape( PREFIX_FILE_LOCATION ), is( false ) );
    }

    @Test
    public void groupLoosesWLIfMemberLooses()
        throws Exception
    {
        assertThat( exists( PREFIX_FILE_LOCATION ), is( true ) );
        assertThat( noscrape( PREFIX_FILE_LOCATION ), is( false ) );
        {
            final DiscoveryConfiguration config = routing().getDiscoveryConfigurationFor( "central" );
            config.setEnabled( false );
            routing().setDiscoveryConfigurationFor( "central", config );
            routingTest().waitForAllRoutingUpdateJobToStop();
            // waitForWLDiscoveryOutcome( "central" );
            // waitForWLPublishingOutcomes( "central", REPO_ID );
        }
        assertThat( exists( PREFIX_FILE_LOCATION ), is( true ) );
        assertThat( noscrape( PREFIX_FILE_LOCATION ), is( true ) );
        {
            final DiscoveryConfiguration config = routing().getDiscoveryConfigurationFor( "central" );
            config.setEnabled( true );
            routing().setDiscoveryConfigurationFor( "central", config );
            routingTest().waitForAllRoutingUpdateJobToStop();
            // waitForWLDiscoveryOutcome( "central" );
            // waitForWLPublishingOutcomes( "central", REPO_ID );
        }
        assertThat( exists( PREFIX_FILE_LOCATION ), is( true ) );
        assertThat( noscrape( PREFIX_FILE_LOCATION ), is( false ) );
    }

    @Test( expected = NexusClientBadRequestException.class )
    public void groupWLNotDeletable()
        throws Exception
    {
        // we did no any waiting, e just booted nexus, so it must be present
        assertThat( exists( PREFIX_FILE_LOCATION ), is( true ) );
        assertThat( noscrape( PREFIX_FILE_LOCATION ), is( false ) );
        content().delete( PREFIX_FILE_LOCATION );
    }
}
