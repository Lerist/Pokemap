package com.omkarmoghe.pokemap.views.map;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.omkarmoghe.pokemap.R;
import com.omkarmoghe.pokemap.controllers.map.LocationManager;
import com.omkarmoghe.pokemap.models.events.CatchablePokemonEvent;
import com.omkarmoghe.pokemap.models.events.SearchInPosition;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 *
 * Use the {@link MapWrapperFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapWrapperFragment extends Fragment implements OnMapReadyCallback,
                                                            GoogleMap.OnMapLongClickListener,
                                                            ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 19;

    private LocationManager locationManager;

    private View mView;
    private SupportMapFragment mSupportMapFragment;
    private GoogleMap mGoogleMap;
    private Location mLocation = null;
    private Marker userSelectedPositionMarker = null;
    private Circle userSelectedPositionCircle = null;
    private List<Marker> markerList = new ArrayList<>();

    public MapWrapperFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapWrapperFragment.
     */
    public static MapWrapperFragment newInstance() {
        MapWrapperFragment fragment = new MapWrapperFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onStart(){
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        locationManager = LocationManager.getInstance(getContext());
        locationManager.register(new LocationManager.Listener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mLocation == null) {
                    mLocation = location;
                    initMap();
                }
                else{
                    mLocation = location;
                }
            }
        });
        // Inflate the layout for this fragment if the view is not null
        if (mView == null) mView = inflater.inflate(R.layout.fragment_map_wrapper, container, false);
        else {

        }

        // build the map
        if (mSupportMapFragment == null) {
            mSupportMapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mSupportMapFragment).commit();
            mSupportMapFragment.setRetainInstance(true);
        }

        if (mGoogleMap == null) {
            mSupportMapFragment.getMapAsync(this);
        }

        FloatingActionButton locationFab = (FloatingActionButton) mView.findViewById(R.id.location_fab);
        locationFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLocation != null && mGoogleMap != null) {
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 15));

                    Toast.makeText(getContext(), "Found you!", Toast.LENGTH_SHORT).show();
                }
                else{

                    Toast.makeText(getContext(), "Waiting on location...", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return mView;
    }
    private void initMap(){
        if (mLocation != null && mGoogleMap != null){
            mGoogleMap.setMyLocationEnabled(true);
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 15));
            Toast.makeText(getContext(), "Found you!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setPokemonMarkers(final List<CatchablePokemon> pokeList){
        if (mGoogleMap != null) {
            //Removing all pokemons from map
            if (markerList != null && !markerList.isEmpty()){
                for(Marker marker : markerList){
                    marker.remove();
                }
                markerList = new ArrayList<Marker>(); //cleaning the array
            }

            for (CatchablePokemon poke : pokeList) {
                int resourceID = getResources().getIdentifier("p" + poke.getPokemonId().getNumber(), "drawable", getActivity().getPackageName());
                long millisLeft = poke.getExpirationTimestampMs() - System.currentTimeMillis();
                Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                        .position(new LatLng(poke.getLatitude(), poke.getLongitude()))
                        .title(poke.getPokemonId().name())
                        .snippet("Dissapears in: " + getDurationBreakdown(millisLeft))
                        .icon(BitmapDescriptorFactory.fromResource(resourceID)));

                marker.showInfoWindow();
                //adding pokemons to list to be removed on next search
                markerList.add(marker);
            }
        } else {
            Toast.makeText(getContext(), "The map is not initialized.", Toast.LENGTH_LONG).show();
        }
    }

    public static String getDurationBreakdown(long millis) {
        if(millis < 0) {
            return "Expired";
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        sb.append(minutes);
        sb.append(" Minutes ");
        sb.append(seconds);
        sb.append(" Seconds");

        return(sb.toString());
    }

    /**
     * Called whenever a CatchablePokemonEvent is posted to the bus. Posted when new catchable pokemon are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CatchablePokemonEvent event) {
        Toast.makeText(getContext(), event.getCatchablePokemon().size() + " new catchable Pokemon have been found.", Toast.LENGTH_LONG).show();
        setPokemonMarkers(event.getCatchablePokemon());
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        UiSettings settings = mGoogleMap.getUiSettings();
        settings.setCompassEnabled(true);
        settings.setTiltGesturesEnabled(true);
        settings.setMyLocationButtonEnabled(false);
        //Handle long click
        mGoogleMap.setOnMapLongClickListener(this);
        //Disable for now coz is under FAB
        settings.setMapToolbarEnabled(false);
        initMap();
    }

    @Override
    public void onMapLongClick(LatLng position) {
        //Draw user position marker with circle
        drawMarkerWithCircle (position);

        //Sending event to MainActivity
        SearchInPosition sip = new SearchInPosition();
        sip.setPosition(position);
        EventBus.getDefault().post(sip);
    }

    private void drawMarkerWithCircle(LatLng position){
        //Check and eventually remove old marker
        if(userSelectedPositionMarker != null && userSelectedPositionCircle != null){
            userSelectedPositionMarker.remove();
            userSelectedPositionCircle.remove();
        }

        double radiusInMeters = 100.0;
        int strokeColor = 0xff3399FF; // outline
        int shadeColor = 0x4400CCFF; // fill

        CircleOptions circleOptions = new CircleOptions().center(position).radius(radiusInMeters).fillColor(shadeColor).strokeColor(strokeColor).strokeWidth(8);
        userSelectedPositionCircle = mGoogleMap.addCircle(circleOptions);

        userSelectedPositionMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(position)
                .title("Position Picked")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
    }

}

