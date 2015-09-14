package kotlinplugin

import Keys._
import sbt._
import sbt.Keys._
import sbt.plugins.JvmPlugin

/**
 * @author pfnguyen
 */
object KotlinPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = JvmPlugin

  override def projectSettings = Seq(
    kotlinVersion := BuildInfo.kotlinVersion,
    kotlincOptions := Nil,
    kotlincPluginOptions := Nil,
    kotlinCompileJava := false,
    kotlinCompileOrder := KotlinCompileOrder.KotlinAfter,
    watchSources     <++= Def.task {
      import language.postfixOps
      val kotlinSources = "*.kt" || "*.kts"
      (sourceDirectories in Compile).value.flatMap(_ ** kotlinSources get) ++
        (sourceDirectories in Test).value.flatMap(_ ** kotlinSources get)
    }
  ) ++ inConfig(Compile)(kotlinCompileSettings) ++
    inConfig(Test)(kotlinCompileSettings)

  val autoImport = Keys

  // public to allow kotlin compile in other configs beyond Compile and Test
  val kotlinCompileSettings = List(
    sourceDirectories += kotlinSource.value,
    kotlinCompileOrder <<= kotlinCompileOrder in This,
    kotlincOptions <<= kotlincOptions in This,
    kotlinCompileJava <<= kotlinCompileJava in This,
    sources := {
      sources.value.filterNot(kotlinCompileJava.value && _.getName.endsWith(".java"))
    },
    kotlincPluginOptions <<= kotlincPluginOptions in This,
    kotlinCompileBefore := {
      if (kotlinCompileOrder.value == KotlinCompileOrder.KotlinBefore) {
        KotlinCompile.compile(kotlincOptions.value,
          sourceDirectories.value, kotlinCompileJava.value, kotlincPluginOptions.value,
          dependencyClasspath.value, classDirectory.value, streams.value)
      }
    },
    compile <<= compile dependsOn kotlinCompileBefore,
    compile := {
      if (kotlinCompileOrder.value == KotlinCompileOrder.KotlinAfter) {
        KotlinCompile.compile(kotlincOptions.value,
          sourceDirectories.value, kotlinCompileJava.value, kotlincPluginOptions.value,
          dependencyClasspath.value, classDirectory.value, streams.value)
      }
      // XXX handle updating Analysis
      // maybe once kotlin supports incremental compilation
      compile.value
    },
    kotlinSource := sourceDirectory.value / "kotlin"
  )
}