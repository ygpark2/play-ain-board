// Comment to get more information during initialization
logLevel := Level.Warn


resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.5.10")

addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.6")

addSbtPlugin("com.jamesward" %% "play-auto-refresh" % "0.0.15")

