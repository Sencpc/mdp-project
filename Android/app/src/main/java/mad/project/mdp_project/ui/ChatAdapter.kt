package mad.project.mdp_project.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon
import mad.project.mdp_project.data.ChatMessage
import mad.project.mdp_project.databinding.ItemChatBotBinding
import mad.project.mdp_project.databinding.ItemChatUserBinding

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DiffCallback()) {

    private var markwon: Markwon? = null

    companion object {
        private const val VIEW_TYPE_BOT = 1
        private const val VIEW_TYPE_USER = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isFromBot) VIEW_TYPE_BOT else VIEW_TYPE_USER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (markwon == null) {
            markwon = Markwon.create(parent.context)
        }
        return if (viewType == VIEW_TYPE_BOT) {
            BotViewHolder(ItemChatBotBinding.inflate(inflater, parent, false), markwon!!)
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

    class BotViewHolder(private val binding: ItemChatBotBinding, private val markwon: Markwon) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            markwon.setMarkdown(binding.tvMessage, message.message)
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
