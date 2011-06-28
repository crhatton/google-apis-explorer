/*
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.explorer.client.selector;

import com.google.api.explorer.client.Resources;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;
import java.util.Map;

/**
 * Generic list view selector, used to display services, version, methods, etc.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class Selector extends Composite implements HasSelectionHandlers<String> {

  private static SelectorUiBinder uiBinder = GWT.create(SelectorUiBinder.class);

  interface SelectorUiBinder extends UiBinder<Widget, Selector> {}

  @UiField Label header;
  @UiField Label loading;
  @UiField Label emptyMessage;
  @UiField VerticalPanel panel;

  private final Map<String, SelectorItemView> options = Maps.newHashMap();
  private SelectorItemView selected;

  private ClickHandler clickHandler = new ClickHandler() {
    public void onClick(ClickEvent event) {
      Selector.this.setSelected((SelectorItemView) event.getSource(), true);
    }
  };

  public Selector() {
    initWidget(uiBinder.createAndBindUi(this));
  }

  /** Set the header displayed over the selector. */
  public void setHeader(String header) {
    this.header.setText(header);
  }

  public void setEmptyMessage(String emptyMessage) {
    this.emptyMessage.setText(emptyMessage);
  }

  /** Clear the list of items. */
  public void clear() {
    options.clear();
    panel.clear();
    loading.setVisible(false);
    emptyMessage.setVisible(false);
  }

  /** Set the list of available options in the selector. */
  public void setOptions(List<SelectorItem> options) {
    clear();
    for (SelectorItem option : options) {
      SelectorItemView view = new SelectorItemView(option);
      view.addClickHandler(clickHandler);
      this.options.put(option.text, view);
      panel.add(view);
    }
    panel.setVisible(!options.isEmpty());
    emptyMessage.setVisible(options.isEmpty());
  }

  void setSelected(SelectorItemView label, boolean fireEvent) {
    if (selected != null) {
      selected.removeStyleName(Resources.INSTANCE.style().selected());
    }
    selected = label;
    selected.addStyleName(Resources.INSTANCE.style().selected());
    if (fireEvent) {
      SelectionEvent.fire(this, selected.getValue());
    }
  }

  /**
   * Sets the item with the given text as the selected item, optionally firing a
   * selection event when it does so.
   */
  public void setSelectedText(String text) {
    setSelected(
        Preconditions.checkNotNull(options.get(text), "No item with the text: " + text), false);
  }

  /** Get the text of the selected item. */
  public String getSelectedText() {
    return selected.getValue();
  }

  /** Show/hide the loading indicator. */
  public void setLoading(boolean loading) {
    this.loading.setVisible(loading);
    this.panel.setVisible(!loading);
    this.emptyMessage.setVisible(!loading);
  }

  /**
   * Add a {@link SelectionHandler} that will be notified when items are
   * selected.
   */
  @Override
  public HandlerRegistration addSelectionHandler(SelectionHandler<String> handler) {
    return addHandler(handler, SelectionEvent.getType());
  }
}
