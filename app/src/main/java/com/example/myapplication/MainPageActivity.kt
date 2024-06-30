package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.WindowInsetsAnimation
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ShareCompat.IntentBuilder
import com.example.myapplication.databinding.ActivityMainPageBinding
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderApi
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.Locale

class MainPageActivity : AppCompatActivity(),OnMapReadyCallback {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainPageBinding
    private lateinit var auth: FirebaseAuth;
    private lateinit var user: FirebaseUser;
    private lateinit var user_Id: String;
    private lateinit var databaseReference: DatabaseReference;
    private lateinit var t1: TextView;
    private lateinit var t2: TextView;
    private lateinit var mMap: GoogleMap
    private lateinit var client: GoogleApiClient;
    private lateinit var request: LocationRequest;
    private lateinit var newLatLng: LatLng;
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var currentMarker: Marker;
    private lateinit var b4_sourceButton: Button;
    private lateinit var destinationMarker: Marker;
    private lateinit var endLatLng: LatLng;

    private lateinit var retrofit: Retrofit
    private lateinit var geoapifyService: GeoapifyService
    private lateinit var autoCompleteTextView: AutoCompleteTextView

    private lateinit var suggestionListView: ListView
    private lateinit var suggestionAdapter: ArrayAdapter<GeoapifyResult>

    private lateinit var orderTaxiButton: Button






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance();
        binding = ActivityMainPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        b4_sourceButton = findViewById(R.id.button4)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        newLatLng = LatLng(0.0,0.0)
        endLatLng = LatLng(0.0,0.0)




