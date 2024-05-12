package org.tasks.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewParent
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.todoroo.astrid.activity.MainActivity
import com.todoroo.astrid.activity.TaskListFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.tasks.LocalBroadcastManager
import org.tasks.R
import org.tasks.Strings.isNullOrEmpty
import org.tasks.compose.drawer.ListSettingsDrawer
import org.tasks.data.LocationDao
import org.tasks.data.Place
import org.tasks.databinding.ActivityLocationSettingsBinding
import org.tasks.extensions.formatNumber
import org.tasks.filters.PlaceFilter
import org.tasks.location.MapFragment
import org.tasks.preferences.Preferences
import org.tasks.themes.CustomIcons
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class PlaceSettingsActivity : BaseListSettingsActivity(), MapFragment.MapFragmentCallback,
    Slider.OnChangeListener {

    companion object {
        const val EXTRA_PLACE = "extra_place"
        private const val MIN_RADIUS = 75
        private const val MAX_RADIUS = 1000
        private const val STEP = 25
    }

    private lateinit var name: TextInputEditText
    private lateinit var nameLayout: TextInputLayout
    private lateinit var slider: Slider

    @Inject lateinit var locationDao: LocationDao
    @Inject lateinit var map: MapFragment
    @Inject lateinit var preferences: Preferences
    @Inject lateinit var locale: Locale
    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    private lateinit var place: Place
    override val defaultIcon: Int = CustomIcons.PLACE

    override val compose: Boolean
        get() = true
    val mapViewReady = mutableStateOf(false)
    val sliderPos = mutableFloatStateOf(100f)
    lateinit var viewHolder: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent?.hasExtra(EXTRA_PLACE) != true) {
            finish()
        }

        val extra: Place? = intent?.getParcelableExtra(EXTRA_PLACE)
        if (extra == null) {
            finish()
            return
        }

        place = extra

        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            textState.value = place.displayName
            //name.setText(place.displayName)
            selectedColor = place.color
            selectedIcon = place.icon
        }

        sliderPos.value = (place.radius / STEP * STEP).toFloat()

        val dark = preferences.mapTheme == 2
                || preferences.mapTheme == 0 && tasksTheme.themeBase.isDarkTheme(this)

        //map.init(this, this, dark)

        setContent {
            ListSettingsDrawer(
                title = toolbarTitle,
                isNew = isNew,
                text = textState,
                error = errorState,
                color = colorState,
                icon = iconState,
                delete = { lifecycleScope.launch { promptDelete() } },
                save = { lifecycleScope.launch { save() } },
                selectColor = { showThemePicker() },
                clearColor = { clearColor() },
                selectIcon = { showIconPicker() }
            ) {
                Row(modifier = Modifier.requiredHeight(56.dp).fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween)
                {
                    Text(stringResource(id = R.string.geofence_radius))
                    Row (horizontalArrangement = Arrangement.End ){
                        Text(getString(
                            R.string.location_radius_meters,
                            locale.formatNumber(sliderPos.value.toInt())
                        ))
                    }

                }
                Slider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(56.dp),
                    value = sliderPos.value,
                    valueRange = (MIN_RADIUS.toFloat() .. MAX_RADIUS.toFloat()),
                    steps = (MAX_RADIUS - MIN_RADIUS) / STEP,
                    onValueChange = { sliderPos.value = it; updateGeofenceCircle() }
                )
/* TODO("delete after debugged")
                setLabelFormatter { value ->
                    getString(
                        R.string.location_radius_meters,
                        locale.formatNumber(value.toInt())
                    )
                }
                valueTo = MAX_RADIUS.toFloat()
                valueFrom = MIN_RADIUS.toFloat()
                stepSize = STEP.toFloat()
                haloRadius = 0
                value = (place.radius / STEP * STEP).roundToInt().toFloat()
*/
                AndroidView(factory = { ctx ->
                    viewHolder = LinearLayout(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                    viewHolder.setBackgroundColor(((((127*256)+127)*256)+127)*256+127)
                    map.init(this@PlaceSettingsActivity, this@PlaceSettingsActivity, dark, viewHolder)
                    viewHolder
                }, update = {
//                    it.setBackgroundColor(((((127*256)+127)*256)+127)*256+127)
                },
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(300.dp)
                        .padding(horizontal = 8.dp)
                )

                /*
                                if (mapViewReady.value) {
                                    val view = LocalView.current
                                    AndroidView(
                                        modifier = Modifier.fillMaxWidth().requiredHeight(550.dp),
                                        factory = {ctx ->
                                            val view = map.getView()
                                            view.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, height)
                                            view.setLayoutParams(view.getLayoutParams())
                                            view
                                        },
                                        update = {view ->
                                            view.layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, height)
                                            view.setLayoutParams(view.getLayoutParams())
                                        }
                                    )
                                }
                */
            }

        }

        updateTheme()
    }

    override fun bind() = TODO("Bind must NOT be called for @Compose'ed activity")
