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
import java.util.stream.Collectors;  

public final class FindMeetingQuery {
    ArrayList<TimeRange> possibleTimes;

    public FindMeetingQuery() {
        possibleTimes = new ArrayList<TimeRange>();
    }

    public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
        long reqDuration = request.getDuration();
        Collection<String> reqAttendees = request.getAttendees();

        // Special cases
        if (reqDuration > TimeRange.WHOLE_DAY.duration()) {
            return possibleTimes;
        }

        // Collection of clashing events' (events where at least one attendee belongs to reqAttendees) timeranges
        ArrayList<TimeRange> clashingEventTimes = new ArrayList<TimeRange>();

        // Check each event in events collection
        // clashingEventTimes = events.stream()
        //     .filter(event -> isClashingEvent(event, reqAttendees))
        //     .map(event -> event.getWhen())
        //     .collect(Collectors.toList()); 
        // print(clashingEventTimes);
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
        addPossibleTimeRange(0, startOfFirstEvent, reqDuration);

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
                    addPossibleTimeRange(end, nextEventTime.start(), reqDuration);
                    start = nextEventTime.start();
                    end = nextEventTime.end();
                }
            }
        }

        // Check if there is space after the end of the last event
        addPossibleTimeRange(end, TimeRange.END_OF_DAY + 1, reqDuration);

        return possibleTimes;
    }

    private boolean isClashingEvent(Event event, Collection<String> reqAttendees) {
        Set<String> eventAttendees = event.getAttendees();
        for (String attendee : eventAttendees) {
            if (reqAttendees.contains(attendee)) {
                return true;
            }
        }
        return false;
    }

    private void addPossibleTimeRange(int start, int end, long reqDuration) {
        if (end - start >= reqDuration) {
            TimeRange possibleTime = new TimeRange(start, end - start);
            possibleTimes.add(possibleTime);
        }
    }
}
