/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.Variant;

import com.prosysopc.ua.nodes.DataChangeListener;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.server.nodes.UaVariableNode;
import com.prosysopc.ua.types.opcua.server.ExclusiveLevelAlarmTypeNode;

public class MyLevelAlarmType extends ExclusiveLevelAlarmTypeNode {
	private final DataChangeListener listener = new DataChangeListener() {

		@Override
		public void onDataChange(UaNode uaNode, DataValue prevValue, DataValue value) {
			Variant varValue = value == null ? Variant.NULL : value.getValue();
			DateTime activeTime = value == null ? null : value.getSourceTimestamp();
			if (varValue.isEmpty())
				inactivateAlarm(activeTime);
			else
				checkAlarm(varValue.floatValue(), activeTime);
		}
	};
	/**
	 *
	 */
	private final MyNodeManager myNodeManager;

	public MyLevelAlarmType(MyNodeManager myNodeManager, NodeManagerUaNode nodeManager, NodeId nodeId,
			QualifiedName browseName, LocalizedText displayName) {
		super(nodeManager, nodeId, browseName, displayName);
		this.myNodeManager = myNodeManager;
	}

	@Override
	public void setInput(UaVariable node) {
		if (getInput() instanceof UaVariableNode)
			((UaVariableNode) getInput()).removeDataChangeListener(listener);
		super.setInput(node);
		if (node instanceof UaVariableNode)
			((UaVariableNode) node).addDataChangeListener(listener);
	}

	private void triggerAlarm(DateTime activeTime) {
		// Trigger event
		byte[] myEventId = this.myNodeManager.getNextUserEventId();
		triggerEvent(DateTime.currentTime(), activeTime, myEventId);
	}

	/**
	 * Creates an alarm, if it is not active
	 *
	 * @param activeTime
	 */
	protected void activateAlarm(int severity, DateTime activeTime) {
		// Note: UaServer does not yet send any event notifications!
		if (isEnabled() && (!isActive() || (getSeverity().getValue() != severity))) {
			MyNodeManager.println("activateAlarm: severity=" + severity);
			setActive(true);
			setRetain(true);
			setAcked(false); // Also sets confirmed to false
			setSeverity(severity);

			triggerAlarm(activeTime);

		}
	}

	protected void checkAlarm(float nextValue, DateTime activeTime) {
		if (nextValue > getHighHighLimit())
			activateAlarm(700, activeTime);
		else if (nextValue > getHighLimit())
			activateAlarm(500, activeTime);
		else if (nextValue < getLowLowLimit())
			activateAlarm(700, activeTime);
		else if (nextValue < getLowLimit())
			activateAlarm(500, activeTime);
		else
			inactivateAlarm(activeTime);
	}

	protected void inactivateAlarm(DateTime activeTime) {
		if (isEnabled() && isActive()) {
			MyNodeManager.println("inactivateAlarm");
			setActive(false);
			setRetain(!isAcked() && !isConfirmed());
			triggerAlarm(activeTime);
		}
	}

}