
public class Admin extends User {
	public Admin(String name, String password) {
		super(name, password);
	}
	
	@Override
	public boolean isAdmin() {
		return true;
	}
}
