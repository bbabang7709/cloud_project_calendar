package model;

import java.util.List;
import java.util.ArrayList;

public class Team {
	private String teamName;
	private List<Project> projects; // 팀에 속한 프로젝트 리스트
	private List<User> members; // 팀에 속한 멤버 리스트
	
	public Team(String teamName) {
		this.teamName = teamName;
		this.projects = new ArrayList<Project>();
		this.members = new ArrayList<User>();
	}
	
	public String getTeamName() { return teamName; }
	public List<Project> getProjects() { return projects; }
	public List<User> getMembers() { return members; }
	public void addProject(Project newProject) {
		projects.add(newProject);
	}
	public void addMember(User newMember) {
		members.add(newMember);
	}
	
	@Override
	public String toString() {
		return teamName;
	}
}
