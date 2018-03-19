/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */
package com.prosysopc.ua.samples;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.PropertyConfigurator;
import org.opcfoundation.ua.builtintypes.DateTime;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.core.UserTokenPolicy;
import org.opcfoundation.ua.transport.security.HttpsSecurityPolicy;
import org.opcfoundation.ua.transport.security.KeyPair;
import org.opcfoundation.ua.transport.security.SecurityMode;
import org.opcfoundation.ua.utils.CertificateUtils;
import org.opcfoundation.ua.utils.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.CertificateValidationListener;
import com.prosysopc.ua.ModelException;
import com.prosysopc.ua.PkiFileBasedCertificateValidator;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.UaAddress;
import com.prosysopc.ua.UaApplication.Protocol;
import com.prosysopc.ua.nodes.UaProperty;
import com.prosysopc.ua.server.FileNodeManager;
import com.prosysopc.ua.server.NodeBuilderException;
import com.prosysopc.ua.server.NodeManagerListener;
import com.prosysopc.ua.server.UaInstantiationException;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.UaServerException;
import com.prosysopc.ua.server.UserValidator;
import com.prosysopc.ua.server.compliance.ComplianceNodeManager;
import com.prosysopc.ua.server.compliance.NonUaNodeComplianceNodeManager;
import com.prosysopc.ua.server.nodes.FileFolderType;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.server.nodes.UaVariableNode;
import com.prosysopc.ua.types.opcua.server.BuildInfoTypeNode;

/**
 * A sample OPC UA server application.
 */
public class SampleConsoleServer {
	enum Action {
		ADD_NODE('a', "add a new node") {
			@Override
			ActionResult performAction(SampleConsoleServer s) {
				println("Enter the name of the new node (enter 'x' to cancel)");
				String name = readInput();
				if (!name.equals("x"))
					s.myNodeManager.addNode(name);
				return ActionResult.NOTHING;
			}
		},

		CLOSE('x', "close the server") {
			@Override
			ActionResult performAction(SampleConsoleServer s) {
				return ActionResult.CLOSE_SERVER;
			}
		},

		DELETE_NODE('d', "delete a node") {
			@Override
			ActionResult performAction(SampleConsoleServer s) throws StatusException {
				println("Enter the name of the node to delete (enter 'x' to cancel)");
				String input = readInput();
				if (!input.equals("x")) {
					QualifiedName nodeName = new QualifiedName(s.myNodeManager.getNamespaceIndex(), input);
					s.myNodeManager.deleteNode(nodeName);
				}
				return ActionResult.NOTHING;
			}
		},

		ENABLE_DIAGNOSTICS('D', "enable/disable server diagnostics") {
			@Override
			ActionResult performAction(SampleConsoleServer s) throws StatusException {
				final UaProperty enabledFlag = s.server.getNodeManagerRoot().getServerData().getServerDiagnosticsNode()
						.getEnabledFlagNode();
				boolean newValue = !((Boolean) enabledFlag.getValue().getValue().getValue());
				enabledFlag.setValue(Boolean.valueOf(newValue));
				println("Server Diagnostics " + (newValue ? "Enabled" : "Disabled"));
				return ActionResult.NOTHING;
			}
		},

		SEND_EVENT('e', "send an event") {
			@Override
			ActionResult performAction(SampleConsoleServer s) {
				s.sendEvent();
				return ActionResult.NOTHING;
			}
		};

		static Map<Character, Action> actionMap = new TreeMap<Character, Action>();

		static {
			for (Action a : Action.values())
				actionMap.put(a.getKey(), a);
		}

		public static Action parseAction(Character s) {
			return actionMap.get(s);
		}

		private final String description;
		private final Character key;

		Action(Character key, String description) {
			this.key = key;
			this.description = description;
		}

		public String getDescription() {
			return description;
		}

		/**
		 * @return the key
		 */
		public Character getKey() {
			return key;
		}

