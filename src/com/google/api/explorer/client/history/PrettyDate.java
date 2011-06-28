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

package com.google.api.explorer.client.history;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Timer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles the human-readability of dates in the form of "X
 * minutes/hours/days/weeks ago".
 *
 * @author jasonhall@google.com (Jason Hall)
 */
public abstract class PrettyDate {

  private PrettyDate() {
  }

  private static String getPrettyText(Date date) {
    long diff = (new Date().getTime() - date.getTime()) / 1000;
    double dayDiff = Math.floor(diff / 86400);

    if (diff < 0) {
      return "in the future?";
    } else if (diff < 60) {
      return "moments ago";
    } else if (diff < 120) {
      return "one minute ago";
    } else if (diff < 3600) {
      return diff / 60 + " minutes ago";
    } else if (diff < 7200) {
      return "one hour ago";
    } else if (diff < 86400) {
      return diff / 3600 + " hours ago";
    } else if (dayDiff == 1) {
      return "yesterday";
    } else if (dayDiff < 7) {
      return dayDiff + " days ago";
    } else {
      return Math.ceil(dayDiff / 7) + " weeks ago";
    }
  }

  /**
   * Sets the text to the pretty-date, once, and never again.
   */
  public static void makePretty(Date date, String prefix, Element element) {
    element.setInnerText(prefix + getPrettyText(date));
  }

  private static final Map<Element, Timer> TIMERS = new HashMap<Element, Timer>();

  /**
   * Schedules a repeating timer to continually update the text with the
   * pretty-date, every minute until the text is more than an hour old, at which
   * point it stops. The assumption is that nobody will wait for 60+ minutes for
   * "one hour ago" to turn into "2 hours ago"
   */
  public static void keepMakingPretty(final Date date, final String prefix, final Element element) {
    makePretty(date, prefix, element);
    if (!TIMERS.containsKey(element)) {
      Timer timer = new Timer() {
        @Override
        public void run() {
          makePretty(date, prefix, element);
          if (element.getInnerText().contains("hour")) {
            stopMakingPretty(element);
          }
        }
      };
      timer.scheduleRepeating(60000);
      TIMERS.put(element, timer);
    }
  }

  public static void stopMakingPretty(Element element) {
    if (TIMERS.containsKey(element)) {
      TIMERS.get(element).cancel();
      TIMERS.remove(element);
    }
  }
}
