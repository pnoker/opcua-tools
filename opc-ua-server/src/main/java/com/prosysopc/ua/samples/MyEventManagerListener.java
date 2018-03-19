/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import java.util.Arrays;

import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.core.EventFilter;
import org.opcfoundation.ua.core.EventFilterResult;
import org.opcfoundation.ua.core.StatusCodes;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.server.EventManager;
import com.prosysopc.ua.server.EventManagerListener;
import com.prosysopc.ua.server.MonitoredEventItem;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.Subscription;
import com.prosysopc.ua.types.opcua.server.AcknowledgeableConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.AlarmConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.ConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.ShelvedStateMachineTypeNode;

/**
 * A sample implementation of an EventManagerListener
 */
public class MyEventManagerListener implements EventManagerListener {

	private int eventId = 0;

	@Override
	public boolean onAcknowledge(ServiceContext serviceContext, AcknowledgeableConditionTypeNode condition,
			byte[] eventId, LocalizedText comment) throws StatusException {
		// Handle acknowledge request to a condition event
		println("Acknowledge: Condition=" + condition + "; EventId=" + eventIdToString(eventId) + "; Comment="
				+ comment);
		// If the acknowledged event is no longer active, return an error
		if (!Arrays.equals(eventId, condition.getEventId()))
			throw new StatusException(StatusCodes.Bad_EventIdUnknown);
		if (condition.isAcked())
			throw new StatusException(StatusCodes.Bad_ConditionBranchAlreadyAcked);

		final DateTime now = DateTime.currentTime();
		condition.setAcked(true, now);
		final byte[] userEventId = getNextUserEventId();
		// addComment triggers a new event
		condition.addComment(eventId, comment, now, userEventId);
		return true; // Handled here
		// NOTE: If you do not handle acknowledge here, and return false,
		// the EventManager (or MethodManager) will call
		// condition.acknowledge, which performs the same actions as this
		// handler, except for setting Retain
	}

	@Override
	public boolean onAddComment(ServiceContext serviceContext, ConditionTypeNode condition, byte[] eventId,
			LocalizedText comment) throws StatusException {
		// Handle add command request to a condition event
		println("AddComment: Condition=" + condition + "; Event=" + eventIdToString(eventId) + "; Comment=" + comment);
		// Only the current eventId can get comments
		if (!Arrays.equals(eventId, condition.getEventId()))
			throw new StatusException(StatusCodes.Bad_EventIdUnknown);
		// triggers a new event
		final byte[] userEventId = getNextUserEventId();
		condition.addComment(eventId, comment, DateTime.currentTime(), userEventId);
		return true; // Handled here
		// NOTE: If you do not handle addComment here, and return false,
		// the EventManager (or MethodManager) will call
		// condition.addComment automatically
	}

	@Override
	public void onAfterCreateMonitoredEventItem(ServiceContext serviceContext, Subscription subscription,
			MonitoredEventItem item) {
		//
	}

	@Override
	public void onAfterDeleteMonitoredEventItem(ServiceContext serviceContext, Subscription subscription,
			MonitoredEventItem item) {
		//
	}

	@Override
	public void onAfterModifyMonitoredEventItem(ServiceContext serviceContext, Subscription subscription,
			MonitoredEventItem item) {
		//
	}

	@Override
	public void onConditionRefresh(ServiceContext serviceContext, Subscription subscription) throws StatusException {
		//
	}

