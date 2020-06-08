/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.mediarouter.media;

import static android.media.MediaRoute2Info.FEATURE_LIVE_AUDIO;
import static android.media.MediaRoute2Info.FEATURE_LIVE_VIDEO;
import static android.media.MediaRoute2Info.FEATURE_REMOTE_PLAYBACK;

import static androidx.mediarouter.media.MediaRouter.RouteInfo.DEVICE_TYPE_UNKNOWN;

import android.annotation.SuppressLint;
import android.content.IntentFilter;
import android.media.MediaRoute2Info;
import android.media.RouteDiscoveryPreference;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

//TODO: Remove SuppressLInt
@SuppressLint("NewApi")
@RequiresApi(api = Build.VERSION_CODES.R)
class MediaRouter2Utils {
    static final String FEATURE_EMPTY = "android.media.route.feature.EMPTY";

    // Used in MediaRoute2Info#getExtras()
    static final String KEY_EXTRAS = "androidx.mediarouter.media.KEY_EXTRAS";
    static final String KEY_CONTROL_FILTERS = "androidx.mediarouter.media.KEY_CONTROL_FILTERS";
    static final String KEY_DEVICE_TYPE = "androidx.mediarouter.media.KEY_DEVICE_TYPE";
    static final String KEY_ORIGINAL_ROUTE_ID = "androidx.mediarouter.media.KEY_ORIGINAL_ROUTE_ID";

    // Used in RoutingController#getControlHints()
    static final String KEY_MESSENGER = "androidx.mediarouter.media.KEY_MESSENGER";
    static final String KEY_SESSION_NAME = "androidx.mediarouter.media.KEY_SESSION_NAME";

    private MediaRouter2Utils() {}

    @Nullable
    public static MediaRoute2Info toFwkMediaRoute2Info(@Nullable MediaRouteDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }

        MediaRoute2Info.Builder builder = new MediaRoute2Info.Builder(descriptor.getId(),
                descriptor.getName())
                .setDescription(descriptor.getDescription())
                .setConnectionState(descriptor.getConnectionState())
                .setVolumeHandling(descriptor.getVolumeHandling())
                .setVolume(descriptor.getVolume())
                .setVolumeMax(descriptor.getVolumeMax())
                .addFeatures(toFeatures(descriptor.getControlFilters()))
                .setIconUri(descriptor.getIconUri())
                //TODO: set client package name
                //.setClientPackageName(clientMap.get(device.getDeviceId()))
                ;

        // Since MediaRouter2Info has no public APIs to get/set device types and control filters,
        // We use extras for passing those kinds of information.
        Bundle extras = new Bundle();
        extras.putBundle(KEY_EXTRAS, descriptor.getExtras());
        extras.putParcelableArrayList(KEY_CONTROL_FILTERS,
                new ArrayList<>(descriptor.getControlFilters()));
        extras.putInt(KEY_DEVICE_TYPE, descriptor.getDeviceType());
        extras.putString(KEY_ORIGINAL_ROUTE_ID, descriptor.getId());
        builder.setExtras(extras);

        // This is a workaround for preventing IllegalArgumentException in MediaRoute2Info.
        if (descriptor.getControlFilters().isEmpty()) {
            builder.addFeature(FEATURE_EMPTY);
        }

