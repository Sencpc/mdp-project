package mad.project.mdp_project.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mad.project.mdp_project.R
import mad.project.mdp_project.data.Habit
import mad.project.mdp_project.databinding.ItemAddHabitBinding
import mad.project.mdp_project.databinding.ItemHabitBinding

class HabitAdapter(
    private val onHabitClick: (Habit) -> Unit,
    private val onAddClick: () -> Unit,
    private val onCompleteClick: (Habit, Boolean) -> Unit
) : ListAdapter<Habit, RecyclerView.ViewHolder>(HabitDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HABIT = 0
        private const val VIEW_TYPE_ADD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < currentList.size) VIEW_TYPE_HABIT else VIEW_TYPE_ADD
    }

    override fun getItemCount(): Int {
        return currentList.size + 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HABIT) {
            val binding = ItemHabitBinding.inflate(inflater, parent, false)
            HabitViewHolder(binding)
        } else {
            val binding = ItemAddHabitBinding.inflate(inflater, parent, false)
            AddViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HabitViewHolder) {
            holder.bind(getItem(position))
        } else if (holder is AddViewHolder) {
            holder.bind()
        }
    }

    inner class HabitViewHolder(private val binding: ItemHabitBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(habit: Habit) {
            binding.tvHabitName.text = habit.name
            binding.tvHabitSubtitle.text = habit.subtitle

            binding.tvCategory.text = habit.category
            
            // Set icon based on category using Material 3 style icons
            val iconRes = when (habit.category.lowercase()) {
                "nutrition" -> R.drawable.ic_habit_nutrition
                "mental" -> R.drawable.ic_habit_mental
                "fitness" -> R.drawable.ic_habit_fitness
                "focus" -> R.drawable.ic_habit_focus
                "sleep" -> R.drawable.ic_habit_sleep
                else -> R.drawable.ic_habit_focus
            }
            binding.ivHabitIcon.setImageResource(iconRes)
            
            if (habit.streak > 0) {
                binding.tvStreak.visibility = View.VISIBLE
                binding.tvStreak.text = "${habit.streak} Day Streak!"
            } else {
                binding.tvStreak.visibility = View.GONE
            }

            // Update Checkbox UI
            binding.cbCompleted.setOnCheckedChangeListener(null) // Prevent recursive calls
            binding.cbCompleted.isChecked = habit.isCompleted
            updateStrikeThrough(habit.isCompleted)
            updateCheckboxForeground(habit.isCompleted)

            binding.cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                updateStrikeThrough(isChecked)
                updateCheckboxForeground(isChecked)
                animateCheckbox(binding.cbCompleted)
                onCompleteClick(habit, isChecked)
            }
            
            binding.root.setOnClickListener {
                onHabitClick(habit)
            }
        }

        private fun updateStrikeThrough(isCompleted: Boolean) {
            if (isCompleted) {
                binding.tvHabitName.paintFlags = binding.tvHabitName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvHabitSubtitle.paintFlags = binding.tvHabitSubtitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvHabitName.setTextColor(0xFF757575.toInt())
            } else {
                binding.tvHabitName.paintFlags = binding.tvHabitName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvHabitSubtitle.paintFlags = binding.tvHabitSubtitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvHabitName.setTextColor(0xFF004B4F.toInt())
            }
        }

        private fun updateCheckboxForeground(isCompleted: Boolean) {
            if (isCompleted) {
                binding.cbCompleted.foregroundTintList =
                    binding.root.context.getColorStateList(android.R.color.white)
            } else {
                binding.cbCompleted.foregroundTintList =
                    binding.root.context.getColorStateList(android.R.color.transparent)
            }
        }

        /**
         * Subtle bounce animation on checkbox toggle.
         * Uses OvershootInterpolator for a satisfying "pop" feel.
         */
        private fun animateCheckbox(view: View) {
            val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.8f)
            val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.8f)
            val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f)
            val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f)

            scaleUpX.interpolator = OvershootInterpolator(2f)
            scaleUpY.interpolator = OvershootInterpolator(2f)

            AnimatorSet().apply {
                play(scaleDownX).with(scaleDownY)
                duration = 100
                start()
            }
            AnimatorSet().apply {
                play(scaleUpX).with(scaleUpY)
                startDelay = 100
                duration = 200
                start()
            }
        }
    }

    inner class AddViewHolder(private val binding: ItemAddHabitBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.root.setOnClickListener {
                onAddClick()
            }
        }
    }

    class HabitDiffCallback : DiffUtil.ItemCallback<Habit>() {
        override fun areItemsTheSame(oldItem: Habit, newItem: Habit): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Habit, newItem: Habit): Boolean {
            return oldItem == newItem
        }
    }
}
