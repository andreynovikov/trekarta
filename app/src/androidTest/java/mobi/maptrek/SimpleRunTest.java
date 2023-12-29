/*
 * Copyright 2020 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek;


import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class SimpleRunTest {
    static {
        BuildConfig.IS_TESTING.set(true); // Do not show targeted advices
    }

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityTestRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void beforeTest() {
        mActivityTestRule.getScenario().onActivity(activity -> activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)));
        Configuration.setLastSeenIntroduction(0);
    }

    @Test
    public void mainActivityTest() {
        try {
            // Close Introduction panel if test is running separately
            ViewInteraction appCompatButton = onView(
                    allOf(
                            withId(R.id.skip),
                            withText("SKIP"),
                            isDisplayed()
                    )
            );
            appCompatButton.perform(click());
        } catch (NoMatchingViewException notExist) {
            // ignore - test is run in sequence
        }

        // World map download dialog is displayed - close it
        ViewInteraction appCompatButton2 = onView(
                allOf(
                        withId(android.R.id.button2),
                        withText("SKIP"),
                        isDisplayed()
                )
        ).inRoot(isDialog());
        appCompatButton2.perform(click());

        // Press 'More' button

        ViewInteraction appCompatImageButton = onView(
                allOf(
                        withId(R.id.moreButton),
                        childAtPosition(
                                allOf(
                                        withId(R.id.actionPanel),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                1
                                        )
                                ),
                                4
                        ),
                        isDisplayed()
                )
        );
        appCompatImageButton.perform(click());

        // Press 'About' menu item

        DataInteraction linearLayout = onData(anything())
                .inAdapterView(
                        allOf(
                                withId(android.R.id.list),
                                childAtPosition(
                                    withClassName(is("android.widget.FrameLayout")),
                                    0
                                )
                        )
                )
                .atPosition(0);
        linearLayout.perform(click());

        // Check for 'Trekarta' title

        ViewInteraction textView = onView(
                allOf(
                        withId(R.id.title),
                        withContentDescription("Trekarta"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(android.widget.LinearLayout.class),
                                        0
                                ),
                                1
                        ),
                        isDisplayed()
                )
        );
        textView.check(matches(withContentDescription("Trekarta")));

        pressBack();

        // Press 'Places' button

        ViewInteraction appCompatImageButton2 = onView(
                allOf(
                        withId(R.id.placesButton),
                        childAtPosition(
                                allOf(
                                        withId(R.id.actionPanel),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                1
                                        )
                                ),
                                2
                        ),
                        isDisplayed()
                )
        );
        appCompatImageButton2.perform(click());

        pressBack();

        // Open Map settings menu

        ViewInteraction appCompatImageButton3 = onView(
                allOf(
                        withId(R.id.mapsButton),
                        childAtPosition(
                                allOf(
                                        withId(R.id.actionPanel),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                1
                                        )
                                ),
                                3
                        ),
                        isDisplayed()
                )
        );
        appCompatImageButton3.perform(longClick());

        // Press 'Legend' menu item

        DataInteraction linearLayout2 = onData(anything())
                .inAdapterView(
                        allOf(
                                withId(android.R.id.list),
                                childAtPosition(
                                    withClassName(is("android.widget.FrameLayout")),
                                    0
                                )
                        )
                )
                .atPosition(8);
        linearLayout2.perform(click());

        // Find first legend title

        ViewInteraction textView2 = onView(
                allOf(
                        withId(R.id.name),
                        withText("Administrative"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.list),
                                        0
                                ),
                                0
                        ),
                        isDisplayed()
                )
        );
        textView2.check(matches(withText("Administrative")));

        pressBack();

        // Long press 'Location' button

        ViewInteraction appCompatImageButton4 = onView(
                allOf(
                        withId(R.id.locationButton),
                        childAtPosition(
                                allOf(
                                        withId(R.id.actionPanel),
                                        childAtPosition(
                                                withClassName(is("android.widget.RelativeLayout")),
                                                1
                                        )
                                ),
                                0
                        ),
                        isDisplayed()
                )
        );
        appCompatImageButton4.perform(longClick());

        // Check presence of 'Share' button

        ViewInteraction imageButton2 = onView(
                allOf(
                        withId(R.id.shareButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.instanceOf(android.widget.GridLayout.class),
                                        2
                                ),
                                0
                        ),
                        isDisplayed()
                )
        );
        imageButton2.check(matches(isDisplayed()));

        pressBack();
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
