package com.example.criminalintent

import android.content.Context
import android.opengl.Visibility
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.*
import com.example.criminalintent.databinding.FragmentCrimeListBinding
import com.example.criminalintent.databinding.ListItemCrimeBinding
import com.example.criminalintent.model.Crime
import java.util.*

private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment() {
    private val crimeListViewModel: CrimeListViewModel by lazy {
        ViewModelProvider(this).get(CrimeListViewModel::class.java)
    }

    private lateinit var binding: FragmentCrimeListBinding

    private var adapter: CrimeListAdapter? = CrimeListAdapter()

    private var callbacks: Callbacks? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCrimeListBinding.inflate(inflater, container, false)
        binding.crimeRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            val dividerItemDecoration = DividerItemDecoration(context, RecyclerView.VERTICAL)
            val drawable = ResourcesCompat.getDrawable(
                resources,
                R.drawable.recycler_view_divider_drawable,
                null
            )
            dividerItemDecoration.setDrawable(drawable!!)
            addItemDecoration(dividerItemDecoration)
            adapter = this@CrimeListFragment.adapter
        }

        return binding.root
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeListViewModel.crimeListLiveData.observe(viewLifecycleOwner, { crimes ->
            crimes?.let {
                Log.i(TAG, "OnViewCreated: crimes size: ${crimes.size}")
                updateUI(crimes)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_crime -> {
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                callbacks?.onCrimeSelected(crime.id)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(crimes: List<Crime>) {
        adapter?.submitList(crimes)
        binding.crimeRecyclerView.adapter = adapter
        binding.noContentTextView.visibility = if (crimes.isEmpty()) View.VISIBLE else View.GONE
    }


    companion object {
        private const val DATE_TIME_FORMAT_PATTERN = "EEEE, dd.MM.yyyy kk:mm"

        @JvmStatic
        fun newInstance() = CrimeListFragment()
    }

    interface Callbacks {
        fun onCrimeSelected(crimeId: UUID)
    }

    private inner class CrimeViewHolder(private val binding: ListItemCrimeBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {
        private lateinit var crime: Crime

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            this.crime = crime
            binding.apply {
                crimeTitleTextView.text = crime.title
                crimeDateTextView.text = DateFormat.format(DATE_TIME_FORMAT_PATTERN, crime.date)
                crimeIsSolvedImageView.visibility = if (crime.isSolved) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }

        override fun onClick(v: View?) {
            callbacks?.onCrimeSelected(crime.id)
        }
    }

    private inner class CrimeItemDiffCallback : DiffUtil.ItemCallback<Crime>() {
        override fun areItemsTheSame(oldItem: Crime, newItem: Crime) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Crime, newItem: Crime) = oldItem == newItem
    }

    // ListAdapter prevents full recyclerview refreshing
    private inner class CrimeListAdapter :
        ListAdapter<Crime, CrimeViewHolder>(CrimeItemDiffCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ListItemCrimeBinding.inflate(layoutInflater, parent, false)
            return CrimeViewHolder(binding)
        }

        override fun onCurrentListChanged(
            previousList: MutableList<Crime>,
            currentList: MutableList<Crime>
        ) {
            super.onCurrentListChanged(previousList, currentList)
        }

        override fun onBindViewHolder(holder: CrimeViewHolder, position: Int) {
            return holder.bind(getItem(position))
        }
    }

//        private inner class CrimeRecyclerViewAdapter(var crimes: List<Crime>) :
//        RecyclerView.Adapter<CrimeViewHolder>() {
//        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CrimeViewHolder {
//            val layoutInflater = LayoutInflater.from(parent.context)
//            val binding = ListItemCrimeBinding.inflate(layoutInflater, parent, false)
//            return CrimeViewHolder(binding)
//        }
//
//        override fun onBindViewHolder(holder: CrimeViewHolder, position: Int) {
//            return holder.bind(crimes[position])
//
//        }
//
//        override fun getItemCount() = crimes.size
//    }

}