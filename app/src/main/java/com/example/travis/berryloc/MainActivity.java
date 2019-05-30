package com.example.travis.berryloc;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.widget.EditText;
import android.widget.TextView;

import com.example.travis.berryloc.Model.BerryBody;
import com.example.travis.berryloc.Model.Event;
import com.example.travis.berryloc.Model.Handshake;
import com.example.travis.berryloc.Model.ServerMessage;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private DatabaseReference myDatabase;
    private ServerSocket serverSocket;
    private com.example.travis.berryloc.Model.Location currLocation;
    private TextView locationResults;
    private EditText widgetName;
    private Handshake handshake;
    private ServerTask serverTask;
    private String id;
    private Server server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myDatabase = FirebaseDatabase.getInstance().getReference();
        myDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    if (child.getKey().equals(MainActivity.this.id)) {
                        final Event event = child.getValue(Event.class);
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //Fixme This will become a problem later
                                event.setName(MainActivity.this.id);
                                server.sendCodeToClient(event);
                            }
                        });
                        thread.start();
                        break;
                    }

                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        locationResults = findViewById(R.id.currLocResults);
        widgetName = findViewById(R.id.widgetId);
        widgetName.setText("Travis' Phone");
        widgetName.addTextChangedListener(textWatcher);
        id = widgetName.getText().toString();
        serverTask = new ServerTask();
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

    public class Server {
        private SocketAddress clientAddress;
        private Socket conn;
        private PrintWriter out;
        public Server() {
            try {
                serverSocket = new ServerSocket(4444);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                conn = serverSocket.accept();
                clientAddress = conn.getRemoteSocketAddress();

                out = new PrintWriter(conn.getOutputStream(), false);
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String inputLine = in.readLine();
                updateRegistry(inputLine);//get initial handshake
                String address = fixIpAddress(clientAddress.toString());
                String local = getLocalIpAdd();

                Socket socket = new Socket(address, 6666);
                out = new PrintWriter(socket.getOutputStream(), true);
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("command", "fixedip-ack");
                jsonObject.addProperty("ip", local);
                out.write(jsonObject.toString());
                out.flush();

                //  out.close();
            } catch (IOException e) {
                e.printStackTrace();
                locationResults.setText("Something went wrong, Server Not Running");
            }        }
            private String fixIpAddress(String clientAddress) {
                StringBuilder sb = new StringBuilder(clientAddress);
                int index = sb.indexOf(":");
                sb.delete(index, index + 6);
                sb.delete(0,1);
            return sb.toString();
            }

        public void sendCodeToClient(Event event) {
            System.out.println("Inside ///////////////////////////////////////////////////");
                Gson gson = new Gson();
                ServerMessage serverMessage = processEventToMessage(event);
               // String jsonServerMessage = gson.toJson(serverMessage);
                JsonObject jsonServerMessage = new JsonObject();
                jsonServerMessage.addProperty("command", serverMessage.getCommand());
                jsonServerMessage.addProperty("name", (String) serverMessage.getPayload().get("name"));
                jsonServerMessage.addProperty("code", (String) serverMessage.getPayload().get("code"));
                out.write(jsonServerMessage.toString());
                out.flush();


        }

        private ServerMessage processEventToMessage(Event event) {
            ServerMessage serverMessage = new ServerMessage();
            serverMessage.readyPayload(event.getName(), event.getCode());
            return serverMessage;
        }

        private String getLocalIpAdd() {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]
                        {Manifest.permission.ACCESS_WIFI_STATE}, 1);
            }
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
            return ip;
        }

        private void updateRegistry(String jsonFromClient) {
            makeLocationNewInstance();
            Gson gson = new Gson();
            handshake = gson.fromJson(jsonFromClient, Handshake.class);
            JsonParser jsonParser = new JsonParser();
            JsonElement json = jsonParser.parse(jsonFromClient);
            JsonObject jsonObject = (JsonObject) ((JsonObject)json).get("berry-body");
            BerryBody berryBody = gson.fromJson(jsonObject, BerryBody.class);
            handshake.setBerryBody(berryBody);
            //handshake = new Handshake();
            widgetName.setText(berryBody.getName());
            MainActivity.this.id = berryBody.getName();
            doAll();
        }
    }

    private class ServerTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            server = new Server();
            server.run();
            return null;
        }
    }



    private class SendCodeTask extends AsyncTask<Event,Void, Void> {
    @Override
        protected Void doInBackground(Event... events) {
        server.sendCodeToClient(events[0]);
        return null;
    }
    }
    @Override
    protected void onStop() {
        super.onStop();
        try {
            server.conn.close();
            server.out.close();
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
                this.currLocation = new com.example.travis.berryloc.Model.Location(longitude, latitude);
            }catch(Exception e) {
                e.printStackTrace();
                locationResults.setText("No GPS Fix, Please Try Again");
            }
            return true;
        }

    public void AddToFirebase() {
        Date cal = Calendar.getInstance().getTime();
        myDatabase.child("Locations").child(this.id).child("Longitude").setValue(currLocation.getLongitude());
        myDatabase.child("Locations").child(this.id).child("Latitude").setValue(currLocation.getLatitude());
        myDatabase.child("Locations").child(this.id).child("Type").setValue("Button");
        myDatabase.child("Locations").child(this.id).child("Code").setValue("Test Code");
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
