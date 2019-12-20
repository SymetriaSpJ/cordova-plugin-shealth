package com.cordova.plugins.shealth;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants.StepCount;
import com.samsung.android.sdk.healthdata.HealthDataService;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionResult;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.content.DialogInterface;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import android.content.Context;
import android.os.Handler;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ShealthPlugin extends CordovaPlugin {

    public static final String TAG = "ShealthPlugin";
    private HealthDataStore mStore;
    private StepCountReader mReader;
    private long mCurrentStartTime;
    private Activity actContext;
    private Context appContext;
    private CallbackContext connectCallbackContext;
    private long reqAuth = 0;
    private HashMap<String, TimeUnit> TimeUnitLookup;
    private HashMap<TimeUnit, String> TimeUnitRLookup;

    private void fillTimeUnit(TimeUnit t) {
        TimeUnitLookup.put(t.name(), t);
        TimeUnitRLookup.put(t, t.name());
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        actContext = cordova.getActivity();
        appContext = actContext.getApplicationContext();

        // Get the start time of today in local
        mCurrentStartTime = StepCountReader.TODAY_START_UTC_TIME;
        HealthDataService healthDataService = new HealthDataService();
        try {
            healthDataService.initialize(actContext);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Create a HealthDataStore instance and set its listener
        mStore = new HealthDataStore(actContext, mConnectionListener);

        // // Request the connection to the health data store
        mStore.connectService();
        mReader = new StepCountReader(mStore, actContext);

        cordova.setActivityResultCallback(this);
    }

    @Override
    public void onDestroy() {
        mStore.disconnectService();
        super.onDestroy();
    }

    private final HealthDataStore.ConnectionListener mConnectionListener = new HealthDataStore.ConnectionListener() {
        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            if (isPermissionAcquired()) {
                Log.d(TAG, "Permission is acquired already");
                if (connectCallbackContext != null) {
                  connectCallbackContext.success();
                }
            } else {
                if (reqAuth == 1) {
                    requestPermission();
                } else {
                    if (connectCallbackContext != null) {
                        connectCallbackContext.error("no permission, no auth");
                    }
                }
            }
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "onConnectionFailed");
            showConnectionFailureDialog(error);
            if (connectCallbackContext != null) {
              connectCallbackContext.error("Failed to connect: " + error.getErrorCode());
            }
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "onDisconnected");
            mStore.connectService();

        }
    };
    private void updateBinningData(List<StepCountReader.StepBinningData> stepBinningDataList) {
        // the following code will be replaced with chart drawing code
        Log.d(TAG, "updateBinningChartView");
        // mBinningListAdapter.changeDataSet(stepBinningDataList);
        // for (StepCountReader.StepBinningData data : stepBinningDataList) {
        //     Log.d(TAG, "TIME : " + data.time + "  COUNT : " + data.count);
        // }
    }

    private final HealthResultHolder.ResultListener<PermissionResult> mPermissionListener =
            new HealthResultHolder.ResultListener<PermissionResult>() {

        @Override
        public void onResult(PermissionResult result) {
            Map<PermissionKey, Boolean> resultMap = result.getResultMap();
            // Show a permission alarm and clear step count if permissions are not acquired
            if (resultMap.values().contains(Boolean.FALSE)) {
                //showPermissionAlarmDialog();
                connectCallbackContext.error("Permission was not given");
            }
            else {
                connectCallbackContext.success();
            }
        }
    };

    private void showPermissionAlarmDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(cordova.getActivity());
        alert.setTitle("Notice")
                .setMessage("Permission Required")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showConnectionFailureDialog(final HealthConnectionErrorResult error) {

        AlertDialog.Builder alert = new AlertDialog.Builder(cordova.getActivity());

        if (error.hasResolution()) {
            switch (error.getErrorCode()) {
                case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                    alert.setMessage("Samsung Health: Platform is not installed");
                    break;
                case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                    alert.setMessage("Samsung Health: Requires Upgrade");
                    break;
                case HealthConnectionErrorResult.PLATFORM_DISABLED:
                    alert.setMessage("Samsung Health: Platform is disabled");
                    break;
                case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                    alert.setMessage("Samsung Health: User agreement needed");
                    break;
                default:
                    alert.setMessage("Connection available");
                    break;
            }
        } else {
            alert.setMessage("Connection not available");
        }

        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (error.hasResolution()) {
                    error.resolve(actContext);
                }
            }
        });

        if (error.hasResolution()) {
            alert.setNegativeButton("Cancel", null);
        }

        alert.show();
    }

    private boolean isPermissionAcquired() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            // Check whether the permissions that this application needs are acquired
            Map<PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(generatePermissionKeySet());
            return !resultMap.values().contains(Boolean.FALSE);
        } catch (Exception e) {
            Log.e(TAG, "Permission request fails.", e);
        }
        return false;
    }

    private void requestPermission() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            // Show user permission UI for allowing user to change options
            pmsManager.requestPermissions(generatePermissionKeySet(), actContext)
                    .setResultListener(mPermissionListener);
        } catch (Exception e) {
            Log.e(TAG, "Permission setting fails.", e);
        }
    }

    private Set<PermissionKey> generatePermissionKeySet() {
        Set<PermissionKey> pmsKeySet = new HashSet<PermissionKey>();
        pmsKeySet.add(new PermissionKey(StepCount.HEALTH_DATA_TYPE, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(StepCountReader.STEP_SUMMARY_DATA_TYPE_NAME, PermissionType.READ));
        return pmsKeySet;
    }

    /**
     * The "execute" method that Cordova calls whenever the plugin is used from the JavaScript
     * @param action          The action to execute.
     * @param args            The exec() arguments.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return
     * @throws JSONException
     */
    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {

        // Select the getData: get Datasets+Datapoints from GoogleFit according to the query parameters
        if ("getData".equals(action)) {

            long st = args.getJSONObject(0).getLong("startTime");

            mReader.requestDailyStepCount(st, callbackContext);

            return true;
        } else if ("connect".equals(action)) {
            connectCallbackContext = callbackContext;
            // Request the connection to the health data store
            if (isPermissionAcquired()) {
                callbackContext.success();
            } else {
                requestPermission();
            }

            return true;
        }

        return false;  // Returning false will result in a "MethodNotFound" error.
    }
}
