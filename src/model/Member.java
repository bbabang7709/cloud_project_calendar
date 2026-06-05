package model;

public class Member extends User {

	public Member(String name, String password, String teamName) {
		super(name, password, teamName);
	}
	
	@Override
	public boolean isAdmin() {
		return false;
	}
	@Override
	public boolean isLeader() {
		return false;
	}
	@Override
	public boolean isMember() {
		return true;
	}
}
