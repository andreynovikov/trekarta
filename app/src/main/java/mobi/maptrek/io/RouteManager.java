/*
 * Copyright 2023 Andrey Novikov
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

package mobi.maptrek.io;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.WireFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import mobi.maptrek.data.Route;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.style.RouteStyle;
import mobi.maptrek.util.ProgressListener;

public class RouteManager extends Manager {
    public static final String EXTENSION = ".mroute";
    public static final int VERSION = 1;

    private static final int FIELD_VERSION = 1;
    private static final int FIELD_INSTRUCTION = 2;
    private static final int FIELD_POINT = 3;
    private static final int FIELD_NAME = 4;
    private static final int FIELD_DESCRIPTION = 5;
    private static final int FIELD_COLOR = 6;
    private static final int FIELD_WIDTH = 7;

    private static final int FIELD_INSTRUCTION_LATITUDE = 1;
    private static final int FIELD_INSTRUCTION_LONGITUDE = 2;
    private static final int FIELD_INSTRUCTION_TEXT = 3;
    private static final int FIELD_INSTRUCTION_SIGN = 4;

    private static final int FIELD_POINT_LATITUDE = 1;
    private static final int FIELD_POINT_LONGITUDE = 2;
    private static final int FIELD_POINT_ALTITUDE = 3;
    private static final int FIELD_POINT_SPEED = 4;
    private static final int FIELD_POINT_BEARING = 5;
    private static final int FIELD_POINT_ACCURACY = 6;
    private static final int FIELD_POINT_TIMESTAMP = 7;
    private static final int FIELD_POINT_CONTINUOUS = 8;

    @NonNull
    @Override
    public FileDataSource loadData(InputStream inputStream, String filePath) throws Exception {
        long propertiesOffset = 0L;
        Route route = new Route();
        Track track = new Track();
        CodedInputStream input = CodedInputStream.newInstance(inputStream);
        boolean done = false;
        while (!done) {
            long offset = input.getTotalBytesRead();
            int tag = input.readTag();
            int field = WireFormat.getTagFieldNumber(tag);
            switch (field) {
                case 0:
                    done = true;
                    break;
                default: {
                    throw new com.google.protobuf.InvalidProtocolBufferException("Unsupported proto field: " + tag);
                }
                case FIELD_VERSION: {
                    // skip version
                    input.skipField(tag);
                    break;
                }
                case FIELD_INSTRUCTION: {
                    int length = input.readRawVarint32();
                    int oldLimit = input.pushLimit(length);
                    readInstruction(route, input);
                    input.popLimit(oldLimit);
                    input.checkLastTagWas(0);
                    break;
                }
                case FIELD_POINT: {
                    int length = input.readRawVarint32();
                    int oldLimit = input.pushLimit(length);
                    readPoint(track, input);
                    input.popLimit(oldLimit);
                    input.checkLastTagWas(0);
                    break;
                }
                case FIELD_NAME: {
                    propertiesOffset = offset;
                    route.name = input.readBytes().toStringUtf8();
                    break;
                }
                case FIELD_DESCRIPTION: {
                    route.description = input.readBytes().toStringUtf8();
                    break;
                }
                case FIELD_COLOR: {
                    route.style.color = input.readUInt32();
                    track.style.color = route.style.color;
                    break;
                }
                case FIELD_WIDTH: {
                    route.style.width = input.readFloat();
                    break;
                }
            }
        }
        inputStream.close();
        route.id = 31 * filePath.hashCode() + 1;
        FileDataSource dataSource = new FileDataSource();
        dataSource.name = route.name;
        dataSource.routes.add(route);
        route.source = dataSource;
        if (track.points.size() > 0) {
            dataSource.tracks.add(track);
            track.source = dataSource;
        }
        dataSource.propertiesOffset = propertiesOffset;
        return dataSource;
    }

    @Override
    public void saveData(OutputStream outputStream, FileDataSource source, @Nullable ProgressListener progressListener) throws Exception {
        if (source.routes.size() != 1 || source.tracks.size() > 1)
            throw new Exception("Only single route can be saved in mroute format");
        Route route = source.routes.get(0);
        if (progressListener != null) {
            int length = route.size();
            if (source.tracks.size() > 0)
                length += source.tracks.get(0).points.size();
            progressListener.onProgressStarted(length);
        }
        CodedOutputStream output = CodedOutputStream.newInstance(outputStream);
        output.writeUInt32(FIELD_VERSION, VERSION);
        int progress = 0;
        for (Route.Instruction instruction : route.getInstructions()) {
            output.writeTag(FIELD_INSTRUCTION, WireFormat.WIRETYPE_LENGTH_DELIMITED);
            output.writeRawVarint32(getSerializedInstructionSize(instruction));
            output.writeInt32(FIELD_INSTRUCTION_LATITUDE, instruction.latitudeE6);
            output.writeInt32(FIELD_INSTRUCTION_LONGITUDE, instruction.longitudeE6);
            if (instruction.text != null)
                output.writeString(FIELD_INSTRUCTION_TEXT, instruction.text);
            if (instruction.sign != Route.Instruction.UNDEFINED)
                output.writeInt32(FIELD_INSTRUCTION_SIGN, instruction.sign);
            progress++;
            if (progressListener != null)
                progressListener.onProgressChanged(progress);
        }
        if (source.tracks.size() > 0) {
            Track track = source.tracks.get(0);
            for (Track.TrackPoint point : track.points) {
                output.writeTag(FIELD_POINT, WireFormat.WIRETYPE_LENGTH_DELIMITED);
                output.writeRawVarint32(getSerializedPointSize(point));
                output.writeInt32(FIELD_POINT_LATITUDE, point.latitudeE6);
                output.writeInt32(FIELD_POINT_LONGITUDE, point.longitudeE6);
                output.writeFloat(FIELD_POINT_ALTITUDE, point.elevation);
                output.writeFloat(FIELD_POINT_SPEED, point.speed);
                output.writeFloat(FIELD_POINT_BEARING, point.bearing);
                output.writeFloat(FIELD_POINT_ACCURACY, point.accuracy);
                output.writeUInt64(FIELD_POINT_TIMESTAMP, point.time);
                if (!point.continuous)
                    //noinspection ConstantConditions
                    output.writeBool(8, point.continuous);
                progress++;
                if (progressListener != null)
                    progressListener.onProgressChanged(progress);
            }
        }

        output.writeBytes(FIELD_NAME, ByteString.copyFromUtf8(route.name));
        if (route.description != null)
            output.writeBytes(FIELD_DESCRIPTION, ByteString.copyFromUtf8(route.description));
        if (route.style.color != RouteStyle.DEFAULT_COLOR)
            output.writeUInt32(FIELD_COLOR, route.style.color);
        if (route.style.width != RouteStyle.DEFAULT_WIDTH)
            output.writeFloat(FIELD_WIDTH, route.style.width);
        output.flush();
        outputStream.close();
        if (progressListener != null)
            progressListener.onProgressFinished();
    }

    @NonNull
    @Override
    public String getExtension() {
        return EXTENSION;
    }

    private void readInstruction(Route route, CodedInputStream input) throws IOException {
        int latitudeE6 = 0;
        int longitudeE6 = 0;
        String text = null;
        int sign = Route.Instruction.UNDEFINED;

        boolean done = false;
        while (!done) {
            int tag = input.readTag();
            int field = WireFormat.getTagFieldNumber(tag);
            switch (field) {
                case 0:
                    done = true;
                    break;
                default: {
                    throw new com.google.protobuf.InvalidProtocolBufferException("Unsupported proto field: " + tag);
                }
                case FIELD_INSTRUCTION_LATITUDE: {
                    latitudeE6 = input.readInt32();
                    break;
                }
                case FIELD_INSTRUCTION_LONGITUDE: {
                    longitudeE6 = input.readInt32();
                    break;
                }
                case FIELD_INSTRUCTION_TEXT: {
                    text = input.readString();
                    break;
                }
                case FIELD_INSTRUCTION_SIGN: {
                    sign = input.readInt32();
                    break;
                }
            }
        }
        route.addInstruction(latitudeE6, longitudeE6, text, sign);
    }

    private void readPoint(Track track, CodedInputStream input) throws IOException {
        int latitudeE6 = 0;
        int longitudeE6 = 0;
        boolean continuous = true;
        float altitude = Float.NaN;
        float speed = Float.NaN;
        float bearing = Float.NaN;
        float accuracy = Float.NaN;
        long timestamp = 0L;

        boolean done = false;
        while (!done) {
            int tag = input.readTag();
            int field = WireFormat.getTagFieldNumber(tag);
            switch (field) {
                case 0:
                    done = true;
                    break;
                default: {
                    throw new com.google.protobuf.InvalidProtocolBufferException("Unsupported proto field: " + tag);
                }
                case FIELD_POINT_LATITUDE: {
                    latitudeE6 = input.readInt32();
                    break;
                }
                case FIELD_POINT_LONGITUDE: {
                    longitudeE6 = input.readInt32();
                    break;
                }
                case FIELD_POINT_ALTITUDE: {
                    altitude = input.readFloat();
                    break;
                }
                case FIELD_POINT_SPEED: {
                    speed = input.readFloat();
                    break;
                }
                case FIELD_POINT_BEARING: {
                    bearing = input.readFloat();
                    break;
                }
                case FIELD_POINT_ACCURACY: {
                    accuracy = input.readFloat();
                    break;
                }
                case FIELD_POINT_TIMESTAMP: {
                    timestamp = input.readUInt64();
                    break;
                }
                case FIELD_POINT_CONTINUOUS: {
                    continuous = input.readBool();
                    break;
                }
            }
        }
        track.addPointFast(continuous, latitudeE6, longitudeE6, altitude, speed, bearing, accuracy, timestamp);
    }

    public int getSerializedInstructionSize(Route.Instruction instruction) {
        int size = 0;
        size += CodedOutputStream.computeInt32Size(FIELD_INSTRUCTION_LATITUDE, instruction.latitudeE6);
        size += CodedOutputStream.computeInt32Size(FIELD_INSTRUCTION_LONGITUDE, instruction.longitudeE6);
        if (instruction.text != null)
            size += CodedOutputStream.computeStringSize(FIELD_INSTRUCTION_TEXT, instruction.text);
        if (instruction.sign != Route.Instruction.UNDEFINED)
            size += CodedOutputStream.computeInt32Size(FIELD_INSTRUCTION_SIGN, instruction.sign);
        return size;
    }

    public int getSerializedPointSize(Track.TrackPoint point) {
        int size = 0;
        size += CodedOutputStream.computeInt32Size(FIELD_POINT_LATITUDE, point.latitudeE6);
        size += CodedOutputStream.computeInt32Size(FIELD_POINT_LONGITUDE, point.longitudeE6);
        size += CodedOutputStream.computeFloatSize(FIELD_POINT_ALTITUDE, point.elevation);
        size += CodedOutputStream.computeFloatSize(FIELD_POINT_SPEED, point.speed);
        size += CodedOutputStream.computeFloatSize(FIELD_POINT_BEARING, point.bearing);
        size += CodedOutputStream.computeFloatSize(FIELD_POINT_ACCURACY, point.accuracy);
        size += CodedOutputStream.computeUInt64Size(FIELD_POINT_TIMESTAMP, point.time);
        if (!point.continuous) {
            //noinspection ConstantConditions
            size += CodedOutputStream.computeBoolSize(FIELD_POINT_CONTINUOUS, point.continuous);
        }
        return size;
    }

    /**
     * Saves route properties by modifying only file tail.
     */
    public void saveProperties(FileDataSource source) throws Exception {
        Route route = source.routes.get(0);
        // Prepare new properties tail
        ByteBuffer buffer = ByteBuffer.allocate(getSerializedPropertiesSize(route));
        CodedOutputStream output = CodedOutputStream.newInstance(buffer);
        output.writeBytes(FIELD_NAME, ByteString.copyFromUtf8(route.name));
        output.writeBytes(FIELD_DESCRIPTION, ByteString.copyFromUtf8(route.description));
        output.writeUInt32(FIELD_COLOR, route.style.color);
        output.writeFloat(FIELD_WIDTH, route.width);
        output.flush();
        // Modify tail of file
        File file = new File(source.path);
        long createTime = file.lastModified();
        RandomAccessFile access = new RandomAccessFile(file, "rw");
        access.setLength(source.propertiesOffset + 1);
        access.seek(source.propertiesOffset);
        access.write(buffer.array());
        access.close();
        //noinspection ResultOfMethodCallIgnored
        file.setLastModified(createTime);
    }

    public int getSerializedPropertiesSize(Route route) {
        int size = 0;
        size += CodedOutputStream.computeStringSize(FIELD_NAME, route.name);
        size += CodedOutputStream.computeStringSize(FIELD_DESCRIPTION, route.description);
        size += CodedOutputStream.computeUInt32Size(FIELD_COLOR, route.style.color);
        size += CodedOutputStream.computeFloatSize(FIELD_WIDTH, route.style.width);
        return size;
    }
}
