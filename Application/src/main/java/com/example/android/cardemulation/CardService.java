/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.cardemulation;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import com.example.android.common.logger.Log;

import java.util.Arrays;

import static com.example.android.cardemulation.Utils.BuildSelectAPDU;
import static com.example.android.cardemulation.Utils.ByteArrayToHexString;
import static com.example.android.cardemulation.Utils.ConcatArrays;
import static com.example.android.cardemulation.Utils.HexStringToByteArray;

/**
 * This is a sample APDU Service which demonstrates how to interface with the card emulation support
 * added in Android 4.4, KitKat.
 *
 * <p>This sample replies to any requests sent with the string "Hello World". In real-world
 * situations, you would need to modify this code to implement your desired communication
 * protocol.
 *
 * <p>This sample will be invoked for any terminals selecting AIDs of 0xF11111111, 0xF22222222, or
 * 0xF33333333. See src/main/res/xml/aid_list.xml for more details.
 *
 * <p class="note">Note: This is a low-level interface. Unlike the NdefMessage many developers
 * are familiar with for implementing Android Beam in apps, card emulation only provides a
 * byte-array based communication channel. It is left to developers to implement higher level
 * protocol support as needed.
 */


//To emulate an NFC card using host-based card emulation, you need to create a Service component that handles the NFC transactions.
//HCEService doesn't require the application to be active to handle the response. That means that our UI may or may not be shown at the time
public class CardService extends HostApduService {

    private static final String TAG = "Host Card Service";

    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";

    // AID for our loyalty card service.
    // Application ID (AID)-it defines a way to select applications
    private final String AID_SAMPLE_LOYALTY_CARD = getString(R.string.loyalty_card_aid);

    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = HexStringToByteArray("9000");

    // "UNKNOWN" status word sent in response to invalid APDU command (0x0000)
    private static final byte[] UNKNOWN_CMD_SW = HexStringToByteArray("0000");

    String STATUS_SUCCESS = "9000";
    String STATUS_FAILED = "6F00";
    String STATUS_UNKNOWN = "0000";
    String DEFAULT_CLA = "00";
    String CLA_NOT_SUPPORTED = "6E00";
    String SELECT_INS = "A4";
    String INS_NOT_SUPPORTED = "6D00";
    int MIN_APDU_LENGTH = 12;


    /**
     * Called if the connection to the NFC card is lost, in order to let the application know the
     * cause for the disconnection (either a lost link, or another AID being selected by the
     * reader).
     *
     * @param reason Either DEACTIVATION_LINK_LOSS or DEACTIVATION_DESELECTED
     */
    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "Deactivated: " + reason);
    }

    /**
     * called whenever a NFC reader sends an Application Protocol Data Unit (APDU) to your service.
     * APDUs are  the application-level packets being exchanged between the NFC reader and your HCE service.(APDU commands are byte arrays)
     * That application-level protocol is half-duplex: the NFC reader will send you a command APDU, and it will wait for you to send a response APDU in return.
     *
     * This method will be called when a command APDU has been received from a remote device.
     * A response APDU can be provided directly by returning a byte-array in this method.
     * In general response APDUs must be sent as quickly as possible,
     * given the fact that the user is likely holding his device over an NFC reader when this method is called.
     *
     * <p class="note">If there are multiple services that have registered for the same AIDs in their meta-data entry,
     * you will only get called if the user has explicitly selected your service, either as a default or just for the next tap.
     *
     * <p class="note">This method is running on the main thread of your application. If you
     * cannot return a response APDU immediately, return null and use the {@link
     * #sendResponseApdu(byte[])} method later.
     *
     * @param APDU_command The APDU that received from the reader device
     * @param extras A bundle containing extra data. May be null.
     * @return a byte-array containing the response APDU, or null if no response APDU can be sent
     * at this point.
     */
    // BEGIN_INCLUDE(processCommandApdu)
    @Override
    public byte[] processCommandApdu(byte[] APDU_command, Bundle extras) {
        //The `processCommandApdu` method will be called every time a card reader sends an APDU command that is filtered by our manifest filter.
        Log.i(TAG, "Received APDU: " + ByteArrayToHexString(APDU_command));

        /*
        -Android uses the Application ID(AID) to determine which HCE service the reader wants to talk to.
        -Typically, the first APDU an NFC reader sends to your device is a "SELECT AID" APDU (this APDU contains the AID that the reader wants to talk to)
        -Android extracts that AID from the APDU, resolves it to an HCE service, then forwards that APDU to the resolved service.
         */


        // If the APDU matches the SELECT AID command for this service,
        // send the loyalty card account number, followed by a SELECT_OK status trailer (0x9000).
        // SELECT_APDU_HEADER + CARD_AID
        if (Arrays.equals(BuildSelectAPDU(SELECT_APDU_HEADER, AID_SAMPLE_LOYALTY_CARD), APDU_command))
        {
            String account = AccountStorage.GetAccount(this);

            //You can send a response APDU by returning the bytes of the response APDU
            // Format: [Data Field | SW_1 SW_2]
            return ConcatArrays(account.getBytes(), HexStringToByteArray(STATUS_SUCCESS));

        } else {

            String hexAPDU_command = ByteArrayToHexString(APDU_command);

            if (hexAPDU_command.length() < MIN_APDU_LENGTH) {
                return HexStringToByteArray(STATUS_FAILED);
            } else if (!hexAPDU_command.substring(0, 2).equals(DEFAULT_CLA)) {
                return HexStringToByteArray(CLA_NOT_SUPPORTED);
            } else if (!hexAPDU_command.substring(2, 4).equals(SELECT_INS)) {
                return HexStringToByteArray(INS_NOT_SUPPORTED);
            } else {
                return HexStringToByteArray(STATUS_FAILED);
            }

        }
    }
    // END_INCLUDE(processCommandApdu)


}
