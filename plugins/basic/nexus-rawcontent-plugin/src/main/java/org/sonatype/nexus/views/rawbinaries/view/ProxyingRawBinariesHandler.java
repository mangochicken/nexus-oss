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
package org.sonatype.nexus.views.rawbinaries.view;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.sonatype.nexus.component.model.Asset;
import org.sonatype.nexus.component.source.AssetResponse;
import org.sonatype.nexus.component.source.ComponentRequest;
import org.sonatype.nexus.component.source.ComponentResponse;
import org.sonatype.nexus.component.source.ComponentSource;
import org.sonatype.nexus.component.source.ComponentSourceRegistry;
import org.sonatype.nexus.componentviews.Handler;
import org.sonatype.nexus.componentviews.HandlerContext;
import org.sonatype.nexus.componentviews.ViewRequest;
import org.sonatype.nexus.componentviews.ViewResponse;
import org.sonatype.nexus.componentviews.responses.Responses;
import org.sonatype.nexus.views.rawbinaries.internal.storage.RawBinaryStore;
import org.sonatype.sisu.goodies.common.ComponentSupport;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonatype.nexus.component.services.adapter.AssetAdapter.P_CONTENT_TYPE;
import static org.sonatype.nexus.component.services.adapter.AssetAdapter.P_LAST_MODIFIED;
import static org.sonatype.nexus.component.services.adapter.AssetAdapter.P_PATH;

/**
 * A proxying handler for raw binary assets.
 *
 * @since 3.0
 */
public class ProxyingRawBinariesHandler
    extends ComponentSupport
    implements Handler

{
  private final RawBinaryStore binaryStore;

  private final String sourceName;

  private final ComponentSourceRegistry sourceRegistry;

  public ProxyingRawBinariesHandler(final RawBinaryStore binaryStore, final String sourceName,
                                    final ComponentSourceRegistry sourceRegistry)
  {
    this.binaryStore = checkNotNull(binaryStore);
    this.sourceName = checkNotNull(sourceName);
    this.sourceRegistry = checkNotNull(sourceRegistry);
  }

  @Override
  public ViewResponse handle(final HandlerContext context) throws Exception {
    final ViewRequest req = context.getRequest();

    final String requestPath = ensureLeadingSlash(
        req.getPath() + (req.getQueryString() == null ? "" : "?" + req.getQueryString()));

    switch (req.getMethod()) {
      case GET:
        // Check locally.

        final List<Asset> localBinaries = binaryStore.getForPath(requestPath);

        // Do we have a single result whose path matches the request exactly?

        final Asset exactMatch = exactMatch(localBinaries, requestPath);
        if (exactMatch != null) {
          return createStreamResponse(exactMatch);
        }

        // If not, contact the remote source and store whatever it has

        try {
          final ComponentRequest path = new ComponentRequest(ImmutableMap.of("path", requestPath));
          final ComponentResponse componentResponse = getSource().fetchComponents(path);

          for (AssetResponse asset : componentResponse.getAssets()) {
            binaryStore.create(requestPath, asset.getContentType(), asset.openStream());
          }
        }
        catch (IOException e) {
          Throwables.propagate(e);
        }

        return context.proceed();

      default:
        return Responses.methodNotAllowed();
    }
  }

  @VisibleForTesting
  ViewResponse createStreamResponse(final Asset binary) throws IOException {
    return Responses.streamResponse(binary.openStream(), binary.get(P_CONTENT_TYPE, String.class),
        binary.get(P_LAST_MODIFIED, DateTime.class).toDate());
  }


  @Nullable
  private Asset exactMatch(final List<Asset> binaries, final String path) {
    for (Asset binary : binaries) {
      if (binary.get(P_PATH, String.class).equals(path)) {
        return binary;
      }
    }
    return null;
  }

  private String ensureLeadingSlash(String path) {
    return path.startsWith("/") ? path : "/" + path;
  }

  private ComponentSource getSource() {
    return sourceRegistry.getSource(sourceName);
  }
}
