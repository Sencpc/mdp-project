package mad.project.mdp_project.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import mad.project.mdp_project.R
import mad.project.mdp_project.databinding.ItemDoctorBinding
import mad.project.mdp_project.model.Doctor

class DoctorAdapter : ListAdapter<Doctor, DoctorAdapter.DoctorViewHolder>(DoctorDiffCallback()) {

    class DoctorViewHolder(val binding: ItemDoctorBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val binding = ItemDoctorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DoctorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = getItem(position)
        with(holder.binding) {
            tvDoctorName.text = doctor.name
            tvSpecialty.text = doctor.specialty
            tvRating.text = root.context.getString(R.string.reviews_format, doctor.rating, doctor.reviewsCount)
            tvDescription.text = doctor.description
            tvNextAvailableValue.text = doctor.nextAvailable
            tvBadge.visibility = if (doctor.isHighlyRecommended) View.VISIBLE else View.GONE
        }
    }

    class DoctorDiffCallback : DiffUtil.ItemCallback<Doctor>() {
        override fun areItemsTheSame(oldItem: Doctor, newItem: Doctor): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Doctor, newItem: Doctor): Boolean {
            return oldItem == newItem
        }
    }
}
