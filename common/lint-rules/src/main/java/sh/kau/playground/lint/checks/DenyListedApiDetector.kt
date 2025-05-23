// Copyright (C) 2024 Moxy Mouse, Inc.
// Copyright (C) 2022 Square, Inc.
// SPDX-License-Identifier: Apache-2.0
package sh.kau.playground.lint.checks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category.Companion.CORRECTNESS
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LocationType.NAME
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity.ERROR
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.XmlContext
import com.android.tools.lint.detector.api.XmlScanner
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import kotlin.collections.get
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UImportStatement
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.util.isConstructorCall
import org.w3c.dom.Element

/**
 * Deny-listed APIs that we don't want people to use.
 *
 * Adapted from:
 * - https://gist.github.com/JakeWharton/1f102d98cd10133b03a5f374540c327a
 * - https://github.com/slackhq/slack-lints/blob/main/slack-lint-checks/src/main/java/slack/lint/denylistedapis/DenyListedApiDetector.kt
 */
internal class DenyListedApiDetector : Detector(), SourceCodeScanner, XmlScanner {

  override fun getApplicableUastTypes() = CONFIG.applicableTypes()

  override fun createUastHandler(context: JavaContext) = CONFIG.visitor(context)

  override fun getApplicableElements() = CONFIG.applicableLayoutInflaterElements.keys

  override fun visitElement(context: XmlContext, element: Element) =
      CONFIG.visitor(context, element)

  private class DenyListConfig(vararg entries: DenyListedEntry) {
    private class TypeConfig(entries: List<DenyListedEntry>) {
      @Suppress("UNCHECKED_CAST") // Safe because of filter call.
      val functionEntries =
          entries.groupBy { it.functionName }.filterKeys { it != null }
              as Map<String, List<DenyListedEntry>>

      @Suppress("UNCHECKED_CAST") // Safe because of filter call.
      val referenceEntries =
          entries.groupBy { it.fieldName }.filterKeys { it != null }
              as Map<String, List<DenyListedEntry>>
    }

    val issues = entries.asSequence().map { it.issue }.distinctBy { it.id }.toList()

    private val typeConfigs =
        entries.groupBy { it.className }.mapValues { (_, entries) -> TypeConfig(entries) }

    val applicableLayoutInflaterElements =
        entries
            .filter { it.functionName == "<init>" }
            .filter {
              it.arguments == null ||
                  it.arguments == listOf("android.content.Context", "android.util.AttributeSet")
            }
            .groupBy { it.className }
            .mapValues { (cls, entries) ->
              entries.singleOrNull() ?: error("Multiple two-arg init rules for $cls")
            }

    fun applicableTypes() =
        listOf<Class<out UElement>>(
            UCallExpression::class.java,
            UImportStatement::class.java,
            UQualifiedReferenceExpression::class.java,
        )

    fun visitor(context: JavaContext) =
        object : UElementHandler() {
          override fun visitCallExpression(node: UCallExpression) {
            val function = node.resolve() ?: return

            val className = function.containingClass?.qualifiedName
            val typeConfig = typeConfigs[className] ?: return

            val functionName =
                if (node.isConstructorCall()) {
                  "<init>"
                } else {
                  // Kotlin compiler mangles function names that use inline value types as
                  // parameters by
                  // suffixing them
                  // with a hyphen.
                  // https://github.com/Kotlin/KEEP/blob/master/proposals/inline-classes.md#mangling-rules
                  function.name.substringBefore("-")
                }

            val deniedFunctions =
                typeConfig.functionEntries.getOrDefault(functionName, emptyList()) +
                    typeConfig.functionEntries.getOrDefault(
                        DenyListedEntry.Companion.MatchAll, emptyList())

            deniedFunctions.forEach { denyListEntry ->
              if (denyListEntry.allowInTests && context.isTestSource) {
                return@forEach
              } else if (denyListEntry.parametersMatchWith(function) &&
                  denyListEntry.argumentsMatchWith(node)) {
                context.report(
                    issue = denyListEntry.issue,
                    location = context.getNameLocation(node),
                    message = denyListEntry.errorMessage,
                )
              }
            }
          }

          override fun visitImportStatement(node: UImportStatement) {
            val reference = node.resolve() as? PsiField ?: return
            visitField(reference, node)
          }

          override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
            val reference = node.resolve() as? PsiField ?: return
            visitField(reference, node)
          }

          private fun visitField(reference: PsiField, node: UElement) {
            val className = reference.containingClass?.qualifiedName
            val typeConfig = typeConfigs[className] ?: return

            val referenceName = reference.name
            val deniedFunctions =
                typeConfig.referenceEntries.getOrDefault(referenceName, emptyList()) +
                    typeConfig.referenceEntries.getOrDefault(
                        DenyListedEntry.Companion.MatchAll, emptyList())

            deniedFunctions.forEach { denyListEntry ->
              if (denyListEntry.allowInTests && context.isTestSource) {
                return@forEach
              }
              context.report(
                  issue = denyListEntry.issue,
                  location = context.getLocation(node),
                  message = denyListEntry.errorMessage,
              )
            }
          }
        }

