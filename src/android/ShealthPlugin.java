package com.cordova.plugins.shealth;

import com.samsung.android.sdk.healthdata.HealthConnectionErrorResult;
import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthPermissionManager;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionKey;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionResult;
import com.samsung.android.sdk.healthdata.HealthPermissionManager.PermissionType;
import com.samsung.android.sdk.healthdata.HealthResultHolder;

import android.app.Activity;
import android.util.Log;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ShealthPlugin extends CordovaPlugin {

    public static final String TAG = "ShealthPlugin";
    private HealthDataStore mStore;
    private StepCountReader mStepCountReader;
    private ExercisesReader mExercisesReader;
    private Activity activityContext;
    private CallbackContext connectCallbackContext;
    private CallbackContext permissionCallbackContext;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activityContext = cordova.getActivity();

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

            connectCallbackContext.success();
        }

        @Override
        public void onConnectionFailed(HealthConnectionErrorResult error) {
            Log.d(TAG, "onConnectionFailed");

            if (connectCallbackContext != null) {
                String message = "";

                if (error.hasResolution()) {
                    switch (error.getErrorCode()) {
                        case HealthConnectionErrorResult.PLATFORM_NOT_INSTALLED:
                            message = "Samsung Health: Platform is not installed";
                            break;
                        case HealthConnectionErrorResult.OLD_VERSION_PLATFORM:
                            message = "Samsung Health: Requires Upgrade";
                            break;
                        case HealthConnectionErrorResult.PLATFORM_DISABLED:
                            message = "Samsung Health: Platform is disabled";
                            break;
                        case HealthConnectionErrorResult.USER_AGREEMENT_NEEDED:
                            message = "Samsung Health: User agreement needed";
                            break;
                        default:
                            message = "Connection available";
                            break;
                    }
                } else {
                    message = "Connection not available";
                }

                JSONObject errorObject = new JSONObject();
                try {
                    errorObject.put("message", "Failed to connect: " + message);
                    errorObject.put("errorCode", error.getErrorCode());

                    connectCallbackContext.error(errorObject);
                } catch (JSONException e) {
                    connectCallbackContext.error("Failed to connect: " + message);
                }
            }
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "onDisconnected");

            mStore.connectService();
        }
    };

    private final HealthResultHolder.ResultListener<PermissionResult> mPermissionListener =
            new HealthResultHolder.ResultListener<PermissionResult>() {

        @Override
        public void onResult(PermissionResult result) {
            Map<PermissionKey, Boolean> resultMap = result.getResultMap();

            if (resultMap.values().contains(Boolean.TRUE)) {
                permissionCallbackContext.success(1);
            }
            else {
                permissionCallbackContext.success(0);
            }
        }
    };

    private boolean isPermissionAcquired() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            Map<PermissionKey, Boolean> resultMap = pmsManager.isPermissionAcquired(generatePermissionKeySet());
            return resultMap.values().contains(Boolean.TRUE);
        } catch (Exception e) {
            Log.e(TAG, "Permission request fails.", e);
        }
        return false;
    }

    private void requestPermission() {
        HealthPermissionManager pmsManager = new HealthPermissionManager(mStore);
        try {
            // Show user permission UI for allowing user to change options
            pmsManager.requestPermissions(generatePermissionKeySet(), activityContext)
                    .setResultListener(mPermissionListener);
        } catch (Exception e) {
            Log.e(TAG, "Permission setting fails.", e);
        }
    }

    private Set<PermissionKey> generatePermissionKeySet() {
        Set<PermissionKey> pmsKeySet = new HashSet<>();
        pmsKeySet.add(new PermissionKey(StepCountReader.STEP_SUMMARY_DATA_TYPE_NAME, PermissionType.READ));
        pmsKeySet.add(new PermissionKey(HealthConstants.Exercise.HEALTH_DATA_TYPE, PermissionType.READ));

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
        if ("initialize".equals(action)) {
            handleInitialize(callbackContext);

            return true;
        }

        if ("isConnected".equals(action)) {
            handleIsConnected(callbackContext);

            return true;
        }

        if ("connect".equals(action)) {
            handleConnect(callbackContext);

            return true;
        }

        if ("getDailySteps".equals(action)) {
            handleGetDailySteps(args, callbackContext);

            return true;
        }

        if ("getExercises".equals(action)) {
            handleGetExercises(args, callbackContext);

            return true;
        }

        callbackContext.error("Action not found");

        return false;
    }

    private void handleInitialize(CallbackContext callbackContext) {
        connectCallbackContext = callbackContext;

        mStore = new HealthDataStore(activityContext, mConnectionListener);

        mStore.connectService();

        mStepCountReader = new StepCountReader(mStore);
        mExercisesReader = new ExercisesReader(mStore);
    }

    private void handleIsConnected(CallbackContext callbackContext) {
        if (isPermissionAcquired()) {
            callbackContext.success();
        } else {
            callbackContext.error("not connected");
        }
    }

    private void handleConnect(CallbackContext callbackContext) {
        permissionCallbackContext = callbackContext;

        requestPermission();
    }

    private void handleGetDailySteps(JSONArray args, CallbackContext callbackContext) throws JSONException {
        long startTime = args.getJSONObject(0).getLong("startTime");

        mStepCountReader.get(startTime, callbackContext);
    }

    private void handleGetExercises(JSONArray args, CallbackContext callbackContext) throws JSONException {
        long startTime = args.getJSONObject(0).getLong("startTime");

        mExercisesReader.get(startTime, callbackContext);
    }
}
