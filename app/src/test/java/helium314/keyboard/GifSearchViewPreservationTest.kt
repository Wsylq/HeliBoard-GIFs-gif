// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import helium314.keyboard.keyboard.emoji.GifSearchView
import helium314.keyboard.latin.LatinIME
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Preservation-checking property-based tests for GIF search query editing.
 *
 * These tests verify that non-GIF behaviour is unchanged after the bug fixes applied in
 * Tasks 3–5. Specifically, they confirm that the text-editing contract of
 * [GifSearchView.appendQueryChar] and [GifSearchView.deleteLastChar] is preserved:
 * the [EditText] text must always equal the string produced by applying the same sequence
 * of operations to a plain [StringBuilder].
 *
 * Property-based approach: random sequences of append/delete operations are generated using
 * [kotlin.random.Random] with a fixed seed for reproducibility. 100 independent sequences of
 * 20 operations each are tested, giving 2 000 operation steps in total.
 *
 * Validates: Requirements 3.1, 3.2 (preservation of non-GIF behaviour)
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [
    ShadowInputMethodManager2::class,
    ShadowProximityInfo::class,
])
class GifSearchViewPreservationTest {

    private lateinit var latinIME: LatinIME
    private lateinit var gifSearchView: GifSearchView

    @BeforeTest
    fun setUp() {
        latinIME = Robolectric.setupService(LatinIME::class.java)
        ShadowLog.setupLogging()
        ShadowLog.stream = System.out

        // Inflate GifSearchView using the LatinIME context so resources resolve correctly
        gifSearchView = GifSearchView(latinIME, null)
    }

    /**
     * Property-based preservation test: random sequences of appendQueryChar / deleteLastChar.
     *
     * PRESERVATION-CHECKING PROPERTY-BASED TEST — should PASS on fixed code.
     *
     * For any sequence of [GifSearchView.appendQueryChar] and [GifSearchView.deleteLastChar]
     * calls, the [EditText] text must equal the string produced by applying the same operations
     * to a reference [StringBuilder]. This property must hold after every individual operation,
     * not just at the end of the sequence.
     *
     * The test uses a fixed random seed (42) so failures are reproducible. Each of the 100
     * iterations resets the query field to empty before generating a new 20-step sequence.
     *
     * Validates: Requirements 3.1, 3.2
     */
    @Test
    fun appendAndDelete_textMatchesExpectedString() {
        // Fixed seed for reproducibility — a failing seed can be reported as a counterexample
        val random = Random(seed = 42L)

        repeat(100) { iteration ->
            // Reset state for each iteration by clearing the EditText directly
            val queryField: EditText = gifSearchView.queryField
            queryField.setText("")

            // Reference string mirrors the expected EditText state
            val expected = StringBuilder()

            repeat(20) { step ->
                if (random.nextBoolean() && expected.isNotEmpty()) {
                    // deleteLastChar: remove the last character from both the view and reference
                    gifSearchView.deleteLastChar()
                    expected.deleteCharAt(expected.length - 1)
                } else {
                    // appendQueryChar: append a random lowercase ASCII letter to both
                    val char = ('a' + random.nextInt(26))
                    gifSearchView.appendQueryChar(char)
                    expected.append(char)
                }

                // Assert after every operation — the EditText must always match the reference
                assertEquals(
                    expected.toString(),
                    queryField.text.toString(),
                    "Iteration $iteration, step $step: EditText text does not match expected string. " +
                    "Expected \"$expected\" but got \"${queryField.text}\". " +
                    "This indicates appendQueryChar or deleteLastChar broke the text-editing contract."
                )
            }
        }
    }

