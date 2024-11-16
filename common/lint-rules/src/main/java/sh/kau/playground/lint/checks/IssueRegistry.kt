package sh.kau.playground.lint.checks

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

/*
 * The list of issues that will be checked when running <code>lint</code>.
 */
class IssueRegistry : IssueRegistry() {

    // Requires lint API 30.0+; if you're still building for something
    // older, just remove this property.
    override val vendor: Vendor =
        Vendor(
            vendorName = "Moxy Mouse",
            identifier = "moxy-mouse",
            feedbackUrl = "https://github.com/kaushikgopal/playground-android",
            contact = "https://github.com/kaushikgopal/playground-android",
        )

    override val api: Int = CURRENT_API
    override val minApi: Int =
        8 // works with Studio 4.1 or later; see com.android.tools.lint.detector.api.Api / ApiKt

    override val issues: List<Issue> = buildList {
        add(AvoidDateDetector.ISSUE)
        add(CoerceAtLeastUsageDetector.ISSUE)
        add(CoerceAtMostUsageDetector.ISSUE)
        add(NotNullAssertionDetector.ISSUE)
        add(RetrofitUsageDetector.ISSUE)
        addAll(DenyListedApiDetector.ISSUES)
    }
}