		/**
		 * Perform the Action
		 *
		 * @param s
		 *            the SampleConsoleServer instance (inner enums are static,
		 *            so this is a "trick" to access SampleConsoleServer's
		 *            fields from the inner enum)
		 * @return ActionResult
		 * @throws Exception
		 */
		abstract ActionResult performAction(SampleConsoleServer s) throws Exception;
	}

	enum ActionResult {
		CLOSE_SERVER, NOTHING;
	}

	/**
	 * Number of nodes to create for the Big Node Manager. This can be modified
	 * from the command line.
	 */
	private static int bigAddressSpaceNodes = 1000;
	private static Logger logger = LoggerFactory.getLogger(SampleConsoleServer.class);
	private static boolean stackTraceOnException = false;
	protected static String APP_NAME = "SampleConsoleServer";

	protected static String discoveryServerUrl = "opc.tcp://localhost:4840";

	/**
	 * @param args
	 *            command line arguments for the application
	 * @throws StatusException
	 *             if the server address space creation fails
	 * @throws UaServerException
	 *             if the server initialization parameters are invalid
	 * @throws CertificateException
	 *             if the application certificate or private key, cannot be
	 *             loaded from the files due to certificate errors
	 */
	public static void main(String[] args) throws Exception {
		// Initialize log4j logging
		PropertyConfigurator.configureAndWatch(SampleConsoleServer.class.getResource("log.properties").getFile(), 5000);

		try {
			if (!parseCmdLineArgs(args)) {
				usage();
				return;
			}
		} catch (IllegalArgumentException e) {
			println("Invalid cmd line argument: " + e.getMessage());
			usage();
			return;
		}

		// *** Initialization and Start Up
		SampleConsoleServer sampleConsoleServer = new SampleConsoleServer();

		// Initialize the server
		sampleConsoleServer.initialize(52520, 52443, APP_NAME);

		// Create the address space
		sampleConsoleServer.createAddressSpace();

		// TCP Buffer size parameters - this may help with high traffic
		// situations.
		// See http://fasterdata.es.net/host-tuning/background/ for some hints
		// how to use it
		// UATcpServer.setReceiveBufferSize(700000);

		// Start the server, when you have finished your own initializations
		// This will allow connections from the clients
		// Start up the server (enabling or disabling diagnostics according to
		// the cmd line args)
		sampleConsoleServer.run(getUseDiags(args));
	}

