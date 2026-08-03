package com.fastaid.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.location.Address;
import android.location.Geocoder;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.TextUtils;
import android.telecom.TelecomManager;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.switchmaterial.SwitchMaterial;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int REQUEST_LOCATION = 4101;
    private static final String MAP_VIEW_BUNDLE_KEY = "fastaid_google_map_view";
    private static final String PREFS_NAME = "fastaid_offline_state";
    private static final String PROFILE_PREFS_NAME = "fastaid_safety_profile";
    private static final String DEFAULT_PROFILE_NAME = "Add your name";
    private static final String LEGACY_DEFAULT_PROFILE_NAME = "FastAid User";
    private static final String BENGALURU_BROSEPH_NUMBER = "+919113890911";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<AidPlace> places = new ArrayList<>();

    private FastAidApiClient apiClient;
    private GooglePlacesRepository placesRepository;
    private AidPlaceCache placeCache;
    private LinearLayout resultsList;
    private LinearLayout contentRoot;
    private LinearLayout bottomNav;
    private LinearLayout headerRoot;
    private MapView googleMapView;
    private MapPreviewView mapPreviewView;
    private GoogleMap googleMap;
    private Bundle mapViewBundle;
    private LinearLayout mapPlaceSheet;
    private TextView statusText;
    private TextView locationText;
    private TextView nearbyCountText;
    private TextView googlePlacesAttribution;
    private TextView patientCountText;
    private EditText notesInput;
    private ProgressBar loading;
    private Spinner incidentSpinner;
    private View locateButton;
    private Button refreshButton;
    private Button incidentButton;
    private Button cancelSosButton;
    private Button sendNowSosButton;
    private TextView sosCountdownText;
    private CountDownTimer sosCountdownTimer;
    private boolean sosCountdownActive;
    private LocationListener pendingLocationListener;
    private Runnable locationTimeoutRunnable;
    private ActivityResultLauncher<Intent> emergencyContactPicker;
    private ActivityResultLauncher<Intent> speechRecognizer;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean pendingSosAfterLocation;
    private boolean showAllPlaces;
    private String nearbyFilter = "all";
    private String activeTab = "SOS";
    private boolean responderAvailable = true;
    private String responderAlertState = "incoming";
    private String activeIncidentStatus = "None";

    private double currentLatitude = Double.NaN;
    private double currentLongitude = Double.NaN;
    private long currentLocationTimestamp;
    private float currentLocationAccuracy = Float.NaN;
    private String currentLocationSource = "No location";
    private String incidentType = "accident";
    private String selectedCategoryLabel = "Accident";
    private AidPlace selectedMapPlace;
    private int patientCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        emergencyContactPicker = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() == Activity.RESULT_OK && data != null) {
                        persistSelectedEmergencyContact(data.getData());
                    }
                });
        speechRecognizer = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (result.getResultCode() != Activity.RESULT_OK || data == null) return;
                    ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) handleVoiceCommand(matches.get(0));
                });
        mapViewBundle = savedInstanceState == null ? null : savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
        apiClient = new FastAidApiClient(getString(R.string.fastaid_backend_base_url));
        placeCache = new AidPlaceCache(this);
        if (!BuildConfig.PLACES_API_KEY.trim().isEmpty()) {
            try {
                placesRepository = new GooglePlacesRepository(this, BuildConfig.PLACES_API_KEY);
            } catch (RuntimeException ignored) {
                placesRepository = null;
            }
        }
        migrateStaleDemoProfileState();
        updateEmergencyNumberFromDeviceCountry();
        restoreOfflineState();
        getWindow().getDecorView().setLayoutDirection(
                UiTranslations.isRtl(preferredLanguage()) ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        WindowInsetsControllerCompat bars = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(true);
        bars.setAppearanceLightNavigationBars(true);
        setContentView(buildContent());
        updateLocationLabel(hasCurrentCoordinates() ? "Saved location" : "Location needed");
        requestLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (googleMapView != null) {
            googleMapView.onStart();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googleMapView != null) {
            googleMapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        if (googleMapView != null) {
            googleMapView.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (googleMapView != null) {
            googleMapView.onStop();
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (googleMapView == null) {
            return;
        }
        Bundle mapBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY);
        if (mapBundle == null) {
            mapBundle = new Bundle();
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapBundle);
        }
        googleMapView.onSaveInstanceState(mapBundle);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (googleMapView != null) {
            googleMapView.onLowMemory();
        }
    }

    @Override
    protected void onDestroy() {
        cancelSosTimerOnly();
        mainHandler.removeCallbacksAndMessages(null);
        removePendingLocationListener();
        if (googleMapView != null) {
            googleMapView.onDestroy();
        }
        executor.shutdownNow();
        super.onDestroy();
    }

    private View buildContent() {
        FrameLayout shell = new FrameLayout(this);
        shell.setBackgroundColor(color(R.color.fastaid_background));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(color(R.color.fastaid_background));
        FrameLayout.LayoutParams pageParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
        pageParams.setMargins(0, 0, 0, dp(72));
        shell.addView(page, pageParams);

        page.addView(buildHeader());

        ScrollView scrollView = new ScrollView(this);
        contentRoot = new LinearLayout(this);
        contentRoot.setOrientation(LinearLayout.VERTICAL);
        contentRoot.setPadding(dp(16), dp(10), dp(16), dp(28));
        contentRoot.setBackgroundColor(color(R.color.fastaid_background));
        scrollView.addView(contentRoot);
        page.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        populateHomeContent();

        bottomNav = buildBottomNav();
        shell.addView(bottomNav, bottomNavParams());
        ViewCompat.setOnApplyWindowInsetsListener(shell, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            page.setPadding(0, bars.top, 0, 0);
            pageParams.bottomMargin = dp(72) + bars.bottom;
            page.setLayoutParams(pageParams);
            FrameLayout.LayoutParams navParams = (FrameLayout.LayoutParams) bottomNav.getLayoutParams();
            navParams.height = dp(72) + bars.bottom;
            bottomNav.setLayoutParams(navParams);
            bottomNav.setPadding(dp(8), dp(7), dp(8), dp(7) + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(shell);
        return shell;
    }

    private void populateHomeContent() {
        activeTab = "SOS";
        if (contentRoot == null) {
            return;
        }
        refreshHeader();
        contentRoot.removeAllViews();
        contentRoot.addView(buildEmergencyPanel());
        updateLocationLabel(currentLocationSource);
        contentRoot.addView(buildCategoryStrip());
        contentRoot.addView(buildActiveIncidentCard());
        contentRoot.addView(buildNearbyHeader());

        resultsList = new LinearLayout(this);
        resultsList.setOrientation(LinearLayout.VERTICAL);
        contentRoot.addView(resultsList);
        contentRoot.addView(buildGooglePlacesAttribution());
        renderPlaces();
    }

    private void populateMapContent() {
        activeTab = "Map";
        if (contentRoot == null) return;
        refreshHeader();
        contentRoot.removeAllViews();
        contentRoot.addView(buildExpandedMapHero());
        updateLocationLabel(currentLocationSource);
        setUiText(statusText, "Live Google Maps with route-ready aid");
    }

    private void populateNearbyContent() {
        activeTab = "Nearby";
        showAllPlaces = true;
        if (contentRoot == null) return;
        refreshHeader();
        contentRoot.removeAllViews();
        contentRoot.addView(buildNearbyCategoryStrip());
        contentRoot.addView(buildNearbyFilters());
        contentRoot.addView(buildNearbyHeader());
        resultsList = new LinearLayout(this);
        resultsList.setOrientation(LinearLayout.VERTICAL);
        contentRoot.addView(resultsList);
        contentRoot.addView(buildGooglePlacesAttribution());
        renderPlaces();
        setUiText(statusText, "Choose a category, then call or navigate");
    }

    private void populateProfileContent() {
        activeTab = "Profile";
        if (contentRoot == null) {
            return;
        }
        refreshHeader();
        contentRoot.removeAllViews();
        contentRoot.addView(buildProfilePage());
        setUiText(statusText, "Emergency handoff details and app settings");
    }

    private void populateIncidentsContent() {
        activeTab = "Incidents";
        if (contentRoot == null) return;
        refreshHeader();
        contentRoot.removeAllViews();
        contentRoot.addView(buildIncidentsPage());
        setUiText(statusText, "Track requests and responder handoff states");
    }

    private void refreshBottomNav() {
        if (bottomNav == null) {
            return;
        }
        bottomNav.removeAllViews();
        bottomNav.addView(navItem(R.drawable.ic_m3_emergency, "SOS", "SOS".equals(activeTab)), rowWeight());
        bottomNav.addView(navItem(R.drawable.ic_m3_map, "Map", "Map".equals(activeTab)), rowWeight());
        bottomNav.addView(navItem(R.drawable.ic_m3_search, "Nearby", "Nearby".equals(activeTab)), rowWeight());
        bottomNav.addView(navItem(R.drawable.ic_m3_list, "Incidents", "Incidents".equals(activeTab)), rowWeight());
        bottomNav.addView(navItem(R.drawable.ic_m3_person, "Profile", "Profile".equals(activeTab)), rowWeight());
    }

    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.WHITE);
        headerRoot = header;
        refreshHeader();
        return header;
    }

    private void refreshHeader() {
        if (headerRoot == null) {
            return;
        }
        headerRoot.removeAllViews();
        boolean sosPage = "SOS".equals(activeTab);
        headerRoot.setPadding(dp(16), dp(14), dp(16), sosPage ? dp(8) : dp(12));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(sosPage ? Gravity.CENTER : Gravity.START);

        if (sosPage) {
            LinearLayout logo = new LinearLayout(this);
            logo.setGravity(Gravity.CENTER);
            logo.setOrientation(LinearLayout.HORIZONTAL);
            TextView fast = text("Fast", 25, R.color.fastaid_red, true);
            TextView aid = text("Aid", 25, R.color.fastaid_blue, true);
            logo.addView(fast);
            logo.addView(aid);
            toolbar.addView(logo, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            TextView title = text(pageTitle(), 24, R.color.fastaid_ink, true);
            title.setGravity(Gravity.START);
            toolbar.addView(title, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        headerRoot.addView(toolbar);

        statusText = text("Requesting current location", 12, R.color.fastaid_muted, false);
        statusText.setGravity(sosPage ? Gravity.CENTER : Gravity.START);
        statusText.setPadding(0, sosPage ? 0 : dp(2), 0, 0);
        ViewCompat.setAccessibilityLiveRegion(statusText, ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        headerRoot.addView(statusText);
    }

    private String pageTitle() {
        if ("Map".equals(activeTab)) return "Map";
        if ("Nearby".equals(activeTab)) return "Nearby Aid";
        if ("Incidents".equals(activeTab)) return "Incidents";
        if ("Profile".equals(activeTab)) return "Safety profile";
        return "FastAid";
    }

    private View buildMapHero() {
        FrameLayout hero = new FrameLayout(this);
        hero.setBackgroundColor(Color.WHITE);
        boolean mapMode = "Map".equals(activeTab);

        if (BuildConfig.MAPS_KEY_CONFIGURED) {
            googleMapView = new MapView(this);
            googleMapView.onCreate(mapViewBundle);
            googleMapView.getMapAsync(this);
            hero.addView(googleMapView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
        } else {
            mapPreviewView = new MapPreviewView(this);
            mapPreviewView.setPlaces(places, currentLatitude, currentLongitude);
            hero.addView(mapPreviewView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            TextView keyNotice = text("Add Maps key for live Google Maps", 12, R.color.fastaid_ink, true);
            keyNotice.setGravity(Gravity.CENTER);
            keyNotice.setPadding(dp(12), dp(8), dp(12), dp(8));
            keyNotice.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_soft_blue), 18));
            keyNotice.setElevation(dp(4));
            FrameLayout.LayoutParams noticeParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP | Gravity.CENTER_HORIZONTAL
            );
            noticeParams.setMargins(dp(14), dp(16), dp(14), 0);
            hero.addView(keyNotice, noticeParams);
        }

        locationText = text("GPS pending", 12, R.color.fastaid_ink, true);
        locationText.setPadding(dp(12), dp(8), dp(12), dp(8));
        locationText.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_soft_blue), 18));
        locationText.setElevation(dp(4));
        FrameLayout.LayoutParams locationParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START
        );
        locationParams.setMargins(dp(14), dp(14), dp(86), 0);
        hero.addView(locationText, locationParams);
        LinearLayout actionRail = new LinearLayout(this);
        actionRail.setOrientation(LinearLayout.VERTICAL);
        actionRail.setGravity(Gravity.CENTER);

        if (!mapMode) {
            TextView sosButton = circularTextMapButton("SOS", color(R.color.fastaid_red), Color.WHITE, "Send SOS");
            sosButton.setOnClickListener(view -> startSosCountdown());
            actionRail.addView(sosButton, circleRailParams(0));
        }

        ImageButton currentLocation = circularMapButton(R.drawable.ic_m3_my_location, color(R.color.fastaid_blue), "Use current location");
        locateButton = currentLocation;
        currentLocation.setOnClickListener(view -> requestLocation());
        actionRail.addView(currentLocation, circleRailParams(mapMode ? 0 : dp(10)));

        if (!mapMode) {
            ImageButton emergencyCall = circularMapButton(
                    R.drawable.ic_m3_call,
                    color(R.color.fastaid_red),
                    "Call emergency number " + emergencyNumber());
            emergencyCall.setOnClickListener(view -> callEmergency());
            actionRail.addView(emergencyCall, circleRailParams(dp(10)));
        }

        ImageButton share = circularMapButton(R.drawable.ic_m3_share, color(R.color.fastaid_ink), "Share location");
        share.setOnClickListener(view -> shareCurrentLocation());
        actionRail.addView(share, circleRailParams(dp(10)));

        FrameLayout.LayoutParams railParams = new FrameLayout.LayoutParams(
                dp(58),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.END | (mapMode ? Gravity.TOP : Gravity.BOTTOM)
        );
        railParams.setMargins(0, mapMode ? dp(112) : 0, dp(14), mapMode ? 0 : dp(124));
        hero.addView(actionRail, railParams);

        if (!mapMode) {
            mapPlaceSheet = buildMapPlaceSheetView();
            FrameLayout.LayoutParams sheetParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
            );
            sheetParams.setMargins(dp(16), 0, dp(16), dp(18));
            hero.addView(mapPlaceSheet, sheetParams);
            updateMapPlaceSheet();
        }

        sosCountdownText = text("Tap SOS for a 5 sec cancellable emergency alert", 12, R.color.fastaid_red, true);
        sosCountdownText.setGravity(Gravity.CENTER);
        sosCountdownText.setVisibility(mapMode ? View.GONE : View.VISIBLE);
        FrameLayout.LayoutParams sosParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        sosParams.setMargins(dp(8), 0, dp(8), dp(104));
        hero.addView(sosCountdownText, sosParams);

        cancelSosButton = whiteButton("Cancel SOS");
        cancelSosButton.setVisibility(View.GONE);
        cancelSosButton.setOnClickListener(view -> cancelSosCountdown());
        FrameLayout.LayoutParams cancelParams = new FrameLayout.LayoutParams(dp(150), dp(48), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        cancelParams.setMargins(0, 0, 0, dp(106));
        hero.addView(cancelSosButton, cancelParams);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(330)
        );
        params.setMargins(0, 0, 0, dp(12));
        hero.setLayoutParams(params);
        return hero;
    }
    private View buildExpandedMapHero() {
        LinearLayout mapScreen = new LinearLayout(this);
        mapScreen.setOrientation(LinearLayout.VERTICAL);
        mapScreen.setPadding(0, 0, 0, 0);

        View hero = buildMapHero();
        mapScreen.addView(hero, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                4));

        mapPlaceSheet = buildMapPlaceSheetView();
        LinearLayout.LayoutParams sheetParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1);
        sheetParams.setMargins(0, dp(10), 0, 0);
        mapScreen.addView(mapPlaceSheet, sheetParams);
        updateMapPlaceSheet();

        mapScreen.post(() -> {
            View viewport = contentRoot == null ? null : (View) contentRoot.getParent();
            if (viewport == null || viewport.getHeight() <= 0) return;
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    Math.round(viewport.getHeight() * 0.90f));
            params.setMargins(0, 0, 0, dp(12));
            mapScreen.setLayoutParams(params);
        });
        return mapScreen;
    }

    private LinearLayout buildMapPlaceSheetView() {
        LinearLayout sheet = new LinearLayout(this);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setGravity(Gravity.CENTER_VERTICAL);
        sheet.setPadding(dp(12), dp(10), dp(12), dp(10));
        sheet.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_soft_blue), 18));
        sheet.setElevation(dp(6));
        return sheet;
    }
    private LinearLayout.LayoutParams circleRailParams(int topMarginPx) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(54), dp(54));
        params.setMargins(0, topMarginPx, 0, 0);
        return params;
    }

    private TextView circularTextMapButton(String label, int backgroundColor, int textColor, String description) {
        TextView button = new TextView(this);
        button.setText(label);
        button.setTextColor(textColor);
        button.setTextSize(15);
        button.setTypeface(null, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setBackground(pill(backgroundColor, backgroundColor, 27));
        button.setContentDescription(description);
        button.setElevation(dp(5));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }
    private ImageButton circularMapButton(int iconResource, int tintColor, String description) {
        ImageButton button = new ImageButton(this);
        button.setImageResource(iconResource);
        button.setColorFilter(tintColor);
        button.setBackground(pill(Color.WHITE, color(R.color.fastaid_soft_blue), 27));
        button.setPadding(dp(14), dp(14), dp(14), dp(14));
        button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        button.setContentDescription(description);
        button.setElevation(dp(5));
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private void updateMapPlaceSheet() {
        if (mapPlaceSheet == null) return;
        mapPlaceSheet.removeAllViews();
        AidPlace place = selectedMapPlace;
        if (place == null || !placesContain(place)) {
            place = firstMappablePlace();
            selectedMapPlace = place;
        }

        if (place == null) {
            TextView title = text("Map context", 15, R.color.fastaid_ink, true);
            mapPlaceSheet.addView(title);
            TextView copy = text("Use current location to load live nearby aid markers.", 12,
                    R.color.fastaid_muted, false);
            copy.setPadding(0, dp(3), 0, 0);
            mapPlaceSheet.addView(copy);
            mapPlaceSheet.setContentDescription("Map context. No nearby aid marker selected.");
            return;
        }

        final AidPlace selectedPlace = place;
        String openText = selectedPlace.openText == null || selectedPlace.openText.isEmpty()
                ? "Hours unavailable" : selectedPlace.openText;

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(iconBubble(placeIconResource(selectedPlace), placeColor(selectedPlace), 40, 10),
                new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(10), 0, dp(8), 0);
        TextView name = text(selectedPlace.name, 14, R.color.fastaid_ink, true);
        name.setMaxLines(2);
        copy.addView(name);
        String quality = ServiceQualityScanner.label(incidentType, selectedPlace);
        TextView detail = text(selectedPlace.distance + " - " + openText + " - " + quality,
                11, R.color.fastaid_muted, false);
        detail.setMaxLines(2);
        copy.addView(detail);
        top.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ImageView call = actionIconButton(
                R.drawable.ic_m3_call,
                selectedPlace.hasPhone() ? color(R.color.fastaid_ink) : color(R.color.fastaid_muted),
                Color.WHITE,
                selectedPlace.hasPhone() ? "Call " + selectedPlace.name : "No phone number for " + selectedPlace.name);
        call.setEnabled(selectedPlace.hasPhone());
        call.setAlpha(selectedPlace.hasPhone() ? 1f : 0.45f);
        call.setOnClickListener(view -> callPlace(selectedPlace));
        top.addView(call, new LinearLayout.LayoutParams(dp(44), dp(44)));

        ImageView go = actionIconButton(R.drawable.ic_m3_navigation, color(R.color.fastaid_ink),
                Color.WHITE, "Open directions to " + selectedPlace.name);
        go.setOnClickListener(view -> navigateToPlace(selectedPlace));
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(dp(44), dp(44));
        goParams.setMargins(dp(6), 0, 0, 0);
        top.addView(go, goParams);
        mapPlaceSheet.addView(top);

        TextView reason = text(ServiceQualityScanner.reason(incidentType, selectedPlace),
                11, R.color.fastaid_muted, false);
        reason.setPadding(dp(50), dp(4), 0, 0);
        mapPlaceSheet.addView(reason);
        mapPlaceSheet.setContentDescription(selectedPlace.name + ". " + selectedPlace.distance + ". "
                + openText + ". " + quality + ". " + ServiceQualityScanner.reason(incidentType, selectedPlace));
    }

    private AidPlace firstMappablePlace() {
        for (AidPlace place : visiblePlaces()) {
            if (place.hasCoordinates()) return place;
        }
        return null;
    }

    private boolean placesContain(AidPlace selected) {
        for (AidPlace place : places) {
            if (samePlace(place, selected)) return true;
        }
        return false;
    }

    private boolean samePlace(AidPlace first, AidPlace second) {
        if (first == null || second == null) return false;
        if (first.id != null && second.id != null && first.id.equals(second.id)) return true;
        return first.name.equals(second.name)
                && Math.abs(first.latitude - second.latitude) < 0.00001
                && Math.abs(first.longitude - second.longitude) < 0.00001;
    }
    private View buildEmergencyPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(16), dp(16), dp(16), dp(16));
        panel.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_soft_blue), 20));
        panel.setElevation(dp(2));

        TextView eyebrow = text("EMERGENCY ASSISTANCE", 11, R.color.fastaid_red, true);
        eyebrow.setBackground(pill(
                color(R.color.fastaid_nav_indicator),
                color(R.color.fastaid_nav_indicator),
                10));
        eyebrow.setPadding(dp(9), dp(4), dp(9), dp(4));
        panel.addView(eyebrow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView emergencyText = text("Call official help first", 22, R.color.fastaid_ink, true);
        emergencyText.setGravity(Gravity.CENTER);
        emergencyText.setPadding(0, dp(10), 0, dp(3));
        panel.addView(emergencyText);

        EmergencyHelplineRegistry.Profile helplineProfile =
                EmergencyHelplineRegistry.profileFor(detectedCountryCode());
        TextView supportText = text(
                emergencySupportCopy(helplineProfile),
                13,
                R.color.fastaid_muted,
                false);
        supportText.setGravity(Gravity.CENTER);
        panel.addView(supportText);

        panel.addView(buildCountryEmergencyContacts(helplineProfile));

        TextView toolsLabel = text("FastAid tools", 12, R.color.fastaid_muted, true);
        toolsLabel.setGravity(Gravity.CENTER);
        toolsLabel.setPadding(0, dp(14), 0, 0);
        panel.addView(toolsLabel);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.setPadding(0, dp(10), 0, dp(12));

        TextView sosButton = circularTextMapButton(
                "SOS", color(R.color.fastaid_red), Color.WHITE, "Send SOS");
        sosButton.setOnClickListener(view -> startSosCountdown());
        actions.addView(emergencyActionItem(sosButton, "SOS"), rowWeight());

        ImageButton currentLocation = circularMapButton(
                R.drawable.ic_m3_my_location,
                color(R.color.fastaid_blue),
                "Use current location");
        locateButton = currentLocation;
        currentLocation.setOnClickListener(view -> requestLocation());
        actions.addView(emergencyActionItem(currentLocation, "Locate"), rowWeight());

        ImageButton share = circularMapButton(
                R.drawable.ic_m3_share,
                color(R.color.fastaid_ink),
                "Share location");
        share.setOnClickListener(view -> shareCurrentLocation());
        actions.addView(emergencyActionItem(share, "Share"), rowWeight());

        if (isBangaloreLocation()) {
            TextView broseph = circularTextMapButton(
                    "B",
                    color(R.color.fastaid_blue),
                    Color.WHITE,
                    "Call St Broseph Bengaluru community support");
            broseph.setOnClickListener(view -> callBrosephBengaluru());
            actions.addView(emergencyActionItem(broseph, "Broseph"), rowWeight());
        }

        panel.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        locationText = text("GPS pending", 12, R.color.fastaid_ink, true);
        locationText.setGravity(Gravity.CENTER);
        locationText.setPadding(dp(12), dp(10), dp(12), dp(10));
        locationText.setBackground(cardBackground(
                color(R.color.fastaid_soft_blue),
                color(R.color.fastaid_soft_blue),
                14));
        panel.addView(locationText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout assistActions = new LinearLayout(this);
        assistActions.setOrientation(LinearLayout.HORIZONTAL);
        assistActions.setPadding(0, dp(10), 0, 0);
        assistActions.addView(profileActionButton(
                "Enter location", R.drawable.ic_m3_edit,
                color(R.color.fastaid_soft_blue), color(R.color.fastaid_blue), false,
                view -> showManualLocationDialog()), rowWeight());
        assistActions.addView(profileActionButton(
                "Voice", R.drawable.ic_m3_mic,
                Color.WHITE, color(R.color.fastaid_ink), true,
                view -> startVoiceCommand()), rowWeight());
        panel.addView(assistActions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, controlSize()));

        sosCountdownText = text(
                "Tap SOS for a 5 sec cancellable emergency alert",
                12,
                R.color.fastaid_red,
                true);
        sosCountdownText.setGravity(Gravity.CENTER);
        sosCountdownText.setPadding(0, dp(10), 0, 0);
        ViewCompat.setAccessibilityLiveRegion(
                sosCountdownText, ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE);
        panel.addView(sosCountdownText);

        LinearLayout countdownActions = new LinearLayout(this);
        countdownActions.setOrientation(LinearLayout.HORIZONTAL);
        countdownActions.setPadding(0, dp(10), 0, 0);
        cancelSosButton = whiteButton("Cancel SOS");
        cancelSosButton.setVisibility(View.GONE);
        cancelSosButton.setOnClickListener(view -> cancelSosCountdown());
        countdownActions.addView(cancelSosButton, rowWeight());
        sendNowSosButton = primaryButton("Send now");
        sendNowSosButton.setVisibility(View.GONE);
        sendNowSosButton.setOnClickListener(view -> sendSosNow());
        countdownActions.addView(sendNowSosButton, rowWeight());
        panel.addView(countdownActions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, controlSize()));

        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        panelParams.setMargins(0, dp(4), 0, dp(12));
        panel.setLayoutParams(panelParams);
        return panel;
    }

    private String emergencySupportCopy(EmergencyHelplineRegistry.Profile profile) {
        String country = profile.countryName == null || profile.countryName.trim().isEmpty()
                ? "your current country" : profile.countryName;
        if (profile.fallback) {
            return "Using a universal fallback for " + country
                    + ". Verify local emergency numbers when possible.";
        }
        if (profile.specialized.isEmpty() && profile.core.isEmpty()) {
            return "Emergency call shortcut detected for " + country
                    + ". Nearby Aid remains available for local services.";
        }
        return "Emergency lines detected for " + country
                + ": official help first, then nearby aid for local support.";
    }

    private View buildCountryEmergencyContacts(EmergencyHelplineRegistry.Profile profile) {
        LinearLayout contacts = new LinearLayout(this);
        contacts.setOrientation(LinearLayout.VERTICAL);
        contacts.setPadding(0, dp(14), 0, 0);

        EmergencyHelplineRegistry.Entry primary = profile.primary;
        MaterialButton unified = emergencyCallButton(
                primary.number + " - " + primary.label,
                primary.description,
                helplineIcon(primary.kind),
                color(R.color.fastaid_red),
                Color.WHITE,
                false,
                primary.number);
        contacts.addView(unified, emergencyCallParams(0, true));

        if (!profile.core.isEmpty()) {
            contacts.addView(buildHelplineRows(profile.core, false, dp(8)));
        }

        if (!profile.specialized.isEmpty()) {
            TextView specialized = text("Specialized helplines", 12, R.color.fastaid_muted, true);
            specialized.setPadding(dp(2), dp(12), 0, dp(6));
            contacts.addView(specialized);
            contacts.addView(buildHelplineRows(profile.specialized, true, 0));
        }

        return contacts;
    }

    private View buildHelplineRows(
            List<EmergencyHelplineRegistry.Entry> entries,
            boolean outlined,
            int topPadding
    ) {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setPadding(0, topPadding, 0, 0);
        for (int index = 0; index < entries.size(); index += 3) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            if (index > 0) row.setPadding(0, dp(8), 0, 0);
            int end = Math.min(entries.size(), index + 3);
            for (int itemIndex = index; itemIndex < end; itemIndex++) {
                EmergencyHelplineRegistry.Entry entry = entries.get(itemIndex);
                row.addView(emergencyCallButton(
                        entry.number + "\n" + entry.label,
                        entry.description,
                        helplineIcon(entry.kind),
                        outlined ? Color.WHITE : helplineColor(entry.kind),
                        outlined ? helplineColor(entry.kind) : Color.WHITE,
                        outlined,
                        entry.number), rowWeight());
            }
            for (int empty = end; empty < index + 3; empty++) {
                Space spacer = new Space(this);
                row.addView(spacer, rowWeight());
            }
            rows.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        return rows;
    }

    private int helplineIcon(String kind) {
        if ("police".equals(kind)) return R.drawable.ic_aid_police;
        if ("fire".equals(kind)) return R.drawable.ic_aid_fire;
        if ("medical".equals(kind)) return R.drawable.ic_aid_medical;
        if ("cyber".equals(kind)) return R.drawable.ic_m3_search;
        if ("accessibility".equals(kind)) return R.drawable.ic_m3_emergency;
        return R.drawable.ic_m3_emergency;
    }

    private int helplineColor(String kind) {
        if ("police".equals(kind)) return color(R.color.fastaid_blue);
        if ("medical".equals(kind)) return color(R.color.fastaid_green);
        if ("fire".equals(kind) || "women".equals(kind) || "child".equals(kind)) {
            return color(R.color.fastaid_red);
        }
        return color(R.color.fastaid_ink);
    }

    private MaterialButton emergencyCallButton(
            String label,
            String description,
            int iconResource,
            int fillColor,
            int textColor,
            boolean outlined,
            String phoneNumber
    ) {
        MaterialButton button = materialButton(label, fillColor, textColor, outlined, outlined ? 10 : 12);
        button.setIconResource(iconResource);
        button.setIconTint(ColorStateList.valueOf(textColor));
        button.setIconPadding(dp(outlined ? 4 : 8));
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setGravity(Gravity.CENTER);
        button.setSingleLine(false);
        button.setMaxLines(2);
        button.setContentDescription("Call " + label + ". " + description);
        button.setOnClickListener(view -> callDirectEmergency(phoneNumber, description));
        return button;
    }

    private LinearLayout.LayoutParams emergencyCallParams(int topMargin, boolean fullWidth) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                fullWidth ? LinearLayout.LayoutParams.MATCH_PARENT : 0,
                dp(52),
                fullWidth ? 0 : 1);
        params.setMargins(dp(fullWidth ? 4 : 0), topMargin, dp(fullWidth ? 4 : 0), 0);
        return params;
    }

    private LinearLayout emergencyActionItem(View circle, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.addView(circle, new LinearLayout.LayoutParams(dp(54), dp(54)));

        TextView caption = text(label, 11, R.color.fastaid_ink, true);
        caption.setGravity(Gravity.CENTER);
        caption.setPadding(0, dp(7), 0, 0);
        item.addView(caption);
        return item;
    }

    private View buildNearbyCategoryStrip() {
        return buildCategoryStrip(true);
    }

    private View buildCategoryStrip() {
        return buildCategoryStrip(false);
    }

    private View buildCategoryStrip(boolean expanded) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setPadding(0, dp(8), 0, expanded ? dp(112) : dp(8));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(expanded ? "Choose aid type" : "Nearby Aid", 16, R.color.fastaid_ink, true);
        heading.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView seeAll = text("See all", 12, R.color.fastaid_blue, true);
        seeAll.setPadding(dp(12), dp(8), 0, dp(8));
        seeAll.setMinimumHeight(dp(48));
        seeAll.setGravity(Gravity.CENTER_VERTICAL);
        seeAll.setContentDescription(ui("See all") + " nearby aid categories");
        seeAll.setVisibility(expanded ? View.GONE : View.VISIBLE);
        seeAll.setOnClickListener(view -> {
            populateNearbyContent();
            refreshBottomNav();
            fetchNearbyAid();
        });
        heading.addView(seeAll);
        section.addView(heading);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.CENTER);
        chips.setPadding(0, dp(10), 0, 0);
        chips.addView(categoryChip("Accident", R.drawable.ic_aid_accident, R.color.fastaid_red), rowWeight());
        chips.addView(categoryChip("Breakdown", R.drawable.ic_aid_breakdown, R.color.fastaid_orange), rowWeight());
        chips.addView(categoryChip("Fuel", R.drawable.ic_aid_fuel, R.color.fastaid_green), rowWeight());
        chips.addView(categoryChip("Medical", R.drawable.ic_aid_medical, R.color.fastaid_blue), rowWeight());
        section.addView(chips);

        LinearLayout chips2 = new LinearLayout(this);
        chips2.setOrientation(LinearLayout.HORIZONTAL);
        chips2.setGravity(Gravity.CENTER);
        chips2.setPadding(0, dp(12), 0, 0);
        chips2.addView(categoryChip("Police", R.drawable.ic_aid_police, R.color.fastaid_blue), rowWeight());
        chips2.addView(categoryChip("Fire", R.drawable.ic_aid_fire, R.color.fastaid_red), rowWeight());
        chips2.addView(categoryChip("Repair", R.drawable.ic_aid_repair, R.color.fastaid_green), rowWeight());
        chips2.addView(categoryChip("EV", R.drawable.ic_aid_ev, R.color.fastaid_green), rowWeight());
        section.addView(chips2);

        if (expanded) {
            LinearLayout chips3 = new LinearLayout(this);
            chips3.setOrientation(LinearLayout.HORIZONTAL);
            chips3.setGravity(Gravity.CENTER);
            chips3.setPadding(0, dp(12), 0, 0);
            chips3.addView(categoryChip("Tyres", R.drawable.ic_m3_tire_repair, R.color.fastaid_green), rowWeight());
            chips3.addView(categoryChip("Clinic", R.drawable.ic_m3_medical_services, R.color.fastaid_red), rowWeight());
            chips3.addView(categoryChip("Pharmacy", R.drawable.ic_m3_pharmacy, R.color.fastaid_red), rowWeight());
            chips3.addView(categoryChip("Toilets", R.drawable.ic_m3_wc, R.color.fastaid_blue), rowWeight());
            section.addView(chips3);

            LinearLayout chips4 = new LinearLayout(this);
            chips4.setOrientation(LinearLayout.HORIZONTAL);
            chips4.setGravity(Gravity.CENTER);
            chips4.setPadding(0, dp(12), 0, 0);
            chips4.addView(categoryChip("Rest stop", R.drawable.ic_m3_rest_stop, R.color.fastaid_orange), rowWeight());
            chips4.addView(categoryChip("Parking", R.drawable.ic_m3_local_parking, R.color.fastaid_blue), rowWeight());
            chips4.addView(categoryChip("Medical lab", R.drawable.ic_m3_medical_services, R.color.fastaid_red), rowWeight());
            chips4.addView(categoryChip("Auto parts", R.drawable.ic_m3_workshop, R.color.fastaid_green), rowWeight());
            section.addView(chips4);

            LinearLayout chips5 = new LinearLayout(this);
            chips5.setOrientation(LinearLayout.HORIZONTAL);
            chips5.setGravity(Gravity.CENTER);
            chips5.setPadding(0, dp(12), 0, 0);
            chips5.addView(categoryChip("Towing", R.drawable.ic_m3_towing, R.color.fastaid_orange), rowWeight());
            chips5.addView(categoryChip("Battery", R.drawable.ic_m3_battery_service, R.color.fastaid_orange), rowWeight());
            chips5.addView(categoryChip("Food", R.drawable.ic_m3_restaurant, R.color.fastaid_green), rowWeight());
            chips5.addView(categoryChip("Lodging", R.drawable.ic_m3_hotel, R.color.fastaid_blue), rowWeight());
            section.addView(chips5);

            LinearLayout chips6 = new LinearLayout(this);
            chips6.setOrientation(LinearLayout.HORIZONTAL);
            chips6.setGravity(Gravity.CENTER);
            chips6.setPadding(0, dp(12), 0, 0);
            chips6.addView(categoryChip("Car wash", R.drawable.ic_m3_car_wash, R.color.fastaid_blue), rowWeight());
            chips6.addView(categoryChip("E-bike", R.drawable.ic_aid_ev, R.color.fastaid_green), rowWeight());
            chips6.addView(categoryChip("ATM", R.drawable.ic_m3_atm, R.color.fastaid_blue), rowWeight());
            chips6.addView(categoryChip("NGO", R.drawable.ic_m3_volunteer, R.color.fastaid_blue), rowWeight());
            section.addView(chips6);

            LinearLayout chips7 = new LinearLayout(this);
            chips7.setOrientation(LinearLayout.HORIZONTAL);
            chips7.setGravity(Gravity.CENTER);
            chips7.setPadding(0, dp(12), 0, 0);
            chips7.addView(categoryChip("Workshop", R.drawable.ic_m3_workshop, R.color.fastaid_green), rowWeight());
            chips7.addView(spacer(), rowWeight());
            chips7.addView(spacer(), rowWeight());
            chips7.addView(spacer(), rowWeight());
            section.addView(chips7);
        }
        return section;
    }

    private Space spacer() {
        Space space = new Space(this);
        space.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return space;
    }

    private LinearLayout categoryChip(String label, int iconResource, int colorResource) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(6), dp(7), dp(6), dp(7));
        chip.setMinimumHeight(Math.max(dp(78), controlSize()));
        boolean selected = label.equals(selectedCategoryLabel);
        chip.setBackground(cardBackground(
                selected ? color(R.color.fastaid_soft_blue) : Color.WHITE,
                selected ? color(colorResource) : color(R.color.fastaid_outline), 18));
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setSelected(selected);
        chip.setContentDescription(ui(label) + " nearby aid category. "
                + (selected ? "Selected. " : "Not selected. ")
                + "Double tap to search " + ui(label) + " nearby.");
        View.OnClickListener categoryClick = view -> selectNearbyCategory(label);
        chip.setOnClickListener(categoryClick);

        ImageView iconView = iconBubble(iconResource, color(colorResource), 42, 10);
        iconView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        iconView.setClickable(false);
        iconView.setFocusable(false);
        chip.addView(iconView, new LinearLayout.LayoutParams(dp(42), dp(42)));

        TextView text = text(label, 11, R.color.fastaid_ink, true);
        text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        text.setClickable(false);
        text.setFocusable(false);
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, dp(6), 0, 0);
        text.setMaxLines(2);
        chip.addView(text);
        return chip;
    }

    private void selectNearbyCategory(String label) {
        selectedCategoryLabel = label;
        incidentType = incidentTypeForLabel(label);
        setUiText(statusText, "Showing " + label.toLowerCase(Locale.US) + " aid nearby");
        if ("Nearby".equals(activeTab)) {
            populateNearbyContent();
        } else {
            populateHomeContent();
        }
        refreshBottomNav();
        fetchNearbyAid();
    }

    private LinearLayout buildActiveIncidentCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_soft_blue), 14));
        card.setElevation(dp(2));

        if (!hasActiveIncident()) {
            TextView badge = text("NO ACTIVE REQUEST", 11, R.color.fastaid_muted, true);
            badge.setBackground(pill(color(R.color.fastaid_surface_container),
                    color(R.color.fastaid_surface_container), 8));
            badge.setPadding(dp(8), dp(3), dp(8), dp(3));
            card.addView(badge, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            TextView title = text("No FastAid request is active", 14, R.color.fastaid_ink, true);
            title.setPadding(0, dp(8), 0, 0);
            card.addView(title);
            card.addView(text("SOS starts only after a confirmed location. Call "
                    + emergencyNumber() + " for official emergency service.",
                    12, R.color.fastaid_muted, false));
        } else {
            TextView badge = text("INCIDENT STATUS", 11, R.color.fastaid_blue, true);
            badge.setBackground(pill(color(R.color.fastaid_soft_blue),
                    color(R.color.fastaid_soft_blue), 8));
            badge.setPadding(dp(8), dp(3), dp(8), dp(3));
            card.addView(badge, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, 0);
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            left.addView(text(incidentDisplayName() + " - " + patientCount + " people",
                    14, R.color.fastaid_ink, true));
            left.addView(text(coordinateSummary(), 12, R.color.fastaid_muted, false));
            row.addView(left, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            LinearLayout right = new LinearLayout(this);
            right.setOrientation(LinearLayout.VERTICAL);
            right.setGravity(Gravity.END);
            TextView status = text(activeIncidentStatus, 13, incidentStatusColor(), true);
            status.setGravity(Gravity.END);
            right.addView(status);
            TextView details = text(incidentProgressCopy(), 12, R.color.fastaid_muted, false);
            details.setGravity(Gravity.END);
            details.setMaxLines(2);
            right.addView(details);
            row.addView(right, new LinearLayout.LayoutParams(
                    dp(152), LinearLayout.LayoutParams.WRAP_CONTENT));
            card.addView(row);
        }
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(8));
        card.setLayoutParams(params);
        return card;
    }

    private View buildIncidentPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(0, dp(4), 0, dp(12));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        locateButton = pinkButton("Use GPS");
        locateButton.setOnClickListener(view -> requestLocation());
        row.addView(locateButton, rowWeight());

        refreshButton = pinkButton("Refresh Aid");
        refreshButton.setOnClickListener(view -> fetchNearbyAid());
        row.addView(refreshButton, rowWeight());
        panel.addView(row);

        TextView typeLabel = text("Incident Type", 12, R.color.fastaid_red, true);
        typeLabel.setPadding(dp(4), dp(10), 0, dp(4));
        panel.addView(typeLabel);

        incidentSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"accident", "medical", "breakdown", "fuel", "police", "fire"}
        );
        incidentSpinner.setAdapter(adapter);
        incidentSpinner.setBackgroundColor(color(R.color.fastaid_card));
        incidentSpinner.setPadding(dp(10), 0, dp(10), 0);
        incidentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                incidentType = parent.getItemAtPosition(position).toString();
                selectedCategoryLabel = categoryLabelForIncidentType(incidentType);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                incidentType = "accident";
                selectedCategoryLabel = "Accident";
            }
        });
        panel.addView(incidentSpinner, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        ));

        panel.addView(buildPatientCountCard());
        panel.addView(buildNotesField());

        incidentButton = primaryButton("Create API Incident");
        incidentButton.setOnClickListener(view -> createIncident());
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
        );
        sendParams.setMargins(0, dp(10), 0, 0);
        panel.addView(incidentButton, sendParams);

        loading = new ProgressBar(this);
        loading.setVisibility(View.GONE);
        panel.addView(loading);
        return panel;
    }

    private View buildNearbyFilters() {
        ChipGroup group = new ChipGroup(this);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);
        group.setChipSpacingHorizontal(dp(8));
        group.setPadding(0, dp(4), 0, dp(6));
        group.addView(filterChip("All", "all"));
        group.addView(filterChip("Open now", "open"));
        group.addView(filterChip("Callable", "callable"));
        return group;
    }

    private Chip filterChip(String label, String value) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setCheckable(true);
        chip.setChecked(value.equals(nearbyFilter));
        chip.setEnsureMinTouchTargetSize(true);
        chip.setOnClickListener(view -> {
            nearbyFilter = value;
            renderPlaces();
        });
        return chip;
    }

    private TextView buildGooglePlacesAttribution() {
        googlePlacesAttribution = text("Place data by Google", 11, R.color.fastaid_muted, false);
        googlePlacesAttribution.setGravity(Gravity.END);
        googlePlacesAttribution.setPadding(0, dp(8), 0, dp(4));
        googlePlacesAttribution.setContentDescription("Place data provided by Google");
        googlePlacesAttribution.setVisibility(View.GONE);
        return googlePlacesAttribution;
    }

    private LinearLayout buildNearbyHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, dp(8), 0, dp(4));

        TextView title = text("Nearby".equals(activeTab) ? "Results" : "Nearby Aid", 18, R.color.fastaid_red, true);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        nearbyCountText = text("Loading", 12, R.color.fastaid_muted, true);
        nearbyCountText.setGravity(Gravity.END);
        header.addView(nearbyCountText);
        return header;
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(7), dp(8), dp(7));
        nav.setBackgroundColor(Color.WHITE);
        nav.setElevation(dp(10));

        nav.addView(navItem(R.drawable.ic_m3_emergency, "SOS", "SOS".equals(activeTab)), rowWeight());
        nav.addView(navItem(R.drawable.ic_m3_map, "Map", "Map".equals(activeTab)), rowWeight());
        nav.addView(navItem(R.drawable.ic_m3_search, "Nearby", "Nearby".equals(activeTab)), rowWeight());
        nav.addView(navItem(R.drawable.ic_m3_list, "Incidents", "Incidents".equals(activeTab)), rowWeight());
        nav.addView(navItem(R.drawable.ic_m3_person, "Profile", "Profile".equals(activeTab)), rowWeight());
        return nav;
    }

    private LinearLayout navItem(int iconResource, String label, boolean active) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);
        item.setSelected(active);
        item.setMinimumHeight(Math.max(dp(64), controlSize()));
        item.setContentDescription(ui(label) + " tab. "
                + (active ? "Selected. " : "Not selected. ")
                + "Double tap to open.");
        item.setOnClickListener(view -> handleNav(label));
        int tint = color(active ? R.color.fastaid_red : R.color.fastaid_muted);

        FrameLayout indicator = new FrameLayout(this);
        indicator.setBackground(pill(active ? color(R.color.fastaid_nav_indicator) : Color.TRANSPARENT, active ? color(R.color.fastaid_nav_indicator) : Color.TRANSPARENT, 18));
        ImageView icon = plainIcon(iconResource, tint, 1);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER);
        indicator.addView(icon, iconParams);
        item.addView(indicator, new LinearLayout.LayoutParams(dp(56), dp(32)));

        TextView text = text(label, 10, active ? R.color.fastaid_red : R.color.fastaid_muted, true);
        text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, dp(2), 0, 0);
        item.addView(text);
        return item;
    }

    private void handleNav(String label) {
        if ("Profile".equals(label)) {
            populateProfileContent();
            refreshBottomNav();
            return;
        }

        if ("Incidents".equals(label)) {
            populateIncidentsContent();
            refreshBottomNav();
            return;
        }

        if ("Map".equals(label)) {
            populateMapContent();
        } else if ("Nearby".equals(label)) {
            populateNearbyContent();
            fetchNearbyAid();
        } else {
            populateHomeContent();
            setUiText(statusText, "Tap SOS only when you need emergency help");
        }
        refreshBottomNav();
    }

    private View buildIncidentsPage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, dp(4), 0, dp(18));

        page.addView(profileSectionTitle("Your Incident"));
        page.addView(buildUserIncidentCard());
        if (!hasActiveIncident()) return page;

        LinearLayout responderHeading = new LinearLayout(this);
        responderHeading.setOrientation(LinearLayout.HORIZONTAL);
        responderHeading.setGravity(Gravity.CENTER_VERTICAL);
        responderHeading.setPadding(dp(4), dp(20), 0, dp(8));
        responderHeading.addView(text("Responder Demo", 15, R.color.fastaid_ink, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView availability = text(responderAvailable ? "AVAILABLE" : "OFF DUTY", 11,
                responderAvailable ? R.color.fastaid_green : R.color.fastaid_muted, true);
        availability.setGravity(Gravity.CENTER);
        availability.setPadding(dp(10), dp(6), dp(10), dp(6));
        availability.setBackground(pill(
                responderAvailable ? color(R.color.fastaid_soft_green) : color(R.color.fastaid_surface_container),
                responderAvailable ? color(R.color.fastaid_soft_green) : color(R.color.fastaid_outline), 16));
        availability.setClickable(true);
        availability.setFocusable(true);
        availability.setMinWidth(dp(96));
        availability.setMinHeight(dp(48));
        availability.setOnClickListener(view -> {
            responderAvailable = !responderAvailable;
            if (!responderAvailable && "incoming".equals(responderAlertState)) responderAlertState = "paused";
            if (responderAvailable && "paused".equals(responderAlertState)) responderAlertState = "incoming";
            populateIncidentsContent();
            refreshBottomNav();
        });
        responderHeading.addView(availability);
        page.addView(responderHeading);

        TextView notice = text("Prototype only - no real responder is being dispatched.", 12, R.color.fastaid_orange, true);
        notice.setPadding(dp(12), dp(9), dp(12), dp(9));
        notice.setBackground(pill(color(R.color.fastaid_card), color(R.color.fastaid_card), 12));
        page.addView(notice);
        page.addView(buildResponderAlertCard());
        return page;
    }

    private View buildUserIncidentCard() {
        if (!hasActiveIncident()) {
            LinearLayout empty = new LinearLayout(this);
            empty.setOrientation(LinearLayout.VERTICAL);
            empty.setPadding(dp(16), dp(14), dp(16), dp(14));
            empty.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_outline), 18));
            empty.addView(text("No active incident", 15, R.color.fastaid_ink, true));
            empty.addView(text("A FastAid request appears here after it is sent or saved on this device.",
                    12, R.color.fastaid_muted, false));
            return empty;
        }
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_outline), 18));
        card.setElevation(dp(2));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(iconBubble(R.drawable.ic_aid_accident, color(R.color.fastaid_red), 46, 11),
                new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), 0, 0, 0);
        copy.addView(text(capitalize(incidentType) + " - " + patientCount + " people", 15, R.color.fastaid_ink, true));
        copy.addView(text(String.format(Locale.US, "%.5f, %.5f", currentLatitude, currentLongitude), 12, R.color.fastaid_muted, false));
        heading.addView(copy, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView status = text(activeIncidentStatus.toUpperCase(Locale.US), 11, incidentStatusColor(), true);
        status.setPadding(dp(9), dp(5), dp(9), dp(5));
        status.setBackground(pill(incidentStatusBackground(), incidentStatusBackground(), 14));
        heading.addView(status);
        card.addView(heading);

        TextView progress = text(incidentProgressCopy(), 13, R.color.fastaid_muted, false);
        progress.setPadding(0, dp(12), 0, 0);
        card.addView(progress);
        return card;
    }

    private View buildResponderAlertCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_outline), 18));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(cardParams);

        String stateTitle;
        int stateColor;
        if ("accepted".equals(responderAlertState)) {
            stateTitle = "Accepted - navigate to incident";
            stateColor = R.color.fastaid_blue;
        } else if ("declined".equals(responderAlertState)) {
            stateTitle = "Alert declined";
            stateColor = R.color.fastaid_muted;
        } else if ("paused".equals(responderAlertState)) {
            stateTitle = "Responder is off duty";
            stateColor = R.color.fastaid_muted;
        } else {
            stateTitle = "New emergency alert";
            stateColor = R.color.fastaid_red;
        }

        card.addView(text(stateTitle, 18, stateColor, true));
        TextView meta = text(capitalize(incidentType) + "  |  " + patientCount + " people  |  High priority", 13, R.color.fastaid_muted, false);
        meta.setPadding(0, dp(6), 0, 0);
        card.addView(meta);
        TextView location = text(String.format(Locale.US, "Incident location  %.5f, %.5f", currentLatitude, currentLongitude), 13, R.color.fastaid_ink, true);
        location.setPadding(0, dp(12), 0, 0);
        card.addView(location);
        TextView timing = text("Reported now - estimated arrival 4 min", 12, R.color.fastaid_muted, false);
        timing.setPadding(0, dp(4), 0, dp(12));
        card.addView(timing);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);

        if ("accepted".equals(responderAlertState)) {
            Button navigate = compactIconButton("Navigate", R.drawable.ic_m3_navigation, color(R.color.fastaid_blue));
            navigate.setTextColor(color(R.color.fastaid_blue));
            navigate.setBackground(pill(color(R.color.fastaid_soft_blue), color(R.color.fastaid_soft_blue), 14));
            navigate.setOnClickListener(view -> navigateToIncident());
            actions.addView(navigate, rowWeight());

            Button arrived = compactButton("Mark arrived");
            arrived.setTextColor(Color.WHITE);
            arrived.setBackground(pill(color(R.color.fastaid_green), color(R.color.fastaid_green), 14));
            arrived.setOnClickListener(view -> {
                activeIncidentStatus = "Arrived";
                populateIncidentsContent();
                refreshBottomNav();
            });
            actions.addView(arrived, rowWeight());
        } else if ("incoming".equals(responderAlertState) && responderAvailable) {
            Button decline = compactButton("Decline");
            decline.setTextColor(color(R.color.fastaid_red));
            decline.setBackground(pill(Color.WHITE, color(R.color.fastaid_red), 14));
            decline.setOnClickListener(view -> {
                responderAlertState = "declined";
                activeIncidentStatus = "Unassigned";
                populateIncidentsContent();
                refreshBottomNav();
            });
            actions.addView(decline, rowWeight());

            Button accept = compactButton("Accept");
            accept.setTextColor(Color.WHITE);
            accept.setBackground(pill(color(R.color.fastaid_blue), color(R.color.fastaid_blue), 14));
            accept.setOnClickListener(view -> {
                responderAlertState = "accepted";
                activeIncidentStatus = "En route";
                populateIncidentsContent();
                refreshBottomNav();
            });
            actions.addView(accept, rowWeight());
        } else {
            Button reset = compactButton("Reset demo alert");
            reset.setTextColor(color(R.color.fastaid_blue));
            reset.setBackground(pill(color(R.color.fastaid_soft_blue), color(R.color.fastaid_soft_blue), 14));
            reset.setOnClickListener(view -> {
                responderAvailable = true;
                responderAlertState = "incoming";
                activeIncidentStatus = "Sent";
                populateIncidentsContent();
                refreshBottomNav();
            });
            actions.addView(reset, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
        }
        card.addView(actions, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(50)));
        return card;
    }

    private void navigateToIncident() {
        Uri uri = Uri.parse("google.navigation:q=" + currentLatitude + "," + currentLongitude);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) == null) intent.setPackage(null);
        startActivity(intent);
    }

    private String capitalize(String value) {
        if (value == null || value.length() == 0) return "Incident";
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    private int incidentStatusColor() {
        if ("En route".equals(activeIncidentStatus) || "Arrived".equals(activeIncidentStatus)) return R.color.fastaid_blue;
        if ("Unassigned".equals(activeIncidentStatus)) return R.color.fastaid_red;
        if ("Saved only".equals(activeIncidentStatus)) return R.color.fastaid_orange;
        return R.color.fastaid_green;
    }

    private int incidentStatusBackground() {
        if ("En route".equals(activeIncidentStatus) || "Arrived".equals(activeIncidentStatus)) return color(R.color.fastaid_soft_blue);
        if ("Unassigned".equals(activeIncidentStatus)) return color(R.color.fastaid_soft_red);
        if ("Saved only".equals(activeIncidentStatus)) return color(R.color.fastaid_card);
        return color(R.color.fastaid_soft_green);
    }

    private String incidentProgressCopy() {
        if ("En route".equals(activeIncidentStatus)) return "A verified responder accepted the demo alert. ETA 4 min.";
        if ("Arrived".equals(activeIncidentStatus)) return "Responder marked arrived. Keep the emergency line available.";
        if ("Unassigned".equals(activeIncidentStatus)) {
            return "No responder assigned. Call " + emergencyNumber() + " for urgent official help.";
        }
        if ("Saved only".equals(activeIncidentStatus)) return "Saved on this device and waiting for connectivity.";
        return "Create an incident to notify verified FastAid partners in a pilot deployment.";
    }

    private View buildProfilePage() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(0, dp(4), 0, dp(260));

        page.addView(buildProfileHero());
        page.addView(buildEmergencyHandoffSnapshot());

        page.addView(profileSectionTitle("Identity and contacts"));
        LinearLayout identityRows = profileRowsContainer();
        addProfileRow(identityRows, editableProfileRow(
                "Name", profileDisplayValue("name", DEFAULT_PROFILE_NAME), "name", DEFAULT_PROFILE_NAME,
                R.drawable.ic_m3_person, R.color.fastaid_blue));
        addProfileRow(identityRows, editableProfileRow(
                "Phone", profileValue("phone", "Add your mobile number"), "phone",
                "Add your mobile number", R.drawable.ic_m3_call, R.color.fastaid_green));
        addProfileRow(identityRows, editableProfileRow(
                "Emergency contacts", emergencyContactsSummary(), "contacts",
                "Add trusted contacts", R.drawable.ic_m3_person, R.color.fastaid_red));
        page.addView(profileGroupCard(identityRows, Color.WHITE));

        page.addView(profileSectionTitle("Medical handoff"));
        LinearLayout medicalRows = profileRowsContainer();
        addProfileRow(medicalRows, editableProfileRow(
                "Blood group", profileValue("blood_group", "Not set"), "blood_group", "Not set",
                R.drawable.ic_aid_medical, R.color.fastaid_red));
        addProfileRow(medicalRows, editableProfileRow(
                "Medical notes", profileValue("medical", "Allergies, medication, conditions"), "medical",
                "Allergies, medication, conditions", R.drawable.ic_aid_medical, R.color.fastaid_blue));
        page.addView(profileGroupCard(medicalRows, Color.WHITE));

        page.addView(profileSectionTitle("Vehicle handoff"));
        LinearLayout vehicleRows = profileRowsContainer();
        addProfileRow(vehicleRows, editableProfileRow(
                "Vehicle", vehicleSummary(), "vehicle",
                "Add vehicle details", R.drawable.ic_aid_breakdown, R.color.fastaid_orange));
        addProfileRow(vehicleRows, profileStatusRow(
                "Registration",
                profileValue("vehicle_registration", "Add registration for quick identification"),
                hasProfileValue("vehicle_registration") ? "Saved" : "Missing",
                R.drawable.ic_m3_list,
                R.color.fastaid_orange,
                hasProfileValue("vehicle_registration") ? R.color.fastaid_green : R.color.fastaid_orange));
        addProfileRow(vehicleRows, profileStatusRow(
                "Insurance",
                profileValue("vehicle_insurance", "Optional policy/provider for roadside support"),
                hasProfileValue("vehicle_insurance") ? "Saved" : "Optional",
                R.drawable.ic_m3_list,
                R.color.fastaid_blue,
                hasProfileValue("vehicle_insurance") ? R.color.fastaid_green : R.color.fastaid_muted));
        page.addView(profileGroupCard(vehicleRows, Color.WHITE));

        page.addView(profileSectionTitle("Country and language"));
        LinearLayout countryRows = profileRowsContainer();
        addProfileRow(countryRows, editableProfileRow(
                "Emergency number", emergencyNumber() + " - " + emergencyNumberSource(),
                "emergency_number_override", "Automatic for current country",
                R.drawable.ic_m3_call, R.color.fastaid_red));
        addProfileRow(countryRows, editableProfileRow(
                "Language", preferredLanguage(), "language", "English",
                R.drawable.ic_m3_map, R.color.fastaid_green));
        page.addView(profileGroupCard(countryRows, Color.WHITE));

        page.addView(profileSectionTitle("App readiness"));
        LinearLayout readinessRows = profileRowsContainer();
        View locationRow = profileStatusRow(
                "Location permission",
                "Used to find aid and share accurate coordinates",
                locationPermissionStatus(),
                R.drawable.ic_m3_my_location,
                R.color.fastaid_blue,
                locationPermissionStatus().equals("Granted") ? R.color.fastaid_green : R.color.fastaid_red);
        locationRow.setOnClickListener(view -> requestLocation());
        addProfileRow(readinessRows, locationRow);
        addProfileRow(readinessRows, profileStatusRow(
                "Maps and nearby places",
                placesRepository == null ? "Add the Android-restricted key to activate live data"
                        : "Native Google Maps and Places are configured",
                placesRepository == null ? "Key needed" : "Ready",
                R.drawable.ic_m3_map,
                R.color.fastaid_blue,
                placesRepository == null ? R.color.fastaid_orange : R.color.fastaid_green));
        addProfileRow(readinessRows, profileStatusRow(
                "Offline recovery",
                "Recent real Places results and SMS coordinates remain available",
                hasQueuedIncident() ? "1 queued" : "Ready",
                R.drawable.ic_m3_share,
                R.color.fastaid_green,
                hasQueuedIncident() ? R.color.fastaid_orange : R.color.fastaid_green));
        page.addView(profileGroupCard(readinessRows, Color.WHITE));
        page.addView(buildMapsPlacesSettingsCard());
        page.addView(buildOfflineActionsCard());

        page.addView(profileSectionTitle("Preferences"));
        LinearLayout settingRows = profileRowsContainer();
        addProfileRow(settingRows, settingSwitchRow(
                "Offline cache", "Keep recent real Google Places results on this device",
                "offline_cache", true));
        addProfileRow(settingRows, settingSwitchRow(
                "SMS SOS", "Prepare a text message with your coordinates",
                "sms_sos", true));
        addProfileRow(settingRows, settingSwitchRow(
                "Voice commands", "Use speech for SOS, calling, location and nearby aid",
                "voice_commands", true));
        addProfileRow(settingRows, settingSwitchRow(
                "Active request location", "Refresh location only while a request is active",
                "active_location_refresh", false));
        addProfileRow(settingRows, settingSwitchRow(
                "Larger controls", "Use roomier touch targets during stressful situations",
                "large_controls", false));
        addProfileRow(settingRows, settingSwitchRow(
                "Reduce motion", "Avoid animated map and progress transitions",
                "reduce_motion", false));
        page.addView(profileGroupCard(settingRows, Color.WHITE));

        page.addView(profileSectionTitle("Emergency actions"));
        MaterialButton call = profileActionButton(
                "Call " + emergencyNumber(), R.drawable.ic_m3_call,
                color(R.color.fastaid_red), Color.WHITE, false,
                view -> callEmergency());
        page.addView(call, profileActionParams(0));
        MaterialButton locate = profileActionButton(
                "Use current location", R.drawable.ic_m3_my_location,
                color(R.color.fastaid_soft_blue), color(R.color.fastaid_blue), false,
                view -> requestLocation());
        page.addView(locate, profileActionParams(dp(8)));
        MaterialButton share = profileActionButton(
                "Share my location", R.drawable.ic_m3_share, Color.WHITE,
                color(R.color.fastaid_ink), true, view -> shareCurrentLocation());
        page.addView(share, profileActionParams(dp(8)));
        page.addView(buildProfilePrivacyNote());
        return page;
    }

    private View buildMapsPlacesSettingsCard() {
        MaterialCardView card = profileCard(Color.WHITE, dp(1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("Maps and nearby data", 14, R.color.fastaid_ink, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        boolean liveReady = BuildConfig.MAPS_KEY_CONFIGURED && placesRepository != null;
        TextView state = text(liveReady ? "LIVE" : "SETUP", 11,
                liveReady ? R.color.fastaid_green : R.color.fastaid_orange, true);
        state.setPadding(dp(9), dp(5), dp(9), dp(5));
        state.setBackground(pill(color(R.color.fastaid_surface_container),
                color(R.color.fastaid_surface_container), 12));
        heading.addView(state);
        content.addView(heading);

        String cacheCopy = places.isEmpty()
                ? "No visible nearby places in memory yet."
                : places.size() + " nearby options loaded from live or saved Places data.";
        TextView copy = text((liveReady
                        ? "Google Maps and Places are configured for live POI results. "
                        : "Add Maps and Places keys to enable live map and POI results. ")
                        + cacheCopy,
                12, R.color.fastaid_muted, false);
        copy.setPadding(0, dp(6), 0, dp(12));
        content.addView(copy);

        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton refresh = profileActionButton(
                "Refresh nearby", R.drawable.ic_m3_search,
                color(R.color.fastaid_blue), Color.WHITE, false,
                view -> {
                    fetchNearbyAid();
                    Toast.makeText(this, "Refreshing nearby aid from current location.", Toast.LENGTH_SHORT).show();
                });
        firstRow.addView(refresh, rowWeight());
        MaterialButton locate = profileActionButton(
                "Use GPS", R.drawable.ic_m3_my_location,
                color(R.color.fastaid_soft_blue), color(R.color.fastaid_blue), false,
                view -> requestLocation());
        firstRow.addView(locate, rowWeight());
        content.addView(firstRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        LinearLayout secondRow = new LinearLayout(this);
        secondRow.setOrientation(LinearLayout.HORIZONTAL);
        secondRow.setPadding(0, dp(8), 0, 0);
        MaterialButton map = profileActionButton(
                "Open map", R.drawable.ic_m3_map,
                Color.WHITE, color(R.color.fastaid_ink), true,
                view -> {
                    populateMapContent();
                    refreshBottomNav();
                });
        secondRow.addView(map, rowWeight());
        MaterialButton clear = profileActionButton(
                "Clear cache", R.drawable.ic_m3_list,
                Color.WHITE, color(R.color.fastaid_ink), true,
                view -> {
                    if (placeCache != null) placeCache.clear();
                    places.clear();
                    updateGoogleMap();
                    if (resultsList != null) renderPlaces();
                    Toast.makeText(this, "Nearby cache cleared.", Toast.LENGTH_SHORT).show();
                    if ("Profile".equals(activeTab)) populateProfileContent();
                });
        secondRow.addView(clear, rowWeight());
        content.addView(secondRow, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));

        card.addView(content);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(params);
        return card;
    }

    private View buildEmergencyHandoffSnapshot() {
        MaterialCardView card = profileCard(Color.WHITE, dp(1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Emergency handoff", 15, R.color.fastaid_ink, true);
        heading.addView(title, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView privacy = text("ON DEVICE", 11, R.color.fastaid_blue, true);
        privacy.setPadding(dp(9), dp(5), dp(9), dp(5));
        privacy.setBackground(pill(color(R.color.fastaid_soft_blue),
                color(R.color.fastaid_soft_blue), 12));
        heading.addView(privacy);
        content.addView(heading);

        TextView copy = text("Keep this readable for bystanders and responders. Only add details you are comfortable storing locally.",
                12, R.color.fastaid_muted, false);
        copy.setPadding(0, dp(4), 0, dp(12));
        content.addView(copy);

        LinearLayout firstRow = new LinearLayout(this);
        firstRow.setOrientation(LinearLayout.HORIZONTAL);
        firstRow.addView(profileFactChip(contactCount() + " contacts",
                contactCount() > 0 ? "Trusted contacts ready" : "Add trusted contacts",
                R.drawable.ic_m3_call, contactCount() > 0 ? R.color.fastaid_green : R.color.fastaid_orange,
                "Double tap to choose emergency contacts.",
                view -> showEmergencyContactsDialog()),
                rowWeight());
        firstRow.addView(profileFactChip(profileValue("blood_group", "Blood not set"),
                "Blood group", R.drawable.ic_aid_medical,
                hasUsefulBloodGroup() ? R.color.fastaid_red : R.color.fastaid_orange,
                "Double tap to select blood group.",
                view -> showBloodGroupSelector()),
                rowWeight());
        content.addView(firstRow);

        LinearLayout secondRow = new LinearLayout(this);
        secondRow.setOrientation(LinearLayout.HORIZONTAL);
        secondRow.setPadding(0, dp(8), 0, 0);
        secondRow.addView(profileFactChip(vehicleFactTitle(),
                hasProfileValue("vehicle_registration") ? "Vehicle identifiable" : "Add vehicle details",
                R.drawable.ic_aid_breakdown,
                hasProfileValue("vehicle_registration") ? R.color.fastaid_green : R.color.fastaid_orange,
                "Double tap to add vehicle details.",
                view -> showVehicleEditor()),
                rowWeight());
        secondRow.addView(profileFactChip(locationPermissionStatus(),
                "Location access", R.drawable.ic_m3_my_location,
                "Granted".equals(locationPermissionStatus()) ? R.color.fastaid_blue : R.color.fastaid_red,
                "Double tap to refresh current location.",
                view -> requestLocation()),
                rowWeight());
        content.addView(secondRow);

        card.addView(content);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(12), 0, 0);
        card.setLayoutParams(params);
        return card;
    }

    private View profileFactChip(
            String title,
            String supporting,
            int iconResource,
            int colorResource,
            String actionDescription,
            View.OnClickListener listener
    ) {
        LinearLayout chip = new LinearLayout(this);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(10), dp(10), dp(10), dp(10));
        chip.setBackground(cardBackground(
                color(R.color.fastaid_surface_container),
                color(R.color.fastaid_outline),
                16));
        chip.addView(iconBubble(iconResource, color(colorResource), 34, 9),
                new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(8), 0, 0, 0);
        TextView titleView = text(title, 12, R.color.fastaid_ink, true);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(titleView);
        TextView supportingView = text(supporting, 10, R.color.fastaid_muted, false);
        supportingView.setMaxLines(1);
        supportingView.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(supportingView);
        chip.addView(copy, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        chip.setContentDescription(title + ". " + supporting + ". " + actionDescription);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setOnClickListener(listener);
        return chip;
    }

    private View buildProfileHero() {
        MaterialCardView card = profileCard(color(R.color.fastaid_soft_blue), 0);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.HORIZONTAL);
        identity.setGravity(Gravity.CENTER_VERTICAL);
        identity.addView(iconBubble(R.drawable.ic_aid_police, color(R.color.fastaid_blue), 56, 13),
                new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(14), 0, dp(8), 0);
        copy.addView(text(profileDisplayValue("name", DEFAULT_PROFILE_NAME), 19, R.color.fastaid_ink, true));
        copy.addView(text("Emergency handoff details stored on this device", 12,
                R.color.fastaid_muted, false));
        identity.addView(copy, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        ImageView edit = actionIconButton(R.drawable.ic_m3_person, color(R.color.fastaid_blue),
                Color.WHITE, "Edit name");
        edit.setOnClickListener(view -> showProfileEditor("Name", "name", DEFAULT_PROFILE_NAME));
        identity.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(48)));
        content.addView(identity);

        int completeness = profileCompleteness();
        LinearLayout progressCopy = new LinearLayout(this);
        progressCopy.setOrientation(LinearLayout.HORIZONTAL);
        progressCopy.setPadding(0, dp(14), 0, dp(6));
        progressCopy.addView(text("Profile completeness", 12, R.color.fastaid_ink, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView percent = text(completeness + "%", 12, R.color.fastaid_blue, true);
        progressCopy.addView(percent);
        content.addView(progressCopy);

        LinearProgressIndicator progress = new LinearProgressIndicator(this);
        progress.setMax(100);
        progress.setProgressCompat(completeness, false);
        progress.setContentDescription("Profile completeness " + completeness + " percent");
        progress.setIndicatorColor(color(R.color.fastaid_blue));
        progress.setTrackColor(Color.WHITE);
        progress.setTrackThickness(dp(6));
        content.addView(progress, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(6)));

        TextView remaining = text(profileCompletenessSummary(), 12, R.color.fastaid_muted, false);
        remaining.setPadding(0, dp(8), 0, 0);
        remaining.setMaxLines(2);
        remaining.setEllipsize(TextUtils.TruncateAt.END);
        content.addView(remaining);

        TextView action = text(completeness >= 100 ? "Review safety profile" : "Complete next item", 12,
                R.color.fastaid_blue, true);
        action.setPadding(0, dp(6), 0, 0);
        content.addView(action);

        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("Profile completeness " + completeness + " percent. "
                + profileCompletenessSummary() + ". Double tap to "
                + (completeness >= 100 ? "review your safety profile." : "complete the next missing item."));
        card.setOnClickListener(view -> openNextMissingProfileItem());
        card.addView(content);
        return card;
    }

    private TextView profileSectionTitle(String label) {
        TextView title = text(label, 14, R.color.fastaid_muted, true);
        title.setPadding(dp(4), dp(22), 0, dp(8));
        return title;
    }

    private MaterialButton profileActionButton(
            String label,
            int iconResource,
            int fillColor,
            int textColor,
            boolean outlined,
            View.OnClickListener listener
    ) {
        MaterialButton button = materialButton(label, fillColor, textColor, outlined, 14);
        button.setIconResource(iconResource);
        button.setIconTint(ColorStateList.valueOf(textColor));
        button.setIconPadding(dp(10));
        button.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        button.setGravity(Gravity.CENTER);
        button.setOnClickListener(listener);
        return button;
    }

    private LinearLayout.LayoutParams profileActionParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        params.setMargins(0, topMargin, 0, 0);
        return params;
    }

    private View buildProfilePrivacyNote() {
        MaterialCardView card = profileCard(color(R.color.fastaid_surface_container), 0);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.addView(iconBubble(R.drawable.ic_aid_police, color(R.color.fastaid_green), 40, 10),
                new LinearLayout.LayoutParams(dp(40), dp(40)));
        TextView copy = text("Your safety profile stays on this device. Share it only with your consent.",
                12, R.color.fastaid_muted, false);
        copy.setPadding(dp(12), 0, 0, 0);
        row.addView(copy, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(row);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(14), 0, 0);
        card.setLayoutParams(params);
        return card;
    }
    private View buildOfflineActionsCard() {
        MaterialCardView card = profileCard(Color.WHITE, dp(1));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("Offline and SMS SOS", 14, R.color.fastaid_ink, true),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView state = text(hasQueuedIncident() ? "1 QUEUED" : "READY", 11,
                hasQueuedIncident() ? R.color.fastaid_orange : R.color.fastaid_green, true);
        state.setPadding(dp(9), dp(5), dp(9), dp(5));
        state.setBackground(pill(color(R.color.fastaid_surface_container),
                color(R.color.fastaid_surface_container), 12));
        heading.addView(state);
        content.addView(heading);

        TextView copy = text(
                "Your last coordinates stay available on this device. Prepare an SMS when data service is unavailable.",
                12, R.color.fastaid_muted, false);
        copy.setPadding(0, dp(6), 0, dp(12));
        content.addView(copy);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        MaterialButton sms = profileActionButton(
                "Prepare SMS", R.drawable.ic_m3_share, color(R.color.fastaid_soft_blue),
                color(R.color.fastaid_blue), false, view -> prepareSmsSos());
        actions.addView(sms, rowWeight());

        MaterialButton retry = profileActionButton(
                hasQueuedIncident() ? "Retry queued" : "Call " + emergencyNumber(),
                hasQueuedIncident() ? R.drawable.ic_m3_my_location : R.drawable.ic_m3_call,
                color(R.color.fastaid_red), Color.WHITE, false, view -> {
                    if (hasQueuedIncident()) retryQueuedIncident();
                    else callEmergency();
                });
        actions.addView(retry, rowWeight());
        content.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        card.addView(content);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(params);
        return card;
    }

    private void restoreOfflineState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.contains("last_lat") && prefs.contains("last_lng")) {
            currentLatitude = Double.longBitsToDouble(
                    prefs.getLong("last_lat", Double.doubleToRawLongBits(Double.NaN)));
            currentLongitude = Double.longBitsToDouble(
                    prefs.getLong("last_lng", Double.doubleToRawLongBits(Double.NaN)));
            currentLocationTimestamp = prefs.getLong("last_location_time", 0L);
            currentLocationAccuracy = prefs.getFloat("last_location_accuracy", Float.NaN);
            currentLocationSource = prefs.getString("last_location_source", "Saved location");
        }
        if (prefs.getBoolean("incident_queued", false)) activeIncidentStatus = "Saved only";
    }

    private void saveLastKnownLocation() {
        if (!hasCurrentCoordinates()) return;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putLong("last_lat", Double.doubleToRawLongBits(currentLatitude))
                .putLong("last_lng", Double.doubleToRawLongBits(currentLongitude))
                .putLong("last_location_time", currentLocationTimestamp)
                .putFloat("last_location_accuracy", currentLocationAccuracy)
                .putString("last_location_source", currentLocationSource)
                .apply();
    }

    private boolean hasQueuedIncident() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getBoolean("incident_queued", false);
    }

    private void queueIncidentForRetry() {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putBoolean("incident_queued", true)
                .putLong("incident_lat", Double.doubleToRawLongBits(currentLatitude))
                .putLong("incident_lng", Double.doubleToRawLongBits(currentLongitude))
                .putString("incident_type", incidentType)
                .putInt("incident_people", patientCount)
                .putString("incident_notes", getIncidentNotes())
                .apply();
        activeIncidentStatus = "Saved only";
    }

    private boolean syncQueuedIncidentIfNeeded() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (!prefs.getBoolean("incident_queued", false)) return false;
        try {
            apiClient.createIncident(
                    Double.longBitsToDouble(prefs.getLong("incident_lat", Double.doubleToRawLongBits(currentLatitude))),
                    Double.longBitsToDouble(prefs.getLong("incident_lng", Double.doubleToRawLongBits(currentLongitude))),
                    prefs.getString("incident_type", "accident"),
                    prefs.getInt("incident_people", 1),
                    prefs.getString("incident_notes", ""));
            prefs.edit().putBoolean("incident_queued", false).apply();
            activeIncidentStatus = "Sent";
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void retryQueuedIncident() {
        setUiText(statusText, "Retrying queued incident...");
        executor.execute(() -> {
            boolean synced = syncQueuedIncidentIfNeeded();
            runOnUiThread(() -> {
                setUiText(statusText, synced ? "Queued incident sent" : "Still offline - incident remains queued");
                if ("Profile".equals(activeTab)) {
                    populateProfileContent();
                    refreshBottomNav();
                }
            });
        });
    }

    private void prepareSmsSos() {
        if (!settingEnabled("sms_sos", true)) {
            Toast.makeText(this, "Enable SMS SOS in Profile preferences.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!hasFreshSosLocation()) {
            Toast.makeText(this,
                    "Refresh GPS or enter a current location before preparing an SOS message.",
                    Toast.LENGTH_LONG).show();
            showManualLocationDialog();
            return;
        }
        String body = String.format(Locale.US,
                "FastAid SOS. I need help at %.5f, %.5f. Map: https://maps.google.com/?q=%.5f,%.5f. Type: %s. People: %d.",
                currentLatitude, currentLongitude, currentLatitude, currentLongitude, incidentType, patientCount);
        Intent sms = new Intent(Intent.ACTION_SENDTO);
        sms.setData(Uri.parse("smsto:"));
        sms.putExtra("sms_body", body);
        if (sms.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No SMS app is available on this device.", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(sms);
    }

    private String locationPermissionStatus() {
        boolean hasFine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return (hasFine || hasCoarse) ? "Granted" : "Needs access";
    }

    private LinearLayout buildPatientCountCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), 0, dp(14), 0);
        card.setBackgroundColor(color(R.color.fastaid_card));

        TextView label = text("Number of People", 13, R.color.fastaid_ink, true);
        card.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button minus = whiteButton("-");
        minus.setContentDescription("Decrease number of people");
        minus.setOnClickListener(view -> updatePatientCount(-1));
        card.addView(minus, new LinearLayout.LayoutParams(dp(48), dp(48)));

        patientCountText = text(String.valueOf(patientCount), 15, R.color.fastaid_red, true);
        patientCountText.setGravity(Gravity.CENTER);
        patientCountText.setContentDescription(patientCount + " people");
        card.addView(patientCountText, new LinearLayout.LayoutParams(
                dp(48), LinearLayout.LayoutParams.WRAP_CONTENT));

        Button plus = whiteButton("+");
        plus.setContentDescription("Increase number of people");
        plus.setOnClickListener(view -> updatePatientCount(1));
        card.addView(plus, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(64)
        );
        params.setMargins(0, dp(10), 0, 0);
        card.setLayoutParams(params);
        return card;
    }

    private EditText buildNotesField() {
        notesInput = new EditText(this);
        notesInput.setHint("Optional notes: bleeding, trapped, fire, landmark...");
        notesInput.setSingleLine(false);
        notesInput.setMinLines(2);
        notesInput.setTextSize(13);
        notesInput.setTextColor(color(R.color.fastaid_ink));
        notesInput.setHintTextColor(color(R.color.fastaid_muted));
        notesInput.setBackgroundColor(color(R.color.fastaid_card));
        notesInput.setPadding(dp(14), 0, dp(14), 0);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(74)
        );
        params.setMargins(0, dp(10), 0, 0);
        notesInput.setLayoutParams(params);
        return notesInput;
    }

    private void updatePatientCount(int delta) {
        patientCount = Math.max(1, Math.min(12, patientCount + delta));
        if (patientCountText != null) {
            patientCountText.setText(String.valueOf(patientCount));
            patientCountText.setContentDescription(patientCount + " people");
            patientCountText.announceForAccessibility(patientCount + " people");
        }
    }

    private String getIncidentNotes() {
        if (notesInput == null || notesInput.getText() == null) {
            return "";
        }
        return notesInput.getText().toString().trim();
    }

    private FrameLayout.LayoutParams bottomNavParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(72),
                Gravity.BOTTOM
        );
    }

    private LinearLayout profileRowsContainer() {
        LinearLayout rows = new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        return rows;
    }

    private MaterialCardView profileCard(int fillColor, int strokeWidth) {
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(fillColor);
        card.setRadius(dp(8));
        card.setCardElevation(0);
        card.setUseCompatPadding(false);
        card.setPreventCornerOverlap(true);
        card.setStrokeColor(color(R.color.fastaid_outline));
        card.setStrokeWidth(strokeWidth);
        return card;
    }

    private MaterialCardView profileGroupCard(LinearLayout content, int fillColor) {
        MaterialCardView card = profileCard(fillColor, dp(1));
        card.addView(content);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private void addProfileRow(LinearLayout container, View row) {
        if (container.getChildCount() > 0) {
            View divider = new View(this);
            divider.setBackgroundColor(color(R.color.fastaid_outline));
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            dividerParams.setMargins(dp(64), 0, 0, 0);
            container.addView(divider, dividerParams);
        }
        container.addView(row);
    }

    private LinearLayout profileRowBase(
            String title,
            String supporting,
            int iconResource,
            int iconColorResource
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(72));
        row.setPadding(dp(12), dp(9), dp(12), dp(9));

        android.util.TypedValue ripple = new android.util.TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true)) {
            row.setBackgroundResource(ripple.resourceId);
        }

        row.addView(iconBubble(iconResource, color(iconColorResource), 40, 10),
                new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), 0, dp(8), 0);
        TextView titleView = text(title, 14, R.color.fastaid_ink, true);
        TextView supportingView = text(supporting, 12, R.color.fastaid_muted, false);
        supportingView.setMaxLines(2);
        supportingView.setEllipsize(TextUtils.TruncateAt.END);
        copy.addView(titleView);
        copy.addView(supportingView);
        row.addView(copy, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private View editableProfileRow(
            String label,
            String value,
            String preferenceKey,
            String defaultValue,
            int iconResource,
            int iconColorResource
    ) {
        LinearLayout row = profileRowBase(label, value, iconResource, iconColorResource);
        ImageView edit = actionIconButton(R.drawable.ic_m3_edit, color(R.color.fastaid_blue),
                color(R.color.fastaid_soft_blue), "Edit " + label);
        edit.setClickable(false);
        edit.setFocusable(false);
        edit.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(edit, new LinearLayout.LayoutParams(dp(48), dp(48)));
        row.setClickable(true);
        row.setFocusable(true);
        row.setContentDescription("Edit " + label + ", current value " + value);
        row.setOnClickListener(view -> editProfileField(label, preferenceKey, defaultValue));
        return row;
    }

    private View profileStatusRow(
            String title,
            String supporting,
            String status,
            int iconResource,
            int iconColorResource,
            int statusColorResource
    ) {
        LinearLayout row = profileRowBase(title, supporting, iconResource, iconColorResource);
        TextView badge = text(status, 11, statusColorResource, true);
        badge.setGravity(Gravity.CENTER);
        badge.setMaxLines(1);
        badge.setPadding(dp(9), dp(5), dp(9), dp(5));
        badge.setBackground(pill(color(R.color.fastaid_surface_container),
                color(R.color.fastaid_surface_container), 12));
        row.addView(badge);
        return row;
    }

    private View settingSwitchRow(
            String label,
            String supporting,
            String key,
            boolean defaultValue
    ) {
        LinearLayout row = profileRowBase(label, supporting, R.drawable.ic_m3_list,
                R.color.fastaid_blue);
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        SwitchMaterial toggle = new SwitchMaterial(this);
        toggle.setText("");
        toggle.setUseMaterialThemeColors(true);
        toggle.setChecked(preferences.getBoolean(key, defaultValue));
        toggle.setClickable(false);
        toggle.setFocusable(false);
        toggle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(toggle, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        row.setClickable(true);
        row.setFocusable(true);
        updateSettingRowDescription(row, label, supporting, toggle.isChecked());
        toggle.setOnCheckedChangeListener((button, checked) -> {
            preferences.edit().putBoolean(key, checked).apply();
            updateSettingRowDescription(row, label, supporting, checked);
            if ("offline_cache".equals(key) && !checked && placeCache != null) placeCache.clear();
            if ("active_location_refresh".equals(key) && checked && hasActiveIncident()) {
                requestLocation();
            }
            if ("large_controls".equals(key)) recreate();
        });
        row.setOnClickListener(view -> toggle.toggle());
        return row;
    }

    private void updateSettingRowDescription(
            View row, String label, String supporting, boolean checked) {
        row.setContentDescription(label + ". " + supporting + ". "
                + (checked ? "On" : "Off") + ". Double tap to change.");
    }

    private int profileCompleteness() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        int completed = hasRealProfileName(preferences) ? 1 : 0;
        String[] optionalFields = {"phone", "contacts", "blood_group", "medical", "vehicle"};
        for (String key : optionalFields) {
            String value = preferences.getString(key, "");
            if (value != null && !value.trim().isEmpty()
                    && !("blood_group".equals(key) && "Not set".equals(value))) completed++;
        }
        return Math.round((completed / 6f) * 100f);
    }

    private String profileCompletenessSummary() {
        List<String> missing = missingProfileItems();
        if (missing.isEmpty()) return "All core emergency handoff fields are filled.";
        int limit = Math.min(3, missing.size());
        StringBuilder summary = new StringBuilder("Missing: ");
        for (int index = 0; index < limit; index++) {
            if (index > 0) summary.append(", ");
            summary.append(missing.get(index));
        }
        if (missing.size() > limit) {
            summary.append(" +").append(missing.size() - limit).append(" more");
        }
        return summary.toString();
    }

    private List<String> missingProfileItems() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        List<String> missing = new ArrayList<>();
        if (!hasRealProfileName(preferences)) missing.add("Name");
        if (isBlankProfilePreference(preferences, "phone")) missing.add("Phone");
        if (isBlankProfilePreference(preferences, "contacts")) missing.add("Emergency contacts");
        if (!hasUsefulBloodGroup()) missing.add("Blood group");
        if (isBlankProfilePreference(preferences, "medical")) missing.add("Medical notes");
        if (isBlankProfilePreference(preferences, "vehicle")
                && isBlankProfilePreference(preferences, "vehicle_type")
                && isBlankProfilePreference(preferences, "vehicle_make_model")
                && isBlankProfilePreference(preferences, "vehicle_registration")) {
            missing.add("Vehicle");
        }
        return missing;
    }

    private boolean isBlankProfilePreference(SharedPreferences preferences, String key) {
        String value = preferences.getString(key, "");
        return value == null || value.trim().isEmpty();
    }

    private void openNextMissingProfileItem() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        if (!hasRealProfileName(preferences)) {
            editProfileField("Name", "name", DEFAULT_PROFILE_NAME);
        } else if (isBlankProfilePreference(preferences, "phone")) {
            editProfileField("Phone", "phone", "Add your mobile number");
        } else if (isBlankProfilePreference(preferences, "contacts")) {
            editProfileField("Emergency contacts", "contacts", "Add trusted contacts");
        } else if (!hasUsefulBloodGroup()) {
            editProfileField("Blood group", "blood_group", "Not set");
        } else if (isBlankProfilePreference(preferences, "medical")) {
            editProfileField("Medical notes", "medical", "Allergies, medication, conditions");
        } else if (isBlankProfilePreference(preferences, "vehicle")
                && isBlankProfilePreference(preferences, "vehicle_type")
                && isBlankProfilePreference(preferences, "vehicle_make_model")
                && isBlankProfilePreference(preferences, "vehicle_registration")) {
            editProfileField("Vehicle", "vehicle", "Add vehicle details");
        } else {
            Toast.makeText(this, "Safety profile core fields are complete.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasRealProfileName(SharedPreferences preferences) {
        String value = preferences.getString("name", "");
        if (value == null) return false;
        String trimmed = value.trim();
        return !trimmed.isEmpty()
                && !DEFAULT_PROFILE_NAME.equals(trimmed)
                && !LEGACY_DEFAULT_PROFILE_NAME.equals(trimmed);
    }

    private String profileValue(String key, String defaultValue) {
        String value = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                .getString(key, defaultValue);
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private String profileDisplayValue(String key, String defaultValue) {
        String value = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                .getString(key, "");
        if (value == null || value.trim().isEmpty()) return defaultValue;
        String trimmed = value.trim();
        if (LEGACY_DEFAULT_PROFILE_NAME.equals(trimmed)) return defaultValue;
        return trimmed;
    }

    private boolean hasProfileValue(String key) {
        String value = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                .getString(key, "");
        return value != null && !value.trim().isEmpty();
    }

    private boolean hasUsefulBloodGroup() {
        String value = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                .getString("blood_group", "");
        return value != null && !value.trim().isEmpty() && !"Not set".equals(value);
    }

    private String vehicleFactTitle() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        String registration = preferences.getString("vehicle_registration", "");
        if (registration != null && !registration.trim().isEmpty()) return registration.trim();
        String type = preferences.getString("vehicle_type", "");
        if (type != null && !type.trim().isEmpty()) return type.trim();
        return "Vehicle not set";
    }

    private void migrateStaleDemoProfileState() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        if (preferences.getBoolean("fast_slate_profile_migrated", false)) return;
        boolean onlyNamePresent = !TextUtils.isEmpty(preferences.getString("name", ""))
                && TextUtils.isEmpty(preferences.getString("phone", ""))
                && TextUtils.isEmpty(preferences.getString("contacts", ""))
                && TextUtils.isEmpty(preferences.getString("blood_group", ""))
                && TextUtils.isEmpty(preferences.getString("medical", ""))
                && TextUtils.isEmpty(preferences.getString("vehicle", ""))
                && TextUtils.isEmpty(preferences.getString("vehicle_type", ""))
                && TextUtils.isEmpty(preferences.getString("vehicle_make_model", ""))
                && TextUtils.isEmpty(preferences.getString("vehicle_registration", ""));
        SharedPreferences.Editor editor = preferences.edit()
                .putBoolean("fast_slate_profile_migrated", true);
        if (onlyNamePresent) {
            editor.remove("name");
        }
        editor.apply();
    }


    private void editProfileField(String label, String key, String defaultValue) {
        if ("contacts".equals(key)) {
            showEmergencyContactsDialog();
        } else if ("blood_group".equals(key)) {
            showBloodGroupSelector();
        } else if ("language".equals(key)) {
            showLanguageSelector();
        } else if ("vehicle".equals(key)) {
            showVehicleEditor();
        } else if ("emergency_number_override".equals(key)) {
            showEmergencyNumberDialog();
        } else {
            showProfileEditor(label, key, defaultValue);
        }
    }

    private void showEmergencyNumberDialog() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        String override = preferences.getString("emergency_number_override", "");
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(8), dp(24), 0);

        TextView guidance = text(
                "Automatic: " + automaticEmergencyNumber() + ". " + automaticEmergencyNumberSource()
                        + ". Verify the official emergency number when travelling.",
                12, R.color.fastaid_muted, false);
        guidance.setPadding(0, 0, 0, dp(12));
        content.addView(guidance);

        TextInputLayout field = profileTextField(
                "Override number",
                override == null ? "" : override,
                InputType.TYPE_CLASS_PHONE, 1);
        content.addView(field);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Emergency number")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Automatic", (dialog, which) -> {
                    preferences.edit().remove("emergency_number_override").apply();
                    refreshProfileAfterEdit();
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    String value = inputValue(field).replaceAll("[^0-9]", "");
                    if (!value.matches("[0-9]{2,8}")) {
                        Toast.makeText(this, "Enter 2 to 8 digits.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    preferences.edit().putString("emergency_number_override", value).apply();
                    refreshProfileAfterEdit();
                })
                .show();
    }

    private String emergencyContactsSummary() {
        String contacts = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                .getString("contacts", "");
        if (contacts == null || contacts.trim().isEmpty()) return "Add trusted contacts";
        String[] entries = contacts.trim().split("\\r?\\n");
        return entries.length == 1 ? entries[0] : entries.length + " trusted contacts";
    }

    private int contactCount() {
        String contacts = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                .getString("contacts", "");
        if (contacts == null || contacts.trim().isEmpty()) return 0;
        int count = 0;
        for (String entry : contacts.trim().split("\\r?\\n")) {
            if (!entry.trim().isEmpty()) count++;
        }
        return count;
    }

    private void showEmergencyContactsDialog() {
        String contacts = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                .getString("contacts", "");
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(6), dp(24), dp(18));

        TextView current = text(
                contacts == null || contacts.trim().isEmpty()
                        ? "No trusted contacts added yet. Pick a phone number without sharing your full address book."
                        : contacts.trim(),
                13, R.color.fastaid_muted, false);
        current.setPadding(0, 0, 0, dp(14));
        content.addView(current);

        MaterialButton pickContact = profileActionButton(
                "Pick contact", R.drawable.ic_m3_person,
                color(R.color.fastaid_blue), Color.WHITE, false,
                view -> {});
        content.addView(pickContact, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        MaterialButton editManually = profileActionButton(
                "Edit manually", R.drawable.ic_m3_edit,
                Color.WHITE, color(R.color.fastaid_ink), true,
                view -> {});
        LinearLayout.LayoutParams secondaryParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        secondaryParams.setMargins(0, dp(8), 0, 0);
        content.addView(editManually, secondaryParams);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Emergency contacts")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .show();
        pickContact.setOnClickListener(view -> {
            dialog.dismiss();
            launchEmergencyContactPicker();
        });
        editManually.setOnClickListener(view -> {
            dialog.dismiss();
            showProfileEditor("Emergency contacts", "contacts", "Add trusted contacts");
        });
    }

    private void launchEmergencyContactPicker() {
        Intent pickContact = new Intent(Intent.ACTION_PICK);
        pickContact.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        try {
            emergencyContactPicker.launch(pickContact);
        } catch (android.content.ActivityNotFoundException error) {
            Toast.makeText(this, "No contacts picker is available", Toast.LENGTH_LONG).show();
        }
    }

    private void persistSelectedEmergencyContact(Uri selectedUri) {
        if (selectedUri == null) {
            Toast.makeText(this, "No contact selected", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        try (Cursor cursor = getContentResolver().query(
                selectedUri, projection, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) {
                Toast.makeText(this, "Unable to read that contact", Toast.LENGTH_LONG).show();
                return;
            }
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if (numberIndex < 0) {
                Toast.makeText(this, "That contact has no readable phone number", Toast.LENGTH_LONG).show();
                return;
            }
            String name = nameIndex >= 0 ? cursor.getString(nameIndex) : "Emergency contact";
            String number = cursor.getString(numberIndex);
            if (number == null || number.trim().isEmpty()) {
                Toast.makeText(this, "That contact has no phone number", Toast.LENGTH_LONG).show();
                return;
            }
            String safeName = name == null || name.trim().isEmpty() ? "Emergency contact" : name.trim();
            appendEmergencyContact(safeName, number.trim());
        } catch (RuntimeException error) {
            Toast.makeText(this, "Unable to import that contact", Toast.LENGTH_LONG).show();
        }
    }

    private void appendEmergencyContact(String name, String number) {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        String existing = preferences.getString("contacts", "");
        String normalized = normalizePhone(number);
        if (existing != null) {
            for (String entry : existing.split("\\r?\\n")) {
                if (!normalized.isEmpty() && normalizePhone(entry).endsWith(normalized)) {
                    Toast.makeText(this, "That contact is already added", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        String entry = name + " - " + number;
        String updated = existing == null || existing.trim().isEmpty()
                ? entry : existing.trim() + "\n" + entry;
        preferences.edit().putString("contacts", updated).apply();
        refreshProfileAfterEdit();
        Toast.makeText(this, name + " added as an emergency contact", Toast.LENGTH_SHORT).show();
    }

    private String normalizePhone(String value) {
        return value == null ? "" : value.replaceAll("[^0-9]", "");
    }

    private void showBloodGroupSelector() {
        String[] bloodGroups = getResources().getStringArray(R.array.blood_groups);
        String current = profileValue("blood_group", "Not set");
        new MaterialAlertDialogBuilder(this)
                .setTitle("Blood group")
                .setSingleChoiceItems(bloodGroups, optionIndex(bloodGroups, current), (dialog, which) -> {
                    getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                            .edit().putString("blood_group", bloodGroups[which]).apply();
                    dialog.dismiss();
                    refreshProfileAfterEdit();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showLanguageSelector() {
        String[] languages = UiTranslations.languagesForCountry(detectedCountryCode());
        String current = preferredLanguage();
        String[] selectedLanguage = {current};

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), 0, dp(24), 0);

        if (!"IN".equals(detectedCountryCode())) {
            TextView guidance = text(
                    "Regional language packs are shown after FastAid identifies a supported country.",
                    13, R.color.fastaid_muted, false);
            guidance.setPadding(0, 0, 0, dp(10));
            content.addView(guidance);
        }

        ScrollView pickerScroll = new ScrollView(this);
        pickerScroll.setFillViewport(false);
        LinearLayout pickerList = new LinearLayout(this);
        pickerList.setOrientation(LinearLayout.VERTICAL);
        List<RadioButton> buttons = new ArrayList<>();
        for (String language : languages) {
            RadioButton option = new RadioButton(this);
            option.setText(language);
            option.setTextSize(18);
            option.setTextColor(color(R.color.fastaid_ink));
            option.setMinHeight(dp(52));
            option.setGravity(Gravity.CENTER_VERTICAL);
            option.setPadding(0, dp(6), 0, dp(6));
            option.setButtonTintList(ColorStateList.valueOf(color(R.color.fastaid_blue)));
            option.setChecked(language.equals(current));
            option.setOnClickListener(view -> {
                selectedLanguage[0] = language;
                for (RadioButton button : buttons) {
                    button.setChecked(button == option);
                }
            });
            buttons.add(option);
            pickerList.addView(option, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        pickerScroll.addView(pickerList);
        content.addView(pickerScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                "IN".equals(detectedCountryCode()) ? dp(460) : LinearLayout.LayoutParams.WRAP_CONTENT));

        new MaterialAlertDialogBuilder(this)
                .setTitle("Preferred language")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dialog, which) -> {
                    getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                            .edit().putString("language", selectedLanguage[0]).apply();
                    recreate();
                })
                .show();
    }

    private int optionIndex(String[] options, String current) {
        for (int index = 0; index < options.length; index++) {
            if (options[index].equals(current)) return index;
        }
        return 0;
    }

    private void showVehicleEditor() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(24), dp(8), dp(24), dp(8));

        TextView guidance = text(
                "These details can help responders identify your vehicle and prepare suitable roadside support.",
                12, R.color.fastaid_muted, false);
        guidance.setPadding(0, 0, 0, dp(12));
        fields.addView(guidance);

        TextInputLayout typeField = profileDropdownField(
                "Vehicle type", R.array.vehicle_types, preferences.getString("vehicle_type", ""));
        TextInputLayout makeField = profileTextField(
                "Make and model", preferences.getString("vehicle_make_model", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, 1);
        makeField.setHelperText("Example: Honda Activa or Hyundai i20.");
        TextInputLayout registrationField = profileTextField(
                "Registration number", preferences.getString("vehicle_registration", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS, 1);
        registrationField.setHelperText("Use the plate number responders can see.");
        TextInputLayout colorField = profileTextField(
                "Colour", preferences.getString("vehicle_color", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS, 1);
        TextInputLayout fuelField = profileDropdownField(
                "Fuel or powertrain", R.array.vehicle_fuels, preferences.getString("vehicle_fuel", ""));
        TextInputLayout insuranceField = profileTextField(
                "Insurance provider and policy", preferences.getString("vehicle_insurance", ""),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE, 2);
        insuranceField.setHelperText("Optional. Keep it brief enough for roadside handoff.");

        fields.addView(typeField, profileFieldParams(0));
        fields.addView(makeField, profileFieldParams(dp(10)));
        fields.addView(registrationField, profileFieldParams(dp(10)));
        fields.addView(colorField, profileFieldParams(dp(10)));
        fields.addView(fuelField, profileFieldParams(dp(10)));
        fields.addView(insuranceField, profileFieldParams(dp(10)));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(fields, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT, ScrollView.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Vehicle details")
                .setView(scroll)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String type = inputValue(typeField);
                    String makeModel = inputValue(makeField);
                    String registration = inputValue(registrationField).toUpperCase(Locale.US);
                    String vehicleColor = inputValue(colorField);
                    String fuel = inputValue(fuelField);
                    String insurance = inputValue(insuranceField);
                    if (!validateVehicleDetails(makeField, makeModel, registrationField, registration)) {
                        return;
                    }
                    String summary = composeVehicleSummary(type, makeModel, registration);
                    boolean hasDetails = !type.isEmpty() || !makeModel.isEmpty()
                            || !registration.isEmpty() || !vehicleColor.isEmpty()
                            || !fuel.isEmpty() || !insurance.isEmpty();
                    if (summary.isEmpty() && hasDetails) summary = "Vehicle details saved";

                    SharedPreferences.Editor editor = preferences.edit();
                    putOrRemove(editor, "vehicle_type", type);
                    putOrRemove(editor, "vehicle_make_model", makeModel);
                    putOrRemove(editor, "vehicle_registration", registration);
                    putOrRemove(editor, "vehicle_color", vehicleColor);
                    putOrRemove(editor, "vehicle_fuel", fuel);
                    putOrRemove(editor, "vehicle_insurance", insurance);
                    putOrRemove(editor, "vehicle", summary);
                    editor.apply();
                    refreshProfileAfterEdit();
                    dialog.dismiss();
                });
    }

    private TextInputLayout profileDropdownField(String label, int optionsResource, String value) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(label);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxCornerRadii(dp(8), dp(8), dp(8), dp(8));
        layout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);

        MaterialAutoCompleteTextView input = new MaterialAutoCompleteTextView(layout.getContext());
        input.setInputType(InputType.TYPE_NULL);
        input.setContentDescription(label);
        input.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                getResources().getStringArray(optionsResource)));
        input.setText(value == null ? "" : value, false);
        input.setOnClickListener(view -> input.showDropDown());
        layout.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return layout;
    }

    private TextInputLayout profileTextField(
            String label,
            String value,
            int inputType,
            int minimumLines
    ) {
        TextInputLayout layout = new TextInputLayout(this);
        layout.setHint(label);
        layout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        layout.setBoxCornerRadii(dp(8), dp(8), dp(8), dp(8));

        TextInputEditText input = new TextInputEditText(layout.getContext());
        input.setInputType(inputType);
        input.setText(value == null ? "" : value);
        input.setMinLines(minimumLines);
        input.setSingleLine(minimumLines == 1);
        if (minimumLines > 1) input.setGravity(Gravity.TOP | Gravity.START);
        layout.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return layout;
    }

    private LinearLayout.LayoutParams profileFieldParams(int topMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, topMargin, 0, 0);
        return params;
    }

    private String inputValue(TextInputLayout field) {
        EditText input = field.getEditText();
        return input == null || input.getText() == null ? "" : input.getText().toString().trim();
    }

    private boolean validateVehicleDetails(
            TextInputLayout makeField,
            String makeModel,
            TextInputLayout registrationField,
            String registration
    ) {
        makeField.setError(null);
        registrationField.setError(null);
        if (!makeModel.isEmpty() && compactAlnumLength(makeModel) < 3) {
            makeField.setError("Add a recognizable make or model, or leave this blank.");
            return false;
        }
        String compactRegistration = registration.replaceAll("[^A-Z0-9]", "");
        if (!compactRegistration.isEmpty()) {
            if (compactRegistration.length() < 6 || compactRegistration.length() > 13) {
                registrationField.setError("Plate number looks incomplete. Add the full plate or leave it blank.");
                return false;
            }
            if ("IN".equals(detectedCountryCode())
                    && !compactRegistration.matches("[A-Z]{2}[0-9]{1,2}[A-Z]{0,3}[0-9]{4}")) {
                registrationField.setError("For India, use a plate like KA 05 AB 1234.");
                return false;
            }
        }
        return true;
    }

    private int compactAlnumLength(String value) {
        if (value == null) return 0;
        return value.replaceAll("[^A-Za-z0-9]", "").length();
    }

    private void putOrRemove(SharedPreferences.Editor editor, String key, String value) {
        if (value == null || value.trim().isEmpty()) editor.remove(key);
        else editor.putString(key, value.trim());
    }

    private String vehicleSummary() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        String summary = composeVehicleSummary(
                preferences.getString("vehicle_type", ""),
                preferences.getString("vehicle_make_model", ""),
                preferences.getString("vehicle_registration", ""));
        if (!summary.isEmpty()) return summary;
        String legacy = preferences.getString("vehicle", "");
        return legacy == null || legacy.trim().isEmpty()
                ? "Add type, registration and insurance" : legacy.trim();
    }

    private String composeVehicleSummary(String type, String makeModel, String registration) {
        List<String> parts = new ArrayList<>();
        if (type != null && !type.trim().isEmpty()) parts.add(type.trim());
        if (makeModel != null && !makeModel.trim().isEmpty()) parts.add(makeModel.trim());
        if (registration != null && !registration.trim().isEmpty()) parts.add(registration.trim());
        return TextUtils.join(" - ", parts);
    }

    private void refreshProfileAfterEdit() {
        populateProfileContent();
        refreshBottomNav();
    }
    private void showProfileEditor(String label, String key, String defaultValue) {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        String savedValue = preferences.getString(key, null);
        String initialValue = savedValue == null
                ? ("language".equals(key) ? defaultValue : "")
                : savedValue;

        TextInputLayout field = new TextInputLayout(this);
        field.setHint(label);
        field.setPlaceholderText(defaultValue);
        field.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        field.setBoxCornerRadii(dp(8), dp(8), dp(8), dp(8));
        String helperText = profileEditorHelperText(key);
        if (!helperText.isEmpty()) field.setHelperText(helperText);

        TextInputEditText input = new TextInputEditText(field.getContext());
        input.setText(initialValue);
        if (!initialValue.isEmpty()) input.setSelection(initialValue.length());
        boolean multiline = "contacts".equals(key) || "medical".equals(key);
        input.setSingleLine(!multiline);
        if (multiline) {
            input.setMinLines(3);
            input.setGravity(Gravity.TOP | Gravity.START);
            input.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        } else if ("phone".equals(key)) {
            input.setInputType(InputType.TYPE_CLASS_PHONE);
        } else {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        }
        field.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout dialogContent = new LinearLayout(this);
        dialogContent.setOrientation(LinearLayout.VERTICAL);
        dialogContent.setPadding(dp(24), dp(8), dp(24), 0);
        dialogContent.addView(field);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Edit " + label)
                .setView(dialogContent)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String value = input.getText() == null ? "" : input.getText().toString().trim();
                    field.setError(null);
                    if (!validateProfileEditorValue(key, value, field)) {
                        return;
                    }
                    SharedPreferences.Editor editor = preferences.edit();
                    if (value.isEmpty() && !"language".equals(key)) {
                        editor.remove(key);
                    } else {
                        editor.putString(key, value.isEmpty() ? defaultValue : value);
                    }
                    editor.apply();
                    populateProfileContent();
                    refreshBottomNav();
                    dialog.dismiss();
                });
    }

    private String profileEditorHelperText(String key) {
        if ("name".equals(key)) return "Use the name responders should hear at handoff.";
        if ("phone".equals(key)) return "Include country code if available. Stored only on this device.";
        if ("contacts".equals(key)) return "Add trusted contacts with names and phone numbers.";
        if ("medical".equals(key)) return "Keep allergies, medication, and major conditions short and scannable.";
        return "";
    }

    private boolean validateProfileEditorValue(String key, String value, TextInputLayout field) {
        if (value == null || value.trim().isEmpty()) return true;
        if ("name".equals(key)) {
            if (compactAlnumLength(value) < 2
                    || DEFAULT_PROFILE_NAME.equalsIgnoreCase(value.trim())
                    || LEGACY_DEFAULT_PROFILE_NAME.equalsIgnoreCase(value.trim())) {
                field.setError("Add a real handoff name or leave this blank.");
                return false;
            }
        }
        if ("phone".equals(key)) {
            String digits = value.replaceAll("[^0-9]", "");
            if (digits.length() < 7 || digits.length() > 15) {
                field.setError("Enter a reachable phone number with 7 to 15 digits.");
                return false;
            }
        }
        if ("contacts".equals(key)) {
            String digits = value.replaceAll("[^0-9]", "");
            if (!digits.isEmpty() && digits.length() < 7) {
                field.setError("Contact number looks incomplete. Add the full number or remove it.");
                return false;
            }
        }
        return true;
    }

    private void applyManualLocation(double latitude, double longitude, String label) {
        if (!EmergencyLocationPolicy.hasValidCoordinates(latitude, longitude)) {
            Toast.makeText(this, "Enter a valid latitude and longitude.", Toast.LENGTH_LONG).show();
            return;
        }
        currentLatitude = latitude;
        currentLongitude = longitude;
        currentLocationTimestamp = System.currentTimeMillis();
        currentLocationAccuracy = Float.NaN;
        currentLocationSource = label == null || label.trim().isEmpty()
                ? "Manual location" : "Manual: " + label.trim();
        saveLastKnownLocation();
        updateLocationLabel(currentLocationSource);
        updateGoogleMap();
        updateCountryFromLocationAsync(latitude, longitude);
        fetchNearbyAid();
        if (pendingSosAfterLocation) {
            pendingSosAfterLocation = false;
            startSosCountdown();
        }
    }

    private void showManualLocationDialog() {
        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(24), dp(8), dp(24), 0);
        TextInputLayout addressLayout = profileTextField(
                "Address or landmark", "", InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, 1);
        TextInputLayout coordinatesLayout = profileTextField(
                "Latitude, longitude (optional)", "", InputType.TYPE_CLASS_TEXT, 1);
        fields.addView(addressLayout, profileFieldParams(0));
        fields.addView(coordinatesLayout, profileFieldParams(dp(10)));

        new MaterialAlertDialogBuilder(this)
                .setTitle(ui("Enter location"))
                .setMessage("Enter coordinates for the most reliable result, or enter an address to geocode.")
                .setView(fields)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Use location", (dialog, which) -> {
                    String address = inputValue(addressLayout);
                    String coordinates = inputValue(coordinatesLayout);
                    if (!coordinates.isEmpty()) {
                        String[] parts = coordinates.split(",");
                        if (parts.length == 2) {
                            try {
                                applyManualLocation(Double.parseDouble(parts[0].trim()),
                                        Double.parseDouble(parts[1].trim()), address);
                                return;
                            } catch (NumberFormatException ignored) {
                                // Try the address below.
                            }
                        }
                    }
                    if (address.isEmpty()) {
                        Toast.makeText(this, "Enter an address or coordinates.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    resolveManualAddress(address);
                })
                .show();
    }

    private void resolveManualAddress(String query) {
        setStatus("Finding entered location...");
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> matches = geocoder.getFromLocationName(query, 1);
                if (matches == null || matches.isEmpty()) throw new IllegalStateException();
                Address match = matches.get(0);
                runOnUiThread(() -> applyManualLocation(
                        match.getLatitude(), match.getLongitude(), query));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    setStatus("Entered location could not be found");
                    Toast.makeText(this,
                            "Location not found. Try latitude and longitude or use GPS.",
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    private void startVoiceCommand() {
        if (!settingEnabled("voice_commands", true)) {
            Toast.makeText(this, "Enable voice commands in Profile preferences.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,
                UiTranslations.localeTag(preferredLanguage()));
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Say SOS, call emergency, share location, or an aid type");
        try {
            speechRecognizer.launch(intent);
        } catch (android.content.ActivityNotFoundException error) {
            Toast.makeText(this, "Voice recognition is unavailable.", Toast.LENGTH_LONG).show();
        }
    }

    private void handleVoiceCommand(String spoken) {
        String command = spoken == null ? "" : spoken.toLowerCase(Locale.ROOT);
        setStatus("Voice command: " + spoken);
        if (command.contains("call")) {
            callEmergency();
            return;
        }
        if (command.contains("share")) {
            shareCurrentLocation();
            return;
        }
        if (command.contains("location") || command.contains("where")) {
            requestLocation();
            return;
        }
        if (command.contains("fuel") || command.contains("petrol")) incidentType = "fuel";
        else if (command.contains("tyre") || command.contains("tire")) incidentType = "tyre";
        else if (command.contains("repair") || command.contains("breakdown")) incidentType = "breakdown";
        else if (command.contains("medical") || command.contains("hospital")
                || command.contains("clinic")) incidentType = "medical";
        else if (command.contains("police")) incidentType = "police";
        else if (command.contains("fire")) incidentType = "fire";
        else if (command.contains("sos") || command.contains("help")
                || command.contains("emergency") || command.contains("accident")) {
            startSosCountdown();
            return;
        } else {
            Toast.makeText(this, "Command not recognized.", Toast.LENGTH_LONG).show();
            return;
        }
        selectedCategoryLabel = categoryLabelForIncidentType(incidentType);
        populateNearbyContent();
        refreshBottomNav();
        fetchNearbyAid();
    }

    private void showQuickActionsMenu() {
        String[] actions = {
                ui("Use current location"), ui("Enter location"), ui("Voice command"),
                ui("Call emergency"), ui("Profile")
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle("Quick actions")
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) requestLocation();
                    else if (which == 1) showManualLocationDialog();
                    else if (which == 2) startVoiceCommand();
                    else if (which == 3) callEmergency();
                    else {
                        populateProfileContent();
                        refreshBottomNav();
                    }
                })
                .show();
    }

    private void showIncidentNotSentDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("No responder was notified")
                .setMessage("The request is saved only on this device. "
                        + "Call an official emergency number or prepare an SMS now.")
                .setNegativeButton("Keep saved", null)
                .setNeutralButton("Prepare SMS", (dialog, which) -> prepareSmsSos())
                .setPositiveButton("Call " + emergencyNumber(),
                        (dialog, which) -> callEmergency())
                .show();
    }

    private void refreshAfterIncidentChange() {
        if ("Incidents".equals(activeTab)) populateIncidentsContent();
        else if ("Profile".equals(activeTab)) populateProfileContent();
        else populateHomeContent();
        refreshBottomNav();
    }
    private String preferredLanguage() {
        String savedLanguage = profileValue("language", "English");
        return UiTranslations.isLanguageAllowedForCountry(savedLanguage, detectedCountryCode())
                ? savedLanguage
                : "English";
    }

    private String ui(String english) {
        return UiTranslations.translate(preferredLanguage(), english);
    }

    private boolean settingEnabled(String key, boolean defaultValue) {
        return getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                .getBoolean(key, defaultValue);
    }

    private int controlSize() {
        return dp(settingEnabled("large_controls", false) ? 56 : 48);
    }

    private boolean hasCurrentCoordinates() {
        return EmergencyLocationPolicy.hasValidCoordinates(currentLatitude, currentLongitude);
    }

    private boolean hasFreshSosLocation() {
        return EmergencyLocationPolicy.canSendSos(
                currentLatitude, currentLongitude, currentLocationTimestamp, System.currentTimeMillis());
    }

    private boolean hasActiveIncident() {
        return !"None".equals(activeIncidentStatus);
    }

    private String coordinateSummary() {
        if (!hasCurrentCoordinates()) return "Location unavailable";
        return String.format(Locale.US, "%.5f, %.5f", currentLatitude, currentLongitude);
    }

    private String locationAgeText() {
        if (currentLocationTimestamp <= 0L) return "time unknown";
        long ageSeconds = Math.max(0L,
                (System.currentTimeMillis() - currentLocationTimestamp) / 1000L);
        if (ageSeconds < 60L) return ageSeconds + " sec old";
        long minutes = ageSeconds / 60L;
        if (minutes < 60L) return minutes + " min old";
        return (minutes / 60L) + " hr old";
    }

    private String emergencyNumber() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        String override = preferences.getString("emergency_number_override", "");
        if (override != null && !override.trim().isEmpty()) return override.trim();
        return automaticEmergencyNumber();
    }

    private String automaticEmergencyNumber() {
        String detected = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                .getString("detected_emergency_number", "");
        if (detected != null && !detected.trim().isEmpty()) return detected.trim();
        return getResources().getString(R.string.emergency_number);
    }

    private String emergencyNumberSource() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        String override = preferences.getString("emergency_number_override", "");
        if (override != null && !override.trim().isEmpty()) return "Profile override";
        return automaticEmergencyNumberSource();
    }

    private String automaticEmergencyNumberSource() {
        SharedPreferences preferences = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE);
        String country = preferences.getString("detected_country_name", "");
        String basis = preferences.getString("detected_country_basis", "");
        if (country == null || country.trim().isEmpty()) {
            return "Fallback; verify for your country";
        }
        return "Detected for " + country
                + (basis == null || basis.trim().isEmpty() ? "" : " from " + basis);
    }

    private String detectedCountryCode() {
        String countryCode = getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE)
                .getString("detected_country_code", "");
        return countryCode == null ? "" : countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private void updateEmergencyNumberFromDeviceCountry() {
        String countryCode = "";
        String basis = "";
        try {
            TelephonyManager telephony =
                    (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            if (telephony != null) {
                countryCode = telephony.getNetworkCountryIso();
                basis = "mobile network";
                if (countryCode == null || countryCode.trim().isEmpty()) {
                    countryCode = telephony.getSimCountryIso();
                    basis = "SIM";
                }
            }
        } catch (RuntimeException ignored) {
            countryCode = "";
        }
        if (countryCode == null || countryCode.trim().isEmpty()) {
            countryCode = Locale.getDefault().getCountry();
            basis = "device region";
        }
        String number = EmergencyNumberResolver.resolve(countryCode);
        if (number == null) return;
        String normalizedCountry = countryCode.trim().toUpperCase(Locale.ROOT);
        String countryName = new Locale("", normalizedCountry).getDisplayCountry();
        getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE).edit()
                .putString("detected_emergency_number", number)
                .putString("detected_country_code", normalizedCountry)
                .putString("detected_country_name", countryName)
                .putString("detected_country_basis", basis)
                .apply();
    }

    private void callEmergency() {
        openDialer(emergencyNumber(), emergencyNumberSource());
    }

    private void callDirectEmergency(String number, String label) {
        openDialer(number, label);
    }

    private void openDialer(String number, String label) {
        String cleanNumber = number == null ? "" : number.trim();
        if (cleanNumber.isEmpty()) {
            Toast.makeText(this, "No phone number available.", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(cleanNumber)));
        TelecomManager telecomManager = (TelecomManager) getSystemService(TELECOM_SERVICE);
        String defaultDialerPackage = telecomManager == null ? null : telecomManager.getDefaultDialerPackage();
        if (!TextUtils.isEmpty(defaultDialerPackage)) {
            intent.setPackage(defaultDialerPackage);
        }
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this, "No phone dialer found for " + cleanNumber + ".", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this,
                "Opening " + cleanNumber + " - " + label,
                Toast.LENGTH_LONG).show();
        startActivity(intent);
    }

    private void callBrosephBengaluru() {
        if (!isBangaloreLocation()) {
            Toast.makeText(this,
                    "St Broseph shortcut is shown only for Bengaluru locations.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        callDirectEmergency(BENGALURU_BROSEPH_NUMBER, "St Broseph Bengaluru community support");
    }

    private boolean isBangaloreLocation() {
        if (!hasCurrentCoordinates()) return false;
        return currentLatitude >= 12.75 && currentLatitude <= 13.20
                && currentLongitude >= 77.35 && currentLongitude <= 77.85;
    }

    private void setStatus(String message) {
        if (statusText != null) setUiText(statusText, message);
    }

    private void applyLocation(Location location, String source) {
        if (location == null) return;
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        currentLocationTimestamp = location.getTime() > 0L
                ? location.getTime() : System.currentTimeMillis();
        currentLocationAccuracy = location.hasAccuracy() ? location.getAccuracy() : Float.NaN;
        currentLocationSource = source;
        saveLastKnownLocation();
        updateLocationLabel(source);
        updateCountryFromLocationAsync(currentLatitude, currentLongitude);
        if ("SOS".equals(activeTab) && !sosCountdownActive && contentRoot != null) {
            populateHomeContent();
            refreshBottomNav();
        }
    }

    private void updateCountryFromLocationAsync(double latitude, double longitude) {
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> matches = geocoder.getFromLocation(latitude, longitude, 1);
                if (matches == null || matches.isEmpty()) return;
                Address address = matches.get(0);
                String number = EmergencyNumberResolver.resolve(address.getCountryCode());
                if (number == null) return;
                getSharedPreferences(PROFILE_PREFS_NAME, MODE_PRIVATE).edit()
                        .putString("detected_emergency_number", number)
                        .putString("detected_country_code",
                                address.getCountryCode() == null
                                        ? "" : address.getCountryCode().toUpperCase(Locale.ROOT))
                        .putString("detected_country_name",
                                address.getCountryName() == null ? "" : address.getCountryName())
                        .putString("detected_country_basis", "current location")
                        .apply();
                runOnUiThread(() -> {
                    if ("Profile".equals(activeTab)) populateProfileContent();
                    if ("SOS".equals(activeTab) && !sosCountdownActive) populateHomeContent();
                });
            } catch (Exception ignored) {
                // Keep the explicit profile override or visible fallback number.
            }
        });
    }
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        googleMap.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof AidPlace) navigateToPlace((AidPlace) tag);
        });
        googleMap.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof AidPlace) {
                selectMapPlace((AidPlace) tag, true);
                return true;
            }
            return false;
        });
        enableMyLocationOnMap();
        updateGoogleMap();
    }

    private void enableMyLocationOnMap() {
        if (googleMap == null) {
            return;
        }
        boolean hasFine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!hasFine && !hasCoarse) {
            return;
        }
        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException ignored) {
            // Permission can change while the map is initializing.
        }
    }

    private void updateGoogleMap() {
        if (mapPreviewView != null && hasCurrentCoordinates()) {
            mapPreviewView.setPlaces(places, currentLatitude, currentLongitude);
        }
        if (googleMap == null) {
            return;
        }
        googleMap.clear();
        if (!hasCurrentCoordinates()) {
            selectedMapPlace = null;
            updateMapPlaceSheet();
            return;
        }
        LatLng current = new LatLng(currentLatitude, currentLongitude);
        com.google.android.gms.maps.model.LatLngBounds.Builder boundsBuilder =
                new com.google.android.gms.maps.model.LatLngBounds.Builder();
        boundsBuilder.include(current);
        int includedMarkerCount = 1;

        googleMap.addMarker(new MarkerOptions()
                .position(current)
                .anchor(0.5f, 0.5f)
                .title("You are here")
                .snippet(String.format(Locale.US, "%.5f, %.5f", currentLatitude, currentLongitude))
                .icon(mapTextMarker("ME", color(R.color.fastaid_blue), Color.WHITE, 44)));

        int markerCount = Math.min(places.size(), 10);
        for (int index = 0; index < markerCount; index++) {
            AidPlace place = places.get(index);
            if (!place.hasCoordinates()) {
                continue;
            }
            LatLng placePosition = new LatLng(place.latitude, place.longitude);
            boundsBuilder.include(placePosition);
            includedMarkerCount++;
            Marker marker = googleMap.addMarker(new MarkerOptions()
                    .position(placePosition)
                    .anchor(0.5f, 0.5f)
                    .title(place.name)
                    .snippet(place.distance + " - " + place.openText)
                    .icon(mapTextMarker(placeIcon(place), placeColor(place), Color.WHITE, 38)));
            if (marker != null) marker.setTag(place);
        }

        fitMapToVisibleAid(boundsBuilder, includedMarkerCount, current);
        updateMapPlaceSheet();
    }

    private void selectMapPlace(AidPlace place, boolean focusMap) {
        selectedMapPlace = place;
        updateMapPlaceSheet();
        if (!focusMap || googleMap == null || place == null || !place.hasCoordinates()) return;
        LatLng position = new LatLng(place.latitude, place.longitude);
        if (settingEnabled("reduce_motion", false)) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15.5f));
        } else {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15.5f));
        }
    }

    private void fitMapToVisibleAid(com.google.android.gms.maps.model.LatLngBounds.Builder boundsBuilder, int includedMarkerCount, LatLng fallback) {
        if (includedMarkerCount <= 1) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 14.5f));
            return;
        }

        try {
            if (settingEnabled("reduce_motion", false)) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), dp(58)));
            } else {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), dp(58)));
            }
        } catch (IllegalStateException error) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(fallback, 14.5f));
        }
    }


    private BitmapDescriptor mapTextMarker(String label, int backgroundColor, int textColor, int sizeDp) {
        int size = dp(sizeDp);
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);

        android.graphics.Paint fill = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        fill.setColor(backgroundColor);
        canvas.drawCircle(size / 2f, size / 2f, size * 0.46f, fill);

        android.graphics.Paint border = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        border.setStyle(android.graphics.Paint.Style.STROKE);
        border.setStrokeWidth(Math.max(2f, size * 0.06f));
        border.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size * 0.42f, border);

        android.graphics.Paint textPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(android.graphics.Paint.Align.CENTER);
        textPaint.setTextSize(size * (label.length() > 1 ? 0.30f : 0.42f));
        android.graphics.Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float baseline = size / 2f - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(label, size / 2f, baseline, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    private void requestLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION);
            return;
        }
        enableMyLocationOnMap();
        loadCurrentLocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_LOCATION) {
            return;
        }

        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }

        if (granted) {
            enableMyLocationOnMap();
            loadCurrentLocation();
        } else {
            if (EmergencyLocationPolicy.canSearchNearby(
                    currentLatitude, currentLongitude, currentLocationTimestamp,
                    System.currentTimeMillis())) {
                Toast.makeText(this,
                        "Location permission denied. Using the recent location saved on this device.",
                        Toast.LENGTH_LONG).show();
                setStatus("Location permission denied - using a recent saved location");
                fetchNearbyAid();
            } else {
                setStatus("Location permission denied - enter a location or call "
                        + emergencyNumber());
                Toast.makeText(this,
                        "No usable saved location. Enter a location or call "
                                + emergencyNumber() + ".",
                        Toast.LENGTH_LONG).show();
                if (pendingSosAfterLocation) showPendingSosLocationOptions(
                        "Location permission was denied.");
            }
        }
    }

    private void loadCurrentLocation() {
        try {
            LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
            Location best = getBestLastKnownLocation(manager);
            boolean requestedFreshFix = requestFreshLocation(manager);

            if (best != null) {
                applyLocation(best, requestedFreshFix ? "Recent fix; waiting live" : "Current location");
                updateLocationLabel(currentLocationSource);
                updateGoogleMap();
                fetchNearbyAid();
                return;
            }

            if (requestedFreshFix) {
                setUiText(statusText, "Waiting for a live location fix...");
                return;
            }

            Toast.makeText(this, "Turn on location services, then retry.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } catch (SecurityException ignored) {
            Toast.makeText(this, "Location permission is required.", Toast.LENGTH_LONG).show();
        }
    }

    private Location getBestLastKnownLocation(LocationManager manager) {
        Location best = null;
        try {
            for (String provider : manager.getProviders(true)) {
                Location candidate = manager.getLastKnownLocation(provider);
                if (candidate == null) continue;
                if (best == null || EmergencyLocationPolicy.isBetterFix(
                        candidate.getTime(), candidate.hasAccuracy() ? candidate.getAccuracy() : Float.NaN,
                        best.getTime(), best.hasAccuracy() ? best.getAccuracy() : Float.NaN)) {
                    best = candidate;
                }
            }
        } catch (SecurityException ignored) {
            return null;
        }
        return best;
    }

    private boolean requestFreshLocation(LocationManager manager) {
        String provider = chooseLiveProvider(manager);
        if (provider == null) {
            return false;
        }

        removePendingLocationListener();
        pendingLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                applyLocation(location, "Live GPS");
                updateGoogleMap();
                setStatus("Live location acquired");
                removePendingLocationListener();
                fetchNearbyAid();
                if (pendingSosAfterLocation) {
                    pendingSosAfterLocation = false;
                    startSosCountdown();
                }
            }

            @Override
            public void onProviderDisabled(String disabledProvider) {
                setUiText(statusText, "Location provider disabled. Using last known location.");
            }

            @Override
            public void onProviderEnabled(String enabledProvider) {
                setUiText(statusText, "Location provider enabled");
            }

            @Override
            public void onStatusChanged(String providerName, int status, Bundle extras) {
                // Kept for compatibility with older Android SDK listener contracts.
            }
        };

        try {
            manager.requestSingleUpdate(provider, pendingLocationListener, getMainLooper());
            locationTimeoutRunnable = () -> {
                if (pendingLocationListener == null) return;
                removePendingLocationListener();
                setStatus("Live location timed out - enter a location or retry GPS");
                if (pendingSosAfterLocation) {
                    showPendingSosLocationOptions(
                            "A fresh GPS fix was not available within 15 seconds.");
                }
            };
            mainHandler.postDelayed(locationTimeoutRunnable, 15000L);
            return true;
        } catch (SecurityException ignored) {
            return false;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String chooseLiveProvider(LocationManager manager) {
        if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return LocationManager.GPS_PROVIDER;
        }
        if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return LocationManager.NETWORK_PROVIDER;
        }
        List<String> providers = manager.getProviders(true);
        return providers.isEmpty() ? null : providers.get(0);
    }

    private void removePendingLocationListener() {
        if (locationTimeoutRunnable != null) {
            mainHandler.removeCallbacks(locationTimeoutRunnable);
            locationTimeoutRunnable = null;
        }
        if (pendingLocationListener == null) {
            return;
        }
        try {
            LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
            manager.removeUpdates(pendingLocationListener);
        } catch (SecurityException ignored) {
            // Nothing to remove when permission is unavailable.
        }
        pendingLocationListener = null;
    }

    private void showPendingSosLocationOptions(String reason) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Location needed for FastAid SOS")
                .setMessage(reason + " FastAid will not send stale or invented coordinates.")
                .setNegativeButton("Cancel SOS", (dialog, which) ->
                        pendingSosAfterLocation = false)
                .setNeutralButton("Call " + emergencyNumber(), (dialog, which) -> {
                    pendingSosAfterLocation = false;
                    callEmergency();
                })
                .setPositiveButton("Enter location", (dialog, which) ->
                        showManualLocationDialog())
                .show();
    }

    private void fetchNearbyAid() {
        if (!EmergencyLocationPolicy.canSearchNearby(
                currentLatitude, currentLongitude, currentLocationTimestamp,
                System.currentTimeMillis())) {
            places.clear();
            updateGoogleMap();
            if (resultsList != null) renderRecoveryState();
            setBusy(false, hasCurrentCoordinates()
                    ? "Location is too old - refresh GPS or enter a location"
                    : "Choose a current or entered location to find nearby aid");
            return;
        }

        places.clear();
        updateGoogleMap();
        renderSearchingState();
        setBusy(true, "Finding nearby aid...");
        if (placesRepository != null) {
            placesRepository.search(currentLatitude, currentLongitude, incidentType,
                    new GooglePlacesRepository.Callback() {
                        @Override
                        public void onSuccess(List<AidPlace> fetched) {
                            if (fetched == null || fetched.isEmpty()) {
                                if (settingEnabled("offline_cache", true)) placeCache.clear();
                                consumeNearbyResults(new ArrayList<>(), "Live Google Places");
                                return;
                            }
                            if (settingEnabled("offline_cache", true)) placeCache.save(fetched);
                            else placeCache.clear();
                            consumeNearbyResults(fetched, "Live Google Places");
                        }

                        @Override
                        public void onFailure(Exception error) {
                            fetchNearbyAidFromBackend(error);
                        }
                    });
            return;
        }
        fetchNearbyAidFromBackend(
                new IllegalStateException("Native Places key is not configured"));
    }

    private void fetchNearbyAidFromBackend(Exception nativeError) {
        if (apiClient == null || !apiClient.isConfigured()) {
            showCachedPlacesOrRecovery(nativeError,
                    new IllegalStateException("No backend URL is configured"));
            return;
        }
        executor.execute(() -> {
            try {
                List<AidPlace> fetched = apiClient.fetchNearbyAid(
                        currentLatitude, currentLongitude, incidentType);
                if (fetched == null || fetched.isEmpty()) {
                    throw new IllegalStateException("No live backend Places results");
                }
                boolean syncedQueuedIncident = syncQueuedIncidentIfNeeded();
                runOnUiThread(() -> {
                    if (settingEnabled("offline_cache", true)) placeCache.save(fetched);
                    else placeCache.clear();
                    consumeNearbyResults(fetched, syncedQueuedIncident
                            ? "Live Google Places - saved request sent"
                            : "Live Google Places");
                });
            } catch (Exception backendError) {
                runOnUiThread(() -> showCachedPlacesOrRecovery(nativeError, backendError));
            }
        });
    }

    private void consumeNearbyResults(List<AidPlace> fetched, String sourceLabel) {
        places.clear();
        places.addAll(fetched);
        renderPlaces();
        setBusy(false, sourceLabel + " - " + fetched.size() + " nearby options");
    }

    private void showCachedPlacesOrRecovery(Exception nativeError, Exception backendError) {
        if (!settingEnabled("offline_cache", true)) {
            placeCache.clear();
            places.clear();
            updateGoogleMap();
            renderRecoveryState();
            setBusy(false, "Live nearby data unavailable - offline cache is off");
            return;
        }
        AidPlaceCache.Snapshot snapshot = placeCache.load();
        if (!snapshot.places.isEmpty()) {
            places.clear();
            places.addAll(snapshot.places);
            renderPlaces();
            String freshness = snapshot.isStale() ? "older saved" : "recently saved";
            setBusy(false, "Offline - showing " + freshness
                    + " places; opening hours need a live refresh");
            return;
        }
        places.clear();
        updateGoogleMap();
        renderRecoveryState();
        setBusy(false, "Nearby data unavailable - emergency calling still works");
    }

    private void renderSearchingState() {
        if (resultsList == null || nearbyCountText == null) {
            return;
        }
        resultsList.removeAllViews();
        setUiText(nearbyCountText, "Searching");
        TextView loadingCopy = text("Searching live Places for "
                        + categoryLabelForIncidentType(incidentType).toLowerCase(Locale.US)
                        + " nearby...",
                14, R.color.fastaid_muted, false);
        loadingCopy.setPadding(0, dp(14), 0, dp(14));
        resultsList.addView(loadingCopy);
    }

    private void startSosCountdown() {
        if (sosCountdownActive) {
            sendSosNow();
            return;
        }
        if (!hasFreshSosLocation()) {
            pendingSosAfterLocation = true;
            setStatus("A fresh location is required before SOS can be sent");
            requestLocation();
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Confirm emergency location")
                    .setMessage("FastAid will not send demo or stale coordinates. "
                            + "Wait for GPS, enter a location, or call " + emergencyNumber() + ".")
                    .setNegativeButton("Cancel", (dialog, which) -> pendingSosAfterLocation = false)
                    .setNeutralButton("Call " + emergencyNumber(), (dialog, which) -> callEmergency())
                    .setPositiveButton("Enter location", (dialog, which) -> showManualLocationDialog())
                    .show();
            return;
        }
        pendingSosAfterLocation = false;
        sosCountdownActive = true;
        setIncidentSelection("accident");
        if (cancelSosButton != null) cancelSosButton.setVisibility(View.VISIBLE);
        if (sendNowSosButton != null) sendNowSosButton.setVisibility(View.VISIBLE);
        if (incidentButton != null) incidentButton.setEnabled(false);
        setStatus("SOS countdown started");

        cancelSosTimerOnly();
        sosCountdownTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = Math.max(1, (millisUntilFinished + 999) / 1000);
                String message = "Sending FastAid request in " + seconds + " seconds";
                if (sosCountdownText != null) {
                    setUiText(sosCountdownText, message);
                    sosCountdownText.announceForAccessibility(message);
                }
            }

            @Override
            public void onFinish() {
                sendSosNow();
            }
        };
        sosCountdownTimer.start();
    }

    private void sendSosNow() {
        sosCountdownActive = false;
        cancelSosTimerOnly();
        if (cancelSosButton != null) cancelSosButton.setVisibility(View.GONE);
        if (sendNowSosButton != null) sendNowSosButton.setVisibility(View.GONE);
        if (sosCountdownText != null) {
            setUiText(sosCountdownText, "Sending request to the configured FastAid network...");
        }
        if (incidentButton != null) incidentButton.setEnabled(true);
        createIncident();
    }

    private void cancelSosCountdown() {
        if (!sosCountdownActive) {
            return;
        }
        sosCountdownActive = false;
        cancelSosTimerOnly();
        if (cancelSosButton != null) cancelSosButton.setVisibility(View.GONE);
        if (sendNowSosButton != null) sendNowSosButton.setVisibility(View.GONE);
        if (incidentButton != null) incidentButton.setEnabled(true);
        setUiText(sosCountdownText, "SOS cancelled. Tap SOS to restart if needed.");
        setUiText(statusText, "SOS cancelled before dispatch");
    }

    private void cancelSosTimerOnly() {
        if (sosCountdownTimer != null) {
            sosCountdownTimer.cancel();
            sosCountdownTimer = null;
        }
    }

    private void createIncident() {
        if (!hasFreshSosLocation()) {
            setStatus("Request not sent - confirm a fresh location");
            showManualLocationDialog();
            return;
        }
        setBusy(true, "Sending FastAid request...");
        executor.execute(() -> {
            try {
                String status = apiClient.createIncident(
                        currentLatitude, currentLongitude, incidentType, patientCount, getIncidentNotes());
                runOnUiThread(() -> {
                    activeIncidentStatus = "Sent";
                    responderAlertState = "incoming";
                    setBusy(false, "Sent to the configured FastAid network: " + status);
                    refreshAfterIncidentChange();
                });
            } catch (Exception error) {
                queueIncidentForRetry();
                runOnUiThread(() -> {
                    setBusy(false, "Saved only - no responder was notified");
                    refreshAfterIncidentChange();
                    showIncidentNotSentDialog();
                });
            }
        });
    }

    private List<AidPlace> visiblePlaces() {
        List<AidPlace> visible = new ArrayList<>();
        for (AidPlace place : places) {
            if ("open".equals(nearbyFilter) && (!place.openKnown || !place.openNow)) continue;
            if ("callable".equals(nearbyFilter) && !place.hasPhone()) continue;
            visible.add(place);
        }
        return visible;
    }

    private void renderPlaces() {
        if (resultsList == null || nearbyCountText == null) {
            updateGoogleMap();
            return;
        }
        List<AidPlace> visible = visiblePlaces();
        if (googlePlacesAttribution != null) {
            googlePlacesAttribution.setVisibility(places.isEmpty() ? View.GONE : View.VISIBLE);
        }
        resultsList.removeAllViews();
        setUiText(nearbyCountText, visible.size() + " found");
        updateGoogleMap();
        if (visible.isEmpty()) {
            String message = places.isEmpty()
                    ? "No nearby aid found. Try another aid type or location."
                    : "No places match this filter.";
            TextView empty = text(message, 14, R.color.fastaid_muted, false);
            empty.setPadding(0, dp(14), 0, dp(14));
            resultsList.addView(empty);
            return;
        }

        List<AidPlace> bestMatches = new ArrayList<>();
        List<AidPlace> checkFirst = new ArrayList<>();
        for (AidPlace place : visible) {
            if (ServiceQualityScanner.shouldSeparateForManualCheck(incidentType, place)) {
                checkFirst.add(place);
            } else {
                bestMatches.add(place);
            }
        }

        int remaining = showAllPlaces ? visible.size() : Math.min(visible.size(), 6);
        remaining = addPlacesSection("Best matches", bestMatches, remaining);
        addPlacesSection("Check before using", checkFirst, remaining);
    }

    private int addPlacesSection(String title, List<AidPlace> sectionPlaces, int remaining) {
        if (sectionPlaces.isEmpty() || remaining <= 0) return remaining;
        TextView sectionTitle = text(title, 12,
                "Check before using".equals(title) ? R.color.fastaid_red : R.color.fastaid_muted,
                true);
        sectionTitle.setPadding(dp(2), dp(12), 0, dp(2));
        resultsList.addView(sectionTitle);
        int count = Math.min(sectionPlaces.size(), remaining);
        for (int index = 0; index < count; index++) {
            LinearLayout card = placeResultCard(sectionPlaces.get(index));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, dp(8), 0, 0);
            resultsList.addView(card, params);
        }
        return remaining - count;
    }

    private LinearLayout placeResultCard(AidPlace place) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(10), dp(8), dp(10));
        card.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_soft_blue), 14));
        card.setElevation(dp(2));

        ImageView icon = iconBubble(placeIconResource(place), placeColor(place), 44, 11);
        card.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout detailsColumn = new LinearLayout(this);
        detailsColumn.setOrientation(LinearLayout.VERTICAL);
        detailsColumn.setPadding(dp(12), 0, dp(8), 0);
        TextView name = text(place.name, 14, R.color.fastaid_ink, true);
        name.setSingleLine(false);
        detailsColumn.addView(name);

        String open = place.openText == null || place.openText.length() == 0
                ? "Open status unknown" : place.openText;
        TextView details = text(place.distance + " - " + open,
                12, R.color.fastaid_muted, false);
        detailsColumn.addView(details);

        String scannerLabel = ServiceQualityScanner.label(incidentType, place);
        int badgeColor = scannerLabel.equals("BEST MATCH")
                ? R.color.fastaid_blue
                : scannerLabel.equals("CHECK FIRST") || scannerLabel.equals("CHECK CATEGORY")
                ? R.color.fastaid_red
                : R.color.fastaid_green;
        int badgeBackground = badgeColor == R.color.fastaid_blue
                ? R.color.fastaid_soft_blue
                : badgeColor == R.color.fastaid_red
                ? R.color.fastaid_card
                : R.color.fastaid_soft_green;
        TextView badge = text(scannerLabel, 11, badgeColor, true);
        badge.setBackground(pill(color(badgeBackground), color(badgeBackground), 8));
        badge.setPadding(dp(6), dp(2), dp(6), dp(2));
        detailsColumn.addView(badge, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView reason = text(ServiceQualityScanner.reason(incidentType, place),
                11, R.color.fastaid_muted, false);
        reason.setPadding(0, dp(2), 0, 0);
        reason.setMaxLines(2);
        detailsColumn.addView(reason);
        card.addView(detailsColumn, new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        ImageView call = actionIconButton(
                R.drawable.ic_m3_call,
                place.hasPhone() ? color(R.color.fastaid_ink) : color(R.color.fastaid_muted),
                Color.WHITE,
                place.hasPhone() ? "Call " + place.name : "No phone number for " + place.name);
        call.setEnabled(place.hasPhone());
        call.setAlpha(place.hasPhone() ? 1f : 0.45f);
        call.setOnClickListener(view -> callPlace(place));
        actions.addView(call, new LinearLayout.LayoutParams(dp(48), dp(48)));
        ImageView go = actionIconButton(
                R.drawable.ic_m3_navigation, color(R.color.fastaid_ink), Color.WHITE,
                "Open directions to " + place.name);
        go.setOnClickListener(view -> navigateToPlace(place));
        LinearLayout.LayoutParams goParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        goParams.setMargins(dp(6), 0, 0, 0);
        actions.addView(go, goParams);
        card.addView(actions);
        card.setContentDescription(place.name + ". " + place.distance + ". " + open
                + ". " + scannerLabel + ". " + ServiceQualityScanner.reason(incidentType, place));
        return card;
    }

    private void renderRecoveryState() {
        if (resultsList == null) return;
        if (googlePlacesAttribution != null) {
            googlePlacesAttribution.setVisibility(places.isEmpty() ? View.GONE : View.VISIBLE);
        }
        resultsList.removeAllViews();
        if (nearbyCountText != null) setUiText(nearbyCountText, "Offline");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_outline), 8));
        card.addView(text("Nearby places could not be refreshed", 15, R.color.fastaid_ink, true));
        TextView copy = text("Use current location and retry. For an emergency, call "
                        + emergencyNumber() + " directly.",
                13, R.color.fastaid_muted, false);
        copy.setPadding(0, dp(5), 0, dp(12));
        card.addView(copy);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button call = primaryButton("Call " + emergencyNumber());
        call.setOnClickListener(view -> callEmergency());
        actions.addView(call, rowWeight());
        Button locate = blueButton("Use GPS");
        locate.setOnClickListener(view -> requestLocation());
        actions.addView(locate, rowWeight());
        Button retry = whiteButton("Retry");
        retry.setOnClickListener(view -> fetchNearbyAid());
        actions.addView(retry, rowWeight());
        card.addView(actions, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));
        resultsList.addView(card);
    }


    private void shareCurrentLocation() {
        if (!hasCurrentCoordinates()) {
            showManualLocationDialog();
            return;
        }
        String body = String.format(Locale.US,
                "FastAid location: %.5f, %.5f https://www.google.com/maps/search/?api=1&query=%.5f,%.5f",
                currentLatitude,
                currentLongitude,
                currentLatitude,
                currentLongitude);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(intent, "Share FastAid location"));
    }
    private void callPlace(AidPlace place) {
        if (!place.hasPhone()) {
            Toast.makeText(this, "No phone number in Google Places data for this place.", Toast.LENGTH_LONG).show();
            return;
        }
        openDialer(place.phone, place.name);
    }

    private void navigateToPlace(AidPlace place) {
        Uri uri;
        if (place.hasCoordinates()) {
            uri = Uri.parse("google.navigation:q=" + place.latitude + "," + place.longitude);
        } else {
            uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(place.name + " " + place.address));
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setPackage("com.google.android.apps.maps");
        if (intent.resolveActivity(getPackageManager()) == null) {
            intent.setPackage(null);
        }
        startActivity(intent);
    }

    private void updateLocationLabel(String prefix) {
        if (locationText == null) return;
        if (!hasCurrentCoordinates()) {
            setUiText(locationText, "No location selected - use GPS or enter a location");
            locationText.setContentDescription("No location selected");
            return;
        }
        String accuracy = Float.isNaN(currentLocationAccuracy)
                ? "" : String.format(Locale.US, " - accuracy %.0f m", currentLocationAccuracy);
        String message = "Map".equals(activeTab)
                ? "Live GPS" + accuracy + " - " + locationAgeText()
                : prefix + ": " + coordinateSummary() + accuracy + " - " + locationAgeText();
        setUiText(locationText, message);
        locationText.setContentDescription(message + ". Coordinates " + coordinateSummary());
    }

    private void setBusy(boolean busy, String message) {
        if (loading != null) loading.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (refreshButton != null) refreshButton.setEnabled(!busy);
        if (locateButton != null) locateButton.setEnabled(!busy);
        if (incidentButton != null) incidentButton.setEnabled(!busy);
        setUiText(statusText, message);
    }

    private void setIncidentSelection(String type) {
        incidentType = type;
        selectedCategoryLabel = categoryLabelForIncidentType(type);
        if (incidentSpinner == null) {
            return;
        }
        for (int index = 0; index < incidentSpinner.getCount(); index++) {
            if (type.equals(incidentSpinner.getItemAtPosition(index).toString())) {
                incidentSpinner.setSelection(index);
                break;
            }
        }
    }


    private String incidentDisplayName() {
        if (incidentType == null || incidentType.isEmpty()) return "Emergency";
        return incidentType.substring(0, 1).toUpperCase(Locale.US) + incidentType.substring(1);
    }
    private String incidentTypeForLabel(String label) {
        String value = label == null ? "" : label.toLowerCase(Locale.US);
        if (value.contains("tyre") || value.contains("tire")) return "tyre";
        if (value.contains("clinic")) return "clinic";
        if (value.contains("pharmacy")) return "pharmacy";
        if (value.contains("toilet") || value.contains("bathroom")) return "toilet";
        if (value.contains("rest stop")) return "rest_stop";
        if (value.contains("parking")) return "parking";
        if (value.contains("medical lab") || value.equals("lab")) return "medical_lab";
        if (value.contains("auto parts")) return "auto_parts";
        if (value.contains("towing")) return "towing";
        if (value.contains("battery")) return "battery";
        if (value.contains("food")) return "food";
        if (value.contains("lodging") || value.contains("accommodation")) return "lodging";
        if (value.contains("car wash")) return "car_wash";
        if (value.contains("e-bike") || value.contains("ebike")) return "ebike";
        if (value.contains("atm")) return "atm";
        if (value.contains("workshop")) return "workshop";
        if (value.contains("ngo") || value.contains("non profit") || value.contains("non-profit")) return "ngo";
        if (value.contains("medical")) return "medical";
        if (value.contains("breakdown")) return "breakdown";
        if (value.contains("repair")) return "repair";
        if (value.contains("fuel")) return "fuel";
        if (value.equals("ev") || value.contains("electric")) return "ev";
        if (value.contains("police")) return "police";
        if (value.contains("fire")) return "fire";
        return "accident";
    }

    private String categoryLabelForIncidentType(String type) {
        String value = type == null ? "" : type.toLowerCase(Locale.US);
        if (value.contains("tyre") || value.contains("tire")) return "Tyres";
        if (value.contains("clinic")) return "Clinic";
        if (value.contains("pharmacy")) return "Pharmacy";
        if (value.contains("toilet")) return "Toilets";
        if (value.contains("rest_stop")) return "Rest stop";
        if (value.contains("parking")) return "Parking";
        if (value.contains("medical_lab")) return "Medical lab";
        if (value.contains("auto_parts")) return "Auto parts";
        if (value.contains("towing")) return "Towing";
        if (value.contains("battery")) return "Battery";
        if (value.contains("food")) return "Food";
        if (value.contains("lodging") || value.contains("hotel")) return "Lodging";
        if (value.contains("car_wash")) return "Car wash";
        if (value.contains("ebike")) return "E-bike";
        if (value.contains("atm")) return "ATM";
        if (value.contains("workshop")) return "Workshop";
        if (value.contains("ngo") || value.contains("non_profit") || value.contains("nonprofit")) return "NGO";
        if (value.contains("medical")) return "Medical";
        if (value.contains("breakdown")) return "Breakdown";
        if (value.contains("repair")) return "Repair";
        if (value.contains("fuel")) return "Fuel";
        if (value.equals("ev") || value.contains("electric")) return "EV";
        if (value.contains("police")) return "Police";
        if (value.contains("fire")) return "Fire";
        return "Accident";
    }
    private String placeIcon(AidPlace place) {
        String category = place.category == null ? "" : place.category.toLowerCase(Locale.US);
        if (category.contains("toilet") || category.contains("bath")) return "WC";
        if (category.contains("pharmacy") || category.contains("drugstore")) return "Rx";
        if (category.contains("atm")) return "ATM";
        if (category.contains("food")) return "FD";
        if (category.contains("lodging") || category.contains("hotel")) return "L";
        if (category.contains("car_wash")) return "W";
        if (category.contains("auto_parts")) return "AP";
        if (category.contains("tire") || category.contains("tyre")) return "T";
        if (category.contains("rest_stop")) return "S";
        if (category.contains("parking")) return "P";
        if (category.contains("ngo")) return "NGO";
        if (category.contains("hospital") || category.contains("medical") || category.contains("clinic")
                || category.contains("doctor") || category.contains("lab")) return "+";
        if (category.contains("police")) return "P";
        if (category.contains("fire")) return "F";
        if (category.contains("gas") || category.contains("fuel")) return "F";
        if (category.contains("electric") || category.contains("ev")) return "E";
        if (category.contains("repair") || category.contains("tire") || category.contains("tow")) return "R";
        return "A";
    }

    private int placeColor(AidPlace place) {
        String category = place.category == null ? "" : place.category.toLowerCase(Locale.US);
        if (category.contains("toilet") || category.contains("parking")
                || category.contains("atm") || category.contains("lodging")
                || category.contains("hotel") || category.contains("car_wash")) {
            return color(R.color.fastaid_blue);
        }
        if (category.contains("rest_stop")) {
            return color(R.color.fastaid_orange);
        }
        if (category.contains("ngo")) {
            return color(R.color.fastaid_blue);
        }
        if (category.contains("hospital") || category.contains("pharmacy") || category.contains("medical")
                || category.contains("clinic") || category.contains("doctor") || category.contains("lab")
                || category.contains("fire")) {
            return color(R.color.fastaid_red);
        }
        if (category.contains("police")) {
            return color(R.color.fastaid_blue);
        }
        if (category.contains("gas") || category.contains("fuel") || category.contains("electric")
                || category.contains("towing") || category.contains("tow")) {
            return color(R.color.fastaid_orange);
        }
        return color(R.color.fastaid_green);
    }

    private ImageView plainIcon(int iconResource, int tintColor, int paddingDp) {
        ImageView view = new ImageView(this);
        view.setImageResource(iconResource);
        view.setColorFilter(tintColor);
        view.setPadding(dp(paddingDp), dp(paddingDp), dp(paddingDp), dp(paddingDp));
        view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return view;
    }

    private LinearLayout heroActionButton(int iconResource, String label, int tintColor) {
        LinearLayout button = new LinearLayout(this);
        button.setOrientation(LinearLayout.VERTICAL);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), dp(6), dp(8), dp(6));
        button.setBackground(cardBackground(Color.WHITE, color(R.color.fastaid_soft_blue), 12));
        button.setElevation(dp(4));
        ImageView icon = plainIcon(iconResource, tintColor, 2);
        button.addView(icon, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView text = text(label, 10, R.color.fastaid_ink, true);
        text.setGravity(Gravity.CENTER);
        text.setPadding(0, dp(4), 0, 0);
        button.addView(text);
        return button;
    }

    private Button compactIconButton(String label, int iconResource, int tintColor) {
        Button button = compactButton(label);
        button.setCompoundDrawablesWithIntrinsicBounds(iconResource, 0, 0, 0);
        button.setCompoundDrawablePadding(dp(6));
        for (android.graphics.drawable.Drawable drawable : button.getCompoundDrawables()) {
            if (drawable != null) drawable.setTint(tintColor);
        }
        return button;
    }

    private ImageView actionIconButton(int iconResource, int tintColor, int backgroundColor, String description) {
        ImageView view = plainIcon(iconResource, tintColor, 9);
        view.setBackground(pill(backgroundColor, color(R.color.fastaid_soft_blue), 21));
        view.setContentDescription(description);
        view.setClickable(true);
        view.setFocusable(true);
        return view;
    }
    private ImageView iconBubble(int iconResource, int backgroundColor, int sizeDp, int paddingDp) {
        ImageView view = new ImageView(this);
        view.setImageResource(iconResource);
        view.setColorFilter(Color.WHITE);
        view.setPadding(dp(paddingDp), dp(paddingDp), dp(paddingDp), dp(paddingDp));
        view.setBackground(pill(backgroundColor, backgroundColor, sizeDp / 2));
        view.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return view;
    }

    private int placeIconResource(AidPlace place) {
        String category = place.category == null ? "" : place.category.toLowerCase(Locale.US);
        if (category.contains("toilet") || category.contains("bath")) return R.drawable.ic_m3_wc;
        if (category.contains("pharmacy") || category.contains("drugstore")) return R.drawable.ic_m3_pharmacy;
        if (category.contains("atm")) return R.drawable.ic_m3_atm;
        if (category.contains("food")) return R.drawable.ic_m3_restaurant;
        if (category.contains("lodging") || category.contains("hotel")) return R.drawable.ic_m3_hotel;
        if (category.contains("car_wash")) return R.drawable.ic_m3_car_wash;
        if (category.contains("auto_parts")) return R.drawable.ic_m3_workshop;
        if (category.contains("tire") || category.contains("tyre")) return R.drawable.ic_m3_tire_repair;
        if (category.contains("rest_stop")) return R.drawable.ic_m3_rest_stop;
        if (category.contains("parking")) return R.drawable.ic_m3_local_parking;
        if (category.contains("ngo")) return R.drawable.ic_m3_volunteer;
        if (category.contains("clinic") || category.contains("doctor") || category.contains("lab")) return R.drawable.ic_m3_medical_services;
        if (category.contains("hospital") || category.contains("medical")) return R.drawable.ic_aid_medical;
        if (category.contains("police")) return R.drawable.ic_aid_police;
        if (category.contains("fire")) return R.drawable.ic_aid_fire;
        if (category.contains("gas") || category.contains("fuel")) return R.drawable.ic_aid_fuel;
        if (category.contains("electric") || category.contains("ev")) return R.drawable.ic_aid_ev;
        if (category.contains("tow")) return R.drawable.ic_m3_towing;
        if (category.contains("repair") || category.contains("workshop")) return R.drawable.ic_aid_repair;
        if (category.contains("responder")) return R.drawable.ic_aid_police;
        return R.drawable.ic_aid_accident;
    }
    private Button compactButton(String label) {
        return materialButton(label, Color.WHITE, color(R.color.fastaid_ink), true, 12);
    }

    private Button blueButton(String label) {
        return materialButton(label, color(R.color.fastaid_blue), Color.WHITE, false, 11);
    }

    private Button primaryButton(String label) {
        return materialButton(label, color(R.color.fastaid_red), Color.WHITE, false, 12);
    }

    private Button pinkButton(String label) {
        return materialButton(label, color(R.color.fastaid_card), color(R.color.fastaid_red), false, 12);
    }

    private Button whiteButton(String label) {
        return materialButton(label, Color.WHITE, color(R.color.fastaid_red), true, 12);
    }

    private MaterialButton materialButton(
            String label,
            int fillColor,
            int textColor,
            boolean outlined,
            int textSize
    ) {
        MaterialButton button = new MaterialButton(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(textColor);
        button.setTextSize(textSize);
        button.setTypeface(null, Typeface.BOLD);
        button.setCornerRadius(dp(8));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setMinHeight(controlSize());
        button.setMinimumHeight(controlSize());
        button.setBackgroundTintList(ColorStateList.valueOf(fillColor));
        button.setStrokeWidth(outlined ? dp(1) : 0);
        button.setStrokeColor(ColorStateList.valueOf(color(R.color.fastaid_outline)));
        return button;
    }

    @SuppressLint("SetTextI18n")
    private void setUiText(TextView target, String content) {
        if (target != null) target.setText(ui(content));
    }

    private TextView text(String content, int size, int colorResource, boolean bold) {
        TextView view = new TextView(this);
        view.setText(ui(content));
        view.setTextSize(size);
        view.setTextColor(color(colorResource));
        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }
        return view;
    }

    private LinearLayout.LayoutParams rowWeight() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        return params;
    }

    private GradientDrawable cardBackground(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(Math.min(radiusDp, 8)));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private GradientDrawable pill(int fillColor, int strokeColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }
    private int color(int resourceId) {
        return getResources().getColor(resourceId, getTheme());
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}







































