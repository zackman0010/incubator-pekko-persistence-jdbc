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

package org.apache.pekko.persistence.jdbc.db

import org.apache.pekko
import pekko.actor.{ ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import pekko.persistence.jdbc.config.{ ConfigKeys, SlickConfiguration }
import pekko.persistence.jdbc.util.ConfigOps._
import com.typesafe.config.{ Config, ConfigObject }

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success }

object SlickExtension extends ExtensionId[SlickExtensionImpl] with ExtensionIdProvider {
  override def lookup: SlickExtension.type = SlickExtension
  override def createExtension(system: ExtendedActorSystem) = new SlickExtensionImpl(system)
}

class SlickExtensionImpl(system: ExtendedActorSystem) extends Extension {

  private val dbProvider: SlickDatabaseProvider = {
    val fqcn = system.settings.config.getString("pekko-persistence-jdbc.database-provider-fqcn")
    val args = List(classOf[ActorSystem] -> system)
    system.dynamicAccess.createInstanceFor[SlickDatabaseProvider](fqcn, args) match {
      case Success(result) => result
      case Failure(t)      => throw new RuntimeException("Failed to create SlickDatabaseProvider", t)
    }
  }

  def database(config: Config): SlickDatabase = dbProvider.database(config)
}

/**
 * User overridable database provider.
 * Since this provider is called from an Apache Pekko extension it must be thread safe!
 *
 * A SlickDatabaseProvider is loaded using reflection,
 * The instance is created using the following:
 * - The fully qualified class name as configured in `jdbc-journal.database-provider-fqcn`.
 * - The constructor with one argument of type [[pekko.actor.ActorSystem]] is used to create the instance.
 *   Therefore the class must have such a constructor.
 */
trait SlickDatabaseProvider {

  /**
   * Create or retrieve the database
   * @param config The configuration which may be used to create the database. If the database is shared
   *               then the SlickDatabaseProvider implementation may choose to ignore this parameter.
   */
  def database(config: Config): SlickDatabase
}

class DefaultSlickDatabaseProvider(system: ActorSystem) extends SlickDatabaseProvider {
  val sharedDatabases: Map[String, LazySlickDatabase] = system.settings.config
    .getObject("pekko-persistence-jdbc.shared-databases")
    .asScala
    .flatMap {
      case (key, confObj: ConfigObject) =>
        val conf = confObj.toConfig
        if (conf.hasPath("profile")) {
          // Only create the LazySlickDatabase if a profile has actually been configured, this ensures that the example in the reference conf is ignored
          List(key -> new LazySlickDatabase(conf, system))
        } else Nil
      case (key, notAnObject) =>
        throw new RuntimeException(
          s"""Expected "pekko-persistence-jdbc.shared-databases.$key" to be a config ConfigObject, but got ${notAnObject
              .valueType()} (${notAnObject.getClass})""")
    }
    .toMap

  private def getSharedDbOrThrow(sharedDbName: String): LazySlickDatabase =
    sharedDatabases.getOrElse(
      sharedDbName,
      throw new RuntimeException(
        s"No shared database is configured under pekko-persistence-jdbc.shared-databases.$sharedDbName"))

  def database(config: Config): SlickDatabase = {
    config.asStringOption(ConfigKeys.useSharedDb) match {
      case None => SlickDatabase.initializeEagerly(config, new SlickConfiguration(config.getConfig("slick")), "slick")
      case Some(sharedDbName) =>
        getSharedDbOrThrow(sharedDbName)
    }
  }
}
