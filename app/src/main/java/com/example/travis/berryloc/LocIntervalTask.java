package com.example.travis.berryloc;

import android.os.AsyncTask;

public class LocIntervalTask extends AsyncTask<MainActivity, Void, Void> {

    @Override
    protected Void doInBackground(MainActivity... mainActivities) {
        final int NUM_OF_SECS = 10;
        while (true){
            try {
                wait(1000 * NUM_OF_SECS);
                mainActivities[0].doAll();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
