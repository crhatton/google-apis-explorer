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

package com.google.api.explorer.client.service;

import com.google.api.explorer.client.ApiDirectory.ServiceDefinition;
import com.google.api.explorer.client.ExplorerConfig;
import com.google.api.explorer.client.selector.Selector;
import com.google.api.explorer.client.selector.SelectorItem;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * View of the Service selection UI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class ServiceSelector extends Composite implements ServiceSelectorPresenter.Display {

  private static ServiceSelectorUiBinder uiBinder = GWT.create(ServiceSelectorUiBinder.class);

  interface ServiceSelectorUiBinder extends UiBinder<Widget, ServiceSelector> {
  }

  @UiField Selector selector;

  private final ServiceSelectorPresenter presenter;

  public ServiceSelector(EventBus eventBus) {
    initWidget(uiBinder.createAndBindUi(this));
    this.presenter = new ServiceSelectorPresenter(eventBus, this);
  }

  @Override
  public void setServices(Set<ServiceDefinition> services) {
    List<SelectorItem> items = Lists.newArrayList();
    for (ServiceDefinition def : services) {
      if (def.isPreferred() && !ExplorerConfig.SERVICE_BLACKLIST.contains(def.getName())) {
        String icon16Url = def.getIcons() != null ? def.getIcons().getIcon16Url() : null;
        items.add(new SelectorItem(def.getName(), icon16Url, def.getDescription()));
      }
    }
    Collections.sort(items);

    selector.setOptions(items);
  }

  @UiHandler("selector")
  void select(SelectionEvent<String> event) {
    presenter.selectService(event.getSelectedItem());
  }

  @Override
  public void selectService(String serviceName) {
    selector.setSelectedText(serviceName);
  }

  @Override
  public void setLoading(boolean loading) {
    selector.setLoading(loading);
  }
}
