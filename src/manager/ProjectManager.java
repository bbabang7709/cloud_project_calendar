package manager;

import model.Project;
import model.Task;
import model.Team;

import java.util.ArrayList;
import java.util.List;

public class ProjectManager {
    private static ProjectManager instance = new ProjectManager();
    private List<Team> database;

    private ProjectManager() {
        database = new ArrayList<>();
    }

    public static ProjectManager getInstance() {
        return instance;
    }

    public List<Team> getDatabase() {
        return database;
    }

    // 프로그램 시작할 때 파일에서 데이터 싹 다 가져옴
    public void initSystemData() {
        this.database = DataManager.getInstance().loadAllData();
        System.out.println("데이터 초기화 완료!");
    }

    // 스레드가 파일 변경을 감지했을 때 호출할 메서드
    public void refreshData() {
        this.database = DataManager.getInstance().loadAllData();
    }

    public void addTeam(String teamName) {
        for (Team t : database) {
            if (t.getTeamName().equals(teamName)) {
                return;
            }
        }

        database.add(new Team(teamName));
        DataManager.getInstance().saveProjectMaster(database);
    }

    public void addProject(Team team, Project project) {
        team.addProject(project);
        DataManager.getInstance().saveProjectMaster(database);
    }

    // GUI에서 model.Task 추가 버튼 눌렀을 때 호출됨
    public void addTask(Project project, Task task) {
        project.addTask(task); // 메모리에 일단 추가
        
        // 추가한 Task의 주인(ownerName) 파일만 다시 저장시킴
        DataManager.getInstance().saveUserTasks(task.getOwnerName(), database);
    }

    // GUI에서 model.Task 삭제 버튼 눌렀을 때 호출됨
    public void removeTask(Project project, Task task) {
        project.removeTask(task);
        
        // 지워진 model.Task 주인의 파일만 다시 덮어씌움 (삭제 반영)
        DataManager.getInstance().saveUserTasks(task.getOwnerName(), database);
    }

    // GUI에서 완료 토글 버튼 눌렀을 때 호출됨
    public void toggleTaskCompletion(Task task) {
        if (task.isCompleted()) {
            task.setCompleted(false);
        } else {
            task.setCompleted(true);
        }
        
        // 상태가 바뀐 주인의 파일을 저장
        DataManager.getInstance().saveUserTasks(task.getOwnerName(), database);
    }

    public Team getTeamByName(String teamName) {
        Team targetTeam = null;
        for (Team t : database) {
            if (t.getTeamName().equals(teamName)) {
                targetTeam = t;
                break;
            }
        }

        return targetTeam;
    }
    public void simulateExternalUpdate() {
        DataManager.getInstance().triggerMockFileChange();
    }
}