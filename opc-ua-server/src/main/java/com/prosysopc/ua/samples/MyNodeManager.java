/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import java.util.Locale;
import java.util.Random;
import java.util.UUID;

import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.Argument;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.NodeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.ValueRanks;
import com.prosysopc.ua.nodes.UaDataType;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaNodeFactoryException;
import com.prosysopc.ua.nodes.UaObject;
import com.prosysopc.ua.nodes.UaObjectType;
import com.prosysopc.ua.nodes.UaType;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.CallableListener;
import com.prosysopc.ua.server.MethodManagerUaNode;
import com.prosysopc.ua.server.ModellingRule;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.server.UaInstantiationException;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.nodes.CacheVariable;
import com.prosysopc.ua.server.nodes.PlainMethod;
import com.prosysopc.ua.server.nodes.PlainProperty;
import com.prosysopc.ua.server.nodes.PlainVariable;
import com.prosysopc.ua.server.nodes.UaDataTypeNode;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.server.nodes.UaObjectTypeNode;
import com.prosysopc.ua.server.nodes.UaVariableNode;
import com.prosysopc.ua.types.opcua.server.BaseEventTypeNode;
import com.prosysopc.ua.types.opcua.server.ExclusiveLevelAlarmTypeNode;
import com.prosysopc.ua.types.opcua.server.ExclusiveLimitState;
import com.prosysopc.ua.types.opcua.server.FolderTypeNode;

/**
 * A sample customized node manager, which actually just overrides the standard
 * NodeManagerUaNode and initializes the nodes for the demo.
 */
public class MyNodeManager extends NodeManagerUaNode {
	public static final String NAMESPACE = "http://www.prosysopc.com/OPCUA/SampleAddressSpace";
	private static final Logger logger = LoggerFactory.getLogger(MyNodeManager.class);
	private static boolean stackTraceOnException;

	/**
	 * @param e
	 */
	private static void printException(Exception e) {
		if (stackTraceOnException)
			e.printStackTrace();
		else {
			println(e.toString());
			if (e.getCause() != null)
				println("Caused by: " + e.getCause());
		}
	}

	/**
	 * @param string
	 */
	protected static void println(String string) {
		System.out.println(string);
	}

	private ExclusiveLevelAlarmTypeNode myAlarm;

	private UaObjectNode myDevice;

	// private MyEventType myEvent;

	private UaVariableNode myLevel;

	private PlainMethod myMethod;

	private CallableListener myMethodManagerListener;

	private FolderTypeNode myObjectsFolder;

	private PlainVariable<Boolean> mySwitch;

	double dx = 1;

	final MyEventManagerListener myEventManagerListener = new MyEventManagerListener();

	/**
	 * Creates a new instance of MyNodeManager
	 *
	 * @param server
	 *            the server in which the node manager is created.
	 * @param namespaceUri
	 *            the namespace URI for the nodes
	 * @throws StatusException
	 *             if something goes wrong in the initialization
	 * @throws UaInstantiationException
	 */
	public MyNodeManager(UaServer server, String namespaceUri) throws StatusException, UaInstantiationException {
		super(server, namespaceUri);
	}

	/**
	 * @return
	 */
	public UaObjectNode[] getHistorizableEvents() {
		return new UaObjectNode[] { myObjectsFolder, myDevice };
	}

	/**
	 * @return
	 */
	public UaVariableNode[] getHistorizableVariables() {
		return new UaVariableNode[] { myLevel, mySwitch };
	}

	/**
	 *
	 */
	public void sendEvent() {
		// If the type has TypeDefinitionId, you can use the class
		MyEventType ev = createEvent(MyEventType.class);
		ev.setMessage("MyEvent");
		ev.setMyVariable(new Random().nextInt());
		ev.setMyProperty("Property Value " + ev.getMyVariable());
		ev.triggerEvent(null);
	}

