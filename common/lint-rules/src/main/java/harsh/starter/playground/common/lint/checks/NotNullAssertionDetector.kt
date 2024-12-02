// Copyright (C) 2024 The Android Open Source Project
// SPDX-License-Identifier: Apache-2.0
package harsh.starter.playground.common.lint.checks


import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UPostfixExpression

/**
 * Adapted from:
 * - https://github.com/googlesamples/android-custom-lint-rules/blob/main/checks/src/main/java/com/example/lint/checks/NotNullAssertionDetector.kt
 */
class NotNullAssertionDetector : Detector(), SourceCodeScanner {
    companion object Issues {
        private val IMPLEMENTATION =
            Implementation(NotNullAssertionDetector::class.java, Scope.JAVA_FILE_SCOPE)

        @JvmField
        val ISSUE =
            Issue.create(
                id = "NotNullAssertion",
                briefDescription = "Avoid `!!`",
                explanation =
                """
          Do not use the `!!` operator. It can lead to null pointer exceptions. \
          Please use the `?` operator instead, or assign to a local variable with \
          `?:` initialization if necessary.
          """,
                category = Category.CORRECTNESS,
                priority = 6,
                severity = Severity.WARNING,
                implementation = IMPLEMENTATION,
            )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>>? {
        return listOf(UPostfixExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitPostfixExpression(node: UPostfixExpression) {
                if (node.operator.text == "!!") {
                    var message = "Do not use `!!`"

                    // Kotlin Analysis API example
                    val sourcePsi = node.operand.sourcePsi
                    if (sourcePsi is KtExpression) {
                        analyze(sourcePsi) {
                            val type = sourcePsi.expressionType
                            if (type != null && !type.canBeNull) {
                                message += " -- it's not even needed here"
                            }
                        }
                    }

                    val incident = Incident(ISSUE, node, context.getLocation(node), message)
                    context.report(incident)
                }
            }
        }
    }
}