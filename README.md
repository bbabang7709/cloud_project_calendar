# Cloud Project Calendar

구글 드라이브의 **로컬 동기화 기능**을 기반으로 팀의 프로젝트, 업무(Task), 일정을 함께 관리하는 Java Swing 데스크톱 애플리케이션입니다.

별도의 서버나 데이터베이스를 사용하지 않고, 구글 드라이브가 PC에 동기화한 로컬 폴더에 텍스트 파일을 저장합니다. 여러 사용자가 동일한 구글 드라이브 공유 폴더를 각자의 PC에 로컬 동기화하도록 설정하면, 파일 변경을 주기적으로 감지해 프로젝트 데이터를 다시 읽고 화면을 갱신합니다.

> 대학 프로젝트용 프로토타입입니다. 서버 기반 협업 도구와 달리 동시 편집 충돌 해결이나 강력한 인증 기능은 제공하지 않습니다.

## 주요 기능

- 이름·비밀번호 기반 로그인 및 신규 사용자 자동 가입
- 로그인 화면에서 구글 드라이브 로컬 동기화 폴더 선택
- 관리자(Admin), 팀장(Leader), 팀원(Member) 역할 구분
- 팀 및 프로젝트 생성·삭제
- Task 생성, 삭제, 완료/진행 상태 변경
- Task 시작일·마감일과 색상을 표시하는 월간/주간 캘린더
- 팀·프로젝트별 캘린더 필터
- 마감일 당일 또는 기한이 지난 미완료 Task 알림
- 공유 폴더의 외부 파일 변경 감지 및 자동 새로고침
- 관리자용 사용자 역할·팀 배정 관리
- 팀장용 미소속 사용자 팀원 지정

## 실행 환경

- Java JDK 8 이상
- Java Swing
- Maven/Gradle 없이 순수 Java 소스만으로 구성
- GUI 환경이 필요하므로 헤드리스 환경에서는 실행할 수 없음

## 실행 방법

### Windows PowerShell

프로젝트 루트에서 다음 명령을 실행합니다.

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java .\src | ForEach-Object { $_.FullName })
java -cp out CloudProjectCalendarApp
```

### macOS/Linux

```bash
javac -d out $(find src -name "*.java")
java -cp out CloudProjectCalendarApp
```

프로그램을 실행하면 로그인 창이 열립니다. 이름과 비밀번호를 입력하고, 데이터를 저장할 폴더를 선택한 뒤 `접속 / 신규가입`을 누르세요.

## 기본 계정 및 가입

처음 사용하는 저장 폴더에는 다음 관리자 계정이 자동으로 생성됩니다.

```text
이름: admin
비밀번호: 1111
```

존재하지 않는 이름을 입력하면 팀 미소속 팀원으로 자동 가입됩니다. 이미 등록된 이름은 저장된 비밀번호와 일치해야 로그인할 수 있습니다.

## 역할별 권한

| 역할 | 기본 권한 |
| --- | --- |
| 관리자 | 모든 팀 조회, 팀 생성·삭제, 모든 프로젝트 관리, 사용자 역할·팀 배정 |
| 팀장 | 소속 팀 조회, 소속 팀 프로젝트 생성·삭제, 소속 팀 Task 관리, 미소속 사용자 영입 |
| 팀원 | 소속 팀 조회, 소속 팀 Task 생성·삭제·완료 처리 |

관리자가 아닌 사용자는 로그인한 계정의 소속 팀만 조회하고 관리할 수 있습니다. 팀이 지정되지 않은 사용자는 팀 배정 전까지 대기 상태로 표시됩니다.

## 프로젝트 구조

```text
src/
├── CloudProjectCalendarApp.java   # 프로그램 시작점
├── gui/                           # Swing 화면과 사용자 입력 처리
│   ├── LoginFrame.java
│   ├── MainFrame.java
│   ├── CalendarPanel.java
│   ├── CalendarCanvas.java
│   ├── TeamPanel.java
│   ├── ProjectPanel.java
│   ├── TaskPanel.java
│   ├── UserManagementPanel.java
│   └── TeamLeaderPanel.java
├── manager/                       # 데이터·권한·프로젝트 관리
│   ├── DataManager.java
│   ├── PermissionManager.java
│   └── ProjectManager.java
├── model/                         # User, Team, Project, Task 도메인 모델
└── thread/                        # 파일 동기화·마감 알림 백그라운드 스레드
    ├── CloudSyncThread.java
    └── NotificationThread.java
