import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CalendarPanel extends JPanel {
	private CalendarCanvas calendarCanvas;
	private YearMonth currentMonth;
	private LocalDate currentWeekStart;
	
	private JLabel monthLabel;
	private JComboBox<String> teamFilterCombo;
	private JComboBox<String> projectFilterCombo;
	private JToggleButton viewModeToggle;
	
	public CalendarPanel() {
		this.currentMonth = YearMonth.of(2026, 5);
		this.currentWeekStart = LocalDate.of(2026, 5, 10);
		
		setLayout(new BorderLayout());
		setBackground(Color.WHITE);
		setBorder(new EmptyBorder(10, 10, 10, 10));
		
		JPanel topControllerPanel = new JPanel();
		topControllerPanel.setLayout(new BoxLayout(topControllerPanel, BoxLayout.Y_AXIS));
		topControllerPanel.setBackground(Color.WHITE);
		
		// 상단 필터 및 조작 패널
		JPanel filterLinePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
		filterLinePanel.setBackground(Color.WHITE);
		
		filterLinePanel.add(new JLabel("뷰 모드: "));
		viewModeToggle = new JToggleButton("월간 뷰 (Monthly)");
		viewModeToggle.setSelected(true);
		viewModeToggle.setFocusPainted(false);
		viewModeToggle.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (viewModeToggle.isSelected()) {
					viewModeToggle.setText("월간 뷰 (Monthly)");
				} else {
					viewModeToggle.setText("주간 뷰 (Weekly)");
				}
				refreshCalendar();
			}
		});
		filterLinePanel.add(viewModeToggle);
		
		filterLinePanel.add(new JLabel("  |  팀 필터:"));
		teamFilterCombo = new JComboBox<>();
		teamFilterCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateProjectFilterOptions();
				refreshCalendar();
			}
		});
		filterLinePanel.add(teamFilterCombo);
		
		filterLinePanel.add(new JLabel("프로젝트 필터:"));
		projectFilterCombo = new JComboBox<>();
		projectFilterCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshCalendar();
			}
		});
		filterLinePanel.add(projectFilterCombo);
		
		topControllerPanel.add(filterLinePanel);
		
		// 네비게이션 헤더 (이전/다음 달 이동)
		JPanel navLinePanel = new JPanel(new BorderLayout());
		navLinePanel.setBackground(Color.WHITE);
		navLinePanel.setBorder(new EmptyBorder(10, 0, 5, 0));
		
		JButton prevBtn = new JButton("<<");
		prevBtn.setContentAreaFilled(false);
		prevBtn.setPreferredSize(new Dimension(60, 35));
		
		JButton nextBtn = new JButton(">>");
		nextBtn.setContentAreaFilled(false);
		nextBtn.setPreferredSize(new Dimension(60, 35));
		
		prevBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleNavigation(-1);
			}
		});
		nextBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleNavigation(1);
			}
		});
		
		// [수정 포인트] 누락된 monthLabel 객체를 생성하고 폰트를 지정합니다.
		monthLabel = new JLabel("", SwingConstants.CENTER);
		monthLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
		
		navLinePanel.add(prevBtn, BorderLayout.WEST);
		navLinePanel.add(monthLabel, BorderLayout.CENTER);
		navLinePanel.add(nextBtn, BorderLayout.EAST);
		topControllerPanel.add(navLinePanel);

		add(topControllerPanel, BorderLayout.NORTH);

		// 일 ~ 토 요일 가이드 패널
		JPanel dayHeaderPanel = new JPanel(new GridLayout(1, 7));
		dayHeaderPanel.setBackground(Color.WHITE);
		dayHeaderPanel.setBorder(new EmptyBorder(5, 0, 5, 0));
		String[] days = { "일", "월", "화", "수", "목", "금", "토" };
		for (int i = 0; i < days.length; i++) {
			JLabel l = new JLabel(days[i], SwingConstants.CENTER);
			l.setFont(new Font("맑은 고딕", Font.BOLD, 13));
			l.setForeground(i == 0 ? new Color(220, 50, 50) : 
				           (i == 6 ? new Color(50, 50, 220) : new Color(150, 150, 150)));
			dayHeaderPanel.add(l);
		}
		
		topControllerPanel.add(dayHeaderPanel);
		
		calendarCanvas = new CalendarCanvas(this);
		add(calendarCanvas, BorderLayout.CENTER);
	}
	
	public boolean isMonthlyMode() {
		return viewModeToggle.isSelected();
	}
	
	public YearMonth getCurrentMonth() {
		return currentMonth;
	}
	
	public LocalDate getCurrentWeekStart() {
		return currentWeekStart;
	}
	
	private void handleNavigation(int direction) {
		if (viewModeToggle.isSelected()) {
			currentMonth = currentMonth.plusMonths(direction);
		} else {
			currentWeekStart = currentWeekStart.plusWeeks(direction);
		}
		refreshCalendar();
	}
	public void updateFilters() {
		ActionListener[] listeners = teamFilterCombo.getActionListeners();
		for (ActionListener l : listeners) {
			teamFilterCombo.removeActionListener(l);
		}
		
		teamFilterCombo.removeAllItems();
		teamFilterCombo.addItem("전체 팀");
		for (Team t : ProjectManager.getInstance().getDatabase()) {
			teamFilterCombo.addItem(t.toString());
		}
		
		for (ActionListener l : listeners) {
			teamFilterCombo.addActionListener(l);
		}
		updateProjectFilterOptions();
	}
	
	private void updateProjectFilterOptions() {
		if (teamFilterCombo.getSelectedItem() == null) return;
		
		ActionListener[] listeners = projectFilterCombo.getActionListeners();
		for (ActionListener l : listeners) {
			projectFilterCombo.removeActionListener(l);
		}
		
		projectFilterCombo.removeAllItems();
		projectFilterCombo.addItem("전체 프로젝트");
		String selectedTeamString = (String)teamFilterCombo.getSelectedItem();
		
		for (Team t : ProjectManager.getInstance().getDatabase()) {
			if (selectedTeamString.equals("전체 팀") || t.toString().equals(selectedTeamString)) {
				for (Project p : t.getProjects()) {
					projectFilterCombo.addItem(p.toString());
				}
			}
		}
		
		for (ActionListener l : listeners) {
			projectFilterCombo.addActionListener(l);
		}
	}
	
	public void refreshCalendar() {
		if (viewModeToggle.isSelected()) {
			monthLabel.setText(currentMonth.getYear() + " . " + String.format("%02d", currentMonth.getMonthValue()) + " (Monthly)");
		} else {
			LocalDate weekEnd = currentWeekStart.plusDays(6);
			monthLabel.setText(currentWeekStart.getMonthValue() + "월 " + currentWeekStart.getDayOfMonth() + "일 ~ " + 
							   weekEnd.getMonthValue() + "월 " + weekEnd.getDayOfMonth() + "일 (Weekly)");
		}
		calendarCanvas.repaint();
	}
	
	public List<Task> getFilteredAndSortedTasks() {
		List<Task> filteredList = new ArrayList<>();
		String selectedTeamName = (String)teamFilterCombo.getSelectedItem();
		String selectedProjectName = (String)projectFilterCombo.getSelectedItem();
		
		if (selectedTeamName == null || selectedProjectName == null) return filteredList;
		
		for (Team t : ProjectManager.getInstance().getDatabase()) {
			if (selectedTeamName.equals("전체 팀") || t.toString().equals(selectedTeamName)) {
				for (Project p : t.getProjects()) {
					if (selectedProjectName.equals("전체 프로젝트") || p.toString().equals(selectedProjectName)) {
						filteredList.addAll(p.getTasks());
					}
				}
			}
		}
		
		for (int i = 1; i < filteredList.size(); i++) {
			Task key = filteredList.get(i);
			int j = i - 1;
			while (j >= 0 && filteredList.get(j).getDeadline().compareTo(key.getDeadline()) > 0) {
				filteredList.set(j + 1, filteredList.get(j));
				j--;
			}
			filteredList.set(j + 1, key);
		}
		
		return filteredList;
	}
}