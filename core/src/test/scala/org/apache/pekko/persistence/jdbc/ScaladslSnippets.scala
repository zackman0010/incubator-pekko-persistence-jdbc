/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2014 - 2019 Dennis Vriend <https://github.com/dnvriend>
 * Copyright (C) 2019 - 2021 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko.persistence.jdbc

import org.apache.pekko
import pekko.{ Done, NotUsed }
import pekko.actor.ActorSystem
import pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
import pekko.persistence.jdbc.testkit.scaladsl.SchemaUtils

import scala.concurrent.Future

object ScaladslSnippets {

  def create(): Unit = {
    // #create

    implicit val system: ActorSystem = ActorSystem("example")
    val done: Future[Done] = SchemaUtils.createIfNotExists()
    // #create
  }

  def readJournal(): Unit = {
    implicit val system: ActorSystem = ActorSystem()

    // #read-journal
    import org.apache.pekko.persistence.query.PersistenceQuery

    val readJournal: JdbcReadJournal =
      PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)
    // #read-journal
  }

  def persistenceIds(): Unit = {
    implicit val system: ActorSystem = ActorSystem()

    // #persistence-ids
    import org.apache
    import pekko.stream.scaladsl.Source
    import pekko.persistence.query.PersistenceQuery

    val readJournal: JdbcReadJournal =
      PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

    val willNotCompleteTheStream: Source[String, NotUsed] = readJournal.persistenceIds()

    val willCompleteTheStream: Source[String, NotUsed] = readJournal.currentPersistenceIds()
    // #persistence-ids
  }

  def eventsByPersistenceId(): Unit = {
    implicit val system: ActorSystem = ActorSystem()

    // #events-by-persistence-id
    import org.apache.pekko
    import pekko.stream.scaladsl.Source
    import pekko.persistence.query.{ EventEnvelope, PersistenceQuery }

    val readJournal: JdbcReadJournal =
      PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

    val willNotCompleteTheStream: Source[EventEnvelope, NotUsed] =
      readJournal.eventsByPersistenceId("some-persistence-id", 0L, Long.MaxValue)

    val willCompleteTheStream: Source[EventEnvelope, NotUsed] =
      readJournal.currentEventsByPersistenceId("some-persistence-id", 0L, Long.MaxValue)
    // #events-by-persistence-id
  }

  def eventsByTag(): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    // #events-by-tag
    import org.apache.pekko
    import pekko.stream.scaladsl.Source
    import pekko.persistence.query.{ EventEnvelope, PersistenceQuery }

    val readJournal: JdbcReadJournal =
      PersistenceQuery(system).readJournalFor[JdbcReadJournal](JdbcReadJournal.Identifier)

    val willNotCompleteTheStream: Source[EventEnvelope, NotUsed] = readJournal.eventsByTag("apple", 0L)

    val willCompleteTheStream: Source[EventEnvelope, NotUsed] = readJournal.currentEventsByTag("apple", 0L)
    // #events-by-tag
  }
}
