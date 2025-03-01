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

package org.apache.pekko.persistence.jdbc.snapshot.dao.legacy

import org.apache.pekko
import pekko.persistence.jdbc.config.LegacySnapshotTableConfiguration
import pekko.persistence.jdbc.snapshot.dao.legacy.SnapshotTables.SnapshotRow
import slick.jdbc.JdbcProfile

class SnapshotQueries(val profile: JdbcProfile, override val snapshotTableCfg: LegacySnapshotTableConfiguration)
    extends SnapshotTables {
  import profile.api._

  private val SnapshotTableC = Compiled(SnapshotTable)

  def insertOrUpdate(snapshotRow: SnapshotRow) =
    SnapshotTableC.insertOrUpdate(snapshotRow)

  private def _selectAll(persistenceId: Rep[String]) =
    SnapshotTable.filter(_.persistenceId === persistenceId).sortBy(_.sequenceNumber.desc)
  val selectAll = Compiled(_selectAll _)

  private def _selectLatestByPersistenceId(persistenceId: Rep[String]) =
    _selectAll(persistenceId).take(1)
  val selectLatestByPersistenceId = Compiled(_selectLatestByPersistenceId _)

  private def _selectByPersistenceIdAndSequenceNr(persistenceId: Rep[String], sequenceNr: Rep[Long]) =
    _selectAll(persistenceId).filter(_.sequenceNumber === sequenceNr)
  val selectByPersistenceIdAndSequenceNr = Compiled(_selectByPersistenceIdAndSequenceNr _)

  private def _selectByPersistenceIdUpToMaxTimestamp(persistenceId: Rep[String], maxTimestamp: Rep[Long]) =
    _selectAll(persistenceId).filter(_.created <= maxTimestamp)
  val selectByPersistenceIdUpToMaxTimestamp = Compiled(_selectByPersistenceIdUpToMaxTimestamp _)

  private def _selectByPersistenceIdUpToMaxSequenceNr(persistenceId: Rep[String], maxSequenceNr: Rep[Long]) =
    _selectAll(persistenceId).filter(_.sequenceNumber <= maxSequenceNr)
  val selectByPersistenceIdUpToMaxSequenceNr = Compiled(_selectByPersistenceIdUpToMaxSequenceNr _)

  private def _selectByPersistenceIdUpToMaxSequenceNrAndMaxTimestamp(
      persistenceId: Rep[String],
      maxSequenceNr: Rep[Long],
      maxTimestamp: Rep[Long]) =
    _selectByPersistenceIdUpToMaxSequenceNr(persistenceId, maxSequenceNr).filter(_.created <= maxTimestamp)
  val selectByPersistenceIdUpToMaxSequenceNrAndMaxTimestamp = Compiled(
    _selectByPersistenceIdUpToMaxSequenceNrAndMaxTimestamp _)

  private def _selectOneByPersistenceIdAndMaxTimestamp(persistenceId: Rep[String], maxTimestamp: Rep[Long]) =
    _selectAll(persistenceId).filter(_.created <= maxTimestamp).take(1)
  val selectOneByPersistenceIdAndMaxTimestamp = Compiled(_selectOneByPersistenceIdAndMaxTimestamp _)

  private def _selectOneByPersistenceIdAndMaxSequenceNr(persistenceId: Rep[String], maxSequenceNr: Rep[Long]) =
    _selectAll(persistenceId).filter(_.sequenceNumber <= maxSequenceNr).take(1)
  val selectOneByPersistenceIdAndMaxSequenceNr = Compiled(_selectOneByPersistenceIdAndMaxSequenceNr _)

  private def _selectOneByPersistenceIdAndMaxSequenceNrAndMaxTimestamp(
      persistenceId: Rep[String],
      maxSequenceNr: Rep[Long],
      maxTimestamp: Rep[Long]) =
    _selectByPersistenceIdUpToMaxSequenceNr(persistenceId, maxSequenceNr).filter(_.created <= maxTimestamp).take(1)
  val selectOneByPersistenceIdAndMaxSequenceNrAndMaxTimestamp = Compiled(
    _selectOneByPersistenceIdAndMaxSequenceNrAndMaxTimestamp _)
}
