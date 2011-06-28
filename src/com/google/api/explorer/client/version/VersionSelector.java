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

package com.google.api.explorer.client.version;

import com.google.api.explorer.client.ApiDirectory.ServiceDefinition;
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

/**
 * View of the Version selection UI.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class VersionSelector extends Composite implements VersionSelectorPresenter.Display {

  private static ServiceSelectorUiBinder uiBinder = GWT.create(ServiceSelectorUiBinder.class);

  interface ServiceSelectorUiBinder extends UiBinder<Widget, VersionSelector> {
  }

  @UiField Selector selector;

  private final VersionSelectorPresenter presenter;

  public VersionSelector(EventBus eventBus, Scheduler scheduler) {
    initWidget(uiBinder.createAndBindUi(this));
    this.presenter = new VersionSelectorPresenter(eventBus, scheduler, this);
  }

  @Override
  public void setVersions(List<ServiceDefinition> versions) {
    List<SelectorItem> items = Lists.newArrayList();
    for (ServiceDefinition version : versions) {
      String docLink = version.getDocumentationLink();
      items.add(new SelectorItem(version.getVersion()).setSublink(
          "Documentation", version.getDocumentationLink()));
    }
    Collections.sort(items);
    selector.setOptions(items);
  }

  @UiHandler("selector")
  void select(SelectionEvent<String> event) {
    presenter.selectVersion(event.getSelectedItem());
  }

  @Override
  public void selectVersion(String versionName) {
    selector.setSelectedText(versionName);
  }
}
