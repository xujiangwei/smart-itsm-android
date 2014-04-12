package smart.itsm.core;


/**
 * 对端目标信息。
 * 
 * @author Jiangwei Xu
 *
 */
public final class Contact {

	protected String identifier;
	protected String address;
	protected int port;

	public Contact(String identifier, String address, int port) {
		this.identifier = identifier;
		this.address = address;
		this.port = port;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public String getAddress() {
		return this.address;
	}

	public int getPort() {
		return this.port;
	}
}
