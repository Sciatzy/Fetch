package com.fetch.auth.production.util;

import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public final class StorageBackedImageLoader {

    private static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;

    private StorageBackedImageLoader() {
    }

    public static void load(
            ImageView imageView,
            @Nullable String source,
            @DrawableRes int placeholderResId,
            boolean circleCrop
    ) {
        if (TextUtils.isEmpty(source)) {
            imageView.setImageResource(placeholderResId);
            return;
        }

        imageView.setTag(source);
        StorageReference storageReference = resolveStorageReference(source);
        if (storageReference != null) {
            imageView.setImageResource(placeholderResId);
            storageReference.getBytes(MAX_IMAGE_BYTES)
                    .addOnSuccessListener(bytes -> {
                        Object tag = imageView.getTag();
                        String currentTag = tag instanceof String ? (String) tag : null;
                        if (!TextUtils.equals(currentTag, source)) {
                            return;
                        }
                        RequestBuilder<?> request = Glide.with(imageView.getContext())
                                .load(bytes)
                                .placeholder(placeholderResId)
                                .error(placeholderResId);
                        if (circleCrop) {
                            request = request.circleCrop();
                        }
                        request.into(imageView);
                    })
                    .addOnFailureListener(error -> {
                        Object tag = imageView.getTag();
                        String currentTag = tag instanceof String ? (String) tag : null;
                        if (TextUtils.equals(currentTag, source)) {
                            imageView.setImageResource(placeholderResId);
                        }
                    });
            return;
        }

        RequestBuilder<?> request = Glide.with(imageView.getContext())
                .load(source)
                .placeholder(placeholderResId)
                .error(placeholderResId);
        if (circleCrop) {
            request = request.circleCrop();
        }
        request.into(imageView);
    }

    @Nullable
    private static StorageReference resolveStorageReference(String source) {
        if (source.startsWith("gs://")) {
            return FirebaseStorage.getInstance().getReferenceFromUrl(source);
        }
        if (source.startsWith("/")) {
            return FirebaseStorage.getInstance().getReference().child(source.substring(1));
        }
        if (!source.startsWith("http://")
                && !source.startsWith("https://")
                && !source.startsWith("data:")
                && source.contains("/")) {
            return FirebaseStorage.getInstance().getReference().child(source);
        }
        return null;
    }
}