        setSupportActionBar(binding.appBarMainPage.toolbar)


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)




        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main_page)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_payment, R.id.nav_trips, R.id.nav_help,R.id.nav_rides,R.id.nav_signOut
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_signOut -> {
                    Toast.makeText(this, "Sign out clicked", Toast.LENGTH_SHORT).show()
                    signOutUser()
                    true
                }
                else -> {
                    navController.navigate(menuItem.itemId)
                    drawerLayout.closeDrawers()
                    true
                }
            }
        }

        if (FirebaseAuth.getInstance().currentUser == null){
            startActivity(Intent(this,MainActivity::class.java))
            finish()
        }else{
            user = FirebaseAuth.getInstance().currentUser!!;
            user_Id = user.uid;
            databaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child(user_Id);
            databaseReference.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Handle the data change here

                    val name: String? = snapshot.child("name").getValue(String::class.java)
                    val email: String? = snapshot.child("email").getValue(String::class.java)
                    t1 = findViewById(R.id.name_text);
                    t2 = findViewById(R.id.email_text);
                    t1.text = name;
                    t2.text = email;

                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle the error here
                }
            })

        }

        retrofit = Retrofit.Builder()
            .baseUrl("https://api.geoapify.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        geoapifyService = retrofit.create(GeoapifyService::class.java)

        autoCompleteTextView = findViewById(R.id.autoCompleteDestination)

        suggestionListView = findViewById(R.id.suggestionListView)
        suggestionAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        suggestionListView.adapter = suggestionAdapter

        setupAutocomplete()

        orderTaxiButton = findViewById(R.id.orderTaxiButton)
        orderTaxiButton.setOnClickListener {
            openOrderConfirmationActivity()
        }



    }


    private var placeSelected = false


    private fun setupAutocomplete() {
        autoCompleteTextView.threshold = 3
        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedPlace = parent.getItemAtPosition(position) as GeoapifyResult
            handleSelectedPlace(selectedPlace)
            suggestionListView.visibility = View.GONE
            placeSelected = true
            autoCompleteTextView.clearFocus() // Add this line to remove focus from the AutoCompleteTextView
        }

        autoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                suggestionListView.visibility = View.GONE
            }
        }

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (placeSelected && before < count) {
                    placeSelected = false
                }
            }
            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { text ->
                    if (text.length >= 3 && !placeSelected) {
                        fetchAutocompleteSuggestions(text)
                    } else {
                        suggestionListView.visibility = View.GONE
                    }
                }
            }
        })

        suggestionListView.setOnItemClickListener { _, _, position, _ ->
            val selectedPlace = suggestionAdapter.getItem(position)
            selectedPlace?.let {
                autoCompleteTextView.setText(it.formatted)
                handleSelectedPlace(it)
                suggestionListView.visibility = View.GONE
                placeSelected = true
                autoCompleteTextView.clearFocus() // Add this line to remove focus from the AutoCompleteTextView
            }
        }
    }


    private fun fetchAutocompleteSuggestions(query: String) {
        val call = geoapifyService.getAutocompleteSuggestions(query, "a5e365066de747f3ad651f388a11edc0")
        Log.d("Autocomplete", "Request URL: ${call.request().url()}")

        call.enqueue(object : retrofit2.Callback<GeoapifyResponse> {
            override fun onResponse(call: Call<GeoapifyResponse>, response: Response<GeoapifyResponse>) {
                if (response.isSuccessful) {
                    if (!placeSelected){
                        Log.d("Autocomplete", "Response: ${response.body()}")
                        suggestionAdapter.clear()
                        response.body()?.features?.map { it.properties }?.let { suggestionAdapter.addAll(it) }
                        suggestionAdapter.notifyDataSetChanged()
                        suggestionListView.visibility = if (suggestionAdapter.count > 0) View.VISIBLE else View.GONE
                    }

                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("Autocomplete", "Response error: ${response.code()} - $errorBody")
                    Toast.makeText(this@MainPageActivity, "Error: ${response.code()} - $errorBody", Toast.LENGTH_LONG).show()
                    suggestionListView.visibility = View.GONE

                }
            }

            override fun onFailure(call: Call<GeoapifyResponse>, t: Throwable) {
                Log.e("Autocomplete", "Network error: ${t.message}")
                Toast.makeText(this@MainPageActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                suggestionListView.visibility = View.GONE
            }
        })
    }





    private fun handleSelectedPlace(place: GeoapifyResult) {
        endLatLng = LatLng(place.lat, place.lon)
        autoCompleteTextView.setText(place.formatted)
        placeSelected = true

        if (!::destinationMarker.isInitialized) {
            val options = MarkerOptions()
                .title("Destination")
                .position(endLatLng)
            destinationMarker = mMap.addMarker(options)!!
        } else {
            destinationMarker.position = endLatLng
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(endLatLng, 15f))
        suggestionListView.visibility = View.GONE
        autoCompleteTextView.clearFocus() // Add this line to remove focus from the AutoCompleteTextView
        orderTaxiButton.visibility = View.VISIBLE
    }

    private fun openOrderConfirmationActivity() {
        val intent = Intent(this, OrderConfirmationActivity::class.java)
        startActivity(intent)
    }




//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if(resultCode==200){
//            if (resultCode == RESULT_OK){
//                Place place = PlaceAutocomplete.getPlace(this,data);
//                String name = place.getName().toString()
//                newLatLng = place.getLatLng()
//                b4_sourceButton.text(name)
//
//                if(currentMarker == null){
//                    val options1 =  MarkerOptions()
//                    options1.title("Pickup Location")
//                    options1.position(newLatLng)
//
//                    currentMarker = mMap.addMarker(options1)
//
//                }else{
//                    currentMarker.setPosition(newLatLng)
//
//                }
//
//
//
//            }
//        }else if(resultCode == 400){
//            if(resultCode == RESULT_OK){
//                Place myplace = PlaceAutocomplete.getPlace(this,data);
//                String name = myplace.getName().toString()
//                endLatLng = myplace.getLatLng()
//                b5_destinationButton.text(name)
//                if(destinationMarker==null){
//                    val options1 =  MarkerOptions()
//                    options1.title("Destination")
//                    options1.position(endLatLng)
//
//                    destinationMarker = mMap.addMarker(options1)
//                }else{
//                    destinationMarker.setPosition(newLatLng)
//
//                }
//
//
//
//            }
//        }
//    }

    private fun updateAddressButton() {
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val myaddresses = geocoder.getFromLocation(newLatLng.latitude, newLatLng.longitude, 1)
            if (myaddresses != null && myaddresses.isNotEmpty()) {
                val address = myaddresses[0].getAddressLine(0) ?: ""
                val city = myaddresses[0].locality ?: ""
                b4_sourceButton.text = "$address $city"
            } else {
                b4_sourceButton.text = "Address not found"
            }
        } catch (e: IOException) {
            e.printStackTrace()
            b4_sourceButton.text = "Error fetching address"
        }
    }

    private fun signOutUser() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            auth.signOut()
            Toast.makeText(this, "User signed out", Toast.LENGTH_SHORT).show()
            // Navigate to login activity or your app's entry point
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, "No user is currently signed in", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_page, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main_page)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        mMap.uiSettings.isMyLocationButtonEnabled = true

        if (hasLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
                requestLocationUpdates()
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun updateMapWithNewLocation(location: Location) {
        newLatLng = LatLng(location.latitude+4.4600078, location.longitude+144.5885779)

        mMap.clear() // Clear previous markers

        if (!::currentMarker.isInitialized) {
            currentMarker = mMap.addMarker(MarkerOptions().position(newLatLng).title("Current Location"))!!
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLatLng, 15f))
        } else {
            currentMarker.setPosition(newLatLng)
        }

        updateAddressButton()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLocationUpdates()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun requestLocationUpdates() {
        if (!hasLocationPermission()) {
            requestLocationPermission()
            return
        }

        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 5000 // Update every 5 seconds
            fastestInterval = 2000 // Fastest update interval is 2 seconds
            smallestDisplacement = 10f // Update if moved by 10 meters
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateMapWithNewLocation(location)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }




    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }




}




