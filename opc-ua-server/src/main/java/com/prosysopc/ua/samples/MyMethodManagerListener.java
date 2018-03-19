/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import java.util.Arrays;

import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.core.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.server.CallableListener;
import com.prosysopc.ua.server.MethodManager;
import com.prosysopc.ua.server.ServiceContext;

/**
 * A sample implementation of an MethodManagerListener
 */
public class MyMethodManagerListener implements CallableListener {

	private static Logger logger = LoggerFactory.getLogger(MyMethodManagerListener.class);
	final private UaNode myMethod;

	/**
	 * @param myMethod
	 *            the method node to handle.
	 */
	public MyMethodManagerListener(UaNode myMethod) {
		super();
		this.myMethod = myMethod;
	}

	@Override
	public boolean onCall(ServiceContext serviceContext, NodeId objectId, UaNode object, NodeId methodId,
			UaMethod method, final Variant[] inputArguments, final StatusCode[] inputArgumentResults,
			final DiagnosticInfo[] inputArgumentDiagnosticInfos, final Variant[] outputs) throws StatusException {
		// Handle method calls
		// Note that the outputs array is already allocated
		if (methodId.equals(myMethod.getNodeId())) {
			logger.info("myMethod: {}", Arrays.toString(inputArguments));
			MethodManager.checkInputArguments(new Class[] { String.class, Double.class }, inputArguments,
					inputArgumentResults, inputArgumentDiagnosticInfos, false);
			// The argument #0 is the operation to perform
			String operation;
			try {
				operation = (String) inputArguments[0].getValue();
			} catch (ClassCastException e) {
				throw inputError(0, e.getMessage(), inputArgumentResults, inputArgumentDiagnosticInfos);
			}
			// The argument #1 is the input (i.e. operand)
			double input;
			try {
				input = inputArguments[1].intValue();
			} catch (ClassCastException e) {
				throw inputError(1, e.getMessage(), inputArgumentResults, inputArgumentDiagnosticInfos);
			}

			// The result is the operation applied to input
			operation = operation.toLowerCase();
			double result;
			if (operation.equals("sin"))
				result = Math.sin(Math.toRadians(input));
			else if (operation.equals("cos"))
				result = Math.cos(Math.toRadians(input));
			else if (operation.equals("tan"))
				result = Math.tan(Math.toRadians(input));
			else if (operation.equals("pow"))
				result = input * input;
			else
				throw inputError(0, "Unknown function '" + operation + "': valid functions are sin, cos, tan, pow",
						inputArgumentResults, inputArgumentDiagnosticInfos);
			outputs[0] = new Variant(result);
			return true; // Handled here
		} else
			return false;
	}

	/**
	 * Handle an error in method inputs.
	 *
	 * @param index
	 *            index of the failing input
	 * @param message
	 *            error message
	 * @param inputArgumentResults
	 *            the results array to fill in
	 * @param inputArgumentDiagnosticInfos
	 *            the diagnostics array to fill in
	 * @return StatusException that can be thrown to break further method
	 *         handling
	 */
	private StatusException inputError(final int index, final String message, StatusCode[] inputArgumentResults,
			DiagnosticInfo[] inputArgumentDiagnosticInfos) {
		logger.info("inputError: #{} message={}", index, message);
		inputArgumentResults[index] = new StatusCode(StatusCodes.Bad_InvalidArgument);
		final DiagnosticInfo di = new DiagnosticInfo();
		di.setAdditionalInfo(message);
		inputArgumentDiagnosticInfos[index] = di;
		return new StatusException(StatusCodes.Bad_InvalidArgument);
	}

}
