
public abstract class User {
	protected String name;
	protected String password;
	
	public User(String name, String password) {
		this.name = name;
		this.password = password;
	}
	
	public String getName() {
		return name;
	}
	
	public String getPassword() {
		return password;
	}
	
	public abstract boolean isAdmin();
	
	@Override
	public String toString() {
		if (isAdmin()) {
			return name + " (관리자)";
		} else {
			return name + " (팀원)";
		}
	}
}
