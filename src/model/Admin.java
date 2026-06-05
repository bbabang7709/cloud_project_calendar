package model;

public class Admin extends User {
	public Admin(String name, String password) {

		super(name, password, "N/A");
	}
	
	@Override
	public boolean isAdmin() {
		return true;
	}
	@Override
	public boolean isLeader() {
		return false;
	}
	@Override
	public boolean isMember() {
		return false;
	}
}
