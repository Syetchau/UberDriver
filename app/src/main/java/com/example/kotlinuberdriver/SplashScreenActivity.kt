package com.example.kotlinuberdriver

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.example.kotlinuberdriver.Model.DriverInfo
import com.example.kotlinuberdriver.Utils.UserUtils
import com.example.kotlinuberdriver.databinding.ActivitySplashScreenBinding
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object{
        private const val LOGIN_REQUEST_CODE = 200
    }

    private lateinit var authUIProviders: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseAuthListener: FirebaseAuth.AuthStateListener
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var driverInfoReference: DatabaseReference

    //ViewBinding
    private lateinit var splashScreenBinding: ActivitySplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splashScreenBinding = ActivitySplashScreenBinding.inflate(layoutInflater)
        val view = splashScreenBinding.root
        setContentView(view)

        initData()
    }

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if(firebaseAuth != null && firebaseAuthListener != null){
            firebaseAuth.removeAuthStateListener(firebaseAuthListener)
        }
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_REQUEST_CODE) {
            val response = IdpResponse.fromResultIntent(data)
            if(resultCode == Activity.RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
            } else{
                Toast.makeText(this@SplashScreenActivity, ""+response!!.error!!.message,
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initData() {
        firebaseDatabase = FirebaseDatabase.getInstance()
        driverInfoReference = firebaseDatabase.getReference(Common.DRIVER_INFO_REFERENCE)

        authUIProviders = Arrays.asList(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseAuthListener = FirebaseAuth.AuthStateListener { myFirebaseAuth ->
            val user = myFirebaseAuth.currentUser
            if (user != null) {
                FirebaseMessaging
                    .getInstance()
                    .token
                    .addOnFailureListener { e->
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
                    .addOnSuccessListener { instanceIdResult ->
                        Log.d("userToken", instanceIdResult)
                        UserUtils.updateToken(this, instanceIdResult)
                    }
                checkUserFromFirebase()
            } else{
                showLoginLayout();
            }
        }
    }

    private fun delaySplashScreen() {
        Completable.timer(3, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
            .subscribe {
               firebaseAuth.addAuthStateListener(firebaseAuthListener)
            }
    }

    private fun showLoginLayout() {
        val authMethodPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAuthMethodPickerLayout(authMethodPickerLayout)
                .setTheme(R.style.LoginTheme)
                .setAvailableProviders(authUIProviders)
                .setIsSmartLockEnabled(false)
                .build(),
            LOGIN_REQUEST_CODE
        )
    }

    private fun checkUserFromFirebase() {
        driverInfoReference
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                       val model = snapshot.getValue(DriverInfo::class.java)
                        navigateToHomeActivity(model)
                    } else{
                        showRegisterLayout()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity, error.message,
                        Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun navigateToHomeActivity(model: DriverInfo?) {
        Common.currentUser = model
        startActivity(Intent(this, DriverHomeActivity::class.java))
        finish()
    }

    private fun showRegisterLayout() {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null)

        val firstNameEditText = itemView.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val lastNameEditText = itemView.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val phoneNumberEditText = itemView.findViewById<View>(R.id.edt_phone_number) as TextInputEditText
        val btnContinue = itemView.findViewById<View>(R.id.btn_continue) as Button

        val userPhoneNumber = FirebaseAuth.getInstance().currentUser!!.phoneNumber
        if (userPhoneNumber != null && !TextUtils.isDigitsOnly(userPhoneNumber)) {
            phoneNumberEditText.setText(userPhoneNumber)

            builder.setView(itemView)
            val dialog = builder.create()
            dialog.show()

            btnContinue.setOnClickListener {
                when {
                    TextUtils.isDigitsOnly(firstNameEditText.toString()) -> {
                        Toast.makeText(
                            this@SplashScreenActivity, "Please enter First Name",
                            Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    TextUtils.isDigitsOnly(lastNameEditText.toString()) -> {
                        Toast.makeText(
                            this@SplashScreenActivity, "Please enter Last Name",
                            Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    TextUtils.isDigitsOnly(phoneNumberEditText.toString()) ->  {
                        Toast.makeText(
                            this@SplashScreenActivity, "Please enter Phone Number",
                            Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    else -> {
                        val driverInfoModel = DriverInfo()
                        driverInfoModel.firstName = firstNameEditText.text.toString()
                        driverInfoModel.lastName = lastNameEditText.text.toString()
                        driverInfoModel.phoneNumber = phoneNumberEditText.text.toString()
                        driverInfoModel.rating = 0.0

                        driverInfoReference
                            .child(FirebaseAuth.getInstance().currentUser!!.uid)
                            .setValue(driverInfoModel)
                            .addOnFailureListener { error ->
                                Toast.makeText(
                                    this@SplashScreenActivity, ""+error.message,
                                    Toast.LENGTH_SHORT).show()
                                dialog.dismiss()
                                splashScreenBinding.progressBar.visibility = View.GONE
                            }
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@SplashScreenActivity, "Register successfully!",
                                    Toast.LENGTH_SHORT).show()
                                dialog.dismiss()

                                navigateToHomeActivity(driverInfoModel)
                                splashScreenBinding.progressBar.visibility = View.GONE
                            }
                    }
                }
            }
        }
    }
}