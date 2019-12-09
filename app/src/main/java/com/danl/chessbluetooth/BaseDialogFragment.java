package com.danl.chessbluetooth;

import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;

public abstract class BaseDialogFragment extends AppCompatDialogFragment {

    @Override
    public void showNow(@NonNull FragmentManager manager, @Nullable String tag) {
        super.showNow(manager, tag);
        Window dialogWindow = requireDialog().getWindow();
        if (dialogWindow != null) {
            dialogWindow.getDecorView().setSystemUiVisibility(requireActivity().getWindow().getDecorView().getSystemUiVisibility());
            dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }
    }

}
