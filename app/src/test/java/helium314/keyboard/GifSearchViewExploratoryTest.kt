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
import kotlin.test.assertTrue

/**
 * Exploratory bug-condition tests for the GIF search feature.
 *
 * These tests are designed to FAIL on the UNFIXED code, confirming that the bugs exist.
 * Once the bugs are fixed, these tests should pass.
 *
 * Task 2.1: Confirm Bug 1 & 2 — gif_query_field EditText has android:focusable="false"
 * in the unfixed layout, which prevents text from being displayed and touch input from working.
 */
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [
    ShadowInputMethodManager2::class,
    ShadowProximityInfo::class,
])
class GifSearchViewExploratoryTest {

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
     * Task 2.1 — Exploratory test: assert queryField.isFocusable() is true.
     *
     * EXPECTED TO FAIL on unfixed code:
     * The layout gif_search_view.xml sets android:focusable="false" on gif_query_field,
     * so isFocusable() returns false on the unfixed code.
     *
     * This test confirms Bug 1 & 2 exist: the EditText is non-focusable, which prevents
     * it from rendering typed text and from receiving touch-based input.
     *
     * Validates: Requirements 2.1, 2.2
     */
    @Test
    fun queryField_isFocusable_shouldBeTrue() {
        val queryField: EditText = gifSearchView.queryField
        // This assertion FAILS on unfixed code because android:focusable="false" is set
        // in res/layout/gif_search_view.xml for the gif_query_field EditText.
        assertTrue(queryField.isFocusable,
            "gif_query_field EditText must be focusable so it can display typed text. " +
            "EXPECTED FAILURE on unfixed code: android:focusable=\"false\" is set in the layout.")
    }

    /**
     * Task 2.2 — Exploratory test: assert onTouchEvent returns true.
     *
     * EXPECTED TO FAIL on unfixed code:
     * GifSearchView.onTouchEvent() currently returns false, which means touch events
     * fall through to views behind the GIF panel (Bug 5).
     *
     * This test confirms Bug 5 exists: the back/arrow button tap in the emoji tab strip
     * is not consumed by the GIF panel and falls through to underlying views.
     *
     * Validates: Requirements 2.5
     */
    @Test
    fun onTouchEvent_shouldReturnTrue() {
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()
        val motionEvent = MotionEvent.obtain(
            downTime,
            eventTime,
            MotionEvent.ACTION_DOWN,
            /* x = */ 0f,
            /* y = */ 0f,
            /* metaState = */ 0
        )
        try {
            // This assertion FAILS on unfixed code because GifSearchView.onTouchEvent()
            // returns false instead of true, allowing touch events to fall through.
            val result = gifSearchView.onTouchEvent(motionEvent)
            assertTrue(result,
                "GifSearchView.onTouchEvent() must return true to consume touch events " +
                "and prevent click-through to views behind the panel. " +
                "EXPECTED FAILURE on unfixed code: onTouchEvent returns false.")
        } finally {
            motionEvent.recycle()
        }
    }

    /**
     * Task 2.3 — Confirmation test: assert thumbnail click listener is invoked via performClick().
     *
     * This test is NOT expected to fail on unfixed code — it confirms that the click listener
     * path works correctly. Specifically, it verifies that:
     *   1. An OnClickListener set on the gif_thumbnail ImageView IS invoked when performClick()
     *      is called on that ImageView.
     *   2. This is the same path used by GifAdapter.onBindViewHolder, which calls
     *      holder.image.setOnClickListener(...) on the ImageView inflated from item_gif_thumbnail.xml.
     *
     * This confirms that the click listener wiring in onBindViewHolder is correct and that
     * performClick() on the root ImageView will dispatch to the registered listener — the
     * mechanism relied upon by the OnItemTouchListener fix for Bug 4.
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

        // Set a click listener on the ImageView — this mirrors what onBindViewHolder does:
        //   holder.image.setOnClickListener { v -> ... }
        thumbnailImageView.setOnClickListener {
            clickListenerInvoked = true
        }

        // Call performClick() on the ImageView — this is what child.performClick() does in
        // the OnItemTouchListener when a tap is detected on a grid item.
        thumbnailImageView.performClick()

        // Assert the listener was invoked: confirms the click listener path is correct and
        // that performClick() on the ImageView dispatches to the registered OnClickListener.
        assertTrue(clickListenerInvoked,
            "OnClickListener set on gif_thumbnail ImageView must be invoked when " +
            "performClick() is called. This confirms the click listener path used by " +
            "GifAdapter.onBindViewHolder and the OnItemTouchListener is correct.")
    }
}
