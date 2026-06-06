import gui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class CloudProjectCalendarApp {
	
	public static void main(String[] args) {
        
        // 윈도우 스타일 적용
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("기본 테마 적용 실패: " + e.getMessage());
        }

        // GUI 그리기 작업은 실행
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // 로그인 화면 -> 메인 윈도우 화면 -> 탭 -> 캘린더 패널 순으로 객체가 생성
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            }
        });
    }
}
