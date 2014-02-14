package smart.itsm.core;

/**
 * 故障监听器。
 * 
 * @author Jiangwei Xu
 *
 */
public interface StatusListener {

	/**
	 * 连接建立。
	 * @param identifier
	 */
	public void onConnected(String identifier);

	/**
	 * 连接断开。
	 * @param identifier
	 */
	public void onDisconnected(String identifier);

	/**
	 * 发生故障。
	 * @param failure
	 */
	public void onFailed(String identifier, Failure failure);
}
