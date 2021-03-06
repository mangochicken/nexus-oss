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
 * Master/Detail tabs.
 *
 * @since 3.0
 */
Ext.define('NX.view.masterdetail.Tabs', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-masterdetail-tabs',
  requires: [
    'NX.Icons',
    'NX.Bookmarks',
    'NX.ext.tab.SortedPanel'
  ],

  /**
   * @override
   */
  initComponent: function() {
    var me = this;

    me.items = [
      {
        xtype: 'panel',
        itemId: 'info',
        iconCls: NX.Icons.cls('masterdetail-info', 'x16'),
        hidden: true
      },
      {
        xtype: 'panel',
        itemId: 'warning',
        iconCls: NX.Icons.cls('masterdetail-warning', 'x16'),
        hidden: true
      },
      {
        xtype: 'nx-sorted-tabpanel',
        itemId: 'tab',
        ui: 'light',
        activeTab: 0,
        layoutOnTabChange: true,
        flex: 1,
        items: me.tabs
      }
    ];

    me.callParent(arguments);

    me.on('afterrender', me.calculateBookmarks, me);
  },

  showInfo: function(message) {
    var me = this,
        infoPanel = me.down('>#info');

    infoPanel.setTitle(message);
    infoPanel.show();
  },

  clearInfo: function() {
    var me = this,
        infoPanel = me.down('>#info');

    infoPanel.hide();
  },

  showWarning: function(message) {
    var me = this,
        warningPanel = me.down('>#warning');

    warningPanel.setTitle(message);
    warningPanel.show();
  },

  clearWarning: function() {
    var me = this,
        warningPanel = me.down('>#warning');

    warningPanel.hide();
  },

  addTab: function(tab) {
    var me = this,
        tabPanel = me.down('>#tab');

    tabPanel.add(tab);
    me.calculateBookmarks();
  },

  removeTab: function(tab) {
    var me = this,
        tabPanel = me.down('>#tab');

    tabPanel.remove(tab);
    me.calculateBookmarks();
  },

  /**
   * @public
   * @returns {String} bookmark token of selected tab
   */
  getBookmarkOfSelectedTab: function() {
    var me = this,
        tabPanel = me.down('>#tab');

    return tabPanel.getActiveTab().bookmark;
  },

  /**
   * @public
   * Finds a tab by bookmark & sets it active (if found).
   * @param {String} bookmark of tab to be activated
   */
  setActiveTabByBookmark: function(bookmark) {
    var me = this,
        tabPanel = me.down('>#tab'),
        tab = me.down('> tabpanel > panel[bookmark=' + bookmark + ']');

    if (tabPanel && tab) {
      tabPanel.setActiveTab(tab);
    }
  },

  /**
   * @private
   * Calculates bookmarks of all tabs based on tab title.
   */
  calculateBookmarks: function() {
    var me = this,
        tabPanel = me.down('>#tab');

    tabPanel.items.each(function(tab) {
      if (tab.title) {
        tab.bookmark = NX.Bookmarks.encode(tab.title).toLowerCase();
      }
    });
  }

});
