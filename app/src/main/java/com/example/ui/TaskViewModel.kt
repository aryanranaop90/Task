package com.example.ui

import android.app.Activity
import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ads.UnityAdsManager
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository
    val allTasks: StateFlow<List<Task>>
    val totalPoints: StateFlow<Int>

    // Ad flows directly forward from manager
    val isAdsInitialized = UnityAdsManager.isInitialized
    val isAdLoaded = UnityAdsManager.isAdLoaded
    val adStateMessage = UnityAdsManager.adStatus

    init {
        val database = AppDatabase.getDatabase(application)
        val taskDao = database.taskDao()
        repository = TaskRepository(taskDao)

        allTasks = repository.allTasks
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        totalPoints = repository.totalPoints
            .map { it ?: 0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

        // Init ads automatically on load
        UnityAdsManager.initialize(application)
    }

    fun addTask(title: String, description: String, points: Int, isPremium: Boolean = false) {
        viewModelScope.launch {
            repository.insertTask(
                Task(
                    title = title,
                    description = description,
                    points = points,
                    isPremium = isPremium
                )
            )
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = true))
        }
    }

    fun toggleTaskComplete(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun watchAdForBonusPoints(activity: Activity) {
        viewModelScope.launch {
            if (!UnityAdsManager.isAdLoaded.value) {
                Toast.makeText(getApplication(), "Rewarded Video Ad is loading. Please wait a moment.", Toast.LENGTH_SHORT).show()
                UnityAdsManager.loadRewardedAd()
                return@launch
            }

            UnityAdsManager.showRewardedAd(activity) {
                // Ad completed callback! Grant +50 points by inserting a Completed Ad Reward Task.
                viewModelScope.launch {
                    repository.insertTask(
                        Task(
                            title = "🎬 Unity Video Reward",
                            description = "Acquired bonus reward for watching Unity video ad!",
                            isCompleted = true,
                            points = 50
                        )
                    )
                    Toast.makeText(getApplication(), "Success! Gained +50 Points!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun doubleTaskReward(task: Task, activity: Activity) {
        viewModelScope.launch {
            if (!UnityAdsManager.isAdLoaded.value) {
                Toast.makeText(getApplication(), "Ad is loading. Please wait.", Toast.LENGTH_SHORT).show()
                UnityAdsManager.loadRewardedAd()
                return@launch
            }

            UnityAdsManager.showRewardedAd(activity) {
                // Ad completed! Double task's completions and mark as completed.
                viewModelScope.launch {
                    val doubledPoints = task.points * 2
                    repository.updateTask(
                        task.copy(
                            title = "${task.title} (2x 💎)",
                            isCompleted = true,
                            points = doubledPoints,
                            description = "${task.description} (Doubled from ${task.points} via rewarded video)"
                        )
                    )
                    Toast.makeText(getApplication(), "Double Points Success! Gained +$doubledPoints Points!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun unlockPremiumTask(task: Task, activity: Activity) {
        viewModelScope.launch {
            if (!UnityAdsManager.isAdLoaded.value) {
                Toast.makeText(getApplication(), "Ad is not ready. Please try again in a few seconds.", Toast.LENGTH_SHORT).show()
                UnityAdsManager.loadRewardedAd()
                return@launch
            }

            UnityAdsManager.showRewardedAd(activity) {
                viewModelScope.launch {
                    repository.updateTask(
                        task.copy(
                            isPremium = false,
                            description = "${task.description} (Unlocked via Video Ad!)"
                        )
                    )
                    Toast.makeText(getApplication(), "Task Unlocked Successfully!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun reloadAd() {
        UnityAdsManager.loadRewardedAd()
    }

    // Factory to construct TaskViewModel
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
