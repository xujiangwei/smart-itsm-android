package smart.itsm;

import net.cellcloud.talk.TalkFailureCode;
import net.cellcloud.talk.dialect.ActionDialect;
import smart.itsm.core.Failure;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class DemoHandler extends Handler {

	protected static final int MSG_ACTION = 1;
	protected static final int MSG_CONNECT = 2;
	protected static final int MSG_DISCONNECT = 3;
	protected static final int MSG_FAILURE = 5;

	private EditText mEtLog;
	private Button mBtnReset;
	private Button mBtnPerform;

	public DemoHandler(View view) {
		mEtLog = (EditText) view.findViewById(R.id.et_log);
		mBtnReset = (Button) view.findViewById(R.id.btn_reset);
		mBtnPerform = (Button) view.findViewById(R.id.btn_perform);
	}

	@Override
	public void handleMessage (Message msg) {
		switch (msg.what) {
		case MSG_ACTION:
			ActionDialect action = (ActionDialect) msg.obj;
			mEtLog.append("[I] Action: " + action.getAction() + "\n");
			mEtLog.append("    N: " + action.getAction() + "\n");
			mEtLog.append("    V: " + action.getParamAsInt("random") + "\n");
			break;
		case MSG_CONNECT:
			mEtLog.append("[I] Connect: " + msg.obj + "\n");
			mBtnPerform.setEnabled(true);
			mBtnReset.setEnabled(false);
			break;
		case MSG_DISCONNECT:
			mEtLog.append("[I] Disconnect: " + msg.obj + "\n");
			break;
		case MSG_FAILURE:
			Failure failure = (Failure) msg.obj;
			mEtLog.append("[F] Failed #" + failure.getCode().getCode() + " - " + failure.getDescription() + "\n");
			if (failure.getCode() == TalkFailureCode.NO_NETWORK) {
				mEtLog.append("[T] 无可用网络\n");
				mBtnReset.setEnabled(true);
				mBtnPerform.setEnabled(false);
			}
			else if (failure.getCode() == TalkFailureCode.NOTFOUND_CELLET) {
				mEtLog.append("[T] 未找到指定的 Cellet\n");
				mBtnReset.setEnabled(true);
				mBtnPerform.setEnabled(false);
			}
			else if (failure.getCode() == TalkFailureCode.RETRY_END) {
				mEtLog.append("[T] 达到最大重试次数，重试结束\n");
				mBtnReset.setEnabled(true);
				mBtnPerform.setEnabled(false);
			}
			else if (failure.getCode() == TalkFailureCode.CALL_FAILED) {
				mEtLog.append("[T] Call cellet 失败\n");
				mBtnPerform.setEnabled(false);
			}
			else if (failure.getCode() == TalkFailureCode.TALK_LOST) {
				mEtLog.append("[T] 丢失连接\n");
				mBtnPerform.setEnabled(false);
			}
			else {
				mEtLog.append("[T] 发现未知错误！赶快买彩票去……\n");
				mBtnPerform.setEnabled(false);
			}
			break;
		default:
			break;
		}
	}
}
