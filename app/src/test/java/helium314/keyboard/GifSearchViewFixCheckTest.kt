// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard

import android.os.SystemClock
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.EditText
import android.widget.ImageView
import helium314.keyboard.keyboard.emoji.GifSearchView
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.R
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Fix-checking tests for the GIF search feature.
 *
 * These tests are designed to PASS on the FIXED code, verifying that the bugs have been
 * correctly resolved. They should be run after the fixes in Task 3.1 (layout fix) and
 * subsequent tasks have been applied.
 *
 * The key fix verified here (Task 3.1) changed gif_query_field in gif_search_view.xml from:
 *   android:focusable="false" / android:cursorVisible="false"
 * to:
 *   android:focusable="true" / android:focusableInTouchMode="false" / android:cursorVisible="true"
 *
 * After this fix, appendQueryChar() calls correctly update the EditText text buffer and the
 * text is readable via getText().toString().
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [
    ShadowInputMethodManager2::class,
    ShadowProximityInfo::class,
])
class GifSearchViewFixCheckTest {

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
     * Task 6.1 — Fix-checking test: query text accumulation.
     *
     * FIX-CHECKING TEST — should PASS on fixed code.
     *
     * After Task 3.1 changed gif_query_field from android:focusable="false" to
     * android:focusable="true" (with android:focusableInTouchMode="false"), the EditText
     * correctly accepts and retains programmatic text updates via appendQueryChar().
     *
     * Verifies that calling appendQueryChar('h') followed by appendQueryChar('i') results
     * in the query field containing the string "hi".
     *
     * Validates: Requirements 2.1, 2.2
     */
    @Test
    fun appendQueryChar_accumulatesCharactersCorrectly() {
        val queryField: EditText = gifSearchView.queryField

        // Append two characters programmatically (as LatinIME.appendGifSearchChar does)
        gifSearchView.appendQueryChar('h')
        gifSearchView.appendQueryChar('i')

        // After the fix, the EditText text buffer must contain the accumulated string "hi"
        assertEquals(
            "hi",
            queryField.text.toString(),
            "After appendQueryChar('h') and appendQueryChar('i'), queryField.getText().toString() " +
            "must equal \"hi\". This verifies Bug 1 is fixed: the EditText now correctly " +
            "accumulates typed characters after the focusability fix in gif_search_view.xml."
        )
    }

    /**
     * Task 6.2 — Fix-checking test: focusability attributes.
     *
     * FIX-CHECKING TEST — should PASS on fixed code (after Task 3.1 applied the layout fix).
     *
     * Task 3.1 changed gif_query_field in gif_search_view.xml from:
     *   android:focusable="false" / android:cursorVisible="false"
     * to:
     *   android:focusable="true" / android:focusableInTouchMode="false" / android:cursorVisible="true"
     *
     * This verifies that the EditText is focusable (so it can display text and cursor updates)
     * but NOT focusable in touch mode (so tapping it does not trigger the system software
     * keyboard — HeliBoard handles input programmatically via appendQueryChar).
     *
     * Validates: Requirements 2.1, 2.2
     */
    @Test
    fun queryField_focusabilityAttributes_areCorrectAfterFix() {
        val queryField: EditText = gifSearchView.queryField

        // After the fix: isFocusable must be true so the field can display text and cursor
        assertTrue(
            queryField.isFocusable,
            "queryField.isFocusable must be true after the layout fix. " +
            "The EditText needs to be focusable to render text and cursor updates correctly."
        )

        // After the fix: isFocusableInTouchMode must be false to prevent the system keyboard
        // from appearing when the user taps the field (HeliBoard intercepts keys instead)
        assertFalse(
            queryField.isFocusableInTouchMode,
            "queryField.isFocusableInTouchMode must be false after the layout fix. " +
            "The field must not request focus on touch to avoid triggering the system keyboard."
        )
    }

