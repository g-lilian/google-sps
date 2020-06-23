// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Set;

public final class FindMeetingQuery {
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // throw new UnsupportedOperationException("TODO: Implement this method."); 
    long reqDuration = request.getDuration();
    Collection<String> reqAttendees = request.getAttendees();
    ArrayList<TimeRange> possibleTimes = new ArrayList<TimeRange>();

    // Special cases
    if (reqDuration > TimeRange.WHOLE_DAY.duration()) {
        return possibleTimes;
    }

    // Collection of clashing events' (events where at least one attendee belongs to reqAttendees) timeranges
    ArrayList<TimeRange> clashingEventTimes = new ArrayList<TimeRange>();

    // Check each event in events collection
    for (Event event : events) {
        Set<String> eventAttendees = event.getAttendees();

        // Check if any of the requested attendees need to attend this event
        boolean isClashingEvent = false;
        for (String attendee : eventAttendees) {
            if (reqAttendees.contains(attendee)) {
                isClashingEvent = true;
                break;
            }
        }
        if (isClashingEvent == false) continue;
        clashingEventTimes.add(event.getWhen());
    }

    // Check for case where there is no clashing event
    int numEvents = clashingEventTimes.size();
    if (numEvents == 0) {
        possibleTimes.add(TimeRange.WHOLE_DAY);
        return possibleTimes;
    }

    // Sort by ascending start time
    Collections.sort(clashingEventTimes, TimeRange.ORDER_BY_START);

    // First check if there is space before the start of the first event
    int startOfFirstEvent = clashingEventTimes.get(0).start();
    if (startOfFirstEvent >= reqDuration) {
        TimeRange possibleTime = new TimeRange(0, startOfFirstEvent);
        possibleTimes.add(possibleTime);
    }

    // Find available times in between the clashing events
    int start = clashingEventTimes.get(0).start();
    int end = clashingEventTimes.get(0).end();

    for (int i=0; i<numEvents; i++) {
        TimeRange eventTime = clashingEventTimes.get(i);
        if (i+1 < numEvents) { // If this is not the last event
            // Check the next event's time range
            TimeRange nextEventTime = clashingEventTimes.get(i+1);
            if (eventTime.contains(nextEventTime)) continue;
            if (eventTime.overlaps(nextEventTime)) {
                end = nextEventTime.end();
            } else {
                // Check if time between events is enough for meeting duration
                int timeBetweenEvents = nextEventTime.start() - end;
                if (timeBetweenEvents >= reqDuration) {
                    TimeRange possibleTime = new TimeRange(end, timeBetweenEvents);
                    possibleTimes.add(possibleTime);
                }
                start = nextEventTime.start();
                end = nextEventTime.end();
            }
        }
    }

    // Check if there is space after the end of the last event
    int timeAfterLastEvent = TimeRange.END_OF_DAY - end + 1;
    if (timeAfterLastEvent >= reqDuration) {
        TimeRange possibleTime = new TimeRange(end, timeAfterLastEvent);
        possibleTimes.add(possibleTime);
    }

    return possibleTimes;
  }
}
