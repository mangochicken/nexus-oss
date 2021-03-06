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
/*global Ext, NX*/

/**
 * Branding controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.Branding', {
  extend: 'Ext.app.Controller',
  requires: [
    'NX.State'
  ],

  views: [
    'header.Branding'
  ],

  refs: [
    { ref: 'viewport', selector: 'viewport' },
    { ref: 'headerBranding', selector: 'nx-header-branding' },
    { ref: 'footerBranding', selector: 'nx-footer-branding' }
  ],

  /**
   * @override
   */
  init: function() {
    var me = this;

    me.listen({
      controller: {
        '#State': {
          brandingchanged: me.onBrandingChanged
        }
      },
      component: {
        'nx-header-branding': {
          afterrender: me.renderHeaderBranding
        },
        'nx-footer-branding': {
          afterrender: me.renderFooterBranding
        }
      }
    });
  },

  /**
   * @private
   * Render header/footer branding when branding configuration changes.
   */
  onBrandingChanged: function() {
    var me = this;

    me.renderHeaderBranding();
    me.renderFooterBranding();
  },

  /**
   * @private
   * Render header branding.
   */
  renderHeaderBranding: function() {
    var me = this,
        branding = NX.State.getValue('branding'),
        headerBranding = me.getHeaderBranding();

    if (headerBranding) {
      if (branding && branding['headerEnabled']) {
        headerBranding.update(branding['headerHtml']);
        headerBranding.show();
      }
      else {
        headerBranding.hide();
      }
    }
  },

  /**
   * @private
   * Render footer branding.
   */
  renderFooterBranding: function() {
    var me = this,
        branding = NX.State.getValue('branding'),
        footerBranding = me.getFooterBranding();

    if (footerBranding) {
      if (branding && branding['footerEnabled']) {
        footerBranding.update(branding['footerHtml']);
        footerBranding.show();
      }
      else {
        footerBranding.hide();
      }
    }
  }

});
