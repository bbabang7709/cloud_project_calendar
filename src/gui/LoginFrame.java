package gui;

import model.User;

import manager.PermissionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {
    
    public LoginFrame() {
        setTitle("시스템 로그인");
        setSize(400, 300);
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

        // 1. 제목 레이블
        JLabel titleLabel = new JLabel("프로젝트 캘린더 스케줄러");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));

        // 2. 이름 입력 영역
        JLabel nameLabel = new JLabel("이름:");
        nameLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        JTextField nameField = new JTextField(15);

        // 3. 비밀번호 입력 영역
        JLabel pwLabel = new JLabel("비밀번호:");
        pwLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 13));
        JPasswordField pwField = new JPasswordField(15);

        // 4. 로그인 버튼
        JButton loginButton = new JButton("접속 / 신규가입");
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.BLACK);
        loginButton.setFocusPainted(false);
        loginButton.setPreferredSize(new Dimension(200, 35));
        loginButton.setFont(new Font("맑은 고딕", Font.BOLD, 13));

        // 로그인 버튼 클릭 이벤트
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String inputName = nameField.getText().trim();
                // JPasswordField에서 비밀번호를 가져올 때는 char[] 형태이므로 String으로 변환.
                String inputPw = new String(pwField.getPassword()).trim();
                
                if (inputName.isEmpty() || inputPw.isEmpty()) {
                    JOptionPane.showMessageDialog(LoginFrame.this, "이름과 비밀번호를 모두 입력해주세요.");
                    return;
                }
                
                // PermissionManager를 통해 인증 시도
                User authenticatedUser = PermissionManager.getInstance().loginOrRegister(inputName, inputPw);
                
                if (authenticatedUser == null) {
                    // null이 반환된 경우는 아이디는 있는데 비밀번호가 틀린 경우입니다.
                    JOptionPane.showMessageDialog(LoginFrame.this, "비밀번호가 일치하지 않습니다.", "로그인 실패", JOptionPane.ERROR_MESSAGE);
                } else {
                    // 인증 성공 또는 신규 가입 성공
                    PermissionManager.getInstance().setCurrentUser(authenticatedUser);
                    
                    MainFrame mainFrame = new MainFrame();
                    mainFrame.setVisible(true);
                    
                    dispose();
                }
            }
        });

        // 5. 화면 배치 (GridBagConstraints 활용)
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5); 
        
        // 타이틀은 2칸을 차지하도록 합치기 (gridwidth = 2)
        gbc.gridx = 0; 
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(titleLabel, gbc);

        // 이름 레이블과 입력창 배치
        gbc.gridwidth = 1; // 다시 1칸으로 되돌리기
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST; // 글자는 오른쪽 정렬
        card.add(nameLabel, gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST; // 입력창은 왼쪽 정렬
        card.add(nameField, gbc);

        // 비밀번호 레이블과 입력창 배치
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        card.add(pwLabel, gbc);
        
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        card.add(pwField, gbc);

        // 로그인 버튼 배치 (다시 2칸 차지)
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        card.add(loginButton, gbc);

        add(card);
    }
}