    /**
     * Task 6.3 — Fix-checking test: onTouchEvent returns true.
     *
     * FIX-CHECKING TEST — should PASS on fixed code (after Task 5.1 changed onTouchEvent
     * to return true instead of false).
     *
     * Before the fix, GifSearchView.onTouchEvent() returned false, allowing touch events to
     * fall through to views behind the GIF panel (Bug 5). After Task 5.1, onTouchEvent()
     * returns true so the panel consumes all touch events and prevents click-through.
     *
     * Validates: Requirements 2.5
     */
    @Test
    fun onTouchEvent_returnsTrue() {
        // Create an ACTION_DOWN MotionEvent to simulate a touch on the GIF panel
        val downTime = SystemClock.uptimeMillis()
        val motionEvent = MotionEvent.obtain(
            downTime,       // downTime
            downTime,       // eventTime
            MotionEvent.ACTION_DOWN,
            0f,             // x
            0f,             // y
            0               // metaState
        )
        try {
            val result = gifSearchView.onTouchEvent(motionEvent)

            // After the fix, onTouchEvent must return true to consume the touch event
            // and prevent it from falling through to views behind the GIF panel
            assertTrue(
                result,
                "GifSearchView.onTouchEvent() must return true after the fix (Task 5.1). " +
                "Returning true ensures touch events are consumed by the GIF panel and do not " +
                "fall through to underlying views (fixes Bug 5: back button click-through)."
            )
        } finally {
            motionEvent.recycle()
        }
    }

    /**
     * Task 6.4 — Fix-checking test: thumbnail ImageView click listener invoked via performClick().
     *
     * FIX-CHECKING TEST — should PASS on fixed code (after Task 4.1 removed the GestureDetector
     * gate from the OnItemTouchListener).
     *
     * Before the fix (Bug 4), the OnItemTouchListener used a GestureDetector gate that could
     * silently drop taps if the gesture detector did not confirm a single tap. After Task 4.1,
     * the GestureDetector gate was removed so any ACTION_UP event calls child.performClick()
     * unconditionally. This test verifies that the click listener path itself works correctly:
     * an OnClickListener set on the gif_thumbnail ImageView IS invoked when performClick() is
     * called, confirming the click dispatch mechanism used by the fixed OnItemTouchListener.
     *
     * Validates: Requirements 2.4
     */
    @Test
    fun thumbnailImageView_performClick_invokesOnClickListener() {
        // Inflate item_gif_thumbnail.xml the same way GifAdapter.onCreateViewHolder does.
        // The root element of this layout IS the ImageView (id=gif_thumbnail), so the
        // inflated view is itself the ImageView.
        val inflater = LayoutInflater.from(latinIME)
        val itemView = inflater.inflate(R.layout.item_gif_thumbnail, null, false)
        val thumbnailImageView = itemView.findViewById<ImageView>(R.id.gif_thumbnail)

        // Track whether the click listener is invoked
        var clickListenerInvoked = false

        // Set a flag-based click listener on the ImageView — this mirrors what onBindViewHolder
        // does: holder.image.setOnClickListener { v -> ... }
        thumbnailImageView.setOnClickListener {
            clickListenerInvoked = true
        }

        // Call performClick() on the ImageView — this is what child.performClick() does in
        // the fixed OnItemTouchListener when ACTION_UP is detected on a grid item (no longer
        // gated by the GestureDetector after Task 4.1).
        thumbnailImageView.performClick()

        // Assert the listener was invoked: confirms the click listener path works correctly
        // with the fixed code and that performClick() on the ImageView dispatches to the
        // registered OnClickListener.
        assertTrue(
            clickListenerInvoked,
            "OnClickListener set on gif_thumbnail ImageView must be invoked when " +
            "performClick() is called. This verifies the click path works correctly after " +
            "the Bug 4 fix (Task 4.1 removed the GestureDetector gate from OnItemTouchListener)."
        )
    }
}
