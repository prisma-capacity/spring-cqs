#!/usr/bin/env kotlin
//
@file:DependsOn("io.github.typesafegithub:github-workflows-kt:0.50.0")



import io.github.typesafegithub.workflows.actions.actions.CacheV3
import io.github.typesafegithub.workflows.actions.actions.CheckoutV3
import io.github.typesafegithub.workflows.actions.actions.SetupJavaV3
import io.github.typesafegithub.workflows.actions.codecov.CodecovActionV3
import io.github.typesafegithub.workflows.domain.RunnerType
import io.github.typesafegithub.workflows.domain.Workflow
import io.github.typesafegithub.workflows.domain.triggers.Push
import io.github.typesafegithub.workflows.dsl.workflow
import io.github.typesafegithub.workflows.yaml.writeToFile
import java.nio.file.Paths

public val workflowMaven: Workflow = workflow(

      name = "Java/Maven build",
      on = listOf(
        Push(),
        ),
        sourceFile = Paths.get(".github/kts/maven.main.kts"),
    ) {
      job(
        id = "build",
        runsOn = RunnerType.UbuntuLatest,
      ) {
        uses(
          name = "Checkout",
          action = CheckoutV3(),
        )
        uses(
          name = "Cache",
          action = CacheV3(
            path = listOf(
              "~/.m2/repository",
            )
            ,
            key = "${'$'}{{ runner.os }}-maven-${'$'}{{ hashFiles('**/pom.xml') }}",
            restoreKeys = listOf(
              "${'$'}{{ runner.os }}-maven-",
            )
            ,
          ),
        )
        uses(
          name = "Set up JDK",
          action = SetupJavaV3(
            javaVersion = "11",
            distribution = SetupJavaV3.Distribution.Corretto
          ),
        )
        run(
          name = "Build with Maven",
          command = "mvn -B install --file pom.xml",
        )
        uses(
          name = "CodecovActionV3",
          action = CodecovActionV3(
            token = "${'$'}{{ secrets.CODECOV_TOKEN }}"
          ),
        )
      }
    }

workflowMaven.writeToFile()