	/**
	 * @param e
	 */
	public static void printException(Exception e) {
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
	public static void println(String string) {
		System.out.println(string);
	}

	/**
	 * @return
	 */
	private static Action readAction() {
		return Action.parseAction(readInput().charAt(0));
	}

	/**
	 * @return
	 */
	private static String readInput() {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String s = null;
		do
			try {
				s = stdin.readLine();
			} catch (IOException e) {
				printException(e);
			}
		while ((s == null) || (s.length() == 0));
		return s;
	}

	/**
	 * Check if diagnostics is enabled from the command line
	 *
	 * @param args
	 * @return
	 */
	protected static boolean getUseDiags(String[] args) {
		for (String arg : args)
			if (arg.equals("-enablesessiondiags"))
				return true;
		return false;
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
	protected static boolean parseCmdLineArgs(String[] args) throws IllegalArgumentException {
		int i = 0;
		while ((args.length > i) && ((args[i].startsWith("-") || args[i].startsWith("/")))) {
			if (args[i].equals("-t"))
				stackTraceOnException = true;
			else if (args[i].equals("-b"))
				bigAddressSpaceNodes = Integer.parseInt(args[++i]);
			else if (args[i].equals("-k"))
				CertificateUtils.setKeySize(Integer.parseInt(args[++i]));
			else if (args[i].equals("-d"))
				discoveryServerUrl = args[++i];
			else if (args[i].equals("-d-"))
				discoveryServerUrl = "";
			else if (args[i].equals("-?"))
				return false;
			else
				throw new IllegalArgumentException(args[i]);
			i++;
		}
		return true;
	}

	/**
	 *
	 */
	protected static void usage() {
		println("Usage: " + APP_NAME + " [-b] [-t] [serverUri]");
		println("   -b n       Define number of nodes to create in the BigNodeManager (default=1000)");
		println("   -k keySize Define the size of the public key of the application certificate (default 1024; other valid values 2048, 4096)");
		println("   -d url     Define the DiscoveryServerUrl to register the application to");
		println("   -d-        Define that the application should not be registered to a DiscoveryServer");
		println("   -t         Output stack trace for errors");
		println("   -?         Show this help text");
		println("");
	}

	static void printMenu() {
		println("");
		println("");
		println("");
		System.out.println("-------------------------------------------------------");
		for (Entry<Character, Action> a : Action.actionMap.entrySet())
			println("- Enter " + a.getKey() + " to " + a.getValue().getDescription());
	}

	private final Runnable simulationTask = new Runnable() {

		@Override
		public void run() {
			if (server.isRunning()) {
				logger.debug("Simulating");
				simulate();
			}
		}
	};

	private final ScheduledExecutorService simulator = Executors.newScheduledThreadPool(10);
	protected ComplianceNodeManager complianceNodeManager;
	protected FileNodeManager fileNodeManager;
	protected MyBigNodeManager myBigNodeManager;
	protected MyHistorian myHistorian = new MyHistorian();
	protected MyNodeManager myNodeManager;
	protected NodeManagerListener myNodeManagerListener = new MyNodeManagerListener();
	protected NonUaNodeComplianceNodeManager nonUaNodeComplianceManager;
	protected UaServer server;
	protected UserValidator userValidator;
	protected final CertificateValidationListener validationListener = new MyCertificateValidationListener();

	public UaServer getServer() {
		return server;
	}

	/**
	 * Create a sample node manager, which does not use UaNode objects. These
	 * are suitable for managing big address spaces for data that is in practice
	 * available from another existing subsystem.
	 */
	private void createBigNodeManager() {
		myBigNodeManager = new MyBigNodeManager(server, "http://www.prosysopc.com/OPCUA/SampleBigAddressSpace",
				bigAddressSpaceNodes);
	}

	/**
	 * @throws StatusException
	 *
	 */
	private void createFileNodeManager() throws StatusException {
		fileNodeManager = new FileNodeManager(getServer(), "http://www.prosysopc.com/OPCUA/FileTransfer", "Files");
		getServer().getNodeManagerRoot().getObjectsFolder().addReference(fileNodeManager.getRootFolder(),
				Identifiers.Organizes, false);
		FileFolderType folder = fileNodeManager.addFolder("Folder");
		folder.setFilter("*");
	}

	/**
	 * Create a sample address space with a new folder, a device object, a level
	 * variable, and an alarm condition.
	 * <p>
	 * The method demonstrates the basic means to create the nodes and
	 * references into the address space.
	 * <p>
	 * Simulation of the level measurement is defined in
	 * {@link #startSimulation()}
	 *
	 * @throws StatusException
	 *             if the referred type nodes are not found from the address
	 *             space
	 * @throws UaInstantiationException
	 * @throws NodeBuilderException
	 * @throws URISyntaxException
	 * @throws ModelException
	 * @throws IOException
	 * @throws SAXException
	 *
	 */
	protected void createAddressSpace() throws StatusException, UaInstantiationException, NodeBuilderException {
		// Load the standard information models
		loadInformationModels();

		// My Node Manager
		myNodeManager = new MyNodeManager(server, MyNodeManager.NAMESPACE);

		myNodeManager.addListener(myNodeManagerListener);

		// My I/O Manager Listener
		myNodeManager.getIoManager().addListeners(new MyIoManagerListener());

		// My HistoryManager
		myNodeManager.getHistoryManager().setListener(myHistorian);

		// ComplianceNodeManagers
		complianceNodeManager = new ComplianceNodeManager(server, "http://www.prosysopc.com/OPCUA/ComplianceNodes");
		nonUaNodeComplianceManager = new NonUaNodeComplianceNodeManager(server,
				"http://www.prosysopc.com/OPCUA/ComplianceNonUaNodes");

		// A sample node manager that can handle a big amount of UA nodes
		// without creating UaNode objects in memory
		createBigNodeManager();

		createFileNodeManager();

		logger.info("Address space created.");
	}

	/**
	 * Initialize the information to the Server BuildInfo structure
	 */
	protected void initBuildInfo() {
		// Initialize BuildInfo - using the version info from the SDK
		// You should replace this with your own build information

		final BuildInfoTypeNode buildInfo = server.getNodeManagerRoot().getServerData().getServerStatusNode()
				.getBuildInfoNode();

		buildInfo.setProductName(APP_NAME);

		// Fetch version information from the package manifest
		final Package sdkPackage = UaServer.class.getPackage();
		final String implementationVersion = sdkPackage.getImplementationVersion();
		if (implementationVersion != null) {
			int splitIndex = implementationVersion.lastIndexOf(".");
			final String softwareVersion = implementationVersion.substring(0, splitIndex);
			String buildNumber = implementationVersion.substring(splitIndex + 1);

			buildInfo.setManufacturerName(sdkPackage.getImplementationVendor());
			buildInfo.setSoftwareVersion(softwareVersion);
			buildInfo.setBuildNumber(buildNumber);

		}

		final URL classFile = UaServer.class.getResource("/com/prosysopc/ua/samples/server/SampleConsoleServer.class");
		if (classFile != null) {
			final File mfFile = new File(classFile.getFile());
			GregorianCalendar c = new GregorianCalendar();
			c.setTimeInMillis(mfFile.lastModified());
			buildInfo.setBuildDate(new DateTime(c));
		}
	}

	/**
	 *
	 */
	protected void initHistory() {
		for (UaVariableNode v : myNodeManager.getHistorizableVariables())
			myHistorian.addVariableHistory(v);
		for (UaObjectNode o : myNodeManager.getHistorizableEvents())
			myHistorian.addEventHistory(o);
	}

	protected void initialize(int port, int httpsPort, String applicationName)
			throws SecureIdentityException, IOException, UaServerException {

		// *** Create the server
		server = new UaServer();
		// Uncomment to enable IPv6 networking
		// server.setEnableIPv6(true);

		// Use PKI files to keep track of the trusted and rejected client
		// certificates...
		final PkiFileBasedCertificateValidator validator = new PkiFileBasedCertificateValidator();
		server.setCertificateValidator(validator);
		// ...and react to validation results with a custom handler
		validator.setValidationListener(validationListener);

		// *** Application Description is sent to the clients
		ApplicationDescription appDescription = new ApplicationDescription();
		// 'localhost' (all lower case) in the ApplicationName and
		// ApplicationURI is converted to the actual host name of the computer
		// (including the possible domain part) in which the application is run.
		// (as available from ApplicationIdentity.getActualHostName())
		// 'hostname' is converted to the host name without the domain part.
		// (as available from
		// ApplicationIdentity.getActualHostNameWithoutDomain())
		appDescription.setApplicationName(new LocalizedText(applicationName + "@localhost"));
		appDescription.setApplicationUri("urn:localhost:OPCUA:" + applicationName);
		appDescription.setProductUri("urn:prosysopc.com:OPCUA:" + applicationName);
		appDescription.setApplicationType(ApplicationType.Server);

		// *** Server Endpoints
		// TCP Port number for the UA Binary protocol
		server.setPort(Protocol.OpcTcp, port);
		// TCP Port for the HTTPS protocol
		server.setPort(Protocol.Https, httpsPort);

		// optional server name part of the URI (default for all protocols)
		server.setServerName("OPCUA/" + applicationName);

		// Optionally restrict the InetAddresses to which the server is bound.
		// You may also specify the addresses for each Protocol.
		// This is the default (isEnableIPv6 defines whether IPv6 address should
		// be included in the bound addresses. Note that it requires Java 7 or
		// later to work in practice in Windows):
		server.setBindAddresses(EndpointUtil.getInetAddresses(server.isEnableIPv6()));

		// *** Certificates

		File privatePath = new File(validator.getBaseDir(), "private");

		// Define a certificate for a Certificate Authority (CA) which is used
		// to issue the keys. Especially
		// the HTTPS certificate should be signed by a CA certificate, in order
		// to make the .NET applications trust it.
		//
		// If you have a real CA, you should use that instead of this sample CA
		// and create the keys with it.
		// Here we use the IssuerCertificate only to sign the HTTPS certificate
		// (below) and not the Application Instance Certificate.
		KeyPair issuerCertificate = ApplicationIdentity.loadOrCreateIssuerCertificate("ProsysSampleCA", privatePath,
				"opcua", 3650, false);

		// If you wish to use big certificates (4096 bits), you will need to
		// define two certificates for your application, since to interoperate
		// with old applications, you will also need to use a small certificate
		// (up to 2048 bits).

		// Also, 4096 bits can only be used with Basic256Sha256 security
		// profile, which is currently not enabled by default, so we will also
		// leave the the keySizes array as null. In that case, the default key
		// size defined by CertificateUtils.getKeySize() is used.
		int[] keySizes = null;

		// Use 0 to use the default keySize and default file names as before
		// (for other values the file names will include the key size).
		// keySizes = new int[] { 0, 4096 };

		// *** Application Identity

		// Define the Server application identity, including the Application
		// Instance Certificate (but don't sign it with the issuerCertificate as
		// explained above).
		final ApplicationIdentity identity = ApplicationIdentity.loadOrCreateCertificate(appDescription,
				"Sample Organisation", /* Private Key Password */"opcua",
				/* Key File Path */privatePath,
				/* Issuer Certificate & Private Key */null,
				/* Key Sizes for instance certificates to create */keySizes,
				/* Enable renewing the certificate */true);

		// Create the HTTPS certificate bound to the hostname.
		// The HTTPS certificate must be created, if you enable HTTPS.
		String hostName = ApplicationIdentity.getActualHostName();
		identity.setHttpsCertificate(ApplicationIdentity.loadOrCreateHttpsCertificate(appDescription, hostName, "opcua",
				issuerCertificate, privatePath, true));

		server.setApplicationIdentity(identity);

		// *** Security settings
		// Define the security modes to support for the Binary protocol -
		// ALL is the default
		server.setSecurityModes(SecurityMode.ALL);
		// The TLS security policies to use for HTTPS
		server.getHttpsSettings().setHttpsSecurityPolicies(HttpsSecurityPolicy.ALL);

		// Number of threads to reserve for the HTTPS server, default is 10
		// server.setHttpsWorkerThreadCount(10);

		// Define a custom certificate validator for the HTTPS certificates
		server.getHttpsSettings().setCertificateValidator(validator);
		// client.getHttpsSettings().setCertificateValidator(...);

		// Or define just a validation rule to check the hostname defined for
		// the certificate; ALLOW_ALL_HOSTNAME_VERIFIER is the default
		// client.getHttpsSettings().setHostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

		// Define the supported user Token policies
		server.addUserTokenPolicy(UserTokenPolicy.ANONYMOUS);
		server.addUserTokenPolicy(UserTokenPolicy.SECURE_USERNAME_PASSWORD);
		server.addUserTokenPolicy(UserTokenPolicy.SECURE_CERTIFICATE);
		// Define a validator for checking the user accounts
		final PkiFileBasedCertificateValidator userCertValidator = new PkiFileBasedCertificateValidator("USERS_PKI");
		userValidator = new MyUserValidator(userCertValidator);
		server.setUserValidator(userValidator);

		// Register on the local discovery server (if present)
		try {
			UaAddress discoveryAddress = new UaAddress(discoveryServerUrl);
			server.setDiscoveryServerAddress(discoveryAddress);
		} catch (URISyntaxException e) {
			logger.error("DiscoveryURL is not valid", e);
		}

		// *** init() creates the service handlers and the default endpoints
		// according to the above settings
		server.init();

		initBuildInfo();

		// "Safety limits" for ill-behaving clients
		server.getSessionManager().setMaxSessionCount(500);
		server.getSessionManager().setMaxSessionTimeout(3600000); // one hour
		server.getSubscriptionManager().setMaxSubscriptionCount(50);

		// You can do your own additions to server initializations here

	}

	/**
	 * Load information models into the address space. Also register classes, to
	 * be able to use the respective Java classes with
	 * NodeManagerUaNode.createInstance().
	 *
	 * See the codegen/Readme.md on instructions how to use your own models.
	 */
	protected void loadInformationModels() {
		// Uncomment to take the extra information models in use.

		// // Register generated classes
		// server.registerModel(com.prosysopc.ua.types.di.server.InformationModel.MODEL);
		// server.registerModel(com.prosysopc.ua.types.adi.server.InformationModel.MODEL);
		// server.registerModel(com.prosysopc.ua.types.plc.server.InformationModel.MODEL);
		//
		// // Load the standard information models
		// try {
		// server.getAddressSpace().loadModel(
		// UaServer.class.getResource("Opc.Ua.Di.NodeSet2.xml")
		// .toURI());
		// server.getAddressSpace().loadModel(
		// UaServer.class.getResource("Opc.Ua.Adi.NodeSet2.xml")
		// .toURI());
		// server.getAddressSpace().loadModel(
		// UaServer.class.getResource("Opc.Ua.Plc.NodeSet2.xml")
		// .toURI());
		// } catch (Exception e) {
		// throw new RuntimeException(e);
		// }
	}

	/*
	 * Main loop for user selecting OPC UA calls
	 */
	protected void mainMenu() {

		/******************************************************************************/
		/* Wait for user command to execute next action. */
		do {
			printMenu();

			try {
				Action action = readAction();
				if (action != null) {
					ActionResult actionResult = action.performAction(this);
					switch (actionResult) {
					case CLOSE_SERVER:
						return; // closes server
					case NOTHING:
						continue; // continue looping menu
					}
				}
			} catch (Exception e) {
				printException(e);
			}

		} while (true);
		/******************************************************************************/
	}

	/**
	 * Run the server.
	 *
	 * @param enableSessionDiagnostics
	 * @throws UaServerException
	 * @throws StatusException
	 */
	protected void run(boolean enableSessionDiagnostics) throws UaServerException, StatusException {
		server.start();
		initHistory();
		if (enableSessionDiagnostics)
			server.getNodeManagerRoot().getServerData().getServerDiagnosticsNode().setEnabled(true);
		startSimulation();

		// *** Main Menu Loop
		mainMenu();

		// *** End
		stopSimulation();
		// Notify the clients about a shutdown, with a 5 second delay
		println("Shutting down...");
		server.shutdown(5, new LocalizedText("Closed by user", Locale.ENGLISH));
		println("Closed.");
	}

	/**
	 *
	 */
	protected void sendEvent() {
		myNodeManager.sendEvent();
	}

	protected void simulate() {
		myNodeManager.simulate();
		myBigNodeManager.simulate();
	}

	/**
	 * Starts the simulation of the level measurement.
	 */
	protected void startSimulation() {
		simulator.scheduleAtFixedRate(simulationTask, 1000, 1000, TimeUnit.MILLISECONDS);
		logger.info("Simulation started.");
	}

	/**
	 * Ends simulation.
	 */
	protected void stopSimulation() {
		simulator.shutdown();
		logger.info("Simulation stopped.");
	}
}