	/**
	 *
	 */
	public void simulate() {
		final DataValue v = myLevel.getValue();
		Double nextValue = v.isNull() ? 0 : v.getValue().doubleValue() + dx;
		if (nextValue <= 0)
			dx = 1;
		else if (nextValue >= 100)
			dx = -1;
		try {
			((CacheVariable) myLevel).updateValue(nextValue);
			if (nextValue > myAlarm.getHighHighLimit())
				activateAlarm(700, ExclusiveLimitState.HighHigh);
			else if (nextValue > myAlarm.getHighLimit())
				activateAlarm(500, ExclusiveLimitState.High);
			else if (nextValue < myAlarm.getLowLowLimit())
				activateAlarm(700, ExclusiveLimitState.Low);
			else if (nextValue < myAlarm.getLowLimit())
				activateAlarm(500, ExclusiveLimitState.LowLow);
			else
				inactivateAlarm();
		} catch (Exception e) {
			logger.error("Error while simulating", e);
			// printException(e);
			throw new RuntimeException(e); // End the task
		}

	}

	/**
	 * Creates an alarm, if it is not active
	 *
	 * @param limitState
	 */
	private void activateAlarm(int severity, ExclusiveLimitState limitState) {
		if (myAlarm.isEnabled() && (!myAlarm.isActive() || (myAlarm.getSeverity().getValue() != severity))) {
			println("activateAlarm: severity=" + severity);
			myAlarm.setActive(true);
			myAlarm.setRetain(true);
			myAlarm.setAcked(false); // Also sets confirmed to false
			myAlarm.setSeverity(severity);
			myAlarm.getLimitStateNode().setCurrentLimitState(limitState);

			triggerEvent(myAlarm);

			// If you wish to check whether any clients are monitoring your
			// alarm, you can use the following

			// logger.info("myAlarm is monitored=" +
			// myAlarm.isMonitoredForEvents());
		}
	}

	private void createAddressSpace() throws StatusException, UaInstantiationException {
		// +++ My nodes +++

		int ns = getNamespaceIndex();

		// My Event Manager Listener
		this.getEventManager().setListener(myEventManagerListener);

		// UA types and folders which we will use
		final UaObject objectsFolder = getServer().getNodeManagerRoot().getObjectsFolder();
		final UaType baseObjectType = getServer().getNodeManagerRoot().getType(Identifiers.BaseObjectType);
		final UaType baseDataVariableType = getServer().getNodeManagerRoot().getType(Identifiers.BaseDataVariableType);

		// Folder for my objects
		final NodeId myObjectsFolderId = new NodeId(ns, "MyObjectsFolder");
		myObjectsFolder = createInstance(FolderTypeNode.class, "MyObjects", myObjectsFolderId);

		this.addNodeAndReference(objectsFolder, myObjectsFolder, Identifiers.Organizes);

		// My Device Type

		final NodeId myDeviceTypeId = new NodeId(ns, "MyDeviceType");
		UaObjectType myDeviceType = new UaObjectTypeNode(this, myDeviceTypeId, "MyDeviceType", Locale.ENGLISH);
		this.addNodeAndReference(baseObjectType, myDeviceType, Identifiers.HasSubtype);

		// My Device

		final NodeId myDeviceId = new NodeId(ns, "MyDevice");
		myDevice = new UaObjectNode(this, myDeviceId, "MyDevice", Locale.ENGLISH);
		myDevice.setTypeDefinition(myDeviceType);
		myObjectsFolder.addReference(myDevice, Identifiers.HasComponent, false);

		// My Level Type

		final NodeId myLevelTypeId = new NodeId(ns, "MyLevelType");
		UaType myLevelType = this.addType(myLevelTypeId, "MyLevelType", baseDataVariableType);

		// My Level Measurement

		final NodeId myLevelId = new NodeId(ns, "MyLevel");
		UaType doubleType = getServer().getNodeManagerRoot().getType(Identifiers.Double);
		myLevel = new CacheVariable(this, myLevelId, "MyLevel", LocalizedText.NO_LOCALE);
		myLevel.setDataType(doubleType);
		myLevel.setTypeDefinition(myLevelType);
		myDevice.addComponent(myLevel);

		// My Switch
		// Use PlainVariable and addComponent() to add it to myDevice
		// Note that we use NodeIds instead of UaNodes to define the data type
		// and type definition

		NodeId mySwitchId = new NodeId(ns, "MySwitch");
		mySwitch = new PlainVariable<Boolean>(this, mySwitchId, "MySwitch", LocalizedText.NO_LOCALE);
		mySwitch.setDataTypeId(Identifiers.Boolean);
		mySwitch.setTypeDefinitionId(Identifiers.BaseDataVariableType);
		myDevice.addComponent(mySwitch); // addReference(...Identifiers.HasComponent...);

		// Initial value
		mySwitch.setCurrentValue(false);

		// A sample alarm node
		createAlarmNode(myLevel);

		// A sample custom event type
		createMyEventType();

		// A sample enumeration type
		createMyEnumNode();

		// A sample method node
		createMethodNode();
	}

