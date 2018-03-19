/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */

import org.opcfoundation.ua.builtintypes.LocalizedText;
import org.opcfoundation.ua.core.ServerState;
import org.opcfoundation.ua.core.ServerStatusDataType;

import com.prosysopc.ua.client.ServerStatusListener;
import com.prosysopc.ua.client.UaClient;

/**
 * A sampler listener for server status changes.
 */
public class MyServerStatusListener implements ServerStatusListener {
	@Override
	public void onShutdown(UaClient uaClient, long secondsTillShutdown, LocalizedText shutdownReason) {
		// Called when the server state changes to Shutdown
		SampleConsoleClient.printf("Server shutdown in %d seconds. Reason: %s\n", secondsTillShutdown,
				shutdownReason.getText());
	}

	@Override
	public void onStateChange(UaClient uaClient, ServerState oldState, ServerState newState) {
		// Called whenever the server state changes
		SampleConsoleClient.printf("ServerState changed from %s to %s\n", oldState, newState);
		if (newState.equals(ServerState.Unknown))
			SampleConsoleClient.println("ServerStatusError: " + uaClient.getServerStatusError());
	}

	@Override
	public void onStatusChange(UaClient uaClient, ServerStatusDataType status) {
		// Called whenever the server status changes, typically every
		// StatusCheckInterval defined in the UaClient.
		// println("ServerStatus: " + status);
	}
};