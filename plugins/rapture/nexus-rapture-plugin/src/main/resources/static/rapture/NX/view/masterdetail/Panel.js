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
 * Abstract Master/Detail panel.
 *
 * @since 3.0
 */
Ext.define('NX.view.masterdetail.Panel', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-masterdetail-panel',
  requires: [
    'NX.Icons'
  ],

  layout: 'fit',

  tabs: {
    xtype: 'nx-info-panel'
  },

  /**
   * @override
   */
  initComponent: function () {
    var me = this,
      items = [];

    // Normalize list of masters
    me.masters = Ext.isArray(me.masters) ? me.masters : [me.masters];

    // Add masters
    for (var i = 0; i < me.masters.length; ++i) {
      items.push(
        {
          xtype: 'nx-drilldown-item',
          layout: 'fit',
          items: me.masters[i]
        }
      );
    }

    // Add details
    if (me.detail) {
      // Use a custom detail panel
      items.push({ xtype: 'nx-drilldown-item', layout: 'fit', items: me.detail });
    } else {
      // Use the default tab panel
      items.push(
        {
          xtype: 'nx-drilldown-item',

          layout: 'fit',

          items: {
            xtype: 'nx-masterdetail-tabs',
            ui: 'masterdetail-tabs',
            header: false,
            plain: true,

            layout: {
              type: 'vbox',
              align: 'stretch',
              pack: 'start'
            },
            tabs: Ext.isArray(me.tabs) ? Ext.Array.clone(me.tabs) : Ext.apply({}, me.tabs),
            tbar: Ext.isArray(me.actions) ? Ext.Array.clone(me.actions) : Ext.apply({}, me.actions)
          }
        }
      );
    }

    // Initialize this component’s items
    me.items = [
      {
        xtype: 'container',
        items: [
          {
            xtype: 'nx-drilldown',
            items: items
          }
        ]
      }
    ];

    me.callParent(arguments);

    if (Ext.isDefined(me.iconName)) {
      me.setDefaultIconName(me.iconName);
    }
  },

  setDefaultIconName: function (iconName) {
    var items = this.query('nx-drilldown-item');
    for (var i = 0; i < items.length; ++i) {
      items[i].setItemClass(NX.Icons.cls(iconName) + (i === 0 ? '-x32' : '-x16'));
    }
  },

  showInfo: function (message) {
    this.down('nx-masterdetail-tabs').showInfo(message);
  },

  clearInfo: function () {
    this.down('nx-masterdetail-tabs').clearInfo();
  },

  showWarning: function (message) {
    this.down('nx-masterdetail-tabs').showWarning(message);
  },

  clearWarning: function () {
    this.down('nx-masterdetail-tabs').clearWarning();
  },

  /*
   * Add a tab to the default detail panel
   *
   * Note: this will have no effect if a custom detail panel has been specified
   */
  addTab: function (tab) {
    var me = this;
    if (!me.detail) {
      this.down('nx-masterdetail-tabs').addTab(tab);
    }
  },

  /*
   * Remove a panel from the default detail panel
   *
   * Note: this will have no effect if a custom detail panel has been specified
   */
  removeTab: function (tab) {
    var me = this;
    if (!me.detail) {
      this.down('nx-masterdetail-tabs').removeTab(tab);
    }
  }

});