/*
    override fun bind() = ActivityLocationSettingsBinding.inflate(layoutInflater).let {
        name = it.name.apply {
            addTextChangedListener(
                onTextChanged = { _, _, _, _ -> nameLayout.error = null }
            )
        }
        nameLayout = it.nameLayout
        slider = it.slider.apply {
            setLabelFormatter { value ->
                getString(
                    R.string.location_radius_meters,
                    locale.formatNumber(value.toInt())
                )
            }
            valueTo = MAX_RADIUS.toFloat()
            valueFrom = MIN_RADIUS.toFloat()
            stepSize = STEP.toFloat()
            haloRadius = 0
            value = (place.radius / STEP * STEP).roundToInt().toFloat()
        }
        slider.addOnChangeListener(this)
        it.root
    }
*/

    override fun hasChanges() = textState.value != place.displayName
                    || selectedColor != place.color
                    || selectedIcon != place.icon

    override suspend fun save() {
        val newName: String = textState.value

        if (isNullOrEmpty(newName)) {
            errorState.value = getString(R.string.name_cannot_be_empty)
            return
        }

        place = place.copy(
            name = newName,
            color = selectedColor,
            icon = selectedIcon,
            radius = sliderPos.value.toInt(),
        )
        locationDao.update(place)
        setResult(
                Activity.RESULT_OK,
                Intent(TaskListFragment.ACTION_RELOAD)
                        .putExtra(MainActivity.OPEN_FILTER, PlaceFilter(place)))
        finish()
    }

    override val isNew: Boolean
        get() = false

    override val toolbarTitle: String
        get() = place.address ?: place.displayName

    override suspend fun delete() {
        locationDao.deleteGeofencesByPlace(place.uid!!)
        locationDao.delete(place)
        setResult(Activity.RESULT_OK, Intent(TaskListFragment.ACTION_DELETED))
        localBroadcastManager.broadcastRefreshList()
        finish()
    }

    override fun onMapReady(mapFragment: MapFragment) {
        map = mapFragment
        map.setMarkers(listOf(place))
        map.disableGestures()
        map.movePosition(place.mapPosition, false)
        updateGeofenceCircle()
        mapViewReady.value = true
    }

    override fun onPlaceSelected(place: Place) {}
    override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
        updateGeofenceCircle()
        TODO("Delete this")
    }

    private fun updateGeofenceCircle() {
        val radius = sliderPos.value.toDouble()
        val zoom = when (radius) {
            in 0f..300f -> 15f
            in 300f..500f -> 14.5f
            in 500f..700f -> 14.25f
            in 700f..900f -> 14f
            else -> 13.75f
        }
        map.showCircle(radius, place.latitude, place.longitude)
        map.movePosition(
            mapPosition = place.mapPosition.copy(zoom = zoom),
            animate = true,
        )
    }
}