package com.example.criminalintent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import com.example.criminalintent.databinding.FragmentCrimeBinding
import com.example.criminalintent.model.Crime
import com.example.criminalintent.utils.getScaledBitmap
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.*

private const val ARG_CRIME_ID = "arg_crime_id"
private const val DATE_PICKER_FRAGMENT = "date_picker_fragment"
private const val TIME_PICKER_FRAGMENT = "time_picker_fragment"
private const val PHOTO_FRAGMENT = "photo_fragment"
private const val DATE_RESULT_KEY = "date_result_key"
private const val TIME_RESULT_KEY = "time_result_key"
private const val SELECTED_DATE = "selected_date"


class CrimeFragment : Fragment() {
    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    private val contactPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                showContactsPermissionRationaleToast()
            }
        }

    private val multiplePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionGrants ->
            permissionGrants.apply {
                if (containsKey(Manifest.permission.CAMERA) && containsKey(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (!getValue(Manifest.permission.CAMERA) || !getValue(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        showCameraAndStoragePermissionRationaleToast()
                    }
                }
            }
        }

    private val contactActivityLauncher =
        registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            getContactData(uri)
            updateUI()
        }

    private val cameraActivityLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                updatePhotoView() // TODO: check if possible to replace to updateUI()?
                requireActivity().revokeUriPermission(
                    photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }

    private val onGlobalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        imageViewWidth = binding.crimeImageView.measuredWidth
        imageViewHeight = binding.crimeImageView.measuredHeight
    }

    private lateinit var binding: FragmentCrimeBinding
    private lateinit var crime: Crime
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var viewTreeObserver: ViewTreeObserver

    private var imageViewWidth = 0
    private var imageViewHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crimeId = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
        crime = Crime()
        setFragmentResultListener(DATE_RESULT_KEY) { _, bundle ->
            val date = bundle.getSerializable(SELECTED_DATE) as Date
            crime.date = date
            updateUI()
        }
        setFragmentResultListener(TIME_RESULT_KEY) { _, bundle ->
            val date = bundle.getSerializable(SELECTED_DATE) as Date
            crime.date = date
            updateUI()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCrimeBinding.inflate(inflater, container, false)
        configureCrimeDateButton()
        configureCrimeTimeButton()
        setCrimeImageViewTreeObserver()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setCrimeLiveDataObserver()
    }

    override fun onStart() {
        super.onStart()
        configureCrimeTitleEditText()
        configureCrimeSolvedCheckBox()
        configureCrimeReportButton()
        configureCrimeSuspectButton()
        configureCrimeCallSuspectButton()
        configureCameraButton()
        configureCrimeImageView()
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener)
        }
    }

    private fun setCrimeLiveDataObserver() {
        crimeDetailViewModel.crimeLiveData.observe(viewLifecycleOwner) { crime ->
            crime?.let {
                this.crime = crime
                photoFile = crimeDetailViewModel.getPhotoFile(crime)
                photoUri = FileProvider.getUriForFile(
                    requireActivity(),
                    FILE_PROVIDER_AUTHORITY,
                    photoFile
                )
                updateUI()
            }
        }
    }

    private fun setCrimeImageViewTreeObserver() {
        viewTreeObserver = binding.crimeImageView.viewTreeObserver
        if (viewTreeObserver.isAlive) {
            viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener)
        }
    }

    private fun getResolvedActivityForContacts(): ResolveInfo? {
        // Check availability of contact Activity
        val packageManager = requireActivity().packageManager
        val pickContactIntent =
            Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
        return packageManager.resolveActivity(
            pickContactIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
    }

    private fun getResolvedActivityForCamera(): ResolveInfo? {
        val packageManager = requireActivity().packageManager
        val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        return packageManager.resolveActivity(
            captureImageIntent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
    }

    private fun configureCrimeTitleEditText() {
        val crimeTitleTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = s.toString()
            }

            override fun afterTextChanged(s: Editable?) {
            }
        }
        binding.crimeTitleEditText.addTextChangedListener(crimeTitleTextWatcher)
    }

    private fun configureCrimeSolvedCheckBox() {
        binding.crimeSolvedCheckBox.setOnCheckedChangeListener { _, isChecked ->
            crime.isSolved = isChecked
        }
    }

    private fun configureCrimeReportButton() {
        binding.crimeReportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent =
                    Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }
    }

    private fun configureCrimeSuspectButton() {
        binding.crimeSuspectButton.apply {
            if (getResolvedActivityForContacts() != null) {
                setOnClickListener {
                    if (hasContactsPermission()) {
                        contactActivityLauncher.launch()
                    } else {
                        requireContactsPermission()
                    }
                }
            } else {
                isEnabled = false
            }
        }
    }

    private fun configureCrimeCallSuspectButton() {
        binding.crimeCallSuspectButton.apply {
            isEnabled = crime.phoneNumber.isNotBlank()
            setOnClickListener {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${crime.phoneNumber}"))
                startActivity(intent)
            }
        }
    }

    private fun configureCameraButton() {
        binding.cameraButton.apply {
            if (getResolvedActivityForCamera() != null) {
                setOnClickListener {
                    if (hasCameraPermission() && hasWriteStoragePermission()) {
                        val packageManager = requireActivity().packageManager
                        val captureImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        val cameraActivities = packageManager.queryIntentActivities(
                            captureImageIntent,
                            PackageManager.MATCH_DEFAULT_ONLY
                        )
                        for (cameraActivity in cameraActivities) {
                            requireActivity().grantUriPermission(
                                cameraActivity.activityInfo.packageName,
                                photoUri,
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        }
                        cameraActivityLauncher.launch(photoUri)
                    } else {
                        requireCameraAndStoragePermissions()
                    }
                }

            } else {
                isEnabled = false
            }
        }
    }

    private fun configureCrimeImageView() {
        binding.crimeImageView.setOnClickListener {
            if (photoFile.exists()) {
                PhotoFragment.newInstance(photoFile.path).apply {
                    show(this@CrimeFragment.parentFragmentManager, PHOTO_FRAGMENT)
                }
            }
        }
    }

    private fun configureCrimeDateButton() {
        binding.crimeDateButton.setOnClickListener {
            DatePickerFragment.newInstance(crime.date).apply {
                show(this@CrimeFragment.parentFragmentManager, DATE_PICKER_FRAGMENT)
            }
        }
    }

    private fun configureCrimeTimeButton() {
        binding.crimeTimeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date).apply {
                show(this@CrimeFragment.parentFragmentManager, TIME_PICKER_FRAGMENT)
            }
        }
    }

    private fun updateUI() {
        with(binding) {
            val dateFormat = DateFormat.getMediumDateFormat(context)
            crimeTitleEditText.setText(crime.title)
            crimeDateButton.text = dateFormat.format(crime.date)
            crimeTimeButton.text = DateFormat.format(TIME_FORMAT_PATTERN, crime.date)
            crimeSolvedCheckBox.apply {
                isChecked = crime.isSolved
                jumpDrawablesToCurrentState() // Skip the animation of checking (because query to database is async - book, page 298)
            }
            if (crime.suspect.isNotEmpty()) {
                crimeSuspectButton.text = crime.suspect
            }
            crimeCallSuspectButton.isEnabled = crime.phoneNumber.isNotBlank()
            updatePhotoView()
        }
    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(
                photoFile.path,
                imageViewWidth,
                imageViewHeight
            )
            binding.crimeImageView.apply {
                setImageBitmap(bitmap)
                contentDescription = getString(R.string.crime_image_view_image_description)
            }
        } else {
            binding.crimeImageView.apply {
                setImageDrawable(null)
                contentDescription = getString(R.string.crime_image_view_no_image_description)
            }
        }
    }

    private fun getCrimeReport(): String {
        with(crime) {
            val dateFormat = DateFormat.getDateFormat(context)

//            val dateString = DateFormat.format(DATE_FORMAT_PATTERN, date)
            val dateString = dateFormat.format(date)
            val solvedString =
                getString(if (isSolved) R.string.crime_report_solved else R.string.crime_report_unsolved)
            val suspectString =
                if (suspect.isBlank()) getString(R.string.crime_report_no_suspect)
                else getString(R.string.crime_report_suspect, suspect)
            return getString(R.string.crime_report, title, dateString, solvedString, suspectString)
        }
    }

    private fun getContactData(contactUri: Uri?) {
        if (contactUri == null || !hasContactsPermission()) {
            return
        }
        val queryFields =
            arrayOf(
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER,
                ContactsContract.Contacts._ID
            )
        val contactCursor =
            requireActivity().contentResolver.query(contactUri, queryFields, null, null, null)
        contactCursor?.use { contacts ->
            if (contacts.count == 0) {
                return
            }
            contacts.moveToFirst()
            var phoneNumber = ""
            val suspect =
                contacts.getString(contacts.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
            val hasPhoneNumber =
                contacts.getInt(contacts.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
            if (hasPhoneNumber > 0) {
                val contactId =
                    contacts.getString(contacts.getColumnIndex(ContactsContract.Contacts._ID))
                val phoneCursor = requireActivity().contentResolver.query(
                    Phone.CONTENT_URI,
                    arrayOf(Phone.CONTACT_ID),
                    "${Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )
                phoneCursor?.use { phones ->
                    Log.d("-------- getContactData", "phones count: ${phones.count}")
                    if (phones.count == 0) {
                        return
                    }
                    phones.moveToFirst()
                    phoneNumber = phones.getString(phones.getColumnIndex(Phone.NUMBER))
                    Log.d("-------- getContactData", "phoneNumber: $phoneNumber")
                }
            }
            crime.suspect = suspect
            crime.phoneNumber = phoneNumber
            crimeDetailViewModel.saveCrime(crime)
        }
    }

    private fun requireContactsPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            showContactsPermissionSnackbar()
        } else {
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun requireCameraAndStoragePermissions() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            showCameraAndStoragePermissionSnackbar()
        } else {
            multiplePermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun hasContactsPermission() = hasPermission(Manifest.permission.READ_CONTACTS)

    private fun hasCameraPermission() = hasPermission(Manifest.permission.CAMERA)

    private fun hasWriteStoragePermission() =
        hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun hasPermission(permission: String) =
        ContextCompat.checkSelfPermission(
            requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED

    private fun showContactsPermissionRationaleToast() {
        Toast.makeText(requireContext(), R.string.contact_permission_rationale, Toast.LENGTH_LONG)
            .show()
    }

    private fun showCameraAndStoragePermissionRationaleToast() {
        Toast.makeText(
            requireContext(),
            R.string.camera_and_storage_permission_rationale,
            Toast.LENGTH_LONG
        )
            .show()
    }

    private fun showCameraAndStoragePermissionSnackbar() {
        Snackbar.make(
            binding.root,
            R.string.camera_and_storage_permission_request,
            Snackbar.LENGTH_INDEFINITE
        )
            .apply {
                setAction(android.R.string.ok) {
                    multiplePermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            }
            .show()
    }

    private fun showContactsPermissionSnackbar() {
        Snackbar.make(binding.root, R.string.contact_permission_request, Snackbar.LENGTH_INDEFINITE)
            .apply {
                setAction(android.R.string.ok) {
                    contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            }
            .show()
    }

    companion object {
        private const val DATE_FORMAT_PATTERN = "dd.MM.yyyy"
        private const val TIME_FORMAT_PATTERN = "kk:mm"
        private const val FILE_PROVIDER_AUTHORITY = "com.example.criminalintent.fileprovider"

        @JvmStatic
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}