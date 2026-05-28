import Foundation

class AppPreferences {
    private let defaults = UserDefaults.standard

    var smtpHost: String {
        get { defaults.string(forKey: "smtp_host") ?? "" }
        set { defaults.set(newValue, forKey: "smtp_host") }
    }
    var smtpPort: Int {
        get { defaults.integer(forKey: "smtp_port") == 0 ? 587 : defaults.integer(forKey: "smtp_port") }
        set { defaults.set(newValue, forKey: "smtp_port") }
    }
    var smtpUser: String {
        get { defaults.string(forKey: "smtp_user") ?? "" }
        set { defaults.set(newValue, forKey: "smtp_user") }
    }
    var smtpPassword: String {
        get { defaults.string(forKey: "smtp_password") ?? "" }
        set { defaults.set(newValue, forKey: "smtp_password") }
    }
    var recipientEmail: String {
        get { defaults.string(forKey: "recipient_email") ?? "" }
        set { defaults.set(newValue, forKey: "recipient_email") }
    }
    var subjectPrefix: String {
        get { defaults.string(forKey: "subject_prefix") ?? "[ORC통화녹음]" }
        set { defaults.set(newValue, forKey: "subject_prefix") }
    }

    var isConfigured: Bool {
        !smtpHost.isEmpty && !smtpUser.isEmpty && !smtpPassword.isEmpty && !recipientEmail.isEmpty
    }
}