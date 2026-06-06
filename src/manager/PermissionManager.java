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
    private List<User> userList;

    private PermissionManager() {
        userList = DataManager.getInstance().loadUsers();

        boolean hasAdmin = false;
        for (User u : userList) {
            if (u.getName().equals("admin") && u.isAdmin()) {
                hasAdmin = true;
                break;
            }
        }
        if (!hasAdmin) {
            userList.add(new Admin("admin", "1111"));
            DataManager.getInstance().saveUsers(userList);
        }
    }

    public static PermissionManager getInstance() {
        return instance;
    }

    public synchronized void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized List<User> getUserList() {
        return new ArrayList<>(userList);
    }

    public synchronized List<User> getUnassignedUsers() {
        List<User> unassigned = new ArrayList<>();
        for (User u : userList) {
            if (u.getTeamName().equals("N/A") && !u.isAdmin() && !u.isLeader()) {
                unassigned.add(u);
            }
        }
        return unassigned;
    }

    public synchronized void initUserDatabase(List<User> loadedUsers) {
        this.userList = loadedUsers != null ? loadedUsers : new ArrayList<>();
    }

    public synchronized User loginOrRegister(String name, String password) {
        userList = DataManager.getInstance().loadUsers();

        for (User u : userList) {
            if (u.getName().equals(name)) {
                if (u.getPassword().equals(password)) {
                    currentUser = u;
                    normalizeCurrentUserTeam();
                    System.out.println("Login success: " + name + " (" + u.getClass().getSimpleName() + ")");
                    return currentUser;
                }
                System.out.println("Login failed. Password mismatch: " + name);
                return null;
            }
        }

        System.out.println("Register new user: " + name);
        User newUser = new Member(name, password, "N/A");
        userList.add(newUser);
        DataManager.getInstance().saveUsers(userList);

        currentUser = newUser;
        return currentUser;
    }

    public synchronized void updateUserRoleAndTeam(String targetName, String newRole, String newTeam) {
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
                if (currentUser != null && currentUser.getName().equals(targetName)) {
                    currentUser = updatedUser;
                }
                break;
            }
        }

        DataManager.getInstance().saveUsers(userList);
    }

    public synchronized void onRemoveTeam(String teamName) {
        for (int i = 0; i < userList.size(); i++) {
            User u = userList.get(i);
            if (u.getTeamName().equals(teamName) && !u.isAdmin()) {
                userList.set(i, new Member(u.getName(), u.getPassword(), "N/A"));

                if (currentUser != null && currentUser.getName().equals(u.getName())) {
                    currentUser = userList.get(i);
                }
            }
        }
        DataManager.getInstance().saveUsers(userList);
    }

    private void normalizeCurrentUserTeam() {
        if (currentUser.getTeamName() == null || currentUser.getTeamName().equals("null") || currentUser.getTeamName().trim().isEmpty()) {
            currentUser.setTeamName("N/A");
        }
    }
}