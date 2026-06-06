package manager;

import model.*;

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
    private final static DataManager instance = new DataManager();

    // 파일 저장할 기본 경로 설정
    private File saveFolder = new File("scheduler_data");

    // 파일들의 마지막 변경 시간을 기록해두는 맵 (변경 감지용)
    private Map<String, Long> fileLastModifiedMap;
    private boolean isFileChanged = false; // 시뮬레이션용 플래그

    private DataManager() {
        fileLastModifiedMap = new HashMap<>();

        // 프로그램 켜질 때 폴더가 없으면 만들어줌
        File metaDir = new File(getMetaPath());
        if (!metaDir.exists()) {
            metaDir.mkdirs();
        }

        File taskDir = new File(getTaskPath());
        if (!taskDir.exists()) {
            taskDir.mkdirs();
        }
    }

    public static DataManager getInstance() {
        return instance;
    }

    private String getMetaPath() {
        return new File(saveFolder, "system_meta").getAbsolutePath();
    }

    private String getTaskPath() {
        return new File(saveFolder, "user_tasks").getAbsolutePath();
    }

    private void ensureDirectories() {
        File metaDir = new File(getMetaPath());
        if (!metaDir.exists()) {
            metaDir.mkdirs();
        }

        File taskDir = new File(getTaskPath());
        if (!taskDir.exists()) {
            taskDir.mkdirs();
        }
    }

    public synchronized void setSaveFolderPath(String path) {
        if (path != null && !path.trim().isEmpty()) {
            this.saveFolder = new File(path);
            ensureDirectories();
            fileLastModifiedMap.clear();
            System.out.println("[DataManager] 동기화 디렉토리 설정 완료: " + this.saveFolder.getAbsolutePath());
        }
    }

    public synchronized String getSaveFolderPath() {
        return this.saveFolder.getAbsolutePath();
    }

    public synchronized List<User> loadUsers() {
        List<User> list = new ArrayList<>();
        ensureDirectories();
        File file = new File(getMetaPath(), "users.txt");

        if (!file.exists()) {
            return list;
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 3) {
                    String name = parts[0];
                    String pw = parts[1];
                    String role = parts[2];

                    // [방어 코드] 텍스트 파일에 실수로 "null" 문자가 들어갔어도 강제로 "N/A"로 치환
                    String teamName = parts.length > 3 ? parts[3] : "N/A";
                    if (teamName == null || teamName.equals("null") || teamName.trim().isEmpty()) {
                        teamName = "N/A";
                    }

                    User u;
                    if (role.equals("ADMIN")) {
                        u = new Admin(name, pw); // Admin은 2개 파라미터만 받는 구조 준수
                    } else if (role.equals("LEADER")) {
                        u = new TeamLeader(name, pw, teamName);
                    } else {
                        u = new Member(name, pw, teamName);
                    }
                    list.add(u);
                }
            }
            br.close();

            fileLastModifiedMap.put(file.getAbsolutePath(), file.lastModified());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    public synchronized void saveUsers(List<User> users) {
        ensureDirectories();
        File file = new File(getMetaPath(), "users.txt");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (int i = 0; i < users.size(); i++) {
                User u = users.get(i);
                String role = "";
                if (u.isAdmin()) role = "ADMIN";
                else if (u.isLeader()) role = "LEADER";
                else role = "MEMBER";

                // [방어 코드] 파일에 쓰기 직전 널 값이나 "null" 문자열이 타는 것을 차단
                String teamName = u.getTeamName();
                if (teamName == null || teamName.equals("null") || teamName.trim().isEmpty()) {
                    teamName = "N/A";
                }

                // 관리자라 할지라도 구분자를 맞추기 위해 강제로 팀이름 텍스트(N/A 등)를 삽입
                String line = u.getName() + "|" + u.getPassword() + "|" + role + "|" + teamName;
                bw.write(line);
                bw.newLine();
            }
            bw.close();

            fileLastModifiedMap.put(file.getAbsolutePath(), file.lastModified());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<Team> loadAllData() {
        List<Team> list = new ArrayList<>();
        ensureDirectories();
        File masterFile = new File(getMetaPath(), "project_master.txt");

        if (!masterFile.exists()) {
            return list;
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(masterFile));
            String line;
            while ((line = br.readLine()) != null) {
                // 데이터 비어있는 빈 줄은 패스
                if (line.trim().isEmpty()) continue;

                // || 가 있어서 배열 뒷부분이 비더라도 길이 유지를 위해 -1 옵션 추가
                String[] parts = line.split("\\|", -1);
                String teamName = parts[0];

                Team team = null;
                for (Team t : list) {
                    if (t.getTeamName().equals(teamName)) {
                        team = t;
                        break;
                    }
                }

                if (team == null) {
                    team = new Team(teamName);
                    list.add(team);
                }

                // 프로젝트 정보가 있을 때만 추가 (비어있지 않은지 검사)
                if (parts.length >= 3 && !parts[1].trim().isEmpty()) {
                    String projectId = parts[1];
                    String projectName = parts[2];
                    Project p = new Project(projectId, projectName);
                    team.addProject(p);
                }
            }
            br.close();

            // 파일 읽기 성공 시, 현재 읽은 시점의 최종 수정 시간 기억해둠
            fileLastModifiedMap.put(masterFile.getAbsolutePath(), masterFile.lastModified());

        } catch (IOException e) {
            e.printStackTrace();
        }

        // 마스터 파일 구축이 끝난 후, 각 유저별 Task 파일들을 읽어서 마스터 파일에 반영
        loadAllUserTasks(list);

        return list;
    }

    public synchronized void saveProjectMaster(List<Team> database) {
        ensureDirectories();
        File masterFile = new File(getMetaPath(), "project_master.txt");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(masterFile));

            for (int i = 0; i < database.size(); i++) {
                Team t = database.get(i);
                // 팀만 생성하고 프로젝트는 비어있는 경우, 빈 데이터 패딩 문자열(||)로 팀 데이터 소실 방지
                if (t.getProjects().isEmpty()) {
                    bw.write(t.getTeamName() + "||");
                    bw.newLine();
                } else {
                    for (int j = 0; j < t.getProjects().size(); j++) {
                        Project p = t.getProjects().get(j);
                        // 형식: 팀명|프로젝트ID|프로젝트명
                        String line = t.getTeamName() + "|" + p.getProjectId() + "|" + p.getProjectName();
                        bw.write(line);
                        bw.newLine();
                    }
                }
            }
            bw.close();

            // 파일 쓰기 완료 후 변경 시간 맵에 강제 반영하여 감지 오작동 방지
            fileLastModifiedMap.put(masterFile.getAbsolutePath(), masterFile.lastModified());
            System.out.println("[manager.DataManager] 마스터 파일 저장 완료");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadAllUserTasks(List<Team> database) {
        File taskDir = new File(getTaskPath());
        File[] files = taskDir.listFiles((dir, name) -> name.endsWith("_tasks.txt"));

        if (files == null) return;

        for (File f : files) {
            String ownerName = f.getName().replace("_tasks.txt", "");

            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;

                    String[] parts = line.split("\\|");
                    if (parts.length < 6) continue;

                    String projectId = parts[0];
                    String title = parts[1];
                    String startDate = parts[2];
                    String deadline = parts[3];
                    boolean completed = Boolean.parseBoolean(parts[4]);

                    // 저장할 때 getRGB()로 저장한 걸 원래 Color 객체로 변환
                    int rgb = Integer.parseInt(parts[5]);
                    Color color = new Color(rgb);

                    Task task = new Task(projectId, ownerName, title, startDate, deadline, color);
                    task.setCompleted(completed);

                    // database를 뒤져서 해당 Task가 들어갈 프로젝트를 찾아 끼워넣기
                    for (Team t : database) {
                        for (Project p : t.getProjects()) {
                            if (p.getProjectId().equals(projectId)) {
                                p.addTask(task);
                            }
                        }
                    }
                }
                br.close();

                // Task 파일도 모니터링을 위해 시간 등록
                fileLastModifiedMap.put(f.getAbsolutePath(), f.lastModified());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void saveUserTasks(String ownerName, List<Team> database) {
        ensureDirectories();
        File f = new File(getTaskPath(), ownerName + "_tasks.txt");
        List<Task> userTasks = new ArrayList<>();

        // 1. 전체 목록에서 이 유저(ownerName)가 생성한 Task들만 싹 다 긁어모으기
        for (Team t : database) {
            for (Project p : t.getProjects()) {
                for (Task task : p.getTasks()) {
                    if (task.getOwnerName().equals(ownerName)) {
                        userTasks.add(task);
                    }
                }
            }
        }

        // 2. 파일에 기록하기
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            for (Task task : userTasks) {
                // 저장 형식: 프로젝트ID|제목|시작일|종료일|완료여부|색상RGB값
                String line = task.getProjectId() + "|"
                        + task.getTitle() + "|"
                        + task.getStartDate() + "|"
                        + task.getDeadline() + "|"
                        + task.isCompleted() + "|"
                        + task.getColor().getRGB();
                bw.write(line);
                bw.newLine();
            }
            bw.close();

            // 저장 직후 최신 변경 시간을 기록해서 스레드가 다시 리로드하지 않게 방어
            fileLastModifiedMap.put(f.getAbsolutePath(), f.lastModified());
            System.out.println("[manager.DataManager] " + ownerName + " 님의 Task 파일 저장 완료");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized boolean checkFileModified() {
        if (isFileChanged) {
            isFileChanged = false; // 플래그를 읽었으니 다시 초기화
            return true;
        }

        // 마스터 파일 타임스탬프 검사
        ensureDirectories();
        File masterFile = new File(getMetaPath(), "project_master.txt");
        if (masterFile.exists()) {
            Long lastTime = fileLastModifiedMap.get(masterFile.getAbsolutePath());
            if (lastTime != null && masterFile.lastModified() > lastTime) {
                return true;
            }
        }

        // Task 디렉토리 내부의 모든 파일 검사
        File taskDir = new File(getTaskPath());
        File[] files = taskDir.listFiles((dir, name) -> name.endsWith("_tasks.txt"));
        if (files != null) {
            for (File f : files) {
                Long lastTime = fileLastModifiedMap.get(f.getAbsolutePath());
                if (lastTime == null || f.lastModified() > lastTime) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void triggerFileChange() {
        this.isFileChanged = true;
    }
}
