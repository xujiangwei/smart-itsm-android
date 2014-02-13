package smart.itsm.core;

/**
 * 故障监听器。
 * 
 * @author Jiangwei Xu
 *
 */
public interface FailureListener {

	/**
	 * 发生故障。
	 * @param failure
	 */
	public void onFailed(Failure failure);
}
