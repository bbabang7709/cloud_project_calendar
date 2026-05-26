import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;

public class ProjectPanel extends JPanel {
    private JList<Project> list;
    private DefaultListModel<Project> model;

    public ProjectPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)), "Projects (프로젝트 목록)"),
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

    public void updateProjectList(List<Project> projects) {
        model.clear();
        for (Project p : projects) {
            model.addElement(p);
        }
    }

    public void clearProjects() {
        model.clear();
    }

    public Project getSelectedProject() {
        return list.getSelectedValue();
    }
}