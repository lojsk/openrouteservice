/*
 * This file is part of Openrouteservice.
 *
 * Openrouteservice is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, see <https://www.gnu.org/licenses/>.
 */

package org.heigit.ors.routing.graphhopper.extensions.flagencoders;

import com.graphhopper.reader.ReaderWay;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.PriorityCode;
import com.graphhopper.storage.IntsRef;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import org.heigit.ors.routing.graphhopper.extensions.OSMTags;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.graphhopper.routing.util.PriorityCode.*;

public class HikingFlagEncoder extends FootFlagEncoder {
    public HikingFlagEncoder(PMap properties) {
        this((int) properties.getLong("speedBits", 4),
                properties.getDouble("speedFactor", 1));
        this.properties = properties;
        this.setBlockFords(properties.getBool("block_fords", false));
    }
    private final Map<String, Double> sacScaleSpeeds = new HashMap<>();

    private HikingFlagEncoder(int speedBits, double speedFactor) {
        super(speedBits, speedFactor);

        hikingNetworkToCode.put("iwn", BEST.getValue());
        hikingNetworkToCode.put("nwn", BEST.getValue());
        hikingNetworkToCode.put("rwn", VERY_NICE.getValue());
        hikingNetworkToCode.put("lwn", VERY_NICE.getValue());

        suitableSacScales.addAll(Arrays.asList(
                "hiking",
                "mountain_hiking",
                "demanding_mountain_hiking",
                "alpine_hiking",
                "demanding_alpine_hiking",
                "difficult_alpine_hiking"
        ));

        preferredWayTags.addAll(Arrays.asList(
                "track",
                "path",
                "footway"
        ));

        sacScaleSpeeds.put("hiking", 4.0);
        sacScaleSpeeds.put("mountain_hiking", 2.5);
        sacScaleSpeeds.put("demanding_mountain_hiking", 1.1);
        sacScaleSpeeds.put("alpine_hiking", 1.0);
        sacScaleSpeeds.put("demanding_alpine_hiking", 0.9);
        sacScaleSpeeds.put("difficult_alpine_hiking", 0.8);

        speedDefault = 4.0;
        init();
    }

    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public String toString() {
        return FlagEncoderNames.HIKING_ORS;
    }

    @Override
    public IntsRef handleWayTags(IntsRef edgeFlags, ReaderWay way, EncodingManager.Access access, long relationFlags) {
        if (!access.isFerry()) {
            speedEncoder.setDecimal(false, edgeFlags, getSpeed(way));
        } else {
            setSpeed(false, edgeFlags, getFerrySpeed(way));
        }

        accessEnc.setBool(false, edgeFlags, true);
        accessEnc.setBool(true, edgeFlags, true);

        int priorityFromRelation = 0;
        if (relationFlags != 0) {
            priorityFromRelation = (int) relationCodeEncoder.getValue(relationFlags);
        }

        priorityWayEncoder.setDecimal(false, edgeFlags, PriorityCode.getFactor(handlePriority(way, priorityFromRelation)));
        return edgeFlags;
    }

    private double getSpeed(ReaderWay way) {
        String tt = way.getTag(OSMTags.Keys.SAC_SCALE);
        if (!Helper.isEmpty(tt) && sacScaleSpeeds.get(tt) != null) {
            return sacScaleSpeeds.get(tt);
        }

        return speedDefault;
    }

}
