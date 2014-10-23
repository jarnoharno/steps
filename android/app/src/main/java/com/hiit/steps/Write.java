package com.hiit.steps;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.protobuf.CodedOutputStream;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class Write {

    public interface WriteClient extends StepsService.ContextClient {
    }

    public Write(WriteClient writeClient) {
        this.writeClient = writeClient;
    }

    public void send(byte[] data) {
        if (codedOutputStream == null) {
            return;
        }
        try {
            // write buffer size as varint first
            codedOutputStream.writeInt32NoTag(data.length);
            codedOutputStream.writeRawBytes(data);
            rows++;
        } catch (IOException e) {
            close();
            reset();
        }
    }

    public void renameFile(String fileName) {
        this.fileName = fileName;
    }

    public void startTrace() {
        rows = 0;
        fileName = null;
        try {
            file = File.createTempFile(FILE_PREFIX, FILE_SUFFIX,
                    writeClient.getContext().getCacheDir());
            bufferedOutputStream =
                    new BufferedOutputStream(new FileOutputStream(file));
            codedOutputStream =
                    CodedOutputStream.newInstance(bufferedOutputStream);
            writeClient.print("writing to " + file.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopTrace() {
        close();
        Context context = writeClient.getContext();
        if (FileSystem.isExternalStorageWritable()) {
            // move to files dir
            File externalFolder = context.getExternalFilesDir(null);
            File externalFile;
            if (fileName == null) {
                externalFile = FileSystem.nextAvailableFile(
                        FILE_PREFIX, FILE_SUFFIX, externalFolder);
            } else {
                externalFile = new File(externalFolder, FILE_PREFIX + fileName);
            }
            try {
                FileSystem.moveFile(file, externalFile);
                file = externalFile;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String msg = String.format("wrote %d rows (%s) to %s", rows,
                FileSystem.byteString(file.length()), file.toString());
        reset();
        writeClient.print(msg);
        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
        toast.show();
    }

    // private

    private void close() {
        try {
            codedOutputStream.flush();
            bufferedOutputStream.close();
        } catch (IOException e) {
            writeClient.print("Error closing file");
        }
    }

    private void reset() {
        bufferedOutputStream = null;
        codedOutputStream = null;
        file = null;
        rows = 0;
        fileName = null;
    }

    private static final String FILE_PREFIX = "steps";
    private static final String FILE_SUFFIX = "";

    private WriteClient writeClient;

    private File file;
    private BufferedOutputStream bufferedOutputStream;
    private CodedOutputStream codedOutputStream;

    private int rows;
    private String fileName;
}
