package com.example.travis.berryloc;

import android.content.res.Resources;

import java.io.FileReader;

public class FileCopy {
    private static FileCopy instance;
    private String code;
    public static FileCopy getInstance() {
        if (instance == null) {
            return new FileCopy();
        }
        return instance;
    }

    public static void setInstance(FileCopy instance) {
        FileCopy.instance = instance;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void makeCopy() {
         code = "package com.example.travis.berryloc;\n" +
                "\n" +
                "import android.Manifest;\n" +
                "import android.content.Context;\n" +
                "import android.content.pm.PackageManager;\n" +
                "import android.location.Location;\n" +
                "import android.location.LocationManager;\n" +
                "import android.os.Environment;\n" +
                "import android.support.v4.app.ActivityCompat;\n" +
                "import android.support.v7.app.AppCompatActivity;\n" +
                "import android.os.Bundle;\n" +
                "import android.util.Log;\n" +
                "import android.view.View;\n" +
                "import android.widget.Button;\n" +
                "import android.widget.TextView;\n" +
                "\n" +
                "import com.google.firebase.database.DatabaseReference;\n" +
                "import com.google.firebase.database.FirebaseDatabase;\n" +
                "\n" +
                "import java.io.BufferedReader;\n" +
                "import java.io.File;\n" +
                "import java.io.FileNotFoundException;\n" +
                "import java.io.FileReader;\n" +
                "import java.io.IOException;\n" +
                "import java.net.URL;\n" +
                "import java.nio.file.DirectoryStream;\n" +
                "import java.nio.file.Path;\n" +
                "import java.time.ZonedDateTime;\n" +
                "import java.util.Calendar;\n" +
                "import java.util.Date;\n" +
                "\n" +
                "public class MainActivity extends AppCompatActivity {\n" +
                "    private DatabaseReference myDatabase;\n" +
                "    private com.example.travis.berryloc.Location currLocation;\n" +
                "    private Button currLocButton;\n" +
                "    private Button fireBaseButton;\n" +
                "    private Button completeAllButton;\n" +
                "    private TextView locationResults;\n" +
                "    private TextView status;\n" +
                "\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n" +
                "        myDatabase = FirebaseDatabase.getInstance().getReference();\n" +
                "        currLocButton = findViewById(R.id.currLocBut);\n" +
                "        fireBaseButton = findViewById(R.id.serverRequestBut);\n" +
                "        completeAllButton = findViewById(R.id.doAll);\n" +
                "        locationResults = findViewById(R.id.currLocResults);\n" +
                "        status = findViewById(R.id.status);\n" +
                "        status.setText(\"Success Status: Pending\");\n" +
                "       currLocButton.setOnClickListener(new View.OnClickListener() {\n" +
                "            @Override\n" +
                "            public void onClick(View v) {\n" +
                "                while(!makeLocationNewInstance()) {\n" +
                "                    makeLocationNewInstance();\n" +
                "                }\n" +
                "                String results = \"Longitude: \" + currLocation.getLongitude() +\n" +
                "                        \"\\nLatitude: \" + currLocation.getLatitude();\n" +
                "                locationResults.setText(results);\n" +
                "            }\n" +
                "        });\n" +
                "       fireBaseButton.setOnClickListener(new View.OnClickListener() {\n" +
                "           @Override\n" +
                "           public void onClick(View v) {\n" +
                "               AddToFirebase();\n" +
                "           }\n" +
                "       });\n" +
                "       completeAllButton.setOnClickListener(new View.OnClickListener() {\n" +
                "           @Override\n" +
                "           public void onClick(View v) {\n" +
                "               doAll();\n" +
                "           }\n" +
                "\n" +
                "        });\n" +
                "        readMyCurrentFile();\n" +
                "    }\n" +
                "\n" +
                "    private String readMyCurrentFile() {\n" +
                "        try {\n" +
                "            String path = getFilesDir().toString();\n" +
                "            File file = new File(path);\n" +
                "            FileReader fileReader = new FileReader(path);\n" +
                "            int i;\n" +
                "            while ((i = fileReader.read()) != -1) {\n" +
                "                System.out.println((char)i);\n" +
                "            }\n" +
                "            fileReader.close();\n" +
                "        } catch (FileNotFoundException e) {\n" +
                "            e.printStackTrace();\n" +
                "        }catch (IOException ex) {\n" +
                "            ex.printStackTrace();\n" +
                "        }\n" +
                "        return null;\n" +
                "    }\n" +
                "\n" +
                "    public boolean makeLocationNewInstance() {\n" +
                "        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);\n" +
                "            // TODO: Consider calling\n" +
                "            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {\n" +
                "                ActivityCompat.requestPermissions(this, new String[]\n" +
                "                        {Manifest.permission.ACCESS_FINE_LOCATION}, 1);\n" +
                "                return false;\n" +
                "            }\n" +
                "            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){\n" +
                "                ActivityCompat.requestPermissions(this,\n" +
                "                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},\n" +
                "                        1);\n" +
                "                return false;\n" +
                "            }\n" +
                "            try {\n" +
                "                if (locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER) == null) {\n" +
                "                    throw new Exception();\n" +
                "                }\n" +
                "                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);\n" +
                "                double longitude = location.getLongitude();\n" +
                "                double latitude = location.getLatitude();\n" +
                "                this.currLocation = new com.example.travis.berryloc.Location(longitude, latitude);\n" +
                "            }catch(Exception e) {\n" +
                "                e.printStackTrace();\n" +
                "                locationResults.setText(\"No GPS Fix, Please Try Again\");\n" +
                "            }\n" +
                "            return true;\n" +
                "        }\n" +
                "\n" +
                "    public void AddToFirebase() {\n" +
                "        Date cal = Calendar.getInstance().getTime();\n" +
                "        myDatabase.child(\"Locations\").child(\"Location at \" + cal.toString()).setValue(currLocation);\n" +
                "        myDatabase.push();\n" +
                "        status.setText(\"Success Status: Database Updated\");\n" +
                "    }\n" +
                "    public void doAll() {\n" +
                "        makeLocationNewInstance();\n" +
                "        String results = \"Longitude: \" + currLocation.getLongitude() +\n" +
                "                \"\\nLatitude: \" + currLocation.getLatitude();\n" +
                "        locationResults.setText(results);\n" +
                "        AddToFirebase();\n" +
                "    }\n" +
                "\n" +
                "}\n";
    }
}
