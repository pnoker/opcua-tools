/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedShort;
import org.opcfoundation.ua.core.StatusCodes;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.DataChangeListener;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.nodes.UaVariableNode;

/**
 * A sample class for keeping a history of a variable node.
 */
class ValueHistory {
	private int capacity = 10000;
	private final DataChangeListener listener = new DataChangeListener() {

		@Override
		public void onDataChange(UaNode uaNode, DataValue prevValue, DataValue value) {
			values.add(value);
			while (values.size() > capacity)
				values.remove(0);
		}
	};
	private final List<DataValue> values = new CopyOnWriteArrayList<DataValue>();
	private final UaVariable variable;

	public ValueHistory(UaVariableNode variable) {
		super();
		this.variable = variable;
		variable.addDataChangeListener(listener);
	}

	/**
	 * @param reqTimes
	 * @param operationResults
	 * @param operationDiagnostics
	 */
	public void deleteAtTimes(DateTime[] reqTimes, StatusCode[] operationResults,
			DiagnosticInfo[] operationDiagnostics) {
		for (int i = 0; i < reqTimes.length; i++)
			try {
				deleteAtTime(reqTimes[i]);
				operationResults[i] = StatusCode.GOOD;
			} catch (StatusException e) {
				operationResults[i] = e.getStatusCode();
				operationDiagnostics[i] = e.getDiagnosticInfo();
			}
	}

	/**
	 * @param startTime
	 * @param endTime
	 * @throws StatusException
	 */
	public void deleteRaw(DateTime startTime, DateTime endTime) throws StatusException {
		int i = 0;
		// boolean startTimeDefined = startTime.compareTo(DateTime.MIN_VALUE) >
		// 0;
		boolean endTimeDefined = endTime.compareTo(DateTime.MIN_VALUE) > 0;
		if (!endTimeDefined)
			throw new StatusException(StatusCodes.Bad_InvalidArgument);
		while (values.size() > i) {
			DataValue value = values.get(i);
			DateTime t = value.getSourceTimestamp();
			if (t == null)
				t = value.getServerTimestamp();
			if (t.compareTo(startTime) >= 0)
				values.remove(i);
			else if (t.compareTo(endTime) >= 0)
				break;
			else
				i++;
		}
	}

	public int getCapacity() {
		return capacity;
	}

	/**
	 * @return the variable
	 */
	public UaVariable getVariable() {
		return variable;
	}

	/**
	 * @param reqTimes
	 * @return
	 */
	public DataValue[] readAtTimes(DateTime[] reqTimes) {
		if (reqTimes == null)
			return null;
		DataValue[] values = new DataValue[reqTimes.length];
		for (int i = 0; i < reqTimes.length; i++) {
			DateTime t = reqTimes[i];
			// Stepped interpolation used to get values
			DataValue v = getValue(t);
			values[i] = new DataValue(v == null ? null : v.getValue(),
					v == null ? new StatusCode(StatusCodes.Bad_NoData) : v.getStatusCode(), t, UnsignedShort.ZERO, null,
					null);
		}
		return values;

	}

	/**
	 * Get the values from the history that are between startTime and endTime.
	 *
	 * @param startTime
	 *            the start of the interval
	 * @param endTime
	 *            the end of the interval
	 * @param maxValues
	 *            maximum number of values to return
	 * @param returnBounds
	 *            whether values at the ends of the interval should be returned
	 *            as well
	 * @param firstIndex
	 *            the index of the first entry in the history data to return
	 *            (i.e. the continuationPoint returned for the previous request)
	 * @param history
	 *            the list of values to fill in
	 * @return the first index that was not added to the history, in case there
	 *         are more than maxValues entries to return (i.e. the
	 *         continuationPoint to return)
	 */
	public Integer readRaw(DateTime startTime, DateTime endTime, int maxValues, boolean returnBounds, int firstIndex,
			List<DataValue> history) {
		int i = 0;
		boolean startTimeDefined = startTime.compareTo(DateTime.MIN_VALUE) > 0;
		boolean endTimeDefined = endTime.compareTo(DateTime.MIN_VALUE) > 0;
		if (startTimeDefined || !endTimeDefined)
			for (DataValue value : values) {
				DateTime t = value.getSourceTimestamp();
				if (t == null)
					t = value.getServerTimestamp();
				final int compareToEnd = endTimeDefined ? t.compareTo(endTime) : -1;
				if ((compareToEnd > 0) || (!returnBounds && (compareToEnd == 0)))
					break;
				else {
					final int compareToStart = t.compareTo(startTime);
					if ((compareToStart > 0) || (returnBounds && (compareToStart == 0))) {
						if (i >= firstIndex)
							history.add(value);
						i++;
						if (history.size() == maxValues)
							return i;
					}
				}
			}
		else
			// !startTimeDefined && endTimeDefined
			for (int j = values.size() - 1; j >= 0; j--) {
				DataValue value = values.get(j);
				DateTime t = value.getSourceTimestamp();
				if (t == null)
					t = value.getServerTimestamp();
				final int compareToEnd = t.compareTo(endTime);
				if ((compareToEnd > 0) || (!returnBounds && (compareToEnd == 0)))
					continue;
				else {
					if (i >= firstIndex)
						history.add(value);
					i++;
					if (history.size() == maxValues)
						return i;
				}
			}
		return null;
	}

	/**
	 * @param capacity
	 *            the capacity to set
	 */
	public void setCapacity(int capacity) {
		if (capacity < 0)
			throw new IllegalArgumentException("capacity must be a positive value");
		this.capacity = capacity;
	}

	/**
	 * Delete a single entry from the history
	 *
	 * @param timestamp
	 *            the sourceTimestamp to look for
	 * @throws StatusException
	 *             if no sample with the given timestamp is found
	 */
	private void deleteAtTime(DateTime timestamp) throws StatusException {
		boolean found = false;
		for (int i = values.size() - 1; i >= 0; i--) {
			int compareTo = timestamp.compareTo(values.get(i).getSourceTimestamp());
			if (compareTo == 0) {
				values.remove(i);
				found = true;
			} else if (compareTo < 0)
				break;
		}
		if (!found)
			throw new StatusException(StatusCodes.Bad_NoData);

	}

	/**
	 * Find the value at the given time from the history using stepped
	 * interpolation.
	 *
	 * @param requestedTime
	 *            the requested time for the value
	 * @return the last value with a smaller or equal timestamp than the
	 *         requestedTime
	 */
	private DataValue getValue(DateTime requestedTime) {
		// a "brute" find starting from the end
		int i = values.size() - 1;
		while ((i >= 0) && (values.get(i).getSourceTimestamp().compareTo(requestedTime) > 0))
			i--;
		return i < 0 ? null : values.get(i);
	}
}