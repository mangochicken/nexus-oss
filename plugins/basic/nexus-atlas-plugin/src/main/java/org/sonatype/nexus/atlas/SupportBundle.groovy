/**
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

package org.sonatype.nexus.atlas

/**
 * Defines the content sources of a support ZIP file.
 *
 * @since 2.7
 */
class SupportBundle
{
  /**
   * Source of content for support bundle.
   */
  static interface ContentSource
  {
    /**
     * The path in the support zip where this content will be saved.
     */
    String getPath()

    /**
     * The size of the content in bytes. Valid after {@link #prepare()} has been called.
     */
    int getSize()

    /**
     * Content bytes. Valid after {@link #prepare()} has been called.
     */
    InputStream getContent()

    /**
     * Prepare content.
     */
    void prepare()

    /**
     * Cleanup content.
     */
    void cleanup()
  }

  private final List<ContentSource> sources = []

  /**
   * Returns all configured content sources.
   */
  List<ContentSource> getSources() {
    return sources
  }

  /**
   * Add a content source.
   */
  void add(final ContentSource contentSource) {
    assert contentSource
    sources << contentSource
  }

  /**
   * @see #add(ContentSource)
   */
  void leftShift(final ContentSource contentSource) {
    add(contentSource)
  }
}