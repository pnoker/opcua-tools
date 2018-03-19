/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.utils.NumericRange;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaInstance;
import com.prosysopc.ua.nodes.UaValueNode;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.io.UaTypeIoListenerImpl;
import com.prosysopc.ua.types.opcua.AnalogItemType;

/**
 * Example {@link com.prosysopc.ua.server.io.UaTypeIoListener} template that
 * could be created with code generator in future.
 */
public abstract class AnalogItemListenerTemplate extends UaTypeIoListenerImpl {
	@Override
	public boolean onWriteValue(ServiceContext serviceContext, UaInstance instance, UaValueNode variable,
			NumericRange indexRange, DataValue dataValue) throws StatusException {
		AnalogItemType parent = (AnalogItemType) instance;
		if (instance == variable)
			return onWriteValue(serviceContext, parent, indexRange, dataValue);
		String s = variable.getBrowseName().getName();
		if (s.equals("EURange"))
			return onWriteEuRange(serviceContext, parent, indexRange, dataValue);
		else if (s.equals("EngineeringUnits"))
			return onWriteEngineeringUnits(serviceContext, parent, indexRange, dataValue);
		else if (s.equals("InstrumentRange"))
			return onWriteInstrumentRange(serviceContext, parent, indexRange, dataValue);
		return false;
	}

	protected boolean onWriteEngineeringUnits(ServiceContext serviceContext, AnalogItemType node,
			NumericRange indexRange, DataValue dataValue) {
		return false;
	}

	protected boolean onWriteEuRange(ServiceContext serviceContext, AnalogItemType node, NumericRange indexRange,
			DataValue dataValue) {
		return false;
	}

	protected boolean onWriteInstrumentRange(ServiceContext serviceContext, AnalogItemType node,
			NumericRange indexRange, DataValue dataValue) {
		return false;
	}

	protected boolean onWriteValue(ServiceContext serviceContext, AnalogItemType node, NumericRange indexRange,
			DataValue dataValue) throws StatusException {
		return false;
	}
}
