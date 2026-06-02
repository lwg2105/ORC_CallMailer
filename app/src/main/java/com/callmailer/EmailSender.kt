package com.callmailer

import android.util.Log
import java.io.File
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

private const val TAG = "EmailSender"

data class SmtpConfig(
    val host: String,
    val port: Int,
    val senderEmail: String,
    val senderPassword: String,
    val recipientEmail: String,
)

fun sendCallRecording(config: SmtpConfig, file: File) {
    val props = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", config.host)
        put("mail.smtp.port", config.port.toString())
        put("mail.smtp.ssl.trust", config.host)
        put("mail.smtp.connectiontimeout", "30000")
        put("mail.smtp.timeout", "60000")
        put("mail.smtp.writetimeout", "120000")
    }

    val session = Session.getInstance(props, object : Authenticator() {
        override fun getPasswordAuthentication() =
            PasswordAuthentication(config.senderEmail, config.senderPassword)
    })

    val message = MimeMessage(session).apply {
        setFrom(InternetAddress(config.senderEmail))
        setRecipient(Message.RecipientType.TO, InternetAddress(config.recipientEmail))
        subject = "[ORC통화녹음] ${file.name}"

        val multipart = MimeMultipart()

        val textPart = MimeBodyPart().apply {
            setText("통화녹음 파일이 첨부되었습니다.\n파일명: ${file.name}\n크기: ${file.length() / 1024}KB")
        }
        multipart.addBodyPart(textPart)

        val filePart = MimeBodyPart().apply {
            attachFile(file)
            fileName = file.name
        }
        multipart.addBodyPart(filePart)

        setContent(multipart)
    }

    Transport.send(message)
    Log.i(TAG, "Sent: ${file.name}")
}
