package com.ahmedtrooper.prism;

import android.net.Uri;
import android.view.View;

import androidx.annotation.NonNull;

import com.ahmedtrooper.prism.filepicker.DocumentPickerFragment;

public class PrismDocumentPickerFragment extends DocumentPickerFragment {

    public PrismDocumentPickerFragment(@NonNull Uri root) {
        super(root);
    }

    @Override
    public void onClickCheckable(@NonNull View view, @NonNull FileViewHolder vh) {
        mListener.onDocumentPicked(vh.file, false);
    }

    @Override
    public boolean onLongClickCheckable(@NonNull View view, @NonNull DirViewHolder vh) {
        mListener.onDocumentPicked(vh.file, true);
        return true;
    }

    public boolean isBackTop() {
        return mCurrentPath.equals(getRoot());
    }
}
