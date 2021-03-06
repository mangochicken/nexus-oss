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

package org.sonatype.nexus.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sonatype.nexus.NexusAppTestSupport;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.maven.RepositoryPolicy;
import org.sonatype.nexus.proxy.maven.maven2.Maven2ContentClass;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.GroupRepository;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.security.PlexusConfiguredRealm;
import org.sonatype.nexus.proxy.targets.Target;
import org.sonatype.nexus.proxy.targets.TargetRegistry;
import org.sonatype.nexus.security.WebSecurityUtil;
import org.sonatype.nexus.templates.repository.maven.Maven2GroupRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven2HostedRepositoryTemplate;
import org.sonatype.nexus.templates.repository.maven.Maven2Maven1ShadowRepositoryTemplate;
import org.sonatype.security.SecuritySystem;
import org.sonatype.security.authentication.AuthenticationException;
import org.sonatype.security.configuration.source.PreconfiguredSecurityConfigurationSource;
import org.sonatype.security.configuration.source.SecurityConfigurationSource;
import org.sonatype.sisu.litmus.testsupport.group.Slow;

import com.google.common.collect.Maps;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import junit.framework.Assert;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(Slow.class) // ~20s
public class ProtectedRepositoryRegistryIT
    extends NexusAppTestSupport
{

  private RepositoryRegistry repositoryRegistry = null;

  private SecuritySystem securitySystem = null;

  @Override
  protected void customizeModules(final List<Module> modules) {
    super.customizeModules(modules);
    modules.add(new Module()
    {
      @Override
      public void configure(final Binder binder) {
        final Map<String, String> privileges = Maps.newHashMap();
        privileges.put("repo1user", "nexus:target:*:repo1:*,nexus:view:repository:repo1");
        privileges.put("repo1userNoView", "nexus:target:*:repo1:*");
        privileges.put("repoall", "nexus:target:*:*:*,nexus:view:repository:*");

        final PlexusConfiguredRealm realm = new PlexusConfiguredRealm(privileges);
        binder.bind(Key.get(Realm.class, Names.named("default"))).toInstance(realm);

        binder.bind(SecurityConfigurationSource.class)
            .annotatedWith(Names.named("default"))
            .toInstance(new PreconfiguredSecurityConfigurationSource(ProtectedRepositoryRegistryITSecurity.security()));
      }
    });
  }

  @Override
  protected boolean runWithSecurityDisabled() {
    return false;
  }

  @Override
  protected void setUp()
      throws Exception
  {
    super.setUp();

    // this will trigger config load!
    startNx();

    this.repositoryRegistry = this.lookup(RepositoryRegistry.class, "protected");

    this.buildRepository("repo1");
    this.buildRepository("repo2");

    this.buildGroupRepository("group1");
    this.buildGroupRepository("group2");

    this.buildShadowRepository("repo3");
    this.buildShadowRepository("repo4");

    // create a target
    TargetRegistry targetRegistry = this.lookup(TargetRegistry.class);
    Target t1 =
        new Target("maven2-all", "All (Maven2)", new Maven2ContentClass(), Arrays.asList(new String[]{".*"}));
    targetRegistry.addRepositoryTarget(t1);
    nexusConfiguration().saveConfiguration();

    // setup security
    this.securitySystem = this.lookup(SecuritySystem.class);

    waitForTasksToStop();
  }

  @Override
  public void tearDown()
      throws Exception
  {
    try {
      ThreadContext.remove();
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  public void testRegistryWithViewAccess()
      throws Exception
  {
    Subject subject = this.loginUser("repo1user");

    List<Repository> repositories = this.repositoryRegistry.getRepositories();

    // // this user only has access to repo1, that is all they should see
    Assert.assertEquals("User should only have access to 'repo1'", 1, repositories.size());
    Assert.assertEquals("repo1", repositories.get(0).getId());

    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testRegistryWithViewAccessFacet()
      throws Exception
  {
    Subject subject = this.loginUser("repo1user");

    List<Repository> repositories = this.repositoryRegistry.getRepositoriesWithFacet(Repository.class);

    // // this user only has access to repo1, that is all they should see
    Assert.assertEquals("User should only have access to 'repo1'", 1, repositories.size());
    Assert.assertEquals("repo1", repositories.get(0).getId());

    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testRegistryWithViewAccessById()
      throws Exception
  {
    Subject subject = this.loginUser("repo1user");

    Repository repository = this.repositoryRegistry.getRepository("repo1");

    Assert.assertNotNull(repository);
    Assert.assertEquals("repo1", repository.getId());

    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testRegistryWithViewAccessByIdNoAccess()
      throws Exception
  {
    Subject subject = this.loginUser("repo1userNoView");

    try {
      this.repositoryRegistry.getRepository("repo1");
      Assert.fail("expected NoSuchRepositoryAccessException");
    }
    catch (NoSuchRepositoryAccessException e) {
      // expected
    }
    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testRegistryWithViewAccessFacetById()
      throws Exception
  {
    Subject subject = this.loginUser("repo1user");

    Repository repository = this.repositoryRegistry.getRepositoryWithFacet("repo1", Repository.class);

    Assert.assertNotNull(repository);
    Assert.assertEquals("repo1", repository.getId());

    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testRegistryWithViewAccessFacetByIdNoAccess()
      throws Exception
  {
    Subject subject = this.loginUser("repo1userNoView");

    try {
      this.repositoryRegistry.getRepositoryWithFacet("repo1", Repository.class);
      Assert.fail("expected NoSuchRepositoryAccessException");
    }
    catch (NoSuchRepositoryAccessException e) {
      // expected
    }
    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testRemoveWithAccess()
      throws Exception
  {
    Subject subject = this.loginUser("repo1user");

    this.repositoryRegistry.removeRepository("repo1");
    try {
      this.repositoryRegistry.getRepository("repo1");
      Assert.fail("expected NoSuchRepositoryException");
    }
    catch (NoSuchRepositoryException e) {
      // expected
    }
    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testRemoveWithoutAccess()
      throws Exception
  {
    Subject subject = this.loginUser("repo1userNoView");

    try {
      this.repositoryRegistry.removeRepository("repo1");
      Assert.fail("expected NoSuchRepositoryAccessException");
    }
    catch (NoSuchRepositoryAccessException e) {
      // expected
    }
    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testRemoveSilentlyWithAccess()
      throws Exception
  {
    Subject subject = this.loginUser("repo1user");

    this.repositoryRegistry.removeRepositorySilently("repo1");
    try {
      this.repositoryRegistry.getRepository("repo1");
      Assert.fail("expected NoSuchRepositoryException");
    }
    catch (NoSuchRepositoryException e) {
      // expected
    }
    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testRemoveSilentlyWithoutAccess()
      throws Exception
  {
    Subject subject = this.loginUser("repo1userNoView");

    try {
      this.repositoryRegistry.removeRepositorySilently("repo1");
      Assert.fail("expected NoSuchRepositoryAccessException");
    }
    catch (NoSuchRepositoryAccessException e) {
      // expected
    }
    // logout user
    this.securitySystem.logout(subject);
  }

  @Test
  public void testGetGroupsWithAccess()
      throws Exception
  {
    Subject subject = this.loginUser("repoall");

    Collection<GroupRepository> groups = this.repositoryRegistry.getRepositoriesWithFacet(GroupRepository.class);

    // list should have 2 group repos
    List<String> groupIds = new ArrayList<String>();

    for (GroupRepository groupRepo : groups) {
      groupIds.add(groupRepo.getId());
    }

    Assert.assertTrue(groupIds.contains("group1"));
    Assert.assertTrue(groupIds.contains("group2"));
    Assert.assertTrue(groupIds.contains("public"));
    Assert.assertEquals(3, groups.size());

    // logout user
    this.securitySystem.logout(subject);

  }

  private Repository buildRepository(String repoId)
      throws Exception
  {
    Maven2HostedRepositoryTemplate template =
        (Maven2HostedRepositoryTemplate) getRepositoryTemplates().getTemplates(
            Maven2HostedRepositoryTemplate.class, RepositoryPolicy.RELEASE).pick();

    template.getConfigurableRepository().setIndexable(false);
    template.getConfigurableRepository().setId(repoId);

    return template.create();
  }

  private Repository buildGroupRepository(String repoId)
      throws Exception
  {
    Maven2GroupRepositoryTemplate template =
        (Maven2GroupRepositoryTemplate) getRepositoryTemplates().getTemplates(
            Maven2GroupRepositoryTemplate.class).pick();

    template.getConfigurableRepository().setIndexable(false);
    template.getConfigurableRepository().setId(repoId);

    return template.create();
  }

  private Repository buildShadowRepository(String repoId)
      throws Exception
  {
    Maven2HostedRepositoryTemplate template =
        (Maven2HostedRepositoryTemplate) getRepositoryTemplates().getTemplates(
            Maven2HostedRepositoryTemplate.class, RepositoryPolicy.RELEASE).pick();

    template.getConfigurableRepository().setIndexable(false);
    template.getConfigurableRepository().setId(repoId);

    // just create
    template.create();

    // now for the shadow
    Maven2Maven1ShadowRepositoryTemplate shadowTemplate =
        (Maven2Maven1ShadowRepositoryTemplate) getRepositoryTemplates().getTemplates(
            Maven2Maven1ShadowRepositoryTemplate.class).pick();

    shadowTemplate.getConfigurableRepository().setIndexable(false);
    shadowTemplate.getConfigurableRepository().setId(repoId + "-shadow");

    shadowTemplate.getExternalConfiguration(true).setMasterRepositoryId(repoId);

    return shadowTemplate.create();
  }

  private Subject loginUser(String username)
      throws AuthenticationException
  {
    WebSecurityUtil.setupWebContext(username);
    return this.securitySystem.login(new UsernamePasswordToken(username, ""));
  }

}
