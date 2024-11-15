package sh.kau.playground.lint.checks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

/*
 * The list of issues that will be checked when running <code>lint</code>.
 */
class IssueRegistry : IssueRegistry() {
    override val issues: List<Issue>
        get() =
            listOf(
                AvoidDateDetector.ISSUE,
                NotNullAssertionDetector.ISSUE,
            )

    override val api: Int = CURRENT_API

    override val minApi: Int
        get() = 8 // works with Studio 4.1 or later; see com.android.tools.lint.detector.api.Api / ApiKt

    // Requires lint API 30.0+; if you're still building for something
    // older, just remove this property.
    override val vendor: Vendor =
        Vendor(
            vendorName = "Kaush's Playground",
            // feedbackUrl = "https://github.com/googlesamples/android-custom-lint-rules/issues",
            // contact = "https://github.com/googlesamples/android-custom-lint-rules",
        )
}
