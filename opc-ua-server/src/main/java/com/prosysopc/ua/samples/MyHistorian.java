/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.AggregateConfiguration;
import org.opcfoundation.ua.core.EventFilter;
import org.opcfoundation.ua.core.HistoryData;
import org.opcfoundation.ua.core.HistoryEvent;
import org.opcfoundation.ua.core.HistoryEventFieldList;
import org.opcfoundation.ua.core.HistoryModifiedData;
import org.opcfoundation.ua.core.HistoryReadDetails;
import org.opcfoundation.ua.core.HistoryReadValueId;
import org.opcfoundation.ua.core.HistoryUpdateDetails;
import org.opcfoundation.ua.core.HistoryUpdateResult;
import org.opcfoundation.ua.core.PerformUpdateType;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.utils.NumericRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prosysopc.ua.EventNotifierClass;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.server.HistoryContinuationPoint;
import com.prosysopc.ua.server.HistoryManagerListener;
import com.prosysopc.ua.server.HistoryResult;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.server.nodes.UaVariableNode;

/**
 * A sample implementation of a data historian.
 * <p>
 * It is implemented as a HistoryManagerListener. It could as well be a
 * HistoryManager, instead.
 */
public class MyHistorian implements HistoryManagerListener {
	private static Logger logger = LoggerFactory.getLogger(MyHistorian.class);
	private final Map<UaObjectNode, EventHistory> eventHistories = new HashMap<UaObjectNode, EventHistory>();

	// The variable histories
	private final Map<UaVariableNode, ValueHistory> variableHistories = new HashMap<UaVariableNode, ValueHistory>();

	public MyHistorian() {
		super();
	}

	/**
	 * Add the object to the historian for event history.
	 * <p>
	 * The historian will mark it to contain history (in EventNotifier
	 * attribute) and it will start monitoring events for it.
	 *
	 * @param node
	 *            the object to initialize
	 */
	public void addEventHistory(UaObjectNode node) {
		EventHistory history = new EventHistory(node);
		// History can be read
		EnumSet<EventNotifierClass> eventNotifier = node.getEventNotifier();
		eventNotifier.add(EventNotifierClass.HistoryRead);
		node.setEventNotifier(eventNotifier);

		eventHistories.put(node, history);
	}

	/**
	 * Add the variable to the historian.
	 * <p>
	 * The historian will mark it to be historized and it will start monitoring
	 * value changes for it.
	 *
	 * @param variable
	 *            the variable to initialize
	 */
	public void addVariableHistory(UaVariableNode variable) {
		ValueHistory history = new ValueHistory(variable);
		// History is being collected
		variable.setHistorizing(true);
		// History can be read
		final EnumSet<AccessLevel> READ_WRITE_HISTORYREAD = EnumSet.of(AccessLevel.CurrentRead,
				AccessLevel.CurrentWrite, AccessLevel.HistoryRead);
		variable.setAccessLevel(READ_WRITE_HISTORYREAD);
		variableHistories.put(variable, history);
	}

	@Override
	public Object onBeginHistoryRead(ServiceContext serviceContext, HistoryReadDetails details,
			TimestampsToReturn timestampsToReturn, HistoryReadValueId[] nodesToRead,
			HistoryContinuationPoint[] continuationPoints, HistoryResult[] results) throws ServiceException {
		return null;
	}

	@Override
	public Object onBeginHistoryUpdate(ServiceContext serviceContext, HistoryUpdateDetails[] details,
			HistoryUpdateResult[] results, DiagnosticInfo[] diagnosticInfos) throws ServiceException {
		return null;
	}

	@Override
	public void onDeleteAtTimes(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node,
			DateTime[] reqTimes, StatusCode[] operationResults, DiagnosticInfo[] operationDiagnostics)
			throws StatusException {
		ValueHistory history = variableHistories.get(node);
		if (history != null)
			history.deleteAtTimes(reqTimes, operationResults, operationDiagnostics);
		else
			throw new StatusException(StatusCodes.Bad_NoData);
	}

	@Override
	public void onDeleteEvents(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node,
			byte[][] eventIds, StatusCode[] operationResults, DiagnosticInfo[] operationDiagnostics)
			throws StatusException {
		EventHistory history = eventHistories.get(node);
		if (history != null)
			history.deleteEvents(eventIds, operationResults, operationDiagnostics);
		else
			throw new StatusException(StatusCodes.Bad_NoData);
	}

	@Override
	public void onDeleteModified(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node,
			DateTime startTime, DateTime endTime) throws StatusException {
		throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
	}

	@Override
	public void onDeleteRaw(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node,
			DateTime startTime, DateTime endTime) throws StatusException {
		ValueHistory history = variableHistories.get(node);
		if (history != null)
			history.deleteRaw(startTime, endTime);
		else
			throw new StatusException(StatusCodes.Bad_NoData);
	}

