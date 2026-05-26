import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarCanvas extends JPanel {
	private CalendarPanel calendarPanel;
	private final Color GRID_COLOR = new Color(242, 242, 242);
	private final int TASK_BAR_HEIGHT = 18;
	private final int TASK_BAR_GAP = 4;
	
	public CalendarCanvas(CalendarPanel calendarPanel) {
		this.calendarPanel = calendarPanel;
		setBackground(Color.WHITE);
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				handleCanvasClick(e.getX(), e.getY());
			}
		});
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		int width = getWidth();
		int height = getHeight();
		int cellWidth = width / 7;
		
		if (calendarPanel.isMonthlyMode()) {
			YearMonth currentMonth = calendarPanel.getCurrentMonth();
			LocalDate firstDay = currentMonth.atDay(1);
			int daysInMonth = currentMonth.lengthOfMonth();
			int startGap = firstDay.getDayOfWeek().getValue() % 7;
			int totalCells = startGap + daysInMonth;
			int numRows = (int)Math.ceil((double)totalCells / 7);
			int cellHeight = height / numRows;
			
			drawMonthlyGrid(g2, cellWidth, cellHeight, startGap, daysInMonth, numRows);
			drawMonthlyTaskBars(g2, cellWidth, cellHeight, startGap, firstDay);
		} else {
			drawWeeklyGrid(g2, cellWidth, height);
			drawWeeklyTaskBars(g2, cellWidth, height);
		}
	}
	
	private void drawMonthlyGrid(Graphics2D g2, int cw, int ch, int startGap, int currentDays, int numRows) {
		Font dateFont = new Font("Segoe UI", Font.PLAIN, 12);
		g2.setFont(dateFont);
		LocalDate today = LocalDate.now();
		
		for (int i = 0; i < numRows * 7; i++) {
			int col = i % 7;
			int row = i / 7;
			int x = col * cw;
			int y = row * ch;
			
			g2.setColor(GRID_COLOR);
			g2.drawRect(x,  y,  cw,  ch);
			
			int dayNum = i - startGap + 1;
			if (dayNum >= 1 && dayNum <= currentDays) {
				LocalDate date = calendarPanel.getCurrentMonth().atDay(dayNum);
				g2.setColor(col == 0 ? new Color(220, 50, 50) :
							col == 6 ? new Color(50, 50, 220) : Color.BLACK);
				
				if (date.equals(today)) {
					g2.setColor(new Color(70, 130, 180));
					g2.setFont(dateFont.deriveFont(Font.BOLD));
				} else {
					g2.setFont(dateFont);
				}
				
				g2.drawString(String.valueOf(dayNum), x + 8, y + g2.getFontMetrics().getAscent() + 6);
			}
		}
	}
	
	private void drawWeeklyGrid(Graphics2D g2, int cw, int ch) {
		Font dateFont = new Font("Segoe UI", Font.PLAIN, 13);
        LocalDate today = LocalDate.now();
        LocalDate weekStart = calendarPanel.getCurrentWeekStart();

        for (int i = 0; i < 7; i++) {
            int x = i * cw;
            g2.setColor(GRID_COLOR);
            g2.drawRect(x, 0, cw, ch);
            
            LocalDate date = weekStart.plusDays(i);
            g2.setColor(i == 0 ? new Color(220, 50, 50) : (i == 6 ? new Color(50, 50, 220) : Color.BLACK));
            
            if (date.equals(today)) {
                g2.setColor(new Color(70, 130, 180));
                g2.setFont(dateFont.deriveFont(Font.BOLD));
            } else {
                g2.setFont(dateFont);
            }

            g2.drawString(date.getDayOfMonth() + "일", x + 10, g2.getFontMetrics().getAscent() + 8);
        }
	}
	
	private void drawMonthlyTaskBars(Graphics2D g2, int cw, int ch, int startGap, LocalDate firstDay) {
        List<Task> filteredTasks = calendarPanel.getFilteredAndSortedTasks();
        Map<LocalDate, Integer> laneAllocator = new HashMap<>();

        for (Task task : filteredTasks) {
            LocalDate start = LocalDate.parse(task.getStartDate());
            LocalDate end = LocalDate.parse(task.getDeadline());
            LocalDate monthStart = firstDay;
            LocalDate monthEnd = calendarPanel.getCurrentMonth().atEndOfMonth();
            
            if (end.isBefore(monthStart) || start.isAfter(monthEnd)) continue;

            LocalDate drawStart = start.isBefore(monthStart) ? monthStart : start;
            LocalDate drawEnd = end.isAfter(monthEnd) ? monthEnd : end;

            int allocatedLane = 0;
            while (true) {
                boolean laneFree = true;
                for (LocalDate d = drawStart; !d.isAfter(drawEnd); d = d.plusDays(1)) {
                    if (laneAllocator.containsKey(d) && laneAllocator.get(d) == allocatedLane) {
                        laneFree = false;
                        break;
                    }
                }
                if (laneFree) break;
                allocatedLane++;
            }

            int maxLanes = (ch - 25) / (TASK_BAR_HEIGHT + TASK_BAR_GAP);
            if (allocatedLane >= maxLanes - 1) {
                for (LocalDate d = drawStart; !d.isAfter(drawEnd); d = d.plusDays(1)) {
                    laneAllocator.put(d, allocatedLane);
                }
                drawMonthlyMoreIndicator(g2, cw, ch, startGap, drawStart, drawEnd, maxLanes);
                continue; 
            }
            
            for (LocalDate d = drawStart; !d.isAfter(drawEnd); d = d.plusDays(1)) {
                laneAllocator.put(d, allocatedLane);
            }

            g2.setPaint(new GradientPaint(0, 0, task.getColor(), cw, 0, task.getColor().darker()));

            LocalDate renderDate = drawStart;
            while (!renderDate.isAfter(drawEnd)) {
                int dayIndex = renderDate.getDayOfMonth() + startGap - 1;
                int col = dayIndex % 7;
                int row = dayIndex / 7;
                int y = row * ch + 25 + (allocatedLane * (TASK_BAR_HEIGHT + TASK_BAR_GAP));

                int daysThisWeek = 0;
                LocalDate weekDate = renderDate;
                while (!weekDate.isAfter(drawEnd)) {
                    daysThisWeek++;
                    if (weekDate.getDayOfWeek() == DayOfWeek.SATURDAY) break; 
                    weekDate = weekDate.plusDays(1);
                }

                int x = ((renderDate.getDayOfMonth() + startGap - 1) % 7) * cw + 2;
                int barWidth = daysThisWeek * cw - 4;

                g2.fill(new RoundRectangle2D.Double(x, y, barWidth, TASK_BAR_HEIGHT, 6, 6));

                if (renderDate.equals(drawStart) || renderDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
                    g2.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                    g2.setColor(Color.WHITE);
                    String dispTitle = (task.isCompleted() ? "[완료] " : "") + task.getTitle();
                    if(g2.getFontMetrics().stringWidth(dispTitle) > barWidth - 10) {
                        dispTitle = dispTitle.substring(0, Math.max(1, dispTitle.length() - 3)) + "..";
                    }
                    g2.drawString(dispTitle, x + 5, y + g2.getFontMetrics().getAscent() + (TASK_BAR_HEIGHT - g2.getFontMetrics().getHeight()) / 2);
                }
                renderDate = weekDate.plusDays(1);
            }
        }
    }
	
	private void drawWeeklyTaskBars(Graphics2D g2, int cw, int ch) {
        List<Task> filteredTasks = calendarPanel.getFilteredAndSortedTasks();
        Map<LocalDate, Integer> laneAllocator = new HashMap<>();
        LocalDate weekEnd = calendarPanel.getCurrentWeekStart().plusDays(6);

        for (Task task : filteredTasks) {
            LocalDate start = LocalDate.parse(task.getStartDate());
            LocalDate end = LocalDate.parse(task.getDeadline());

            if (end.isBefore(calendarPanel.getCurrentWeekStart()) || start.isAfter(weekEnd)) continue;

            LocalDate drawStart = start.isBefore(calendarPanel.getCurrentWeekStart()) ? calendarPanel.getCurrentWeekStart() : start;
            LocalDate drawEnd = end.isAfter(weekEnd) ? weekEnd : end;

            int allocatedLane = 0;
            while (true) {
                boolean laneFree = true;
                for (LocalDate d = drawStart; !d.isAfter(drawEnd); d = d.plusDays(1)) {
                    if (laneAllocator.containsKey(d) && laneAllocator.get(d) == allocatedLane) {
                        laneFree = false;
                        break;
                    }
                }
                if (laneFree) break;
                allocatedLane++;
            }

            int maxLanes = (ch - 30) / (TASK_BAR_HEIGHT + TASK_BAR_GAP + 2);
            if (allocatedLane >= maxLanes) continue; 

            for (LocalDate d = drawStart; !d.isAfter(drawEnd); d = d.plusDays(1)) {
                laneAllocator.put(d, allocatedLane);
            }

            g2.setPaint(new GradientPaint(0, 0, task.getColor(), cw, 0, task.getColor().darker()));

            int startCol = (int) (drawStart.toEpochDay() - calendarPanel.getCurrentWeekStart().toEpochDay());
            int totalDays = (int) (drawEnd.toEpochDay() - drawStart.toEpochDay()) + 1;
            int x = startCol * cw + 4;
            int y = 30 + (allocatedLane * (TASK_BAR_HEIGHT + TASK_BAR_GAP + 2));
            int barWidth = totalDays * cw - 8;

            g2.fill(new RoundRectangle2D.Double(x, y, barWidth, TASK_BAR_HEIGHT + 2, 8, 8));
            g2.setFont(new Font("맑은 고딕", Font.BOLD, 11));
            g2.setColor(Color.WHITE);
            g2.drawString(task.getTitle() + (task.isCompleted() ? " (완료)" : ""), x + 10, y + g2.getFontMetrics().getAscent() + (TASK_BAR_HEIGHT + 2 - g2.getFontMetrics().getHeight()) / 2);
        }
    }

    private void drawMonthlyMoreIndicator(Graphics2D g2, int cw, int ch, int startGap, LocalDate drawStart, LocalDate drawEnd, int maxLanes) {
        g2.setFont(new Font("맑은 고딕", Font.BOLD, 10));
        g2.setColor(Color.DARK_GRAY);
        for (LocalDate d = drawStart; !d.isAfter(drawEnd); d = d.plusDays(1)) {
            int dayIndex = d.getDayOfMonth() + startGap - 1;
            g2.drawString("+ 일정 더보기", (dayIndex % 7) * cw + 5, (dayIndex / 7) * ch + 25 + ((maxLanes - 1) * (TASK_BAR_HEIGHT + TASK_BAR_GAP)) + g2.getFontMetrics().getAscent());
        }
    }

    private void handleCanvasClick(int mx, int my) {
        int cw = getWidth() / 7;
        LocalDate clickedDate = null;

        if (calendarPanel.isMonthlyMode()) {
            LocalDate firstDay = calendarPanel.getCurrentMonth().atDay(1);
            int daysInMonth = calendarPanel.getCurrentMonth().lengthOfMonth();
            int startGap = firstDay.getDayOfWeek().getValue() % 7;
            int ch = getHeight() / ((int) Math.ceil((double) (startGap + daysInMonth) / 7));
            int dayNum = (my / ch) * 7 + (mx / cw) - startGap + 1;
            if (dayNum >= 1 && dayNum <= daysInMonth) {
                clickedDate = calendarPanel.getCurrentMonth().atDay(dayNum);
            }
        } else {
            if (mx / cw < 7) {
                clickedDate = calendarPanel.getCurrentWeekStart().plusDays(mx / cw);
            }
        }

        if (clickedDate != null) {
            List<String> matchDetails = new ArrayList<>();
            for (Team t : ProjectManager.getInstance().getDatabase()) {
                for (Project p : t.getProjects()) {
                    for (Task task : p.getTasks()) {
                        if (task.isWithinRange(clickedDate)) {
                            matchDetails.add((task.isCompleted() ? "[완료]" : "[진행]") + " [" + t.toString() + "] " + p.toString() + " ➔ " + task.getTitle() + " (~" + task.getDeadline() + ")");
                        }
                    }
                }
            }

            StringBuilder sb = new StringBuilder("📅 상세 일정 브리핑: " + clickedDate + "\n===============================\n\n");
            if (matchDetails.isEmpty()) {
                sb.append("등록된 중요 스케줄이 없습니다.");
            } else {
                for (String d : matchDetails) {
                    sb.append(d).append("\n");
                }
            }
            
            JOptionPane.showMessageDialog(this, sb.toString(), "일정 브리핑", JOptionPane.PLAIN_MESSAGE);
        }
    }
}
