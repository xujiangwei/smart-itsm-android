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
import net.cellcloud.talk.TalkFailureCode;
import net.cellcloud.talk.TalkListener;
import net.cellcloud.talk.TalkService;
import net.cellcloud.talk.TalkServiceFailure;
import net.cellcloud.talk.dialect.ActionDialect;
import android.app.Application;

/**
 * 引擎根类。
 * 
 * @author Jiangwei Xu
 */
public final class MastEngine implements TalkListener {

	private final static MastEngine instance = new MastEngine();

	// 动作监听器 Key：Cellet 标识（名称）
	private HashMap<String, ListenerSet> actionListeners;
	// 状态监听器 Key：Cellet 标识
	private HashMap<String, List<StatusListener>> statusListeners;

	private volatile boolean started;

	private MastEngine() {
		this.actionListeners = new HashMap<String, ListenerSet>(2);
		this.statusListeners = new HashMap<String, List<StatusListener>>(2);
		this.started = false;
	}

	public static MastEngine getInstance() {
		return MastEngine.instance;
	}

	/**
	 * 启动引擎。
	 * @param app
	 * @return
	 */
	public boolean start(Application app) {
		if (this.started) {
			return true;
		}

		this.started = true;

		Nucleus nucleus = Nucleus.getInstance();
		if (null == nucleus) {
			NucleusConfig config = new NucleusConfig();
			config.role = NucleusConfig.Role.CONSUMER;
			config.device = NucleusConfig.Device.PHONE;
			config.talk.block = 10240;

			try {
				nucleus = Nucleus.createInstance(config, app);
			} catch (SingletonException e) {
				nucleus = Nucleus.getInstance();
			}
		}

		// 启动 CC 内核
		if (!nucleus.startup()) {
			return false;
		}

		// 添加监听器
		nucleus.getTalkService().addListener(this);

		return true;
	}

	/**
	 * 关闭引擎。
	 */
	public void stop() {
		if (!this.started) {
			return;
		}

		Nucleus nucleus = Nucleus.getInstance();
		if (null != nucleus) {
			nucleus.getTalkService().removeListener(this);

			nucleus.shutdown();
		}

		this.started = false;
	}

	/**
	 * 引擎是否已启动。
	 * @return
	 */
	public boolean hasStarted() {
		return this.started;
	}

	/**
	 * 连接 Cellet 。
	 * @param contact
	 * @param reconnection
	 * @return
	 */
	public boolean contactCellet(Contact contact, boolean reconnection) {
		return this.contactCellet(contact, 0, 5000, reconnection);
	}

	/**
	 * 连接 Cellet 。
	 * @param contact
	 * @param retryAttempts
	 * @param retryDelay
	 * @param reconnection
	 * @return
	 */
	public boolean contactCellet(Contact contact, int retryAttempts, long retryDelay, boolean reconnection) {
		TalkService ts = Nucleus.getInstance().getTalkService();
		if (reconnection) {
			// 强制重连，先断开
			ts.hangUp(contact.identifier);
		}

		if (!reconnection && ts.isCalled(contact.identifier)) {
			// 不是强制重连，并且已经 Call 了 Cellet，则返回
			return true;
		}

		InetSocketAddress address = new InetSocketAddress(contact.address, contact.port);
		if (retryAttempts > 0) {
			TalkCapacity capacity = new TalkCapacity(retryAttempts, retryDelay);
			return ts.call(contact.identifier, address, capacity);
		}
		else {
			return ts.call(contact.identifier, address);
		}
	}

	/**
	 * 添加动作监听器。
	 * @param cellet
	 * @param name
	 * @param listener
	 */
	public void addActionListener(String cellet, ActionListener listener) {
		synchronized (this.actionListeners) {
			ListenerSet set = this.actionListeners.get(cellet);
			if (null != set) {
				set.add(listener.getAction(), listener);
			}
			else {
				set = new ListenerSet();
				set.add(listener.getAction(), listener);
				this.actionListeners.put(cellet, set);
			}
		}
	}

	/**
	 * 移除动作监听器。
	 * @param cellet
	 * @param name
	 * @param listener
	 */
	public void removeActionListener(String cellet, ActionListener listener) {
		synchronized (this.actionListeners) {
			ListenerSet set = this.actionListeners.get(cellet);
			if (null != set) {
				set.remove(listener.getAction(), listener);
				if (set.isEmpty()) {
					this.actionListeners.remove(cellet);
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

		synchronized (this.actionListeners) {
			// 分发动作
			ListenerSet set = this.actionListeners.get(identifier);
			if (null != set) {
				List<ActionListener> list = set.getListeners(action.getAction());
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

		if (failure.getCode() == TalkFailureCode.NO_NETWORK
			|| failure.getCode() == TalkFailureCode.NOTFOUND_CELLET
			|| failure.getCode() == TalkFailureCode.RETRY_END) {
			// TODO 这3个错误码，Cell Cloud 不会进行自动重连
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
