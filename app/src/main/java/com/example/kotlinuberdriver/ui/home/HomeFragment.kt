package com.example.kotlinuberdriver.ui.home

import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.kotlinuberdriver.Common
import com.example.kotlinuberdriver.R
import com.example.kotlinuberdriver.databinding.FragmentHomeBinding
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import java.util.*

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mMap: GoogleMap
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var mapFragment: SupportMapFragment

    //location
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //FirebaseReference
    private lateinit var onlineRef: DatabaseReference
    private lateinit var driversLocationRef: DatabaseReference
    private lateinit var geofire: GeoFire
    private var currentUserRef: DatabaseReference?= null

    private val onlineValueEventListener = object:ValueEventListener{
        override fun onDataChange(snapshot: DataSnapshot) {
            if(snapshot.exists() && currentUserRef != null){
                currentUserRef!!.onDisconnect().removeValue()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        initDriverLocation()

        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        geofire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
        onlineRef.removeEventListener(onlineValueEventListener)
        super.onDestroy()
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!

        Dexter.withContext(requireContext())
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION,)
            .withListener(object:PermissionListener{
                @SuppressLint("MissingPermission")
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {

                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationClickListener {
                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener { e ->
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                            .addOnSuccessListener { location ->
                                val userLatLng = LatLng(location.latitude, location.longitude)
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f))
                            }
                    }
                    val view = mapFragment.requireView()
                        .findViewById<View>("1".toInt())
                        .parent as View
                    val locationBtn = view.findViewById<View>("2".toInt())
                    val params = locationBtn.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
                    params.bottomMargin = 50
                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(context, "Permission " + p0!!.permissionName+ " was denied",
                    Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }
            }).check()

        try {
            val success = googleMap.setMapStyle(MapStyleOptions.
            loadRawResourceStyle(context, R.raw.uber_maps_style))
            if (!success) {
                Log.e("ERROR GET MAP STYLE", "parsing error")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("ERROR", e.message!!)
        }
        Snackbar.make(mapFragment.requireView(), "You're online", Snackbar.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingPermission")
    private fun initDriverLocation() {

        onlineRef = FirebaseDatabase.getInstance().reference.child(".info/connected")

        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.fastestInterval = 15000
        locationRequest.interval = 10000
        locationRequest.smallestDisplacement = 50f

        locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                val pos = LatLng(locationResult.lastLocation.latitude,
                    locationResult.lastLocation.longitude)
                val geoCoder = Geocoder(requireContext(), Locale.getDefault())
                val addressList: List<Address>?

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 18f))
                try {
                    addressList = geoCoder.getFromLocation(
                            locationResult.lastLocation.latitude,
                            locationResult.lastLocation.longitude, 1)
                    val cityName = addressList[0].locality

                    driversLocationRef = FirebaseDatabase.getInstance()
                        .getReference(Common.DRIVERS_LOCATION_REFERENCE)
                        .child(cityName)
                    currentUserRef = driversLocationRef
                        .child(FirebaseAuth.getInstance().currentUser!!.uid)
                    geofire = GeoFire(driversLocationRef)

                    //update location
                    geofire.setLocation(
                        FirebaseAuth.getInstance().currentUser!!.uid,
                        GeoLocation(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
                    ){ _: String?, error: DatabaseError? ->
                        if(error != null) {
                            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    registerOnlineSystem()
                }catch (e: IOException){
                    Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()
                }
            }
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
            locationCallback, Looper.myLooper())
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)
    }
}