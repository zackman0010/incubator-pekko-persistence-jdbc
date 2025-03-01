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

package org.apache.pekko.persistence.jdbc.snapshot.dao

import org.apache.pekko
import pekko.persistence.jdbc.config.SnapshotTableConfiguration
import pekko.persistence.jdbc.snapshot.dao.SnapshotTables.SnapshotRow
import pekko.persistence.jdbc.snapshot.dao.legacy.SnapshotTables.isOracleDriver
import pekko.persistence.jdbc.util.InputStreamOps.InputStreamImplicits

object SnapshotTables {
  case class SnapshotRow(
      persistenceId: String,
      sequenceNumber: Long,
      created: Long,
      snapshotSerId: Int,
      snapshotSerManifest: String,
      snapshotPayload: Array[Byte],
      metaSerId: Option[Int],
      metaSerManifest: Option[String],
      metaPayload: Option[Array[Byte]])
}

trait SnapshotTables {
  val profile: slick.jdbc.JdbcProfile
  import profile.api._
  def snapshotTableCfg: SnapshotTableConfiguration

  class Snapshot(_tableTag: Tag)
      extends Table[SnapshotRow](
        _tableTag,
        _schemaName = snapshotTableCfg.schemaName,
        _tableName = snapshotTableCfg.tableName) {
    def * =
      (
        persistenceId,
        sequenceNumber,
        created,
        snapshotSerId,
        snapshotSerManifest,
        snapshotPayload,
        metaSerId,
        metaSerManifest,
        metaPayload).<>(SnapshotRow.tupled, SnapshotRow.unapply)

    val persistenceId: Rep[String] =
      column[String](snapshotTableCfg.columnNames.persistenceId, O.Length(255, varying = true))
    val sequenceNumber: Rep[Long] = column[Long](snapshotTableCfg.columnNames.sequenceNumber)
    val created: Rep[Long] = column[Long](snapshotTableCfg.columnNames.created)

    val snapshotPayload: Rep[Array[Byte]] = column[Array[Byte]](snapshotTableCfg.columnNames.snapshotPayload)
    val snapshotSerId: Rep[Int] = column[Int](snapshotTableCfg.columnNames.snapshotSerId)
    val snapshotSerManifest: Rep[String] = column[String](snapshotTableCfg.columnNames.snapshotSerManifest)

    val metaPayload: Rep[Option[Array[Byte]]] = column[Option[Array[Byte]]](snapshotTableCfg.columnNames.metaPayload)
    val metaSerId: Rep[Option[Int]] = column[Option[Int]](snapshotTableCfg.columnNames.metaSerId)
    val metaSerManifest: Rep[Option[String]] = column[Option[String]](snapshotTableCfg.columnNames.metaSerManifest)

    val pk = primaryKey(s"${tableName}_pk", (persistenceId, sequenceNumber))
  }

  case class OracleSnapshot(_tableTag: Tag) extends Snapshot(_tableTag) {
    import java.sql.Blob

    import javax.sql.rowset.serial.SerialBlob

    private val columnType =
      MappedColumnType.base[Array[Byte], Blob](bytes => new SerialBlob(bytes), blob => blob.getBinaryStream.toArray)

    override val snapshotPayload: Rep[Array[Byte]] =
      column[Array[Byte]](snapshotTableCfg.columnNames.snapshotPayload)(columnType)
  }

  lazy val SnapshotTable = new TableQuery(tag =>
    if (isOracleDriver(profile)) OracleSnapshot(tag) else new Snapshot(tag))
}
