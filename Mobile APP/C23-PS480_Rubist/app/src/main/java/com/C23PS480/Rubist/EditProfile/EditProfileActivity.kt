package com.C23PS480.Rubist.EditProfile

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_GET_CONTENT
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import com.C23PS480.Rubist.API.Response.AddPostResponse
import com.C23PS480.Rubist.API.Response.FileUploadResponse
import com.C23PS480.Rubist.API.Response.UpdateProfileResponse
import com.C23PS480.Rubist.API.Retrofit.ApiConfig
import com.C23PS480.Rubist.Fragment.AddPost
import com.C23PS480.Rubist.MainViewModel
import com.C23PS480.Rubist.Model.UserPreference
import com.C23PS480.Rubist.R

import com.C23PS480.Rubist.Utils.uriToFile
import com.C23PS480.Rubist.ViewModelFactory
import com.C23PS480.Rubist.databinding.ActivityEditProfileBinding
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "Setting")
class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding : ActivityEditProfileBinding
    private lateinit var mainViewModel : MainViewModel

    private var getFile: File? = null

    companion object{
        private const val MAXIMAL_SIZE = 1000000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mainViewModel = ViewModelProvider(
            this,
            ViewModelFactory(UserPreference.getInstance(dataStore))
        )[MainViewModel::class.java]

        setupViewModel()
        binding.userAvatar.setOnClickListener{
            startGallery()
        }

        binding.tvDone.setOnClickListener{
            SaveDialog()
        }

    }

    private fun setupViewModel() {

        mainViewModel.getUser().observe(this) { user ->
            binding.tvProfileName.text = user.name
            binding.tvProfileEmail.text = user.email

        }
    }

    private fun startGallery() {
        val intent = Intent()
        intent.action = ACTION_GET_CONTENT
        intent.type = "image/*"
        val chooser = Intent.createChooser(intent, "Pilih gambar")
        launcherIntentGallery.launch(chooser)
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImg = result.data?.data as Uri
            selectedImg.let { uri ->
                val myFile = uriToFile(uri, this@EditProfileActivity)
                getFile = myFile
                binding.userAvatar.setImageURI(uri)
            }
        }
    }

    private fun SaveDialog() {
        val dialogMessage = getString(R.string.SaveProfile)
        val dialogTitle = getString(R.string.Profile)


        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle(dialogTitle)

        alertDialogBuilder
            .setMessage(dialogMessage)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                uploadImage()

            }
            .setNegativeButton(getString(R.string.No)) { dialog, _ -> dialog.cancel() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun uploadImage(){
        if (getFile != null){
            val file = reduceFileImage(getFile as File)

            val location = binding.etProfileLocation.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val mobileNumber = binding.etProfileNumber.text.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val requestFile: RequestBody = RequestBody.create(
                "image/jpeg".toMediaType(),
                file
            )
            val multipartImage = MultipartBody.Part.createFormData("file", file.getName(), requestFile);


            val apiService = ApiConfig.getApiService()
            val uploadImageRequest = apiService.updateProfile(location, mobileNumber, multipartImage)
            uploadImageRequest.enqueue(object : Callback<UpdateProfileResponse> {
                override fun onResponse(
                    call: Call<UpdateProfileResponse>,
                    response: Response<UpdateProfileResponse>
                ) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody != null) {
                            Toast.makeText(
                                this@EditProfileActivity,
                                "Post Uploaded",
                                Toast.LENGTH_SHORT
                            ).show()
                            //navigateBackToStoryList()
                        }
                    } else {
                        Toast.makeText(
                            this@EditProfileActivity,
                            response.body()?.error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<UpdateProfileResponse>, t: Throwable) {
                    Toast.makeText(this@EditProfileActivity, t.message, Toast.LENGTH_SHORT).show()

                }
            })
        } else {
            Toast.makeText(
                this@EditProfileActivity,
                "Masukkan Gambar Terlebih Dahulu",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun reduceFileImage(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.path)
        var compressQuality = 100
        var streamLength: Int
        do {
            val bmpStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
            val bmpPicByteArray = bmpStream.toByteArray()
            streamLength = bmpPicByteArray.size
            compressQuality -= 5
        } while (streamLength > MAXIMAL_SIZE)
        bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, FileOutputStream(file))
        return file
    }
}