        return builder.build();
    }

    @Nullable
    public static MediaRouteDescriptor toMediaRouteDescriptor(
            @Nullable MediaRoute2Info fwkMediaRoute2Info) {
        if (fwkMediaRoute2Info == null) {
            return null;
        }

        MediaRouteDescriptor.Builder builder = new MediaRouteDescriptor.Builder(
                fwkMediaRoute2Info.getId(), fwkMediaRoute2Info.getName().toString())
                .setConnectionState(fwkMediaRoute2Info.getConnectionState())
                .setVolumeHandling(fwkMediaRoute2Info.getVolumeHandling())
                .setVolumeMax(fwkMediaRoute2Info.getVolumeMax())
                .setVolume(fwkMediaRoute2Info.getVolume())
                .setExtras(fwkMediaRoute2Info.getExtras())
                .setEnabled(true)
                .setCanDisconnect(false);

        CharSequence description = fwkMediaRoute2Info.getDescription();
        if (description != null) {
            builder.setDescription(description.toString());
        }

        Uri iconUri = fwkMediaRoute2Info.getIconUri();
        if (iconUri != null) {
            builder.setIconUri(iconUri);
        }

        Bundle extras = fwkMediaRoute2Info.getExtras();
        if (extras == null || !extras.containsKey(KEY_EXTRAS)
                || !extras.containsKey(KEY_DEVICE_TYPE)
                || !extras.containsKey(KEY_CONTROL_FILTERS)) {
            // Filter out routes from non-AndroidX MediaRouteProviderService.
            return null;
        }

        builder.setExtras(extras.getBundle(KEY_EXTRAS));
        builder.setDeviceType(extras.getInt(KEY_DEVICE_TYPE, DEVICE_TYPE_UNKNOWN));

        List<IntentFilter> controlFilters = extras.getParcelableArrayList(KEY_CONTROL_FILTERS);
        if (controlFilters != null) {
            builder.addControlFilters(controlFilters);
        }

        // TODO: Set 'dynamic group route' related values properly
        // builder.setIsDynamicGroupRoute();
        // builder.addGroupMemberIds();

        return builder.build();
    }

    static Collection<String> toFeatures(List<IntentFilter> controlFilters) {
        Set<String> features = new HashSet<>();
        for (IntentFilter filter : controlFilters) {
            int count = filter.countCategories();
            for (int i = 0; i < count; i++) {
                features.add(toRouteFeature(filter.getCategory(i)));
            }
        }
        return features;
    }

    @NonNull
    static List<IntentFilter> toControlFilters(@Nullable Collection<String> features) {
        if (features == null) {
            return new ArrayList<>();
        }
        return features.stream().distinct().map(f -> {
            IntentFilter filter = new IntentFilter();
            filter.addCategory(toControlCategory(f));
            // TODO: Add actions by using extras. (see RemotePlaybackClient#detectFeatures())
            // filter.addAction(MediaControlIntent.ACTION_PLAY);
            // filter.addAction(MediaControlIntent.ACTION_SEEK);
            return filter;
        }).collect(Collectors.toList());
    }

    @NonNull
    static List<String> getRouteIds(@Nullable List<MediaRoute2Info> routes) {
        if (routes == null) {
            return new ArrayList<>();
        }
        return routes.stream().filter(Objects::nonNull)
                .map(MediaRoute2Info::getId).collect(Collectors.toList());
    }

    @NonNull
    static RouteDiscoveryPreference toDiscoveryPreference(
            @Nullable MediaRouteDiscoveryRequest discoveryRequest) {
        if (discoveryRequest == null || !discoveryRequest.isValid()) {
            return new RouteDiscoveryPreference.Builder(new ArrayList<>(), false).build();
        }
        boolean activeScan = discoveryRequest.isActiveScan();
        List<String> routeFeatures = discoveryRequest.getSelector().getControlCategories()
                .stream().map(MediaRouter2Utils::toRouteFeature)
                .collect(Collectors.toList());
        return new RouteDiscoveryPreference.Builder(routeFeatures, activeScan).build();
    }

    static String toRouteFeature(String controlCategory) {
        switch (controlCategory) {
            case MediaControlIntent.CATEGORY_LIVE_AUDIO:
                return FEATURE_LIVE_AUDIO;
            case MediaControlIntent.CATEGORY_LIVE_VIDEO:
                return FEATURE_LIVE_VIDEO;
            case MediaControlIntent.CATEGORY_REMOTE_PLAYBACK:
                return FEATURE_REMOTE_PLAYBACK;
        }
        return controlCategory;
    }

    static String toControlCategory(String routeFeature) {
        switch (routeFeature) {
            case FEATURE_LIVE_AUDIO:
                return MediaControlIntent.CATEGORY_LIVE_AUDIO;
            case FEATURE_LIVE_VIDEO:
                return MediaControlIntent.CATEGORY_LIVE_VIDEO;
            case FEATURE_REMOTE_PLAYBACK:
                return MediaControlIntent.CATEGORY_REMOTE_PLAYBACK;
        }
        return routeFeature;
    }
}
