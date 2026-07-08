package mad.project.mdp_project.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.SleepLog
import mad.project.mdp_project.databinding.FragmentSleepTrackerBinding
import mad.project.mdp_project.model.SleepViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SleepTrackerFragment : Fragment() {

    private var _binding: FragmentSleepTrackerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SleepViewModel by viewModels()

    private var selectedStartTime: Long = 0L
    private var selectedEndTime: Long = 0L

    private lateinit var sleepLogAdapter: SleepLogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSleepTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        sleepLogAdapter = SleepLogAdapter()
        binding.rvSleepLogs.apply {
            adapter = sleepLogAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectBedtime.setOnClickListener {
            showTimePicker { timeMillis ->
                selectedStartTime = timeMillis
                binding.tvBedtime.text = formatTime(timeMillis)
            }
        }

        binding.btnSelectWakeTime.setOnClickListener {
            showTimePicker { timeMillis ->
                selectedEndTime = timeMillis
                binding.tvWakeTime.text = formatTime(timeMillis)
            }
        }

        binding.btnLogSleep.setOnClickListener {
            if (selectedStartTime == 0L || selectedEndTime == 0L) {
                Toast.makeText(requireContext(), "Pilih waktu tidur dan bangun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.addSleepLog(selectedStartTime, selectedEndTime)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sleepLogs.collectLatest { logs ->
                    sleepLogAdapter.submitList(logs)
                    updateStats()
                }
            }
        }

        viewModel.addResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Sleep log berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
                // Reset form
                selectedStartTime = 0L
                selectedEndTime = 0L
                binding.tvBedtime.text = "Select"
                binding.tvWakeTime.text = "Select"
            }
            result.onFailure { error ->
                Toast.makeText(requireContext(), error.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStats() {
        binding.tvAvgSleep.text = String.format(Locale.getDefault(), "%.1fh", viewModel.getAverageSleepHours())
        binding.tvAvgQuality.text = String.format(Locale.getDefault(), "%.1f/5", viewModel.getAverageQuality())
        binding.tvSleepStreak.text = "${viewModel.getSleepStreak()} days"
    }

    /**
     * Menampilkan TimePicker saja — tanggal otomatis hari ini.
     */
    private fun showTimePicker(onTimeSelected: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, hour, minute ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onTimeSelected(selectedCalendar.timeInMillis)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    private fun formatTime(millis: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Adapter untuk RecyclerView daftar sleep log.
 */
class SleepLogAdapter : androidx.recyclerview.widget.ListAdapter<SleepLog, SleepLogAdapter.ViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<SleepLog>() {
        override fun areItemsTheSame(oldItem: SleepLog, newItem: SleepLog) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: SleepLog, newItem: SleepLog) = oldItem == newItem
    }
) {
    inner class ViewHolder(private val binding: mad.project.mdp_project.databinding.ItemSleepLogBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(sleepLog: SleepLog) {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            binding.tvLogDate.text = dateFormat.format(Date(sleepLog.date))
            binding.tvLogTimeRange.text = "${timeFormat.format(Date(sleepLog.startTime))} - ${timeFormat.format(Date(sleepLog.endTime))}"
            binding.tvLogDuration.text = sleepLog.getFormattedDuration()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = mad.project.mdp_project.databinding.ItemSleepLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}