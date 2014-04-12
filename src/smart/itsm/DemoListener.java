package smart.itsm;

import net.cellcloud.talk.dialect.ActionDialect;
import smart.itsm.core.ActionListener;
import smart.itsm.core.Failure;
import smart.itsm.core.StatusListener;

/**
 * 演示用监听器。
 * 
 * @author Jiangwei Xu
 *
 */
public class DemoListener implements ActionListener, StatusListener {

	private static final String ACTION_NAME = "test";

	private DemoHandler handler;

	public DemoListener(DemoHandler handler) {
		this.handler = handler;
	}

	public String getAction() {
		return ACTION_NAME;
	}

	@Override
	public void onAction(ActionDialect action) {
		this.handler.obtainMessage(DemoHandler.MSG_ACTION, action).sendToTarget();
	}

	@Override
	public void onConnected(String identifier) {
		this.handler.obtainMessage(DemoHandler.MSG_CONNECT, identifier).sendToTarget();
	}

	@Override
	public void onDisconnected(String identifier) {
		this.handler.obtainMessage(DemoHandler.MSG_DISCONNECT, identifier).sendToTarget();
	}

	@Override
	public void onFailed(String identifier, Failure failure) {
		this.handler.obtainMessage(DemoHandler.MSG_FAILURE, failure).sendToTarget();
	}
}
