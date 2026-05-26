import java.time.LocalDate;
import java.awt.Color;

public class Task {
	private String projectId; // 속한 프로젝트 ID (파일 저장 시 필요)
	private String ownerName; // 이 Task를 생성한 사람 (파일명 구분용)
	private String title;
	private String startDate; // YYYY-MM-DD
	private String deadline;
	private boolean completed;
	private Color color;
	
	public Task(String projectId, String ownerName, String title, String startDate, String deadline, Color color) {
		this.projectId = projectId;
		this.ownerName = ownerName;
		this.title = title;
		this.startDate = startDate;
		this.deadline = deadline;
		this.completed = false;
		this.color = color;
	}
	
	public String getProjectId() { return projectId; }
	public String getOwnerName() { return ownerName; }
	public String getTitle() { return title; }
	public String getStartDate() { return startDate; }
	public String getDeadline() { return deadline; }
	public boolean isCompleted() { return completed; }
	public void setCompleted(boolean state) {
		this.completed = state;
	}
	public Color getColor() { return color; }
	
	public boolean isWithinRange(LocalDate date) {
		try {
			LocalDate start = LocalDate.parse(startDate);
			LocalDate end = LocalDate.parse(deadline);
			
			if (date.isEqual(start) || date.isEqual(end)) {
				return true;
			} else if (date.isAfter(start) && date.isBefore(end)) {
				return true;
			}
		} catch (Exception e) {
			System.out.println("날짜 파싱 에러 발생: " + e.getMessage());
		}
		return false;
	}
	
	@Override
	public String toString() {
		String status = "";
		if (completed) {
			status = "[완료] ";
		} else {
			status = "[진행중] ";
		}
		
		return status + title + "(" + startDate + " ~ " + deadline + ")";
	}
}
