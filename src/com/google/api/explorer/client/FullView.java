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

package com.google.api.explorer.client;

import com.google.api.explorer.client.FullViewPresenter.NavigationItem;
import com.google.api.explorer.client.analytics.AnalyticsManager;
import com.google.api.explorer.client.auth.AuthView;
import com.google.api.explorer.client.base.ApiDirectory.ServiceDefinition;
import com.google.api.explorer.client.base.ApiMethod;
import com.google.api.explorer.client.base.ApiRequest;
import com.google.api.explorer.client.base.ApiResponse;
import com.google.api.explorer.client.base.ApiService;
import com.google.api.explorer.client.base.NameHelper;
import com.google.api.explorer.client.context.ExplorerContext;
import com.google.api.explorer.client.context.ListServiceContext.TagProcessor;
import com.google.api.explorer.client.embedded.EmbeddedParameterFormPresenter.RequestFinishedCallback;
import com.google.api.explorer.client.embedded.EmbeddedView;
import com.google.api.explorer.client.history.EmbeddedHistoryItemView;
import com.google.api.explorer.client.history.HistoryItem;
import com.google.api.explorer.client.history.JsonPrettifier;
import com.google.api.explorer.client.navigation.EntryAggregatorView;
import com.google.api.explorer.client.navigation.HistoryEntry;
import com.google.api.explorer.client.navigation.MethodEntry;
import com.google.api.explorer.client.navigation.SectionedAggregator;
import com.google.api.explorer.client.navigation.ServiceEntry;
import com.google.api.explorer.client.navigation.ServiceEntry.DescriptionTag;
import com.google.api.explorer.client.routing.TitleSupplier.Title;
import com.google.api.explorer.client.routing.URLManipulator;
import com.google.api.explorer.client.routing.UrlBuilder.RootNavigationItem;
import com.google.api.explorer.client.routing.handler.HistoryManager.HistoryManagerDelegate;
import com.google.api.explorer.client.search.SearchManager.SearchReadyCallback;
import com.google.api.explorer.client.search.SearchResult;
import com.google.api.explorer.client.search.SearchResult.MethodBundle;
import com.google.api.explorer.client.widgets.PlaceholderTextBox;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHyperlink;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestBox.SuggestionDisplay;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.SuggestOracle.Suggestion;
import com.google.gwt.user.client.ui.Widget;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * View of the whole app.
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public class FullView extends Composite
    implements FullViewPresenter.Display, HistoryManagerDelegate, SearchReadyCallback {

  private static FullViewUiBinder uiBinder = GWT.create(FullViewUiBinder.class);

  private static final String REPORT_ERROR_URL = "http://code.google.com/p/google-apis-explorer/"
      + "issues/entry?template=Defect%20report%20from%20user";
  private static final String EXPLORER_HELP_URL = "http://code.google.com/apis/explorer-help";
  private static final String EXPLORER_FORUM_URL =
      "http://code.google.com/apis/explorer-help/forum.html";

  private static final String NEW_TAB_TARGET = "_blank";
  private static final String SETTINGS_MENU_CSS_RULE = "settingsMenu";
  private static final boolean HIDE_AUTH = false;

  interface FullViewUiBinder extends UiBinder<Widget, FullView> {
  }

  interface FullViewStyle extends CssResource {
    String selectedNavigation();

    String searchPlaceholderText();

    String methodSubtitle();
  }

  @UiField FullViewStyle style;

  @UiField DockLayoutPanel dockLayoutPanel;
  @UiField Image logo;
  @UiField PushButton backButton;

  @UiField Widget searchLoadingIndicator;
  @UiField(provided = true) SuggestBox searchBox;
  @UiField Panel searchErrorPanel;

  @UiField SectionedAggregator searchResults;

  @UiField EntryAggregatorView drillDownNav;

  @UiField Panel detailHeader;
  @UiField Panel detailTitleContainer;
  @UiField Panel authViewPlaceholder;

  @UiField Panel docsContainer;

  @UiField Panel detailPane;

  @UiField Panel preferredServicesMenuItem;
  @UiField Panel requestHistoryMenuItem;
  @UiField Panel allServicesMenuItem;

  @UiField MenuBar settingsMenu;
  @UiField MenuItem helpItem;
  @UiField MenuItem forumItem;
  @UiField MenuItem bugReportItem;

  private final FullViewPresenter presenter;
  private final AuthManager authManager;
  private final AnalyticsManager analytics;

  public FullView(URLManipulator urlManipulator, AuthManager authManager,
      AnalyticsManager analytics, SuggestOracle searchKeywords) {

    this.analytics = analytics;
    this.presenter = new FullViewPresenter(urlManipulator, this);
    this.authManager = authManager;
    PlaceholderTextBox searchBackingTextBox =
        new PlaceholderTextBox("Search for services, methods, and recent requests...");
    this.searchBox = new SuggestBox(searchKeywords, searchBackingTextBox);

    searchBox.setAutoSelectEnabled(false);
    initWidget(uiBinder.createAndBindUi(this));
    setMenuActions();

    // Add a fixed css class name that I can use to be able to style the menu.
    settingsMenu.setStyleName(SETTINGS_MENU_CSS_RULE + " " + settingsMenu.getStyleName());

    // Set the style of the search box.
    searchBackingTextBox.setPlaceholderTextStyleName(style.searchPlaceholderText());
  }

  /**
   * Assign the actions to the settings menu items.
   */
  private void setMenuActions() {
    bugReportItem.setCommand(getOpenUrlAction(REPORT_ERROR_URL));
    helpItem.setCommand(getOpenUrlAction(EXPLORER_HELP_URL));
    forumItem.setCommand(getOpenUrlAction(EXPLORER_FORUM_URL));
  }

  /**
   * Create a command that can be bound to a menu item that will open a url in a new tab.
   */
  private Command getOpenUrlAction(final String url) {
    return new Command() {
      @Override
      public void execute() {
        Window.open(url, NEW_TAB_TARGET, "");
      }
    };
  }

  @UiHandler("preferredServicesMenuItem")
  void clickPreferred(ClickEvent event) {
    presenter.clickNavigationItem(NavigationItem.PREFERRED_SERVICES);
  }

  @UiHandler("requestHistoryMenuItem")
  void clickHistory(ClickEvent event) {
    presenter.clickNavigationItem(NavigationItem.REQUEST_HISTORY);
  }

  @UiHandler("allServicesMenuItem")
  void clickAllVersions(ClickEvent event) {
    presenter.clickNavigationItem(NavigationItem.ALL_VERSIONS);
  }

  @UiHandler("logo")
  void clickLogo(ClickEvent event) {
    // Go back to the "home" state of the app when the logo is clicked.
    presenter.handleClickLogo();
  }

  @UiHandler("backButton")
  void clickBack(ClickEvent event) {
    presenter.handleClickBack();
  }

  @UiHandler("searchButton")
  void clickSearch(ClickEvent event) {
    presenter.handleSearch(searchBox.getText());
  }

  @UiHandler("searchBox")
  void searchBoxEnter(KeyDownEvent event) {
    if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
      SuggestionDisplay suggestionDisplay = searchBox.getSuggestionDisplay();

      // This should always be true unless GWT changes the type of the suggestion generated by the
      // SuggestBox. It is too complicated and nasty to switch out the SuggestBox suggestion display
      // factory, so we're left with this type safety check and broken functionality if GWT changes.
      Preconditions.checkState(suggestionDisplay instanceof DefaultSuggestionDisplay);

      // At this point this should always be true.
      if (suggestionDisplay instanceof DefaultSuggestionDisplay) {
        DefaultSuggestionDisplay suggestions = (DefaultSuggestionDisplay) suggestionDisplay;
        if (!suggestions.isSuggestionListShowing()) {
          presenter.handleSearch(searchBox.getValue());
        }
      }
    }
  }

  @UiHandler("searchBox")
  void suggestionSelected(SelectionEvent<Suggestion> event) {
    presenter.handleSearch(event.getSelectedItem().getReplacementString());
  }

  @Override
  public void setContext(ExplorerContext context) {
    presenter.setContext(context);

    // Fill in the entry list widget, only the collections that have entries will be shown
    drillDownNav.setVisible(context.isEntryListVisible());
    drillDownNav.clear();

    if (context.isEntryListVisible()) {
      populateHistoryItems("", context.getHistoryItems(), drillDownNav);
      populateServiceEntries(
          sortServices(context.getServicesList()), drillDownNav, context.getServiceTagProcessor());
      populateServiceMethods(context.getService(), context.getMethods(), drillDownNav);
    }

    // Fill in the detail pane.
    detailPane.setVisible(context.isHistoryItemVisible() || context.isMethodFormVisible());
    detailPane.clear();

    if (context.isHistoryItemVisible()) {
      HistoryItem item = Iterables.getOnlyElement(context.getHistoryItems());
      EmbeddedHistoryItemView view = generateHistoryItemView(item);

      detailPane.add(view);
    } else if (context.isMethodFormVisible()) {
      ApiMethod method = context.getMethod();

      // Wrap the callback given by the context so that we may also be notified when a request is
      // finished. Pass through events to the original callback.
      CallbackWrapper cbWrapper = new CallbackWrapper();
      cbWrapper.delegate = context.getRequestFinishedCallback();
      cbWrapper.methodName = method.getId();

      // Create the view of the request editor and the single history item.
      EmbeddedView view = new EmbeddedView(authManager,
          context.getService(),
          method,
          context.getMethodParameters(),
          cbWrapper,
          HIDE_AUTH,
          analytics);

      cbWrapper.localView = view;

      // If this context came bundled with a history item, that means the navigation references a
      // previous executed request, and we should show the result.
      List<HistoryItem> historyItems = context.getHistoryItems();
      if (!historyItems.isEmpty()) {
        view.showHistoryItem(generateHistoryItemView(Iterables.getLast(historyItems)));
      }

      detailPane.add(view);
    }

    // Show the search results.
    searchResults.setVisible(context.isSearchResultsVisible());
    searchResults.clear();
    searchErrorPanel.setVisible(false);
    if (context.isSearchResultsVisible()) {
      populateSearchResults(context.getSearchResults(), context.getServiceTagProcessor());
    }

    // Show the auth panel.
    authViewPlaceholder.setVisible(context.isAuthVisible());
    authViewPlaceholder.clear();
    if (context.isAuthVisible()) {
      showAuth(context.getService(), context.getMethod());
    }

    // Show the documentation link.
    docsContainer.setVisible(context.isDocsLinkVisible());
    docsContainer.clear();
    if (context.isDocsLinkVisible()) {
      showDocumentationLink("the " + context.getService().displayTitle(),
          context.getService().getDocumentationLink());
    }

    // Show the title.
    boolean showContentTitle = context.getContentTitles() != null;
    if (showContentTitle) {
      generateBreadcrumbs(detailTitleContainer, context.getContentTitles());
    }

    // Show the detail header.
    detailHeader.setVisible(showContentTitle || context.isAuthVisible());

    // Show the back button.
    backButton.setVisible(context.getParentUrl() != null);

    // Highlight the navigation item which was the root of our navigation.
    highlightNavigationItem(context.getRootNavigationItem());
  }

  /**
   * Generate a view of the provided history item.
   */
  private EmbeddedHistoryItemView generateHistoryItemView(HistoryItem item) {
    EmbeddedHistoryItemView view = new EmbeddedHistoryItemView(item.getRequest());
    view.complete(item.getResponse(), item.getEndTime() - item.getStartTime(),
        JsonPrettifier.LOCAL_LINK_FACTORY);
    return view;
  }

  /**
   * Generate breadcrumbs into the specified container using the format link > link > text where the
   * last breadcrumb is always plain text.
   */
  private void generateBreadcrumbs(Panel container, List<Title> titles) {
    container.clear();

    // For all of the titles previous to the last, add a link and a separator.
    for (Title notLast : titles.subList(0, titles.size() - 1)) {
      container.add(new InlineHyperlink(notLast.getTitle(), notLast.getFragment()));
      container.add(new InlineLabel(" > "));
    }

    // Append only the text for the last title.
    Title lastTitle = Iterables.getLast(titles);
    container.add(new InlineLabel(lastTitle.getTitle()));
    if (lastTitle.getSubtitle() != null) {
      Label subtitle = new InlineLabel(" - " + lastTitle.getSubtitle());
      subtitle.addStyleName(style.methodSubtitle());
      container.add(subtitle);
    }
  }

  private void showAuth(ApiService service, ApiMethod method) {
    AuthView auth = new AuthView(authManager, service, analytics);

    if (method != null) {
      auth.getPresenter().setStateForMethod(method);
    }

    authViewPlaceholder.add(auth);
  }

  private void showDocumentationLink(String componentName, String href) {
    docsContainer.add(
        new InlineLabel("Learn more about using " + componentName + " by reading the "));
    docsContainer.add(new Anchor("documentation", href, NEW_TAB_TARGET));
    docsContainer.add(new InlineLabel("."));
  }

  /**
   * Display the specified service entries in the container provided, while applying the tags
   * generated by the tag processor.
   */
  private void populateServiceEntries(Iterable<ServiceDefinition> services,
      EntryAggregatorView toPopulate,
      Set<TagProcessor> tagProcessors) {

    for (final ServiceDefinition service : services) {
      String iconUrl = service.getIcons().getIcon16Url();
      String displayName = NameHelper.generateDisplayTitle(service.getTitle(), service.getName());

      Set<DescriptionTag> tags = Sets.newHashSet();
      for (TagProcessor processor : tagProcessors) {
        tags.addAll(processor.process(service));
      }

      HasClickHandlers rowHandle = toPopulate.addEntry(new ServiceEntry(
          iconUrl, displayName, service.getVersion(), service.getDescription(), tags));
      rowHandle.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          presenter.handleClickService(service);
        }
      });
    }
  }

  /**
   * Display the spcified history items in the aggregator specified.
   *
   * @param prefix Prefix that should be prepended to the history item URL when an item is clicked,
   *        changes based on whether this was a search result or the history item list.
   * @param historyItems Items which to render and display in the aggregator,
   * @param aggregator Aggregator that will display rendered history items.
   */
  private void populateHistoryItems(
      final String prefix, Iterable<HistoryItem> historyItems, EntryAggregatorView aggregator) {

    for (final HistoryItem item : historyItems) {
      ApiRequest request = item.getRequest();
      HasClickHandlers rowHandler = aggregator.addEntry(new HistoryEntry(request.getMethod()
          .getId(), request.getHttpMethod().toString() + " " + request.getRequestPath(), item
          .getEndTime()));
      rowHandler.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          presenter.handleClickHistoryItem(prefix, item);
        }
      });
    }
  }

  /**
   * Display all of the methods for the specified service in the aggregator provided.
   */
  private void populateServiceMethods(
      ApiService service, Iterable<ApiMethod> methods, EntryAggregatorView view) {

    for (final ApiMethod method : methods) {
      populateMethodEntry(method, null, "", view);
    }
  }

  /**
   * Add an aggregator line for the particular method specified. When clicked, append the prefix
   * specified and then the method identifier to the current URL.
   */
  private void populateMethodEntry(final ApiMethod method, @Nullable String serviceTitle,
      final String prefix, EntryAggregatorView aggregator) {

    HasClickHandlers rowHandler = aggregator.addEntry(
        new MethodEntry(method.getId(), serviceTitle, method.getDescription()));
    rowHandler.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent arg0) {
        presenter.handleClickMethod(prefix, method);
      }
    });
  }

  /**
   * Take the list of search results and split them into appropriate aggregators hidden under
   * disclosure panels.
   */
  private void populateSearchResults(
      Iterable<SearchResult> results, Set<TagProcessor> serviceTagProcessors) {
    List<MethodBundle> methodResults = Lists.newArrayList();
    List<ServiceDefinition> serviceResults = Lists.newArrayList();
    List<HistoryItem> historyResults = Lists.newArrayList();

    for (SearchResult result : results) {
      switch(result.getKind()) {
        case HISTORY_ITEM:
          historyResults.add(result.getHistoryItem());
          break;

        case METHOD:
          methodResults.add(result.getMethodBundle());
          break;

        case SERVICE:
          serviceResults.add(result.getService());
          break;

        default:
          throw new RuntimeException("Unknown search result type: " + result.toString());
      }
    }

    if (!serviceResults.isEmpty()) {
      EntryAggregatorView serviceAggregator = new EntryAggregatorView();

      populateServiceEntries(serviceResults, serviceAggregator, serviceTagProcessors);

      searchResults.addSection("Services", serviceAggregator);
    }

    if (!methodResults.isEmpty()) {
      EntryAggregatorView methodAggregator = new EntryAggregatorView();

      for (MethodBundle bundle : methodResults) {
        String prefix =
            "m/" + bundle.getService().getName() + "/" + bundle.getService().getVersion() + "/";
        String serviceTitle =
            bundle.getService().displayTitle() + " " + bundle.getService().getVersion();
        populateMethodEntry(bundle.getMethod(), serviceTitle, prefix, methodAggregator);
      }

      searchResults.addSection("Methods", methodAggregator);
    }

    if (!historyResults.isEmpty()) {
      EntryAggregatorView historyAggregator = new EntryAggregatorView();
      populateHistoryItems("h/", historyResults, historyAggregator);
      searchResults.addSection("History", historyAggregator);
    }

    if (serviceResults.isEmpty() && methodResults.isEmpty() && historyResults.isEmpty()) {
      // There are no results, show the message
      searchResults.setVisible(false);
      searchErrorPanel.setVisible(true);
    }
  }

  /**
   * Highlight the navigation item for the root navigation item specified.
   */
  private void highlightNavigationItem(RootNavigationItem navItem) {
    preferredServicesMenuItem.removeStyleName(style.selectedNavigation());
    requestHistoryMenuItem.removeStyleName(style.selectedNavigation());
    allServicesMenuItem.removeStyleName(style.selectedNavigation());

    switch(navItem) {
      case ALL_VERSIONS:
        allServicesMenuItem.addStyleName(style.selectedNavigation());
        break;

      case PREFERRED_SERVICES:
        preferredServicesMenuItem.addStyleName(style.selectedNavigation());
        break;

      case REQUEST_HISTORY:
        requestHistoryMenuItem.addStyleName(style.selectedNavigation());
        break;
    }
  }

  @Override
  public void hideSearchLoadingIndicator() {
    searchLoadingIndicator.setVisible(false);
  }

  @Override
  public void searchReady() {
    // Delegate to the presenter
    presenter.searchReady();
  }

  private List<ServiceDefinition> sortServices(Set<ServiceDefinition> services) {
    List<ServiceDefinition> serviceList = Lists.newArrayList(services);
    Collections.sort(serviceList, new Comparator<ServiceDefinition>() {
      @Override
      public int compare(ServiceDefinition s1, ServiceDefinition s2) {
        String s1Title = NameHelper.generateDisplayTitle(s1.getTitle(), s1.getName());
        String s2Title = NameHelper.generateDisplayTitle(s2.getTitle(), s2.getName());
        return s1Title.toLowerCase().compareTo(s2Title.toLowerCase());
      }
    });
    return Collections.unmodifiableList(serviceList);
  }

  /**
   * Wrapper class that is used to siphon off request complete events, while still passing the
   * original events through to the wrapped delegate class.
   */
  private static class CallbackWrapper implements RequestFinishedCallback {
    public RequestFinishedCallback delegate;
    public EmbeddedView localView;
    public String methodName;

    private Map<ApiRequest, EmbeddedHistoryItemView> incompleteRequests = Maps.newHashMap();

    @Override
    public void finished(ApiRequest request, ApiResponse response, long startTime, long endTime) {
      EmbeddedHistoryItemView toComplete = incompleteRequests.get(request);
      toComplete.complete(response, endTime - startTime, JsonPrettifier.LOCAL_LINK_FACTORY);
      incompleteRequests.remove(request);

      delegate.finished(request, response, startTime, endTime);
    }

    @Override
    public void starting(ApiRequest request) {
      EmbeddedHistoryItemView incomplete = new EmbeddedHistoryItemView(request);
      incompleteRequests.put(request, incomplete);
      localView.showHistoryItem(incomplete);

      delegate.starting(request);
    }
  }
}
