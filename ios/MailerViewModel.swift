import Foundation
import SwiftUI
import UniformTypeIdentifiers

@MainActor
class MailerViewModel: ObservableObject {
    @Published var statusMessage = "파일을 선택하면 자동으로 메일을 발송합니다."
    @Published var showFilePicker = false
    @Published var isSending = false

    private let prefs = AppPreferences()

    func handlePickedFiles(_ result: Result<[URL], Error>) {
        switch result {
        case .failure(let err):
            statusMessage = "파일 선택 실패: \(err.localizedDescription)"
        case .success(let urls):
            guard !urls.isEmpty else { return }
            Task { await sendFiles(urls) }
        }
    }

    private func sendFiles(_ urls: [URL]) async {
        guard prefs.isConfigured else {
            statusMessage = "설정에서 SMTP 정보를 먼저 입력하세요."
            return
        }
        isSending = true
        var sent = 0
        var failed = 0
        for url in urls {
            _ = url.startAccessingSecurityScopedResource()
            defer { url.stopAccessingSecurityScopedResource() }
            do {
                let data = try Data(contentsOf: url)
                let filename = url.lastPathComponent
                let subject = "\(prefs.subjectPrefix) \(filename)"
                try await SMTPSender.send(
                    host: prefs.smtpHost, port: prefs.smtpPort,
                    user: prefs.smtpUser, password: prefs.smtpPassword,
                    to: prefs.recipientEmail,
                    subject: subject,
                    attachmentName: filename, attachmentData: data
                )
                sent += 1
            } catch {
                failed += 1
            }
        }
        isSending = false
        statusMessage = "완료: \(sent)개 발송" + (failed > 0 ? ", \(failed)개 실패" : "")
    }
}