package com.example.travis.berryloc;

import android.os.AsyncTask;

public class LocIntervalTask extends AsyncTask<MainActivity, Void, Void> {

    @Override
    protected Void doInBackground(MainActivity... mainActivities) {
                mainActivities[0].doAll();
                return null;
    }
}
