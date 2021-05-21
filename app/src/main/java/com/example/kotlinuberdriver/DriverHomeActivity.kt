package com.example.kotlinuberdriver

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.example.kotlinuberdriver.Utils.UserUtils
import com.example.kotlinuberdriver.databinding.ActivityDriverHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.lang.StringBuilder

class DriverHomeActivity : AppCompatActivity() {

    companion object{
        private const val PICK_IMG_REQUEST = 200
    }

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityDriverHomeBinding
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var circularImgAvatar: ImageView
    private lateinit var waitingDialog: AlertDialog
    private lateinit var storageReference: StorageReference
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDriverHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarDriverHome.toolbar)

        drawerLayout = binding.drawerLayout
        navView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_content_driver_home)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        init()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.driver_home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_driver_home)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMG_REQUEST && resultCode == Activity.RESULT_OK) {
            if(data != null && data.data != null) {
                imageUri = data.data
                circularImgAvatar.setImageURI(imageUri)

                showUploadImageDialog()
            }
        }
    }

    private fun init() {
        storageReference = FirebaseStorage.getInstance().reference

        waitingDialog = AlertDialog.Builder(this)
            .setMessage("Waiting...")
            .setCancelable(false)
            .create()

        navView.setNavigationItemSelectedListener {
            if(it.itemId == R.id.nav_sign_out) {
                val builder = AlertDialog.Builder(this)
                builder
                    .setTitle("Sign out")
                    .setMessage("Do you really want to sign out?")
                    .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
                    .setPositiveButton("SIGN OUT") {dialogInterface, _ ->
                        FirebaseAuth.getInstance().signOut()
                        val intent = Intent(this, SplashScreenActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }.setCancelable(false)

                val dialog = builder.create()
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                }
                dialog.show()
            }
            true
        }

        val headerView = navView.getHeaderView(0)
        val txtName = headerView.findViewById<View>(R.id.tv_username) as TextView
        val txtPhone = headerView.findViewById<View>(R.id.tv_user_phone_number) as TextView
        val txtRate = headerView.findViewById<View>(R.id.tv_rating) as TextView
        circularImgAvatar = headerView.findViewById<View>(R.id.circular_avatar) as ImageView

        txtName.text = Common.buildWelcomeMessage()
        txtPhone.text = Common.currentUser!!.phoneNumber
        txtRate.text = StringBuilder().append(Common.currentUser!!.rating)

        if(Common.currentUser != null && !TextUtils.isEmpty(Common.currentUser!!.avatar)) {
            Glide.with(this)
                .load(Common.currentUser!!.avatar)
                .into(circularImgAvatar)
        }

        circularImgAvatar.setOnClickListener {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(Intent.createChooser(intent, "Select Picture"),
                PICK_IMG_REQUEST)
        }
    }

    private fun showUploadImageDialog() {
        val builder = AlertDialog.Builder(this)
        builder
            .setTitle("Change Profile Picture")
            .setMessage("Do you really want change profile picture ?")
            .setNegativeButton("CANCEL") { dialogInterface, _ -> dialogInterface.dismiss() }
            .setPositiveButton("CHANGE") {dialogInterface, _ ->
               if (imageUri != null ){
                   waitingDialog.show()
                   val avatarFolder = storageReference.child("avatars/"+
                           FirebaseAuth.getInstance().currentUser!!.uid)

                   avatarFolder
                       .putFile(imageUri!!)
                       .addOnFailureListener{ e->
                           Snackbar.make(drawerLayout, e.message!!, Snackbar.LENGTH_SHORT).show()
                           waitingDialog.dismiss()
                       }
                       .addOnCompleteListener{ snapshot->
                           if(snapshot.isSuccessful) {
                               avatarFolder.downloadUrl.addOnSuccessListener { uri ->
                                   val data = HashMap<String, Any>()
                                   data["avatar"] = uri.toString()
                                   UserUtils.updateUser(drawerLayout, data)
                               }
                           }
                           waitingDialog.dismiss()
                       }
                       .addOnProgressListener { snapshot ->
                           val progress = (100.0*snapshot.bytesTransferred / snapshot.totalByteCount)
                           waitingDialog.setMessage(StringBuilder("Uploading: ")
                               .append(progress).append("%"))
                       }
               }
            }.setCancelable(false)

        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
        }
        dialog.show()
    }
}