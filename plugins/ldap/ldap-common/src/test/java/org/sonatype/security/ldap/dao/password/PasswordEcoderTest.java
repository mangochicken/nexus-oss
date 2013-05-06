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
package org.sonatype.security.ldap.dao.password;

import junit.framework.Assert;

import org.junit.Test;
import org.sonatype.nexus.test.PlexusTestCaseSupport;

public class PasswordEcoderTest
    extends PlexusTestCaseSupport
{
    private PasswordEncoderManager passwordEncoderManager;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();
        this.passwordEncoderManager = this.lookup( PasswordEncoderManager.class );
    }

    @Test
    public void testMD5()
    {
        // String cryptPassword = "{MD5}VtAV1lfgo6fnA60qDj64iA=="; generated by apacheds (for testing), uses hash
        String cryptPassword = "{MD5}56d015d657e0a3a7e703ad2a0e3eb888"; // just a simple md5 of the string
        String password = "md5123";

        Assert.assertTrue( this.passwordEncoderManager.isPasswordValid( cryptPassword, password, null ) );
    }

    @Test
    public void testClear()
    {
        String cryptPassword = "clear123";
        String password = "clear123";

        Assert.assertTrue( this.passwordEncoderManager.isPasswordValid( cryptPassword, password, null ) );
        Assert.assertTrue( this.passwordEncoderManager.isPasswordValid( "{CLEAR}" + cryptPassword, password, null ) );
    }

    @Test
    public void testCrypt()
    {
        // String cryptPassword = "{CRYPT}h/Uf.HBpLVxmY"; generated by apacheds (for testing)
        String cryptPassword = "{CRYPT}$1$SpIxKuJe$KTEcMasljm9eC3CfgE72t/"; // generated by MD5Crypt.class
        String password = "crypt123";

        Assert.assertTrue( this.passwordEncoderManager.isPasswordValid( cryptPassword, password, null ) );
    }
    @Test
    public void testPlain()
    {
        String cryptPassword = "plain123";
        String password = "plain123";

        Assert.assertTrue( this.passwordEncoderManager.isPasswordValid( cryptPassword, password, null ) );
        Assert.assertTrue( this.passwordEncoderManager.isPasswordValid( "{PLAIN}" + cryptPassword, password, null ) );
    }

    @Test
    public void testSha()
    {
        String cryptPassword = "{SHA}5Rs+hbtHUzvAhwkuaV5g7hW2KGc=";
        String password = "sha123";

        Assert.assertTrue( this.passwordEncoderManager.isPasswordValid( cryptPassword, password, null ) );
    }

    @Test
    public void testSsha()
    {
        String cryptPassword = "{SSHA}JoYvOj2uRZsp5hPMVgWmuwh0CnDwSgiQXMwPkg==";
        String password = "ssha123";

        Assert.assertTrue( this.passwordEncoderManager.isPasswordValid( cryptPassword, password, null ) );
    }

    @Test
    public void testNull()
    {
        Assert.assertFalse( this.passwordEncoderManager.isPasswordValid( null, "non null string", null ) );
        Assert.assertFalse( this.passwordEncoderManager.isPasswordValid( null, null, null ) );
    }

}