	/**
	 * Create a sample alarm node structure.
	 *
	 * @param source
	 *
	 * @throws StatusException
	 * @throws UaInstantiationException
	 */
	private void createAlarmNode(UaVariable source) throws StatusException, UaInstantiationException {

		// Level Alarm from the LevelMeasurement

		// See the Spec. Part 9. Appendix B.2 for a similar example

		int ns = this.getNamespaceIndex();
		final NodeId myAlarmId = new NodeId(ns, source.getNodeId().getValue() + ".Alarm");
		String name = source.getBrowseName().getName() + "Alarm";
		myAlarm = createInstance(ExclusiveLevelAlarmTypeNode.class, name, myAlarmId);

		// ConditionSource is the node which has this condition
		myAlarm.setSource(source);
		// Input is the node which has the measurement that generates the alarm
		myAlarm.setInput(source);

		myAlarm.setMessage("Level exceeded"); // Default locale
		myAlarm.setMessage("Füllständalarm!", Locale.GERMAN);
		myAlarm.setSeverity(500); // Medium level warning
		myAlarm.setHighHighLimit(90.0);
		myAlarm.setHighLimit(70.0);
		myAlarm.setLowLowLimit(10.0);
		myAlarm.setLowLimit(30.0);
		myAlarm.setEnabled(true);
		myDevice.addComponent(myAlarm); // addReference(...Identifiers.HasComponent...)

		// + HasCondition, the SourceNode of the reference should normally
		// correspond to the Source set above
		source.addReference(myAlarm, Identifiers.HasCondition, false);

		// + EventSource, the target of the EventSource is normally the
		// source of the HasCondition reference
		myDevice.addReference(source, Identifiers.HasEventSource, false);

		// + HasNotifier, these are used to link the source of the EventSource
		// up in the address space hierarchy
		myObjectsFolder.addReference(myDevice, Identifiers.HasNotifier, false);
	}

	/**
	 * Create a sample method.
	 *
	 * @throws StatusException
	 */
	private void createMethodNode() throws StatusException {
		int ns = this.getNamespaceIndex();
		final NodeId myMethodId = new NodeId(ns, "MyMethod");
		myMethod = new PlainMethod(this, myMethodId, "MyMethod", Locale.ENGLISH);
		Argument[] inputs = new Argument[2];
		inputs[0] = new Argument();
		inputs[0].setName("Operation");
		inputs[0].setDataType(Identifiers.String);
		inputs[0].setValueRank(ValueRanks.Scalar);
		inputs[0].setArrayDimensions(null);
		inputs[0].setDescription(new LocalizedText(
				"The operation to perform on parameter: valid functions are sin, cos, tan, pow", Locale.ENGLISH));
		inputs[1] = new Argument();
		inputs[1].setName("Parameter");
		inputs[1].setDataType(Identifiers.Double);
		inputs[1].setValueRank(ValueRanks.Scalar);
		inputs[1].setArrayDimensions(null);
		inputs[1].setDescription(new LocalizedText("The parameter for operation", Locale.ENGLISH));
		myMethod.setInputArguments(inputs);

		Argument[] outputs = new Argument[1];
		outputs[0] = new Argument();
		outputs[0].setName("Result");
		outputs[0].setDataType(Identifiers.Double);
		outputs[0].setValueRank(ValueRanks.Scalar);
		outputs[0].setArrayDimensions(null);
		outputs[0].setDescription(new LocalizedText("The result of 'operation(parameter)'", Locale.ENGLISH));
		myMethod.setOutputArguments(outputs);

		this.addNodeAndReference(myDevice, myMethod, Identifiers.HasComponent);

		// Create the listener that handles the method calls
		myMethodManagerListener = new MyMethodManagerListener(myMethod);
		MethodManagerUaNode m = (MethodManagerUaNode) this.getMethodManager();
		m.addCallListener(myMethodManagerListener);
	}

