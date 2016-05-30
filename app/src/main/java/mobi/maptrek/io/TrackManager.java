package mobi.maptrek.io;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

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

import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.util.ProgressListener;

public class TrackManager extends Manager {
    public static final String EXTENSION = ".mtrack";
    public static final int VERSION = 1;

    private static final int FIELD_VERSION = 1;
    private static final int FIELD_POINT = 2;
    private static final int FIELD_NAME = 3;
    private static final int FIELD_COLOR = 4;
    private static final int FIELD_WIDTH = 5;

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
                    track.name = input.readBytes().toStringUtf8();
                    break;
                }
                case FIELD_COLOR: {
                    track.style.color = input.readUInt32();
                    break;
                }
                case FIELD_WIDTH: {
                    track.style.width = input.readFloat();
                    break;
                }
            }
        }
        inputStream.close();
        track.id = 31 * filePath.hashCode() + 1;
        FileDataSource dataSource = new FileDataSource();
        dataSource.name = track.name;
        dataSource.tracks.add(track);
        track.source = dataSource;
        dataSource.propertiesOffset = propertiesOffset;
        return dataSource;
    }

    @Override
    public void saveData(OutputStream outputStream, FileDataSource source, @Nullable ProgressListener progressListener) throws Exception {
        if (!source.isNativeTrack())
            throw new Exception("Only single track can be saved in mtrack format");
        Track track = source.tracks.get(0);
        if (progressListener != null)
            progressListener.onProgressStarted(track.points.size());
        CodedOutputStream output = CodedOutputStream.newInstance(outputStream);
        output.writeUInt32(FIELD_VERSION, VERSION);
        int progress = 0;
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
        output.writeBytes(FIELD_NAME, ByteString.copyFromUtf8(track.name));
        output.writeUInt32(FIELD_COLOR, track.style.color);
        output.writeFloat(FIELD_WIDTH, track.style.width);
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
     * Saves track properties by modifying only file tail.
     */
    public void saveProperties(FileDataSource source) throws Exception {
        Track track = source.tracks.get(0);
        // Prepare new properties tail
        ByteBuffer buffer = ByteBuffer.allocate(getSerializedPropertiesSize(track));
        CodedOutputStream output = CodedOutputStream.newInstance(buffer);
        output.writeBytes(FIELD_NAME, ByteString.copyFromUtf8(track.name));
        output.writeUInt32(FIELD_COLOR, track.style.color);
        output.writeFloat(FIELD_WIDTH, track.style.width);
        output.flush();
        // Modify tail of file
        File file = new File(source.path);
        RandomAccessFile access = new RandomAccessFile(file, "rw");
        access.setLength(source.propertiesOffset + 1);
        access.seek(source.propertiesOffset);
        access.write(buffer.array());
        access.close();
    }

    public int getSerializedPropertiesSize(Track track) {
        int size = 0;
        size += CodedOutputStream.computeStringSize(FIELD_NAME, track.name);
        size += CodedOutputStream.computeUInt32Size(FIELD_COLOR, track.style.color);
        size += CodedOutputStream.computeFloatSize(FIELD_WIDTH, track.style.width);
        return size;
    }
}
