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

package com.google.api.explorer.client.auth;

import com.google.api.explorer.client.AuthManager;
import com.google.api.explorer.client.analytics.AnalyticsManager;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.ApiService.AuthScope;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * View for Authentication status and authentication link.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class AuthView extends Composite implements AuthPresenter.Display {
  private static AuthUiBinder uiBinder = GWT.create(AuthUiBinder.class);

  interface AuthUiBinder extends UiBinder<Widget, AuthView> {
  }

  interface AuthViewStyle extends CssResource {
    String clickable();

    String discoveryScopeSelector();
  }

  @UiField AuthViewStyle style;

  @UiField Panel scopeSelector;
  @UiField ToggleButton authToggle;

  @UiField Image authInfoIcon;
  @UiField Image authWarningIcon;
  @UiField Image authErrorIcon;

  @UiField Panel discloseScopeInfo;
  @UiField PopupPanel scopeInfoPopup;
  @UiField Label authMessage;
  @UiField Label scopeList;

  @UiField PopupPanel scopePopup;
  @UiField Panel scopePanel;
  @UiField Panel additionalScopePanel;
  @UiField Label hasScopesText;
  @UiField Label noScopesText;
  @UiField Label optionalAdditionalScopes;

  @UiField Label serviceName;

  @UiField Button authorizeButton;
  @UiField Button cancelAuthButton;

  private final AuthPresenter presenter;
  private Map<String, AuthScope> scopesFromDiscovery = Maps.newHashMap();
  private Set<String> selectedScopes = Sets.newLinkedHashSet();
  private List<TextBox> freeFormEditors = Lists.newLinkedList();

  /**
   * Variable to track when it was a click that was used to disclose the scope info rather than a
   * hover. This will prevent the widget from closing when the user moves the mouse away.
   */
  private boolean clickedToDiscloseScopeInfo = false;

  public AuthView(AuthManager authManager, ApiService service, AnalyticsManager analytics) {
    initWidget(uiBinder.createAndBindUi(this));

    this.presenter = new AuthPresenter(service, authManager, analytics, this);

    serviceName.setText(service.displayTitle());

    // Unless you show then hide popup windows they do not initialize properly.
    scopePopup.show();
    scopePopup.hide();

    scopeInfoPopup.show();
    scopeInfoPopup.hide();

    scopeInfoPopup.addCloseHandler(new CloseHandler<PopupPanel>() {
      @Override
      public void onClose(CloseEvent<PopupPanel> event) {
        GWT.log("Handler for closing popup.");
        clickedToDiscloseScopeInfo = false;
      }
    });
  }

  public AuthPresenter getPresenter() {
    return presenter;
  }

  @UiHandler("authToggle")
  void authToggled(ValueChangeEvent<Boolean> event) {
    if (event.getValue()) {
      presenter.clickEnableAuth();
    } else {
      presenter.clickDisableAuth();
    }
  }

  @UiHandler("authorizeButton")
  void authorize(ClickEvent event) {
    presenter.clickExecuteAuth();
  }

  @UiHandler("cancelAuthButton")
  void cancelAuth(ClickEvent event) {
    presenter.clickCancelAuth();
  }

  @UiHandler("discloseScopeInfo")
  void discloseScopeInfo(ClickEvent event) {
    clickedToDiscloseScopeInfo = true;
    showScopeInfoPopup();
  }

  @UiHandler("discloseScopeInfo")
  void scopeInfoHover(MouseOverEvent event) {
    showScopeInfoPopup();
  }

  @UiHandler("discloseScopeInfo")
  void scopeInfoMouseOut(MouseOutEvent event) {
    if (!clickedToDiscloseScopeInfo) {
      scopeInfoPopup.hide();
    }
  }

  private void showScopeInfoPopup() {
    scopeInfoPopup.setPopupPositionAndShow(new PositionCallback() {
      @Override
      public void setPosition(int offsetWidth, int offsetHeight) {
        int left = discloseScopeInfo.getAbsoluteLeft() - offsetWidth
            + discloseScopeInfo.getOffsetWidth();
        int top = discloseScopeInfo.getAbsoluteTop() + discloseScopeInfo.getOffsetHeight();
        scopeInfoPopup.setPopupPosition(left, top);
      }
    });
  }

  @Override
  public void setState(State state, Set<String> requiredScopes, Set<String> heldScopes) {
    scopeSelector.setVisible(state != State.ONLY_PUBLIC);
    authToggle.setValue(state == State.PRIVATE);

    Set<String> missingScopes = Sets.difference(requiredScopes, heldScopes);

    Set<String> scopesToShow;
    String message;
    AuthIconState iconState;
    if (missingScopes.isEmpty()) {
      iconState = AuthIconState.INFO;
      scopesToShow = heldScopes;
      if (heldScopes.isEmpty()) {
        message = "No auth required.";
      } else {
        message = "Auth scopes held: ";
      }
    } else if (missingScopes.equals(requiredScopes)) {
      iconState = AuthIconState.ERROR;
      scopesToShow = Collections.emptySet();
      message = "Method requires authorized requests.";
    } else {
      iconState = AuthIconState.WARNING;
      scopesToShow = missingScopes;
      message = "Method may require additional auth scopes: ";
    }

    authInfoIcon.setVisible(iconState == AuthIconState.INFO);
    authWarningIcon.setVisible(iconState == AuthIconState.WARNING);
    authErrorIcon.setVisible(iconState == AuthIconState.ERROR);

    authMessage.setText(message);

    Iterable<String> shortNames = Iterables.transform(scopesToShow, new Function<String, String>() {
        @Override
      public String apply(String scopeUrl) {
        return AuthPresenter.scopeName(scopeUrl);
      }
    });
    String delimitedNames = Joiner.on(", ").join(shortNames);
    scopeList.setText(delimitedNames);
  }

  @Override
  public void setScopes(Map<String, AuthScope> scopes) {
    selectedScopes.clear();
    scopesFromDiscovery = scopes;
  }

  /**
   * Rebuild the popup from scratch with all of the scopes that we have from the last time we were
   * presented.
   */
  private void buildScopePopup() {

    scopePanel.clear();
    additionalScopePanel.clear();

    Set<TextBox> oldEditors = Sets.newLinkedHashSet(freeFormEditors);
    freeFormEditors.clear();

    // Hide the service scopes label if there aren't any.
    hasScopesText.setVisible(!scopesFromDiscovery.isEmpty());
    noScopesText.setVisible(scopesFromDiscovery.isEmpty());

    // Show different text on the free-form scopes section when there are discovery scopes.
    optionalAdditionalScopes.setVisible(!scopesFromDiscovery.isEmpty());

    for (final Map.Entry<String, AuthScope> scope : scopesFromDiscovery.entrySet()) {
      // Add the check box to the table.
      CheckBox scopeToggle = new CheckBox();

      SafeHtmlBuilder safeHtml = new SafeHtmlBuilder();
      safeHtml.appendEscaped(scope.getKey()).appendHtmlConstant("<br><span>")
          .appendEscaped(scope.getValue().getDescription()).appendHtmlConstant("</span>");
      scopeToggle.setHTML(safeHtml.toSafeHtml());

      scopeToggle.addStyleName(style.discoveryScopeSelector());
      scopePanel.add(scopeToggle);

      // When the box is checked, add our scope to the selected list.
      scopeToggle.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          CheckBox checkBox = (CheckBox) event.getSource();
          if (checkBox.getValue()) {
            selectedScopes.add(scope.getKey());
          } else {
            selectedScopes.remove(scope.getKey());
          }
        }
      });

      // Enable the check box if the scope is selected.
      scopeToggle.setValue(selectedScopes.contains(scope.getKey()));
    }

    // Process any scopes that are extra.
    for (TextBox editor : oldEditors) {
      if (!editor.getValue().trim().isEmpty()) {
        addFreeFormEditorRow(editor.getValue(), true);
      }
    }

    // There should always be one empty editor.
    addFreeFormEditorRow("", false);
  }

  /**
   * Add an editor row in the form of a textbox, that will allow an arbitrary scope to be added.
   */
  private FocusWidget addFreeFormEditorRow(String name, boolean showRemoveLink) {
    final FlowPanel newRow = new FlowPanel();

    // Create the new editor and do the appropriate bookkeeping.
    final TextBox scopeText = new TextBox();
    scopeText.setValue(name);
    newRow.add(scopeText);
    freeFormEditors.add(scopeText);

    final Label removeLink = new InlineLabel("X");
    removeLink.addStyleName(style.clickable());
    removeLink.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        freeFormEditors.remove(scopeText);
        additionalScopePanel.remove(newRow);

        if (freeFormEditors.isEmpty()) {
          addFreeFormEditorRow("", false);
        }
      }
    });
    newRow.add(removeLink);
    removeLink.setVisible(showRemoveLink);

    // Add a handler to add a new editor when there is text in the existing editor.
    scopeText.addKeyDownHandler(new KeyDownHandler() {
      @Override
      public void onKeyDown(KeyDownEvent event) {
        TextBox editor = (TextBox) event.getSource();
        boolean isLastEditor = editor.equals(Iterables.getLast(freeFormEditors));
        if (isLastEditor && !editor.getValue().isEmpty()) {
          presenter.addNewScope();
          removeLink.setVisible(true);
        }
      }
    });

    additionalScopePanel.add(newRow);

    return scopeText;
  }

  @Override
  public Set<String> getSelectedScopes() {
    // Concatenate the structured scopes with the free-form scopes.
    Set<String> allScopes = Sets.newHashSet(selectedScopes);

    for (TextBox editor : freeFormEditors) {
      if (!editor.getValue().trim().isEmpty()) {
        allScopes.add(editor.getValue());
      }
    }

    return allScopes;
  }

  @Override
  public void showScopeDialog() {
    buildScopePopup();
    scopePopup.show();
    scopePopup.center();
  }

  @Override
  public void addScopeEditor() {
    addFreeFormEditorRow("", false);
  }

  @Override
  public void hideScopeDialog() {
    scopePopup.hide();
  }

  @Override
  public void preSelectScopes(Set<String> scopes) {
    selectedScopes.removeAll(scopesFromDiscovery.keySet());
    selectedScopes.addAll(scopes);
  }

  /** Possible states of the auth icon indicator. */
  private enum AuthIconState {
    WARNING,
    ERROR,
    INFO,
  }
}
