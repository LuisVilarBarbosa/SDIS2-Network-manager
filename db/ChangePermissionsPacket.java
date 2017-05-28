package db;

import java.io.Serializable;

public class ChangePermissionsPacket implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String username;
	private boolean isAdmin;
	
	public ChangePermissionsPacket(String username, boolean isAdmin) {
		this.username = username;
		this.isAdmin = isAdmin;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean isAdmin() {
		return isAdmin;
	}

	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	
	
}
