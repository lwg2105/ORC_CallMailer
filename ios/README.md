# iOS 앱 (ORC 통화녹음 메일발송)

## 사용 방법

1. **Mac + Xcode 필요** — iOS 앱은 Mac에서만 빌드 가능합니다.
2. 이 `ios/` 폴더의 `.swift` 파일들로 Xcode 프로젝트를 생성하세요:
   - Xcode → File → New → Project → iOS → App
   - Product Name: `OrcCallMailer`, Interface: SwiftUI, Language: Swift
   - 기존 Swift 파일을 삭제하고 이 파일들을 프로젝트에 추가
3. Info.plist에 추가 필요:
   - `NSPhotoLibraryUsageDescription` (파일 접근용)
   - `Networking` capability

## 기능

- 파일 선택 (fileImporter) → m4a 파일 다중 선택
- SMTP STARTTLS로 자동 발송 (Gmail/Naver/Outlook 지원)
- 설정에서 SMTP 정보 저장

## Android 앱과의 차이

iOS는 타사 앱이 통화 녹음 폴더에 자동 접근할 수 없어서, 파일을 직접 선택하는 방식으로 동작합니다.