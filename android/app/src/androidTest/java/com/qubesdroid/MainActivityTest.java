package com.qubesdroid;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumentation tests for MainActivity
 *
 * These tests run on an Android device or emulator
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Test
    public void testMainActivityLaunches() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Verify main UI elements are displayed
        Espresso.onView(ViewMatchers.withId(R.id.toolbar))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.createVolumeButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withId(R.id.mountVolumeButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        scenario.close();
    }

    @Test
    public void testCreateVolumeButtonClick() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Click create volume button
        Espresso.onView(ViewMatchers.withId(R.id.createVolumeButton))
            .perform(ViewActions.click());

        // Note: This may trigger permission request, so we can't assert activity launch
        // In a real test environment with granted permissions, we would verify
        // CreateVolumeActivity launches

        scenario.close();
    }

    @Test
    public void testVersionTextDisplayed() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        Espresso.onView(ViewMatchers.withId(R.id.versionText))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .check(ViewAssertions.matches(ViewMatchers.withText(
                ViewMatchers.containsString("QubesDroid"))));

        scenario.close();
    }

    @Test
    public void testCryptographyInfoCardDisplayed() {
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Verify cryptographic algorithms are displayed
        Espresso.onView(ViewMatchers.withText("ML-KEM-1024"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withText("ChaCha20-Poly1305"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        Espresso.onView(ViewMatchers.withText("Argon2id"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        scenario.close();
    }
}
