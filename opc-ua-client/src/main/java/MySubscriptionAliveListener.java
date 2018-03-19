/**
 * Prosys OPC UA Java SDK
 *
 * Copyright (c) Prosys PMS Ltd., <http://www.prosysopc.com>.
 * All rights reserved.
 */

import java.util.Calendar;

import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;

/**
 * A sampler listener for subscription alive events.
 */
public class MySubscriptionAliveListener implements SubscriptionAliveListener {

	@Override
	public void onAfterCreate(Subscription s) {
		// the subscription was (re)created to the server
		// this happens if the subscription was timed out during
		// a communication break and had to be recreated after reconnection
		SampleConsoleClient.println(String.format("%tc Subscription created: ID=%d lastAlive=%tc",
				Calendar.getInstance(), s.getSubscriptionId().getValue(), s.getLastAlive()));
	}

	@Override
	public void onAlive(Subscription s) {
		// the server acknowledged that the connection is alive,
		// although there were no changes to send
		SampleConsoleClient.println(String.format("%tc Subscription alive: ID=%d lastAlive=%tc", Calendar.getInstance(),
				s.getSubscriptionId().getValue(), s.getLastAlive()));
	}

	@Override
	public void onTimeout(Subscription s) {
		// the server did not acknowledge that the connection is alive, and the
		// maxKeepAliveCount has been exceeded
		SampleConsoleClient.println(String.format("%tc Subscription timeout: ID=%d lastAlive=%tc",
				Calendar.getInstance(), s.getSubscriptionId().getValue(), s.getLastAlive()));

	}

};