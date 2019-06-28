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
import com.google.firebase.database.ValueEventListener;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private DatabaseReference myDatabase;
    private DatabaseReference firebase;
    private ServerSocket serverSocket;
    private com.example.travis.berryloc.Model.Location currLocation;
    private TextView locationResults;
    private EditText widgetName;
    private Handshake handshake;
    private DataSnapshot snap;
    private ServerTask serverTask;
    private String id;
    private Server server;
    private ArrayList<String> commands;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        commands = new ArrayList<>();

        firebase = FirebaseDatabase.getInstance().getReference().child("Callbacks");
        firebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                snap = dataSnapshot;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        myDatabase = FirebaseDatabase.getInstance().getReference().child("Locations");
        myDatabase.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (handshake.getCode() == null) {
                    return;
                }
                updateCommands();
                if (dataSnapshot.getKey().equals(MainActivity.this.id)) {
                        final Event event = dataSnapshot.getValue(Event.class);
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //Fixme This will become a problem later
                                event.setName(MainActivity.this.id);
                                server.sendCodeToClient(event);
                            }
                        });
                        thread.start();
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
        id = widgetName.getText().toString();
        serverTask = new ServerTask();
        serverTask.execute();
    }



    public class Server {
        private SocketAddress clientAddress;
        private String address;
        private String local;
        private Socket conn;
        private BufferedReader in;
        private PrintWriter out;
        public Server() {
            try {
                serverSocket = new ServerSocket(8080);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                conn = serverSocket.accept();
                clientAddress = conn.getRemoteSocketAddress();
                initiateHandshake();
                completeHandshake();
                conn = serverSocket.accept();
                out = new PrintWriter(conn.getOutputStream(), false);
                in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                String inputCodeLine;
                StringBuilder codeSb = new StringBuilder();
                while((inputCodeLine = in.readLine()) != null) {
                    codeSb.append(inputCodeLine + "\n");
                }

                handshake.setCode(codeSb.toString());

                updateCommands();
                doAll();
                buttonPressProcess();

            } catch (IOException e) {
                e.printStackTrace();
                locationResults.setText("Something went wrong, Server Not Running");
            }
        }

            private void initiateHandshake() {
                try {
                    out = new PrintWriter(conn.getOutputStream(), false);
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder sb = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        sb.append(inputLine);
                    }
                    updateRegistry(sb.toString());//get initial handshake
                    address = fixIpAddress(clientAddress.toString());
                    local = getLocalIpAdd();
                }catch (IOException e) {
                    e.printStackTrace();
                    locationResults.setText("Something went wrong when initiating handshake, Server Not Running");
                }
            }

            private void completeHandshake() {
            try {
                Socket socket = new Socket(address, 6666);
                out = new PrintWriter(socket.getOutputStream(), true);
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("command", "fixedip-ack");
                jsonObject.addProperty("ip", local);
                out.write(jsonObject.toString());
                out.flush();
                AutoLocationUpdate autoLocationUpdate = new AutoLocationUpdate();
                autoLocationUpdate.startAutoUpdate();
            }catch(IOException e) {
                e.printStackTrace();
                locationResults.setText("Something went wrong when completing handshake, Server Not Running");
            }
            }
            private String fixIpAddress(String clientAddress) {
                StringBuilder sb = new StringBuilder(clientAddress);
                int index = sb.indexOf(":");
                sb.delete(index, index + 6);
                sb.delete(0,1);
            return sb.toString();
            }

        public void sendCodeToClient(Event event) {
            try {
                Socket socket = new Socket(address, 6666);
                out = new PrintWriter(socket.getOutputStream(), true);
                String command = "code-save ";
                out.write(command + event.getCode());
                out.flush();
            }catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String getLocalIpAdd() {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]
                        {Manifest.permission.ACCESS_WIFI_STATE}, 1);
            }
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
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
            widgetName.setText(berryBody.getName());
            MainActivity.this.id = berryBody.getName();
        }

        private void buttonPressProcess() {
            while (true) {
                try {
                    conn = serverSocket.accept();
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputCodeLine;
                    StringBuilder codeSb = new StringBuilder();
                    JsonParser jsonParser = new JsonParser();
                    while((inputCodeLine = in.readLine()) != null) {
                        codeSb.append(inputCodeLine);
                        JsonElement json = jsonParser.parse(inputCodeLine);
                        System.out.println(json);
                        if(((JsonObject) json).get("event") == null) {
                           // MainActivity.this.AddToCallbackList(json);
                        }
                        else if (((JsonObject) json).get("event") != null && ((JsonObject) json).get("name") != null) {
                            ParseList(json);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        private void ParseList(JsonElement json) {
            myDatabase = FirebaseDatabase.getInstance().getReference().child("Callbacks");
            String command = ((JsonObject) json).get("command").toString();
            String event = ((JsonObject) json).get("event").toString();
            String name = ((JsonObject) json).get("name").toString();

            Iterable <DataSnapshot> calls = snap.getChildren();
            //add ipAddress to Callback
            String destination;
            String key;
            String viableClient;
            for (DataSnapshot child : calls) {
                String source = (String) child.child("Source").getValue();
                String attribute = (String) child.child("Attribute").getValue();
                if (name.equals(source) && event.equals(attribute)) {
                    destination = (String) child.child("Destination").getValue();
                    key = (String) child.child("Key").getValue();
                    viableClient = (String) child.child("Address").getValue();
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("attribute", attribute);
                    jsonObject.addProperty("source", source);
                    jsonObject.addProperty("key", key);
                    sendToViableClient(viableClient, jsonObject);

                }
            }
            System.out.println("done");

        }

        private void sendToViableClient(String address, JsonObject json) {
            Socket socket = null;
            try {
                socket = new Socket(address, 6666);
                out = new PrintWriter(socket.getOutputStream(), true);
                out.write(json.toString());
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
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


    public void UpdateLocationOnly() {
        makeLocationNewInstance();
        myDatabase = FirebaseDatabase.getInstance().getReference().child("Locations").child(this.id);
        myDatabase.child("Longitude").setValue(currLocation.getLongitude());
        myDatabase.child("Latitude").setValue(currLocation.getLatitude());
        myDatabase.push();
    }

    public void AddToFirebase() {
        myDatabase = FirebaseDatabase.getInstance().getReference().child("Locations").child(this.id);
        myDatabase.child("Longitude").setValue(currLocation.getLongitude());
        myDatabase.child("Latitude").setValue(currLocation.getLatitude());
        myDatabase.child("Type").setValue(handshake.getBerryBody().getType());
        myDatabase.child("Code").setValue(handshake.getCode());
        myDatabase.push();
    }

    public void AddToCallbackList(JsonElement json) {
        myDatabase = FirebaseDatabase.getInstance().getReference().child("Callbacks").child("Callback0");
        String command = ((JsonObject) json).get("command").toString();
        String destination = ((JsonObject) json).get("destination").toString();
        String source = ((JsonObject) json).get("source").toString();
        String attribute = ((JsonObject) json).get("attribute").toString();
        String key = ((JsonObject) json).get("key").toString();
        myDatabase.child("Command").setValue(command);
        myDatabase.child("Destination").setValue(destination);
        myDatabase.child("Source").setValue(source);
        myDatabase.child("Attribute").setValue(attribute);
        myDatabase.child("Key").setValue(key);
        myDatabase.push();
    }

    public void AddToCallbackList() {
        //work here
        myDatabase = FirebaseDatabase.getInstance().getReference().child("Callbacks").child(this.id);
        int counter = 0;
        for(int i = 0; i < commands.size(); i++) {
        }
    }


    public void doAll() {
        makeLocationNewInstance();
        String results = "Longitude: " + currLocation.getLongitude() +
                "\nLatitude: " + currLocation.getLatitude() + "\nSession Token: " + MainActivity.this.id;
        locationResults.setText(results);
        AddToFirebase();

    }


    public class AutoLocationUpdate {
        public Timer timer;
       public synchronized void startAutoUpdate() {
            if (timer == null) {
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        MainActivity.this.UpdateLocationOnly();
                    }
                };
                timer = new Timer();
                timer.scheduleAtFixedRate(task, 0, 30000);
            }
       }
    }

    public void updateCommands() { //Update Attributes in the code running on this server's client
        AttributeParser attributeParser = new AttributeParser(handshake.getCode());
        commands = attributeParser.parse();

    }



}
