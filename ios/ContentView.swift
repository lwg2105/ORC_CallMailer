import SwiftUI

struct ContentView: View {
    @StateObject private var vm = MailerViewModel()

    var body: some View {
        NavigationView {
            VStack(spacing: 16) {
                Text("통화녹음 자동 메일발송 앱")
                    .font(.headline)

                Text(vm.statusMessage)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)

                Button("녹음 파일 선택 및 발송") {
                    vm.showFilePicker = true
                }
                .buttonStyle(.borderedProminent)
                .disabled(vm.isSending)

                if vm.isSending {
                    ProgressView("발송 중...")
                }

                Spacer()

                NavigationLink("설정") {
                    SettingsView()
                }
                .buttonStyle(.bordered)
            }
            .padding()
            .navigationTitle("통화녹음 메일발송")
            .fileImporter(
                isPresented: $vm.showFilePicker,
                allowedContentTypes: [.audio],
                allowsMultipleSelection: true
            ) { result in
                vm.handlePickedFiles(result)
            }
        }
    }
}