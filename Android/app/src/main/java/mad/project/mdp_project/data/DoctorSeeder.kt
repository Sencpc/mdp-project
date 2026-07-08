package mad.project.mdp_project.data

import java.time.LocalDateTime

object DoctorSeeder {

    fun getSeedDoctors(): List<DoctorEntity> {
        val now = LocalDateTime.now()

        return listOf(
            // ==================== General Practice ====================
            DoctorEntity(
                id = 1,
                doctorName = "Dr. Sarah Jenkins",
                category = "General Practice",
                description = "Experienced general practitioner focusing on holistic health and preventative care for adults and families.",
                rating = 4.9,
                availableTime = now.plusHours(2),
                profileIcon = "medical_services"
            ),
            DoctorEntity(
                id = 2,
                doctorName = "Dr. Kevin Smith",
                category = "General Practice",
                description = "Board-certified family medicine physician specializing in chronic disease management and elderly care.",
                rating = 4.7,
                availableTime = now.plusHours(4),
                profileIcon = "stethoscope"
            ),
            DoctorEntity(
                id = 3,
                doctorName = "Dr. Amelia Brown",
                category = "General Practice",
                description = "Dedicated to providing comprehensive primary care with a focus on women's health and pediatric medicine.",
                rating = 4.8,
                availableTime = now.plusHours(6),
                profileIcon = "local_hospital"
            ),
            DoctorEntity(
                id = 4,
                doctorName = "Dr. David Wilson",
                category = "General Practice",
                description = "Passionate about preventive medicine and health education, offering personalized treatment plans for all ages.",
                rating = 4.6,
                availableTime = now.plusHours(8),
                profileIcon = "medical_services"
            ),
            DoctorEntity(
                id = 5,
                doctorName = "Dr. Olivia Clark",
                category = "General Practice",
                description = "Expert in urgent care and routine check-ups, committed to accessible healthcare for underserved communities.",
                rating = 4.5,
                availableTime = now.plusHours(24),
                profileIcon = "stethoscope"
            ),

            // ==================== Therapy ====================
            DoctorEntity(
                id = 6,
                doctorName = "Dr. Michael Chen",
                category = "Therapy",
                description = "Specializing in anxiety, stress management, and cognitive behavioral therapy (CBT) for adults.",
                rating = 4.8,
                availableTime = now.plusHours(3),
                profileIcon = "psychology"
            ),
            DoctorEntity(
                id = 7,
                doctorName = "Dr. Emily White",
                category = "Therapy",
                description = "Licensed psychologist with expertise in trauma recovery, PTSD, and mindfulness-based stress reduction.",
                rating = 4.9,
                availableTime = now.plusHours(5),
                profileIcon = "self_improvement"
            ),
            DoctorEntity(
                id = 8,
                doctorName = "Dr. Daniel Moore",
                category = "Therapy",
                description = "Experienced therapist helping individuals and couples navigate relationship issues, depression, and life transitions.",
                rating = 4.7,
                availableTime = now.plusHours(7),
                profileIcon = "spa"
            ),
            DoctorEntity(
                id = 9,
                doctorName = "Dr. Sophia Taylor",
                category = "Therapy",
                description = "Child and adolescent psychologist focusing on behavioral disorders, ADHD, and emotional development.",
                rating = 4.6,
                availableTime = now.plusHours(10),
                profileIcon = "psychology"
            ),
            DoctorEntity(
                id = 10,
                doctorName = "Dr. Ethan Scott",
                category = "Therapy",
                description = "Integrative therapist combining evidence-based techniques with holistic approaches for mental wellness.",
                rating = 5.0,
                availableTime = now.plusHours(26),
                profileIcon = "self_improvement"
            ),

            // ==================== Nutrition ====================
            DoctorEntity(
                id = 11,
                doctorName = "Dr. Elena Rodriguez",
                category = "Nutrition",
                description = "Providing personalized nutrition plans focused on mindful eating, gut health, and sustained energy.",
                rating = 4.9,
                availableTime = now.plusHours(3).plusMinutes(30),
                profileIcon = "nutrition"
            ),
            DoctorEntity(
                id = 12,
                doctorName = "Dr. Chloe Evans",
                category = "Nutrition",
                description = "Registered dietitian specializing in sports nutrition, weight management, and metabolic health optimization.",
                rating = 4.8,
                availableTime = now.plusHours(5).plusMinutes(30),
                profileIcon = "restaurant"
            ),
            DoctorEntity(
                id = 13,
                doctorName = "Dr. Lucas Hall",
                category = "Nutrition",
                description = "Expert in clinical nutrition for chronic conditions including diabetes, heart disease, and food allergies.",
                rating = 4.7,
                availableTime = now.plusHours(9),
                profileIcon = "eco"
            ),
            DoctorEntity(
                id = 14,
                doctorName = "Dr. Grace Young",
                category = "Nutrition",
                description = "Holistic nutritionist helping clients achieve optimal health through plant-based diets and functional foods.",
                rating = 4.5,
                availableTime = now.plusHours(12),
                profileIcon = "nutrition"
            ),
            DoctorEntity(
                id = 15,
                doctorName = "Dr. Ryan Adams",
                category = "Nutrition",
                description = "Specializing in pediatric nutrition, prenatal dietary planning, and family wellness programs.",
                rating = 4.6,
                availableTime = now.plusHours(28),
                profileIcon = "restaurant"
            )
        )
    }
}
