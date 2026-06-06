package gui;

import model.Team;
import model.User;

import manager.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class UserManagementPanel extends JPanel {
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton btnPromote;
    private JButton btnDemote;
    private JButton btnAssignTeam;

    public UserManagementPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // 중앙 : 사용자 테이블 영역
        String[] columnNames = { "성명 (ID)", "직급", "소속 팀"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 더블클릭 수정 불가능
            }
        };

        userTable = new JTable(tableModel);
        userTable.setRowHeight(30);
        userTable.setFont(new Font("맑은 고딕", Font.BOLD, 13));
        userTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 13));
        userTable.getTableHeader().setReorderingAllowed(false);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(userTable);
        add(scrollPane, BorderLayout.CENTER);

        // 하단 : 조작용 패널
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        controlPanel.setBackground(Color.WHITE);

        btnPromote = createStyledButton("팀장 지정 (Leader)", new Color(70, 130, 180));
        btnDemote = createStyledButton("팀원 지정 (Member)", new Color(119, 136, 153));
        btnAssignTeam = createStyledButton("팀 배정", new Color(255, 140, 0));

        controlPanel.add(btnPromote);
        controlPanel.add(btnDemote);
        controlPanel.add(btnAssignTeam);
        add(controlPanel, BorderLayout.SOUTH);

        setupEventHandlers();

        refreshUserTable();
    }

    private void setupEventHandlers() {
        btnPromote.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = userTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "설정할 대상을 선택하세요.");
                    return;
                }
                String targetName = (String) tableModel.getValueAt(selectedRow, 0);
                String currentRole = (String) tableModel.getValueAt(selectedRow, 1);
                String currentTeam = (String) tableModel.getValueAt(selectedRow, 2);

                if (currentRole.equals("관리자")) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "관리자의 권한은 변경할 수 없습니다.");
                    return;
                }
                if (currentRole.equals("팀장")) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "이미 [팀장] 직급인 사용자입니다.");
                    return;
                }

                PermissionManager.getInstance().updateUserRoleAndTeam(targetName, "LEADER", currentTeam);
                JOptionPane.showMessageDialog(UserManagementPanel.this, targetName + " 님이 [팀장] 직급으로 설정되었습니다.");
                refreshUserTable();
            }
        });

        btnDemote.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = userTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "설정할 대상을 선택하세요.");
                    return;
                }

                String targetName = (String) tableModel.getValueAt(selectedRow, 0);
                String currentRole = (String) tableModel.getValueAt(selectedRow, 1);
                String currentTeam = (String) tableModel.getValueAt(selectedRow, 2);

                if (currentRole.equals("관리자")) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "관리자의 권한은 변경할 수 없습니다.");
                    return;
                }

                if (currentRole.equals("팀원")) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "이미 [팀원] 직급인 사용자입니다.");
                    return;
                }

                PermissionManager.getInstance().updateUserRoleAndTeam(targetName, "MEMBER", currentTeam);
                JOptionPane.showMessageDialog(UserManagementPanel.this, targetName + " 님이 [팀원] 직급으로 설정되었습니다.");
                refreshUserTable();
            }
        });

        btnAssignTeam.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = userTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "설정할 대상을 선택하세요.");
                    return;
                }

                String targetName = (String) tableModel.getValueAt(selectedRow, 0);
                String currentRole = (String) tableModel.getValueAt(selectedRow, 1);

                if (currentRole.equals("관리자")) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "관리자는 팀 배정이 불가능합니다.");
                    return;
                }

                List<Team> teams = ProjectManager.getInstance().getDatabase();
                if (teams.isEmpty()) {
                    JOptionPane.showMessageDialog(UserManagementPanel.this, "개설된 팀이 존재하지 않습니다.");
                    return;
                }

                List<String> teamNames = new ArrayList<>();
                teamNames.add("N/A");
                for (Team t : teams) {
                    teamNames.add(t.getTeamName());
                }

                String selectedTeam = (String) JOptionPane.showInputDialog(
                        UserManagementPanel.this,
                        targetName + " 님이 소속될 팀을 선택하세요: ",
                        "소속 팀 배정",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        teamNames.toArray(),
                        teamNames.get(0)
                );

                if (selectedTeam != null) {
                    String roleCode = currentRole.equals("팀장") ? "LEADER" : "MEMBER";
                    PermissionManager.getInstance().updateUserRoleAndTeam(targetName, roleCode, selectedTeam);
                    JOptionPane.showMessageDialog(UserManagementPanel.this, targetName + " 님이 [" + selectedTeam + "] 팀으로 배정되었습니다.");
                    refreshUserTable();
                }
            }
        });
    }

    public void refreshUserTable() {
        tableModel.setRowCount(0);
        List<User> list = PermissionManager.getInstance().getUserList();

        for (User u : list) {
            String role;
            if (u.isAdmin()) {
                role = "관리자";
            } else if (u.isLeader()) {
                role = "팀장";
            } else {
                role = "팀원";
            }

            Object[] row = { u.getName(), role, u.getTeamName() };
            tableModel.addRow(row);
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setBackground(Color.WHITE);
        btn.setForeground(Color.BLACK);
        btn.setFocusPainted(false);
        btn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        btn.setBorder(new EmptyBorder(8, 15, 8, 15));

        return btn;
    }
}
