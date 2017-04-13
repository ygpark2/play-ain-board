name := """play-ain-board"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

// routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Seq(
    cache,
    ws,
    filters,
    "com.typesafe.play" %% "play-slick" % "2.0.2",
    "com.typesafe.play" %% "play-slick-evolutions" % "2.0.2",
    "com.h2database" % "h2" % "1.4.192",

    "com.typesafe.play" %% "play-mailer" % "5.0.0",
    "com.github.t3hnar" %% "scala-bcrypt" % "3.0",

    "org.pac4j" % "play-pac4j" % "3.0.0-RC2-SNAPSHOT",
    "org.pac4j" % "pac4j-http" % "2.0.0-RC2-SNAPSHOT",
    // "org.pac4j" % "pac4j-cas" % "2.0.0-RC2-SNAPSHOT",
    "org.pac4j" % "pac4j-openid" % "2.0.0-RC2-SNAPSHOT" exclude("xml-apis" , "xml-apis"),
    "org.pac4j" % "pac4j-oauth" % "2.0.0-RC2-SNAPSHOT",
    // "org.pac4j" % "pac4j-saml" % "2.0.0-RC2-SNAPSHOT",
    // "org.pac4j" % "pac4j-oidc" % "2.0.0-RC2-SNAPSHOT" exclude("commons-io" , "commons-io"),
    // "org.pac4j" % "pac4j-gae" % "2.0.0-RC2-SNAPSHOT",
    "org.pac4j" % "pac4j-jwt" % "2.0.0-RC2-SNAPSHOT" exclude("commons-io" , "commons-io"),
    // "org.pac4j" % "pac4j-ldap" % "2.0.0-RC2-SNAPSHOT",
    "org.pac4j" % "pac4j-sql" % "2.0.0-RC2-SNAPSHOT",
    // "org.pac4j" % "pac4j-mongo" % "2.0.0-RC2-SNAPSHOT",
    // "org.pac4j" % "pac4j-stormpath" % "2.0.0-RC2-SNAPSHOT",
    "com.typesafe.play" % "play-cache_2.11" % "2.5.4",
    "commons-io" % "commons-io" % "2.5",

    "org.webjars" %% "webjars-play" % "2.5.0",
    "org.webjars" % "jquery" % "2.2.4",
    "org.webjars" % "material-design-icons" % "3.0.1",
    "org.webjars" % "material-design-lite" % "1.3.0",

    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test",
    specs2 % Test
)

resolvers ++= Seq(
    Resolver.mavenLocal,
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    "Sonatype snapshots repository" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

// fork in run := true