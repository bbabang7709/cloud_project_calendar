package manager;

import model.Admin;
import model.Member;
import model.TeamLeader;
import model.User;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
	private final static PermissionManager instance = new PermissionManager();
	private User currentUser;

	// 프로그램에 등록된 유저 리스트
	private List<User> userList;

	private PermissionManager() {
		userList = DataManager.getInstance().loadUsers();
		// 관리용 Admin 계정 추가 (이미 파일에 저장되어 있다면 덮어쓰지 않음)
		boolean hasAdmin = false;
		for (User u : userList) {
			if (u.getName().equals("admin") && u.isAdmin()) {
				hasAdmin = true;
				break;
			}
		}
		if (!hasAdmin) {
			userList.add(new Admin("admin", "1111"));
		}

		DataManager.getInstance().saveUsers(userList);
	}

	public static PermissionManager getInstance() { return instance; }

	public void setCurrentUser(User user) {
		this.currentUser = user;
	}

	public User getCurrentUser() { return currentUser; }

	public List<User> getUserList() {
		return userList;
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

	public void initUserDatabase(List<User> loadedUsers) {
		this.userList = loadedUsers != null ? loadedUsers : new ArrayList<>();
	}

	public User loginOrRegister(String name, String password) {
		userList = DataManager.getInstance().loadUsers();

		// 기존 회원 검색
		for (User u : userList) {
			if (u.getName().equals(name) && u.getPassword().equals(password)) {
				currentUser = u;
				// 파일 읽기 오류 등으로 소속이 null이 된 경우 N/A로 복원
				if (currentUser.getTeamName() == null || currentUser.getTeamName().equals("null")) {
					currentUser.setTeamName("N/A");
				}
				System.out.println("로그인 성공: " + name + " (" + u.getClass().getSimpleName() + ")");
				return currentUser;
			}
		}

		// 일치하는 계정이 없는 경우 신규 가입 처리
		System.out.println("새로운 팀원 가입 진행: " + name);
		User newUser = new Member(name, password, "N/A");
		userList.add(newUser);

		DataManager.getInstance().saveUsers(userList);

		currentUser = newUser;
		return currentUser;
	}

	public void updateUserRoleAndTeam(String targetName, String newRole, String newTeam) {
		// [방어 코드] 콤보박스나 UI에서 잘못된 값이 넘어와서 null이 될 경우를 완벽 차단
		if (newTeam == null || newTeam.trim().isEmpty() || newTeam.equals("null")) {
			newTeam = "N/A";
		}

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

				// [핵심] 만약 직급/팀이 바뀐 대상이 현재 로그인한 자기 자신이라면, 세션 정보(currentUser) 즉시 교체
				if (currentUser != null && currentUser.getName().equals(targetName)) {
					currentUser = updatedUser;
				}

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
		DataManager.getInstance().saveUsers(userList);
	}
}