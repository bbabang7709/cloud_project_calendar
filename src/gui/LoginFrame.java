package gui;

import manager.DataManager;
import manager.ProjectManager;
import model.User;

import manager.PermissionManager;

import java.io.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

import java.util.List;

public class LoginFrame extends JFrame {
    
    public LoginFrame() {
        setTitle("시스템 로그인");
        setSize(480, 380);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(Color.WHITE);
        setLayout(new GridBagLayout());

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // 메인 타이틀
        JLabel titleLabel = new JLabel("프로젝트 캘린더 스케줄러");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));

        // 이름 입력 영역
        JLabel nameLabel = new JLabel("이름:");
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        JTextField nameField = new JTextField(15);

        // 비밀번호 입력 영역
        JLabel pwLabel = new JLabel("비밀번호:");
        pwLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        JPasswordField pwField = new JPasswordField(15);

        // 폴더 설정 영역
        JLabel pathLabel = new JLabel("동기화 폴더 경로:");
        pathLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        JTextField pathField = new JTextField(15);
//        pathField.setText(new File(".").getAbsolutePath());
        pathField.setText(DataManager.getInstance().getSaveFolderPath());
        pathField.setEditable(false);
        pathField.setBackground(new Color(245, 245, 245));

        JButton btnBrowse = new JButton("찾기...");
        btnBrowse.setFocusPainted(false);
        btnBrowse.setFont(new Font("맑은 고딕", Font.PLAIN, 11));
        btnBrowse.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // 폴더만 선택 가능하게 설정
            chooser.setDialogTitle("구글 드라이브 혹은 협업 동기화 폴더 선택");

            int result = chooser.showOpenDialog(LoginFrame.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = chooser.getSelectedFile();
                pathField.setText(selectedFolder.getAbsolutePath());
            }
        });


        // 로그인 버튼
        JButton loginButton = new JButton("접속 / 신규가입");
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.BLACK);
        loginButton.setFocusPainted(false);
        loginButton.setPreferredSize(new Dimension(200, 35));
        loginButton.setFont(new Font("맑은 고딕", Font.BOLD, 13));

        // 로그인 버튼 클릭 이벤트
        loginButton.addActionListener(e -> {
            String inputName = nameField.getText().trim();
            // JPasswordField에서 비밀번호를 가져올 때는 char[] 형태이므로 String으로 변환.
            String inputPw = new String(pwField.getPassword()).trim();
            String folderPath = pathField.getText().trim();

            if (inputName.isEmpty() || inputPw.isEmpty()) {
                JOptionPane.showMessageDialog(LoginFrame.this, "이름과 비밀번호를 모두 입력해주세요.");
                return;
            }

            if (folderPath.isEmpty()) {
                JOptionPane.showMessageDialog(LoginFrame.this, "데이터 연동을 위해 동기화 폴더를 지정해야 합니다.");
                return;
            }

            // DataManager에 선택한 폴더 경로 주입
            DataManager.getInstance().setSaveFolderPath(folderPath);

            // 주입된 폴더 기반으로 파일 데이터베이스 리로드
            List<User> loadedUsers = DataManager.getInstance().loadUsers();
            PermissionManager.getInstance().initUserDatabase(loadedUsers);

            ProjectManager.getInstance().initSystemData();

            // PermissionManager를 통해 인증 시도
            User authenticatedUser = PermissionManager.getInstance().loginOrRegister(inputName, inputPw);

            if (authenticatedUser != null) {
                // 인증 성공 또는 신규 가입 성공
                PermissionManager.getInstance().setCurrentUser(authenticatedUser);

                MainFrame mainFrame = new MainFrame();
                mainFrame.setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(LoginFrame.this, "로그인 실패. 아이디(이름)과 비밀번호를 확인하세요.");
            }
        });

        // 5. 화면 배치 (GridBagConstraints 활용)
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        
        // 메인 타이틀
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        card.add(titleLabel, gbc);

        // 이름 입력란
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1; gbc.gridx = 0;
        card.add(nameLabel, gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        card.add(nameField, gbc);

        // 비밀번호 레이블과 입력창 배치
        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2;
        card.add(pwLabel, gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        card.add(pwField, gbc);

        // 폴더 선택 브라우저
        gbc.gridwidth = 1;
        gbc.gridy = 3; gbc.gridx = 0;
        card.add(pathLabel, gbc);
        gbc.gridx = 1;
        card.add(pathField, gbc);
        gbc.gridx = 2;
        card.add(btnBrowse, gbc);

        // 로그인 버튼 배치
        gbc.gridwidth = 3;
        gbc.gridy = 4; gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        card.add(loginButton, gbc);

        add(card);
    }
}
