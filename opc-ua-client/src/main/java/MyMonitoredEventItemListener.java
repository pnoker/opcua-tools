
import org.opcfoundation.ua.builtintypes.QualifiedName;
import org.opcfoundation.ua.builtintypes.Variant;

import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.MonitoredEventItemListener;

/**
 * A sampler listener for monitored event notifications.
 */
public class MyMonitoredEventItemListener implements MonitoredEventItemListener {
	private final SampleConsoleClient client;
	private final QualifiedName[] requestedEventFields;

	/**
	 * @param client
	 * @param eventFieldNames
	 */
	public MyMonitoredEventItemListener(SampleConsoleClient client, QualifiedName[] requestedEventFields) {
		super();
		this.requestedEventFields = requestedEventFields;
		this.client = client;
	}

	@Override
	public void onEvent(MonitoredEventItem sender, Variant[] eventFields) {
		SampleConsoleClient.println(client.eventToString(sender.getNodeId(), requestedEventFields, eventFields));
	}
};
