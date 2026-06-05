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
import java.util.UUID;
import java.time.LocalDate;

public class MainFrame extends JFrame {
    private TeamPanel teamPanel;
    private ProjectPanel projectPanel;
    private TaskPanel taskPanel;
    private CalendarPanel calendarPanel;

    // 백그라운드 작동용 멀티 스레드 쌍
    private CloudSyncThread syncThread;
    private NotificationThread alarmThread; // 유저 스레드명에 맞게 매핑

    // [권한 제어용] 일반 팀원 로그인 시 본인 소속 팀을 저장하는 변수
    private String memberTeamName = null;

    public MainFrame() {
        User currentUser = PermissionManager.getInstance().getCurrentUser();

        if (currentUser.isAdmin()) {
            memberTeamName = "N/A";
        } else {
            memberTeamName = currentUser.getTeamName();
            if (memberTeamName == null || memberTeamName.trim().isEmpty()) {
                memberTeamName = "N/A";
            }
        }

        String titlePrefix;
        if (currentUser.isAdmin()) {
            titlePrefix = "[관리자] ";
        } else if (currentUser.isLeader()) {
            titlePrefix = "[" + memberTeamName + " 팀장] ";
        } else {
            titlePrefix = "[" + memberTeamName + " 팀원] ";
        }

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

        if (currentUser.isAdmin()) {
            UserManagementPanel userManagementPanel = new UserManagementPanel();
            tabbedPane.addTab("  👥 회원 권한 및 팀 설정  ", userManagementPanel);
        }

        // ----------------- [탭 2: 데이터 관리 패널 부착] -----------------
        JPanel managementTab = new JPanel(new BorderLayout(15, 15));
        managementTab.setBackground(Color.WHITE);
        managementTab.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 상단 컨테이너 (관리자 툴바 + 팀/프로젝트 리스트)
        JPanel northContainer = new JPanel(new BorderLayout(0, 10));
        northContainer.setBackground(Color.WHITE);

        // [RBAC] 관리자 전용 '팀/프로젝트 추가' 패널
        if (currentUser.isAdmin()) {
            JPanel adminPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            adminPanel.setBackground(Color.WHITE);
            adminPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "관리자 전용 패널"));

            JButton btnAddTeam = createStyledButton("팀 추가", new Color(255, 140, 0));
            JButton btnAddProject = createStyledButton("프로젝트 추가", new Color(255, 140, 0));