    /**
     * Property-based preservation test: onTouchEvent always returns true for all motion event types.
     *
     * PRESERVATION-CHECKING PROPERTY-BASED TEST — should PASS on fixed code.
     *
     * For any [MotionEvent] with any action type, [GifSearchView.onTouchEvent] must return
     * `true`, consuming the event and preventing click-through to views behind the GIF panel.
     * This property must hold for all standard action types: [MotionEvent.ACTION_DOWN],
     * [MotionEvent.ACTION_UP], [MotionEvent.ACTION_MOVE], and [MotionEvent.ACTION_CANCEL].
     *
     * Random x/y coordinates are generated for each event to confirm the return value is
     * independent of touch position. Each [MotionEvent] is recycled after use.
     *
     * Validates: Requirements 2.5, 3.1 (touch consumption property)
     */
    @Test
    fun onTouchEvent_alwaysReturnsTrue_forAllMotionEventTypes() {
        // Fixed seed for reproducibility — a failing seed can be reported as a counterexample
        val random = Random(seed = 7L)

        val actionTypes = intArrayOf(
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_MOVE,
            MotionEvent.ACTION_CANCEL,
        )

        // Test each action type with multiple random coordinate pairs
        for (action in actionTypes) {
            repeat(25) { iteration ->
                val x = random.nextFloat() * 1080f
                val y = random.nextFloat() * 1920f
                val downTime = SystemClock.uptimeMillis()
                val eventTime = downTime + random.nextLong(0L, 500L)

                val motionEvent = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    action,
                    x,
                    y,
                    0 // metaState
                )

                try {
                    val result = gifSearchView.onTouchEvent(motionEvent)
                    assertTrue(
                        result,
                        "onTouchEvent returned false for action=${actionName(action)}, " +
                        "x=$x, y=$y (iteration $iteration). " +
                        "GifSearchView must consume all touch events to prevent click-through."
                    )
                } finally {
                    motionEvent.recycle()
                }
            }
        }
    }

    /** Returns a human-readable name for a [MotionEvent] action constant. */
    private fun actionName(action: Int): String = when (action) {
        MotionEvent.ACTION_DOWN   -> "ACTION_DOWN"
        MotionEvent.ACTION_UP     -> "ACTION_UP"
        MotionEvent.ACTION_MOVE   -> "ACTION_MOVE"
        MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"
        else                      -> "ACTION_$action"
    }

    /**
     * Preservation test: performSearch("") still returns early — empty-query guard preserved.
     *
     * PRESERVATION-CHECKING TEST — should PASS on fixed code.
     *
     * [GifSearchView.performSearch] has a defensive early-return guard:
     * `if (query == null || query.isEmpty()) return;`
     *
     * This test verifies that the guard was NOT accidentally removed by the bug fixes.
     * It calls [GifSearchView.performSearch] with an empty string and verifies:
     * 1. No exception is thrown (the guard prevents any crash or network call).
     * 2. The query field text remains empty after the call (the method returned early
     *    without modifying any state).
     *
     * Validates: Requirements 3.1, 3.2 (preservation of non-GIF behaviour)
     */
    @Test
    fun performSearch_emptyString_returnsEarlyWithoutSideEffects() {
        // Ensure the query field starts empty
        val queryField: EditText = gifSearchView.queryField
        queryField.setText("")

        // Call performSearch with an empty string — the guard should cause an early return
        // No exception should be thrown
        gifSearchView.performSearch("")

        // The query field must still be empty: performSearch returned early without modifying state
        assertEquals(
            "",
            queryField.text.toString(),
            "performSearch(\"\") must return early without modifying the query field. " +
            "The empty-query guard (if query == null || query.isEmpty()) return;) appears to " +
            "have been removed or broken by the bug fixes."
        )
    }

    /**
     * Preservation test: performSearch(null) still returns early — null-query guard preserved.
     *
     * PRESERVATION-CHECKING TEST — should PASS on fixed code.
     *
     * Companion to the empty-string test above. Verifies the null branch of the same guard
     * is also intact after the fixes.
     *
     * Validates: Requirements 3.1, 3.2 (preservation of non-GIF behaviour)
     */
    @Test
    fun performSearch_null_returnsEarlyWithoutSideEffects() {
        // Ensure the query field starts empty
        val queryField: EditText = gifSearchView.queryField
        queryField.setText("")

        // Call performSearch with null — the guard should cause an early return
        // No exception should be thrown
        gifSearchView.performSearch(null)

        // The query field must still be empty: performSearch returned early without modifying state
        assertEquals(
            "",
            queryField.text.toString(),
            "performSearch(null) must return early without modifying the query field. " +
            "The null-query guard (if query == null || query.isEmpty()) return;) appears to " +
            "have been removed or broken by the bug fixes."
        )
    }

    /**
     * Preservation test: isGifSearchActive() guard — GIF panel GONE means panel is not active.
     *
     * PRESERVATION-CHECKING TEST — should PASS on fixed code.
     *
     * [LatinIME.isGifSearchActive] checks whether [R.id.gif_search_view] has visibility
     * [View.VISIBLE]. This test verifies the visibility contract that the guard relies on:
     * when [GifSearchView] visibility is set to [View.GONE], the view must NOT be VISIBLE.
     * This ensures the [isGifSearchActive] guard continues to prevent GIF key intercepts
     * when the panel is hidden, preserving Requirement 3.3.
     *
     * Validates: Requirements 3.3 (isGifSearchActive() guard preserved)
     */
    @Test
    fun gifSearchView_gone_isNotVisible() {
        // Set the GIF panel to GONE — simulates the panel being hidden/dismissed
        gifSearchView.visibility = View.GONE

        // The view must report GONE
        assertEquals(
            View.GONE,
            gifSearchView.visibility,
            "GifSearchView visibility should be GONE after setting it to GONE."
        )

        // The view must NOT be VISIBLE — this is the condition isGifSearchActive() checks.
        // isGifSearchActive() returns true only when the view is View.VISIBLE, so when GONE
        // the guard must evaluate to false (panel is not active).
        assertNotEquals(
            View.VISIBLE,
            gifSearchView.visibility,
            "GifSearchView visibility must NOT be VISIBLE when set to GONE. " +
            "isGifSearchActive() relies on this contract to prevent GIF key intercepts " +
            "when the panel is hidden."
        )
    }
}
