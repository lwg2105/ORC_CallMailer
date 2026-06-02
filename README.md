# 통화녹음 자동 메일발송 앱 (ORC_CallMailer)

삼성 갤럭시의 통화녹음 파일(.m4a)을 감지하여 지정한 이메일로 자동 발송하는 Android 앱.

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 상시 감시 모드 | FileObserver로 녹음 폴더를 실시간 감시, 새 파일 즉시 발송 |
| 예약 실행 모드 | 지정 시각에 미발송 파일 일괄 처리 후 자동 종료 |
| 중복 방지 | 발송 완료 파일명을 SharedPreferences에 저장, 재실행 시 건너뜀 |
| 이름 필터 | 특정 이름이 포함된 파일만 발송 (토글 on/off) |
| 폴더 선택 | 트리 브라우저로 감시 폴더 직접 선택 |
| 인앱 업데이트 알림 | 앱 실행 시 최신 릴리즈 확인, 새 버전 있으면 다운로드 버튼 표시 |
| 오류 표시 | 발송 실패 시 메인 화면에 오류 내용 표시 (탭하면 닫힘) |

---

## 설치 및 업데이트

### 최초 설치
1. [최신 릴리즈](https://github.com/lwg2105/ORC_CallMailer/releases/latest)에서 `app-debug.apk` 다운로드
2. 설치 (출처를 알 수 없는 앱 허용 필요)
3. 앱 실행 → **"모든 파일 접근"** 권한 허용 (자동으로 설정 화면으로 이동)

### 업데이트
앱 실행 시 새 버전이 있으면 파란 버튼 **"업데이트 vXX 다운로드"** 가 자동으로 표시됨  
탭하면 APK 다운로드 → 설치 (기존 설정 및 발송 기록 유지됨)

---

## 설정

**설정 버튼** → SMTP 서버 / 계정 / 감시 폴더 / 이름 필터

### SMTP 서버 정보

| 제공사 | SMTP 서버 | 포트 |
|--------|-----------|------|
| Gmail | smtp.gmail.com | 587 |
| Naver | smtp.naver.com | 587 |
| Outlook | smtp.office365.com | 587 |
| Kakao | smtp.kakao.com | 587 |

> **Gmail / Naver 주의**: 일반 비밀번호 대신 **앱 비밀번호** 사용 필요  
> - Gmail: Google 계정 → 보안 → 2단계 인증 → 앱 비밀번호  
> - Naver: 네이버 계정 → 보안설정 → 앱 비밀번호 설정

### 감시 폴더 기본 경로
```
/storage/emulated/0/Recordings/Call
```
삼성 갤럭시 기준. "찾기" 버튼으로 실제 경로 직접 탐색 가능.

---

## 동작 구조

```
앱 시작
 ├── 권한 확인 (MANAGE_EXTERNAL_STORAGE)
 ├── [상시 감시 모드]
 │    ├── 앱 꺼진 사이 미발송 파일 일괄 처리 (catchUp)
 │    └── FileObserver → 새 .m4a 감지 → 즉시 발송
 └── [예약 실행 모드]
      └── AlarmManager → 지정 시각에 일괄 처리 → 다음 날 재예약
```

**중복 방지**: 발송 성공 시 파일명을 SharedPreferences에 저장 → 재실행 시 건너뜀 (최대 1000개 유지)

**파일명 이름 파싱 (이름 필터용)**:
- Format A: `YYYYMMDD_HHMMSS_이름.m4a` → 세 번째 파트
- Format B: `이름_YYMMDD_HHMMSS.m4a` 또는 `통화 녹음 이름_YYMMDD_HHMMSS.m4a` → 첫 번째 파트

---

## 파일 구조

```
app/src/main/java/com/orc/callmailer/
├── MainActivity.kt           # 메인 화면, 권한 요청, 업데이트 체크
├── SettingsActivity.kt       # SMTP 설정, 폴더 선택, 이름 필터
├── CallRecordingService.kt   # 상시 감시 ForegroundService
├── ScheduledUploadService.kt # 예약 실행 ForegroundService
├── ScheduledAlarmReceiver.kt # AlarmManager BroadcastReceiver
├── BootReceiver.kt           # 부팅 시 서비스 자동 시작
├── EmailSender.kt            # JavaMail SMTP 발송 로직
├── PreferencesHelper.kt      # 설정·발송기록·오류 SharedPreferences 래퍼
└── UpdateChecker.kt          # GitHub Releases API 버전 비교
```

---

## 자동 빌드

`main` 브랜치 push 시 GitHub Actions가 자동으로:
1. APK 빌드 (run number → `versionCode` / `versionName`)
2. GitHub Release 생성 및 APK 업로드

앱은 `BuildConfig.VERSION_CODE`와 GitHub Releases API 최신 태그를 비교해 업데이트 여부 판단.