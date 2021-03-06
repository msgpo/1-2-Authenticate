/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 * Modified Copyright 2018 Wilco van Beijnum.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wilco375.onetwoauthenticate.activity;

import android.content.Intent;
import android.preference.Preference;
import android.test.ActivityInstrumentationTestCase2;

import com.wilco375.onetwoauthenticate.R;
import com.wilco375.onetwoauthenticate.TestUtilities;
import com.wilco375.onetwoauthenticate.activity.SettingsAboutActivity;
import com.wilco375.onetwoauthenticate.testability.DependencyInjector;

/**
 * Unit tests for {@link SettingsAboutActivity}.
 *
 * @author klyubin@google.com (Alex Klyubin)
 */
public class SettingsAboutActivityTest
        extends ActivityInstrumentationTestCase2<SettingsAboutActivity> {

    public SettingsAboutActivityTest() {
        super(SettingsAboutActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        DependencyInjector.resetForIntegrationTesting(getInstrumentation().getTargetContext());
        TestUtilities.withLaunchPreventingStartActivityListenerInDependencyResolver();
    }

    @Override
    protected void tearDown() throws Exception {
        DependencyInjector.close();

        super.tearDown();
    }

    public void testVersionTakenFromPackageVersion() throws Exception {
        Preference preference = getActivity().findPreference("version");
        String expectedVersion =
                getInstrumentation().getTargetContext().getPackageManager().getPackageInfo(
                        getActivity().getPackageName(), 0).versionName;
        assertEquals(expectedVersion, preference.getSummary());
    }

    public void testOpenSourcePreferenceOpensUrl() {
        Intent intent = tapOnPreferenceAndCatchFiredIntent("opensource");
        assertDefaultViewActionIntent(
                getInstrumentation().getTargetContext().getString(R.string.opensource_page_url),
                intent);
    }

    private static void assertDefaultViewActionIntent(String expectedData, Intent intent) {
        assertEquals("android.intent.action.VIEW", intent.getAction());
        assertEquals(expectedData, intent.getDataString());
    }

    private Intent tapOnPreferenceAndCatchFiredIntent(String preferenceKey) {
        TestUtilities.tapPreference(this, getActivity(), getActivity().findPreference(preferenceKey));
        return TestUtilities.verifyWithTimeoutThatStartActivityAttemptedExactlyOnce();
    }
}
