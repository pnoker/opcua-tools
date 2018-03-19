/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Locale;

import org.apache.log4j.PropertyConfigurator;
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.core.ApplicationDescription;
import org.opcfoundation.ua.core.ApplicationType;
import org.opcfoundation.ua.core.Identifiers;
import org.opcfoundation.ua.transport.security.SecurityMode;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.client.UaClient;

/**
 * A very minimal client application. Connects to the server and reads one
 * variable. Works with a non-secure connection.
 */
public class SimpleClient {
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configureAndWatch(SampleConsoleClient.class.getResource("log.properties").getFile(), 5000);
		UaClient client = new UaClient("opc.tcp://localhost:52520/OPCUA/SampleConsoleServer");
		client.setSecurityMode(SecurityMode.NONE);
		initialize(client);
		client.connect();
		DataValue value = client.readValue(Identifiers.Server_ServerStatus_State);
		System.out.println(value);
		client.disconnect();
	}

	/**
	 * Define a minimal ApplicationIdentity. If you use secure connections, you
	 * will also need to define the application instance certificate and manage
	 * server certificates. See the SampleConsoleClient.initialize() for a full
	 * example of that.
	 */
	protected static void initialize(UaClient client)
			throws SecureIdentityException, IOException, UnknownHostException {
		// *** Application Description is sent to the server
		ApplicationDescription appDescription = new ApplicationDescription();
		appDescription.setApplicationName(new LocalizedText("SimpleClient", Locale.ENGLISH));
		// 'localhost' (all lower case) in the URI is converted to the actual
		// host name of the computer in which the application is run
		appDescription.setApplicationUri("urn:localhost:UA:SimpleClient");
		appDescription.setProductUri("urn:prosysopc.com:UA:SimpleClient");
		appDescription.setApplicationType(ApplicationType.Client);

		final ApplicationIdentity identity = new ApplicationIdentity();
		identity.setApplicationDescription(appDescription);
		client.setApplicationIdentity(identity);
	}

}
