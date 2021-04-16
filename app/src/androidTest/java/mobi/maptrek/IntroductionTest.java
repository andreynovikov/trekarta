/*
 * Copyright 2021 Andrey Novikov
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


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.filters.LargeTest;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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
                        childAtPosition(
                                allOf(withId(R.id.main),
                                        withParent(withId(R.id.view_pager))),
                                0),
                        isDisplayed()));
        textView.check(matches(withText("Offline maps")));

        ViewInteraction appCompatImageButton = onView(
                allOf(withId(R.id.next),
                        childAtPosition(
                                allOf(withId(R.id.background),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                        0)),
                                6),
                        isDisplayed()));


        appCompatImageButton.perform(click());

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.title), withText("Places"),
                        childAtPosition(
                                allOf(withId(R.id.main),
                                        withParent(withId(R.id.view_pager))),
                                0),
                        isDisplayed()));
        textView2.check(matches(withText("Places")));

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withId(R.id.next),
                        childAtPosition(
                                allOf(withId(R.id.background),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                6),
                        isDisplayed()));
        appCompatImageButton2.perform(click());

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.title), withText("Tracks"),
                        childAtPosition(
                                allOf(withId(R.id.main),
                                        withParent(withId(R.id.view_pager))),
                                0),
                        isDisplayed()));
        textView3.check(matches(withText("Tracks")));

        ViewInteraction appCompatImageButton3 = onView(
                allOf(withId(R.id.next),
                        childAtPosition(
                                allOf(withId(R.id.background),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                6),
                        isDisplayed()));
        appCompatImageButton3.perform(click());

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.title), withText("Off-road"),
                        childAtPosition(
                                allOf(withId(R.id.main),
                                        withParent(withId(R.id.view_pager))),
                                0),
                        isDisplayed()));
        textView4.check(matches(withText("Off-road")));

        ViewInteraction appCompatImageButton4 = onView(
                allOf(withId(R.id.next),
                        childAtPosition(
                                allOf(withId(R.id.background),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                6),
                        isDisplayed()));
        appCompatImageButton4.perform(click());

        ViewInteraction textView5 = onView(
                allOf(withId(R.id.title), withText("Hiking"),
                        childAtPosition(
                                allOf(withId(R.id.main),
                                        withParent(withId(R.id.view_pager))),
                                0),
                        isDisplayed()));
        textView5.check(matches(withText("Hiking")));

        ViewInteraction appCompatImageButton5 = onView(
                allOf(withId(R.id.next),
                        childAtPosition(
                                allOf(withId(R.id.background),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                6),
                        isDisplayed()));
        appCompatImageButton5.perform(click());

        ViewInteraction textView6 = onView(
                allOf(withId(R.id.title), withText("Cycling"),
                        childAtPosition(
                                allOf(withId(R.id.main),
                                        withParent(withId(R.id.view_pager))),
                                0),
                        isDisplayed()));
        textView6.check(matches(withText("Cycling")));

        ViewInteraction appCompatImageButton6 = onView(
                allOf(withId(R.id.next),
                        childAtPosition(
                                allOf(withId(R.id.background),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                6),
                        isDisplayed()));
        appCompatImageButton5.perform(click());

        ViewInteraction textView7 = onView(
                allOf(withId(R.id.title), withText("Skiing and skating"),
                        childAtPosition(
                                allOf(withId(R.id.main),
                                        withParent(withId(R.id.view_pager))),
                                0),
                        isDisplayed()));
        textView7.check(matches(withText("Skiing and skating")));

        if (BuildConfig.FULL_VERSION) {
            ViewInteraction appCompatImageButton7 = onView(
                    allOf(withId(R.id.next),
                            childAtPosition(
                                    allOf(withId(R.id.background),
                                            childAtPosition(
                                                    withId(android.R.id.content),
                                                    0)),
                                    6),
                            isDisplayed()));
            appCompatImageButton7.perform(click());

            ViewInteraction textView8 = onView(
                    allOf(withId(R.id.title), withText("Night mode"),
                            childAtPosition(
                                    allOf(withId(R.id.main),
                                            withParent(withId(R.id.view_pager))),
                                    0),
                            isDisplayed()));
            textView8.check(matches(withText("Night mode")));
        }

        ViewInteraction button = onView(
                allOf(withId(R.id.done),
                        childAtPosition(
                                allOf(withId(R.id.background),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                7),
                        isDisplayed()));
        button.check(matches(isDisplayed()));

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.done), withText("DONE"),
                        childAtPosition(
                                allOf(withId(R.id.background),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                7),
                        isDisplayed()));
        appCompatButton.perform(click());

        ViewInteraction frameLayout = onView(
                allOf(withId(R.id.contentPanel),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                IsInstanceOf.instanceOf(android.widget.FrameLayout.class),
                                                1)),
                                8),
                        isDisplayed()));
        frameLayout.check(matches(isDisplayed()));

        ViewInteraction imageButton = onView(
                allOf(withId(R.id.actionButton),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                IsInstanceOf.instanceOf(android.widget.FrameLayout.class),
                                                1)),
                                10),
                        isDisplayed()));
        imageButton.check(matches(isDisplayed()));

        pressBack();

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.actionPanel),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.coordinatorLayout),
                                        7),
                                1),
                        isDisplayed()));
        linearLayout.check(matches(isDisplayed()));
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
