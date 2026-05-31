import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Rectangle2D;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarCanvas extends JPanel {
    private CalendarPanel calendarPanel;
    private static final Color GRID_COLOR = new Color(242, 242, 242);
    private static final int TASK_BAR_HEIGHT = 18;
    private static final int TASK_BAR_GAP = 4;

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

        // 선명하게 그리기 옵션 켜기
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
        Font dateFont = new Font("맑은 고딕", Font.PLAIN, 12);
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
        Font dateFont = new Font("맑은 고딕", Font.PLAIN, 13);
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

            g2.drawString(date.getDayOfMonth() + "", x + 10, g2.getFontMetrics().getAscent() + 8);
        }
    }

    private void drawMonthlyTaskBars(Graphics2D g2, int cw, int ch, int startGap, LocalDate firstDay) {
        List<Task> filteredTasks = calendarPanel.getFilteredAndSortedTasks();
        Map<LocalDate, Integer> laneAllocator = new HashMap<>();

        for (int i = 0; i < filteredTasks.size(); i++) {
            Task task = filteredTasks.get(i);
            LocalDate start = parseTaskDate(task, true);
            LocalDate end = parseTaskDate(task, false);
            if (start == null || end == null || start.isAfter(end)) continue;

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

            // 그래픽 렌더링 오류를 막기 위해 그라데이션 대신 깔끔한 단색(Solid) 적용
            g2.setColor(task.getColor());

            // 2. 완벽한 일자 계산 (ChronoUnit) 기반의 띠지 드로잉 알고리즘
            LocalDate renderDate = drawStart;
            while (!renderDate.isAfter(drawEnd)) {

                // 그려야 할 날짜가 현재 달력의 몇 번째 칸(Index)인지 정확히 계산
                int dayNum = renderDate.getDayOfMonth();
                int cellIndex = dayNum + startGap - 1;
                int col = cellIndex % 7;
                int row = cellIndex / 7;

                // 이번 줄(Row)에서 토요일(오른쪽 끝)까지 그릴 수 있는 남은 칸 수
                int daysRemainingInWeek = 7 - col;

                // 실제로 이 Task를 위해 앞으로 그려야 할 총 남은 일수
                int totalDaysLeft = (int) ChronoUnit.DAYS.between(renderDate, drawEnd) + 1;

                // 이번 줄에 그릴 칸 수 (전체 남은 일정 vs 이번 주 남은 칸 중 작은 값)
                int drawDays = Math.min(totalDaysLeft, daysRemainingInWeek);

                boolean startsThisSegment = renderDate.equals(drawStart);
                boolean endsThisSegment = renderDate.plusDays(drawDays - 1).equals(drawEnd);

                int x = col * cw;
                int y = row * ch + 25 + (allocatedLane * (TASK_BAR_HEIGHT + TASK_BAR_GAP));
                int barWidth = drawDays * cw;

                // 시작과 끝에만 여백(2px)을 주어 중간에 끊어지는 현상 완벽 방지
                if (startsThisSegment) { x += 2; barWidth -= 2; }
                if (endsThisSegment) { barWidth -= 2; }

                // 기본적으로 둥근 사각형 그리기
                g2.fill(new RoundRectangle2D.Double(x, y, barWidth, TASK_BAR_HEIGHT, 6, 6));

                // [핵심 트릭] 다음 주로 이어지면 오른쪽 모서리를 직각(Flat)으로 덮어서 안 끊어진 것처럼 보이게 함
                if (!endsThisSegment) {
                    g2.fill(new Rectangle2D.Double(x + barWidth - 6, y, 6, TASK_BAR_HEIGHT));
                }
                // 이전 주에서 이어져 오면 왼쪽 모서리를 직각(Flat)으로 덮음
                if (!startsThisSegment) {
                    g2.fill(new Rectangle2D.Double(x, y, 6, TASK_BAR_HEIGHT));
                }

                // 텍스트 출력 (시작일이거나, 이어지는 주의 첫 칸(일요일)인 경우에만 그림)
                if (startsThisSegment || col == 0) {
                    g2.setFont(new Font("맑은 고딕", Font.BOLD, 10));
                    g2.setColor(Color.WHITE);

                    int textX = x + 5;
                    if (!startsThisSegment) textX += 3; // 이전 주에서 넘어오면 글씨가 테두리에 닿지 않게 살짝 띄움

                    String dispTitle = (task.isCompleted() ? "[완료] " : "") + task.getTitle();
                    dispTitle = ellipsize(g2, dispTitle, barWidth - (textX - x) - 5);
                    g2.drawString(dispTitle, textX, y + g2.getFontMetrics().getAscent() + (TASK_BAR_HEIGHT - g2.getFontMetrics().getHeight()) / 2);
                }

                // 그린 일수만큼 확실하게 날짜를 미래로 이동시킴 (버그 원천 차단)
                renderDate = renderDate.plusDays(drawDays);
            }
        }
    }

    private void drawWeeklyTaskBars(Graphics2D g2, int cw, int ch) {
        List<Task> filteredTasks = calendarPanel.getFilteredAndSortedTasks();
        Map<LocalDate, Integer> laneAllocator = new HashMap<>();
        LocalDate weekEnd = calendarPanel.getCurrentWeekStart().plusDays(6);

        for (int i = 0; i < filteredTasks.size(); i++) {
            Task task = filteredTasks.get(i);
            LocalDate start = parseTaskDate(task, true);
            LocalDate end = parseTaskDate(task, false);
            if (start == null || end == null || start.isAfter(end)) continue;

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

            g2.setColor(task.getColor());

            // 주간 뷰 절대 좌표 렌더링
            int startCol = (int) ChronoUnit.DAYS.between(calendarPanel.getCurrentWeekStart(), drawStart);
            int drawDays = (int) ChronoUnit.DAYS.between(drawStart, drawEnd) + 1;

            int y = 30 + (allocatedLane * (TASK_BAR_HEIGHT + TASK_BAR_GAP + 2));
            int x = startCol * cw;
            int barWidth = drawDays * cw;

            boolean startsThisWeek = !start.isBefore(calendarPanel.getCurrentWeekStart());
            boolean endsThisWeek = !end.isAfter(weekEnd);

            if (startsThisWeek) { x += 4; barWidth -= 4; }
            if (endsThisWeek) { barWidth -= 4; }

            g2.fill(new RoundRectangle2D.Double(x, y, barWidth, TASK_BAR_HEIGHT + 2, 8, 8));

            if (!startsThisWeek) {
                g2.fill(new Rectangle2D.Double(x, y, 8, TASK_BAR_HEIGHT + 2));
            }
            if (!endsThisWeek) {
                g2.fill(new Rectangle2D.Double(x + barWidth - 8, y, 8, TASK_BAR_HEIGHT + 2));
            }

            g2.setFont(new Font("맑은 고딕", Font.BOLD, 11));
            g2.setColor(Color.WHITE);

            int textX = x + 10;
            if (!startsThisWeek) textX += 4;

            String dispTitle = ellipsize(g2, task.getTitle() + (task.isCompleted() ? " (완료)" : ""), barWidth - (textX - x) - 5);
            g2.drawString(dispTitle, textX, y + g2.getFontMetrics().getAscent() + (TASK_BAR_HEIGHT + 2 - g2.getFontMetrics().getHeight()) / 2);
        }
    }

    private LocalDate parseTaskDate(Task task, boolean startDate) {
        try {
            // 사용자의 Task 클래스 구현에 맞게 파싱
            return LocalDate.parse(startDate ? task.getStartDate() : task.getDeadline());
        } catch (DateTimeParseException e) {
            System.out.println("Task 날짜 형식 오류(" + task.getTitle() + "): " + e.getParsedString());
            return null;
        }
    }

    private String ellipsize(Graphics2D g2, String text, int maxWidth) {
        if (maxWidth <= 0 || g2.getFontMetrics().stringWidth(text) <= maxWidth) {
            return text;
        }

        String suffix = "..";
        int maxTextWidth = maxWidth - g2.getFontMetrics().stringWidth(suffix);
        if (maxTextWidth <= 0) {
            return suffix;
        }

        String result = text;
        while (result.length() > 1 && g2.getFontMetrics().stringWidth(result) > maxTextWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + suffix;
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