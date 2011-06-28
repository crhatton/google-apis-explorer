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

package com.google.api.explorer.client.method;

import com.google.api.explorer.client.AppState;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.selector.Selector;
import com.google.api.explorer.client.selector.SelectorItem;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * View of the Method selection UI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class MethodSelector extends Composite implements MethodSelectorPresenter.Display {

  private static MethodSelectorUiBinder uiBinder = GWT.create(MethodSelectorUiBinder.class);

  interface MethodSelectorUiBinder extends UiBinder<Widget, MethodSelector> {
  }

  @UiField Selector selector;

  private final MethodSelectorPresenter presenter;

  public MethodSelector(EventBus eventBus, AppState appState, Scheduler scheduler) {
    initWidget(uiBinder.createAndBindUi(this));
    this.presenter = new MethodSelectorPresenter(eventBus, appState, scheduler, this);
  }

  @Override
  public void setMethods(Map<String, ApiMethod> methods) {
    List<SelectorItem> items = Lists.newArrayList();
    for (Map.Entry<String, ApiMethod> entry : methods.entrySet()) {
      items.add(new SelectorItem(entry.getKey(), "", entry.getValue().getDescription()));
    }
    Collections.sort(items);

    this.setVisible(true);
    selector.setOptions(items);
  }

  @UiHandler("selector")
  void select(SelectionEvent<String> event) {
    presenter.selectMethod(event.getSelectedItem());
  }

  @Override
  public void selectMethod(String methodName) {
    selector.setSelectedText(methodName);
  }

  @Override
  public void setLoading(boolean loading) {
    selector.setLoading(loading);
  }
}
