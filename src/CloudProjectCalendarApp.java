import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class CloudProjectCalendarApp {
	
	public static void main(String[] args) {
        // 프로그램 시작 시 구글 드라이브 동기화 폴더(.txt)에서 
        // 전체 데이터를 싹 다 읽어서 메모리(ProjectManager)에 올려놓습니다.
        ProjectManager.getInstance().initSystemData();
        
        // 윈도우 스타일 적용 (보기 좋게 기본 자바 모양 대신 윈도우 스타일 사용)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("기본 테마 적용 실패: " + e.getMessage());
        }

        // GUI 그리기 작업은 항상 Event Dispatch Thread (EDT)에서 실행되도록 넘깁니다.
        // 이는 Swing의 쓰레드 안전성 규칙입니다.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // 로그인 화면을 켜고, 보이게 설정합니다.
                // 로그인 화면 -> 메인 윈도우 화면 -> 탭 -> 캘린더 패널 순으로 객체가 생성됩니다.
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            }
        });
    }
}
