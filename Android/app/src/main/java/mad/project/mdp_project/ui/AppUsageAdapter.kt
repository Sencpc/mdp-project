package mad.project.mdp_project.ui

import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mad.project.mdp_project.databinding.ItemAppUsageBinding
import mad.project.mdp_project.model.AppUsageInfo

class AppUsageAdapter(
    private val packageManager: PackageManager
) : RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder>() {

    private var appList: List<AppUsageInfo> = emptyList()
    private var totalScreenTimeMs: Long = 0L

    fun submitList(list: List<AppUsageInfo>, totalTime: Long) {
        appList = list
        totalScreenTimeMs = totalTime
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
        val binding = ItemAppUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppUsageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
        holder.bind(appList[position])
    }

    override fun getItemCount(): Int = appList.size

    inner class AppUsageViewHolder(private val binding: ItemAppUsageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appInfo: AppUsageInfo) {
            binding.tvAppName.text = appInfo.appName
            binding.tvAppTime.text = appInfo.getFormattedTime()

            // Try to load app icon
            try {
                val icon = packageManager.getApplicationIcon(appInfo.packageName)
                binding.ivAppIcon.setImageDrawable(icon)
                binding.ivAppIcon.setPadding(0, 0, 0, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                // Keep default or handle error
            }

            // Calculate progress relative to total screen time
            val progress = if (totalScreenTimeMs > 0) {
                ((appInfo.totalTimeInForeground.toDouble() / totalScreenTimeMs) * 100).toInt().coerceIn(0, 100)
            } else {
                0
            }
            binding.progressApp.progress = progress
        }
    }
}
