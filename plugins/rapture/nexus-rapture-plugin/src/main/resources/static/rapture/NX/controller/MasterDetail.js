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
 * Abstract Master/Detail controller.
 *
 * @since 3.0
 */
Ext.define('NX.controller.MasterDetail', {
  extend: 'Ext.app.Controller',
  requires: [
    // many impls use this
    'NX.view.info.Panel',
    'NX.view.info.Entry',
    'NX.Conditions',
    'NX.Dialogs',
    'NX.Bookmarks'
  ],
  mixins: {
    logAware: 'NX.LogAware'
  },

  views: [
    'masterdetail.Panel',
    'masterdetail.Tabs'
  ],

  permission: undefined,

  getDescription: Ext.emptyFn,

  /**
   * @cfg {Function} optional function to be called on delete
   */
  deleteModel: undefined,

  onLaunch: function () {
    var me = this;
    me.getApplication().getIconController().addIcons({
      'masterdetail-info': {
        file: 'information.png',
        variants: ['x16', 'x32']
      },
      'masterdetail-warning': {
        file: 'warning.png',
        variants: ['x16', 'x32']
      }
    });
  },

  init: function () {
    var me = this,
        componentListener = {};

    // Normalize lists into an array
    if (!Ext.isArray(me.list)) {
      me.list = [me.list];
    }

    // Add event handlers to each list
    for (var i = 0; i < me.list.length; ++i) {
      componentListener[me.list[i]] = {
        afterrender: me.onAfterRender,
        selection: me.onSelection,
        cellclick: me.onCellClick
      };
      componentListener[me.list[i] + ' button[action=new]'] = {
        afterrender: me.bindNewButton
      };
      componentListener[me.list[i] + ' ^ nx-masterdetail-panel nx-masterdetail-tabs > tabpanel'] = {
        tabchange: function() {
          var selected = me.getList().getSelectionModel().getSelection();
          me.bookmark(selected.length === 1 ? selected[0] : null);
          // TODO: implement activeSelection
          //me.bookmark(me.activeSelection);
        }
      };

      // bind to a delete button if delete function defined
      if (me.deleteModel) {
        componentListener[me.list[i] + ' ^ nx-masterdetail-panel nx-masterdetail-tabs button[action=delete]'] = {
          afterrender: me.bindDeleteButton,
          click: me.onDelete
        };
      }
    }

    me.listen({
      component: componentListener,
      controller: {
        '#Bookmarking': {
          navigate: me.navigateTo
        },
        '#Refresh': {
          refresh: me.refreshList
        }
      }
    });

    if (me.icons) {
      me.getApplication().getIconController().addIcons(me.icons);
    }
    if (me.features) {
      me.getApplication().getFeaturesController().registerFeature(me.features, me);
    }
  },

  // Return references to all of the master views
  getLists: function() {
    var me = this,
      feature = me.getFeature(),
      lists = [];

    for (var i = 0; i < me.list.length; ++i) {
      lists.push(feature.down(me.list[i]));
    }

    return lists;
  },

  loadStore: function () {
    var me = this,
        lists = me.getLists();

    for (var i = 0; i < lists.length; ++i) {
      lists[i].getStore().load();
    }
  },

  loadStoreAndSelect: function (model) {
    var me = this;

    me.bookmark(model);
    me.loadStore();
  },

  onStoreLoad: function () {
    var me = this,
        lists = me.getLists();

    if (lists.length) {
      me.navigateTo(NX.Bookmarks.getBookmark());
    }
  },

  reselect: function () {
    var me = this,
        lists = me.getLists();

    if (lists.length) {
      me.navigateTo(NX.Bookmarks.getBookmark());
    }
  },

  refreshList: function () {
    var me = this,
        lists = me.getLists();

    if (lists.length) {
      me.loadStore();
    }
  },

  onAfterRender: function () {
    var me = this,
        lists = me.getLists();

    for (var i = 0; i < lists.length; ++i) {
      lists[i].mon(lists[i].getStore(), 'load', me.onStoreLoad, me);
    }
    me.loadStore();
  },

  onCellClick: function(list, td, cellIndex, model) {
    var me = this;

    me.loadView(list, model, true);
    me.bookmark(model);
  },

  onModelChanged: function (model) {
    var me = this,
        lists = me.getLists(),
        masterdetail = me.getFeature();

    if (model) {
      // Find the list to which this model belongs, and focus on it
      for (var i = 0; i < lists.length; ++i) {
        if (lists[i].getView().getNode(model)) {
          lists[i].getView().focusRow(model);

          // TODO: Set the description of next view
          masterdetail.setDescription(me.getDescription(model), i + 1);

          break;
        }
      }
    }
  },

  /**
   * Make the detail view appear, update models and bookmarks
   */
  loadView: function (list, model, animate) {
    var me = this,
      lists = me.getLists(),
      feature = me.getFeature();

    // No model specified, go to the root view
    if (!model) {
      feature.down('#nx-drilldown').showChild(0, animate);
    }

    // Model specified, find the associated list and show that
    for (var i = 0; i < lists.length; ++i) {
      if (list === lists[i].getView() && model) {
        lists[i].fireEvent("selection", list, model);
        me.onModelChanged(model);
        feature.down('#nx-drilldown').setItemBookmark(0, NX.Bookmarks.fromToken(NX.Bookmarks.getBookmark().getSegment(0)), me);

        // Show the next view in line
        feature.down('#nx-drilldown').showChild(i + 1, animate);
        break;
      }
    }
  },

  /**
   * Bookmark specified model / selected tab.
   */
  bookmark: function (model) {
    var me = this,
        lists = me.getLists(),
        feature = me.getFeature(),
        tabs = feature.down('nx-masterdetail-tabs'),
        bookmark = NX.Bookmarks.fromToken(NX.Bookmarks.getBookmark().getSegment(0)),
        segments = [],
        selected,
        selectedTabBookmark,
        index;

    // Find all parent models and add them to the bookmark array
    for (index = 0; index < lists.length; ++index) {
      if (!lists[index].getView().getNode(model)) {
        selected = lists[index].getSelectionModel().getSelection();
        if (selected.length === 1) {
          segments.push(encodeURIComponent(selected[0].getId()));
        } else {
          // Error: cannot construct a complete path
          return;
        }
      } else {
        // All done adding parents
        break;
      }
    }

    // Add the currently selected model to the bookmark array
    if (model) {
      segments.push(encodeURIComponent(model.getId()));

      // Is this the last list model? And is a tab selected? If so, add it.
      if (index == lists.length - 1) {
        selectedTabBookmark = tabs.getBookmarkOfSelectedTab();
        if (selectedTabBookmark) {
          segments.push(selectedTabBookmark);
        }
      }
    }

    bookmark.appendSegments(segments);
    NX.Bookmarks.bookmark(bookmark, me);
  },

  /**
   * @public
   * @param {NX.Bookmark} bookmark to navigate to
   */
  navigateTo: function (bookmark) {
    var me = this,
        lists = me.getLists(),
        feature = me.getFeature(),
        store, modelId, tabBookmark, model, tabs;

    if (lists.length && bookmark) {
      modelId = bookmark.getSegment(1);
      tabBookmark = bookmark.getSegment(2);
      if (modelId) {
        modelId = decodeURIComponent(modelId);
        me.logDebug('Navigate to: ' + modelId + (tabBookmark ? ":" + tabBookmark : ''));
        store = lists[0].getStore();
        model = store.getById(modelId);
        if (model) {
          me.loadView(lists[0].getView(), model, false);
          lists[0].fireEvent('selectionchange', lists[0], [model]);
        }
        if (tabBookmark) {
          feature.down('nx-masterdetail-tabs').setActiveTabByBookmark(tabBookmark);
        }
      }
      else {
        lists[0].getSelectionModel().deselectAll();
        me.loadView(lists[0].getView(), null, false);
      }
    }
  },

  // TODO: wire this to work with multiple list views
  onDelete: function () {
    var me = this,
        selection = me.getLists()[0].getSelectionModel().getSelection(),
        description;

    if (Ext.isDefined(selection) && selection.length > 0) {
      description = me.getDescription(selection[0]);
      NX.Dialogs.askConfirmation('Confirm deletion?', description, function () {
        me.deleteModel(selection[0]);
        me.bookmark(null);
      }, {scope: me});
    }
  },

  /**
   * @protected
   * Enable 'New' when user has 'create' permission.
   */
  bindNewButton: function (button) {
    var me = this;
    button.mon(
        NX.Conditions.isPermitted(me.permission, 'create'),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  },

  /**
   * @protected
   * Enable 'Delete' when user has 'delete' permission.
   */
  bindDeleteButton: function (button) {
    var me = this;
    button.mon(
        NX.Conditions.and(
            NX.Conditions.isPermitted(me.permission, 'delete')
        ),
        {
          satisfied: button.enable,
          unsatisfied: button.disable,
          scope: button
        }
    );
  }

});
