/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * license agreements; and to You under the Apache License, version 2.0:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * This file is part of the Apache Pekko project, derived from Akka.
 */

/*
 * Copyright (C) 2018-2022 Lightbend Inc. <https://www.lightbend.com>
 */

import de.heikoseeberger.sbtheader.HeaderPlugin.autoImport._
import de.heikoseeberger.sbtheader.{ CommentCreator, HeaderPlugin, NewLine }
import org.apache.commons.lang3.StringUtils
import sbt.Keys._
import sbt._

trait CopyrightHeader extends AutoPlugin {

  override def requires: Plugins = HeaderPlugin

  override def trigger: PluginTrigger = allRequirements

  protected def headerMappingSettings: Seq[Def.Setting[_]] =
    Seq(Compile, Test, IntegrationTest).flatMap { config =>
      inConfig(config)(
        Seq(
          headerLicense := Some(HeaderLicense.Custom(apacheHeader)),
          headerMappings := headerMappings.value ++ Map(
            HeaderFileType.scala -> cStyleComment,
            HeaderFileType.java -> cStyleComment,
            HeaderFileType.conf -> hashLineComment,
            HeaderFileType("template") -> cStyleComment)))
    }

  override def projectSettings: Seq[Def.Setting[_]] =
    Def.settings(headerMappingSettings, additional) ++ Defaults.itSettings ++
    headerSettings(IntegrationTest)

  def additional: Seq[Def.Setting[_]] =
    Def.settings(Compile / compile := {
        (Compile / headerCreate).value
        (Compile / compile).value
      },
      Test / compile := {
        (Test / headerCreate).value
        (Test / compile).value
      })

  def apacheHeader: String =
    """Licensed to the Apache Software Foundation (ASF) under one or more
      |license agreements; and to You under the Apache License, version 2.0:
      |
      |  https://www.apache.org/licenses/LICENSE-2.0
      |
      |This file is part of the Apache Pekko project, derived from Akka.
      |""".stripMargin

  val apacheSpdxHeader: String = "SPDX-License-Identifier: Apache-2.0"

  val cStyleComment = HeaderCommentStyle.cStyleBlockComment.copy(commentCreator = new CommentCreator() {

    override def apply(text: String, existingText: Option[String]): String = {
      val formatted = existingText match {
        case Some(currentText) if isValidCopyrightAnnotated(currentText) =>
          currentText
        case Some(currentText) if isLightbendCopyrighted(currentText) =>
          HeaderCommentStyle.cStyleBlockComment.commentCreator(text, existingText) + NewLine * 2 + currentText
        case Some(currentText) =>
          throw new IllegalStateException(s"Unable to detect copyright for header:[$currentText]")
        case None =>
          HeaderCommentStyle.cStyleBlockComment.commentCreator(text, existingText)
      }
      formatted.trim
    }
  })

  val hashLineComment = HeaderCommentStyle.hashLineComment.copy(commentCreator = new CommentCreator() {

    // deliberately hardcode use of apacheSpdxHeader and ignore input text
    override def apply(text: String, existingText: Option[String]): String = {
      val formatted = existingText match {
        case Some(currentText) if isApacheCopyrighted(currentText) =>
          currentText
        case Some(currentText) =>
          HeaderCommentStyle.hashLineComment.commentCreator(apacheSpdxHeader, existingText) + NewLine * 2 + currentText
        case None =>
          HeaderCommentStyle.hashLineComment.commentCreator(apacheSpdxHeader, existingText)
      }
      formatted.trim
    }
  })

  private def isApacheCopyrighted(text: String): Boolean =
    StringUtils.containsIgnoreCase(text, "licensed to the apache software foundation (asf)") ||
    StringUtils.containsIgnoreCase(text, "www.apache.org/licenses/license-2.0") ||
    StringUtils.contains(text, "Apache-2.0")

  private def isLightbendCopyrighted(text: String): Boolean =
    StringUtils.containsIgnoreCase(text, "lightbend inc.")

  private def isValidCopyrightAnnotated(text: String): Boolean = {
    isApacheCopyrighted(text)
  }
}

object CopyrightHeader extends CopyrightHeader

object CopyrightHeaderInPr extends CopyrightHeader