	@Override
	public boolean onConfirm(ServiceContext serviceContext, AcknowledgeableConditionTypeNode condition, byte[] eventId,
			LocalizedText comment) throws StatusException {
		// Handle confirm request to a condition event
		println("Confirm: Condition=" + condition + "; EventId=" + eventIdToString(eventId) + "; Comment=" + comment);
		// If the confirmed event is no longer active, return an error
		if (!Arrays.equals(eventId, condition.getEventId()))
			throw new StatusException(StatusCodes.Bad_EventIdUnknown);
		if (condition.isConfirmed())
			throw new StatusException(StatusCodes.Bad_ConditionBranchAlreadyConfirmed);
		if (!condition.isAcked())
			throw new StatusException("Condition can only be confirmed when it is acknowledged.",
					StatusCodes.Bad_InvalidState);
		// If the condition is no longer active, set retain to false, i.e.
		// remove it from the visible alarms
		if (!(condition instanceof AlarmConditionTypeNode) || !((AlarmConditionTypeNode) condition).isActive())
			condition.setRetain(false);

		final DateTime now = DateTime.currentTime();
		condition.setConfirmed(true, now);
		final byte[] userEventId = getNextUserEventId();
		// addComment triggers a new event
		condition.addComment(eventId, comment, now, userEventId);
		return true; // Handled here
		// NOTE: If you do not handle Confirm here, and return false,
		// the EventManager (or MethodManager) will call
		// condition.confirm, which performs the same actions as this
		// handler
	}

	@Override
	public void onCreateMonitoredEventItem(ServiceContext serviceContext, NodeId nodeId, EventFilter eventFilter,
			EventFilterResult filterResult) throws StatusException {
		// Item created
	}

	@Override
	public void onDeleteMonitoredEventItem(ServiceContext serviceContext, Subscription subscription,
			MonitoredEventItem monitoredItem) {
		// Stop monitoring the item?
	}

	@Override
	public boolean onDisable(ServiceContext serviceContext, ConditionTypeNode condition) throws StatusException {
		// Handle disable request to a condition
		println("Disable: Condition=" + condition);
		if (condition.isEnabled()) {
			DateTime now = DateTime.currentTime();
			// Setting enabled to false, also sets retain to false
			condition.setEnabled(false, now);
			// notify the clients of the change
			condition.triggerEvent(now, null, getNextUserEventId());
		}
		return true; // Handled here
		// NOTE: If you do not handle disable here, and return false,
		// the EventManager (or MethodManager) will request the
		// condition to handle the call, and it will unset the enabled
		// state, and triggers a new notification event, as here
	}

	@Override
	public boolean onEnable(ServiceContext serviceContext, ConditionTypeNode condition) throws StatusException {
		// Handle enable request to a condition
		println("Enable: Condition=" + condition);
		if (!condition.isEnabled()) {
			DateTime now = DateTime.currentTime();
			condition.setEnabled(true, now);
			// You should evaluate the condition now, set Retain to true,
			// if necessary and in that case also call triggerEvent
			// condition.setRetain(true);
			// condition.triggerEvent(now, null, getNextUserEventId());
		}
		return true; // Handled here
		// NOTE: If you do not handle enable here, and return false,
		// the EventManager (or MethodManager) will request the
		// condition to handle the call, and it will set the enabled
		// state.

		// You should however set the status of the condition yourself
		// and trigger a new event if necessary
	}

	@Override
	public void onModifyMonitoredEventItem(ServiceContext serviceContext, Subscription subscription,
			MonitoredEventItem monitoredItem, EventFilter eventFilter, EventFilterResult filterResult)
					throws StatusException {
		// Modify event monitoring, when the client modifies a monitored
		// item
	}

	@Override
	public boolean onOneshotShelve(ServiceContext serviceContext, AlarmConditionTypeNode condition,
			ShelvedStateMachineTypeNode stateMachine) throws StatusException {
		return false;
	}

	@Override
	public boolean onTimedShelve(ServiceContext serviceContext, AlarmConditionTypeNode condition,
			ShelvedStateMachineTypeNode stateMachine, double shelvingTime) throws StatusException {
		return false;
	}

	@Override
	public boolean onUnshelve(ServiceContext serviceContext, AlarmConditionTypeNode condition,
			ShelvedStateMachineTypeNode stateMachine) throws StatusException {
		return false;
	}

	private String eventIdToString(byte[] eventId) {
		return eventId == null ? "(null)" : Arrays.toString(eventId);
	}

	/**
	 * @param string
	 */
	private void println(String string) {
		MyNodeManager.println(string);
	}

	/**
	 * @return
	 * @throws RuntimeException
	 */
	byte[] getNextUserEventId() throws RuntimeException {
		return EventManager.createEventId(eventId++);
	}
}
