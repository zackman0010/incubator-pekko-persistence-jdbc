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
import pekko.actor.ActorSystem
import pekko.util.Timeout
import com.typesafe.config.{ Config, ConfigFactory, ConfigValue }
import pekko.persistence.jdbc.config.{ JournalConfig, ReadJournalConfig, SlickConfiguration }
import pekko.persistence.jdbc.db.SlickDatabase
import pekko.persistence.jdbc.query.scaladsl.JdbcReadJournal
import pekko.persistence.jdbc.util.DropCreate
import org.scalatest.BeforeAndAfterEach
import slick.jdbc.JdbcBackend.Database

import scala.concurrent.duration._

abstract class SingleActorSystemPerTestSpec(val config: Config)
    extends SimpleSpec
    with DropCreate
    with BeforeAndAfterEach {
  def this(config: String = "postgres-application.conf", configOverrides: Map[String, ConfigValue] = Map.empty) =
    this(configOverrides.foldLeft(ConfigFactory.load(config)) { case (conf, (path, configValue)) =>
      conf.withValue(path, configValue)
    })

  implicit val pc: PatienceConfig = PatienceConfig(timeout = 1.minute)
  implicit val timeout: Timeout = Timeout(1.minute)

  val cfg = config.getConfig("jdbc-journal")
  val journalConfig = new JournalConfig(cfg)
  val journalTableName =
    if (newDao) journalConfig.eventJournalTableConfiguration.tableName
    else journalConfig.journalTableConfiguration.tableName
  val tables =
    if (newDao)
      List(journalConfig.eventTagTableConfiguration.tableName, journalConfig.eventJournalTableConfiguration.tableName)
    else List(journalConfig.journalTableConfiguration.tableName)
  val profile = if (cfg.hasPath("slick.profile")) {
    SlickDatabase.profile(cfg, "slick")
  } else SlickDatabase.profile(config, "pekko-persistence-jdbc.shared-databases.slick")
  val readJournalConfig = new ReadJournalConfig(config.getConfig(JdbcReadJournal.Identifier))

  // The db is initialized in the before and after each bocks
  var dbOpt: Option[Database] = None
  def db: Database = {
    dbOpt.getOrElse {
      val newDb = if (cfg.hasPath("slick.profile")) {
        SlickDatabase.database(cfg, new SlickConfiguration(cfg.getConfig("slick")), "slick.db")
      } else
        SlickDatabase.database(
          config,
          new SlickConfiguration(config.getConfig("pekko-persistence-jdbc.shared-databases.slick")),
          "pekko-persistence-jdbc.shared-databases.slick.db")

      dbOpt = Some(newDb)
      newDb
    }
  }

  def closeDb(): Unit = {
    dbOpt.foreach(_.close())
    dbOpt = None
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    closeDb()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    closeDb()
  }

  def withActorSystem(f: ActorSystem => Unit): Unit = {
    implicit val system: ActorSystem = ActorSystem("test", config)
    f(system)
    system.terminate().futureValue
  }
}