            btnAddTeam.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String newTeam = JOptionPane.showInputDialog(MainFrame.this, "신규 팀 이름을 입력하세요:");
                    if (newTeam != null && !newTeam.trim().isEmpty()) {
                        ProjectManager.getInstance().addTeam(newTeam.trim());
                        refreshAllViews();
                    }
                }
            });

            btnAddProject.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Team selectedTeam = teamPanel.getSelectedTeam();
                    if (selectedTeam == null) {
                        JOptionPane.showMessageDialog(MainFrame.this, "프로젝트를 추가할 팀을 먼저 선택하세요.");
                        return;
                    }
                    String newProject = JOptionPane.showInputDialog(MainFrame.this, selectedTeam.getTeamName() + "에 추가할 신규 프로젝트명:");
                    if (newProject != null && !newProject.trim().isEmpty()) {
                        // 프로젝트 식별자 생성
                        String pId = "P_" + UUID.randomUUID().toString().substring(0, 5);
                        ProjectManager.getInstance().addProject(selectedTeam, new Project(pId, newProject.trim()));
                        refreshAllViews();
                    }
                }
            });

            adminPanel.add(btnAddTeam);
            adminPanel.add(btnAddProject);
            northContainer.add(adminPanel, BorderLayout.NORTH);
        }

        if (currentUser.isLeader()) {
            JPanel leaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            leaderPanel.setBackground(Color.WHITE);
            leaderPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "관리자 전용 마스터 컨트롤"));

            JButton btnAddProject = createStyledButton("프로젝트 추가", new Color(255, 140, 0));

            btnAddProject.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    Team selectedTeam = teamPanel.getSelectedTeam();
                    if (selectedTeam == null) {
                        JOptionPane.showMessageDialog(MainFrame.this, "프로젝트를 추가할 '팀'을 먼저 아래에서 선택하세요.");
                        return;
                    }
                    if (!selectedTeam.getTeamName().equals(currentUser.getTeamName())) {
                        JOptionPane.showMessageDialog(MainFrame.this, "자신이 소속된 팀의 프로젝트만 생성할 수 있습니다.");
                        return;
                    }
                    String newProject = JOptionPane.showInputDialog(MainFrame.this, selectedTeam.getTeamName() + "에 추가할 신규 프로젝트명:");
                    if (newProject != null && !newProject.trim().isEmpty()) {
                        // 프로젝트 식별자 생성
                        String pId = "P_" + UUID.randomUUID().toString().substring(0, 5);
                        ProjectManager.getInstance().addProject(selectedTeam, new Project(pId, newProject.trim()));
                        refreshAllViews();
                    }
                }
            });

            leaderPanel.add(btnAddProject);
            northContainer.add(leaderPanel, BorderLayout.NORTH);
        }

        // 기존 상단 반반 스플릿 배치 (팀 목록 / 프로젝트 목록)
        JPanel topSplitPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        topSplitPanel.setBackground(Color.WHITE);
        topSplitPanel.setPreferredSize(new Dimension(0, 200));

        teamPanel = new TeamPanel();
        projectPanel = new ProjectPanel();
        topSplitPanel.add(teamPanel);
        topSplitPanel.add(projectPanel);

        northContainer.add(topSplitPanel, BorderLayout.CENTER);
        managementTab.add(northContainer, BorderLayout.NORTH);

        // 하단 전체 model.Task 목록 배치
        taskPanel = new TaskPanel();
        managementTab.add(taskPanel, BorderLayout.CENTER);

        // 최하단 띠지 조작 버튼 바 배치
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnAdd = createStyledButton("model.Task 추가", new Color(70, 130, 180));
        JButton btnToggle = createStyledButton("완료 상태 토글 (미완료↔완료)", new Color(60, 179, 113));
        JButton btnDelete = createStyledButton("model.Task 삭제", new Color(220, 20, 60));
        JButton btnSimulateSync = createStyledButton("수동 동기화", Color.DARK_GRAY);

        bottomPanel.add(btnSimulateSync);
        bottomPanel.add(btnAdd);
        bottomPanel.add(btnToggle);
        bottomPanel.add(btnDelete);
        managementTab.add(bottomPanel, BorderLayout.SOUTH);

        tabbedPane.addTab("  📁 프로젝트 데이터 관리  ", managementTab);
        add(tabbedPane, BorderLayout.CENTER);

        // 이벤트 처리 위임
        setupEventHandlers(btnAdd, btnToggle, btnDelete, btnSimulateSync);

        // 초기 데이터 가시화
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

    // [RBAC] 권한 검사 공통 헬퍼 메서드
    private boolean hasPermissionForSelectedTeam() {
        User currentUser = PermissionManager.getInstance().getCurrentUser();
        // 1. 관리자(model.Admin)는 무조건 프리패스
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

        // model.Task 추가 버튼 권한 제어 연동
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

        // 완료 상태 토글 버튼 권한 제어 연동
        btnToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Task selectedTask = taskPanel.getSelectedTask();
                if (selectedTask == null) {
                    JOptionPane.showMessageDialog(MainFrame.this, "상태를 변경할 Task를 선택해주세요.");
                    return;
                }

                // [RBAC] 권한 검사
                if (!hasPermissionForSelectedTeam()) return;

                ProjectManager.getInstance().toggleTaskCompletion(selectedTask);
                taskPanel.repaintList();
                calendarPanel.refreshCalendar();
            }
        });

        // model.Task 삭제 버튼 권한 제어 연동
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

        btnSimulateSync.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProjectManager.getInstance().simulateExternalUpdate();
                JOptionPane.showMessageDialog(MainFrame.this, "동기화 요청 완료. 약 3초 이내에 자동 반영됩니다.");
            }
        });
    }

    public void refreshAllViews() {
        teamPanel.updateTeamList(ProjectManager.getInstance().getDatabase());
        projectPanel.clearProjects();
        taskPanel.clearTasks();
        calendarPanel.updateFilters();
        calendarPanel.refreshCalendar();

        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i) instanceof JTabbedPane) {
                JTabbedPane tp = (JTabbedPane) getComponent(i);
                for (int j = 0; j < tp.getTabCount(); j++) {
                    if (tp.getComponent(j) instanceof UserManagementPanel) {
                        ((UserManagementPanel) tp.getComponentAt(j)).refreshUserTable();
                    }
                }
            }
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(color);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));
        return btn;
    }
}