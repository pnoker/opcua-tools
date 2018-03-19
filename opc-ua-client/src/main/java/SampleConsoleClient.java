/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.ExpandedNodeId;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.NodeId;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedByte;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.UnsignedShort;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.common.ServiceResultException;
import org.opcfoundation.ua.core.AccessLevel;
import org.opcfoundation.ua.core.AggregateConfiguration;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.Argument;
import org.opcfoundation.ua.core.Attributes;
import org.opcfoundation.ua.core.BrowseDirection;
import org.opcfoundation.ua.core.BrowsePathTarget;
import org.opcfoundation.ua.core.DataChangeFilter;
import org.opcfoundation.ua.core.DataChangeTrigger;
import org.opcfoundation.ua.core.DeadbandType;
import org.opcfoundation.ua.core.EUInformation;
import org.opcfoundation.ua.core.ElementOperand;
import org.opcfoundation.ua.core.EndpointDescription;
import org.opcfoundation.ua.core.EventFilter;
import org.opcfoundation.ua.core.FilterOperator;
import org.opcfoundation.ua.core.HistoryEventFieldList;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.LiteralOperand;
import org.opcfoundation.ua.core.MonitoringMode;
import org.opcfoundation.ua.core.NodeClass;
import org.opcfoundation.ua.core.Range;
import org.opcfoundation.ua.core.ReferenceDescription;
import org.opcfoundation.ua.core.RelativePathElement;
import org.opcfoundation.ua.core.SimpleAttributeOperand;
import org.opcfoundation.ua.core.TimestampsToReturn;
import org.opcfoundation.ua.core.UserTokenPolicy;
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.SecurityMode;
import org.opcfoundation.ua.utils.AttributesUtil;
import org.opcfoundation.ua.utils.CertificateUtils;
import org.opcfoundation.ua.utils.MultiDimensionArrayUtils;
import org.opcfoundation.ua.utils.NumericRange;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.CertificateValidationListener;
import com.prosysopc.ua.ContentFilterBuilder;
import com.prosysopc.ua.EventNotifierClass;
import com.prosysopc.ua.MethodCallStatusException;
import com.prosysopc.ua.MonitoredItemBase;
import com.prosysopc.ua.PkiFileBasedCertificateValidator;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.SessionActivationException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.UaAddress;
import com.prosysopc.ua.UaApplication.Protocol;
import com.prosysopc.ua.UserIdentity;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.client.InvalidServerEndpointException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.MonitoredDataItemListener;
import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.MonitoredEventItemListener;
import com.prosysopc.ua.client.MonitoredItem;
import com.prosysopc.ua.client.ServerConnectionException;
import com.prosysopc.ua.client.ServerList;
import com.prosysopc.ua.client.ServerListException;
import com.prosysopc.ua.client.ServerStatusListener;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import com.prosysopc.ua.client.SubscriptionNotificationListener;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.client.UaClientListener;
import com.prosysopc.ua.nodes.MethodArgumentException;
import com.prosysopc.ua.nodes.UaDataType;
import com.prosysopc.ua.nodes.UaInstance;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaObject;
import com.prosysopc.ua.nodes.UaReferenceType;
import com.prosysopc.ua.nodes.UaType;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.types.opcua.AnalogItemType;

/**
 * A sample OPC UA client, running from the console.
 */
public class SampleConsoleClient {

	// Action codes for readAction, etc.
	protected static final int ACTION_ALL = -4;
	protected static final int ACTION_BACK = -2;
	protected static final int ACTION_RETURN = -1;
	protected static final int ACTION_ROOT = -3;
	protected static final int ACTION_TRANSLATE = -6;
	protected static final int ACTION_UP = -5;

	/**
	 * The name of the application.
	 */
	protected static String APP_NAME = "SampleConsoleClient";

	protected final static List<String> cmdSequence = new ArrayList<String>();

	protected static boolean stackTraceOnException = false;

	public static void main(String[] args) throws Exception {
		// Load Log4j configurations from external file
		PropertyConfigurator.configureAndWatch(SampleConsoleClient.class.getResource("log.properties").getFile(), 5000);
		SampleConsoleClient sampleConsoleClient = new SampleConsoleClient();
		try {
			if (!sampleConsoleClient.parseCmdLineArgs(args)) {
				usage();
				return;
			}
		} catch (IllegalArgumentException e) {
			// If message is not defined, the command line was empty and the
			// user did not enter any URL when prompted. Otherwise, the
			// exception is used to notify of an invalid argument.
			if (e.getMessage() != null)
				println("Invalid cmd line argument: " + e.getMessage());
			usage();
			return;
		}

		sampleConsoleClient.initialize(args);
		// Show the menu, which is the main loop of the client application
		sampleConsoleClient.mainMenu();

		println(APP_NAME + ": Closed");

	}

	/**
	 * @param title
	 * @param timestamp
	 * @param picoSeconds
	 * @return
	 */
	protected static String dateTimeToString(String title, DateTime timestamp, UnsignedShort picoSeconds) {
		if ((timestamp != null) && !timestamp.equals(DateTime.MIN_VALUE)) {
			SimpleDateFormat format = new SimpleDateFormat("yyyy MMM dd (zzz) HH:mm:ss.SSS");
			StringBuilder sb = new StringBuilder(title);
			sb.append(format.format(timestamp.getCalendar(TimeZone.getDefault()).getTime()));
			if ((picoSeconds != null) && !picoSeconds.equals(UnsignedShort.valueOf(0)))
				sb.append(String.format("/%d picos", picoSeconds.getValue()));
			return sb.toString();
		}
		return "";
	}

	/**
	 * @param s
	 * @return
	 */
	protected static int parseAction(String s) {
		if (s.equals("x"))
			return ACTION_RETURN;
		if (s.equals("b"))
			return ACTION_BACK;
		if (s.equals("r"))
			return ACTION_ROOT;
		if (s.equals("a"))
			return ACTION_ALL;
		if (s.equals("u"))
			return ACTION_UP;
		if (s.equals("t"))
			return ACTION_TRANSLATE;
		return Integer.parseInt(s);
	}

	/**
	 * @param string
	 */
	protected static void print(String string) {
		System.out.print(string);

	}

	/**
	 * @param e
	 */
	protected static void printException(Exception e) {
		if (stackTraceOnException)
			e.printStackTrace();
		else {
			println(e.toString());
			if (e instanceof MethodCallStatusException) {
				MethodCallStatusException me = (MethodCallStatusException) e;
				final StatusCode[] results = me.getInputArgumentResults();
				if (results != null)
					for (int i = 0; i < results.length; i++) {
						StatusCode s = results[i];
						if (s.isBad()) {
							println("Status for Input #" + i + ": " + s);
							DiagnosticInfo d = me.getInputArgumentDiagnosticInfos()[i];
							if (d != null)
								println("  DiagnosticInfo:" + i + ": " + d);
						}
					}
			}
			if (e.getCause() != null)
				println("Caused by: " + e.getCause());
		}
	}

	/**
	 * @param format
	 * @param args
	 */
	protected static void printf(String format, Object... args) {
		System.out.printf(format, args);

	}

	/**
	 * @param string
	 */
	protected static void println(String string) {
		System.out.println(string);
	}

	// protected fields
	/**
	 * @return
	 */
	protected static int readAction() {
		return parseAction(readInput(true).toLowerCase());
	}

	protected static String readInput(boolean useCmdSequence) {
		return readInput(useCmdSequence, null);
	}

