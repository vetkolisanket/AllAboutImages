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
import android.os.Parcelable
import android.provider.MediaStore
import android.support.media.ExifInterface
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

        const val SELECT_IMAGE = 1
        const val MY_CAMERA_PERMISSION_CODE = 101

        const val TAG = "ImageMetadataActivity"

        fun newIntent(context: Context): Intent {
            return Intent(context, ImageMetadataActivity::class.java)
        }
    }

    private var mImageBitmap: Bitmap? = null
    private var mCurrentPhotoPath: String? = null
    private lateinit var photoFile: File

    //Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_metadata)

        init()
    }

    private lateinit var exifInterface: ExifInterface

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            try {
                val uri = getImageUri(resultCode, data)
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    exifInterface = ExifInterface(inputStream)
                    val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    when (orientation) {
                        ExifInterface.ORIENTATION_NORMAL -> tvExifInfo.append("Orientation normal")
                        ExifInterface.ORIENTATION_UNDEFINED -> tvExifInfo.append("Orientation undefined")
                        ExifInterface.ORIENTATION_ROTATE_90 -> tvExifInfo.append("Orientation 90")
                        ExifInterface.ORIENTATION_ROTATE_180 -> tvExifInfo.append("Orientation 180")
                        ExifInterface.ORIENTATION_ROTATE_270 -> tvExifInfo.append("Orientation 270")
                    }
                    tvExifInfo.append("\n")
                    tvExifInfo.append("Latitude: " + exifInterface.latLong!![0].toString() + "\nLongitude: " + exifInterface.latLong!![1].toString())
                    inputStream.close()
                }
                mImageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
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
        btnSelectImage.setOnClickListener { onSelectImageBtnClick() }
    }

    private fun onSelectImageBtnClick() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_CAMERA_PERMISSION_CODE);
        } else {
            requestImageCapture()
        }
    }

    private fun requestImageCapture() {
        photoFile = createImageFile()
        val selectImageIntent = getPickImageIntent()
        if (selectImageIntent!!.resolveActivity(packageManager) != null) {
            startActivityForResult(selectImageIntent, SELECT_IMAGE)
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

    private fun getPickImageIntent(): Intent? {
        var chooserIntent: Intent? = null

        var intentList: MutableList<Intent> = ArrayList()

        val pickIntent = Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.putExtra("return-data", true)
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(applicationContext, "$packageName.fileprovider", photoFile))
        intentList = addIntentsToList(intentList, pickIntent)
        intentList = addIntentsToList(intentList, takePhotoIntent)

        if (intentList.size > 0) {
            chooserIntent = Intent.createChooser(intentList.removeAt(intentList.size - 1),
                    getString(R.string.pick_image_intent_text))
            chooserIntent!!.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentList.toTypedArray<Parcelable>())
        }

        return chooserIntent
    }

    private fun addIntentsToList(list: MutableList<Intent>, intent: Intent): MutableList<Intent> {
        val resInfo = packageManager.queryIntentActivities(intent, 0)
        for (resolveInfo in resInfo) {
            val packageName = resolveInfo.activityInfo.packageName
            val targetedIntent = Intent(intent)
            targetedIntent.setPackage(packageName)
            list.add(targetedIntent)
            Log.d(TAG, "Intent: " + intent.action + " package: " + packageName)
        }
        return list
    }

    private fun getImageUri(resultCode: Int, imageReturnedIntent: Intent?): Uri? {
        var selectedImageUri: Uri? = null
        if (resultCode == Activity.RESULT_OK) {
            val isCamera = imageReturnedIntent == null ||
                    imageReturnedIntent.data == null ||
                    imageReturnedIntent.data!!.toString().contains(photoFile.toString())
            if (isCamera) {
                /** CAMERA  */
                selectedImageUri = FileProvider.getUriForFile(applicationContext, "$packageName.fileprovider", photoFile)
            } else {
                /** ALBUM  */
                selectedImageUri = imageReturnedIntent!!.data
            }
            Log.d(TAG, "selectedImageUri: " + selectedImageUri!!)
        }
        return selectedImageUri
    }

}
