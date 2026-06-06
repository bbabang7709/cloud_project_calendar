package manager;

import model.Project;
import model.Task;
import model.Team;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectManager {
    private final static ProjectManager instance = new ProjectManager();
    private List<Team> database;

    private ProjectManager() {
        database = new ArrayList<>();
    }

    public static ProjectManager getInstance() {
        return instance;
    }

    public synchronized List<Team> getDatabase() {
        return new ArrayList<>(database);
    }

    public synchronized List<Team> getDatabaseSnapshot() {
        List<Team> snapshot = new ArrayList<>();
        for (Team team : database) {
            Team copiedTeam = new Team(team.getTeamName());
            for (Project project : team.getProjects()) {
                Project copiedProject = new Project(project.getProjectId(), project.getProjectName());
                for (Task task : project.getTasks()) {
                    copiedProject.addTask(task);
                }
                copiedTeam.addProject(copiedProject);
            }
            snapshot.add(copiedTeam);
        }
        return snapshot;
    }

    public synchronized void initSystemData() {
        this.database = DataManager.getInstance().loadAllData();
        System.out.println("Data init completed!");
    }

    public synchronized void refreshData() {
        this.database = DataManager.getInstance().loadAllData();
    }

    public synchronized void addTeam(String teamName) {
        for (Team t : database) {
            if (t.getTeamName().equals(teamName)) {
                return;
            }
        }

        database.add(new Team(teamName));
        DataManager.getInstance().saveProjectMaster(database);
    }

    public synchronized void removeTeam(Team team) {
        Set<String> owners = collectOwners(team);
        if (database.remove(team)) {
            DataManager.getInstance().saveProjectMaster(database);
            saveOwners(owners);
        }
    }

    public synchronized void addProject(Team team, Project project) {
        team.addProject(project);
        DataManager.getInstance().saveProjectMaster(database);
    }

    public synchronized void removeProject(Team team, Project project) {
        Set<String> owners = collectOwners(project);
        if (team.getProjects().remove(project)) {
            DataManager.getInstance().saveProjectMaster(database);
            saveOwners(owners);
        }
    }

    public synchronized void addTask(Project project, Task task) {
        project.addTask(task);
        DataManager.getInstance().saveUserTasks(task.getOwnerName(), database);
    }

    public synchronized void removeTask(Project project, Task task) {
        if (project.getTasks().remove(task)) {
            DataManager.getInstance().saveUserTasks(task.getOwnerName(), database);
        }
    }

    public synchronized void toggleTaskCompletion(Task task) {
        task.setCompleted(!task.isCompleted());
        DataManager.getInstance().saveUserTasks(task.getOwnerName(), database);
    }

    public synchronized Team getTeamByName(String teamName) {
        for (Team t : database) {
            if (t.getTeamName().equals(teamName)) {
                return t;
            }
        }
        return null;
    }

    public void simulateExternalUpdate() {
        DataManager.getInstance().triggerFileChange();
    }

    private Set<String> collectOwners(Team team) {
        Set<String> owners = new HashSet<>();
        if (team == null) return owners;

        for (Project project : team.getProjects()) {
            owners.addAll(collectOwners(project));
        }
        return owners;
    }

    private Set<String> collectOwners(Project project) {
        Set<String> owners = new HashSet<>();
        if (project == null) return owners;

        for (Task task : project.getTasks()) {
            owners.add(task.getOwnerName());
        }
        return owners;
    }

    private void saveOwners(Set<String> owners) {
        for (String owner : owners) {
            DataManager.getInstance().saveUserTasks(owner, database);
        }
    }
}