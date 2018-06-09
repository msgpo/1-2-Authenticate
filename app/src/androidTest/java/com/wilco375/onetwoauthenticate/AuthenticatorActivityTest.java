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

package com.wilco375.onetwoauthenticate;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;
import android.test.ViewAsserts;
import android.text.ClipboardManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.wilco375.onetwoauthenticate.AccountDb.OtpType;
import com.wilco375.onetwoauthenticate.testability.DependencyInjector;
import com.wilco375.onetwoauthenticate.testability.StartActivityListener;
import com.wilco375.onetwoauthenticate.R;

import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Unit test for authenticator activity (part/shard 1).
 *
 * @author sarvar@google.com (Sarvar Patel)
 */
public class AuthenticatorActivityTest extends
        ActivityInstrumentationTestCase2<AuthenticatorActivity> {

    private AccountDb mAccountDb;

    public AuthenticatorActivityTest() {
        super(TestUtilities.APP_PACKAGE_NAME, AuthenticatorActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        DependencyInjector.resetForIntegrationTesting(getInstrumentation().getTargetContext());
        mAccountDb = DependencyInjector.getAccountDb();

        initMocks(this);
        TestUtilities.withLaunchPreventingStartActivityListenerInDependencyResolver();
    }

    @Override
    protected void tearDown() throws Exception {
        // Stop the activity to avoid it using the DependencyInjector after it's been closed.
        TestUtilities.invokeFinishActivityOnUiThread(getActivity());

        DependencyInjector.close();

        super.tearDown();
    }

    public void testGetTitle() {
        assertEquals(getActivity().getString(R.string.app_name), getActivity().getTitle());
    }

    //////////////////////// Main screen UI Tests ///////////////////////////////

    public void testNoAccountUi() {
        getActivity();
        ListView userList = getActivity().findViewById(R.id.user_list);
        TextView enterPinPrompt = getActivity().findViewById(R.id.enter_pin_prompt);
        Button addAccountButton = getActivity().findViewById(R.id.add_account_button);
        View contentWhenNoAccounts = getActivity().findViewById(R.id.content_no_accounts);

        // check existence of fields
        assertNotNull(userList);
        assertNotNull(enterPinPrompt);
        assertNotNull(addAccountButton);
        assertNotNull(contentWhenNoAccounts);

        // check visibility
        View origin = getActivity().getWindow().getDecorView();
        ViewAsserts.assertOnScreen(origin, enterPinPrompt);
        ViewAsserts.assertOnScreen(origin, addAccountButton);
        ViewAsserts.assertOnScreen(origin, contentWhenNoAccounts);
        assertFalse(userList.isShown());
    }

    public void testGetOtpWithOneTotpAccount() {
        mAccountDb.update(
                "johndoeTotp@gmail.com", "7777777777777777", "johndoeTotp@gmail.com", OtpType.TOTP, null);
        ListView userList = getActivity().findViewById(R.id.user_list);
        assertEquals(1, userList.getChildCount());
        View listEntry = userList.getChildAt(0);
        String user = ((TextView) listEntry.findViewById(R.id.current_user)).getText().toString();
        String pin = ((TextView) listEntry.findViewById(R.id.pin_value)).getText().toString();
        assertEquals("johndoeTotp@gmail.com", user);
        assertTrue(Integer.parseInt(pin) >= 0 && Integer.parseInt(pin) <= 999999);
        assertEquals(6, pin.length());
        assertFalse(listEntry.findViewById(R.id.next_otp).isShown());
        assertTrue(listEntry.findViewById(R.id.countdown_icon).isShown());
    }

    public void testGetOtpWithOneHotpAccount() {
        mAccountDb.update(
                "johndoeHotp@gmail.com", "7777777777777777", "johndoeHotp@gmail.com", OtpType.HOTP, null);
        ListView userList = getActivity().findViewById(R.id.user_list);
        assertEquals(1, userList.getChildCount());
        View listEntry = userList.getChildAt(0);
        String user = ((TextView) listEntry.findViewById(R.id.current_user)).getText().toString();
        String pin = ((TextView) listEntry.findViewById(R.id.pin_value)).getText().toString();
        assertEquals("johndoeHotp@gmail.com", user);
        assertEquals(getActivity().getString(R.string.empty_pin), pin);  // starts empty
        assertFalse(listEntry.findViewById(R.id.countdown_icon).isShown());
        View buttonView = listEntry.findViewById(R.id.next_otp);
        assertTrue(buttonView.isShown());
        // get next Otp value by clicking icon
        TestUtilities.clickView(getInstrumentation(), buttonView);
        pin = ((TextView) listEntry.findViewById(R.id.pin_value)).getText().toString();
        assertEquals("683298", pin);
    }

    public void testGetOtpWithMultipleAccounts() {
        mAccountDb.update(
                "johndoeHotp@gmail.com", "7777777777777777", "johndoeHotp@gmail.com", OtpType.HOTP, null);
        mAccountDb.update(
                "johndoeTotp1@gmail.com", "2222222222222222", "johndoeTotp1@gmail.com", OtpType.TOTP, null);
        mAccountDb.update(
                "johndoeTotp2@gmail.com", "3333333333333333", "johndoeTotp2@gmail.com", OtpType.TOTP, null);
        ListView userList = getActivity().findViewById(R.id.user_list);
        assertEquals(3, userList.getChildCount());

        // check hotp account
        View listEntry0 = userList.getChildAt(0);
        String user = ((TextView) listEntry0.findViewById(R.id.current_user)).getText().toString();
        String pin = ((TextView) listEntry0.findViewById(R.id.pin_value)).getText().toString();
        assertEquals("johndoeHotp@gmail.com", user);
        assertEquals(getActivity().getString(R.string.empty_pin), pin);  // starts empty
        assertFalse(listEntry0.findViewById(R.id.countdown_icon).isShown());
        View buttonView = listEntry0.findViewById(R.id.next_otp);
        assertTrue(buttonView.isShown());
        // get next Otp value by clicking icon
        TestUtilities.clickView(getInstrumentation(), buttonView);
        listEntry0 = userList.getChildAt(0); // get refreshed value after clicking nextOtp button.
        pin = ((TextView) listEntry0.findViewById(R.id.pin_value)).getText().toString();
        assertEquals("683298", pin);

        // check first totp account
        View listEntry1 = userList.getChildAt(1);
        user = ((TextView) listEntry1.findViewById(R.id.current_user)).getText().toString();
        pin = ((TextView) listEntry1.findViewById(R.id.pin_value)).getText().toString();
        assertEquals("johndoeTotp1@gmail.com", user);
        assertTrue(Integer.parseInt(pin) > 0 && Integer.parseInt(pin) <= 999999);
        assertFalse(listEntry1.findViewById(R.id.next_otp).isShown());
        assertTrue(listEntry1.findViewById(R.id.countdown_icon).isShown());

        View listEntry2 = userList.getChildAt(2);
        // check second totp account
        user = ((TextView) listEntry2.findViewById(R.id.current_user)).getText().toString();
        pin = ((TextView) listEntry2.findViewById(R.id.pin_value)).getText().toString();
        assertEquals("johndoeTotp2@gmail.com", user);
        assertTrue(Integer.parseInt(pin) > 0 && Integer.parseInt(pin) <= 999999);
        assertFalse(listEntry1.findViewById(R.id.next_otp).isShown());
        assertTrue(listEntry1.findViewById(R.id.countdown_icon).isShown());
    }

    //////////////////////////   Context Menu Tests  ////////////////////////////

    public void testContextMenuCheckCode() {
        mAccountDb.update(
                "johndoeHotp@gmail.com", "7777777777777777", "johndoeHotp@gmail.com", OtpType.HOTP, null);
        ListView userList = getActivity().findViewById(R.id.user_list);
        View listEntry0 = userList.getChildAt(0);
        TestUtilities.openContextMenuAndInvokeItem(
                getInstrumentation(),
                getActivity(),
                listEntry0,
                AuthenticatorActivity.CHECK_KEY_VALUE_ID);

        Intent launchIntent = TestUtilities.verifyWithTimeoutThatStartActivityAttemptedExactlyOnce();
        assertEquals(
                new ComponentName(getInstrumentation().getTargetContext(), CheckCodeActivity.class),
                launchIntent.getComponent());
        assertEquals("johndoeHotp@gmail.com", launchIntent.getStringExtra("user"));
    }

    public void testContextMenuRemove() throws Exception {
        mAccountDb.update(
                "johndoeHotp@gmail.com", "7777777777777777", "johndoeHotp@gmail.com", OtpType.HOTP, null);
        ListView userList = getActivity().findViewById(R.id.user_list);
        View listEntry0 = userList.getChildAt(0);
        TestUtilities.openContextMenuAndInvokeItem(
                getInstrumentation(),
                getActivity(),
                listEntry0,
                AuthenticatorActivity.REMOVE_ID);
        // Wait for webview loading
        Thread.sleep(1000);
        // Select Remove on confirmation dialog to remove account.
        sendKeys(KeyEvent.KEYCODE_TAB);
        TestUtilities.tapDialogPositiveButton(this);
        // check main  screen gets focus back;
        TestUtilities.waitForWindowFocus(listEntry0);
        // check that account is deleted in database.
        assertEquals(0, mAccountDb.getNames(new ArrayList<>()));
    }

    public void testContextMenuRename() throws Exception {
        mAccountDb.update(
                "johndoeHotp@gmail.com", "7777777777777777", "johndoeHotp@gmail.com", OtpType.HOTP, null);
        ListView userList = getActivity().findViewById(R.id.user_list);
        View listEntry0 = userList.getChildAt(0);
        TestUtilities.openContextMenuAndInvokeItem(
                getInstrumentation(),
                getActivity(),
                listEntry0,
                AuthenticatorActivity.RENAME_ID);
        // Type new name and select save on dialog.
        sendKeys("21*DPAD_RIGHT");  // move right to end;
        sendKeys("21*DEL"); // delete the entire name
        sendKeys("N E W N A M E AT G M A I L PERIOD C O M");
        sendKeys("TAB");
        TestUtilities.tapDialogPositiveButton(this); // select save on the dialog
        // check main  screen gets focus back;
        listEntry0 = userList.getChildAt(0);
        TestUtilities.waitForWindowFocus(listEntry0);
        // check update to database.
        List<String> accountNames = new ArrayList<>();
        assertEquals(1, mAccountDb.getNames(accountNames));
        assertEquals("newname@gmail.com", accountNames.get(0));
    }

    public void testContextMenuCopyToClipboard() {
        // use HOTP to avoid any timing issues when "current" pin is compared with clip board text.
        mAccountDb.update(
                "johndoeHotp@gmail.com", "7777777777777777", "johndoeHotp@gmail.com", OtpType.HOTP, null);
        ListView userList = getActivity().findViewById(R.id.user_list);
        // find and click next otp button.
        View buttonView = getActivity().findViewById(R.id.next_otp);
        TestUtilities.clickView(getInstrumentation(), buttonView);
        // get the pin being displayed
        View listEntry0 = userList.getChildAt(0);
        String pin = ((TextView) listEntry0.findViewById(R.id.pin_value)).getText().toString();
        TestUtilities.openContextMenuAndInvokeItem(
                getInstrumentation(),
                getActivity(),
                listEntry0,
                AuthenticatorActivity.COPY_TO_CLIPBOARD_ID);
        // check clip board value.
        Context context = getInstrumentation().getTargetContext();
        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        assertEquals(pin, clipboard.getText());
    }

    ///////////////////////////   Options Menu Tests  /////////////////////////////

    private void checkOptionsMenuItemWithComponent(int itemId, Class<?> cls) {
        TestUtilities.openOptionsMenuAndInvokeItem(getInstrumentation(), getActivity(), itemId);

        Intent launchIntent = TestUtilities.verifyWithTimeoutThatStartActivityAttemptedExactlyOnce();
        assertEquals(new ComponentName(getInstrumentation().getTargetContext(), cls),
                launchIntent.getComponent());
    }

    public void testIntentActionScanBarcode_withScannerInstalled() {
        setActivityIntent(new Intent(AuthenticatorActivity.ACTION_SCAN_BARCODE));
        getActivity();

        Intent launchIntent = TestUtilities.verifyWithTimeoutThatStartActivityAttemptedExactlyOnce();
        assertEquals("com.google.zxing.client.android.SCAN", launchIntent.getAction());
        assertEquals("QR_CODE_MODE", launchIntent.getStringExtra("SCAN_MODE"));
        assertEquals(false, launchIntent.getExtras().get("SAVE_HISTORY"));
    }

    public void testIntentActionScanBarcode_withScannerNotInstalled() {
        // When no barcode scanner is installed no matching activities are found as emulated below.
        StartActivityListener mockStartActivityListener = mock(StartActivityListener.class);
        doThrow(new ActivityNotFoundException())
                .when(mockStartActivityListener).onStartActivityInvoked(
                Mockito.anyObject(), Mockito.anyObject());
        DependencyInjector.setStartActivityListener(mockStartActivityListener);

        setActivityIntent(new Intent(AuthenticatorActivity.ACTION_SCAN_BARCODE));
        getActivity();

        TestUtilities.assertDialogWasDisplayed(
                getActivity(), Utilities.DOWNLOAD_DIALOG);
    }
}
