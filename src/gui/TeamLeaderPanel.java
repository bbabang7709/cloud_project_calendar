package gui;

import manager.PermissionManager;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;

public class TeamLeaderPanel extends JPanel {
    private JTable unassignedUserTable;
    private DefaultTableModel tableModel;

    public TeamLeaderPanel() {
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground((Color.WHITE));

        //상단 안내 문구
        User currentUser = PermissionManager.getInstance().getCurrentUser();
        JLabel title = new JLabel("[" + currentUser.getTeamName() + "] 팀장 전용 데스크");
        add(title, BorderLayout.NORTH);

        // 테이블 모델 및 JTable 설정
        String[] columnNames = {"사용자 이름", "현재 상태", "직급"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 더블클릭 수정 방지
            }
        };
        unassignedUserTable = new JTable(tableModel);
        unassignedUserTable.setRowHeight(25);
        unassignedUserTable.getTableHeader().setFont(new Font("맑은 고딕", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(unassignedUserTable);
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        // 하단 조작 버튼
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setBackground(Color.WHITE);

        JButton btnRecruit = new JButton("팀원 지정 (+)");
        btnRecruit.setBackground(Color.WHITE);
        btnRecruit.setForeground(Color.BLACK);
        btnRecruit.setFocusPainted(false);

        btnRecruit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedRow = unassignedUserTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(TeamLeaderPanel.this, "팀원을 선택해주세요.");
                    return;
                }

                String targetName = (String) tableModel.getValueAt(selectedRow, 0);

                int confirm = JOptionPane.showConfirmDialog(TeamLeaderPanel.this,
                        targetName + "님을 [" + currentUser.getTeamName() + "] 소속으로 지정하시겠습니까?",
                        "팀원 지정 확인", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    PermissionManager.getInstance().updateUserRoleAndTeam(targetName, "MEMBER", currentUser.getTeamName());
                    JOptionPane.showMessageDialog(TeamLeaderPanel.this, "소속이 확정되었습니다.");
                    refreshTableData();
                }
            }
        });

        bottomPanel.add(btnRecruit);
        add(bottomPanel, BorderLayout.SOUTH);

        refreshTableData();
    }

    public void refreshTableData() {
       tableModel.setRowCount(0);
       List<User> unassigned = PermissionManager.getInstance().getUnassignedUsers();

       for (User u : unassigned) {
           Object[] rowData = { u.getName(), u.getTeamName(), "소속 지정 대기"};
           tableModel.addRow(rowData);
       }
    }
}