	/**
	 * @throws StatusException
	 *             if the necessary type node(s) are not found
	 *
	 */
	private void createMyEnumNode() throws StatusException {
		// An example showing how a new enumeration type can be defined in code.
		// It is usually easier to define new types using information models and
		// generating Java code out of those. See the more about that in the
		// 'codegen' documentation.

		// 1. Create the type node...

		NodeId myEnumTypeId = new NodeId(this.getNamespaceIndex(), "MyEnumType");

		UaDataType myEnumType = new UaDataTypeNode(this, myEnumTypeId, "MyEnumType", LocalizedText.NO_LOCALE);

		// ... as sub type of Enumeration
		UaType enumerationType = getServer().getNodeManagerRoot().getType(Identifiers.Enumeration);
		enumerationType.addSubType(myEnumType);

		// 2. Add the EnumStrings property ...

		NodeId myEnumStringsId = new NodeId(this.getNamespaceIndex(), "MyEnumType_EnumStrings");
		;
		PlainProperty<LocalizedText[]> enumStringsProperty = new PlainProperty<LocalizedText[]>(this, myEnumStringsId,
				new QualifiedName("EnumStrings"), new LocalizedText("EnumStrings", LocalizedText.NO_LOCALE));
		enumStringsProperty.setDataTypeId(Identifiers.LocalizedText);
		enumStringsProperty.setValueRank(ValueRanks.OneDimension);
		enumStringsProperty.setArrayDimensions(new UnsignedInteger[] { UnsignedInteger.ZERO });
		enumStringsProperty.setAccessLevel(AccessLevel.READONLY);
		enumStringsProperty.addReference(Identifiers.ModellingRule_Mandatory, Identifiers.HasModellingRule, false);

		myEnumType.addProperty(enumStringsProperty);

		// ... with Value
		enumStringsProperty.setCurrentValue(MyEnumType.getEnumStrings());

		// 3. Create the instance

		NodeId myEnumObjectId = new NodeId(this.getNamespaceIndex(), "MyEnumObject");
		PlainVariable<MyEnumType> myEnumVariable = new PlainVariable<MyEnumType>(this, myEnumObjectId, "MyEnumObject",
				LocalizedText.NO_LOCALE);
		myEnumVariable.setDataType(myEnumType);

		// .. as a component of myDevice
		myDevice.addComponent(myEnumVariable);

		// 4. Initialize the value
		myEnumVariable.setCurrentValue(MyEnumType.One);
	}

	/**
	 * A sample custom event type.
	 * <p>
	 * NOTE that it is usually easier to create new types using the Information
	 * Models and export them from XML to the server. You can also generate the
	 * respective Java types with the 'codegen' from the same XML. In this
	 * example, we will construct the type into the address space "manually".
	 * MyEventType is also hand-coded and is registered to be used to create the
	 * instances of that type.
	 * <p>
	 * When the type definition is in the address space, and the respective Java
	 * class is registered to the server, it will create those instances, for
	 * example as shown in {@link #sendEvent()}.
	 *
	 * @throws StatusException
	 */
	private void createMyEventType() throws StatusException {
		int ns = this.getNamespaceIndex();

		NodeId myEventTypeId = new NodeId(ns, MyEventType.MY_EVENT_ID);
		UaObjectType myEventType = new UaObjectTypeNode(this, myEventTypeId, "MyEventType", LocalizedText.NO_LOCALE);
		getServer().getNodeManagerRoot().getType(Identifiers.BaseEventType).addSubType(myEventType);

		NodeId myVariableId = new NodeId(ns, MyEventType.MY_VARIABLE_ID);
		PlainVariable<Integer> myVariable = new PlainVariable<Integer>(this, myVariableId, MyEventType.MY_VARIABLE_NAME,
				LocalizedText.NO_LOCALE);
		myVariable.setDataTypeId(Identifiers.Int32);
		// The modeling rule must be defined for the mandatory elements to
		// ensure that the event instances will also get the elements.
		myVariable.addModellingRule(ModellingRule.Mandatory);
		myEventType.addComponent(myVariable);

		NodeId myPropertyId = new NodeId(ns, MyEventType.MY_PROPERTY_ID);
		PlainProperty<Integer> myProperty = new PlainProperty<Integer>(this, myPropertyId, MyEventType.MY_PROPERTY_NAME,
				LocalizedText.NO_LOCALE);
		myProperty.setDataTypeId(Identifiers.String);
		myProperty.addModellingRule(ModellingRule.Mandatory);
		myEventType.addProperty(myProperty);

		getServer().registerClass(MyEventType.class, myEventTypeId);
	}

