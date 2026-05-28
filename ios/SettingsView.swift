import SwiftUI

struct SettingsView: View {
    @StateObject private var vm = SettingsViewModel()

    var body: some View {
        Form {
            Section("SMTP 서버") {
                TextField("서버 (예: smtp.gmail.com)", text: $vm.host)
                    .autocapitalization(.none)
                TextField("포트 (기본: 587)", text: $vm.portStr)
                    .keyboardType(.numberPad)
                Text("Gmail: smtp.gmail.com:587\nNaver: smtp.naver.com:587\nOutlook: smtp.office365.com:587")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Section("계정") {
                TextField("발신 이메일", text: $vm.user)
                    .autocapitalization(.none)
                    .keyboardType(.emailAddress)
                SecureField("앱 비밀번호", text: $vm.password)
                TextField("수신자 이메일", text: $vm.recipient)
                    .autocapitalization(.none)
                    .keyboardType(.emailAddress)
                Text("Gmail/Naver는 일반 비밀번호 대신 앱 비밀번호를 사용하세요.\nGmail: Google 계정 → 보안 → 앱 비밀번호")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Section("기타") {
                TextField("제목 접두어", text: $vm.prefix)
            }
            Section {
                Button("저장") { vm.save() }
                    .frame(maxWidth: .infinity)
            }
        }
        .navigationTitle("설정")
        .alert("저장되었습니다", isPresented: $vm.saved) { Button("확인") {} }
    }
}

@MainActor
class SettingsViewModel: ObservableObject {
    @Published var host: String
    @Published var portStr: String
    @Published var user: String
    @Published var password: String
    @Published var recipient: String
    @Published var prefix: String
    @Published var saved = false

    private let prefs = AppPreferences()

    init() {
        host = prefs.smtpHost
        portStr = "\(prefs.smtpPort)"
        user = prefs.smtpUser
        password = prefs.smtpPassword
        recipient = prefs.recipientEmail
        prefix = prefs.subjectPrefix
    }

    func save() {
        prefs.smtpHost = host.trimmingCharacters(in: .whitespaces)
        prefs.smtpPort = Int(portStr) ?? 587
        prefs.smtpUser = user.trimmingCharacters(in: .whitespaces)
        prefs.smtpPassword = password
        prefs.recipientEmail = recipient.trimmingCharacters(in: .whitespaces)
        prefs.subjectPrefix = prefix.trimmingCharacters(in: .whitespaces)
        saved = true
    }
}