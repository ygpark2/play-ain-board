name := """play-ain-board"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.3"

val playPac4jVersion = "4.0.0"
val pac4jVersion = "2.1.0"
val playVersion = "2.6.6"

scalacOptions := Seq("-feature", "-deprecation")

// routesGenerator := StaticRoutesGenerator
routesGenerator := InjectedRoutesGenerator

resolvers ++= Seq(
    Resolver.mavenLocal,
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    "Sonatype snapshots repository" at "https://oss.sonatype.org/content/repositories/snapshots/"
)

libraryDependencies ++= Seq(
    guice,
    ehcache, // or cacheApi
    ws,
    filters,
    "com.typesafe.play" %% "play-slick" % "3.0.1",
    "com.typesafe.play" %% "play-slick-evolutions" % "3.0.1",
    "com.h2database" % "h2" % "1.4.192",

    "com.typesafe.play" %% "play-mailer" % "6.0.1",
    "com.github.t3hnar" %% "scala-bcrypt" % "3.0",

    "org.pac4j" % "play-pac4j" % playPac4jVersion,
    "org.pac4j" % "pac4j-http" % pac4jVersion,
    // "org.pac4j" % "pac4j-cas" % "4.0.0",
    "org.pac4j" % "pac4j-openid" % pac4jVersion exclude("xml-apis" , "xml-apis"),
    "org.pac4j" % "pac4j-oauth" % pac4jVersion,
    // "org.pac4j" % "pac4j-saml" % "4.0.0",
    // "org.pac4j" % "pac4j-oidc" % "4.0.0" exclude("commons-io" , "commons-io"),
    // "org.pac4j" % "pac4j-gae" % "4.0.0",
    "org.pac4j" % "pac4j-jwt" % pac4jVersion exclude("commons-io" , "commons-io"),
    // "org.pac4j" % "pac4j-ldap" % "4.0.0",
    "org.pac4j" % "pac4j-sql" % pac4jVersion,
    // "org.pac4j" % "pac4j-mongo" % "4.0.0",
    // "org.pac4j" % "pac4j-stormpath" % "4.0.0",
    "com.typesafe.play" % "play-cache_2.12" % playVersion,
    "commons-io" % "commons-io" % "2.5",

    "org.webjars" %% "webjars-play" % "2.6.1",
    "org.webjars" % "jquery" % "2.2.4",
    "org.webjars" % "material-design-icons" % "3.0.1",
    "org.webjars" % "material-design-lite" % "1.3.0",

    // "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test",
    specs2 % Test
)

lazy val webJarsPlay = RootProject(file("..").getAbsoluteFile.toURI)

lazy val root = (project in file(".")).enablePlugins(PlayScala).dependsOn(webJarsPlay)

// fork in run := true