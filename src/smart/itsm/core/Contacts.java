package smart.itsm.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 对端目标信息。
 * 
 * @author Jiangwei Xu
 *
 */
public final class Contacts {

	private ArrayList<Address> address;

	public Contacts() {
		this.address = new ArrayList<Address>(2);
	}

	public void addAddress(String identifier, String host, int port) {
		this.address.add(new Address(identifier, host, port));
	}

	protected List<Address> getAddresses() {
		return this.address;
	}

	/**
	 * 封装的目标容器。
	 * 
	 * @author Jiangwei Xu
	 */
	protected final class Address {
		protected String identifier;
		protected String host;
		protected int port;

		protected Address(String identifier, String host, int port) {
			this.identifier = identifier;
			this.host = host;
			this.port = port;
		}
	}
}
