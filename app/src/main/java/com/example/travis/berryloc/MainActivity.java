package com.example.travis.berryloc;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.travis.berryloc.Model.BerryBody;
import com.example.travis.berryloc.Model.Event;
import com.example.travis.berryloc.Model.Handshake;
import com.example.travis.berryloc.Model.ServerMessage;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;

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
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private DatabaseReference myDatabase;
    private DatabaseReference firebase;
    private DatabaseReference callBacks;
    private ServerSocket serverSocket;
    private com.example.travis.berryloc.Model.Location currLocation;
    private TextView locationResults;
    private EditText widgetName;
    private Button restartButton;
    private Handshake handshake;
    private DataSnapshot snap;
    private DataSnapshot callBackSnap;
    private ServerTask serverTask;
    private String id;
    private Server server;
    private ArrayList<String> commands;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        commands = new ArrayList<>();

        firebase = FirebaseDatabase.getInstance().getReference().child("Locations");
        firebase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                snap = dataSnapshot;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        callBacks = FirebaseDatabase.getInstance().getReference().child("Callbacks");
        callBacks.keepSynced(true);
        callBacks.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                callBackSnap = dataSnapshot;
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
                //updateCommands();
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
        restartButton = findViewById(R.id.restartButton);
        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this.getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                MainActivity.this.getApplicationContext().startActivity(intent);
                Runtime.getRuntime().exit(0);
            }
        });
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
                conn = serverSocket.accept(); //waits for client to connect
                clientAddress = conn.getRemoteSocketAddress();
                initiateHandshake(); //does preliminary communication
                completeHandshake();
                PurgeCallback(MainActivity.this.id);
                readInRemoteCalls(); //reads in the remote calls from the client
                //updateCommands();
                doAll();
                buttonPressProcess();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this.getApplicationContext(), "Something went wrong initializing server, Server Not Running", Toast.LENGTH_LONG);
            }
        }
        private void readInRemoteCalls() throws IOException {
            boolean doneWaiting = false;
            while (!doneWaiting) {
                serverSocket.setSoTimeout(500);
                try {
                    conn = serverSocket.accept();

                    out = new PrintWriter(conn.getOutputStream(), false);
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String inputCodeLine;
                    StringBuilder codeSb = new StringBuilder();
                    while ((inputCodeLine = in.readLine()) != null) {
                        codeSb.append(inputCodeLine + "\n");
                    }
                    JsonParser parser = new JsonParser();
                    try {
                        JsonObject json = (JsonObject) parser.parse(codeSb.toString()); //Check to see if is in json form, if so it is a remote call, if not it is the client's code
                        AddToCallbackList(json); //send remote to firebase


                    }catch (JsonSyntaxException ex) {
                        doneWaiting = true;
                        handshake.setCode(codeSb.toString());
                    }
                } catch (SocketTimeoutException e) {
                    doneWaiting = true;
                }
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
                Toast.makeText(MainActivity.this.getApplicationContext(), "Something went wrong when initiating handshake, Server Not Running", Toast.LENGTH_LONG);
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
                out.close();
                socket.close();
                AutoLocationUpdate autoLocationUpdate = new AutoLocationUpdate();
                autoLocationUpdate.startAutoUpdate();
            }catch(IOException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this.getApplicationContext(), "Something went wrong when completing handshake, Server Not Running", Toast.LENGTH_LONG);
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
                if (event.getCode() == null) {
                    return;
                }
                handshake.setCode(event.getCode());
                out.write(command + event.getCode());
                out.flush();
                out.close();
                socket.close();
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
                    serverSocket.setSoTimeout(0);
                    conn = serverSocket.accept();
                    in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputCodeLine;
                    StringBuilder codeSb = new StringBuilder();
                    JsonParser jsonParser = new JsonParser();
                    while((inputCodeLine = in.readLine()) != null) {
                        codeSb.append(inputCodeLine);
                        JsonElement json =jsonParser.parse(inputCodeLine);
                        System.out.println(json);
                        if (((JsonObject) json).get("status") != null) {
                            PurgeCallback(MainActivity.this.id);
                            continue;
                        }
                        String command = FixStringsFromFirebase(((JsonObject) json).get("command").toString());
                        if (command.equals("remote-response")) {
                            break;
                        }
                        else if(command.equals("remote-command")) { //If is a new remote Call
                            MainActivity.this.AddToCallbackList(json);
                        }
                        else if (command.equals("event")) { //If is an event call
                            ParseList(json);
                        }
                    }
                    conn.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        private void PurgeCallback(final String firebaseID) {
            if (callBackSnap != null) {
                for (DataSnapshot child : callBackSnap.getChildren()) {
                    String childName = (String) child.child("Source").getValue();
                    childName = FixStringsFromFirebase(childName);
                    if (childName.equals(firebaseID)) {
                        child.getRef().removeValue();
                    }
                }
            }
        }

        private String FixStringsFromFirebase(String myString) {
            StringBuilder sb = new StringBuilder(myString);
            int index;
            while ((index = sb.indexOf("\"")) != -1) { //while an " is found, necessary because firebase adds quotations sometimes
                sb.deleteCharAt(index);
            }
            myString = sb.toString();
            return myString;
        }

        private void ParseList(JsonElement json) {
            String command = FixStringsFromFirebase(((JsonObject) json).get("command").toString());
            String event = FixStringsFromFirebase(((JsonObject) json).get("event").toString());
            String name = FixStringsFromFirebase(((JsonObject) json).get("name").toString());
            Iterable<DataSnapshot> calls;
            if (callBackSnap != null) {
                calls = callBackSnap.getChildren();
            }
            else {
                return;
            }
            //add ipAddress to Callback
            String destination;
            String address;
            String key;
            for (DataSnapshot child : calls) {
                while (!FirebaseUpdateDone(child)) {
                    System.out.println("Waiting for Firebase");
                }
                child = callBackSnap.child(child.getKey());
                String source = FixStringsFromFirebase((String) child.child("Source").getValue());
                String attribute = FixStringsFromFirebase((String) child.child("Attribute").getValue());
                if (name.equals(source) && event.equals(attribute)) {
                    key = FixStringsFromFirebase((String) child.child("Key").getValue());
                    destination = FixStringsFromFirebase((String) child.child("Destination").getValue());
                    address = FixStringsFromFirebase((String) snap.child(destination).child("zClient Address").getValue());
                    JsonObject jsonObject = new JsonObject();
                    jsonObject.addProperty("command", "remote-command");
                    jsonObject.addProperty("attribute", attribute);
                    jsonObject.addProperty("source", source);
                    jsonObject.addProperty("key", key);
                    sendToViableClient(jsonObject, address);

                }
            }
        }

        private boolean FirebaseUpdateDone(DataSnapshot child) {
            child = MainActivity.this.callBackSnap.child(child.getKey());
            System.out.println("Check");
            //Error caused when the datasnapshot hasn't updated completely from firebase in time
            if (child.getChildrenCount() == 5) {
                return true;
            }
            else {
                return false;
            }
        }

        private void sendToViableClient(JsonObject json, String clientAddress) {
            try {
                Socket socket = new Socket(clientAddress, 6666);
                out = new PrintWriter(socket.getOutputStream(), true);
                out.write(json.toString());
                out.flush();
                out.close();
                socket.close();
            }catch (IOException e) {
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
        myDatabase.child("zClient Address").setValue(this.server.fixIpAddress(server.clientAddress.toString()));
        myDatabase.push();
    }

    public void AddToCallbackList(JsonElement json) {
        String command = ((JsonObject) json).get("command").toString();
        String destination = ((JsonObject) json).get("destination").toString();
        String source = ((JsonObject) json).get("source").toString();
        String attribute = ((JsonObject) json).get("attribute").toString();
        String key = ((JsonObject) json).get("key").toString();
        callBacks = FirebaseDatabase.getInstance().getReference().child("Callbacks").child("CallBack: " + key + destination);
        callBacks.child("Command").setValue(command);
        callBacks.child("Destination").setValue(destination);
        callBacks.child("Source").setValue(source);
        callBacks.child("Attribute").setValue(attribute);
        callBacks.child("Key").setValue(key);
        callBacks.push();
    }

   /* public void AddToCallbackList() {
        //work here
        myDatabase = FirebaseDatabase.getInstance().getReference().child("Commands").child(this.id);
        for(int i = 0; i < commands.size(); i++) {
            myDatabase.child("command " + (i + 1)).setValue(commands.get(i));
        }
        myDatabase.push();
    }*/


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

    /*public void updateCommands() { //Update Attributes in the code running on this server's client
        AttributeParser attributeParser = new AttributeParser(handshake.getCode());
        commands = attributeParser.parse();
        //AddToCallbackList();

    }*/



}
