
public class Member extends User {
	public Member(String name, String password) {
		super(name, password);
	}
	
	@Override
	public boolean isAdmin() {
		return false;
	}
}
