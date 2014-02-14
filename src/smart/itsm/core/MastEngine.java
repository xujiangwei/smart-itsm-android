package smart.itsm.core;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Nucleus;
import net.cellcloud.core.NucleusConfig;
import net.cellcloud.exception.SingletonException;
import net.cellcloud.talk.Primitive;
import net.cellcloud.talk.TalkCapacity;
import net.cellcloud.talk.TalkListener;
import net.cellcloud.talk.TalkService;
import net.cellcloud.talk.TalkServiceFailure;
import net.cellcloud.talk.dialect.ActionDialect;
import android.app.Application;

/**
 * 
 * @author Jiangwei Xu
 */
public final class MastEngine implements TalkListener {

	private final static MastEngine instance = new MastEngine();

	// 动作监听器 Key：Cellet 标识（名称）
	private HashMap<String, ListenerSet> listeners;
	// 故障监听器 Key：Cellet 标识
	private HashMap<String, List<StatusListener>> statusListeners;

	private MastEngine() {
		this.listeners = new HashMap<String, ListenerSet>(2);
		this.statusListeners = new HashMap<String, List<StatusListener>>(2);
	}

	public static MastEngine getInstance() {
		return MastEngine.instance;
	}

	/**
	 * 启动引擎。
	 * @param app
	 * @param host
	 * @param port
	 * @param identifiers
	 * @return
	 */
	public boolean start(Application app, Contacts contacts) {
		if (contacts.getAddresses().isEmpty()) {
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

			List<Contacts.Address> list = contacts.getAddresses();
			for (Contacts.Address addr : list) {
				InetSocketAddress address = new InetSocketAddress(addr.host, addr.port);
				TalkCapacity capacity = new TalkCapacity(30, 5000);
				nucleus.getTalkService().call(addr.identifier, address, capacity);
			}
		} catch (SingletonException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/**
	 * 关闭引擎。
	 */
	public void stop() {
		Nucleus nucleus = Nucleus.getInstance();
		if (null != nucleus) {
			nucleus.getTalkService().removeListener(this);

			nucleus.shutdown();
		}
	}

	/**
	 * 添加监听器。
	 * @param cellet
	 * @param name
	 * @param listener
	 */
	public void addListener(String cellet, String name, ActionListener listener) {
		synchronized (this.listeners) {
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
	}

	/**
	 * 移除监听器。
	 * @param cellet
	 * @param name
	 * @param listener
	 */
	public void removeListener(String cellet, String name, ActionListener listener) {
		synchronized (this.listeners) {
			ListenerSet set = this.listeners.get(cellet);
			if (null != set) {
				set.remove(name, listener);
				if (set.isEmpty()) {
					this.listeners.remove(cellet);
				}
			}
		}
	}

	/**
	 * 添加状态监听器。
	 * @param cellet
	 * @param listener
	 */
	public void addStatusListener(String cellet, StatusListener listener) {
		synchronized (this.statusListeners) {
			List<StatusListener> list = this.statusListeners.get(cellet);
			if (null != list) {
				list.add(listener);
			}
			else {
				list = new ArrayList<StatusListener>(2);
				list.add(listener);
				this.statusListeners.put(cellet, list);
			}
		}
	}

	/**
	 * 删除状态监听器。
	 * @param cellet
	 * @param listener
	 */
	public void removeStatusListener(String cellet, StatusListener listener) {
		synchronized (this.statusListeners) {
			List<StatusListener> list = this.statusListeners.get(cellet);
			if (null != list) {
				list.remove(listener);
				if (list.isEmpty()) {
					this.statusListeners.remove(cellet);
				}
			}
		}
	}

	/**
	 * 执行动作。
	 * @param cellet
	 * @param action
	 */
	public boolean performAction(String identifier, ActionDialect action) {
		return TalkService.getInstance().talk(identifier, action);
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
		if (Logger.isDebugLevel()) {
			Logger.d(MastEngine.class, "contacted @" + identifier);
		}

		List<StatusListener> list = this.statusListeners.get(identifier);
		if (null != list) {
			for (StatusListener listener : list) {
				listener.onConnected(identifier);
			}
		}
	}

	@Override
	public void quitted(String identifier, String tag) {
		if (Logger.isDebugLevel()) {
			Logger.d(MastEngine.class, "quitted @" + identifier);
		}

		List<StatusListener> list = this.statusListeners.get(identifier);
		if (null != list) {
			for (StatusListener listener : list) {
				listener.onDisconnected(identifier);
			}
		}
	}

	@Override
	public void failed(String identifier, String tag, TalkServiceFailure failure) {
		if (Logger.isDebugLevel()) {
			Logger.d(MastEngine.class, "failed @" + identifier);
		}

		List<StatusListener> list = this.statusListeners.get(identifier);
		if (null != list) {
			Failure f = new Failure(failure);
			for (StatusListener listener : list) {
				listener.onFailed(identifier, f);
			}
		}
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
