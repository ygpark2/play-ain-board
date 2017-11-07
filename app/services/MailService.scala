package services

import javax.inject.Inject

import akka.actor.ActorSystem
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.mailer._

import play.api.libs.concurrent.ActorSystemProvider

import utils.ConfigSupport

import scala.concurrent.duration._
import scala.language.postfixOps

// internals

class MailService @Inject() (mailerClient: MailerClient,
                             system: ActorSystem,
                             val conf: Configuration) {

  lazy val from = conf.get[String]("play.mailer.from") // confRequiredString("play.mailer.from")

  def sendEmailAsync(recipients: String*)(subject: String, bodyHtml: String, bodyText: String) = {
    system.scheduler.scheduleOnce(100 milliseconds) {
      sendEmail(recipients: _*)(subject, bodyHtml, bodyText)
    }
  }

  def sendEmail(recipients: String*)(subject: String, bodyHtml: String, bodyText: String) = {

    val cid = "1234"
    val email = Email(
      subject,
      from,
      Seq("Miss TO <to@email.com>"),
      // adds attachment
      attachments = Seq(
        // AttachmentFile("attachment.pdf", new File("/some/path/attachment.pdf")),
        // adds inline attachment from byte array
        // AttachmentData("data.txt", "data".getBytes, "text/plain", Some("Simple data"), Some(EmailAttachment.INLINE)),
        // adds cid attachment
        // AttachmentFile("image.jpg", new File("/some/path/image.jpg"), contentId = Some(cid))
      ),
      // sends text, HTML or both...
      bodyText = Some(bodyText),
      bodyHtml = Some(bodyHtml)
    )
    mailerClient.send(email)
    // mailerClient.send(Email(subject, from, recipients, Some(bodyText), Some(bodyHtml)))
  }
}