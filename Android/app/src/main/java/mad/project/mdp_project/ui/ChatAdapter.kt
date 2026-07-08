package mad.project.mdp_project.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.core.text.HtmlCompat
import mad.project.mdp_project.data.ChatMessage
import mad.project.mdp_project.databinding.ItemChatBotBinding
import mad.project.mdp_project.databinding.ItemChatUserBinding

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_BOT = 1
        private const val VIEW_TYPE_USER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isFromBot) VIEW_TYPE_BOT else VIEW_TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_BOT) {
            BotViewHolder(ItemChatBotBinding.inflate(inflater, parent, false))
        } else {
            UserViewHolder(ItemChatUserBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        if (holder is BotViewHolder) {
            holder.bind(message)
        } else if (holder is UserViewHolder) {
            holder.bind(message)
        }
    }

    class BotViewHolder(private val binding: ItemChatBotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvMessage.text = HtmlCompat.fromHtml(
                message.message,
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
    }

    class UserViewHolder(private val binding: ItemChatUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.message
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
            return oldItem == newItem
        }
    }
}
