/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.nodes.UaProperty;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.types.opcua.server.BaseEventTypeNode;

/**
 * A sample implementation of a custom event type.
 * <p>
 * You can use the TypeDefinitionId annotation or getDefaultTypeDefinition() to
 * define the type. If you use the annotation, you can also register the type
 * and create event instances with the class only.
 *
 * @see {@link MyNodeManager#createMyEventType}
 */
@TypeDefinitionId(nsu = MyNodeManager.NAMESPACE, i = MyEventType.MY_EVENT_ID)
public class MyEventType extends BaseEventTypeNode {

	public static final int MY_EVENT_ID = 10000;
	public static final UnsignedInteger MY_PROPERTY_ID = UnsignedInteger.valueOf(10001);
	public static final String MY_PROPERTY_NAME = "MyProperty";
	public static final UnsignedInteger MY_VARIABLE_ID = UnsignedInteger.valueOf(10002);
	public static final String MY_VARIABLE_NAME = "MyVariable";

	/**
	 * The constructor is used by the NodeBuilder and should not be used
	 * directly by the application. Therefore we define it with protected
	 * visibility.
	 */
	protected MyEventType(NodeManagerUaNode nodeManager, NodeId nodeId, QualifiedName browseName,
			LocalizedText displayName) {
		super(nodeManager, nodeId, browseName, displayName);
	}

	/**
	 * @return the value of MyProperty
	 */
	public String getMyProperty() {
		UaProperty property = getMyPropertyNode();
		if (property == null)
			return null;
		return (String) property.getValue().getValue().getValue();
	}

	/**
	 * @return the myProperty node object
	 */
	public UaProperty getMyPropertyNode() {
		UaProperty property = getProperty(new QualifiedName(getNodeManager().getNamespaceIndex(), MY_PROPERTY_NAME));
		return property;
	}

	/**
	 * @return the value of MyVariable
	 */
	public Integer getMyVariable() {
		UaVariable variable = getMyVariableNode();
		if (variable == null)
			return null;
		return (Integer) variable.getValue().getValue().getValue();
	}

	/**
	 * @return the MyVariable node object
	 */
	public UaVariable getMyVariableNode() {
		UaVariable property = (UaVariable) getComponent(
				new QualifiedName(getNodeManager().getNamespaceIndex(), MY_VARIABLE_NAME));
		return property;
	}

	/**
	 * @param value
	 *            the value to set to MyProperty
	 */
	public void setMyProperty(String myValue) {
		UaProperty property = getMyPropertyNode();
		if (property != null)
			try {
				property.setValue(myValue);
			} catch (StatusException e) {
				throw new RuntimeException(e);
			}
		else
			System.out.println("No property");
	}

	/**
	 * @param value
	 *            the value to set to MyVariable
	 */
	public void setMyVariable(int myValue) {
		UaVariable variable = getMyVariableNode();
		if (variable != null)
			try {
				variable.setValue(myValue);
			} catch (StatusException e) {
				throw new RuntimeException(e);
			}
		else
			System.out.println("No variable");
	}

}