	@Override
	public void onEndHistoryRead(ServiceContext serviceContext, Object operationContext, HistoryReadDetails details,
			TimestampsToReturn timestampsToReturn, HistoryReadValueId[] nodesToRead,
			HistoryContinuationPoint[] continuationPoints, HistoryResult[] results) throws ServiceException {
	}

	@Override
	public void onEndHistoryUpdate(ServiceContext serviceContext, Object operationContext,
			HistoryUpdateDetails[] details, HistoryUpdateResult[] results, DiagnosticInfo[] diagnosticInfos)
			throws ServiceException {
	}

	@Override
	public Object onReadAtTimes(ServiceContext serviceContext, Object operationContext,
			TimestampsToReturn timestampsToReturn, NodeId nodeId, UaNode node, Object continuationPoint,
			DateTime[] reqTimes, NumericRange indexRange, HistoryData historyData) throws StatusException {
		if (logger.isDebugEnabled())
			logger.debug("onReadAtTimes: reqTimes=[" + reqTimes.length + "] "
					+ ((reqTimes.length < 20) ? Arrays.toString(reqTimes) : ""));
		ValueHistory history = variableHistories.get(node);
		if (history != null)
			historyData.setDataValues(history.readAtTimes(reqTimes));
		else
			throw new StatusException(StatusCodes.Bad_NoData);
		return null;
	}

	@Override
	public Object onReadEvents(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node,
			Object continuationPoint, DateTime startTime, DateTime endTime, UnsignedInteger numValuesPerNode,
			EventFilter filter, HistoryEvent historyEvent) throws StatusException {
		EventHistory history = eventHistories.get(node);
		if (history != null) {
			List<HistoryEventFieldList> events = new ArrayList<HistoryEventFieldList>();
			int firstIndex = continuationPoint == null ? 0 : (Integer) continuationPoint;
			Integer newContinuationPoint = history.readEvents(startTime, endTime, numValuesPerNode.intValue(), filter,
					events, firstIndex);
			historyEvent.setEvents(events.toArray(new HistoryEventFieldList[events.size()]));
			return newContinuationPoint;
		} else
			throw new StatusException(StatusCodes.Bad_NoData);
	}

	@Override
	public Object onReadModified(ServiceContext serviceContext, Object operationContext,
			TimestampsToReturn timestampsToReturn, NodeId nodeId, UaNode node, Object continuationPoint,
			DateTime startTime, DateTime endTime, UnsignedInteger numValuesPerNode, NumericRange indexRange,
			HistoryModifiedData historyData) throws StatusException {
		throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
	}

	@Override
	public Object onReadProcessed(ServiceContext serviceContext, Object operationContext,
			TimestampsToReturn timestampsToReturn, NodeId nodeId, UaNode node, Object continuationPoint,
			DateTime startTime, DateTime endTime, Double processingInterval, NodeId aggregateType,
			AggregateConfiguration aggregateConfiguration, NumericRange indexRange, HistoryData historyData)
			throws StatusException {
		logger.debug("onReadProcessed: nodeId={}, startTime={}, endime={}, processingInterval={}", nodeId, startTime,
				endTime, processingInterval);
		throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
	}

	@Override
	public Object onReadRaw(ServiceContext serviceContext, Object operationContext,
			TimestampsToReturn timestampsToReturn, NodeId nodeId, UaNode node, Object continuationPoint,
			DateTime startTime, DateTime endTime, UnsignedInteger numValuesPerNode, Boolean returnBounds,
			NumericRange indexRange, HistoryData historyData) throws StatusException {
		logger.debug("onReadRaw: startTime={} endTime={} numValuesPerNode={}", startTime, endTime, numValuesPerNode);
		ValueHistory history = variableHistories.get(node);
		if (history != null) {
			List<DataValue> values = new ArrayList<DataValue>();
			int firstIndex = continuationPoint == null ? 0 : (Integer) continuationPoint;
			Integer newContinuationPoint = history.readRaw(startTime, endTime, numValuesPerNode.intValue(),
					returnBounds, firstIndex, values);
			historyData.setDataValues(values.toArray(new DataValue[values.size()]));
			return newContinuationPoint;
		}
		return null;
	}

	@Override
	public void onUpdateData(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node,
			DataValue[] updateValues, PerformUpdateType performInsertReplace, StatusCode[] operationResults,
			DiagnosticInfo[] operationDiagnostics) throws StatusException {
		throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
	}

	@Override
	public void onUpdateEvent(ServiceContext serviceContext, Object operationContext, NodeId nodeId, UaNode node,
			Variant[] eventFields, EventFilter filter, PerformUpdateType performInsertReplace,
			StatusCode[] operationResults, DiagnosticInfo[] operationDiagnostics) throws StatusException {
		throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
	}

	@Override
	public void onUpdateStructureData(ServiceContext serviceContext, Object operationContext, NodeId nodeId,
			UaNode node, DataValue[] updateValues, PerformUpdateType performUpdateType, StatusCode[] operationResults,
			DiagnosticInfo[] operationDiagnostics) throws StatusException {
		throw new StatusException(StatusCodes.Bad_HistoryOperationUnsupported);
	}

};
