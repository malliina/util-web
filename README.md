[![Build Status](https://travis-ci.org/malliina/util-play.png?branch=master)](https://travis-ci.org/malliina/util-play)
[![Maven Central](https://img.shields.io/maven-central/v/com.malliina/util-play_2.11.svg)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.malliina%22%20AND%20a%3A%22util-play_2.11%22)

A set of modules I find helpful when building applications.

# util-play

Useful code for Play Framework. A selection of what is provided:

- A SyncAction for synchronous IO, using a more suitable threadpool than the default one
- A WebSocketBase trait for WebSocket-enabled applications
- Content negotiation helpers
- Code that starts Play! without creating a RUNNING_PID file

# play-social

Social login implementations for the following service providers:

- Google
- Facebook
- Microsoft
- Twitter
- GitHub
- Amazon

# util-html

[Scalatags](http://www.lihaoyi.com/scalatags/) helpers for [Bootstrap 4](https://getbootstrap.com). Supports 
[Scala.js](https://www.scala-js.org) in addition to the JVM.
