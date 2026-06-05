package manager;

import model.*;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
    private static DataManager instance = new DataManager();
    
    // 파일 저장할 기본 경로 설정
    private final String BASE_PATH = "G:/내 드라이브/scheduler_data";
    private final String META_PATH = BASE_PATH + "/system_meta";
    private final String TASK_PATH = BASE_PATH + "/user_tasks";

    // 파일들의 마지막 변경 시간을 기록해두는 맵 (변경 감지용)
    private Map<String, Long> fileLastModifiedMap;
    private boolean isFileChanged = false; // 시뮬레이션용 플래그

    private DataManager() {
        fileLastModifiedMap = new HashMap<>();

        // 프로그램 켜질 때 폴더가 없으면 만들어줌
        File metaDir = new File(META_PATH);
        if (!metaDir.exists()) {
            metaDir.mkdirs();
        }
        
        File taskDir = new File(TASK_PATH);
        if (!taskDir.exists()) {
            taskDir.mkdirs();
        }

        // 초기 유저 리스트 파일 생성
        initDefaultUsersFile();
        // 초기 파일들의 수정 시간 기록
        recordFileTimestamps();
    }

    public static DataManager getInstance() {
        return instance;
    }

    // 현재 폴더에 존재하는 파일들의 수정 시간을 기록해두는 헬퍼 메서드
    private void recordFileTimestamps() {
        fileLastModifiedMap.clear(); // 맵을 한 번 청소하고 다시 기록

        // 마스터 파일 기록
        File masterFile = new File(META_PATH + "/project_master.txt");
        if (masterFile.exists()) {
            fileLastModifiedMap.put(masterFile.getAbsolutePath(), masterFile.lastModified());
        }

        // 유저 목록 파일 기록
        File usersListFile = new File(META_PATH + "/users.txt");
        if (usersListFile.exists()) {
            fileLastModifiedMap.put(usersListFile.getAbsolutePath(), usersListFile.lastModified());
        }

        // 개별 태스크 파일들 기록
        File taskFolder = new File(TASK_PATH);
        File[] userFiles = taskFolder.listFiles();
        if (userFiles != null) {
            for (File file : userFiles) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    fileLastModifiedMap.put(file.getAbsolutePath(), file.lastModified());
                }
            }
        }
    }

    private void initDefaultUsersFile() {
        File userListFile = new File(META_PATH + "/users.txt");
        if (!userListFile.exists()) {
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(userListFile));
                bw.write("admin|1111|ADMIN|N/A");
                bw.newLine();
                bw.close();
                System.out.println("[manager.DataManager] 초기 사용자 리스트 파일(users.txt) 생성 완료");
            } catch (IOException e) {
                System.out.println("사용자 리스트 파일 생성 실패: " + e.getMessage());
            }
        }
    }

    // 폴더 내의 파일들을 훑어보며 마지막으로 읽은 시점보다 수정 시간이 변한 파일이 있는지 감시합니다.
    public boolean checkFileModified() {
        // 수동 동기화 버튼을 누른 경우 즉시 반영
        if (isFileChanged) {
            isFileChanged = false;
            recordFileTimestamps(); // 기준 시간 최신화
            return true;
        }

        // 실제 파일들의 물리적인 최종 수정 시간 변경 확인
        // 1. 마스터 파일 확인
        File masterFile = new File(META_PATH + "/project_master.txt");
        if (masterFile.exists()) {
            String path = masterFile.getAbsolutePath();
            long currentModified = masterFile.lastModified();
            if (!fileLastModifiedMap.containsKey(path) || fileLastModifiedMap.get(path) != currentModified) {
                recordFileTimestamps();
                return true;
            }
        }

        // 2. 유저 리스트 파일 수정 감지
        File userListFile = new File(META_PATH + "/users.txt");
        if (userListFile.exists()) {
            String path = userListFile.getAbsolutePath();
            long currentModified = userListFile.lastModified();
            if (!fileLastModifiedMap.containsKey(path) || fileLastModifiedMap.get(path) != currentModified) {
                recordFileTimestamps();
                return true;
            }
        }

        // 3. 유저 태스크 폴더 내 파일 확인
        File taskFolder = new File(TASK_PATH);
        File[] userFiles = taskFolder.listFiles();
        if (userFiles != null) {
            // 파일 개수 자체가 변했는지(새로운 유저 파일 추가 등) 확인
            int trackedCount = 0;
            // 윈도우 환경과 리눅스 환경의 경로 슬래시 구분자 통일을 위해 File 객체로 직접 비교 준비
            for (String key : fileLastModifiedMap.keySet()) {
                File trackedFile = new File(key);
                if (trackedFile.getParentFile() != null && trackedFile.getParentFile().getName().equals("user_tasks")) {
                    trackedCount++;
                }
            }
            
            int actualCount = 0;
            for (File file : userFiles) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    actualCount++;
                }
            }

            // 파일의 개수가 달라졌으면 동기화 필요
            if (trackedCount != actualCount) {
                recordFileTimestamps();
                return true;
            }

            // 개별 파일의 수정 시간 변화 확인
            for (File file : userFiles) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    String path = file.getAbsolutePath();
                    long currentModified = file.lastModified();

                    if (!fileLastModifiedMap.containsKey(path) || fileLastModifiedMap.get(path) != currentModified) {
                        recordFileTimestamps();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // 수동 동기화 변경용 메서드
    public void triggerMockFileChange() {
        this.isFileChanged = true;
    }

    public List<User> loadUsers() {
        List<User> list = new ArrayList<>();
        File userListFile = new File(META_PATH + "/users.txt");
        if (userListFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(userListFile));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|", -1);
                    if (parts.length >= 4) {
                        String name = parts[0];
                        String pw = parts[1];
                        String role = parts[2];
                        String teamName = parts[3];

                        User u;
                        if (role.equalsIgnoreCase("ADMIN")) {
                            u = new Admin(name, pw);
                        } else if (role.equalsIgnoreCase("LEADER")) {
                            u = new TeamLeader(name, pw, teamName);
                        } else {
                            u = new Member(name, pw, teamName);
                        }
                        list.add(u);
                    }
                }
                br.close();
            } catch (Exception e) {
                System.out.println("사용자 정보 로드 실패: " + e.getMessage());
            }
        }
        return list;
    }

    public void saveUsers(List<User> userList) {
        File userListFile = new File(META_PATH + "/users.txt");

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(userListFile));
            for (User u : userList) {
                String role;
                if (u.isAdmin()) {
                    role = "ADMIN";
                } else if (u.isLeader()) {
                    role = "LEADER";
                } else {
                    role = "MEMBER";
                }
                bw.write(u.getName() + "|" + u.getPassword() + "|" + role + "|" + u.getTeamName());
                bw.newLine();
            }
            bw.close();
            fileLastModifiedMap.put(userListFile.getAbsolutePath(), userListFile.lastModified());
            System.out.println("[manager.DataManager] 사용자 리스트 파일 저장 완료");
        } catch (IOException e) {
            System.out.println("사용자 리스트 파일 저장 에러: " + e.getMessage());
        }
    }

    // 전체 데이터 로드하는 메서드 (마스터 파일 읽고 -> 유저 파일 읽음)
    public List<Team> loadAllData() {
        List<Team> database = new ArrayList<>();
        
        // 1. 프로젝트 마스터 파일 읽기 (뼈대 만들기)
        File masterFile = new File(META_PATH + "/project_master.txt");
        if (masterFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(masterFile));
                String line;
                while ((line = br.readLine()) != null) {
                    // 팀명|프로젝트ID|프로젝트명 형식
                    String[] parts = line.split("\\|", -1);

                    if (parts.length >= 1) {
                        String teamName = parts[0];
                        if (teamName.trim().isEmpty()) continue;

                        Team targetTeam = null;
                        for (int i = 0; i < database.size(); i++) {
                            Team t = database.get(i);
                            if (t.getTeamName().equals(teamName)) {
                                targetTeam = t;
                                break;
                            }
                        }

                        if (targetTeam == null) {
                            targetTeam = new Team(teamName);
                            database.add(targetTeam);
                        }

                        if (parts.length >= 3 && !parts[1].trim().isEmpty()) {
                            String pId = parts[1];
                            String pName = parts[2];
                            targetTeam.addProject(new Project(pId, pName));
                        }
                    }
                }
                br.close();
            } catch (IOException e) {
                System.out.println("마스터 파일 읽기 에러!");
                e.printStackTrace();
            }
        }

        // 2. 유저별 태스크 파일 읽기 (살 붙이기)
        File taskFolder = new File(TASK_PATH);
        File[] userFiles = taskFolder.listFiles();
        if (userFiles != null) {
            for (int i = 0; i < userFiles.length; i++) {
                File userFile = userFiles[i];
                if (userFile.isFile() && userFile.getName().endsWith(".txt")) {
                    String ownerName = userFile.getName().replace(".txt", "");
                    readUserTaskFile(userFile, ownerName, database);
                }
            }
        }

        // 로드 완료 후 다시 타임스탬프 현황을 동기화 기록
        recordFileTimestamps();
        return database;
    }

    private void readUserTaskFile(File file, String ownerName, List<Team> database) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                // 프로젝트ID|제목|시작일|종료일|완료여부|색상RGB
                String[] parts = line.split("\\|");
                if (parts.length >= 6) {
                    String pId = parts[0];
                    String title = parts[1];
                    String start = parts[2];
                    String end = parts[3];
                    boolean isCompleted = Boolean.parseBoolean(parts[4]);
                    Color color = new Color(Integer.parseInt(parts[5]));

                    Task task = new Task(pId, ownerName, title, start, end, color);
                    task.setCompleted(isCompleted);

                    for (int i = 0; i < database.size(); i++) {
                        Team t = database.get(i);
                        for (int j = 0; j < t.getProjects().size(); j++) {
                            Project p = t.getProjects().get(j);
                            if (p.getProjectId().equals(pId)) {
                                p.addTask(task);
                            }
                        }
                    }
                }
            }
            br.close();
        } catch (Exception e) {
            System.out.println(ownerName + " 파일 읽기 실패");
        }
    }

    // 특정 유저의 Task들만 모아서 해당 유저 이름의 txt 파일에 덮어쓰기
    public void saveUserTasks(String targetOwnerName, List<Team> database) {
        File file = new File(TASK_PATH + "/" + targetOwnerName + ".txt");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            
            for (int i = 0; i < database.size(); i++) {
                Team t = database.get(i);
                for (int j = 0; j < t.getProjects().size(); j++) {
                    Project p = t.getProjects().get(j);
                    for (int k = 0; k < p.getTasks().size(); k++) {
                        Task task = p.getTasks().get(k);
                        if (task.getOwnerName().equals(targetOwnerName)) {
                            String line = task.getProjectId() + "|" + 
                                          task.getTitle() + "|" + 
                                          task.getStartDate() + "|" + 
                                          task.getDeadline() + "|" + 
                                          task.isCompleted() + "|" + 
                                          task.getColor().getRGB();
                            bw.write(line);
                            bw.newLine();
                        }
                    }
                }
            }
            bw.close();
            
            // 파일 쓰기 완료 후 변경 시간 맵에 강제 반영하여 자가 리프레시 방지
            fileLastModifiedMap.put(file.getAbsolutePath(), file.lastModified());
        } catch (IOException e) {
            System.out.println("파일 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 관리자가 전체 프로젝트 구조(새로운 팀 추가, 새로운 프로젝트 생성 등)를 조작했을 때
     * system_meta/project_master.txt 파일을 덮어쓰기 저장하는 메서드.
     */
    public void saveProjectMaster(List<Team> database) {
        File masterFile = new File(META_PATH + "/project_master.txt");
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
            System.out.println("[manager.DataManager] 프로젝트 마스터 파일 변경 완료 및 동기화 셋업!");
        } catch (IOException e) {
            System.out.println("프로젝트 마스터 파일 저장 실패: " + e.getMessage());
        }
    }
}