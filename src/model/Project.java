package model;

import java.util.List;
import java.util.ArrayList;

public class Project {
	private String projectId;
	private String projectName;
	private List<Task> tasks; // 해당 프로젝트에 속한 model.Task 리스트
	
	public Project(String projectId, String projectName) {
		this.projectId = projectId;
		this.projectName = projectName;
		this.tasks = new ArrayList<Task>();
	}
	
	public String getProjectId() { return projectId; }
	public String getProjectName() { return projectName; }
	public List<Task> getTasks() { return tasks; }
	public void addTask(Task newTask) {
		tasks.add(newTask);
	}
	
	public void removeTask(Task targetTask) {
		tasks.remove(targetTask);
	}
	
	@Override
	public String toString() {
		return projectName;
	}
}
