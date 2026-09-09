package com.armadio

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ArmarioApplication::class)
class InventoryUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun label(id: Int) = compose.activity.getString(id)

    private fun waitFor(text: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun nativeApp_canCreateEditSearchAndDeleteGarment() {
        compose.onNodeWithText(label(R.string.add_garment)).performClick()
        compose.onNodeWithText(label(R.string.garment_name)).performTextInput("Cappotto blu")
        compose.onNodeWithText(label(R.string.garment_brand)).performTextInput("Marca prova")
        compose.onNodeWithText(label(R.string.save)).performScrollTo().performClick()
        waitFor(label(R.string.search_garments))
        waitFor("Cappotto blu")
        compose.onNodeWithText("Cappotto blu").assertIsDisplayed()
        compose.onNodeWithText(label(R.string.edit_garment)).performClick()
        compose.onNodeWithText(label(R.string.garment_name)).performTextReplacement("Cappotto verde")
        compose.onNodeWithText(label(R.string.save)).performScrollTo().performClick()
        waitFor(label(R.string.search_garments))
        waitFor("Cappotto verde")
        compose.onNodeWithText(label(R.string.search_garments)).performTextInput("inesistente")
        compose.onNodeWithText("Cappotto verde").assertDoesNotExist()
        compose.onNodeWithText(label(R.string.no_results)).assertIsDisplayed()
        compose.onNodeWithText(label(R.string.search_garments)).performTextReplacement("")
        compose.onNodeWithText(label(R.string.delete_garment)).performClick()
        compose.onNodeWithText(label(R.string.cancel)).performClick()
        compose.onNodeWithText("Cappotto verde").assertIsDisplayed()
        compose.onNodeWithText(label(R.string.delete_garment)).performClick()
        compose.onAllNodesWithText(label(R.string.delete_garment))[1].performClick()
        waitFor(label(R.string.empty_inventory))
        compose.onNodeWithText("Cappotto verde").assertDoesNotExist()
    }
}
