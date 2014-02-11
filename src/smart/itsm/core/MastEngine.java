package smart.itsm.core;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Nucleus;
import net.cellcloud.core.NucleusConfig;
import net.cellcloud.exception.SingletonException;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.TalkListener;
import net.cellcloud.talk.TalkServiceFailure;
import net.cellcloud.talk.dialect.ActionDialect;
import android.app.Application;

/**
 * 
 * @author Jiangwei Xu
 */
public final class MastEngine implements TalkListener {

	private final static MastEngine instance = new MastEngine();

	// Key：Cellet 标识（名称）
	private HashMap<String, ListenerSet> listeners;

	private MastEngine() {
		this.listeners = new HashMap<String, ListenerSet>(2);
	}

	public static MastEngine getInstance() {
		return MastEngine.instance;
	}

	public boolean start(Application app, String host, int port, String[] identifiers) {
		if (null == identifiers || identifiers.length < 1) {
			return false;
		}

		NucleusConfig config = new NucleusConfig();
		config.role = NucleusConfig.Role.CONSUMER;
		config.device = NucleusConfig.Device.PHONE;
		config.talk.block = 10240;

		try {
			Nucleus nucleus = new Nucleus(config, app);

			// 启动 CC 内核
			if (!nucleus.startup()) {
				return false;
			}

			// 添加监听器
			nucleus.getTalkService().addListener(this);

			for (String identifier : identifiers) {
				InetSocketAddress address = new InetSocketAddress(host, port);
				nucleus.getTalkService().call(identifier, address);
			}
		} catch (SingletonException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	public void stop() {
		Nucleus nucleus = Nucleus.getInstance();
		if (null != nucleus) {
			nucleus.shutdown();
		}
	}

	public synchronized void addListener(String cellet, String name, ActionListener listener) {
		ListenerSet set = this.listeners.get(cellet);
		if (null != set) {
			set.add(name, listener);
		}
		else {
			set = new ListenerSet();
			set.add(name, listener);
			this.listeners.put(cellet, set);
		}
	}

	public synchronized void removeListener(String cellet, String name, ActionListener listener) {
		ListenerSet set = this.listeners.get(cellet);
		if (null != set) {
			set.remove(name, listener);
			if (set.isEmpty()) {
				this.listeners.remove(cellet);
			}
		}
	}

	@Override
	public void dialogue(String identifier, Primitive primitive) {
		if (!primitive.isDialectal()) {
			Logger.d(MastEngine.class, "原语不是方言格式");
			return;
		}

		if (!(primitive.getDialect() instanceof ActionDialect)) {
			Logger.d(MastEngine.class, "原语方言不是 ActionDialect 类型");
			return;
		}

		ActionDialect action = (ActionDialect) primitive.getDialect();

		synchronized (this.listeners) {
			// 分发动作
			ListenerSet set = this.listeners.get(identifier);
			if (null != set) {
				List<ActionListener> list = set.getListener(action.getAction());
				if (null != list) {
					for (ActionListener listener : list) {
						listener.onAction(action);
					}
				}
			}
		}
	}

	@Override
	public void contacted(String identifier, String tag) {
		// TODO
	}

	@Override
	public void quitted(String identifier, String tag) {
		// TODO
	}

	@Override
	public void failed(String identifier, String tag, TalkServiceFailure failure) {
		// TODO
	}

	@Override
	public void resumed(String identifier, String tag, long timestamp, Primitive primitive) {
		// Nothing
	}

	@Override
	public void suspended(String identifier, String tag, long timestamp, int mode) {
		// Nothing
	}
}
