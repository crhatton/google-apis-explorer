// Copyright 2012 Google Inc. All Rights Reserved.

package com.google.api.explorer.client.parameter.schema;

import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.Widget;

/**
 * Class where we will put utility methods that can be used to reduce redundancy among schema
 * editors.
 *
 */
class EditorHelper {

  /** Prevent instantiation. */
  private EditorHelper() {
  }

  /**
   * Show the specified popup panel below and to the right of the specified widget.
   */
  public static void discloseLowerRight(final PopupPanel toDisclose, final Widget relativeTo) {
    toDisclose.setPopupPositionAndShow(new PositionCallback() {
      @Override
      public void setPosition(int offsetWidth, int offsetHeight) {
        int left = relativeTo.getAbsoluteLeft() + relativeTo.getOffsetWidth();
        int top = relativeTo.getAbsoluteTop() + relativeTo.getOffsetHeight();
        toDisclose.setPopupPosition(left, top);
      }
    });
  }
}
