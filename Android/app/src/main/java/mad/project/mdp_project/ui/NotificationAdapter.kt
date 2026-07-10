package mad.project.mdp_project.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mad.project.mdp_project.R
import mad.project.mdp_project.data.NotificationEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private var notifications: List<NotificationEntity>
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.iv_icon)
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvMessage: TextView = view.findViewById(R.id.tv_message)
        val tvTime: TextView = view.findViewById(R.id.tv_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]
        
        holder.tvTitle.text = notification.title
        holder.tvMessage.text = notification.message
        
        val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
        holder.tvTime.text = sdf.format(Date(notification.timestamp))

        when (notification.type) {
            "HABIT" -> holder.ivIcon.setImageResource(R.drawable.ic_dash_checklist)
            "SCREEN_TIME" -> holder.ivIcon.setImageResource(R.drawable.ic_nav_screen)
            "AI_INSIGHT" -> holder.ivIcon.setImageResource(R.drawable.ic_dash_notification)
            else -> holder.ivIcon.setImageResource(R.drawable.ic_dash_notification)
        }
    }

    override fun getItemCount() = notifications.size

    fun updateData(newNotifications: List<NotificationEntity>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
}
