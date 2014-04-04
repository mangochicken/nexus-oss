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
/**
 * Mark log window.
 *
 * @since 3.0
 */
Ext.define('NX.coreui.view.search.LogMark', {
  extend: 'NX.view.AddWindow',
  alias: 'widget.nx-coreui-log-mark',

  title: 'Mark log',
  defaultFocus: 'message',

  items: {
    xtype: 'nx-settingsform',
    api: {
      submit: 'NX.direct.logging_Log.mark'
    },
    settingsFormSuccessMessage: 'Log has been marked',
    editableCondition: NX.Conditions.isPermitted('nexus:logconfig', 'update'),
    editableMarker: 'You do not have permission to mark the log',
    items: [
      {
        xtype: 'textfield',
        name: 'message',
        itemId: 'message',
        fieldLabel: 'Message',
        emptyText: 'enter a marker text'
      }
    ]
  }

});
