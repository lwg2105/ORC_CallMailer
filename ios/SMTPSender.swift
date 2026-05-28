import Foundation
import Network

actor SMTPSender {
    static func send(
        host: String, port: Int,
        user: String, password: String,
        to recipient: String,
        subject: String,
        attachmentName: String, attachmentData: Data
    ) async throws {
        let conn = NWConnection(
            host: NWEndpoint.Host(host),
            port: NWEndpoint.Port(rawValue: UInt16(port))!,
            using: .tcp
        )
        let session = SMTPSession(conn: conn)
        try await session.connect()
        try await session.handshake(host: host, user: user, password: password)
        try await session.sendMail(
            from: user, to: recipient, subject: subject,
            attachmentName: attachmentName, attachmentData: attachmentData
        )
        try await session.quit()
    }
}

private actor SMTPSession {
    private let conn: NWConnection
    private var buffer = Data()

    init(conn: NWConnection) { self.conn = conn }

    func connect() async throws {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            conn.stateUpdateHandler = { state in
                switch state {
                case .ready: cont.resume()
                case .failed(let e): cont.resume(throwing: e)
                default: break
                }
            }
            conn.start(queue: .global())
        }
        _ = try await readLine()  // 220 greeting
    }

    func handshake(host: String, user: String, password: String) async throws {
        try await send("EHLO \(host)\r\n"); _ = try await readUntilBlankLine()
        try await send("STARTTLS\r\n"); _ = try await readLine()
        // Upgrade to TLS
        let tlsOpts = NWProtocolTLS.Options()
        sec_protocol_options_set_verify_block(tlsOpts.securityProtocolOptions, { _, _, sec in
            sec(true)
        }, .global())
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            conn.stateUpdateHandler = { state in
                if case .ready = state { cont.resume() }
                if case .failed(let e) = state { cont.resume(throwing: e) }
            }
        }
        try await send("EHLO \(host)\r\n"); _ = try await readUntilBlankLine()
        try await send("AUTH LOGIN\r\n"); _ = try await readLine()
        let b64user = Data(user.utf8).base64EncodedString()
        let b64pass = Data(password.utf8).base64EncodedString()
        try await send("\(b64user)\r\n"); _ = try await readLine()
        try await send("\(b64pass)\r\n"); _ = try await readLine()
    }

    func sendMail(from: String, to: String, subject: String, attachmentName: String, attachmentData: Data) async throws {
        try await send("MAIL FROM:<\(from)>\r\n"); _ = try await readLine()
        try await send("RCPT TO:<\(to)>\r\n"); _ = try await readLine()
        try await send("DATA\r\n"); _ = try await readLine()
        let boundary = "boundary_\(UUID().uuidString)"
        var msg = "From: \(from)\r\nTo: \(to)\r\nSubject: \(subject)\r\n"
        msg += "MIME-Version: 1.0\r\nContent-Type: multipart/mixed; boundary=\"\(boundary)\"\r\n\r\n"
        msg += "--\(boundary)\r\nContent-Type: text/plain; charset=utf-8\r\n\r\n통화녹음 파일을 전송합니다.\r\n\r\n"
        msg += "--\(boundary)\r\nContent-Type: audio/mp4; name=\"\(attachmentName)\"\r\n"
        msg += "Content-Transfer-Encoding: base64\r\nContent-Disposition: attachment; filename=\"\(attachmentName)\"\r\n\r\n"
        msg += attachmentData.base64EncodedString(options: .lineLength76Characters)
        msg += "\r\n--\(boundary)--\r\n.\r\n"
        try await send(msg)
        _ = try await readLine()
    }

    func quit() async throws {
        try await send("QUIT\r\n")
        conn.cancel()
    }

    private func send(_ text: String) async throws {
        let data = Data(text.utf8)
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            conn.send(content: data, completion: .contentProcessed { err in
                if let e = err { cont.resume(throwing: e) } else { cont.resume() }
            })
        }
    }

    private func readLine() async throws -> String {
        while true {
            if let range = buffer.range(of: Data("\r\n".utf8)) {
                let line = String(data: buffer[..<range.lowerBound], encoding: .utf8) ?? ""
                buffer.removeSubrange(..<range.upperBound)
                return line
            }
            let chunk = try await receiveChunk()
            buffer.append(chunk)
        }
    }

    private func readUntilBlankLine() async throws -> String {
        var result = ""
        while true {
            let line = try await readLine()
            result += line + "\n"
            if line.count >= 4 && line[line.index(line.startIndex, offsetBy: 3)] == " " { break }
        }
        return result
    }

    private func receiveChunk() async throws -> Data {
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Data, Error>) in
            conn.receive(minimumIncompleteLength: 1, maximumLength: 4096) { data, _, _, err in
                if let e = err { cont.resume(throwing: e) }
                else { cont.resume(returning: data ?? Data()) }
            }
        }
    }
}