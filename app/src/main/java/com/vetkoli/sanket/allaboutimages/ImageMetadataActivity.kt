package com.vetkoli.sanket.allaboutimages

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_image_metadata.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*




class ImageMetadataActivity : AppCompatActivity() {

    companion object {

        const val REQUEST_IMAGE_CAPTURE = 1
        const val MY_CAMERA_PERMISSION_CODE = 101

        fun newIntent(context: Context): Intent {
            return Intent(context, ImageMetadataActivity::class.java)
        }
    }

    private var mImageBitmap: Bitmap? = null
    private var mCurrentPhotoPath: String? = null

    //Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_metadata)

        init()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            try {
                mImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, Uri.parse(mCurrentPhotoPath))
                imageView.setImageBitmap(mImageBitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "write storage permission granted", Toast.LENGTH_LONG).show()
                requestImageCapture()
            } else {
                Toast.makeText(this, "write storage permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    //Helpers

    private fun init() {
        initOnclickListeners()
    }

    private fun initOnclickListeners() {
        btnCamera.setOnClickListener { onCameraBtnClick() }
    }

    private fun onCameraBtnClick() {
        if (ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_CAMERA_PERMISSION_CODE);
        } else {
            requestImageCapture()
        }
    }

    private fun requestImageCapture() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (cameraIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Log.i("IMA", "IOException")
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(applicationContext, "$packageName.fileprovider", photoFile))
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(
                Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, // prefix
                ".jpg", // suffix
                storageDir      // directory
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.absolutePath
        return image
    }

}
