package uk.co.fireburn.gettaeit.shared.domain

import uk.co.fireburn.gettaeit.shared.data.TaskEntity

/** A deterministic, local-only answer to “what can I manage just now?” */
object TaskNowFilter {
    fun filter(
        tasks: List<TaskEntity>,
        availableMinutes: Int?,
        lowEnergyOnly: Boolean
    ): List<TaskEntity> = tasks.filter { task ->
        val estimate = task.estimatedMinutes
        val fitsTime = availableMinutes == null || (estimate != null && estimate <= availableMinutes)
        val fitsEnergy = !lowEnergyOnly || (estimate != null && estimate <= LOW_ENERGY_MAX_MINUTES)
        fitsTime && fitsEnergy
    }

    const val LOW_ENERGY_MAX_MINUTES = 15
}
