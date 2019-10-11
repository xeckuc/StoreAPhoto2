package com.examples.storeaphoto2

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    //Saved instance state key
    val photoPathKey = "lastPhotoPath"

    //Our custom request code for image capture from camera.
    private val REQUEST_TAKE_PHOTO = 1

    //Modify for storing full size picture
    var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindUI()
    }

    //Handle rotation
    //Must use here because when calling this function the views are laid out and measured
    //so the setPic will not crash on dividing by 0
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            currentPhotoPath?.let {
                setPic(it)
            }
        }
    }

    //Pull the photoPath when state is restored
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        savedInstanceState?.getString(photoPathKey)?.let {
            currentPhotoPath = it
        }
        super.onRestoreInstanceState(savedInstanceState)
    }


    //Save the photoPath when state is changed
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(photoPathKey, currentPhotoPath)
        super.onSaveInstanceState(outState)
    }

    /**
     * Method Binds actions to the UI widgets of the layout
     */
    private fun bindUI() {
        btnCamera.setOnClickListener {
            //dispatchTakePictureIntent()
            dispatchTakePictureAndSaveToFileIntent()
        }
    }

    /**
     * Method that handles the result that comes from 'startActivityForResult'.
     * In our case -> camera capture
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //Storing the image to file
        if (requestCode == REQUEST_TAKE_PHOTO) {

            if (resultCode == RESULT_OK) {
                currentPhotoPath?.let {
                    setPic(it)
                    scanFile(it)
                    Toast.makeText(
                        this,
                        getString(R.string.toast_action_photo_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            //If result is canceled
            else if (resultCode == RESULT_CANCELED)
                Toast.makeText(
                    this,
                    getString(R.string.toast_action_cancelled),
                    Toast.LENGTH_SHORT
                ).show()

        }
    }

    /**
     * Creates an image file with unique signature to store the captured photo
     */
    @Throws(IOException::class)
    private fun createImageFile(): File {

        //Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "JPEG_${timeStamp}_", //prefix
            ".jpg", //suffix
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    /**
     * Method calls the camera activity to handle the image capture AND save it into a file
     * in the external storage using file provider
     */
    private fun dispatchTakePictureAndSaveToFileIntent() {

        //Check permissions of writing to external storage
        if (!PermissionManager.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return
        }

        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->

            //Ensure there is a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                //Create the file which will hold the photo
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    //Error occurred while creating file
                    Log.e(TAG, "Error while creating file. Exception: ${ex.message}")
                    null
                }

                //If the file was created, build a URI path for it and dispatch activity for camera capture
                //with an 'extra' message that tells it to store the result into out URI
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        BuildConfig.APPLICATION_ID,
                        it
                    )


                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    private fun setPic(photoPath: String) {

        //Get the dimensions of the view
        val targetWidth: Int = ivPhoto.width
        val targetHeight: Int = ivPhoto.height

        val bmOptions = BitmapFactory.Options().apply {
            //Get the dimensions of the bitmap
            inJustDecodeBounds = true

            val photoWidth: Int = outWidth
            val photoHeight: Int = outHeight

            //Determine how much to scale down the image
            val scaleFactor: Int = min(photoWidth / targetWidth, photoHeight / targetHeight)

            //Decode the image file into Bitmap sized to fill the View
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
        }

        BitmapFactory.decodeFile(photoPath, bmOptions)?.also { bitmap ->
            ivPhoto.setImageBitmap(bitmap)
        }
    }

    private fun scanFile(path: String) {

        val galleryIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val imageFile = File(path)
        val imageUri = Uri.fromFile(imageFile)

        galleryIntent.data = imageUri
        this.sendBroadcast(galleryIntent)
    }
}
