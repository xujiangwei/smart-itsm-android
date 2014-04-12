package smart.itsm.core;

import net.cellcloud.talk.dialect.ActionDialect;

/**
 * 动作监听器。
 * 
 * @author Jiangwei Xu
 *
 */
public interface ActionListener {

	/**
	 * 返回动作名。
	 * @return
	 */
	public String getAction();

	/**
	 * 当接收到动作数据时被回调。
	 * @param action
	 */
	public void onAction(ActionDialect action);
}