	/**
	 *
	 */
	private void inactivateAlarm() {
		if (myAlarm.isEnabled() && myAlarm.isActive()) {
			println("inactivateAlarm");
			myAlarm.setActive(false);
			myAlarm.setRetain(!myAlarm.isAcked());
			myAlarm.getLimitStateNode().setCurrentLimitState(ExclusiveLimitState.None);

			triggerEvent(myAlarm);
		}
	}

	/**
	 * Send an event notification.
	 *
	 * @param event
	 *            The event to trigger.
	 */
	private void triggerEvent(BaseEventTypeNode event) {
		// Trigger event
		final DateTime now = DateTime.currentTime();
		byte[] myEventId = getNextUserEventId();
		/* byte[] fullEventId = */event.triggerEvent(now, now, myEventId);
	}

	/**
	 * @return
	 */
	protected byte[] getNextUserEventId() {
		return myEventManagerListener.getNextUserEventId();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.prosysopc.ua.server.NodeManagerUaNode#init()
	 */
	@Override
	protected void init() throws StatusException, UaNodeFactoryException {
		super.init();

		createAddressSpace();
	}

	/**
	 *
	 */
	// protected void initMyEvent() {
	// if (myEvent == null)
	// myEvent = new MyEventType(this);
	// }

	/**
	 * Send an event
	 *
	 * @throws StatusException
	 */
	// protected void sendEvent() throws StatusException {
	// // 1. send a standard SystemEventType here
	// SystemEventTypeNode newEvent = createEvent(SystemEventTypeNode.class);
	//
	// newEvent.setMessage("New event");
	// // Set the severity of the event between 1 and 1000
	// newEvent.setSeverity(1);
	// // By default the event is sent for the "Server" object. If you want to
	// // send it for some other object, use Source (or SourceNode), e.g.
	// // newEvent.setSource(myDevice);
	// triggerEvent(newEvent);
	//
	// // 2. Send our own event
	//
	// initMyEvent();
	// myEvent.setSource(myObjectsFolder);
	//
	// myEvent.setMyVariable(myEvent.getMyVariable() + 1);
	// myEvent.setMyProperty(DateTime.currentTime().toString());
	// triggerEvent(myEvent);
	// this.deleteNode(myEvent, true, true);
	// }

	void addNode(String name) {
		// Initialize NodeVersion property, to enable ModelChangeEvents
		myObjectsFolder.initNodeVersion();

		getServer().getNodeManagerRoot().beginModelChange();
		try {
			NodeId nodeId = new NodeId(this.getNamespaceIndex(), UUID.randomUUID());

			UaNode node = this.getNodeFactory().createNode(NodeClass.Variable, nodeId, name, Locale.ENGLISH,
					Identifiers.PropertyType);
			myObjectsFolder.addComponent(node);
		} catch (UaNodeFactoryException e) {
			printException(e);
		} catch (IllegalArgumentException e) {
			printException(e);
		} finally {
			getServer().getNodeManagerRoot().endModelChange();
		}
	}

	void deleteNode(QualifiedName nodeName) throws StatusException {
		UaNode node = myObjectsFolder.getComponent(nodeName);
		if (node != null) {
			getServer().getNodeManagerRoot().beginModelChange();
			try {
				this.deleteNode(node, true, true);
			} finally {
				getServer().getNodeManagerRoot().endModelChange();
			}
		} else
			println("MyObjects does not contain a component with name " + nodeName);
	}
}
