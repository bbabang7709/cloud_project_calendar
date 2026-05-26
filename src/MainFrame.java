import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame {
    private TeamPanel teamPanel;
    private ProjectPanel projectPanel;
    private TaskPanel taskPanel;
    private CalendarPanel calendarPanel;
    
    // 백그라운드 작동용 멀티 스레드 쌍
    private CloudSyncThread syncThread;
    private NotificationThread notificationThread;

    public MainFrame() {
        User currentUser = PermissionManager.getInstance().getCurrentUser();
        setTitle("클라우드 캘린더 - 접속 유저: " + currentUser.getName());
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

        // 상단 반반 스플릿 배치 (팀 목록 / 프로젝트 목록)
        JPanel topSplitPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        topSplitPanel.setBackground(Color.WHITE);
        topSplitPanel.setPreferredSize(new Dimension(0, 250)); 

        teamPanel = new TeamPanel();
        projectPanel = new ProjectPanel();
        topSplitPanel.add(teamPanel);
        topSplitPanel.add(projectPanel);

        // 하단 전체 Task 목록 배치
        taskPanel = new TaskPanel();

        managementTab.add(topSplitPanel, BorderLayout.NORTH);
        managementTab.add(taskPanel, BorderLayout.CENTER);

        // 최하단 띠지 조작 버튼 바 배치
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnAdd = createStyledButton("Task 추가", new Color(70, 130, 180));
        JButton btnToggle = createStyledButton("완료 상태 토글 (미완료↔완료)", new Color(60, 179, 113));
        JButton btnDelete = createStyledButton("Task 삭제", new Color(220, 20, 60));
        JButton btnSimulateSync = createStyledButton("🔄 외부 동기화 흉내내기", Color.DARK_GRAY);

        // [RBAC 권한 제어] Admin이 아니면 삭제 버튼 비활성화 조치
        if (!PermissionManager.getInstance().hasAdminAccess()) {
            btnDelete.setEnabled(false);
            btnDelete.setToolTipText("관리자 권한이 필요합니다.");
        }

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
        // 1. 3초 주기 파일 변경 추적용 백그라운드 동기화 스레드 작동
        syncThread = new CloudSyncThread(this);
        syncThread.start();

        // 2. 5초 주기 마감일 도달 실시간 알림 경고 스레드 작동
        notificationThread = new NotificationThread();
        notificationThread.start();
    }

    private void setupEventHandlers(JButton btnAdd, JButton btnToggle, JButton btnDelete, JButton btnSimulateSync) {
        
        // 1. Team 목록 클릭 시 Project 목록 변경 리스너
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

        // 2. Project 목록 클릭 시 Task 목록 변경 리스너
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

        // 3. Task 추가 버튼 (익명 클래스 사용)
        btnAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Project selectedProject = projectPanel.getSelectedProject();
                if (selectedProject == null) {
                    JOptionPane.showMessageDialog(MainFrame.this, "먼저 상단에서 프로젝트를 선택하세요.");
                    return;
                }

                JTextField titleField = new JTextField();
                JTextField startField = new JTextField("2026-05-15");
                JTextField endField = new JTextField("2026-05-20");
                
                Color[] palette = {
                    new Color(100, 149, 237, 220), new Color(60, 179, 113, 220),  
                    new Color(255, 127, 80, 220), new Color(186, 85, 211, 220),  
                    new Color(218, 165, 32, 220)   
                };
                Color selectedColor = palette[(int)(Math.random() * palette.length)];

                Object[] message = { "업무명:", titleField, "시작일:", startField, "종료일:", endField };
                int option = JOptionPane.showConfirmDialog(MainFrame.this, message, "스케줄 추가", JOptionPane.OK_CANCEL_OPTION);
                
                if (option == JOptionPane.OK_OPTION && !titleField.getText().trim().isEmpty()) {
                    // 접속 중인 사용자명을 ownerName으로 설정하여 파일 분리 기반 마련
                    String owner = PermissionManager.getInstance().getCurrentUser().getName();
                    Task newTask = new Task(selectedProject.getProjectId(), owner, titleField.getText(), startField.getText(), endField.getText(), selectedColor);
                    
                    // 비즈니스 총괄 ProjectManager에 추가 위임 (이후 DataManager로 자동 보존)
                    ProjectManager.getInstance().addTaskToProject(selectedProject, newTask);
                    
                    taskPanel.refreshTaskList(selectedProject.getTasks());
                    calendarPanel.refreshCalendar(); 
                }
            }
        });

        // 4. 완료 상태 토글 버튼
        btnToggle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Task selectedTask = taskPanel.getSelectedTask();
                if (selectedTask != null) {
                    ProjectManager.getInstance().toggleTaskCompletion(selectedTask);
                    taskPanel.repaintList();
                    calendarPanel.refreshCalendar();
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this, "상태를 변경할 Task를 선택해주세요.");
                }
            }
        });

        // 5. Task 삭제 버튼
        btnDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Project currentProject = projectPanel.getSelectedProject();
                Task selectedTask = taskPanel.getSelectedTask();
                if (currentProject != null && selectedTask != null) {
                    ProjectManager.getInstance().removeTaskFromProject(currentProject, selectedTask);
                    taskPanel.refreshTaskList(currentProject.getTasks());
                    calendarPanel.refreshCalendar();
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this, "삭제할 Task를 선택해주세요.");
                }
            }
        });

        // 6. 외부 변경사항 가상 트리거
        btnSimulateSync.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProjectManager.getInstance().simulateExternalUpdate();
                JOptionPane.showMessageDialog(MainFrame.this, "동기화 파일 외부 수정을 시뮬레이션했습니다. 약 3초 이내에 자동 반영됩니다!");
            }
        });
    }

    public void refreshAllViews() {
        teamPanel.updateTeamList(ProjectManager.getInstance().getDatabase());
        projectPanel.clearProjects();
        taskPanel.clearTasks();
        calendarPanel.updateFilters(); 
        calendarPanel.refreshCalendar(); 
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