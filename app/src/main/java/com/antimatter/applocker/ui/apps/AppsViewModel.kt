package com.antimatter.applocker.ui.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.antimatter.applocker.data.AppItem
import com.antimatter.applocker.data.InstalledAppsRepository
import com.antimatter.applocker.data.ProtectedAppsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppsViewMode { List, Grid }

data class AppsUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    val viewMode: AppsViewMode = AppsViewMode.List,
    val visibleApps: List<AppItem> = emptyList(),
    val totalApps: Int = 0,
    val protectedPackages: Set<String> = emptySet(),
    /** Protected apps that are currently installed. */
    val protectedCount: Int = 0
)

class AppsViewModel(
    private val installed: InstalledAppsRepository,
    private val protectedApps: ProtectedAppsRepository
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val viewMode = MutableStateFlow(AppsViewMode.List)

    // The installed-apps list lives in the repository (already loaded at app start),
    // so opening the tab shows it immediately; "loading" only means "not loaded yet".
    val state: StateFlow<AppsUiState> = combine(
        installed.apps,
        query,
        viewMode,
        protectedApps.packages
    ) { apps, q, mode, protectedSet ->
        val all = apps.orEmpty()
        AppsUiState(
            isLoading = apps == null,
            query = q,
            viewMode = mode,
            visibleApps = filterApps(all, q),
            totalApps = all.size,
            protectedPackages = protectedSet,
            protectedCount = all.count { it.packageName in protectedSet }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    /** Quietly re-reads the installed apps; the current list stays on screen meanwhile. */
    fun refresh() {
        viewModelScope.launch { installed.refresh() }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun setViewMode(mode: AppsViewMode) {
        viewMode.value = mode
    }

    fun setProtected(packageName: String, isProtected: Boolean) {
        viewModelScope.launch { protectedApps.setProtected(packageName, isProtected) }
    }

    companion object {
        fun factory(installed: InstalledAppsRepository, protectedApps: ProtectedAppsRepository) =
            viewModelFactory {
                initializer { AppsViewModel(installed, protectedApps) }
            }
    }
}
