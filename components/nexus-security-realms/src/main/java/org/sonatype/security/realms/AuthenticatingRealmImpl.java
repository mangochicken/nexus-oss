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
package org.sonatype.security.realms;

import java.util.ConcurrentModificationException;

import javax.enterprise.inject.Typed;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.sonatype.security.model.CUser;
import org.sonatype.security.realms.tools.ConfigurationManager;
import org.sonatype.security.usermanagement.UserNotFoundException;

import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.DisabledAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authc.credential.PasswordMatcher;
import org.apache.shiro.authc.credential.PasswordService;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.Realm;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link AuthenticatingRealm}.
 * This realm ONLY handles authentication.
 */
@Singleton
@Typed(Realm.class)
@Named(AuthenticatingRealmImpl.ROLE)
@Description("Nexus Authenticating Realm")
public class AuthenticatingRealmImpl
    extends AuthenticatingRealm
    implements Realm
{
  private static final Logger logger = LoggerFactory.getLogger(AuthenticatingRealmImpl.class);

  public static final String ROLE = "NexusAuthenticatingRealm";

  private ConfigurationManager configuration;

  private PasswordService passwordService;

  private final int MAX_LEGACY_PASSWORD_LENGTH = 40;

  @Inject
  public AuthenticatingRealmImpl(ConfigurationManager configuration,
                                 PasswordService passwordService)
  {
    this.configuration = configuration;
    this.passwordService = passwordService;

    PasswordMatcher passwordMatcher = new PasswordMatcher();
    passwordMatcher.setPasswordService(this.passwordService);
    setCredentialsMatcher(passwordMatcher);
    setName(ROLE);
    setAuthenticationCachingEnabled(true);
  }

  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token)
      throws AuthenticationException
  {
    UsernamePasswordToken upToken = (UsernamePasswordToken) token;

    CUser user;
    try {
      user = configuration.readUser(upToken.getUsername());
    }
    catch (UserNotFoundException e) {
      throw new AccountException("User '" + upToken.getUsername() + "' cannot be retrieved.", e);
    }

    if (user.getPassword() == null) {
      throw new AccountException("User '" + upToken.getUsername() + "' has no password, cannot authenticate.");
    }

    if (CUser.STATUS_ACTIVE.equals(user.getStatus())) {
      // Check for legacy user that has unsalted password hash
      // Update if unsalted password hash and valid credentials were specified
      if (hasLegacyPassword(user) && isValidCredentials(upToken, user)) {
        reHashPassword(user, new String(upToken.getPassword()));
      }

      return this.createAuthenticationInfo(user);
    }
    else if (CUser.STATUS_DISABLED.equals(user.getStatus())) {
      throw new DisabledAccountException("User '" + upToken.getUsername() + "' is disabled.");
    }
    else {
      throw new AccountException("User '" + upToken.getUsername() + "' is in illegal status '"
          + user.getStatus() + "'.");
    }
  }

  /*
   * Re-hash user password, and persist changes.
   *
   * @param user to update
   * @param password clear-text password to hash
   */
  private void reHashPassword(final CUser user, final String password) {
    String hashedPassword = passwordService.encryptPassword(password);
    try {
      boolean updated = false;
      do {
        CUser toUpdate = configuration.readUser(user.getId());
        toUpdate.setPassword(hashedPassword);
        try {
          configuration.updateUser(user);
          updated = true;
        }
        catch (ConcurrentModificationException e) {
          logger.debug("Could not re-hash user {} password as user was concurrently being updated. Retrying...");
        }
      }
      while (!updated);
      user.setPassword(hashedPassword);
    }
    catch (Exception e) {
      logger.error("Unable to update hash for user {}", user.getId(), e);
    }
  }

  /*
   * Checks to see if the credentials in token match the credentials stored on
   * user
   *
   * @param token the username/password token containing the credentials to
   * verify
   * @param the user object containing the stored credentials
   * @return true if credentials match, false otherwise
   */
  private boolean isValidCredentials(UsernamePasswordToken token, CUser user) {
    boolean credentialsValid = false;

    AuthenticationInfo info = this.createAuthenticationInfo(user);
    CredentialsMatcher matcher = this.getCredentialsMatcher();
    if (matcher != null) {
      if (matcher.doCredentialsMatch(token, info)) {
        credentialsValid = true;
      }
    }

    return credentialsValid;
  }

  /*
   * Checks to see if the specified user is a legacy user
   * A legacy user has an unsalted password
   *
   * @param user to check
   * @return true if legacy user, false otherwise
   */
  private boolean hasLegacyPassword(CUser user) {
    //Legacy users have a shorter, unsalted, SHA1 or MD5 based hash
    return user.getPassword().length() <= this.MAX_LEGACY_PASSWORD_LENGTH;
  }

  /*
   * Creates an authentication info object
   * using the credentials of the provided user
   *
   * @param user
   * @return authentication info object based on user credentials
   */
  private AuthenticationInfo createAuthenticationInfo(CUser user) {
    return new SimpleAuthenticationInfo(user.getId(), user.getPassword().toCharArray(), getName());
  }
}