    fun visitor(context: XmlContext, element: Element) {
      val denyListEntry = applicableLayoutInflaterElements.getValue(element.tagName)
      context.report(
          issue = denyListEntry.issue,
          location = context.getLocation(element, type = NAME),
          message = denyListEntry.errorMessage,
      )
    }

    private fun DenyListedEntry.parametersMatchWith(function: PsiMethod): Boolean {
      val expected = parameters
      val actual = function.parameterList.parameters.map { it.type.canonicalText }

      return when {
        expected == null -> true
        expected.isEmpty() && actual.isEmpty() -> true
        expected.size != actual.size -> false
        else -> expected == actual
      }
    }

    private fun DenyListedEntry.argumentsMatchWith(node: UCallExpression): Boolean {
      // "arguments" being null means we don't care about this check and it should just return true.
      val expected = arguments ?: return true
      val actual = node.valueArguments

      return when {
        expected.size != actual.size -> false
        else ->
            expected.zip(actual).all { (expectedValue, actualValue) ->
              argumentMatches(expectedValue, actualValue)
            }
      }
    }

    private fun argumentMatches(expectedValue: String, actualValue: UExpression): Boolean {
      if (expectedValue == "*") return true
      val renderString =
          (actualValue as? ULiteralExpression)?.asRenderString()
              ?: (actualValue as? UQualifiedReferenceExpression)
                  ?.asRenderString() // Helps to match against static method params
      // 'Class.staticMethod()'.
      if (expectedValue == renderString) return true

      return false
    }
  }

  companion object {
    val DEFAULT_ISSUE = createIssue("DenyListedApi")
    val BLOCKING_ISSUE = createIssue("DenyListedBlockingApi")

    private val CONFIG =
        DenyListConfig(
            DenyListedEntry(
                className = "androidx.core.content.ContextCompat",
                functionName = "getDrawable",
                parameters = listOf("android.content.Context", "int"),
                errorMessage = "Use Context#getDrawableCompat() instead",
            ),
            DenyListedEntry(
                className = "androidx.core.content.res.ResourcesCompat",
                functionName = "getDrawable",
                parameters = listOf("android.content.Context", "int"),
                errorMessage = "Use Context#getDrawableCompat() instead",
            ),
            DenyListedEntry(
                className = "android.support.test.espresso.matcher.ViewMatchers",
                functionName = "withId",
                parameters = listOf("int"),
                errorMessage =
                    "Consider matching the content description instead. IDs are " +
                        "implementation details of how a screen is built, not how it works. You can't" +
                        " tell a user to click on the button with ID 428194727 so our tests should not" +
                        " be doing that. ",
            ),
            DenyListedEntry(
                className = "android.view.View",
                functionName = "setOnClickListener",
                parameters = listOf("android.view.View.OnClickListener"),
                arguments = listOf("null"),
                errorMessage =
                    "This fails to also set View#isClickable. Use View#clearOnClickListener() instead",
            ),
            DenyListedEntry(
                // If you are deny listing an extension method you need to ascertain the fully
                // qualified
                // name
                // of the class the extension method ends up on.
                className = "kotlinx.coroutines.flow.FlowKt__CollectKt",
                functionName = "launchIn",
                errorMessage =
                    "Use the structured concurrent CoroutineScope#launch and Flow#collect " +
                        "APIs instead of reactive Flow#onEach and Flow#launchIn. Suspend calls like Flow#collect " +
                        "can be refactored into standalone suspend funs and mixed in with regular control flow " +
                        "in a suspend context, but calls that invoke CoroutineScope#launch and Flow#collect at " +
                        "the same time hide the suspend context, encouraging the developer to continue working in " +
                        "the reactive domain.",
            ),
            DenyListedEntry(
                className = "androidx.viewpager2.widget.ViewPager2",
                functionName = "setId",
                parameters = listOf("int"),
                arguments = listOf("ViewCompat.generateViewId()"),
                errorMessage =
                    "Use an id defined in resources or a statically created instead of generating with ViewCompat.generateViewId(). See https://issuetracker.google.com/issues/185820237",
            ),
            DenyListedEntry(
                className = "androidx.viewpager2.widget.ViewPager2",
                functionName = "setId",
                parameters = listOf("int"),
                arguments = listOf("View.generateViewId()"),
                errorMessage =
                    "Use an id defined in resources or a statically created instead of generating with View.generateViewId(). See https://issuetracker.google.com/issues/185820237",
            ),
            DenyListedEntry(
                className = "java.util.LinkedList",
                functionName = "<init>",
                errorMessage =
                    "For a stack/queue/double-ended queue use ArrayDeque, for a list use ArrayList. Both are more efficient internally.",
            ),
            DenyListedEntry(
                className = "java.util.Stack",
                functionName = "<init>",
                errorMessage = "For a stack use ArrayDeque which is more efficient internally.",
            ),
            DenyListedEntry(
                className = "java.util.Vector",
                functionName = "<init>",
                errorMessage =
                    "For a vector use ArrayList or ArrayDeque which are more efficient internally.",
            ),
            DenyListedEntry(
                className = "android.os.Build.VERSION_CODES",
                fieldName = DenyListedEntry.Companion.MatchAll,
                errorMessage =
                    "No one remembers what these constants map to. Use the API level integer value directly since it's self-defining."),
            DenyListedEntry(
                className = "kotlinx.coroutines.rx3.RxCompletableKt",
                functionName = "rxCompletable",
                errorMessage =
                    "rxCompletable defaults to Dispatchers.Default. Provide an explicit dispatcher which can be replaced with a test dispatcher to make your tests more deterministic.",
                parameters =
                    listOf(
                        "kotlin.coroutines.CoroutineContext",
                        "kotlin.jvm.functions.Function2<? super kotlinx.coroutines.CoroutineScope,? super kotlin.coroutines.Continuation<? super kotlin.Unit>,? extends java.lang.Object>",
                    ),
                arguments = listOf("*"),
            ),
            DenyListedEntry(
                className = "kotlinx.coroutines.rx3.RxMaybeKt",
                functionName = "rxMaybe",
                errorMessage =
                    "rxMaybe defaults to Dispatchers.Default. Provide an explicit dispatcher which can be replaced with a test dispatcher to make your tests more deterministic.",
                parameters =
                    listOf(
                        "kotlin.coroutines.CoroutineContext",
                        "kotlin.jvm.functions.Function2<? super kotlinx.coroutines.CoroutineScope,? super kotlin.coroutines.Continuation<? super T>,? extends java.lang.Object>",
                    ),
                arguments = listOf("*"),
            ),
            DenyListedEntry(
                className = "kotlinx.coroutines.rx3.RxSingleKt",
                functionName = "rxSingle",
                errorMessage =
                    "rxSingle defaults to Dispatchers.Default. Provide an explicit dispatcher which can be replaced with a test dispatcher to make your tests more deterministic.",
                parameters =
                    listOf(
                        "kotlin.coroutines.CoroutineContext",
                        "kotlin.jvm.functions.Function2<? super kotlinx.coroutines.CoroutineScope,? super kotlin.coroutines.Continuation<? super T>,? extends java.lang.Object>",
                    ),
                arguments = listOf("*"),
            ),
            DenyListedEntry(
                className = "kotlinx.coroutines.rx3.RxObservableKt",
                functionName = "rxObservable",
                errorMessage =
                    "rxObservable defaults to Dispatchers.Default. Provide an explicit dispatcher which can be replaced with a test dispatcher to make your tests more deterministic.",
                parameters =
                    listOf(
                        "kotlin.coroutines.CoroutineContext",
                        "kotlin.jvm.functions.Function2<? super kotlinx.coroutines.channels.ProducerScope<T>,? super kotlin.coroutines.Continuation<? super kotlin.Unit>,? extends java.lang.Object>",
                    ),
                arguments = listOf("*"),
            ),
            //            DenyListedEntry(
            //                className = "java.util.Date",
            //                functionName = MatchAll,
            //                errorMessage =
            //                    "Use java.time.Instant or java.time.ZonedDateTime instead. There
            // is no reason to use java.util.Date in Java 8+.",
            //            ),
            DenyListedEntry(
                className = "java.text.DateFormat",
                fieldName = DenyListedEntry.Companion.MatchAll,
                errorMessage =
                    "Use java.time.DateTimeFormatter instead. There is no reason to use java.text.DateFormat in Java 8+.",
            ),
            DenyListedEntry(
                className = "java.text.SimpleDateFormat",
                fieldName = DenyListedEntry.Companion.MatchAll,
                errorMessage =
                    "Use java.time.DateTimeFormatter instead. There is no reason to use java.text.DateFormat in Java 8+.",
            ),
            DenyListedEntry(
                className = "java.text.DateFormat",
                functionName = DenyListedEntry.Companion.MatchAll,
                errorMessage =
                    "Use java.time.DateTimeFormatter instead. There is no reason to use java.text.DateFormat in Java 8+.",
            ),
            DenyListedEntry(
                className = "java.text.SimpleDateFormat",
                functionName = DenyListedEntry.Companion.MatchAll,
                errorMessage =
                    "Use java.time.DateTimeFormatter instead. There is no reason to use java.text.DateFormat in Java 8+.",
            ),
            DenyListedEntry(
                className = "kotlin.ResultKt",
                functionName = "runCatching",
                errorMessage =
                    "runCatching has hidden issues when used with coroutines as it catches and doesn't rethrow CancellationException. " +
                        "This can interfere with coroutines cancellation handling! " +
                        "Prefer catching specific exceptions based on the current case.",
            ),
            // Blocking calls
            DenyListedEntry(
                className = "kotlinx.coroutines.BuildersKt",
                functionName = "runBlocking",
                errorMessage =
                    "Blocking calls in coroutines can cause deadlocks and application jank. " +
                        "Prefer making the enclosing function a suspend function or refactoring this in a way to use non-blocking calls. " +
                        "If running in a test, use runTest {} or Turbine to test synchronous values.",
                issue = BLOCKING_ISSUE,
            ),
            *rxJavaBlockingCalls().toTypedArray(),
        )

    val ISSUES = CONFIG.issues

    private fun createIssue(
        id: String,
        briefDescription: String = "Deny-listed API",
        explanation: String =
            "This lint check flags usages of APIs in external libraries that we prefer not to use.",
    ): Issue {
      return Issue.create(
          id = id,
          briefDescription = briefDescription,
          explanation = explanation,
          category = CORRECTNESS,
          priority = 5,
          severity = ERROR,
          implementation =
              Implementation(
                  DenyListedApiDetector::class.java,
                  EnumSet.of(Scope.JAVA_FILE, Scope.RESOURCE_FILE, Scope.TEST_SOURCES),
                  EnumSet.of(Scope.JAVA_FILE),
                  EnumSet.of(Scope.RESOURCE_FILE),
                  EnumSet.of(Scope.TEST_SOURCES),
              ),
      )
    }
  }
}

