package smart.itsm.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 
 * @author Jiangwei Xu
 */
public final class ListenerSet {

	private HashMap<String, ArrayList<ActionListener>> listeners;

	public ListenerSet() {
		this.listeners = new HashMap<String, ArrayList<ActionListener>>(2);
	}

	public void add(String name, ActionListener listener) {
		ArrayList<ActionListener> list = this.listeners.get(name);
		if (null == list) {
			list = new ArrayList<ActionListener>(2);
			list.add(listener);
			this.listeners.put(name, list);
		}
		else {
			list.add(listener);
		}
	}

	public void remove(String name, ActionListener listener) {
		ArrayList<ActionListener> list = this.listeners.get(name);
		if (null != list) {
			list.remove(listener);
			if (list.isEmpty()) {
				this.listeners.remove(name);
			}
		}
	}

	public List<ActionListener> getListener(String name) {
		return this.listeners.get(name);
	}

	public boolean isEmpty() {
		return this.listeners.isEmpty();
	}
}
