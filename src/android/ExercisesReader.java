package com.cordova.plugins.shealth;

import android.util.Log;

import com.samsung.android.sdk.healthdata.HealthConstants;
import com.samsung.android.sdk.healthdata.HealthData;
import com.samsung.android.sdk.healthdata.HealthDataResolver;
import com.samsung.android.sdk.healthdata.HealthDataResolver.Filter;
import com.samsung.android.sdk.healthdata.HealthDataResolver.ReadRequest;
import com.samsung.android.sdk.healthdata.HealthDataResolver.SortOrder;
import com.samsung.android.sdk.healthdata.HealthDataStore;

import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;


public class ExercisesReader {

    private final HealthDataResolver mResolver;

    public ExercisesReader(HealthDataStore store) {
        mResolver = new HealthDataResolver(store, null);
    }

    public void get(long startTime, final CallbackContext callbackContext) {
        Filter filter = Filter.greaterThanEquals(HealthConstants.Exercise.END_TIME, startTime);

        ReadRequest request = new ReadRequest.Builder()
                .setDataType(HealthConstants.Exercise.HEALTH_DATA_TYPE)
                .setProperties(new String[]{
                        HealthConstants.Exercise.START_TIME,
                        HealthConstants.Exercise.END_TIME,
                        HealthConstants.Exercise.DISTANCE,
                        HealthConstants.Exercise.CALORIE,
                        HealthConstants.Exercise.EXERCISE_TYPE,
                        HealthConstants.Common.PACKAGE_NAME
                })
                .setSort(HealthConstants.Exercise.END_TIME, SortOrder.DESC)
                .setFilter(filter)
                .build();

        JSONArray response = new JSONArray();

        try {
            mResolver.read(request).setResultListener((HealthDataResolver.ReadResult result) -> {
                try {
                    Iterator<HealthData> iterator = result.iterator();
                    while (iterator.hasNext()) {
                        HealthData data = iterator.next();

                        JSONObject exercise = new JSONObject();

                        exercise.put("startDate", data.getLong(HealthConstants.Exercise.START_TIME));
                        exercise.put("endDate", data.getLong(HealthConstants.Exercise.END_TIME));
                        exercise.put("distance", Math.round(data.getDouble(HealthConstants.Exercise.DISTANCE)));
                        exercise.put("calories", Math.round(data.getDouble(HealthConstants.Exercise.CALORIE)));
                        exercise.put("type", data.getLong(HealthConstants.Exercise.EXERCISE_TYPE));
                        exercise.put("source", data.getString(HealthConstants.Common.PACKAGE_NAME));

                        response.put(exercise);
                    }
                } catch (JSONException e) {
                    Log.e("message", e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    result.close();
                }

                if(callbackContext != null) {
                    callbackContext.success(response);
                }

            });
        } catch (Exception e) {
            Log.e("ExercisesReader", "Getting exercises fails.", e);
            callbackContext.success(response);
        }
    }
}
