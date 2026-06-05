package manager;

import model.Admin;
import model.Member;
import model.TeamLeader;
import model.User;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
	private static PermissionManager instance = new PermissionManager();
	private User currentUser;

	// 프로그램에 등록된 유저 리스트
	private List<User> userList;
	
	private PermissionManager() {
		userList = DataManager.getInstance().loadUsers();
		// 관리용 Admin 계정 추가
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

		// 기존 회원 검색
        for (User u : userList) {
			if (u.getName().equals(name) && u.getPassword().equals(password)) {
				currentUser = u;
				System.out.println("로그인 성공: " + name + " (" + u.getClass().getSimpleName() + ")");
				return currentUser;
			}
		}

		// 신규 가입 (미소속(N/A)의 Member 타입)
		User newUser = new Member(name, password, "N/A");
		userList.add(newUser);

		// DataManager을 통해 userList 파일(users.txt)에 저장
		DataManager.getInstance().saveUsers(userList);

		currentUser = newUser;
		return currentUser;
	}

	// Admin이 특정 유저의 역할 및 소속 팀을 변경할 때 호출되는 메서드.
	// 클래스 타입을 직접 바꿀 수는 없으므로, 알맞은 클래스로 인스턴스를 재생성하여 교체
	public void updateUserRoleAndTeam(String targetName, String newRole, String newTeam) {
		for (int i = 0; i < userList.size(); i++) {
			User u = userList.get(i);
			if (u.getName().equals(targetName)) {
				User updatedUser;
				if (newRole.equalsIgnoreCase("ADMIN")) {
					updatedUser = new Admin(u.getName(), u.getPassword());
				} else if (newRole.equalsIgnoreCase("LEADER")) {
					updatedUser = new TeamLeader(u.getName(), u.getPassword(), newTeam);
				} else {
					updatedUser = new Member(u.getName(), u.getPassword(), newTeam);
				}

				userList.set(i, updatedUser);
				break;
			}
		}

		DataManager.getInstance().saveUsers(userList);
	}
	// 팀이 삭제될 때 해당 팀에 소속된 모든 팀원들을 미소속(N/A)로 지정하는 메서드
	public void onRemoveTeam(String teamName) {
		for (int i = 0; i < userList.size(); i++) {
			User u = userList.get(i);
			// 팀장 및 팀원은 미소속(N/A)로 초기화
			if (u.getTeamName().equals(teamName) && !u.isAdmin()) {
				userList.set(i, new Member(u.getName(), u.getPassword(), "N/A"));

				// 만약 현재 로그인한 유저 본인이 삭제된 팀 소속이었다면, 세션도 갱신
				if (currentUser != null && currentUser.getName().equals(u.getName())) {
					currentUser = userList.get(i);
				}
			}
		}
	}

	// 영입 가능한 미소속 유저 목록만 필터링하여 반환
	public List<User> getUnassignedUsers() {
		List<User> unassigned = new ArrayList<>();
		for (User u : userList) {
			// 관리자나 팀장이 아니며, 팀이 없는 유저만 필터링
			if (u.getTeamName().equals("N/A") && !u.isAdmin() && !u.isLeader()) {
				unassigned.add(u);
			}
		}
		return unassigned;
	}

	// 모든 유저의 목록 반환
	public List<User> getAllUsers() {
		return userList;
	}
}
