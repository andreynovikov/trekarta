/*
 * Copyright 2022 Andrey Novikov
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


import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import android.content.Intent;

@LargeTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class IntroductionTest {
    static {
        BuildConfig.IS_TESTING.set(true); // Do not show targeted advices
    }

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityTestRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void beforeTest() {
        mActivityTestRule.getScenario().onActivity(activity -> activity.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)));
    }

    @Test
    public void introductionTest() {
        // Offline maps slide
        ViewInteraction textView = onView(
                allOf(
                        withId(R.id.title),
                        withText("Offline maps"),
                        withParent(
                                allOf(
                                        withId(R.id.main),
                                        withParent(withId(R.id.view_pager))
                                )
                        ),
                        isDisplayed()
                )
        );
        textView.check(matches(withText("Offline maps")));

        ViewInteraction appCompatImageButton = onView(
                allOf(
                        withId(R.id.next),
                        withParent(
                                allOf(
                                        withId(R.id.background),
                                        withParent(withId(android.R.id.content))
                                )
                        ),
                        isDisplayed()
                )
        );
        appCompatImageButton.perform(click());

        // Places slide
        ViewInteraction textView2 = onView(
                allOf(
                        withId(R.id.title),
                        withText("Places"),
                        withParent(
                                allOf(
                                        withId(R.id.main),
                                        withParent(withId(R.id.view_pager))
                                )
                        ),
                        isDisplayed()
                )
        );
        textView2.check(matches(withText("Places")));

        appCompatImageButton.perform(click());

        // Tracks slide
        ViewInteraction textView3 = onView(
                allOf(
                        withId(R.id.title),
                        withText("Tracks"),
                        withParent(
                                allOf(
                                        withId(R.id.main),
                                        withParent(withId(R.id.view_pager))
                                )
                        ),
                        isDisplayed()
                )
        );
        textView3.check(matches(withText("Tracks")));

        appCompatImageButton.perform(click());

        // Off-road slide
        ViewInteraction textView4 = onView(
                allOf(
                        withId(R.id.title),
                        withText("Off-road"),
                        withParent(
                                allOf(
                                        withId(R.id.main),
                                        withParent(withId(R.id.view_pager))
                                )
                        ),
                        isDisplayed()
                )
        );
        textView4.check(matches(withText("Off-road")));

        appCompatImageButton.perform(click());

        // Hiking slide
        ViewInteraction textView5 = onView(
                allOf(withId(R.id.title), withText("Hiking"),
                        withParent(allOf(withId(R.id.main),
                                withParent(withId(R.id.view_pager)))),
                        isDisplayed()));
        textView5.check(matches(withText("Hiking")));

        appCompatImageButton.perform(click());

        // Cycling slide
        ViewInteraction textView6 = onView(
                allOf(
                        withId(R.id.title),
                        withText("Cycling"),
                        withParent(
                                allOf(
                                        withId(R.id.main),
                                        withParent(withId(R.id.view_pager))
                                )
                        ),
                        isDisplayed()
                )
        );
        textView6.check(matches(withText("Cycling")));

        appCompatImageButton.perform(click());

        // Skiing and skating slide
        ViewInteraction textView7 = onView(
                allOf(
                        withId(R.id.title),
                        withText("Skiing and skating"),
                        withParent(
                                allOf(
                                        withId(R.id.main),
                                        withParent(withId(R.id.view_pager))
                                )
                        ),
                        isDisplayed()
                )
        );
        textView7.check(matches(withText("Skiing and skating")));

        appCompatImageButton.perform(click());

        // Night mode slide
        ViewInteraction textView8 = onView(
                allOf(
                        withId(R.id.title),
                        withText("Night mode"),
                        withParent(
                                allOf(
                                        withId(R.id.main),
                                        withParent(withId(R.id.view_pager))
                                )
                        ),
                        isDisplayed()
                )
        );
        textView8.check(matches(withText("Night mode")));

        // Next button is gone
        appCompatImageButton.check(doesNotExist());

        // Close introduction
        ViewInteraction appCompatButton = onView(
                allOf(
                        withId(R.id.done),
                        withText("DONE"),
                        withParent(
                                allOf(
                                        withId(R.id.background),
                                        withParent(withId(android.R.id.content))
                                )
                        ),
                        isDisplayed()
                )
        );
        appCompatButton.perform(click());

        // World map download dialog is displayed - close it
        ViewInteraction appCompatButton2 = onView(
                allOf(
                        withId(android.R.id.button2),
                        withText("SKIP"),
                        isDisplayed()
                )
        ).inRoot(isDialog());
        appCompatButton2.perform(click());

        // Main map view is displayed
        ViewInteraction view = onView(
                allOf(
                        withId(R.id.mapView),
                        withParent(withParent(withId(android.R.id.content))),
                        isDisplayed()
                )
        );
        view.check(matches(isDisplayed()));

        // Bottom action bar is displayed
        ViewInteraction linearLayout = onView(
                allOf(
                        withId(R.id.actionPanel),
                        withParent(withParent(withId(R.id.coordinatorLayout))),
                        isDisplayed()
                )
        );
        linearLayout.check(matches(isDisplayed()));
    }
}
