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
Ext.define('NX.view.drilldown.Panel', {
  extend: 'Ext.panel.Panel',
  alias: 'widget.nx-drilldown',
  itemId: 'nx-drilldown',

  requires: [
    'NX.Icons'
  ],

  layout: {
    type: 'hbox',
    align: 'stretch'
  },

  tabs: {
    xtype: 'nx-info-panel'
  },

  defaults: {
    flex: 1
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
            xtype: 'nx-drilldown-details',
            ui: 'drilldown-tabs',
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
    me.items = items;

    // TODO: Is this necessary? It results in a stack overflow when uncommented.
    me.callParent(arguments);

    if (Ext.isDefined(me.iconName)) {
      me.setDefaultIconName(me.iconName);
    }
  },

  /**
   * @override
   */
  onRender: function () {
    var me = this;

    me.currentIndex = 0;

    if (me.ownerCt) {
      me.relayEvents(me.ownerCt, ['resize'], 'owner');
      me.on({
        ownerresize: me.syncSizeToOwner
      });
      me.syncSizeToOwner();
    }

    me.callParent(arguments);
  },

  syncSizeToOwner: function () {
    var me = this;
    if (me.ownerCt) {
      me.setSize(me.ownerCt.el.getWidth() * me.items.items.length, me.ownerCt.el.getHeight());
      me.showChild(me.currentIndex, false);
    }
  },

  showChild: function (index, animate) {
    var me = this;

    if (me.items.items[index].el) {

      // Hack to prevent resize events until the animation is complete
      if (animate) {
        me.ownerCt.suspendEvents(false);
        setTimeout(function () { me.ownerCt.resumeEvents(); }, 300);
      }

      // Show the new panel
      var left = me.items.items[index].el.getLeft() - me.el.getLeft();
      me.el.first().move('l', left, animate);
      me.currentIndex = index;

      me.refreshBreadcrumb();
    }
  },

  setItemName: function (index, text) {
    var me = this;
    me.items.items[index].setItemName(text);
  },

  setItemClass: function (index, cls) {
    var me = this;
    me.items.items[index].setItemClass(cls);
  },

  setItemBookmark: function (index, bookmark, scope) {
    var me = this;
    me.items.items[index].setItemBookmark(bookmark, scope);
  },

  setDefaultIconName: function (iconName) {
    var items = this.query('nx-drilldown-item');
    for (var i = 0; i < items.length; ++i) {
      items[i].setItemClass(NX.Icons.cls(iconName) + (i === 0 ? '-x32' : '-x16'));
    }
  },

  showInfo: function (message) {
    this.down('nx-drilldown-details').showInfo(message);
  },

  clearInfo: function () {
    this.down('nx-drilldown-details').clearInfo();
  },

  showWarning: function (message) {
    this.down('nx-drilldown-details').showWarning(message);
  },

  clearWarning: function () {
    this.down('nx-drilldown-details').clearWarning();
  },

  /*
   * Add a tab to the default detail panel
   *
   * Note: this will have no effect if a custom detail panel has been specified
   */
  addTab: function (tab) {
    var me = this;
    if (!me.detail) {
      this.down('nx-drilldown-details').addTab(tab);
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
      this.down('nx-drilldown-details').removeTab(tab);
    }
  },

  refreshBreadcrumb: function() {
    var me = this;
    var content = me.up('#feature-content');
    var root = content.down('#feature-root');
    var breadcrumb = content.down('#breadcrumb');

    if (me.currentIndex == 0) {
      // Feature's home page, no breadcrumb required
      breadcrumb.hide();
      root.show();
    } else {
      breadcrumb.removeAll();

      // Make a breadcrumb (including icon and 'home' link)
      breadcrumb.add(
        '',
        {
          xtype: 'image',
          height: 32,
          width: 32,
          cls: content.currentIconCls
        },
        {
          xtype: 'nx-drilldown-link',
          scale: 'large',
          text: content.currentTitle,
          handler: function() {
            me.showChild(0, true);

            // Set the bookmark
            var bookmark = me.items.items[0].itemBookmark;
            if (bookmark) {
              NX.Bookmarks.bookmark(bookmark.obj, bookmark.scope);
            }
          }
        }
      );

      // Create the rest of the links
      for (var i = 1; i <= me.currentIndex && i < me.items.items.length; ++i) {
        breadcrumb.add(
          // Separator
          {
            xtype: 'tbtext',
            cls: 'breadcrumb-separator',
            text: '/'
          },
          {
            xtype: 'image',
            height: 16,
            width: 16,
            cls: 'breadcrumb-icon ' + me.items.items[i].itemClass
          },

          // Create a closure within a closure to decouple 'i' from the current context
          (function(j) {
            return {
              xtype: 'nx-drilldown-link',
              disabled: (i == me.currentIndex ? true : false), // Disabled if it’s the last item in the breadcrumb
              text: me.items.items[j].itemName,
              handler: function() {
                me.showChild(j, true);

                // Set the bookmark
                var bookmark = me.items.items[j].itemBookmark;
                if (bookmark) {
                  NX.Bookmarks.bookmark(bookmark.obj, bookmark.scope);
                }
              }
            }
          })(i)
        );
      }

      root.hide();
      breadcrumb.show();
    }
  }
});
