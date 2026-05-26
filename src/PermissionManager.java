import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
	private static PermissionManager instance = new PermissionManager();
	private User currentUser;
	
	private List<User> userList;
	
	private PermissionManager() {
		userList = new ArrayList<>();
		// 테스트용 임시 User
		userList.add(new Admin("김팀장", "1234"));
	}
	
	public static PermissionManager getInstance() { return instance; }
	
	public void setCurrentUser(User user) {
		this.currentUser = user;
	}
	
	public User getCurrentUser() { return currentUser; }
	
	public boolean hasAdminAccess() {
		if (currentUser != null && currentUser.isAdmin()) {
			return true;
		}
		
		return false;
	}
	
	public User loginOrRegister(String name, String password) {
		for (int i = 0; i < userList.size(); i++) {
			User u = userList.get(i);
			if (u.getName().equals(name)) {
				if (u.getPassword().equals(password)) {
					return u;
				} else {
					return null;
				}
			}
		}
		
		System.out.println("등록되지 않은 사용자입니다. 새로운 멤버로 등록합니다: " + name);
		User newUser = new Member(name, password);
		userList.add(newUser);
		
		return newUser;
	}
}
