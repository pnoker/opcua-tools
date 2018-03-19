/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.core.Range;
import org.opcfoundation.ua.core.StatusCodes;
import org.opcfoundation.ua.utils.NumericRange;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.types.opcua.AnalogItemType;

/**
 * Example how to implement custom behavior by extending a
 * {@link com.prosysopc.ua.server.io.UaTypeIoListener} template.
 */
public class MyAnalogItemListener extends AnalogItemListenerTemplate {
	@Override
	protected boolean onWriteValue(ServiceContext serviceContext, AnalogItemType node, NumericRange indexRange,
			DataValue dataValue) throws StatusException {
		double value = (Double) dataValue.getValue().getValue();
		Range euRangeValue = node.getEuRange();
		if ((euRangeValue != null) && ((value < euRangeValue.getLow()) || (value > euRangeValue.getHigh())))
			throw new StatusException(StatusCodes.Bad_OutOfRange);
		return false;
	}
}
