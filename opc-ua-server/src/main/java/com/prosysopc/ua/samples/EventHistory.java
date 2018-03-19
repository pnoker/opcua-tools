/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.core.EventFilter;
import org.opcfoundation.ua.core.EventFilterResult;
import org.opcfoundation.ua.core.HistoryEventFieldList;

import com.prosysopc.ua.EventData;
import com.prosysopc.ua.EventListener;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.server.ContentFilterDefinition;
import com.prosysopc.ua.server.nodes.UaObjectNode;

/**
 *
 */
public class EventHistory {
	private final int capacity = 10000;
	private final List<EventData> events = new CopyOnWriteArrayList<EventData>();
	private final EventListener listener = new EventListener() {

		@Override
		public boolean isMonitored(UaNode event) {
			return false;
		}

		@Override
		public void onEvent(UaNode node, EventData eventData) {
			events.add(eventData);
			while (events.size() > capacity)
				events.remove(0);
		}
	};
	private final UaObjectNode node;

	/**
	 * @param node
	 */
	public EventHistory(UaObjectNode node) {
		super();
		this.node = node;
		node.addEventListener(listener);
	}

	/**
	 * @param eventIds
	 * @param operationResults
	 * @param operationDiagnostics
	 */
	public void deleteEvents(byte[][] eventIds, StatusCode[] operationResults, DiagnosticInfo[] operationDiagnostics) {
		for (int i = events.size() - 1; i >= 0; i--) {
			EventData event = events.get(i);
			byte[] id1 = event.getEventId();
			for (byte[] eventId : eventIds)
				if (Arrays.equals(eventId, id1)) {
					events.remove(i);
					break;
				}
		}
	}

	/**
	 * @param startTime
	 *            the start of the interval
	 * @param endTime
	 *            the end of the interval
	 * @param maxValues
	 *            maximum number of values to return
	 * @param eventFilter
	 *            the event filter that defines the fields and events to return
	 * @param firstIndex
	 *            the index of the first entry in the history data to return
	 *            (i.e. the continuationPoint returned for the previous request)
	 * @param history
	 *            the list of values to fill in
	 * @return the first index that was not added to the history, in case there
	 *         are more than maxValues entries to return (i.e. the
	 *         continuationPoint to return)
	 */
	public Integer readEvents(DateTime startTime, DateTime endTime, int maxValues, EventFilter eventFilter,
			List<HistoryEventFieldList> history, int firstIndex) {
		int i = 0;
		boolean startTimeDefined = startTime.compareTo(DateTime.MIN_VALUE) > 0;
		boolean endTimeDefined = endTime.compareTo(DateTime.MIN_VALUE) > 0;
		List<List<QualifiedName>> fieldPaths = new ArrayList<List<QualifiedName>>();
		ContentFilterDefinition filterDefinition = new ContentFilterDefinition();
		EventFilterResult eventFilterResult = new EventFilterResult();
		ContentFilterDefinition.parseEventFilter(node.getNodeManager().getNodeManagerTable().getNodeManagerRoot(),
				eventFilter, fieldPaths, filterDefinition, eventFilterResult);
		if (startTimeDefined || !endTimeDefined)
			for (int j = 0; j < events.size(); j++) {
				EventData event = events.get(j);
				DateTime t = event.getTime();
				final int compareToEnd = endTimeDefined ? t.compareTo(endTime) : -1;
				if (compareToEnd > 0)
					break;
				else {
					final int compareToStart = t.compareTo(startTime);
					if (compareToStart >= 0) {
						if ((i >= firstIndex) && filterDefinition.evaluate(event, true))
							history.add(new HistoryEventFieldList(event.getFieldValues(fieldPaths)));
						i++;
						if (history.size() == maxValues)
							// Return continuation point if no more events exist
							// and both timestamps were defined
							return endTimeDefined && (j < events.size()) ? i : null;
					}
				}
			}
		else
			// !startTimeDefined && endTimeDefined
			for (int j = events.size() - 1; j >= 0; j--) {
				EventData event = events.get(j);
				DateTime t = event.getTime();
				final int compareToEnd = t.compareTo(endTime);
				if (compareToEnd > 0)
					continue;
				else {
					if (i >= firstIndex)
						history.add(new HistoryEventFieldList(event.getFieldValues(fieldPaths)));
					i++;
					if (history.size() == maxValues)
						// Return continuation point if no more events exist and
						// both timestamps were defined
						return startTimeDefined && (j > 0) ? i : null;
				}
			}
		return null;
	}

}
