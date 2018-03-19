/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import java.util.EnumSet;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.utils.NumericRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.WriteAccess;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaValueNode;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.io.IoManagerListener;

/**
 * A sample implementation of a {@link IoManagerListener}
 */
public class MyIoManagerListener implements IoManagerListener {
	private static Logger logger = LoggerFactory.getLogger(MyIoManagerListener.class);

	@Override
	public EnumSet<AccessLevel> onGetUserAccessLevel(ServiceContext serviceContext, NodeId nodeId, UaVariable node) {
		// The AccessLevel defines the accessibility of the Variable.Value
		// attribute

		// Define anonymous access
		// if (serviceContext.getSession().getUserIdentity().getType()
		// .equals(UserTokenType.Anonymous))
		// return EnumSet.noneOf(AccessLevel.class);
		if (node.getHistorizing())
			return EnumSet.of(AccessLevel.CurrentRead, AccessLevel.CurrentWrite, AccessLevel.HistoryRead);
		else
			return EnumSet.of(AccessLevel.CurrentRead, AccessLevel.CurrentWrite);
	}

	@Override
	public Boolean onGetUserExecutable(ServiceContext serviceContext, NodeId nodeId, UaMethod node) {
		// Enable execution of all methods that are allowed by default
		return true;
	}

	@Override
	public EnumSet<WriteAccess> onGetUserWriteMask(ServiceContext serviceContext, NodeId nodeId, UaNode node) {
		// Enable writing to everything that is allowed by default
		// The WriteMask defines the writable attributes, except for Value,
		// which is controlled by UserAccessLevel (above)

		// The following would deny write access for anonymous users:
		// if
		// (serviceContext.getSession().getUserIdentity().getType().equals(
		// UserTokenType.Anonymous))
		// return EnumSet.noneOf(WriteAccess.class);

		return EnumSet.allOf(WriteAccess.class);
	}

	@Override
	public boolean onReadNonValue(ServiceContext serviceContext, NodeId nodeId, UaNode node,
			UnsignedInteger attributeId, DataValue dataValue) throws StatusException {
		return false;
	}

	@Override
	public boolean onReadValue(ServiceContext serviceContext, NodeId nodeId, UaValueNode node, NumericRange indexRange,
			TimestampsToReturn timestampsToReturn, DateTime minTimestamp, DataValue dataValue) throws StatusException {
		if (logger.isDebugEnabled())
			logger.debug("onReadValue: nodeId=" + nodeId + (node != null ? " node=" + node.getBrowseName() : ""));
		return false;
	}

	@Override
	public boolean onWriteNonValue(ServiceContext serviceContext, NodeId nodeId, UaNode node,
			UnsignedInteger attributeId, DataValue dataValue) throws StatusException {
		return false;
	}

	@Override
	public boolean onWriteValue(ServiceContext serviceContext, NodeId nodeId, UaValueNode node, NumericRange indexRange,
			DataValue dataValue) throws StatusException {
		logger.info("onWriteValue: nodeId=" + nodeId + (node != null ? " node=" + node.getBrowseName() : "")
				+ (indexRange != null ? " indexRange=" + indexRange : "") + " value=" + dataValue);
		return false;
	}
}
