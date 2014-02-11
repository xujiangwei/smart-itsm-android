package smart.itsm.core;

import net.cellcloud.talk.dialect.ActionDialect;

/**
 * 动作监听器。
 * 
 * @author Jiangwei Xu
 *
 */
public interface ActionListener {

	public void onAction(ActionDialect action);
}
