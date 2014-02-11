package smart.itsm;

import smart.itsm.core.MastEngine;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

/**
 * 
 * 
 * @author Jiangwei Xu
 */
public class MainActivity extends Activity {

	private final String host = "127.0.0.1";
	private final int port = 7000;

	protected static final String MS = "Monitoring";
	protected static final String SD = "ServiceDesk";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (!MastEngine.getInstance().start(this.getApplication(), this.host, this.port
			, new String[]{MS, SD})) {
			// 引擎启动失败处理
			Log.e("MainActivity", "引擎启动失败！");
		}
	}

	@Override
	protected void onDestroy() {
		MastEngine.getInstance().stop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
}
