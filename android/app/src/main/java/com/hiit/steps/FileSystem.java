package com.hiit.steps;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileSystem {
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static void copyFile(File src, File dst) throws IOException
    {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    public static void moveFile(File src, File dst) throws IOException
    {
        copyFile(src, dst);
        src.delete();
    }

    public static File nextAvailableFile(String prefix,
                                         File dir) {
        return nextAvailableFile(prefix, "", dir);
    }

    public static File nextAvailableFile(String prefix,
                                         String suffix,
                                         File dir) {
        return nextAvailableFile(prefix, suffix, dir, 3);
    }


    public static File nextAvailableFile(String prefix,
                                         String suffix,
                                         File dir,
                                         int idLength) {
        File file;
        if (idLength < 1 || idLength > 9) {
            throw new IllegalArgumentException("idLength must be in range [1, 9]");
        }
        int max = FileSystem.pow(10, idLength - 1);
        int i = 0;
        do {
            if (i == max) {
                return nextAvailableFile(prefix, suffix, dir, idLength + 1);
            }
            String name = prefix + String.format("%0" + idLength + "d", i++) + suffix;
            file = new File(dir, name);
        } while (file.exists());
        return file;
    }

    private static int pow(int base, int exp) {
        int ret = 1;
        for (int i = 0; i < exp; ++i) {
            ret *= base;
        }
        return ret;
    }

    public static enum Size {

        TB (1L << (10 * 4)),
        GB (1L << (10 * 3)),
        MB (1L << (10 * 2)),
        KB (1L << (10 * 1));

        public long bytes;

        Size(long bytes) {
            this.bytes = bytes;
        }

    }

    public static String byteString(long bytes) {
        for (Size size: Size.values()) {
            if (bytes >= size.bytes) {
                return String.format("%.2f%s",
                        ((double) bytes) / size.bytes, size.name());
            }
        }
        return bytes + "B";
    }

}
