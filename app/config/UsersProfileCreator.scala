package config

import org.pac4j.core.context.Pac4jConstants
import org.pac4j.core.context.WebContext
import org.pac4j.core.credentials.UsernamePasswordCredentials
import org.pac4j.core.exception.HttpAction
import org.pac4j.core.profile.CommonProfile
import org.pac4j.core.profile.creator.ProfileCreator


class UsersProfileCreator extends ProfileCreator[UsernamePasswordCredentials, CommonProfile] {
  @throws[HttpAction]
  override def create(credentials: UsernamePasswordCredentials, context: WebContext): CommonProfile = {
    println("----------- create profile ------------------------")
    val userProfile = credentials.getUserProfile()
    println("user profile => " + userProfile)
    userProfile
    // println("----------- end profile ------------------------")
  }
}