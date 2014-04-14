package smart.itsm.core;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * 
 * @author Jiangwei Xu
 */
public final class MastEngine implements TalkListener {

	private final static MastEngine instance = new MastEngine();

	// 动作监听器 Key：Cellet 标识（名称）
	private HashMap<String, ListenerSet> listeners;
	// 状态监听器 Key：Cellet 标识
	private HashMap<String, List<StatusListener>> statusListeners;

	// 通信地址表
	private HashMap<String, Contact> contacts;

	private MastEngine() {
		this.listeners = new HashMap<String, ListenerSet>(2);
		this.statusListeners = new HashMap<String, List<StatusListener>>(2);
		this.contacts = new HashMap<String, Contact>(2);
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
		Nucleus nucleus = Nucleus.getInstance();
		if (null != nucleus) {
			nucleus.getTalkService().removeListener(this);

			nucleus.shutdown();
		}
	}

	/**
	 * 添加服务器联系人。
	 * @param contact
	 */
	public void addContact(Contact contact) {
		this.contacts.put(contact.identifier, contact);
	}

	/**
	 * 删除服务器联系人。
	 * @param contact
	 */
	public void removeContact(Contact contact) {
		this.contacts.remove(contact.identifier);
	}

	/**
	 * 重置联系服务器。
	 */
	public void resetContacts() {
		Set<Map.Entry<String, Contact>> list = this.contacts.entrySet();
		for (Map.Entry<String, Contact> e : list) {
			Contact contact = e.getValue();
			Nucleus.getInstance().getTalkService().hangUp(contact.identifier);
		}

		list = this.contacts.entrySet();
		for (Map.Entry<String, Contact> e : list) {
			Contact contact = e.getValue();
			InetSocketAddress address = new InetSocketAddress(contact.address, contact.port);
			// 设置自动重试次数和重试间隔
			TalkCapacity capacity = new TalkCapacity(3, 5000);
			Nucleus.getInstance().getTalkService().call(contact.identifier, address, capacity);
		}
	}

	/**
	 * 添加监听器。
	 * @param cellet
	 * @param name
	 * @param listener
	 */
	public void addListener(String cellet, ActionListener listener) {
		synchronized (this.listeners) {
			ListenerSet set = this.listeners.get(cellet);
			if (null != set) {
				set.add(listener.getAction(), listener);
			}
			else {
				set = new ListenerSet();
				set.add(listener.getAction(), listener);
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
	public void removeListener(String cellet, ActionListener listener) {
		synchronized (this.listeners) {
			ListenerSet set = this.listeners.get(cellet);
			if (null != set) {
				set.remove(listener.getAction(), listener);
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
