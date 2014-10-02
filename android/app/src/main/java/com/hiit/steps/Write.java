package com.hiit.steps;

import android.content.Context;
import android.widget.Toast;

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
        if (stream == null) {
            return;
        }
        try {
            stream.write(data);
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
            stream = new BufferedOutputStream(new FileOutputStream(file));
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
                externalFile = new File(externalFolder, fileName);
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
            stream.close();
        } catch (IOException e) {
            writeClient.print("Error closing file");
        }
    }

    private void reset() {
        stream = null;
        file = null;
        rows = 0;
        fileName = null;
    }

    private static final String FILE_PREFIX = "steps";
    private static final String FILE_SUFFIX = "";

    private WriteClient writeClient;

    private File file;
    private BufferedOutputStream stream;

    private int rows;
    private String fileName;
}
