package com.example.criminalintent

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.example.criminalintent.databinding.FragmentPhotoBinding
import com.example.criminalintent.utils.getScaledBitmap

private const val ARG_PHOTO_FILE_NAME = "photo_file_name"

class PhotoFragment : DialogFragment(), View.OnClickListener {
    private var photoFileName: String? = null
    private lateinit var binding: FragmentPhotoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            photoFileName = it.getString(ARG_PHOTO_FILE_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPhotoBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (photoFileName != null) {
            binding.photoImageView.setImageBitmap(
                getScaledBitmap(
                    photoFileName!!,
                    requireActivity()
                )
            )
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.requestFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(true)
        }
    }

    override fun onClick(v: View?) {
        if (v != binding.photoImageView) {
            dismissAllowingStateLoss()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(photoFileName: String) =
            PhotoFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_FILE_NAME, photoFileName)
                }
            }
    }

}