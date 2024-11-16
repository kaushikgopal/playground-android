// Copyright (C) 2024 Moxy Mouse, Inc.
// SPDX-License-Identifier: Apache-2.0
package sh.kau.playground.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import org.jetbrains.uast.UCallExpression

class CoerceAtMostUsageDetector : Detector(), SourceCodeScanner {
    companion object {
        val ISSUE =
            Issue.create(
                id = "CoerceAtMostUsage",
                briefDescription = "Prefer minOf over coerceAtMost",
                explanation =
                "The `coerceAtMost` method can be replaced with the simpler `minOf` function.",
                category = Category.CORRECTNESS,
                priority = 6,
                severity = Severity.ERROR,
                implementation =
                Implementation(CoerceAtMostUsageDetector::class.java, Scope.JAVA_FILE_SCOPE)
            )
    }

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (node.methodName == "coerceAtMost") {

                    // Get the receiver (the value before .coerceAtMost)
                    val receiver = node.receiver?.asSourceString() ?: ""

                    // Get the argument (the value inside .coerceAtMost)
                    val argument = node.valueArguments.firstOrNull()?.asSourceString() ?: ""

                    // Build the replacement string
                    val replacement = "minOf($receiver, $argument)"

                    val fix =
                        fix()
                            .replace()
                            .all() // Replace the entire call expression, not just the method name
                            .with(replacement)
                            .build()

                    context.report(
                        CoerceAtLeastUsageDetector.ISSUE,
                        node,
                        context.getLocation(node),
                        "Prefer using `minOf`",
                        fix
                    )
                }
            }
        }
}
