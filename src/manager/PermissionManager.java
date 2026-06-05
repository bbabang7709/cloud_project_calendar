package manager;

import model.Admin;
import model.Member;
import model.TeamLeader;
import model.User;

import java.util.List;

public class PermissionManager {
	private static PermissionManager instance = new PermissionManager();
	private User currentUser;

	// 프로그램에 등록된 유저 리스트
	private List<User> userList;
	
	private PermissionManager() {
		userList = DataManager.getInstance().loadUsers();
		// 관리용 model.Admin 추가
		userList.add(new Admin("admin", "1111"));
	}
	
	public static PermissionManager getInstance() { return instance; }
	
	public void setCurrentUser(User user) {
		this.currentUser = user;
	}
	
	public User getCurrentUser() { return currentUser; }
	
	public boolean hasAdminAccess() {
		return currentUser != null && currentUser.isAdmin();
	}

	public List<User> getUserList() {
		return userList;
	}

	public void saveUserDatabase() {
		DataManager.getInstance().saveUsers(userList);
	}
	
	public User loginOrRegister(String name, String password) {
		userList = DataManager.getInstance().loadUsers();

        for (User u : userList) {
			if (u.getName().equals(name)) {
				if (u.getPassword().equals(password)) {
					return u;
				} else {
					return null;
				}
			}
        }

		System.out.println("새로운 팀원 가입 진행: " + name);

		User newUser = new Member(name, password, "N/A");
		userList.add(newUser);

		saveUserDatabase();

		return newUser;
	}

	// Admin이 특정 유저의 역할 및 소속 팀을 변경할 때 호출되는 메서드.
	// 클래스 타입을 직접 바꿀 수는 없으므로, 알맞은 클래스로 인스턴스를 재생성하여 교체
	public void changeUserRoleAndTeam(String targetName, String newRole, String newTeamName) {
		userList = DataManager.getInstance().loadUsers();

		for (int i = 0; i < userList.size(); i++) {
			User u = userList.get(i);
			if (u.getName().equals(targetName)) {
				User updateUser;
				if (newRole.equalsIgnoreCase("LEADER")) {
					updateUser = new TeamLeader(u.getName(), u.getPassword(), newTeamName);
				} else if (newRole.equalsIgnoreCase("ADMIN")) {
					updateUser = new Admin(u.getName(), u.getPassword());
				} else {
					updateUser = new Member(u.getName(), u.getPassword(), newTeamName);
				}
				userList.set(i, updateUser);
				break;
			}
		}

		saveUserDatabase();
	}
}
