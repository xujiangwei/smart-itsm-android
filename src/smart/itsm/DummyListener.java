package smart.itsm;

import net.cellcloud.talk.dialect.ActionDialect;
import smart.itsm.core.ActionListener;
import smart.itsm.core.Failure;
import smart.itsm.core.StatusListener;

public class DummyListener implements ActionListener, StatusListener {

	public static final String ACTION_NAME = "test";

	private LogHandler handler;

	public DummyListener(LogHandler handler) {
		this.handler = handler;
	}

	@Override
	public void onAction(ActionDialect action) {
		this.handler.obtainMessage(LogHandler.MSG_ACTION, action).sendToTarget();
	}

	@Override
	public void onConnected(String identifier) {
		this.handler.obtainMessage(LogHandler.MSG_CONNECT, identifier).sendToTarget();
	}

	@Override
	public void onDisconnected(String identifier) {
		this.handler.obtainMessage(LogHandler.MSG_DISCONNECT, identifier).sendToTarget();
	}

	@Override
	public void onFailed(String identifier, Failure failure) {
		this.handler.obtainMessage(LogHandler.MSG_FAILURE, failure).sendToTarget();
	}
}
