package gui;

import model.Task;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class TaskPanel extends JPanel {
    private JList<Task> list;
    private DefaultListModel<Task> model;

    public TaskPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)), "Tasks (해당 프로젝트의 전체 상세 세션 목록)"),
                new EmptyBorder(5, 5, 5, 5)
        ));

        model = new DefaultListModel<>();
        list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(32);
        list.setFont(new Font("맑은 고딕", Font.PLAIN, 13));

        add(new JScrollPane(list), BorderLayout.CENTER);
    }

    public void refreshTaskList(List<Task> tasks) {
        model.clear();
        for (Task t : tasks) {
            model.addElement(t);
        }
    }

    public void clearTasks() {
        model.clear();
    }

    public Task getSelectedTask() {
        return list.getSelectedValue();
    }

    public void repaintList() {
        list.repaint();
    }
}