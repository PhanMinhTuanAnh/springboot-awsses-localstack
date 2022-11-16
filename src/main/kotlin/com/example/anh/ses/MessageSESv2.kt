package com.example.anh.ses

import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sesv2.SesV2Client
import software.amazon.awssdk.services.sesv2.model.*
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.util.*
import javax.mail.Session
import javax.mail.internet.MimeMessage


@Component
class SendMessageSESv2 {

    private val region = Region.AP_NORTHEAST_1

    private val stsClient: StsClient = StsClient.builder()
        .endpointOverride(URI.create("http://localhost:4566"))
        .region(region)
        .build()

    private val roleRequest = AssumeRoleRequest.builder()
        .roleArn("arn:aws:iam::764274372519:role/client-role")
        .roleSessionName("ANHNE").durationSeconds(900)
        .build()

    private val roleResponse = stsClient.assumeRole(roleRequest)

    private val credentials = roleResponse.credentials()

    private val credentialsProvider = StaticCredentialsProvider.create(
        AwsSessionCredentials.create(
            credentials.accessKeyId(),
            credentials.secretAccessKey(),
            credentials.sessionToken()
        )
    )

    private val client = SesV2Client.builder()
        .endpointOverride(URI.create("http://localhost:4566"))
        .region(region)
        .credentialsProvider(credentialsProvider)
        .build()

    fun send() {
        val sender = "anh@gmail.com"
        val recipients = arrayOf( "phan@gmail.com")
        val subject = "AWS SES Test limit"


        for (i in 1..1) {
            val bodyHTML = ("<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "Hello world!!!" +
                    "</body>\n")
            send(client, sender, recipients, subject, bodyHTML, i)
        }
    }

    fun send(
        sesv2Client: SesV2Client?,
        sender: String,
        recipients: Array<String>,
        subject: String,
        bodyHtml: String,
        waitKey: Int,
    ) {
        val session = Session.getDefaultInstance(Properties())

        val helper = MimeMessageHelper(MimeMessage(session), true, "UTF-8")

        helper.setFrom(sender)
        helper.setTo(recipients)
        helper.setSubject(subject)
        helper.setText(bodyHtml, true)

        val mimeMessage =helper.mimeMessage

        println("Attempting to send an email through Amazon SES using the AWS SDK for Java...")
        val outputStream = ByteArrayOutputStream()
        mimeMessage.writeTo(outputStream)

        val buf: ByteBuffer = ByteBuffer.wrap(outputStream.toByteArray())
        val arr = ByteArray(buf.remaining())
        buf.get(arr)

        val data = SdkBytes.fromByteArray(arr)

        val rawMessage = RawMessage.builder()
            .data(data)
            .build()

        val emailContent = EmailContent.builder()
            .raw(rawMessage)
            .build()

        val request = SendEmailRequest.builder()
            .content(emailContent)
            .feedbackForwardingEmailAddress(sender)
            .replyToAddresses("tuan@gmail.com")
            .build()

        sesv2Client!!.sendEmail(request)
    }
}