```

## 데이터 저장 방식

로그인 화면에서 구글 드라이브의 로컬 동기화 폴더를 선택하면 프로그램은 선택한 폴더 아래에 `scheduler_data` 디렉터리를 만들고 다음 파일을 사용합니다.

```text
선택한_폴더/
└── scheduler_data/
    ├── system_meta/
    │   ├── users.txt
    │   └── project_master.txt
    └── user_tasks/
        ├── admin_tasks.txt
        ├── 사용자이름_tasks.txt
        └── ...
```

파일 형식은 `|` 구분자를 사용하는 단순 텍스트 형식입니다.

- `system_meta/users.txt`: 사용자 이름, 비밀번호, 역할, 소속 팀
- `system_meta/project_master.txt`: 팀 이름, 프로젝트 ID, 프로젝트 이름
- `user_tasks/{사용자이름}_tasks.txt`: 해당 사용자가 만든 Task의 프로젝트 ID, 제목, 시작일, 마감일, 완료 여부, 색상

날짜는 `YYYY-MM-DD` 형식으로 저장합니다. 프로젝트 ID는 프로젝트 생성 시 `P_` 접두사와 짧은 UUID를 조합해 자동 생성됩니다.

## 구글 드라이브 로컬 동기화 및 알림 동작

이 프로젝트는 구글 드라이브 API를 직접 호출하지 않습니다. 구글 드라이브 데스크톱 앱이 공유 폴더의 파일을 각 사용자의 PC에 동기화하면, 애플리케이션은 해당 로컬 폴더의 파일을 읽고 씁니다.

- `CloudSyncThread`는 3초마다 구글 드라이브 로컬 동기화 폴더 안의 `project_master.txt`와 사용자별 Task 파일의 수정 시간을 확인합니다.
- 외부 변경이 발견되면 `ProjectManager`가 데이터를 다시 읽고 Swing 이벤트 스레드에서 모든 화면을 갱신합니다.
- `NotificationThread`는 5초마다 모든 미완료 Task를 확인합니다.
- 현재 날짜가 Task 기간 안에 있고 마감일이 오늘이거나 지난 경우, 같은 Task에 대해 중복되지 않도록 경고 팝업을 표시합니다.
- `수동 동기화` 버튼은 파일 변경 감지 플래그를 발생시켜 다음 감시 주기에 데이터를 갱신하게 합니다.

## 개발 시 참고

수정 후에는 위의 컴파일 명령으로 전체 소스를 다시 빌드한 다음 실행합니다. 프로그램은 Swing 이벤트 디스패치 스레드에서 화면을 갱신하며, 데이터 관리 클래스는 동기화 메서드로 메모리 데이터와 파일 접근을 보호합니다.

## 알려진 제약 사항

- 구글 드라이브 API, 중앙 서버, 데이터베이스를 직접 사용하지 않습니다. 구글 드라이브 데스크톱 앱의 로컬 동기화 기능에 의존합니다.
- 여러 사용자가 같은 파일을 동시에 수정하면 마지막 저장 내용이 남을 수 있습니다.
- 비밀번호가 평문 텍스트로 저장됩니다.
- `|`가 구분자이므로 이름, 팀명, 프로젝트명, Task 제목 등에 `|`를 입력하면 파일 파싱에 문제가 생길 수 있습니다.
- 날짜 형식이 `YYYY-MM-DD`가 아니면 캘린더 표시나 알림에서 해당 Task가 제외될 수 있습니다.
- 사용자가 폴더를 선택할 때마다 그 폴더 아래의 `scheduler_data`를 사용하므로, 공유할 때는 모든 사용자가 동일한 구글 드라이브 공유 폴더를 로컬 동기화하고 동일한 상위 폴더를 선택해야 합니다.
- 자동화된 테스트와 Maven/Gradle 빌드 설정은 포함되어 있지 않습니다.

## 개선 아이디어

- 비밀번호 해시 및 사용자 인증 강화
- JSON/CSV 또는 데이터베이스 기반 저장소 도입
- 파일 잠금·충돌 병합 등 동시 편집 처리
- 입력값 검증 및 구분자 이스케이프 처리
- Maven/Gradle과 JUnit 기반 빌드·테스트 환경 추가
- 실제 클라우드 API 또는 중앙 서버 기반 동기화로 확장
