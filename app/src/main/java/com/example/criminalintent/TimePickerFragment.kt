package com.example.criminalintent

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import java.util.*

private const val ARG_DATE = "date"
private const val TIME_RESULT_KEY = "time_result_key"
private const val SELECTED_DATE = "selected_date"

class TimePickerFragment : DialogFragment() {
    private var date = Date()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        date = arguments?.getSerializable(ARG_DATE) as Date
        val onTimeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
            }
            setFragmentResult(TIME_RESULT_KEY, bundleOf(SELECTED_DATE to calendar.time))
        }
        val calendar = Calendar.getInstance()
        calendar.time = date

        return TimePickerDialog(
            requireContext(),
            onTimeSetListener,
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
    }

    companion object {
        fun newInstance(date: Date): TimePickerFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DATE, date)
            }
            return TimePickerFragment().apply {
                arguments = args
            }
        }
    }
}