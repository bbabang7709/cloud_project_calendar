package gui;

import model.Project;
import model.Task;
import model.Team;
import model.User;

import manager.ProjectManager;
import manager.PermissionManager;

import thread.CloudSyncThread;
import thread.NotificationThread;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public class MainFrame extends JFrame {
    private TeamPanel teamPanel;
    private ProjectPanel projectPanel;
    private TaskPanel taskPanel;
    private CalendarPanel calendarPanel;

    // 백그라운드 작동용 멀티 스레드 쌍
    private CloudSyncThread syncThread;
    private NotificationThread alarmThread; // 유저 스레드명에 맞게 매핑

    // [권한 제어용] 일반 팀원 로그인 시 본인 소속 팀을 저장하는 변수
    private String memberTeamName;

    public MainFrame() {
        User currentUser = PermissionManager.getInstance().getCurrentUser();
        memberTeamName = currentUser.getTeamName();

        // 관리자가 아닌데 미소속인 경우 로그인 시 대기 안내 팝업 출력
        if (!currentUser.isAdmin() && currentUser.getTeamName().equals("N/A")) {
            JOptionPane.showMessageDialog(null,
                    "현재 소속된 팀이 없습니다.\n관리자 또는 팀장의 팀 배치를 기다려주세요.",
                    "대기 발령 상태", JOptionPane.INFORMATION_MESSAGE);
        }

        String titlePrefix = currentUser.isAdmin() ? "[관리자] " :
                             currentUser.isLeader() ? "[" + currentUser.getTeamName() + "] " :
                             currentUser.getTeamName().equals("N/A") ? "" :
                             "[" + memberTeamName + " 팀원] ";

        setTitle("클라우드 캘린더 - " + titlePrefix + currentUser.getName());
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBorder(new EmptyBorder(5, 5, 5, 5));

        // ----------------- [탭 1: 달력 뷰 패널 부착] -----------------
        calendarPanel = new CalendarPanel();
        tabbedPane.addTab("  📅 스마트 캘린더 뷰  ", calendarPanel);

        // ----------------- [탭 2: 데이터 관리 패널 부착] -----------------
        JPanel managementTab = new JPanel(new BorderLayout(15, 15));
        managementTab.setBackground(Color.WHITE);
        managementTab.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 상단 컨테이너 (관리자 툴바 + 팀/프로젝트 리스트)
        JPanel northContainer = new JPanel(new BorderLayout(0, 10));
        northContainer.setBackground(Color.WHITE);

        // 관리자 & 팀장 전용 조작 패널
        if (currentUser.isAdmin() || currentUser.isLeader()) {
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            controlPanel.setBackground(Color.WHITE);
            String borderTitle = currentUser.isAdmin() ? "관리자 전용" :
                    "[" + currentUser.getTeamName() + "] 팀장 프로젝트 관리";
            controlPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), borderTitle));

            // 관리자 전용: 팀 추가
            if (currentUser.isAdmin()) {
                JButton btnAddTeam = createStyledButton("팀 추가 (+)", new Color(255, 140, 0));
                JButton btnRemoveTeam = createStyledButton("팀 삭제 (-)", new Color(220, 20, 60));

                btnAddTeam.addActionListener(e -> {
                    String newTeam = JOptionPane.showInputDialog(this, "신규 팀 이름을 입력하세요.");
                    if (newTeam != null && !newTeam.trim().isEmpty()) {
                        ProjectManager.getInstance().addTeam(newTeam.trim());
                        refreshAllViews();
                    }
                });
                btnRemoveTeam.addActionListener(e -> {
                   Team targetTeam = teamPanel.getSelectedTeam();
                   if (targetTeam == null) {
                       JOptionPane.showMessageDialog(this, "삭제할 팀을 선택하세요.", "안내", JOptionPane.WARNING_MESSAGE);
                       return;
                   }
                    int confirm = JOptionPane.showConfirmDialog(this,
                            "정말 [" + targetTeam.getTeamName() + "] 팀을 삭제하시겠습니까?\n이 작업은 되돌릴 수 없으며, 소속 팀원들은 모두 '미소속'으로 강등됩니다.",
                            "팀 삭제 경고", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);

                    if (confirm == JOptionPane.YES_OPTION) {
                        ProjectManager.getInstance().removeTeam(targetTeam);
                        PermissionManager.getInstance().onRemoveTeam(targetTeam.getTeamName()); // 뿔뿔이 흩어짐
                        refreshAllViews();
                    }
                });

                controlPanel.add(btnAddTeam);
                controlPanel.add(btnRemoveTeam);

                controlPanel.add(new JLabel("  |  "));
            }

            // 관리자 + 팀장 : 프로젝트 추가 버튼
            JButton btnAddProject = createStyledButton("프로젝트 추가 (+)", new Color(255, 140, 0));
            JButton btnRemoveProject = createStyledButton("프로젝트 삭제 (-)", new Color(220, 20, 60));

            btnAddProject.addActionListener(e -> {
                Team targetTeam = currentUser.isAdmin() ? teamPanel.getSelectedTeam() : ProjectManager.getInstance().getTeamByName(currentUser.getTeamName());
                if (targetTeam == null) {
                    JOptionPane.showMessageDialog(this, "프로젝트를 추가할 '팀'을 먼저 선택하세요.");
                    return;
                }
                String newProject = JOptionPane.showInputDialog(this, targetTeam.getTeamName() + "에 추가할 신규 프로젝트명:");
                if (newProject != null && !newProject.trim().isEmpty()) {
                    String pId = "P_" + UUID.randomUUID().toString().substring(0, 5);
                    ProjectManager.getInstance().addProject(targetTeam, new Project(pId, newProject.trim()));
                    refreshAllViews();
                }
            });

            btnRemoveProject.addActionListener(e -> {
                Team targetTeam = currentUser.isAdmin() ? teamPanel.getSelectedTeam() : ProjectManager.getInstance().getTeamByName(currentUser.getTeamName());
                Project targetProject = projectPanel.getSelectedProject();

                if (targetTeam == null || targetProject == null) {
                    JOptionPane.showMessageDialog(this, "삭제할 프로젝트를 선택하세요.", "안내", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(this,
                        "정말 [" + targetProject.getProjectName() + "] 프로젝트를 삭제하시겠습니까?",
                        "프로젝트 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

                if (confirm == JOptionPane.YES_OPTION) {
                    ProjectManager.getInstance().removeProject(targetTeam, targetProject);
                    refreshAllViews();
                }
            });
            controlPanel.add(btnAddProject);
            controlPanel.add(btnRemoveProject);
            northContainer.add(controlPanel, BorderLayout.NORTH);
        }

        // 상단에 팀 목록 + 프로젝트 목록 배치
        JPanel topSplitPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        topSplitPanel.setBackground(Color.WHITE);
        topSplitPanel.setPreferredSize(new Dimension(0, 200));

        teamPanel = new TeamPanel();
        projectPanel = new ProjectPanel();
        topSplitPanel.add(teamPanel);
        topSplitPanel.add(projectPanel);

        northContainer.add(topSplitPanel, BorderLayout.CENTER);
        managementTab.add(northContainer, BorderLayout.NORTH);

        // 하단에 Task 목록 배치
        taskPanel = new TaskPanel();
        managementTab.add(taskPanel, BorderLayout.CENTER);

        // 최하단 Task 목록 관리 버튼 배치
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnAdd = createStyledButton("할 일(Task) 추가", new Color(70, 130, 180));
        JButton btnToggle = createStyledButton("할 일 상태 갱신", new Color(60, 179, 113));
        JButton btnDelete = createStyledButton("할 일(Task) 삭제", new Color(220, 20, 60));
        JButton btnSimulateSync = createStyledButton("수동 동기화", Color.DARK_GRAY);

        bottomPanel.add(btnSimulateSync);
        bottomPanel.add(btnAdd);
        bottomPanel.add(btnToggle);
        bottomPanel.add(btnDelete);
        managementTab.add(bottomPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("  📁 프로젝트 데이터 관리  ", managementTab);

        // ----------------- [탭 3: 권한별 특수 관리 탭] -----------------
        if (currentUser.isAdmin()) {
            tabbedPane.addTab("👥 사용자 관리", new UserManagementPanel());
        } else if (currentUser.isLeader()) {
            tabbedPane.addTab("👥 팀원 관리", new TeamLeaderPanel());
        }

        add(tabbedPane, BorderLayout.CENTER);

        // 이벤트 리스터 부착
        setupEventHandlers(btnAdd, btnToggle, btnDelete, btnSimulateSync);

        // 초기 데이터 리프레쉬
        refreshAllViews();

        // ----------------- [멀티 스레드 가동 개시] -----------------
        syncThread = new CloudSyncThread(this);
        syncThread.start();

        // 사용자 환경에 맞게 Alarm 스레드 가동 방어 코드
        try {
            alarmThread = new NotificationThread();
            alarmThread.start();
        } catch (Exception ex) {
            System.out.println("알람 스레드가 비활성화되어 생략합니다.");
        }
    }

    // 권한 확인 메서드
    private boolean hasPermissionForSelectedTeam() {
        User currentUser = PermissionManager.getInstance().getCurrentUser();
        // 1. 관리자(Admin)는 무조건 프리패스
        if (currentUser.isAdmin()) return true;

        // 2. 팀장과 일반 팀원은 선택한 팀이 로그인한 계정의 teamName과 선택한 팀명이 일치할 때만 허용
        Team selectedTeam = teamPanel.getSelectedTeam();
        if (selectedTeam != null && selectedTeam.getTeamName().equals(this.memberTeamName)) {
            return true;
        }

        // 권한 부족 시 팝업 띄우고 차단
        JOptionPane.showMessageDialog(this,
                "권한 오류: 본인 소속 팀(" + this.memberTeamName + ")의 업무만 관리할 수 있습니다.",
                "접근 거부", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    private void setupEventHandlers(JButton btnAdd, JButton btnToggle, JButton btnDelete, JButton btnSimulateSync) {

        teamPanel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    Team selectedTeam = teamPanel.getSelectedTeam();
                    if (selectedTeam != null) {
                        projectPanel.updateProjectList(selectedTeam.getProjects());
                        taskPanel.clearTasks();
                    }
                }
            }
        });

        projectPanel.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    Project selectedProject = projectPanel.getSelectedProject();
                    if (selectedProject != null) {
                        taskPanel.refreshTaskList(selectedProject.getTasks());
                    }
                }
            }
        });

        // Task 추가 버튼 권한 제어 연동
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Project selectedProject = projectPanel.getSelectedProject();
                if (selectedProject == null) {
                    JOptionPane.showMessageDialog(MainFrame.this, "먼저 상단에서 프로젝트를 선택하세요.");
                    return;
                }

                // [RBAC] 권한 검사
                if (!hasPermissionForSelectedTeam()) return;

                JTextField titleField = new JTextField();
                String startDateText = LocalDate.now().toString();
                String endDateText = LocalDate.now().plusDays(7).toString();
                JTextField startField = new JTextField(startDateText);
                JTextField endField = new JTextField(endDateText);

                Color[] palette = {
                        new Color(246, 144, 61), new Color(54, 207, 201),
                        new Color(246, 189, 22), new Color(97, 221, 170),
                        new Color(146, 112, 202), new Color(91, 143, 249)
                };
                Color selectedColor = palette[(int)(Math.random() * palette.length)];

                Object[] message = { "업무명:", titleField, "시작일:", startField, "종료일:", endField };
                int option = JOptionPane.showConfirmDialog(MainFrame.this, message, "스케줄 추가", JOptionPane.OK_CANCEL_OPTION);

                if (option == JOptionPane.OK_OPTION && !titleField.getText().trim().isEmpty()) {
                    String owner = PermissionManager.getInstance().getCurrentUser().getName();
                    Task newTask = new Task(selectedProject.getProjectId(), owner, titleField.getText(), startField.getText(), endField.getText(), selectedColor);

                    ProjectManager.getInstance().addTask(selectedProject, newTask);

                    taskPanel.refreshTaskList(selectedProject.getTasks());
                    calendarPanel.refreshCalendar();
                }
            }
        });

        // Task 상태 갱신 버튼
        btnToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Task selectedTask = taskPanel.getSelectedTask();
                if (selectedTask == null) {
                    JOptionPane.showMessageDialog(MainFrame.this, "상태를 변경할 Task를 선택해주세요.");
                    return;
                }

                // 권한 검사
                if (!hasPermissionForSelectedTeam()) return;

                ProjectManager.getInstance().toggleTaskCompletion(selectedTask);
                taskPanel.repaintList();
                calendarPanel.refreshCalendar();
            }
        });

        // Task 삭제 버튼
        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Project currentProject = projectPanel.getSelectedProject();
                Task selectedTask = taskPanel.getSelectedTask();
                if (currentProject == null || selectedTask == null) {
                    JOptionPane.showMessageDialog(MainFrame.this, "삭제할 Task를 선택해주세요.");
                    return;
                }

                // [RBAC] 권한 검사
                if (!hasPermissionForSelectedTeam()) return;

                ProjectManager.getInstance().removeTask(currentProject, selectedTask);
                taskPanel.refreshTaskList(currentProject.getTasks());
                calendarPanel.refreshCalendar();
            }
        });

        // 수동 동기화 버튼
        btnSimulateSync.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProjectManager.getInstance().simulateExternalUpdate();
                JOptionPane.showMessageDialog(MainFrame.this, "동기화 요청 완료. 약 3초 이내에 자동 반영됩니다.");
            }
        });
    }

    // 화면에 출력되는 모든 데이터 갱신
    public void refreshAllViews() {
        User currentUser = PermissionManager.getInstance().getCurrentUser();
        List<Team> allowedTeams = new ArrayList<>();

        // 관리자는 전체 팀에 대해 열람, 팀장 및 팀원인 본인 팀만 열람
        if (currentUser.isAdmin()) {
            allowedTeams = ProjectManager.getInstance().getDatabase();
        } else {
            Team myTeam = ProjectManager.getInstance().getTeamByName(currentUser.getTeamName());
            if (myTeam != null) {
                allowedTeams.add(myTeam);   // 본인 팀만 리스트에 추가
            }
        }

        teamPanel.updateTeamList(allowedTeams);

        // 현재 선택된 팀/프로젝트가 있다면 유지
        Team currentTeam = teamPanel.getSelectedTeam();
        if (currentTeam != null) {
            projectPanel.updateProjectList(currentTeam.getProjects());
            Project currentProject = projectPanel.getSelectedProject();
            if (currentProject != null) {
                taskPanel.refreshTaskList(currentProject.getTasks());
            } else {
                taskPanel.clearTasks();
            }
        } else {
            projectPanel.clearProjects();
            taskPanel.clearTasks();
        }

        calendarPanel.updateFilters();
        calendarPanel.refreshCalendar();
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        btn.setBackground(color);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
        return btn;
    }
}