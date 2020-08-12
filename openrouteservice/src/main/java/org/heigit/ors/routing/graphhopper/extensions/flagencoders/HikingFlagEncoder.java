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
import com.graphhopper.util.EdgeIteratorState;
import com.graphhopper.util.Helper;
import com.graphhopper.util.PMap;
import com.graphhopper.util.PointList;
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

        sacScaleSpeeds.put("hiking", 5.0);
        sacScaleSpeeds.put("mountain_hiking", 4.5);
        sacScaleSpeeds.put("demanding_mountain_hiking", 1.8);
        sacScaleSpeeds.put("alpine_hiking", 1.4);
        sacScaleSpeeds.put("demanding_alpine_hiking", 1.2);
        sacScaleSpeeds.put("difficult_alpine_hiking", 1.2);

        speedDefault = 5.0;
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

    @Override
    public void applyWayTags(ReaderWay way, EdgeIteratorState edge) {
        PointList pl = edge.fetchWayGeometry(3);
        if (!pl.is3D())
            return;

        if (way.hasTag("tunnel", "yes") || way.hasTag("bridge", "yes") || way.hasTag("highway", "steps"))
            // do not change speed
            // note: although tunnel can have a difference in elevation it is unlikely that the elevation data is correct for a tunnel
            return;

        // Decrease the speed for ele increase (incline), and slightly decrease the speed for ele decrease (decline)
        double prevEle = pl.getElevation(0);
        double fullDistance = edge.getDistance();

        // for short edges an incline makes no sense and for 0 distances could lead to NaN values for speed, see #432
        if (fullDistance < 2)
            return;

        double eleDelta = Math.abs(pl.getElevation(pl.size() - 1) - prevEle);
        double slope = eleDelta / fullDistance;

        IntsRef edgeFlags = edge.getFlags();
        if ((accessEnc.getBool(false, edgeFlags) || accessEnc.getBool(true, edgeFlags))
                && slope > 0.005) {

            // see #1679 => v_hor=4.5km/h for horizontal speed; v_vert=2*0.5km/h for vertical speed (assumption: elevation < edge distance/4.5)
            // s_3d/v=h/v_vert + s_2d/v_hor => v = s_3d / (h/v_vert + s_2d/v_hor) = sqrt(s²_2d + h²) / (h/v_vert + s_2d/v_hor)
            // slope=h/s_2d=~h/2_3d              = sqrt(1+slope²)/(slope+1/4.5) km/h
            // maximum slope is 0.37 (Ffordd Pen Llech)
            double newSpeed = Math.sqrt(1 + slope * slope) / (slope + 1 / getSpeed(way));
            edge.set(speedEncoder, Helper.keepIn(newSpeed, 0.3, speedDefault));
        }
    }

}
