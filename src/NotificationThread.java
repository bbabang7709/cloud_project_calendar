import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class NotificationThread extends Thread {
    private boolean isRunning;
    // 이미 알림을 보낸 Task들을 보관하여 중복 팝업 방지
    private List<Task> alarmedTasks;

    public NotificationThread() {
        this.isRunning = true;
        this.alarmedTasks = new ArrayList<>();
        setDaemon(true); // 데몬 스레드 설정
    }

    @Override
    public void run() {
        System.out.println("[알림 스레드] 마감일 모니터링 작동 개시...");
        
        while (isRunning) {
            try {
                // 5초 간격으로 마감일 검사
                Thread.sleep(5000);
                
                LocalDate today = LocalDate.now();
                List<Team> database = ProjectManager.getInstance().getDatabase();
                
                // 전사적인 모든 미완료 Task 탐색
                for (Team team : database) {
                    for (Project project : team.getProjects()) {
                        for (Task task : project.getTasks()) {
                            
                            // 아직 완료되지 않았고, 이미 알림을 보내지 않은 Task 대상
                            if (!task.isCompleted() && !alarmedTasks.contains(task)) {
                                try {
                                    LocalDate deadline = LocalDate.parse(task.getDeadline());
                                    
                                    // 마감 기한이 오늘이거나 이미 지난 경우
                                    if (deadline.isEqual(today) || deadline.isBefore(today)) {
                                        alarmedTasks.add(task); // 다시 알림이 울리지 않게 저장
                                        
                                        // 익명 클래스로 Swing UI 스레드에 다이얼로그 팝업 지시
                                        SwingUtilities.invokeLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                String msg = "🚨 [마감 경고] 🚨\n" +
                                                        "팀: " + team.getTeamName() + "\n" +
                                                        "프로젝트: " + project.getProjectName() + "\n" +
                                                        "업무명: " + task.getTitle() + "\n" +
                                                        "마감일: " + task.getDeadline() + "\n\n" +
                                                        "해당 업무의 마감 기한이 임박했거나 지났습니다!";
                                                JOptionPane.showMessageDialog(null, msg, "업무 마감 경고", JOptionPane.WARNING_MESSAGE);
                                            }
                                        });
                                    }
                                } catch (Exception ex) {
                                    // 날짜 형식이 잘못 파싱된 경우 오류 방지용 예외처리
                                }
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                isRunning = false;
            }
        }
    }
}