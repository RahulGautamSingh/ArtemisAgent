package artemis.agent.help

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import artemis.agent.MainActivity
import artemis.agent.R
import com.adevinta.android.barista.assertion.BaristaListAssertions.assertDisplayedAtPosition
import com.adevinta.android.barista.assertion.BaristaRecyclerViewAssertions.assertRecyclerViewItemCount
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class HelpFragmentTest {
    private companion object {
        val stringResources = intArrayOf(
            R.string.help_topics_getting_started,
            R.string.help_topics_basics,
            R.string.help_topics_stations,
            R.string.help_topics_allies,
            R.string.help_topics_missions,
            R.string.help_topics_routing,
            R.string.help_topics_enemies,
            R.string.help_topics_biomechs,
            R.string.help_topics_about,
        )
    }

    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    private fun assertHelpMenuDisplayed() {
        assertNotDisplayed(R.id.helpTopicTitle)
        assertNotDisplayed(R.id.backButton)
        assertDisplayed(R.id.helpTopicContent)
        assertRecyclerViewItemCount(R.id.helpTopicContent, stringResources.size)

        stringResources.forEachIndexed { index, res ->
            assertDisplayedAtPosition(R.id.helpTopicContent, index, res)
        }
    }

    @Test
    fun menuOptionsTest() {
        clickOn(R.id.helpPageButton)
        assertHelpMenuDisplayed()

        stringResources.forEach { stringRes ->
            clickOn(stringRes)

            assertDisplayed(R.id.helpTopicTitle, stringRes)
            assertDisplayed(R.id.backButton)
            assertDisplayed(R.id.helpTopicContent)

            clickOn(R.id.backButton)

            assertHelpMenuDisplayed()
        }
    }
}
