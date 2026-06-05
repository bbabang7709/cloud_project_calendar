package gui;

import model.Team;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;

public class TeamPanel extends JPanel {
    private JList<Team> list;
    private DefaultListModel<Team> model;

    public TeamPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)), "Teams (팀 목록)"),
                new EmptyBorder(5, 5, 5, 5)
        ));

        model = new DefaultListModel<>();
        list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(32);
        list.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void addListSelectionListener(ListSelectionListener l) {
        list.addListSelectionListener(l);
    }

    public void updateTeamList(List<Team> teams) {
        model.clear();
        for (Team t : teams) {
            model.addElement(t);
        }
    }

    public Team getSelectedTeam() {
        return list.getSelectedValue();
    }
}