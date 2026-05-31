import javax.swing.SwingUtilities;

public class CloudSyncThread extends Thread {
    private MainFrame mainFrame;
    private boolean isRunning;

    public CloudSyncThread(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.isRunning = true;
        
        // 메인 스레드가 종료되면 이 스레드도 함께 종료되도록 데몬 스레드로 설정
        setDaemon(true); 
    }

    @Override
    public void run() {
        System.out.println("[동기화 스레드] 구글 드라이브 파일 폴링 감시 시작...");
        
        while (isRunning) {
            try {
                // 3초 대기
                Thread.sleep(3000);

                // DataManager를 통해 구글 드라이브 로컬 동기화 파일이 변경되었는지 감시
                if (DataManager.getInstance().checkFileModified()) {
                    System.out.println("[동기화 스레드] 외부 변경 감지! 메모리 리로드 및 GUI 갱신 진행");
                    
                    // 메모리상의 DB 최신 데이터로 동기화
                    ProjectManager.getInstance().refreshData();
                    
                    // Swing 컴포넌트 갱신은 반드시 Event Dispatch Thread(EDT)에서 처리해야 안전함
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            mainFrame.refreshAllViews();
                        }
                    });
                }
            } catch (InterruptedException e) {
                System.out.println("[동기화 스레드] 스레드 중단됨");
                isRunning = false;
            }
        }
    }
}