package config

import org.mindrot.jbcrypt.BCrypt
import org.pac4j.core.credentials.password.PasswordEncoder
import com.github.t3hnar.bcrypt._

class BlowfishPasswordEncoder extends PasswordEncoder {
  override def encode(password: String): String = BCrypt.hashpw(password, BCrypt.gensalt)
  def encode(password: String, salt: String): String = password.bcrypt(salt)
  override def matches(plainPassword: String, encodedPassword: String): Boolean = plainPassword.isBcrypted(encodedPassword)
}