package smart.itsm;

import net.cellcloud.talk.dialect.ActionDialect;
import net.cellcloud.util.Utils;
import smart.itsm.core.Contacts;
import smart.itsm.core.MastEngine;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * 
 * 
 * @author Jiangwei Xu
 */
public class MainActivity extends Activity {

	protected static final String MS = "Monitoring";
	protected static final String SD = "ServiceDesk";

	private EditText logText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.logText = (EditText) this.getWindow().getDecorView().findViewById(R.id.edittext_log);
		Button perform = (Button) this.getWindow().getDecorView().findViewById(R.id.btn_perform);
		perform.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				performAction();
			}
		});

		this.configEngine();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		MastEngine.getInstance().stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void configEngine() {
		// 演示关联服务器目标
		
		MastEngine mast = MastEngine.getInstance();
		LogHandler handler = new LogHandler(this.logText);

		// 添加监听器
		// 用于演示的监听器，注意这里只是演示，所以混合了两个不同的 Cellet
		// 这样演示时总能看到来自 SD 这个 Cellet 的错误信息
		// 项目中应用时，建议将监听不同 Cellet 动作的监听器分开管理
		DummyListener listener = new DummyListener(handler);
		// 监听来自 Dummy 的动作
		mast.addListener("Dummy", DummyListener.ACTION_NAME, listener);
		// 监听来自 ServiceDesk 的错误
		mast.addFailureListener(SD, listener);

		// 对于不存在的 Cellet 访问或者地址和端口不正确的访问，
		// 通过加入故障监听器来获得错误信息。
		Contacts contacts = new Contacts();
		contacts.addAddress("Dummy", "192.168.2.3", 7000);
		contacts.addAddress("Monitoring", "127.0.0.1", 7000);
		contacts.addAddress("ServiceDesk", "127.0.0.1", 7000);

		this.printLog("[I] Starting the Mast ...");

		// 启动引擎
		if (!mast.start(this.getApplication(), contacts)) {
			this.printLog("[E] Failed start the Mast");
		}

		this.printLog("[I] Mast started!");
	}

	private void performAction() {
		ActionDialect action = new ActionDialect("client");
		action.setAction(DummyListener.ACTION_NAME);
		action.appendParam("random", Utils.randomInt());

		MastEngine.getInstance().performAction("Dummy", action);

		printLog("[I] Perform action: " + action.getAction());
	}

	private void printLog(final String text) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				logText.append(text + "\n");
			}
		});
	}
}