	protected static String readInput(boolean useCmdSequence, String defaultValue) {
		// You can provide "commands" already from the command line, in which
		// case they will be kept in cmdSequence
		if (useCmdSequence && !cmdSequence.isEmpty()) {
			String cmd = cmdSequence.remove(0);
			try {
				// Negative int values are used to pause for n seconds
				int i = Integer.parseInt(cmd);
				if (i < 0) {
					try {
						TimeUnit.SECONDS.sleep(-i);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
					return readInput(useCmdSequence, defaultValue);
				}
			} catch (NumberFormatException e) {
				// never mind
			}
			return cmd;
		}
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String s = null;
		do
			try {
				s = stdin.readLine();
				if ((s == null) || (s.length() == 0))
					s = defaultValue;
			} catch (IOException e) {
				printException(e);
			}
		while ((s == null) || (s.length() == 0));
		return s;
	}

	/**
	 *
	 */
	protected static void usage() {
		println("Usage: " + APP_NAME + " [-d] [-t] [-n] [-?] [serverUri]");
		println("   -d         Connect to a discovery server");
		println("   -n nodeId  Define the NodeId to select after connect (requires serverUri)");
		println("   -s n|s|e[bits]   Define the security mode (n=none/s=sign/e=signAndEncrypt). Default is none.");
		println("                    Oprionally, define the bit strength (128 or 256) you want to use for encryption. Default is 128");
		println("   -k keySize Define the size of the public key of the application certificate (default 1024; other valid values 2048, 4096)");
		println("   -m nodeId  Subscribe to the given node at start up");
		println("   -t         Output stack trace for errors");
		println("   -dt        Show the DataType of read values When displaying them.");
		println("   -?         Show this help text");
		println("   serverUri  The address of the server to connect to. If you do not specify it, you will be prompted for it.");
		println("");
		println(" Examples of valid arguments:");
		println("   opc.tcp://localhost:4841                            (UA Demo Server)");
		println("   opc.tcp://localhost:52520/OPCUA/SampleConsoleServer (Prosys Sample Server)");
		println("   opc.tcp://localhost:51210/UA/SampleServer           (OPC Foundation Sample Server)");
		println("   -d opc.tcp://localhost:4840/UADiscovery             (OPC Foundation Discovery Server)");
	}

	protected UaClient client;
	protected UaClientListener clientListener = new MyUaClientListener();

	protected boolean connectToDiscoveryServer = false;
	protected MonitoredDataItemListener dataChangeListener = new MyMonitoredDataItemListener(this);
	protected String defaultServerUri = "opc.tcp://localhost:52520/OPCUA/SampleConsoleServer";
	// requested fields for event subscriptions
	// the last two fields reserved for our custom fields
	protected final QualifiedName[] eventFieldNames = { new QualifiedName("EventType"), new QualifiedName("Message"),
			new QualifiedName("SourceName"), new QualifiedName("Time"), new QualifiedName("Severity"),
			new QualifiedName("ActiveState/Id"), null, null };
	protected final MonitoredEventItemListener eventListener = new MyMonitoredEventItemListener(this, eventFieldNames);

	protected final List<String> initialMonitoredItems = new ArrayList<String>();

	protected NodeId nodeId = null;
	protected String passWord;
	protected SecurityMode securityMode = SecurityMode.NONE;

	protected ServerStatusListener serverStatusListener = new MyServerStatusListener();
	protected String serverUri = null;

	protected int sessionCount = 0;
	protected boolean showReadValueDataType = false;

	protected Subscription subscription;

	protected SubscriptionAliveListener subscriptionAliveListener = new MySubscriptionAliveListener();

	protected SubscriptionNotificationListener subscriptionListener = new MySubscriptionNotificationListener();

	protected String userName;

	protected CertificateValidationListener validationListener = new MyCertificateValidationListener();

	public SampleConsoleClient() {

	}

	/**
	 * @param nodeClass
	 * @return
	 */
	private String nodeClassToStr(NodeClass nodeClass) {
		return "[" + nodeClass + "]";
	}

	/**
	 * Browse the references for a node.
	 *
	 * @param nodeId
	 * @param prevId
	 * @throws ServiceException
	 * @throws StatusException
	 */
	protected NodeId browse(NodeId nodeId, NodeId prevId) throws ServiceException, StatusException {
		printCurrentNode(nodeId);
		// client.getAddressSpace().setReferenceTypeId(ReferencesToReturn);
		List<ReferenceDescription> references;
		// Find the reference to use for browsing up: prefer the previous node,
		// but otherwise accept any hierarchical inverse reference
		List<ReferenceDescription> upReferences;
		try {
			client.getAddressSpace().setMaxReferencesPerNode(1000);
			references = client.getAddressSpace().browse(nodeId);
			for (int i = 0; i < references.size(); i++)
				printf("%d - %s\n", i, referenceToString(references.get(i)));
			upReferences = client.getAddressSpace().browseUp(nodeId);
		} catch (Exception e) {
			printException(e);
			references = new ArrayList<ReferenceDescription>();
			upReferences = new ArrayList<ReferenceDescription>();
		}

		System.out.println("-------------------------------------------------------");
		println("- Enter node number to browse into that");
		println("- Enter a to show/hide all references");
		if (prevId != null) {
			String prevName = null;
			try {
				UaNode prevNode = client.getAddressSpace().getNode(prevId);
				if (prevNode != null)
					prevName = prevNode.getDisplayName().getText();
			} catch (AddressSpaceException e) {
				prevName = prevId.toString();
			}
			if (prevName != null)
				println("- Enter b to browse back to the previous node (" + prevName + ")");
		}
		if (!upReferences.isEmpty())
			println("- Enter u to browse up to the 'parent' node");
		println("- Enter r to browse back to the root node");
		println("- Enter t to translate a BrowsePath to NodeId");
		System.out.println("- Enter x to select the current node and return to previous menu");
		System.out.println("-------------------------------------------------------");
		do {
			int action = readAction();
			switch (action) {
			case ACTION_RETURN:
				return nodeId;
			case ACTION_BACK:
				if (prevId == null)
					continue;
				return prevId;
			case ACTION_UP:
				if ((!upReferences.isEmpty()))
					try {
						ReferenceDescription upReference = null;
						if (upReferences.size() == 1)
							upReference = upReferences.get(0);
						else {
							println("Which inverse reference do you wish to go up?");
							for (int i = 0; i < upReferences.size(); i++)
								printf("%d - %s\n", i, referenceToString(upReferences.get(i)));
							while (upReference == null) {
								int upIndex = readAction();
								try {
									upReference = upReferences.get(upIndex);
								} catch (Exception e) {
									printException(e);
								}
							}
						}
						if (!upReference.getNodeId().isLocal())
							println("Not a local node");
						else
							return browse(
									client.getAddressSpace().getNamespaceTable().toNodeId(upReference.getNodeId()),
									nodeId);
					} catch (ServiceResultException e1) {
						printException(e1);
					}
			case ACTION_ROOT:
				return browse(Identifiers.RootFolder, nodeId);
			case ACTION_ALL:
				if (NodeId.isNull(client.getAddressSpace().getReferenceTypeId())) {
					client.getAddressSpace().setReferenceTypeId(Identifiers.HierarchicalReferences);
					client.getAddressSpace().setBrowseDirection(BrowseDirection.Forward);
				} else {
					// request all types
					client.getAddressSpace().setReferenceTypeId(NodeId.NULL);
					client.getAddressSpace().setBrowseDirection(BrowseDirection.Both);
				}
				// if (ReferencesToReturn == null) {
				// ReferencesToReturn = Identifiers.HierarchicalReferences;
				// client.getAddressSpace().setBrowseDirection(
				// BrowseDirection.Forward);
				// } else {
				// ReferencesToReturn = null;
				// client.getAddressSpace().setBrowseDirection(
				// BrowseDirection.Both);
				// }
				return browse(nodeId, prevId);
			case ACTION_TRANSLATE:
				println("Which node do you wish to translate?");
				println("Use / to separate nodes in the browsePath, e.g. 'Types/ObjectTypes/BaseObjectType/3:YourType'");
				println("where each element is a 'parseable' BrowseName, i.e. the namespaceIndex can be defined with a prefix, like '3:'");
				String browsePathString = readInput(false);

				List<RelativePathElement> browsePath = new ArrayList<RelativePathElement>();
				for (String s : browsePathString.split("/")) {
					final QualifiedName targetName = QualifiedName.parseQualifiedName(s);
					browsePath
							.add(new RelativePathElement(Identifiers.HierarchicalReferences, false, true, targetName));
				}
				// The result may always contain several targets (if there are
				// nodes with the same browseName), although normally only one
				// is expected
				BrowsePathTarget[] pathTargets;
				try {
					pathTargets = client.getAddressSpace().translateBrowsePathToNodeId(nodeId,
							browsePath.toArray(new RelativePathElement[0]));
					for (BrowsePathTarget pathTarget : pathTargets) {
						String targetStr = "Target: " + pathTarget.getTargetId();
						if (!pathTarget.getRemainingPathIndex().equals(UnsignedInteger.MAX_VALUE))
							targetStr = targetStr + " - RemainingPathIndex: " + pathTarget.getRemainingPathIndex();
						println(targetStr);
					}
				} catch (StatusException e1) {
					printException(e1);
				}
				break;

			default:
				try {
					ReferenceDescription r = references.get(action);
					NodeId target;
					try {
						target = browse(client.getAddressSpace().getNamespaceTable().toNodeId(r.getNodeId()), nodeId);
					} catch (ServiceResultException e) {
						throw new ServiceException(e);
					}
					if (target != nodeId)
						return target;
					return browse(nodeId, prevId);
				} catch (IndexOutOfBoundsException e) {
					System.out.println("No such item: " + action);
				}
			}
		} while (true);
	}

	/**
	 * Call a method on an object. Note that methods are called on objects, so
	 * you need to define the NodeId for both the object and the method.
	 *
	 * @param nodeId
	 *            The ID of the object
	 * @param methodId
	 *            The ID of the method of the object
	 * @throws ServiceException
	 * @throws AddressSpaceException
	 * @throws ServerConnectionException
	 * @throws MethodArgumentException
	 * @throws StatusException
	 *
	 */
	protected void callMethod(NodeId nodeId, NodeId methodId) throws ServiceException, ServerConnectionException,
			AddressSpaceException, MethodArgumentException, StatusException {
		// // Example values to call "condition acknowledge" using the standard
		// // methodId:
		// methodId = Identifiers.AcknowledgeableConditionType_Acknowledge;
		// // change this to the ID of the event you are acknowledging:
		// byte[] eventId = null;
		// LocalizedText comment = new LocalizedText("Your comment",
		// Locale.ENGLISH);
		// final Variant[] inputs = new Variant[] { new Variant(eventId),
		// new Variant(comment) };
		UaMethod method = client.getAddressSpace().getMethod(methodId);
		Variant[] inputs = readInputArguments(method);
		Variant[] outputs = client.call(nodeId, methodId, inputs);
		printOutputArguments(method, outputs);
	}

	/**
	 * Connect to the server.
	 *
	 * @throws ServerConnectionException
	 */
	protected void connect() throws ServerConnectionException {
		if (!client.isConnected())
			try {
				if (client.getProtocol() == Protocol.Https)
					println("Using HttpsSecurityPolicies "
							+ Arrays.toString(client.getHttpsSettings().getHttpsSecurityPolicies()));
				else {
					String securityPolicy = client.getEndpoint() == null
							? client.getSecurityMode().getSecurityPolicy().getPolicyUri()
							: client.getEndpoint().getSecurityPolicyUri();
					println("Using SecurityPolicy " + securityPolicy);
				}

				// Define the session name that is visible in the server
				client.setSessionName(String.format("%s@%s/Session%d", APP_NAME,
						ApplicationIdentity.getActualHostNameWithoutDomain(), ++sessionCount));

				client.connect();
				try {
					println("ServerStatus: " + client.getServerStatus());
					// println("Endpoint: " + client.getEndpoint());
				} catch (StatusException ex) {
					printException(ex);
				}
			} catch (InvalidServerEndpointException e) {
				print("Invalid Endpoint: ");
				printException(e);
				try {
					// In case we have selected a wrong endpoint, print out the
					// supported ones
					printEndpoints(client.discoverEndpoints());
				} catch (Exception ex) {
					// never mind, if the endpoints are not available
				}
			} catch (ServerConnectionException e) {
				printException(e);
				try {
					// In case we have selected an unavailable security mode,
					// print out the supported ones
					printSecurityModes(client.getSupportedSecurityModes());
				} catch (ServerConnectionException e1) {
					// never mind, if the security modes are not available
				} catch (ServiceException e1) {
					// never mind, if the security modes are not available
				}
			} catch (SessionActivationException e) {
				printException(e);
				try {
					printUserIdentityTokens(client.getSupportedUserIdentityTokens());
				} catch (ServiceException e1) {
					// never mind, if not available
				}
				return; // No point to continue
			} catch (ServiceException e) {
				printException(e);
			}
	}

	/**
	 * @param qualifiedName
	 * @return
	 */
	protected QualifiedName[] createBrowsePath(QualifiedName qualifiedName) {
		if (!qualifiedName.getName().contains("/"))
			return new QualifiedName[] { qualifiedName };
		int namespaceIndex = qualifiedName.getNamespaceIndex();
		String[] names = qualifiedName.getName().split("/");
		QualifiedName[] result = new QualifiedName[names.length];
		for (int i = 0; i < names.length; i++)
			result[i] = new QualifiedName(namespaceIndex, names[i]);
		return result;
	}

	/**
	 * Create an EventFilter that will pick all events from the node, except for
	 * ModelChange events, and requests the specific event fields that are
	 * defined in 'eventFieldNames'
	 *
	 * @return
	 */
	protected EventFilter createEventFilter(QualifiedName[] eventFields) {

		// This defines the event type of the fields.
		// It should be defined per browsePath, but for example
		// the Java SDK servers ignore the value at the moment
		NodeId eventTypeId = Identifiers.BaseEventType;
		UnsignedInteger eventAttributeId = Attributes.Value;
		String indexRange = null;
		SimpleAttributeOperand[] selectClauses = new SimpleAttributeOperand[eventFields.length + 1];
		for (int i = 0; i < eventFields.length; i++) {
			QualifiedName[] browsePath = createBrowsePath(eventFields[i]);
			selectClauses[i] = new SimpleAttributeOperand(eventTypeId, browsePath, eventAttributeId, indexRange);
		}
		// Add a field to get the NodeId of the event source
		selectClauses[eventFields.length] = new SimpleAttributeOperand(eventTypeId, null, Attributes.NodeId, null);
		EventFilter filter = new EventFilter();
		// Event field selection
		filter.setSelectClauses(selectClauses);

		// Event filtering: the following sample creates a
		// "Not OfType GeneralModelChangeEventType" filter
		ContentFilterBuilder fb = new ContentFilterBuilder(client.getEncoderContext());
		// The element operand refers to another operand -
		// operand #1 in this case which is the next,
		// LiteralOperand
		fb.add(FilterOperator.Not, new ElementOperand(UnsignedInteger.valueOf(1)));
		final LiteralOperand filteredType = new LiteralOperand(new Variant(Identifiers.GeneralModelChangeEventType));
		fb.add(FilterOperator.OfType, filteredType);

		// // Another example:
		// // OfType(custom NodeId) And (Severity > 800)
		// // Comment the previous example out if you try this
		// // Element #0
		// fb.add(FilterOperator.And, new ElementOperand(
		// UnsignedInteger.valueOf(1)),
		// new ElementOperand(UnsignedInteger.valueOf(2)));
		// // Element #1
		// final LiteralOperand filteredType = new
		// LiteralOperand(
		// new Variant(new NodeId(3, 1101)));
		// fb.add(FilterOperator.OfType, filteredType);
		// // Element #2
		// QualifiedName[] severityPath = { new QualifiedName(
		// "Severity") };
		// fb.add(FilterOperator.GreaterThan,
		// new SimpleAttributeOperand(
		// Identifiers.ConditionType,
		// severityPath, Attributes.Value, null),
		// new LiteralOperand(new Variant(800)));

		// Apply the filter to Where-clause
		filter.setWhereClause(fb.getContentFilter());
		return filter;
	}

	/**
	 * @param nodeId
	 * @param attributeId
	 * @return
	 */
	protected MonitoredDataItem createMonitoredDataItem(NodeId nodeId, UnsignedInteger attributeId) {
		/*
		 * Creating MonitoredDataItem, could also use the constructor without
		 * the sampling interval parameter, i.e. it would be default -1 and use
		 * the publishing interval of the subscription, but CTT expects positive
		 * values here for some tests.
		 */
		MonitoredDataItem dataItem = new MonitoredDataItem(nodeId, attributeId, MonitoringMode.Reporting,
				subscription.getPublishingInterval());

		dataItem.setDataChangeListener(dataChangeListener);
		DataChangeFilter filter = new DataChangeFilter();
		filter.setDeadbandValue(1.00);
		filter.setTrigger(DataChangeTrigger.StatusValue);
		filter.setDeadbandType(UnsignedInteger.valueOf(DeadbandType.Percent.getValue()));
		// Uncomment the following to set the DataChangeFilter for the items
		// try {
		// dataItem.setDataChangeFilter(filter);
		// } catch (ServiceException e) {
		// printException(e);
		// } catch (StatusException e) {
		// printException(e);
		// }
		return dataItem;
	}

	/**
	 * @param nodeId
	 * @return
	 * @throws StatusException
	 */
	protected MonitoredEventItem createMonitoredEventItem(NodeId nodeId) throws StatusException {
		initEventFieldNames();
		EventFilter filter = createEventFilter(eventFieldNames);

		// Create the item
		MonitoredEventItem eventItem = new MonitoredEventItem(nodeId, filter);
		eventItem.setEventListener(eventListener);
		return eventItem;
	}

	/**
	 * @param nodeId
	 * @param attributeId
	 * @return
	 * @throws ServiceException
	 * @throws StatusException
	 */
	protected void createMonitoredItem(Subscription sub, NodeId nodeId, UnsignedInteger attributeId)
			throws ServiceException, StatusException {
		UnsignedInteger monitoredItemId = null;
		// Create the monitored item, if it is not already in the
		// subscription
		if (!sub.hasItem(nodeId, attributeId)) {
			// Event or DataChange?
			final MonitoredItem item;

			if (attributeId == Attributes.EventNotifier) {
				MonitoredEventItem eventItem = createMonitoredEventItem(nodeId);

				// Refresh the current state
				// client.call(
				// Identifiers.Server,
				// Identifiers.ConditionType_ConditionRefresh,
				// new Variant[] { new Variant(subscription
				// .getSubscriptionId()) });
				item = eventItem;
			} else {
				MonitoredDataItem dataItem = createMonitoredDataItem(nodeId, attributeId);
				// Set the filter if you want to limit data changes
				DataChangeFilter filter = null;
				/* Request deadband */
				// try {
				// UaVariable node = (UaVariable) client.getAddressSpace()
				// .getNode(nodeId);
				// if (node.getDataType().inheritsFrom(Identifiers.Number)) {
				// print("Absolute deadband:");
				// Double deadband = Double.parseDouble(readInput(true));
				// filter = new DataChangeFilter(
				// DataChangeTrigger.StatusValue,
				// UnsignedInteger.valueOf(DeadbandType.Absolute
				// .getValue()), deadband);
				// }
				// } catch (Exception e) {
				// // nevermind
				// }
				dataItem.setDataChangeFilter(filter);
				item = dataItem;
			}
			double requestedSamplingInterval = item.getSamplingInterval();
			sub.addItem(item);
			monitoredItemId = item.getMonitoredItemId();
			double revisitedSamplingInterval = item.getSamplingInterval();

			if (Double.isInfinite(revisitedSamplingInterval))
				println("Error, got infinite number as revised sampling interval");
			else if (Double.isNaN(revisitedSamplingInterval))
				println("Error, got NaN as revised sampling interval");
			else if (revisitedSamplingInterval < 0)
				println("Warning, got negative number as revised sampling interval");
			else if (Math.abs(requestedSamplingInterval - revisitedSamplingInterval) > 0.01)
				println(String.format(
						"The requested sampling inteval is different than reguested, requested: %s, got: %s",
						requestedSamplingInterval, revisitedSamplingInterval));

		}
		println("-------------------------------------------------------");
		println("Subscription: Id=" + sub.getSubscriptionId() + " ItemId=" + monitoredItemId);
	}

	/**
	 * @param subscription
	 * @param nodeIds
	 * @return
	 * @throws ServiceException
	 * @throws StatusException
	 */
	protected void createMonitoredItemsForChildren(Subscription subscription, String nodeIds)
			throws ServiceException, StatusException {
		{
			String parentNodeId = nodeIds.substring(0, nodeIds.length() - 2);
			NodeId nodeId = NodeId.parseNodeId(parentNodeId);
			List<ReferenceDescription> refs = client.getAddressSpace().browse(nodeId, BrowseDirection.Forward,
					Identifiers.HasChild, true, NodeClass.Variable);
			List<MonitoredItem> items = new ArrayList<MonitoredItem>(refs.size());
			println("Subscribing to " + refs.size() + " items under node " + parentNodeId);
			for (ReferenceDescription r : refs)
				try {
					items.add(createMonitoredDataItem(client.getNamespaceTable().toNodeId(r.getNodeId()),
							Attributes.Value));
				} catch (ServiceResultException e) {
					printException(e);
				}
			try {
				subscription.addItems(items.toArray(new MonitoredItem[items.size()]));
				println("done");
			} catch (Exception e) {
				printException(e);
			}
		}
	}

	/**
	 * @return
	 * @throws ServiceException
	 * @throws StatusException
	 */
	protected Subscription createSubscription() throws ServiceException, StatusException {
		// Create the subscription
		Subscription subscription = new Subscription();

		// Default PublishingInterval is 1000 ms

		// subscription.setPublishingInterval(1000);

		// LifetimeCount should be at least 3 times KeepAliveCount

		// subscription.setLifetimeCount(1000);
		// subscription.setMaxKeepAliveCount(50);

		// If you are expecting big data changes, it may be better to break the
		// notifications to smaller parts

		// subscription.setMaxNotificationsPerPublish(1000);

		// Listen to the alive and timeout events of the subscription

		subscription.addAliveListener(subscriptionAliveListener);

		// Listen to notifications - the data changes and events are
		// handled using the item listeners (see below), but in many
		// occasions, it may be best to use the subscription
		// listener also to handle those notifications

		subscription.addNotificationListener(subscriptionListener);

		// Add it to the client
		client.addSubscription(subscription);
		return subscription;
	}

	/**
	 * @param attributeId
	 * @param nodeId
	 * @param value
	 * @return
	 */
	protected String dataValueToString(NodeId nodeId, UnsignedInteger attributeId, DataValue value) {
		StringBuilder sb = new StringBuilder();
		sb.append("Node: ");
		sb.append(nodeId);
		sb.append(".");
		sb.append(AttributesUtil.toString(attributeId));
		sb.append(" | Status: ");
		sb.append(value.getStatusCode());
		if (value.getStatusCode().isNotBad()) {
			sb.append(" | Value: ");
			if (value.isNull())
				sb.append("NULL");
			else {
				if (showReadValueDataType && Attributes.Value.equals(attributeId))
					try {
						UaVariable variable = (UaVariable) client.getAddressSpace().getNode(nodeId);
						if (variable == null)
							sb.append("(Cannot read node datatype from the server) ");
						else {

							NodeId dataTypeId = variable.getDataTypeId();
							UaType dataType = variable.getDataType();
							if (dataType == null)
								dataType = client.getAddressSpace().getType(dataTypeId);

							Variant variant = value.getValue();
							variant.getCompositeClass();
							if (attributeId.equals(Attributes.Value))
								if (dataType != null)
									sb.append("(" + dataType.getDisplayName().getText() + ")");
								else
									sb.append("(DataTypeId: " + dataTypeId + ")");
						}
					} catch (ServiceException e) {
					} catch (AddressSpaceException e) {
					}
				final Object v = value.getValue().getValue();
				if (value.getValue().isArray())
					sb.append(MultiDimensionArrayUtils.toString(v));
				else
					sb.append(v);
			}
		}
		sb.append(dateTimeToString(" | ServerTimestamp: ", value.getServerTimestamp(), value.getServerPicoseconds()));
		sb.append(dateTimeToString(" | SourceTimestamp: ", value.getSourceTimestamp(), value.getSourcePicoseconds()));
		return sb.toString();
	}

	/**
	 * Disconnect from the server.
	 */
	protected void disconnect() {
		client.disconnect();
	}

	/**
	 * @return
	 * @throws ServerListException
	 * @throws URISyntaxException
	 */
	protected boolean discover() throws ServerListException, URISyntaxException {
		ApplicationDescription serverApp;
		serverApp = discoverServer(client.getUri());
		if (serverApp != null) {
			EndpointDescription endpoint = discoverEndpoints(serverApp);
			if (endpoint != null) {
				client.disconnect();
				client.setEndpoint(endpoint);
				return true;
			}
		}
		return false;
	}

	protected EndpointDescription discoverEndpoints(ApplicationDescription serverApp) throws URISyntaxException {
		final String[] discoveryUrls = serverApp.getDiscoveryUrls();
		if (discoveryUrls != null) {
			UaClient discoveryClient = new UaClient();
			int i = 0;
			List<EndpointDescription> edList = new ArrayList<EndpointDescription>();

			println("Available endpoints: ");
			println(String.format("%s - %-50s - %-20s - %-20s - %s", "#", "URI", "Security Mode", "Security Policy",
					"Transport Profile"));
			for (String url : discoveryUrls) {
				discoveryClient.setUri(url);
				try {
					for (EndpointDescription ed : discoveryClient.discoverEndpoints()) {
						println(String.format("%s - %-50s - %-20s - %-20s - %s", i++, ed.getEndpointUrl(),
								ed.getSecurityMode(),
								ed.getSecurityPolicyUri().replaceFirst("http://opcfoundation.org/UA/SecurityPolicy#",
										""),
								ed.getTransportProfileUri()
										.replaceFirst("http://opcfoundation.org/UA-Profile/Transport/", "")));
						edList.add(ed);
					}
				} catch (Exception e) {
					println("Cannot discover Endpoints from URL " + url + ": " + e.getMessage());
				}
			}
			System.out.println("-------------------------------------------------------");
			println("- Enter endpoint number to select that one");
			println("- Enter x to return to cancel");
			System.out.println("-------------------------------------------------------");
			// // Select an endpoint with the same protocol as the
			// // original request, if available
			// URI uri = new URI(url);
			// if (uri.getScheme().equals(client.getProtocol().toString()))
			// {
			// connectUrl = url;
			// println("Selected application "
			// + serverApp.getApplicationName().getText()
			// + " at " + url);
			// break;
			// } else if (connectUrl == null)
			// connectUrl = url;

			EndpointDescription endpoint = null;
			while (endpoint == null)
				try {
					int n = readAction();
					if (n == ACTION_RETURN)
						return null;
					else
						return edList.get(n);
				} catch (Exception e) {

				}
		} else
			println("No suitable discoveryUrl available: using the current Url");
		return null;
	}

	/**
	 * @param fieldValues
	 * @return
	 */
	protected String eventFieldsToString(QualifiedName[] fieldNames, Variant[] fieldValues) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fieldValues.length; i++) {
			Object fieldValue = fieldValues[i] == null ? null : fieldValues[i].getValue();
			// Find the BrowseName of the node corresponding to NodeId values
			try {
				UaNode node = null;
				if (fieldValue instanceof NodeId)
					node = client.getAddressSpace().getNode((NodeId) fieldValue);
				else if (fieldValue instanceof ExpandedNodeId)
					node = client.getAddressSpace().getNode((ExpandedNodeId) fieldValue);
				if (node != null)
					fieldValue = String.format("%s {%s}", node.getBrowseName(), fieldValue);
			} catch (Exception e) {
				// Node not found, just use fieldValue
			}
			if (i < fieldNames.length) {
				QualifiedName fieldName = fieldNames[i];
				sb.append(fieldName.getName() + "=" + fieldValue + "; ");
			} else
				sb.append("Node=" + fieldValue + "; ");
		}
		return sb.toString();
	}

	protected String eventToString(NodeId nodeId, QualifiedName[] fieldNames, Variant[] fieldValues) {
		return String.format("Node: %s Fields: %s", nodeId, eventFieldsToString(fieldNames, fieldValues));
	}

	/**
	 * @param nodeId
	 * @return
	 */
	protected String getCurrentNodeAsString(UaNode node) {
		String nodeStr = "";
		String typeStr = "";
		String analogInfoStr = "";
		nodeStr = node.getDisplayName().getText();
		UaType type = null;
		if (node instanceof UaInstance)
			type = ((UaInstance) node).getTypeDefinition();
		typeStr = (type == null ? nodeClassToStr(node.getNodeClass()) : type.getDisplayName().getText());

		// This is the way to access type specific nodes and their
		// properties, for example to show the engineering units and
		// range for all AnalogItems
		if (node instanceof AnalogItemType)
			try {
				AnalogItemType analogNode = (AnalogItemType) node;
				EUInformation units = analogNode.getEngineeringUnits();
				analogInfoStr = units == null ? "" : " Units=" + units.getDisplayName().getText();
				Range range = analogNode.getEuRange();
				analogInfoStr = analogInfoStr
						+ (range == null ? "" : String.format(" Range=(%f; %f)", range.getLow(), range.getHigh()));
			} catch (Exception e) {
				printException(e);
			}

		String currentNodeStr = String.format("*** Current Node: %s: %s (ID: %s)%s", nodeStr, typeStr, node.getNodeId(),
				analogInfoStr);
		return currentNodeStr;
	}

	/**
	 * @throws StatusException
	 */
	protected void initEventFieldNames() throws StatusException {
		if (eventFieldNames[eventFieldNames.length - 1] == null) {
			// Define the custom fields, against the MyEventType definition of
			// the
			// SampleConsoleServer
			// For other servers we should get null values in response

			// First find the namespaceIndex for our custom fields
			String SAMPLE_ADDRESS_SPACE = "http://www.prosysopc.com/OPCUA/SampleAddressSpace";
			int namespaceIndex;
			namespaceIndex = client.getNamespaceTable().getIndex(SAMPLE_ADDRESS_SPACE);

			if (namespaceIndex < 0)
				// We are connected to a server other than SampleConsoleServer
				// Setting the namespaceIndex to some valid index
				namespaceIndex = 0;
			eventFieldNames[eventFieldNames.length - 2] = new QualifiedName(namespaceIndex, "MyVariable");
			eventFieldNames[eventFieldNames.length - 1] = new QualifiedName(namespaceIndex, "MyProperty");

		}
	}

	/**
	 * Initializes the object, must be called after constructor
	 *
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws SecureIdentityException
	 * @throws SessionActivationException
	 * @throws ServerListException
	 */
	protected void initialize(String[] args) throws URISyntaxException, SecureIdentityException, IOException,
			SessionActivationException, ServerListException {

		println("Connecting to " + serverUri);

		// *** Create the UaClient
		client = new UaClient(serverUri);

		// Add listener
		client.setListener(clientListener);

		// Use PKI files to keep track of the trusted and rejected server
		// certificates...
		final PkiFileBasedCertificateValidator validator = new PkiFileBasedCertificateValidator();
		client.setCertificateValidator(validator);
		// ...and react to validation results with a custom handler (to prompt
		// the user what to do, if necessary)
		validator.setValidationListener(validationListener);

		// *** Application Description is sent to the server
		ApplicationDescription appDescription = new ApplicationDescription();
		// 'localhost' (all lower case) in the ApplicationName and
		// ApplicationURI is converted to the actual host name of the computer
		// in which the application is run
		appDescription.setApplicationName(new LocalizedText(APP_NAME + "@localhost"));
		appDescription.setApplicationUri("urn:localhost:OPCUA:" + APP_NAME);
		appDescription.setProductUri("urn:prosysopc.com:OPCUA:" + APP_NAME);
		appDescription.setApplicationType(ApplicationType.Client);

		// *** Certificates

		File privatePath = new File(validator.getBaseDir(), "private");

		// Create self-signed certificates
		KeyPair issuerCertificate = null;

		// Enable the following to define a CA certificate which is used to
		// issue the keys.

		// issuerCertificate =
		// ApplicationIdentity.loadOrCreateIssuerCertificate(
		// "ProsysSampleCA", privatePath, "opcua", 3650, false);

		int[] keySizes = null;
		// If you wish to use big certificates (4096 bits), you will need to
		// define two certificates for your application, since to interoperate
		// with old applications, you will also need to use a small certificate
		// (up to 2048 bits).

		// 4096 bits can only be used with Basic256Sha256 security profile,
		// which is currently not enabled by default, so we will also not define
		// this by default.

		// Use 0 to use the default keySize and default file names as before
		// (for other values the file names will include the key size).
		// keySizes = new int[] { 0, 4096 };

		// *** Application Identity
		// Define the client application identity, including the security
		// certificate
		final ApplicationIdentity identity = ApplicationIdentity.loadOrCreateCertificate(appDescription,
				"Sample Organisation", /* Private Key Password */"opcua",
				/* Key File Path */privatePath,
				/* CA certificate & private key */issuerCertificate,
				/* Key Sizes for instance certificates to create */keySizes,
				/* Enable renewing the certificate */true);

		// Create the HTTPS certificate.
		// The HTTPS certificate must be created, if you enable HTTPS.
		String hostName = InetAddress.getLocalHost().getHostName();
		identity.setHttpsCertificate(ApplicationIdentity.loadOrCreateHttpsCertificate(appDescription, hostName, "opcua",
				issuerCertificate, privatePath, true));

		client.setApplicationIdentity(identity);

		// Define our user locale - the default is Locale.getDefault()
		client.setLocale(Locale.ENGLISH);

		// Define the call timeout in milliseconds. Default is null - to
		// use the value of UaClient.getEndpointConfiguration() which is
		// 120000 (2 min) by default
		client.setTimeout(30000);

		// StatusCheckTimeout is used to detect communication
		// problems and start automatic reconnection.
		// These are the default values:
		client.setStatusCheckTimeout(10000);
		// client.setAutoReconnect(true);

		// Listen to server status changes
		client.addServerStatusListener(serverStatusListener);

		// Define the security mode
		// - Default (in UaClient) is BASIC128RSA15_SIGN_ENCRYPT
		// client.setSecurityMode(SecurityMode.BASIC128RSA15_SIGN_ENCRYPT);
		// client.setSecurityMode(SecurityMode.BASIC128RSA15_SIGN);
		// client.setSecurityMode(SecurityMode.NONE);

		// securityMode is defined from the command line
		client.setSecurityMode(securityMode);

		// Define the security policies for HTTPS; ALL is the default
		client.getHttpsSettings().setHttpsSecurityPolicies(HttpsSecurityPolicy.ALL);

		// Define a custom certificate validator for the HTTPS certificates
		client.getHttpsSettings().setCertificateValidator(validator);

		// Or define just a validation rule to check the hostname defined for
		// the certificate; ALLOW_ALL_HOSTNAME_VERIFIER is the default
		// client.getHttpsSettings().setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		// If the server supports user authentication, you can set the user
		// identity.
		if (userName == null)
			// - Default is to use Anonymous authentication, like this:
			client.setUserIdentity(new UserIdentity());
		else {
			// - Use username/password authentication (note requires security,
			// above):
			if (passWord == null) {
				print("Enter password for user " + userName + ":");
				passWord = readInput(false);
			}
			client.setUserIdentity(new UserIdentity(userName, passWord));
		}
		// - Read the user certificate and private key from files:
		// client.setUserIdentity(new UserIdentity(new java.net.URL(
		// "my_certificate.der"), new java.net.URL("my_protectedkey.pfx"),
		// "my_protectedkey_password"));

		// Session timeout 10 minutes; default is one hour
		// client.setSessionTimeout(600000);

		// Set endpoint configuration parameters
		client.getEndpointConfiguration().setMaxByteStringLength(Integer.MAX_VALUE);
		client.getEndpointConfiguration().setMaxArrayLength(Integer.MAX_VALUE);

		// TCP Buffer size parameters - these may help with high traffic
		// situations.
		// See http://fasterdata.es.net/host-tuning/background/ for some hints
		// how to use them
		// TcpConnection.setReceiveBufferSize(700000);
		// TcpConnection.setSendBufferSize(700000);

	}

	protected void mainMenu() throws ServerListException, URISyntaxException {

		if (connectToDiscoveryServer)
			if (!discover())
				return;

		// Try to connect to the server already at this point.
		connect();

		// Subscribe to items specified from command line
		subscribeToInitialItems();

		// You have one node selected all the time, and all operations
		// target that. We can initialize that to the standard ID of the
		// RootFolder (unless it was specified from command line).

		// Identifiers contains a list of all standard node IDs
		if (nodeId == null)
			nodeId = Identifiers.RootFolder;

		/******************************************************************************/
		/* Wait for user command to execute next action. */
		do {
			printMenu(nodeId);

			try {
				switch (readAction()) {
				case ACTION_RETURN:
					disconnect();
					return;
				case 0:
					if (discover())
						connect();
					break;
				case 1:
					connect();
					break;
				case 2:
					disconnect();
					break;
				case 3:
					NodeId browseId = browse(nodeId, null);
					if (browseId != null)
						nodeId = browseId;
					break;
				case 4:
					read(nodeId);
					break;
				case 5:
					write(nodeId);
					break;
				case 6:
					registerNodes(nodeId);
					break;
				case 7:
					unregisterNodes();
					break;
				case 8:
					subscribe(nodeId);
					break;
				case 9:
					NodeId methodId = readMethodId(nodeId);
					if (methodId != null)
						callMethod(nodeId, methodId);
					break;
				case 10:
					readHistory(nodeId);
				default:
					continue;
				}
			} catch (Exception e) {
				printException(e);
			}

		} while (true);
		/******************************************************************************/
	}

	/**
	 * Parse Command line arguments. Expected options:
	 * <UL>
	 * <LI>-d connect to a discovery server instead of a normal server
	 * <LI>-t show stack trace with exceptions
	 * <LI>-n do not prompt for the server URI, if it is not specified
	 * </UL>
	 *
	 * Also expects to get the serverUri - if not, it is prompted (unless -n
	 * given)
	 *
	 * @param args
	 *            the arguments
	 * @return
	 */
	protected boolean parseCmdLineArgs(String[] args) throws IllegalArgumentException {
		int i = 0;
		boolean secModeSet = false;
		while ((args.length > i) && ((args[i].startsWith("-") || args[i].startsWith("/")))) {
			if (args[i].equals("-d")) {
				println("Connecting to a discovery server.");
				connectToDiscoveryServer = true;
			} else if (args[i].equals("-n"))
				nodeId = NodeId.parseNodeId(args[++i]);
			else if (args[i].equals("-s")) {
				String arg = args[++i];
				parseSecurityMode(arg);
				secModeSet = true;

			} else if (args[i].equals("-k"))
				CertificateUtils.setKeySize(Integer.parseInt(args[++i]));
			else if (args[i].equals("-m"))
				initialMonitoredItems.add(args[++i]);
			else if (args[i].equals("-u"))
				userName = args[++i];
			else if (args[i].equals("-p"))
				passWord = args[++i];
			else if (args[i].equals("-t"))
				stackTraceOnException = true;
			else if (args[i].equals("-dt"))
				showReadValueDataType = true;
			else if (args[i].equals("-?"))
				return false;
			else
				throw new IllegalArgumentException(args[i]);
			i++;
		}
		if (i < args.length) {
			serverUri = args[i++];
			while ((i < args.length) && !args[i].startsWith("#"))
				cmdSequence.add(args[i++]);
		}
		if (serverUri == null) {
			serverUri = promptServerUri();
			if (!secModeSet)
				promptSecurityMode();
		}
		return true;
	}

	protected void parseSecurityMode(String arg) {
		char secModeStr = arg.charAt(0);
		int level = 0;
		if (arg.length() > 1)
			level = Integer.parseInt(arg.substring(1, 2));
		if (secModeStr == 'n')
			securityMode = SecurityMode.NONE;
		else if (secModeStr == 's')
			switch (level) {
			default:
			case 1:
				securityMode = SecurityMode.BASIC128RSA15_SIGN;
				break;
			case 2:
				securityMode = SecurityMode.BASIC256_SIGN;
				break;
			// Will be available in a new stack
			case 3:
				securityMode = SecurityMode.BASIC256SHA256_SIGN;
				break;
			}
		else if (secModeStr == 'e')
			switch (level) {
			default:
			case 1:
				securityMode = SecurityMode.BASIC128RSA15_SIGN_ENCRYPT;
				break;
			case 2:
				securityMode = SecurityMode.BASIC256_SIGN_ENCRYPT;
				break;
			// Will be available in a new stack
			case 3:
				securityMode = SecurityMode.BASIC256SHA256_SIGN_ENCRYPT;
				break;
			}
		else
			throw new IllegalArgumentException(
					"parameter for SecuirtyMode (-s) is invalid, expected 'n', 's' or 'e'; was '" + secModeStr + "'");
	}

	/**
	 * @param nodeId
	 */
	protected void printCurrentNode(NodeId nodeId) {
		if (client.isConnected())
			// Find the node from the NodeCache
			try {
			UaNode node = client.getAddressSpace().getNode(nodeId);

			if (node == null)
			return;
			String currentNodeStr = getCurrentNodeAsString(node);
			if (currentNodeStr != null) {
			println(currentNodeStr);
			println("");
			}
			} catch (ServiceException e) {
			printException(e);
			} catch (AddressSpaceException e) {
			printException(e);
			}
	}

	protected void printEndpoints(EndpointDescription[] endpoints) {
		println("Endpoints supported by the server (by Discovery Service)");
		for (EndpointDescription e : endpoints)
			println(String.format("%s [%s,%s]", e.getEndpointUrl(), e.getSecurityPolicyUri(), e.getSecurityMode()));

	}

	protected void printMenu(NodeId nodeId) {
		println("");
		println("");
		println("");
		if (client.isConnected()) {
			println("*** Connected to: " + client.getUri());
			println("");
			if (nodeId != null)
				printCurrentNode(nodeId);
		} else
			println("*** NOT connected to: " + client.getUri());

		System.out.println("-------------------------------------------------------");
		println("- Enter x to close client");
		System.out.println("-------------------------------------------------------");
		System.out.println("- Enter 0 to start discovery                          -");
		System.out.println("- Enter 1 to connect to server                        -");
		System.out.println("- Enter 2 to disconnect from server                   -");
		System.out.println("- Enter 3 to browse the server address space          -");
		System.out.println("- Enter 4 to read values                              -");
		System.out.println("- Enter 5 to write values                             -");
		System.out.println("- Enter 6 to register nodes                           -");
		System.out.println("- Enter 7 to unregister nodes                         -");
		if (subscription == null)
			System.out.println("- Enter 8 to create a subscription                    -");
		else
			System.out.println("- Enter 8 to add a new item to the subscription       -");
		System.out.println("- Enter 9 to call a method                            -");
		System.out.println("- Enter 10 to read history                            -");
		System.out.println("-------------------------------------------------------");
	}

	/**
	 * @param method
	 *            The method node
	 * @param outputs
	 *            The output values The output values
	 * @throws AddressSpaceException
	 * @throws ServiceException
	 * @throws MethodArgumentException
	 *             if the output arguments are not valid for the node
	 * @throws StatusException
	 */
	protected void printOutputArguments(UaMethod method, Variant[] outputs)
			throws ServiceException, AddressSpaceException, MethodArgumentException, StatusException {
		if ((outputs != null) && (outputs.length > 0)) {
			println("Output values:");
			Argument[] outputArguments = method.getOutputArguments();
			for (int i = 0; i < outputArguments.length; i++) {
				UaNode dataType = client.getAddressSpace().getType(outputArguments[i].getDataType());
				println(String.format("%s: %s {%s} = %s", outputArguments[i].getName(), dataType.getBrowseName(),
						outputArguments[i].getDescription().getText(), outputs[i].getValue()));
			}
		} else
			println("OK (no output)");
	}

	/**
	 * @param supportedSecurityModes
	 */
	protected void printSecurityModes(List<SecurityMode> supportedSecurityModes) {
		println("SecurityModes supported by the server:");
		for (SecurityMode m : supportedSecurityModes)
			println(m.toString());

	}

	/**
	 * @param supportedUserIdentityTokens
	 */
	protected void printUserIdentityTokens(UserTokenPolicy[] supportedUserIdentityTokens) {
		println("The server supports the following user tokens:");
		for (UserTokenPolicy p : supportedUserIdentityTokens)
			println(p.getTokenType().toString());

	}

	/**
	 *
	 */
	protected void promptSecurityMode() {
		println("Select the security mode to use.");
		println("(n=None,s=Sign,e=SignAndEncrypt)");
		while (true)
			try {
				parseSecurityMode(readInput(false).toLowerCase());
				break;
			} catch (IllegalArgumentException e) {
				printException(e);
			}
	}

	/**
	 * @return
	 * @throws IllegalArgumentException
	 */
	protected String promptServerUri() throws IllegalArgumentException {
		while (true) {
			println("Enter the connection URL of the server to connect to\n(press enter to use the default address="
					+ defaultServerUri + "):");

			String url = readInput(false, defaultServerUri);
			try {
				UaAddress.validate(url);
				return url;
			} catch (URISyntaxException e) {
				print(e.getMessage() + "\n\n");
			}
		}
	}

	/**
	 * @param nodeId
	 * @throws StatusException
	 * @throws ServiceException
	 *
	 */
	protected void read(NodeId nodeId) throws ServiceException, StatusException {
		println("read node " + nodeId);
		UnsignedInteger attributeId = readAttributeId();
		DataValue value = client.readAttribute(nodeId, attributeId);
		println(dataValueToString(nodeId, attributeId, value));

	}

	/**
	 * @return
	 */
	protected UnsignedInteger readAttributeId() {

		println("Select the node attribute.");
		for (long i = Attributes.NodeId.getValue(); i < Attributes.UserExecutable.getValue(); i++)
			printf("%d - %s\n", i, AttributesUtil.toString(UnsignedInteger.valueOf(i)));
		int action = readAction();
		if (action < 0)
			return null;
		UnsignedInteger attributeId = UnsignedInteger.valueOf(action);
		System.out.println("attribute: " + AttributesUtil.toString(attributeId));
		return attributeId;
	}

	/**
	 * @param nodeId
	 * @throws StatusException
	 * @throws ServiceException
	 * @throws AddressSpaceException
	 *
	 */
	protected void readHistory(NodeId nodeId) throws ServiceException, StatusException, AddressSpaceException {
		UaNode node = client.getAddressSpace().getNode(nodeId);

		if (node instanceof UaVariable) {

			// Validate that history is readable for the node

			UaVariable variable = (UaVariable) node;
			if (!variable.getAccessLevel().contains(AccessLevel.HistoryRead)) {
				println("The variable does not have history");
				return;
			}
			println(" 0 - raw data");
			println(" 1 - at times");
			println(" 2 - Average"); // Unless stated otherwise, all the
										// Aggregates are calculated from the
			println(" 3 - Count"); // last 30 minutes, using 5 seconds intervals
									// and forward time flow.
			println(" 4 - Delta");
			println(" 5 - End");
			println(" 6 - Maximum");
			println(" 7 - MaximumActualTime");
			println(" 8 - Minimum");
			println(" 9 - MinimumActualTime");
			println("10 - Range");
			println("11 - Start");
			println("12 - WorstQuality");
			int action = readAction();

			println("Reading history of variable " + variable.getBrowseName());
			try {
				DateTime endTime = DateTime.currentTime();
				DateTime startTime = new DateTime((endTime.getMilliSeconds() - (1800 * 1000)) * 10000);
				DataValue[] values = null;

				switch (action) {
				case 0:
					println("between " + startTime + " and " + endTime);

					values = client.historyReadRaw(nodeId, startTime, endTime, UnsignedInteger.valueOf(1000), true,
							null, TimestampsToReturn.Source);
					break;
				case 1:
					println("at " + startTime + " and " + endTime);

					DateTime[] reqTimes = new DateTime[] { startTime, endTime };
					values = client.historyReadAtTimes(nodeId, reqTimes, null, TimestampsToReturn.Source);
					break;
				default:
					println("at " + startTime + " and " + endTime);
					Double processingInterval = 5 * 1000.0;
					NodeId aggregateType = null;
					AggregateConfiguration aggregateConfiguration = new AggregateConfiguration(false, true,
							new UnsignedByte(100), new UnsignedByte(100), false);
					NumericRange indexRange = null;
					switch (action) {
					case 2:
						aggregateType = Identifiers.AggregateFunction_Average;
						break;
					case 3:
						aggregateType = Identifiers.AggregateFunction_Count;
						break;
					case 4:
						aggregateType = Identifiers.AggregateFunction_Delta;
						break;
					case 5:
						aggregateType = Identifiers.AggregateFunction_End;
						break;
					case 6:
						aggregateType = Identifiers.AggregateFunction_Maximum;
						break;
					case 7:
						aggregateType = Identifiers.AggregateFunction_MaximumActualTime;
						break;
					case 8:
						aggregateType = Identifiers.AggregateFunction_Minimum;
						break;
					case 9:
						aggregateType = Identifiers.AggregateFunction_MinimumActualTime;
						break;
					case 10:
						aggregateType = Identifiers.AggregateFunction_Range;
						break;
					case 11:
						aggregateType = Identifiers.AggregateFunction_Start;
						break;
					case 12:
						aggregateType = Identifiers.AggregateFunction_WorstQuality;
						break;
					}
					values = client.historyReadProcessed(nodeId, startTime, endTime, processingInterval, aggregateType,
							aggregateConfiguration, indexRange, TimestampsToReturn.Source);
				}

				if (values != null) {
					println("Count = " + values.length);
					for (int i = 0; i < values.length; i++)
						println("Value " + (i + 1) + " = " + values[i].getValue().getValue() + " | "
								+ values[i].getSourceTimestamp() + " | " + values[i].getStatusCode());
				}
			} catch (Exception e) {
				printException(e);
			}
		} else if (node instanceof UaObject) {

			// Validate that history is readable for the node

			UaObject object = (UaObject) node;
			if (!object.getEventNotifier().contains(EventNotifierClass.HistoryRead)) {
				println("The object does not have history");
				return;
			}

			println("Reading event history of node " + node);
			try {
				DateTime endTime = DateTime.currentTime();
				DateTime startTime = new DateTime((endTime.getMilliSeconds() - (3600 * 1000)) * 10000);
				HistoryEventFieldList[] events = null;

				println("between " + startTime + " and " + endTime);

				// Use the same filter that is used by MonitoredEventItems
				initEventFieldNames();
				EventFilter eventFilter = createEventFilter(eventFieldNames);
				events = client.historyReadEvents(nodeId, startTime, endTime, UnsignedInteger.ZERO, eventFilter,
						TimestampsToReturn.Source);

				if (events != null) {
					println("Count = " + events.length);
					for (int i = 0; i < events.length; i++)
						println("Event " + (i + 1) + " = "
								+ eventFieldsToString(eventFieldNames, events[i].getEventFields()));
				}
			} catch (Exception e) {
				printException(e);
			}
		}

		else
			println("History is only available for object and variable nodes. The current node is a "
					+ node.getNodeClass() + ".");
	}

	/**
	 * Read the values for each input argument from the console.
	 *
	 * @param method
	 *            The method whose inputs are read
	 * @return Variant array of the input values
	 * @throws ServiceException
	 *             if a service call fails
	 * @throws AddressSpaceException
	 *             if the input data types cannot be determined
	 * @throws ServerConnectionException
	 *             if we are not connected to the client
	 * @throws MethodArgumentException
	 *             if the input arguments are not validly defined for the node
	 * @throws StatusException
	 */
	protected Variant[] readInputArguments(UaMethod method) throws ServiceException, ServerConnectionException,
			AddressSpaceException, MethodArgumentException, StatusException {
		Argument[] inputArguments = method.getInputArguments();
		if ((inputArguments == null) || (inputArguments.length == 0))
			return new Variant[0];
		Variant[] inputs = new Variant[inputArguments.length];
		println("Enter value for Inputs:");
		for (int i = 0; i < inputs.length; i++) {
			UaDataType dataType = (UaDataType) client.getAddressSpace().getType(inputArguments[i].getDataType());
			println(String.format("%s: %s {%s} = ", inputArguments[i].getName(), dataType.getDisplayName().getText(),
					inputArguments[i].getDescription().getText()));
			while (inputs[i] == null)
				try {
					inputs[i] = client.getAddressSpace().getDataTypeConverter().parseVariant(readInput(false),
							dataType);
				} catch (NumberFormatException e) {
					printException(e);
				}
		}
		return inputs;
	}

	/**
	 * @param nodeId
	 * @return
	 * @throws ServiceException
	 * @throws ServerConnectionException
	 * @throws StatusException
	 * @throws AddressSpaceException
	 */
	protected NodeId readMethodId(NodeId nodeId)
			throws ServerConnectionException, ServiceException, StatusException, AddressSpaceException {
		// Ensure that we are in an object node
		NodeClass nodeClass = client.getAddressSpace().getNode(nodeId).getNodeClass();
		if (!nodeClass.equals(NodeClass.Object)) {
			println("Not in an object.");
			println("You must be in an object that has methods to be able to call methods.");
			if (nodeClass.equals(NodeClass.Method))
				println("Since you are currently at a method node, back up one step to the object.");
			return null;
		}
		// A lightweight way to list the methods is to use browseMethods
		// List<ReferenceDescription> methodRefs =
		// client.getAddressSpace().browseMethods(nodeId);
		List<UaMethod> methods = client.getAddressSpace().getMethods(nodeId);
		if (methods.size() == 0) {
			println("No methods available.");
			return null;
		}
		println("Select the method to execute.");
		for (int i = 0; i < methods.size(); i++)
			printf("%d - %s\n", i, methods.get(i).getDisplayName().getText());
		int action;
		do
			action = readAction();
		while (action >= methods.size());
		if (action < 0)
			return null;
		NodeId methodId = methods.get(action).getNodeId();
		System.out.println("Method: " + methodId);
		return methodId;
	}

	/**
	 * @param r
	 * @return
	 * @throws ServiceException
	 * @throws ServerConnectionException
	 * @throws StatusException
	 */
	protected String referenceToString(ReferenceDescription r)
			throws ServerConnectionException, ServiceException, StatusException {
		if (r == null)
			return "";
		String referenceTypeStr = null;
		try {
			// Find the reference type from the NodeCache
			UaReferenceType referenceType = (UaReferenceType) client.getAddressSpace().getType(r.getReferenceTypeId());
			if ((referenceType != null) && (referenceType.getDisplayName() != null))
				if (r.getIsForward())
					referenceTypeStr = referenceType.getDisplayName().getText();
				else
					referenceTypeStr = referenceType.getInverseName().getText();
		} catch (AddressSpaceException e) {
			printException(e);
			print(r.toString());
			referenceTypeStr = r.getReferenceTypeId().getValue().toString();
		}
		String typeStr;
		switch (r.getNodeClass()) {
		case Object:
		case Variable:
			try {
				// Find the type from the NodeCache
				UaNode type = client.getAddressSpace().getNode(r.getTypeDefinition());
				if (type != null)
					typeStr = type.getDisplayName().getText();
				else
					typeStr = r.getTypeDefinition().getValue().toString();
			} catch (AddressSpaceException e) {
				printException(e);
				print("type not found: " + r.getTypeDefinition().toString());
				typeStr = r.getTypeDefinition().getValue().toString();
			}
			break;
		default:
			typeStr = nodeClassToStr(r.getNodeClass());
			break;
		}
		return String.format("%s%s (ReferenceType=%s, BrowseName=%s%s)", r.getDisplayName().getText(), ": " + typeStr,
				referenceTypeStr, r.getBrowseName(), r.getIsForward() ? "" : " [Inverse]");
	}

	/**
	 * @param nodeId
	 *
	 */
	protected void registerNodes(NodeId nodeId) {
		try {
			NodeId[] registeredNodeId = client.getAddressSpace().registerNodes(nodeId);
			println("Registered NodeId " + nodeId + " -> registeredNodeId is " + registeredNodeId[0]);
		} catch (ServiceException e) {
			printException(e);
		}
	}

	/**
	 * @param serverUri
	 */
	protected void setServerUri(String serverUri) {
		this.serverUri = serverUri;
	}

	/**
	 * @param nodeId
	 *
	 */
	protected void subscribe(NodeId nodeId) {
		if (nodeId == null) {
			println("*** Select a node to subscribe first ");
			println("");
			return;
		}
		println("*** Subscribing to node: " + nodeId);
		println("");
		UnsignedInteger attributeId = readAttributeId();
		if (attributeId != null) {
			try {
				// Create the subscription
				if (subscription == null)
					subscription = createSubscription();
				// Create the monitored item
				createMonitoredItem(subscription, nodeId, attributeId);

			} catch (ServiceException e) {
				printException(e);
			} catch (StatusException e) {
				printException(e);
			}

			/*
			 * Show the menu and wait for action.
			 */
			try {
				subscriptionMenu();
			} catch (ServiceException e) {
				printException(e);
			} catch (StatusException e) {
				printException(e);
			}
		}

	}

	/**
	 * Subscribe to items specified from command line
	 */
	protected void subscribeToInitialItems() {
		if (!initialMonitoredItems.isEmpty())
			// while (true) {
			try {
			if (subscription == null)
			subscription = createSubscription();
			} catch (ServiceException e) {
			printException(e);
			return;
			} catch (StatusException e) {
			printException(e);
			return;
			}
		for (String s : initialMonitoredItems)
			try {
				if (s.endsWith("/*"))
					createMonitoredItemsForChildren(subscription, s);
				else
					createMonitoredItem(subscription, NodeId.parseNodeId(s), Attributes.Value);
			} catch (IllegalArgumentException e1) {
				printException(e1);
			} catch (ServiceException e1) {
				printException(e1);
			} catch (StatusException e1) {
				printException(e1);
			}
		try {
			if (subscription != null)
				subscriptionMenu();
		} catch (ServiceException e) {
			printException(e);
		} catch (StatusException e) {
			printException(e);
		}
		// }
	}

	/**
	 * @throws ServiceException
	 * @throws StatusException
	 */
	protected void subscriptionMenu() throws ServiceException, StatusException {
		// Make sure the subscription is re-enabled, in case it was
		// paused earlier (see below). By default, new subscriptions are
		// enabled
		subscription.setPublishingEnabled(true);

		do {
			println("-------------------------------------------------------");
			println("- Enter x to end (and remove) the subcription");
			println("- Enter p to pause the subscription (e.g. to add new items)");
			println("- Enter r to remove an item from the subscription");
			println("- Enter i to change the publishing interval of the subscription");
			println("-------------------------------------------------------");
			String input;
			input = readInput(true);
			if (input.equals("r")) {
				subscription.setPublishingEnabled(false);
				try {
					MonitoredItemBase removedItem = null;
					while (removedItem == null) {
						println("-------------------------------------------------------");
						println("Monitored Items:");
						for (MonitoredItemBase item : subscription.getItems())
							println(item.toString());
						println("- Enter the ClientHandle of the item to remove it");
						println("- Enter x to cancel.");
						println("-------------------------------------------------------");
						String handleStr = readInput(true);
						if (handleStr.equals("x"))
							break;
						try {
							UnsignedInteger handle = UnsignedInteger.parseUnsignedInteger(handleStr);
							removedItem = subscription.removeItem(handle);
							printf(removedItem != null ? "Item %s removed\n" : "No such item: %s\n", handle);
						} catch (Exception e) {
							printException(e);
						}
					}
				} finally {
					subscription.setPublishingEnabled(true);

				}
			} else if (input.equals("p")) {
				subscription.setPublishingEnabled(false);
				break;
			} else if (input.equals("x")) {
				try {
					StatusCode sc = client.removeSubscription(subscription);
					if ((sc != null) && sc.isNotGood())
						throw new StatusException(sc);
				} catch (Exception e) {
					/*
					 * Nevermind. Well actually the CTT does check that these
					 * exceptions are displayed so..
					 */
					print("Got exception while deleting Subscription: ");
					printException(e);
				}
				subscription = null;
				break;
			} else if (input.equals("i")) {
				boolean wasEnabled = subscription.isPublishingEnabled();
				println("Current publishing interval is " + subscription.getPublishingInterval() + " ms");
				print("New publishing interval: ");
				try {
					subscription.setPublishingEnabled(false);
					try {
						double interval = Double.parseDouble(readInput(true));
						subscription.setPublishingInterval(interval);
						println("Publishing interval changed to " + interval + " ms");
					} catch (NumberFormatException e) {
						printException(e);
					}
				} finally {
					subscription.setPublishingEnabled(wasEnabled);
				}
			}
		} while (true);
	}

	/**
	 * Unregisters all previously registered nodes.
	 */
	protected void unregisterNodes() {
		try {
			NodeId[] nodes = client.getAddressSpace().unregisterAllNodes();
			println("Unregistered " + nodes.length + " node(s).");
		} catch (ServiceException e) {
			printException(e);
		}
	}

	/**
	 * @param nodeId
	 * @throws StatusException
	 * @throws AddressSpaceException
	 * @throws ServiceException
	 */
	protected void write(NodeId nodeId) throws ServiceException, AddressSpaceException, StatusException {
		UnsignedInteger attributeId = readAttributeId();

		UaNode node = client.getAddressSpace().getNode(nodeId);
		println("Writing to node " + nodeId + " - " + node.getDisplayName().getText());

		// Find the DataType if setting Value - for other properties you must
		// find the correct data type yourself
		UaDataType dataType = null;
		if (attributeId.equals(Attributes.Value) && (node instanceof UaVariable)) {
			UaVariable v = (UaVariable) node;
			dataType = (UaDataType) v.getDataType();
			println("DataType: " + dataType.getDisplayName().getText());
		}

		print("Enter the value to write: ");
		String value = readInput(true);
		try {
			Object convertedValue = dataType != null
					? client.getAddressSpace().getDataTypeConverter().parseVariant(value, dataType) : value;
			boolean status = client.writeAttribute(nodeId, attributeId, convertedValue);
			if (status)
				println("OK");
			else
				println("OK (completes asynchronously)");
		} catch (ServiceException e) {
			printException(e);
		} catch (StatusException e) {
			printException(e);
		}

	}

	/**
	 * @return
	 * @throws ServerListException
	 *             if the client list cannot be retrieved
	 *
	 */
	ApplicationDescription discoverServer(String uri) throws ServerListException {
		// Discover a new server list from a discovery server at URI
		ServerList serverList = new ServerList(uri);
		if (serverList.size() == 0) {
			println("No servers found");
			return null;
		}

		println(String.format("%s - %-25s - %-15s - %-30s - %s", "#", "Name", "Type", "Product", "Application"));
		for (int i = 0; i < serverList.size(); i++) {
			final ApplicationDescription s = serverList.get(i);
			println(String.format("%d - %-25s - %-15s - %-30s - %s", i, s.getApplicationName().getText(),
					s.getApplicationType(), s.getProductUri(), s.getApplicationUri()));
		}
		System.out.println("-------------------------------------------------------");
		println("- Enter client number to select that one");
		println("- Enter x to return to cancel");
		System.out.println("-------------------------------------------------------");
		do {
			int action = readAction();
			switch (action) {
			case ACTION_RETURN:
				return null;
			default:
				return serverList.get(action);
			}
		} while (true);
	}

}
