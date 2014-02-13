package smart.itsm;

import net.cellcloud.talk.dialect.ActionDialect;
import smart.itsm.core.Failure;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;

public class LogHandler extends Handler {

	protected static final int MSG_ACTION = 1;
	protected static final int MSG_FAILURE = 2;

	private EditText editText;

	public LogHandler(EditText editText) {
		this.editText = editText;
	}

	@Override
	public void handleMessage (Message msg) {
		switch (msg.what) {
		case MSG_ACTION:
			ActionDialect action = (ActionDialect) msg.obj;
			this.editText.append("[I] Action: " + action.getAction() + "\n");
			this.editText.append("    N: " + action.getAction() + "\n");
			this.editText.append("    V: " + action.getParamAsInt("random") + "\n");
			break;
		case MSG_FAILURE:
			Failure failure = (Failure) msg.obj;
			this.editText.append("[F] Failed #" + failure.getCode() + " - " + failure.getDescription() + "\n");
			break;
		default:
			break;
		}
	}
}