data class DenyListedEntry(
    val className: String,
    /**
     * The function name to match, [MatchAll] to match all functions, or null if matching a field.
     */
    val functionName: String? = null,
    /** The field name to match, [MatchAll] to match all fields, or null if matching a function. */
    val fieldName: String? = null,
    /** Fully-qualified types of function parameters to match, or null to match all overloads. */
    val parameters: List<String>? = null,
    /** Argument expressions to match at the call site, or null to match all invocations. */
    val arguments: List<String>? = null,
    val errorMessage: String,
    /**
     * Option to allow this issue in tests. Should _only_ be reserved for invocations that make
     * sense in tests.
     */
    val allowInTests: Boolean = false,
    /**
     * Issue that should be reported for this entry. Defaults to
     * [DenyListedApiDetector.DEFAULT_ISSUE]
     */
    val issue: Issue = DenyListedApiDetector.DEFAULT_ISSUE,
) {
  init {
    require((functionName == null) xor (fieldName == null)) {
      "One of functionName or fieldName must be set"
    }
  }

  companion object {
    const val MatchAll = "*"
  }
}

private fun rxJavaBlockingCalls() =
    listOf(
            "io.reactivex.rxjava3.core.Completable" to listOf("blockingAwait"),
            "io.reactivex.rxjava3.core.Single" to listOf("blockingGet", "blockingSubscribe"),
            "io.reactivex.rxjava3.core.Maybe" to listOf("blockingGet", "blockingSubscribe"),
            "io.reactivex.rxjava3.core.Observable" to
                listOf(
                    "blockingFirst",
                    "blockingForEach",
                    "blockingIterable",
                    "blockingLatest",
                    "blockingMostRecent",
                    "blockingNext",
                    "blockingSingle",
                    "blockingSubscribe",
                ),
            "io.reactivex.rxjava3.core.Flowable" to
                listOf(
                    "blockingFirst",
                    "blockingForEach",
                    "blockingIterable",
                    "blockingLatest",
                    "blockingMostRecent",
                    "blockingNext",
                    "blockingSingle",
                    "blockingSubscribe",
                ),
        )
        .flatMap { (className, methods) ->
          val shortType = className.substringAfterLast('.')
          val isCompletable = shortType == "Completable"
          val orMessage =
              if (!isCompletable) {
                " Completable (if you want to hide emission values but defer subscription),"
              } else {
                ""
              }
          methods.map { method ->
            DenyListedEntry(
                className = className,
                functionName = method,
                errorMessage =
                    "Blocking calls in RxJava can cause deadlocks and application jank. " +
                        "Prefer making the enclosing method/function return this $shortType, a Disposable to grant control to the caller,$orMessage or refactoring this in a way to use non-blocking calls. " +
                        "If running in a test, use the .test()/TestObserver API (https://reactivex.io/RxJava/3.x/javadoc/io/reactivex/rxjava3/observers/TestObserver.html) test synchronous values.",
                issue = DenyListedApiDetector.BLOCKING_ISSUE,
            )
          }
        }
