// Copyright (C) 2024 Moxy Mouse Inc.
// SPDX-License-Identifier: Apache-2.0
package sh.kau.playground.lint.utils

import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiType
import com.intellij.psi.PsiWildcardType
import java.util.EnumSet
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UMethod

/**
 * Copyright (C) 2021 Slack Technologies, LLC SPDX-License-Identifier: Apache-2.0
 *
 * Removes a given [node] as a fix.
 */
internal fun LintFix.Builder.removeNode(
    context: JavaContext,
    node: PsiElement,
    name: String? = null,
    autoFix: Boolean = true,
    text: String = node.text,
): LintFix {
  val fixName = name ?: "Remove '$text'"
  return replace()
      .name(fixName)
      .range(context.getLocation(node))
      .shortenNames()
      .text(text)
      .with("")
      .apply {
        if (autoFix) {
          autoFix()
        }
      }
      .build()
}

/**
 * Copyright (C) 2021 Slack Technologies, LLC SPDX-License-Identifier: Apache-2.0
 *
 * Collects the return type of this [UMethod] in a suspend-safe way.
 *
 * For coroutines, the suspend methods return context rather than the source-declared return type,
 * which is encoded in a continuation parameter at the end of the parameter list.
 *
 * For example, the following snippet:
 * ```
 * suspend fun foo(): String
 * ```
 *
 * Will appear like so to lint:
 * ```
 * Object foo(Continuation<? super String> continuation)
 * ```
 */
internal fun UMethod.safeReturnType(context: JavaContext): PsiType? {
  if (language == KotlinLanguage.INSTANCE && context.evaluator.isSuspend(this)) {
    val classReference = parameterList.parameters.lastOrNull()?.type as? PsiClassType ?: return null
    val wildcard = classReference.parameters.singleOrNull() as? PsiWildcardType ?: return null
    return wildcard.bound
  } else {
    return returnType
  }
}

/** Copyright (C) 2021 Slack Technologies, LLC SPDX-License-Identifier: Apache-2.0 */
@Suppress("SpreadOperator")
internal inline fun <reified T> sourceImplementation(
    shouldRunOnTestSources: Boolean = true
): Implementation where T : Detector, T : SourceCodeScanner {
  // We use the overloaded constructor that takes a varargs of `Scope` as the last param.
  // This is to enable on-the-fly IDE checks. We are telling lint to run on both
  // JAVA and TEST_SOURCES in the `scope` parameter but by providing the `analysisScopes`
  // params, we're indicating that this check can run on either JAVA or TEST_SOURCES and
  // doesn't require both of them together.
  // From discussion on lint-dev https://groups.google.com/d/msg/lint-dev/ULQMzW1ZlP0/1dG4Vj3-AQAJ
  // This was supposed to be fixed in AS 3.4 but still required as recently as 3.6-alpha10.
  return if (shouldRunOnTestSources) {
    Implementation(
        T::class.java,
        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES),
        EnumSet.of(Scope.JAVA_FILE),
        EnumSet.of(Scope.TEST_SOURCES),
    )
  } else {
    Implementation(T::class.java, EnumSet.of(Scope.JAVA_FILE))
  }
}
