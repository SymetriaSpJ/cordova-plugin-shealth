package com.cordova.plugins.shealth;

import android.util.Log;

import org.apache.cordova.CallbackContext;

import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataResolver.Filter;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadRequest;
import com.samsung.android.sdk.healthdata.HealthDataResolver.SortOrder;
import com.samsung.android.sdk.healthdata.HealthDataStore;
import com.samsung.android.sdk.healthdata.HealthConstants.StepCount;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;


public class StepCountReader {

    public static final String STEP_SUMMARY_DATA_TYPE_NAME = "com.samsung.shealth.step_daily_trend";

    private static final String PROPERTY_TIME = "day_time";

    private final HealthDataResolver mResolver;

    public StepCountReader(HealthDataStore store) {
        mResolver = new HealthDataResolver(store, null);
    }

    public void get(long startTime, final CallbackContext callbackContext) {

        Filter filter = Filter.and(Filter.greaterThanEquals(PROPERTY_TIME, startTime),
                Filter.eq("source_type", -2));

        ReadRequest request = new ReadRequest.Builder()
                .setDataType(STEP_SUMMARY_DATA_TYPE_NAME)
                .setProperties(new String[]{PROPERTY_TIME, StepCount.UPDATE_TIME, StepCount.COUNT, StepCount.CALORIE, StepCount.DISTANCE})
                .setSort(PROPERTY_TIME, SortOrder.DESC)
                .setFilter(filter)
                .build();



        try {
            mResolver.read(request).setResultListener((HealthDataResolver.ReadResult result) -> {
                JSONArray stepResponse = new JSONArray();

                try {
                    Iterator<HealthData> iterator = result.iterator();
                    while (iterator.hasNext()) {
                        HealthData data = iterator.next();

                        JSONObject daySteps = new JSONObject();
                        daySteps.put("date", data.getLong(PROPERTY_TIME));
                        daySteps.put("stepCount", data.getInt(StepCount.COUNT));
                        daySteps.put("calories", Math.round(data.getFloat(StepCount.CALORIE)));
                        daySteps.put("distance", Math.round(data.getFloat(StepCount.DISTANCE)));
                        daySteps.put("updateTime", data.getLong(StepCount.UPDATE_TIME));

                        stepResponse.put(daySteps);
                    }
                } catch (JSONException e) {
                    Log.e("message", e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    result.close();
                }

                if(callbackContext != null) {
                    callbackContext.success(stepResponse);
                }

            });
        } catch (Exception e) {
            JSONArray stepResponse = new JSONArray();
            Log.e("StepCounterReader", "Getting daily step trend fails.", e);
            callbackContext.success(stepResponse);
        }
    }
}
