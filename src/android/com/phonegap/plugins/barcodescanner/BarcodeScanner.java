/*
 * Copyright 2012-2018 Marcelo Buregio
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
package com.marceloburegio.zxingplugin;

// Cordova-required packages
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.util.Log;

// ZXing packages
import com.google.zxing.client.android.Intents;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.Arrays;

public class ZXingPlugin extends CordovaPlugin {
    private static final String CANCELLED = "cancelled";
    private static final String FORMAT = "format";
    private static final String TEXT = "text";

    private CallbackContext scanCallbackContext;

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
        // Verify that the user sent a 'scan' action
        if (!action.equals("scan")) {
            callbackContext.error("\"" + action + "\" is not a recognized action.");
            return false;
        }

        // Creating new interface (Integrator)
        IntentIntegrator integrator = new IntentIntegrator((CordovaActivity) cordova.getActivity());
        cordova.setActivityResultCallback(this);
        try {
            JSONObject params = args.getJSONObject(0);
            if (params.has("prompt") && params.getString("prompt").length() > 0) integrator.setPrompt(params.getString("prompt")); // Prompt Message
            if (params.has("preferFrontCamera")) integrator.setCameraId(params.getBoolean("preferFrontCamera") ? 1 : 0); // Camera Id
            if (params.has("disableSuccessBeep")) integrator.setBeepEnabled(!params.getBoolean("disableSuccessBeep")); // Beep Enabled
            if (params.has("resultDisplayDuration")) integrator.setTimeout(params.getInt("resultDisplayDuration")); // Timeout
            // Scan Type
            if (params.has("scan_type")) {
                String scanType = params.getString("scan_type");
                if (scanType.equals("inverted")) integrator.addExtra("SCAN_TYPE", 1);
                else if (scanType.equals("mixed")) integrator.addExtra("SCAN_TYPE", 2);
                else integrator.addExtra("SCAN_TYPE", 0);
            }

            // Barcode Formats
            if (params.has("formats")) {
                ArrayList<String> formats = new ArrayList<String>();
                String[] barcodeFormats = params.optString("formats", "").split(",");
                if (barcodeFormats.length > 0) {
                    integrator.setDesiredBarcodeFormats(Arrays.asList(barcodeFormats));
                }
            }

            // Extras
            JSONObject extras = params.has("extras") ? params.getJSONObject("extras") : null;
            if (extras != null) {
                JSONArray extraNames = extras.names();
                if (extraNames != null) {
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        Object value = extras.get(key);
                        integrator.addExtra(key, value);
                    }
                }
            }
        } catch (JSONException e) {
            callbackContext.error("Error encountered: " + e.getMessage());
            return false;
        }

        // Init scanner using a camera
        integrator.initiateScan();
        scanCallbackContext = callbackContext;
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if(result != null) {
            JSONObject obj = new JSONObject();
            if(result.getContents() == null) {
                try {
                    obj.put(TEXT, "");
                    obj.put(FORMAT, "");
                    obj.put(CANCELLED, true);
                } catch (JSONException e) {
                    Log.d("Code scanner", "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                scanCallbackContext.success(obj);
            } else {
                try {
                    obj.put(TEXT, result.getContents());
                    obj.put(FORMAT, result.getFormatName());
                    obj.put(CANCELLED, false);
                    scanCallbackContext.success(obj);
                } catch (JSONException e) {
                    Log.d("Code scanner", "This should never happen");
                }
            }
        } else {
            // This is important, otherwise the result will not be passed to the fragment
            super.onActivityResult(requestCode, resultCode, intent);
        }
    }
}
