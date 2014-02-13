package smart.itsm;

import net.cellcloud.talk.dialect.ActionDialect;
import smart.itsm.core.ActionListener;
import smart.itsm.core.Failure;
import smart.itsm.core.FailureListener;

public class DummyListener implements ActionListener, FailureListener {

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
	public void onFailed(Failure failure) {
		this.handler.obtainMessage(LogHandler.MSG_FAILURE, failure).sendToTarget();
	}
}
