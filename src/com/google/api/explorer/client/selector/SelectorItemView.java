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

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * Represents a {@link SelectorItem}'s view.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class SelectorItemView extends Composite implements HasClickHandlers {

  private static SelectorItemUiBinder uiBinder = GWT.create(SelectorItemUiBinder.class);

  interface SelectorItemUiBinder extends UiBinder<Widget, SelectorItemView> {
  }

  private String value;
  @UiField SpanElement text;
  @UiField ImageElement image;
  @UiField AnchorElement subtext;

  public SelectorItemView(SelectorItem item) {
    initWidget(uiBinder.createAndBindUi(this));

    Preconditions.checkArgument(!Strings.isNullOrEmpty(item.text));

    this.value = item.text;
    this.text.setInnerText(item.text);

    this.subtext.setInnerText(Strings.nullToEmpty(item.subtext));

    if (!Strings.isNullOrEmpty(item.url)) {
      this.subtext.setHref(item.url);
    }

    if (!Strings.isNullOrEmpty(item.iconUrl)) {
      this.image.setSrc(item.iconUrl);
      UIObject.setVisible(image, true);
    } else {
      UIObject.setVisible(image, false);
    }
  }

  String getValue() {
    return value;
  }

  @Override
  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addDomHandler(handler, ClickEvent.getType());
  }
}
