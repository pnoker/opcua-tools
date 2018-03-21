
import org.opcfoundation.ua.builtintypes.DataValue;
import org.opcfoundation.ua.builtintypes.DiagnosticInfo;
import org.opcfoundation.ua.builtintypes.ExtensionObject;
import org.opcfoundation.ua.builtintypes.StatusCode;
import org.opcfoundation.ua.builtintypes.UnsignedInteger;
import org.opcfoundation.ua.builtintypes.Variant;
import org.opcfoundation.ua.core.NotificationData;

import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionNotificationListener;

/**
 * A sampler listener for subscription notifications.
 */
public class MySubscriptionNotificationListener implements SubscriptionNotificationListener {

	@Override
	public void onBufferOverflow(Subscription subscription, UnsignedInteger sequenceNumber,
			ExtensionObject[] notificationData) {
		SampleConsoleClient.println("*** SUBCRIPTION BUFFER OVERFLOW ***");
	}

	@Override
	public void onDataChange(Subscription subscription, MonitoredDataItem item, DataValue newValue) {
		// Called for each data change notification
	}

	@Override
	public void onError(Subscription subscription, Object notification, Exception exception) {
		// Called if the parsing of the notification data fails,
		// notification is either a MonitoredItemNotification or
		// an EventList
		SampleConsoleClient.printException(exception);
	}

	@Override
	public void onEvent(Subscription subscription, MonitoredEventItem item, Variant[] eventFields) {
		// Called for each event notification
	}

	@Override
	public long onMissingData(UnsignedInteger lastSequenceNumber, long sequenceNumber, long newSequenceNumber,
			StatusCode serviceResult) {
		// Called if a data packet is missed due to communication errors and
		// failing Republish
		SampleConsoleClient.println(
				"Data missed: lastSequenceNumber=" + lastSequenceNumber + " newSequenceNumber=" + newSequenceNumber);
		return newSequenceNumber; // Accept the default
	}

	@Override
	public void onNotificationData(Subscription subscription, NotificationData notification) {
		// Called after a complete notification data package is
		// handled
		// if (notification instanceof DataChangeNotification) {
		// DataChangeNotification d = (DataChangeNotification) notification;
		// SampleConsoleClient.println("onNotificationData: " +
		// d.getMonitoredItems().length);
		// }
	}

	@Override
	public void onStatusChange(Subscription subscription, StatusCode oldStatus, StatusCode newStatus,
			DiagnosticInfo diagnosticInfo) {
		// Called when the subscription status has changed in
		// the server
	}

};
