package utils

import play.api.i18n.Messages
import play.twirl.api.Html
import models.User
import services.MailService
import views.html.mails
import scala.language.implicitConversions

object Mailer {

  implicit def html2String(html: Html): String = html.toString

  def welcome(user: User, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(user.email)(
      subject = Messages("mail.welcome.subject"),
      bodyHtml = views.html.mails.welcome(user.name, link),
      bodyText = views.html.mails.welcomeTxt(user.name, link)
    )
  }

  def forgotPassword(email: String, link: String)(implicit ms: MailService, m: Messages) {
    ms.sendEmailAsync(email)(
      subject = Messages("mail.forgotpwd.subject"),
      bodyHtml = views.html.mails.forgotPassword(email, link),
      bodyText = views.html.mails.forgotPasswordTxt(email, link)
    )
  }

}