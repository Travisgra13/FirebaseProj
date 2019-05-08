package com.example.travis.berryloc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private DatabaseReference myDatabase;
    private ServerSocket serverSocket;
    private com.example.travis.berryloc.Location currLocation;
    private TextView locationResults;
    private EditText widgetName;
    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myDatabase = FirebaseDatabase.getInstance().getReference();
        locationResults = findViewById(R.id.currLocResults);
        widgetName = findViewById(R.id.widgetId);
        widgetName.setText("Travis' Phone");
        widgetName.addTextChangedListener(textWatcher);
        id = widgetName.getText().toString();
        ServerTask serverTask = new ServerTask();
        serverTask.execute();
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            id = s.toString();
        }
    };

    public class Server extends Thread {

        public Server() {
            try {
                serverSocket = new ServerSocket(12345);

                Socket conn = serverSocket.accept();
                PrintWriter out = new PrintWriter(conn.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.equals("good bye ")) {
                        out.write("Ok, Closing Connection");
                        break;
                    }
                    else {
                        out.write("Ok, Updating Firebase");
                        doAll();
                    }
                    System.out.println(out.toString());
                    out.flush();
                }
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                locationResults.setText("Something went wrong, Server Not Running");
            }
        }
    }

    private class ServerTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            new Server().start();
            return null;
        }
    }
    @Override
    protected void onStop() {
        super.onStop();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            locationResults.setText("Server Stopped");
        }
    }

    public boolean makeLocationNewInstance() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // TODO: Consider calling
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]
                        {Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return false;
            }
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        1);
                return false;
            }
            try {
                if (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) == null) {
                    throw new Exception();
                }
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                double longitude = location.getLongitude();
                double latitude = location.getLatitude();
                this.currLocation = new com.example.travis.berryloc.Location(longitude, latitude);
            }catch(Exception e) {
                e.printStackTrace();
                locationResults.setText("No GPS Fix, Please Try Again");
            }
            return true;
        }

    public void AddToFirebase() {
        Date cal = Calendar.getInstance().getTime();
        FileCopy fileCopy = FileCopy.getInstance();
        fileCopy.makeCopy();
        myDatabase.child("Locations").child(this.id).child("Longitude").setValue(currLocation.getLongitude());
        myDatabase.child("Locations").child(this.id).child("Latitude").setValue(currLocation.getLatitude());
        myDatabase.child("Locations").child(this.id).child("Type").setValue("Button");
        myDatabase.child("Locations").child(this.id).child("Code").setValue(fileCopy.getCode());
        myDatabase.push();
    }
    public void doAll() {
        makeLocationNewInstance();
        String results = "Longitude: " + currLocation.getLongitude() +
                "\nLatitude: " + currLocation.getLatitude() + "\nSession Token: " + MainActivity.this.id;
        locationResults.setText(results);
        AddToFirebase();

    }

}
