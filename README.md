<p align="center"><a href="https://auklet.io"><img src="https://s3.amazonaws.com/auklet/static/github_readme_java.png" alt="Auklet - Problem Solving Software for Java"></a></p>

# Auklet for Java
[![Maintainability](https://api.codeclimate.com/v1/badges/e1ced62442c7cf49d58a/maintainability)](https://codeclimate.com/github/aukletio/Auklet-Agent-Java/maintainability)

This is the Java agent for Auklet, officially supporting Java 7+.

# Features

* Automatic report of unhandled exceptions.
* Location, and system metrics identification for all issues.


# Quickstart

* Download the agent JAR using Maven or Gradle from https://bintray.com/aukletio/agent-java/auklet-agent-java.
* Configure your app ID and API key using the environment variables `AUKLET_APP_ID` and `AUKLET_API_KEY` or the
JVM system properties `auklet.app.id` and `auklet.api.key`.
* The agent needs a dedicated folder to create and store its configuration files. The agent will create these files 
on startup if they do not exist, or will use files that are already available on disk.
* If you are running the agent on an Android device, you will need to pass in your Android Context to the Config using
`new Config().setAndroidContext(getApplicationContext())`. You will also need to add
`<uses-permission android:name="android.permission.INTERNET" />` to your Android Manifest.

# Authorization

To authorize your application you need to provide both an API key and app ID.
These values are available in the connection settings of your application as well as during initial setup.

# Questions? Problems? Ideas?

To get support, report a bug or suggest future ideas for Auklet, go to https://help.auklet.io and click the blue button in the lower-right corner to send a message to our support team.
