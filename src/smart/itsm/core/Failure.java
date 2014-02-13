package smart.itsm.core;

import net.cellcloud.talk.TalkServiceFailure;

/**
 * 故障。
 * 
 * @author Jiangwei Xu
 *
 */
public class Failure {

	private int code;
	private String description;
	private String reason;

	public Failure(TalkServiceFailure tsf) {
		this.code = tsf.getCode();
		this.description = tsf.getDescription();
		this.reason = tsf.getReason();
	}

	public int getCode() {
		return this.code;
	}

	public String getDescription() {
		return this.description;
	}

	public String getReason() {
		return this.reason;
	}
}
