package com.fetch.auth.production.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;

import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;

public final class ImageDataUriUtil {

    private static final int MAX_FIRESTORE_IMAGE_BYTES = 350 * 1024;

    private ImageDataUriUtil() {
    }

    @Nullable
    public static String toJpegDataUri(Context context, Uri uri, int maxDimension, int jpegQuality) {
        try {
            ContentResolver resolver = context.getContentResolver();
            Bitmap original = MediaStore.Images.Media.getBitmap(resolver, uri);
            Bitmap working = scaleDown(original, maxDimension);

            for (int resizeAttempt = 0; resizeAttempt < 4; resizeAttempt++) {
                int quality = Math.min(95, Math.max(20, jpegQuality));
                while (quality >= 20) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    working.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                    byte[] bytes = baos.toByteArray();
                    if (bytes.length <= MAX_FIRESTORE_IMAGE_BYTES) {
                        String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                        return "data:image/jpeg;base64," + base64;
                    }
                    quality -= 5;
                }

                int nextWidth = Math.max(1, Math.round(working.getWidth() * 0.8f));
                int nextHeight = Math.max(1, Math.round(working.getHeight() * 0.8f));
                if (nextWidth == working.getWidth() || nextHeight == working.getHeight()) {
                    break;
                }
                working = Bitmap.createScaledBitmap(working, nextWidth, nextHeight, true);
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Nullable
    public static String toPngDataUri(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            return "data:image/png;base64," + base64;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Bitmap scaleDown(Bitmap source, int maxDimension) {
        int width = source.getWidth();
        int height = source.getHeight();
        int largest = Math.max(width, height);
        if (largest <= maxDimension) {
            return source;
        }
        float ratio = (float) maxDimension / (float) largest;
        int targetWidth = Math.round(width * ratio);
        int targetHeight = Math.round(height * ratio);
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true);
    }
}
