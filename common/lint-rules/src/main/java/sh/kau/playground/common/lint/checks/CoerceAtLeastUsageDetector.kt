// Copyright (C) 2024 Moxy Mouse, Inc.
// SPDX-License-Identifier: Apache-2.0
package sh.kau.playground.common.lint.checks

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

class CoerceAtLeastUsageDetector : Detector(), SourceCodeScanner {
    companion object {
        val ISSUE =
            Issue.create(
                id = "CoerceAtLeastUsage",
                briefDescription = "Prefer maxOf over coerceAtLeast",
                explanation =
                "The `coerceAtLeast` method can be replaced with the simpler `maxOf` function.",
                category = Category.CORRECTNESS,
                priority = 6,
                severity = Severity.ERROR,
                implementation =
                Implementation(CoerceAtLeastUsageDetector::class.java, Scope.JAVA_FILE_SCOPE)
            )
    }

    override fun getApplicableUastTypes() = listOf(UCallExpression::class.java)


    override fun createUastHandler(context: JavaContext) =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                if (node.methodName == "coerceAtLeast") {

                    // Get the receiver (the value before .coerceAtLeast)
                    val receiver = node.receiver?.asSourceString() ?: ""

                    // Get the argument (the value inside .coerceAtLeast)
                    val argument = node.valueArguments.firstOrNull()?.asSourceString() ?: ""

                    // Build the replacement string
                    val replacement = "maxOf($receiver, $argument)"

                    val fix =
                        fix()
                            .replace()
                            .all() // Replace the entire call expression, not just the method name
                            .with(replacement)
                            .build()

                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Prefer using `maxOf`",
                        fix
                    )
                }
            }
        }
}
