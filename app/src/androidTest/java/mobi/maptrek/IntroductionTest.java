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

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4ClassRunner.class)
public class IntroductionTest {

    @Rule
    public ActivityScenarioRule<MainActivity> mActivityTestRule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void introductionTest() {
        ViewInteraction textView = onView(
                allOf(withId(R.id.title), withText("Offline maps"),
                        withParent(allOf(withId(R.id.main),
                                withParent(withId(R.id.view_pager)))),
                        isDisplayed()));
        textView.check(matches(withText("Offline maps")));

        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.next),
                        withParent(allOf(withId(R.id.background),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.title), withText("Places"),
                        withParent(allOf(withId(R.id.main),
                                withParent(withId(R.id.view_pager)))),
                        isDisplayed()));
        textView2.check(matches(withText("Places")));

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withId(R.id.next),
                        withParent(allOf(withId(R.id.background),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        appCompatImageButton2.perform(click());

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.title), withText("Tracks"),
                        withParent(allOf(withId(R.id.main),
                                withParent(withId(R.id.view_pager)))),
                        isDisplayed()));
        textView3.check(matches(withText("Tracks")));

        ViewInteraction appCompatImageButton3 = onView(
                allOf(withId(R.id.next),
                        withParent(allOf(withId(R.id.background),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        appCompatImageButton3.perform(click());

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.title), withText("Off-road"),
                        withParent(allOf(withId(R.id.main),
                                withParent(withId(R.id.view_pager)))),
                        isDisplayed()));
        textView4.check(matches(withText("Off-road")));

        ViewInteraction appCompatImageButton4 = onView(
                allOf(withId(R.id.next),
                        withParent(allOf(withId(R.id.background),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        appCompatImageButton4.perform(click());

        ViewInteraction textView5 = onView(
                allOf(withId(R.id.title), withText("Hiking"),
                        withParent(allOf(withId(R.id.main),
                                withParent(withId(R.id.view_pager)))),
                        isDisplayed()));
        textView5.check(matches(withText("Hiking")));

        ViewInteraction appCompatImageButton5 = onView(
                allOf(withId(R.id.next),
                        withParent(allOf(withId(R.id.background),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        appCompatImageButton5.perform(click());

        ViewInteraction textView6 = onView(
                allOf(withId(R.id.title), withText("Cycling"),
                        withParent(allOf(withId(R.id.main),
                                withParent(withId(R.id.view_pager)))),
                        isDisplayed()));
        textView6.check(matches(withText("Cycling")));

        ViewInteraction appCompatImageButton6 = onView(
                allOf(withId(R.id.next),
                        withParent(allOf(withId(R.id.background),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        appCompatImageButton6.perform(click());

        ViewInteraction textView7 = onView(
                allOf(withId(R.id.title), withText("Skiing and skating"),
                        withParent(allOf(withId(R.id.main),
                                withParent(withId(R.id.view_pager)))),
                        isDisplayed()));
        textView7.check(matches(withText("Skiing and skating")));

        if (BuildConfig.FULL_VERSION) {
            ViewInteraction appCompatImageButton7 = onView(
                    allOf(withId(R.id.next),
                            withParent(allOf(withId(R.id.background),
                                    withParent(withId(android.R.id.content)))),
                            isDisplayed()));
            appCompatImageButton7.perform(click());

            ViewInteraction textView8 = onView(
                    allOf(withId(R.id.title), withText("Night mode"),
                            withParent(allOf(withId(R.id.main),
                                    withParent(withId(R.id.view_pager)))),
                            isDisplayed()));
            textView8.check(matches(withText("Night mode")));
        }

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.done), withText("DONE"),
                        withParent(allOf(withId(R.id.background),
                                withParent(withId(android.R.id.content)))),
                        isDisplayed()));
        appCompatButton.perform(click());

        ViewInteraction imageButton = onView(
                allOf(withId(R.id.actionButton),
                        withParent(allOf(withId(R.id.coordinatorLayout),
                                withParent(IsInstanceOf.instanceOf(android.widget.FrameLayout.class)))),
                        isDisplayed()));
        imageButton.check(matches(isDisplayed()));

        pressBack();

        ViewInteraction view = onView(
                allOf(withId(R.id.mapView),
                        withParent(withParent(withId(android.R.id.content))),
                        isDisplayed()));
        view.check(matches(isDisplayed()));

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.actionPanel),
                        withParent(withParent(withId(R.id.coordinatorLayout))),
                        isDisplayed()));
        linearLayout.check(matches(isDisplayed()));
    }
}
