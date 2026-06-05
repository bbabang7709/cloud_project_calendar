package model;

public abstract class User {
	protected String name;
	protected String password;
	protected String teamName;
	
	public User(String name, String password, String teamName) {
		this.name = name;
		this.password = password;
		this.teamName = teamName;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPassword() {
		return password;
	}

	public String getTeamName() {
		return teamName;
	}

	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}

	// 역할 구분용 메서드
	public abstract boolean isAdmin();
	public abstract boolean isLeader();
	public abstract boolean isMember();
	
	@Override
	public String toString() {
		String role;
		if (isAdmin()) {
			role = "관리자";
		} else if (isLeader()) {
			role = "팀장";
		} else {
			role = "팀원";
		}
		return name + " (" + role + " - " + teamName + ")";
	}
}