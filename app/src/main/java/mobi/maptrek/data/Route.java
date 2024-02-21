/*
 * Copyright 2018 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek.data;

import android.os.Parcel;
import android.os.Parcelable;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.style.RouteStyle;
import mobi.maptrek.util.Geo;
import mobi.maptrek.util.StringFormatter;

public class Route implements Parcelable {
    public int id;
    public String name;
    public String description;
    public boolean show;
    public int width;

    public double distance;
    public boolean removed = false;
    private BoundingBox mBox = null;

    public RouteStyle style = new RouteStyle();
    public DataSource source; // back reference to it's source

    private final ArrayList<Instruction> instructions = new ArrayList<>();
    private Instruction lastInstruction;
    private UpdateListener updateListener;

    public Route() {
        this(null, null, false);
    }

    public Route(String name, String description, boolean show) {
        this.name = name;
        this.description = description;
        this.show = show;
        distance = 0;
    }

    public Instruction addInstruction(int latitudeE6, int longitudeE6) {
        return addInstruction(new GeoPoint(latitudeE6, longitudeE6));
    }

    public Instruction addInstruction(GeoPoint point) {
        Instruction instruction = new Instruction(point);
        addInstruction(instruction);
        return instruction;
    }

    public Instruction addInstruction(int latitudeE6, int longitudeE6, String text, int sign) {
        Instruction instruction = new Instruction(latitudeE6, longitudeE6, text, sign);
        addInstruction(instruction);
        return instruction;
    }

    private void addInstruction(Instruction instruction) {
        if (lastInstruction != null) {
            lastInstruction.next = instruction;
            lastInstruction.reset();
            instruction.distance = lastInstruction.vincentyDistance(instruction);
            distance += instruction.distance;
        }
        instruction.previous = lastInstruction;
        lastInstruction = instruction;
        instructions.add(lastInstruction);
        notifyChanged();
    }

    public Instruction insertInstruction(GeoPoint point) {
        Instruction instruction = new Instruction(point);
        insertInstruction(instruction);
        return instruction;
    }

    private void insertInstruction(Instruction instruction) {
        if (instructions.size() < 2) {
            addInstruction(instruction);
            return;
        }
        int after = -1;
        double xtk = Double.MAX_VALUE;
        synchronized (instructions) {
            for (int i = instructions.size() - 2; i >= 0; i--) {
                double distance = instruction.vincentyDistance(instructions.get(i + 1));
                double bearing1 = instruction.bearingTo(instructions.get(i + 1));
                double dtk1 = instructions.get(i).bearingTo(instructions.get(i + 1));
                double cxtk1 = Math.abs(Geo.xtk(distance, dtk1, bearing1));
                double bearing2 = instruction.bearingTo(instructions.get(i));
                double dtk2 = instructions.get(i + 1).bearingTo(instructions.get(i));
                double cxtk2 = Math.abs(Geo.xtk(distance, dtk2, bearing2));

                if (cxtk2 != Double.POSITIVE_INFINITY && cxtk1 < xtk) {
                    xtk = cxtk1;
                    after = i;
                }
            }
        }
        if (after < 0)
            addInstruction(instruction);
        else
            insertInstruction(after, instruction);
    }

    public void insertInstruction(int after, Instruction instruction) {
        Instruction prev = instructions.get(after);
        Instruction next = instructions.get(after + 1);
        instructions.add(after + 1, instruction);
        instruction.previous = prev;
        instruction.next = next;
        prev.next = instruction;
        next.previous = instruction;
        prev.reset();
        next.reset();
        instruction.distance = prev.vincentyDistance(instruction);
        next.distance = instruction.vincentyDistance(next);
        lastInstruction = instructions.get(instructions.size() - 1);
        distance = distanceBetween(0, instructions.size() - 1);
        notifyChanged();
    }

    public void removeInstruction(Instruction instruction) {
        Instruction prev = instruction.previous;
        Instruction next = instruction.next;
        instructions.remove(instruction);
        if (prev != null) {
            prev.next = next;
            prev.reset();
        }
        if (next != null) {
            next.previous = prev;
            next.reset();
            if (prev != null)
                next.distance = prev.vincentyDistance(next);
        }
        if (instructions.size() > 0) {
            lastInstruction = instructions.get(instructions.size() - 1);
            distance = distanceBetween(0, instructions.size() - 1);
        } else {
            lastInstruction = null;
            distance = 0f;
        }
        notifyChanged();
    }

    public void clear() {
        synchronized (instructions) {
            instructions.clear();
        }
        lastInstruction = null;
        distance = 0;
        notifyChanged();
    }

    public double distanceBetween(int first, int last) {
        double dist = 0.0;
        int lastIndex = instructions.size() - 1;
        synchronized (instructions) {
            for (int i = first; i < last; i++) {
                if (i >= 0 && i < lastIndex)
                    dist += instructions.get(i).vincentyDistance(instructions.get(i + 1));
            }
        }
        return dist;
    }

    public Instruction getNearestInstruction(GeoPoint point) {
        synchronized (instructions) {
            // try fast
            if (instructions.size() == 0)
                return null;
            if (instructions.size() == 1)
                return instructions.get(0);
            for (Instruction instruction : instructions) {
                if (point.latitudeE6 == instruction.latitudeE6
                        && point.longitudeE6 == instruction.longitudeE6)
                    return instruction;
            }
            // try slow
            int index = instructions.size() - 1;
            double distance = point.vincentyDistance(instructions.get(index));
            for (int i = index - 1; i >= 0; i--) {
                double d = point.vincentyDistance(instructions.get(i));
                if (d < distance) {
                    distance = d;
                    index = i;
                }
            }
            return instructions.get(index);
        }
    }

    public int size() {
        return instructions.size();
    }

    public Instruction get(int index) {
        if (index < 0)
            index = instructions.size() + index;
        return instructions.get(index);
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public List<GeoPoint> getCoordinates() {
        List<GeoPoint> points = new ArrayList<>(instructions.size());
        points.addAll(instructions);
        return points;
    }

    public BoundingBox getBoundingBox() {
        if (mBox == null) {
            mBox = new BoundingBox(getCoordinates());
        }
        return mBox;
    }

    public double getTotalDistance() {
        return distance;
    }

    public void setUpdateListener(UpdateListener updateListener) {
        this.updateListener = updateListener;
    }

    public void removeUpdateListener() {
        updateListener = null;
    }

    private void notifyChanged() {
        if (updateListener != null)
            updateListener.onRouteChanged();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(id);
        out.writeString(name);
        out.writeString(description);
        out.writeInt(width);
        synchronized (instructions) {
            out.writeInt(instructions.size());
            for (Instruction instruction : instructions) {
                out.writeInt(instruction.latitudeE6);
                out.writeInt(instruction.longitudeE6);
                out.writeString(instruction.text);
                out.writeInt(instruction.sign);
            }
        }
    }

    public static final Parcelable.Creator<Route> CREATOR = new Parcelable.Creator<Route>() {
        public Route createFromParcel(Parcel in) {
            return new Route(in);
        }

        public Route[] newArray(int size) {
            return new Route[size];
        }
    };

    private Route(Parcel in) {
        id = in.readInt();
        name = in.readString();
        description = in.readString();
        width = in.readInt();
        int size = in.readInt();
        for (int i = 0; i < size; i++) {
            int latitudeE6 = in.readInt();
            int longitudeE6 = in.readInt();
            String text = in.readString();
            int sign = in.readInt();
            addInstruction(new Instruction(latitudeE6, longitudeE6, text, sign));
        }
    }

    public Track toTrack() {
        Track track = new Track();
        track.name = name;
        track.description = description;
        boolean continuous = false;
        for (Instruction instruction : instructions) {
            track.addPointFast(continuous, instruction.latitudeE6, instruction.longitudeE6, instruction.elevation, Float.NaN, Float.NaN, Float.NaN, 0L);
            continuous = true;
        }
        track.style.color = style.color;
        return track;
    }

    public static class Instruction extends GeoPoint {
        public static final int UNDEFINED = -299;
        public static final int START = -199;
        // Instruction directions taken from GraphHopper
        public static final int UNKNOWN = -99;
        public static final int U_TURN_UNKNOWN = -98;
        public static final int U_TURN_LEFT = -8;
        public static final int KEEP_LEFT = -7;
        public static final int LEAVE_ROUNDABOUT = -6; // for future use
        public static final int TURN_SHARP_LEFT = -3;
        public static final int TURN_LEFT = -2;
        public static final int TURN_SLIGHT_LEFT = -1;
        public static final int CONTINUE_ON_STREET = 0;
        public static final int TURN_SLIGHT_RIGHT = 1;
        public static final int TURN_RIGHT = 2;
        public static final int TURN_SHARP_RIGHT = 3;
        public static final int FINISH = 4;
        public static final int REACHED_VIA = 5;
        public static final int USE_ROUNDABOUT = 6;
        public static final int IGNORE = Integer.MIN_VALUE;
        public static final int KEEP_RIGHT = 7;
        public static final int U_TURN_RIGHT = 8;

        public String text;
        public int sign;
        public float elevation;
        public double distance = 0.0;

        private Instruction previous;
        private Instruction next;

        // cached
        private int signInt;
        private long turn;

        Instruction(GeoPoint point) {
            super(point.latitudeE6, point.longitudeE6);
            sign = UNDEFINED;
            elevation = Float.NaN;
        }

        Instruction(int latitudeE6, int longitudeE6, String text, int sign) {
            super(latitudeE6, longitudeE6);
            this.text = text;
            this.sign = sign;
        }

        public String getText() {
            if (text != null)
                return text;
            String course = null;
            switch (getSign()) {
                case Instruction.TURN_SHARP_LEFT:
                case Instruction.TURN_LEFT:
                case Instruction.TURN_SLIGHT_LEFT:
                case Instruction.TURN_SLIGHT_RIGHT:
                case Instruction.TURN_RIGHT:
                case Instruction.TURN_SHARP_RIGHT:
                    course = StringFormatter.angleH(Math.abs(turn));
            }
            switch (getSign()) {
                case Instruction.START:
                    return "Start";
                case Instruction.U_TURN_UNKNOWN:
                    return "Make U-turn";
                case Instruction.U_TURN_LEFT:
                    return "Make left U-turn";
                case Instruction.U_TURN_RIGHT:
                    return "Make right U-turn";
                case Instruction.KEEP_LEFT:
                    return "Keep left";
                case Instruction.KEEP_RIGHT:
                    return "Keep right";
                case Instruction.TURN_LEFT:
                    return "Turn left " + course;
                case Instruction.TURN_SHARP_LEFT:
                    return "Turn sharp " + course;
                case Instruction.TURN_SLIGHT_LEFT:
                    return "Turn slight left " + course;
                case Instruction.TURN_RIGHT:
                    return "Turn right " + course;
                case Instruction.TURN_SHARP_RIGHT:
                    return "Turn sharp right " + course;
                case Instruction.TURN_SLIGHT_RIGHT:
                    return "Turn slight right " + course;
                case Instruction.FINISH:
                    return "Finish";
                case Instruction.REACHED_VIA:
                    return "Via point";
                case Instruction.LEAVE_ROUNDABOUT:
                    return "Leave roundabout";
                case Instruction.USE_ROUNDABOUT:
                    return "Use roundabout";
                case Instruction.CONTINUE_ON_STREET:
                case Instruction.IGNORE:
                case Instruction.UNKNOWN:
                default:
                    return "Continue straight";
            }
        }

        public int getSign() {
            if (sign != Instruction.UNDEFINED)
                return sign;
            if (previous == null) {
                signInt = Instruction.START;
            } else if (next == null) {
                signInt = Instruction.FINISH;
            } else {
                double nextCourse = this.bearingTo(next);
                double prevCourse = previous.bearingTo(this);
                // turn
                turn = Math.round(nextCourse - prevCourse);
                if (Math.abs(turn) > 180) {
                    turn = turn - (long) (Math.signum(turn)) * 360;
                }
                if (Math.abs(turn) < 5) {
                    signInt = Instruction.CONTINUE_ON_STREET;
                } else if (Math.abs(turn) < 60) {
                    signInt = turn > 0 ? Instruction.TURN_SLIGHT_RIGHT : Instruction.TURN_SLIGHT_LEFT;
                } else if (Math.abs(turn) < 110) {
                    signInt = turn > 0 ? Instruction.TURN_RIGHT : Instruction.TURN_LEFT;
                } else if (Math.abs(turn) < 170) {
                    signInt = turn > 0 ? Instruction.TURN_SHARP_RIGHT : Instruction.TURN_SHARP_LEFT;
                } else {
                    signInt = turn > 0 ? Instruction.U_TURN_RIGHT : Instruction.U_TURN_LEFT;
                }
            }
            return signInt;
        }

        public Instruction getPrevious() {
            return previous;
        }

        public Instruction getNext() {
            return next;
        }

        public int position() {
            int pos = 0;
            Instruction prev = previous;
            while (prev != null) {
                pos++;
                prev = prev.previous;
            }
            return pos;
        }

        private void reset() {
            signInt = Instruction.UNDEFINED;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            } else if (!(obj instanceof Instruction)) {
                return false;
            }
            Instruction other = (Instruction) obj;
            return sign == other.sign && Objects.equals(text, other.text) && super.equals(other);
        }
    }

    public interface UpdateListener {
        void onRouteChanged();
    }
}
