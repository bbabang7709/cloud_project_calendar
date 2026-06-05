package model;

public class TeamLeader extends User {

    public TeamLeader(String name, String password, String teamName) {
        super(name, password, teamName);
    }

    @Override
    public boolean isAdmin() {
        return false;
    }
    @Override
    public boolean isLeader() {
        return true;
    }
    @Override
    public boolean isMember() {
        return false;
    }
}
