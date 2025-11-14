package com.qubesdroid;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation tests for CreateVolumeActivity
 */
@RunWith(AndroidJUnit4.class)
public class CreateVolumeActivityTest {

    @Test
    public void testCreateVolumeUIElements() {
        ActivityScenario<CreateVolumeActivity> scenario =
            ActivityScenario.launch(CreateVolumeActivity.class);

        // Verify UI elements are displayed
        Espresso.onView(ViewMatchers.withId(R.id.volumeNameInput))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.volumeSizeSlider))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.passwordInput))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.confirmPasswordInput))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.createVolumeButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        scenario.close();
    }

    @Test
    public void testPasswordValidation() {
        ActivityScenario<CreateVolumeActivity> scenario =
            ActivityScenario.launch(CreateVolumeActivity.class);

        // Enter volume name
        Espresso.onView(ViewMatchers.withId(R.id.volumeNameInput))
            .perform(ViewActions.typeText("test_volume"), ViewActions.closeSoftKeyboard());

        // Enter short password (should fail validation)
        Espresso.onView(ViewMatchers.withId(R.id.passwordInput))
            .perform(ViewActions.typeText("short"), ViewActions.closeSoftKeyboard());

        // Click create button
        Espresso.onView(ViewMatchers.withId(R.id.createVolumeButton))
            .perform(ViewActions.click());

        // Verify error message is shown
        Espresso.onView(ViewMatchers.withText("Password must be at least 8 characters"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        scenario.close();
    }

    @Test
    public void testPasswordMismatch() {
        ActivityScenario<CreateVolumeActivity> scenario =
            ActivityScenario.launch(CreateVolumeActivity.class);

        // Enter volume name
        Espresso.onView(ViewMatchers.withId(R.id.volumeNameInput))
            .perform(ViewActions.typeText("test_volume"), ViewActions.closeSoftKeyboard());

        // Enter passwords that don't match
        Espresso.onView(ViewMatchers.withId(R.id.passwordInput))
            .perform(ViewActions.typeText("password123"), ViewActions.closeSoftKeyboard());

        Espresso.onView(ViewMatchers.withId(R.id.confirmPasswordInput))
            .perform(ViewActions.typeText("password456"), ViewActions.closeSoftKeyboard());

        // Click create button
        Espresso.onView(ViewMatchers.withId(R.id.createVolumeButton))
            .perform(ViewActions.click());

        // Verify error message
        Espresso.onView(ViewMatchers.withText("Passwords do not match"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        scenario.close();
    }

    @Test
    public void testPostQuantumBadgeDisplayed() {
        ActivityScenario<CreateVolumeActivity> scenario =
            ActivityScenario.launch(CreateVolumeActivity.class);

        // Verify post-quantum security badge is shown
        Espresso.onView(ViewMatchers.withText("Post-Quantum Security"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withText("ML-KEM-1024"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        scenario.close();
    }
}
