package com.example.shortvideod.popup;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.databinding.DataBindingUtil;
import com.example.shortvideod.databinding.ItemPopupDiscardReliteBinding;
import com.example.shortvideod.databinding.ItemSimplepopupBinding;
import com.example.shortvideod.R;
import com.example.shortvideod.util.SessionManager;

public class PopupBuilder {
    Dialog mBuilder;
    private final Context mContext;
    SessionManager sessionManager;

    public interface OnPopupClickListner {
        void onClickCountinue();
    }

    public interface OnRcoinConvertPopupClickListner {
        void onClickConvert(int i);
    }

    public PopupBuilder(Context context) {
        this.mContext = context;
        if (context != null) {
            this.sessionManager = new SessionManager(context);
            Dialog dialog = new Dialog(context);
            this.mBuilder = dialog;
            dialog.requestWindowFeature(1);
            this.mBuilder.setCancelable(false);
            this.mBuilder.setCanceledOnTouchOutside(false);
            Dialog dialog2 = this.mBuilder;
            if (dialog2 != null && dialog2.getWindow() != null) {
                this.mBuilder.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
        }
    }

    public void showSimplePopup(String s, String btnText, OnPopupClickListner onPopupClickListner) {
        if (this.mContext != null) {
            this.mBuilder.setCancelable(true);
            this.mBuilder.setCanceledOnTouchOutside(true);
            ItemSimplepopupBinding binding = (ItemSimplepopupBinding) DataBindingUtil.inflate(LayoutInflater.from(this.mContext), R.layout.item_simplepopup, (ViewGroup) null, false);
            this.mBuilder.setContentView(binding.getRoot());
            binding.tvText.setText(s);
            binding.btncountinue.setText(btnText);

            binding.btncountinue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showSimplePopupClick(onPopupClickListner,view);
                }
            });

            this.mBuilder.show();
        }
    }

    public  void showSimplePopupClick(OnPopupClickListner onPopupClickListner, View v) {
        this.mBuilder.dismiss();
        onPopupClickListner.onClickCountinue();
    }

    public void showReliteDiscardPopup(String s1, String s2, String btn1, String btn2, OnPopupClickListner onPopupClickListner) {
        if (this.mContext != null) {
            this.mBuilder.setCancelable(true);
            this.mBuilder.setCanceledOnTouchOutside(true);
            ItemPopupDiscardReliteBinding binding = (ItemPopupDiscardReliteBinding) DataBindingUtil.inflate(LayoutInflater.from(this.mContext), R.layout.item_popup_discard_relite, (ViewGroup) null, false);
            this.mBuilder.setContentView(binding.getRoot());
            binding.tvText.setText(s1);
            binding.tvText2.setText(s2);
            binding.btncountinue.setText(btn1);
            binding.btnCancel.setText(btn2);
            binding.btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cancelPopUp(view);
                }
            });
            binding.btncountinue.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            continuewPopup( onPopupClickListner,view);
                        }
                    }
            );
            if (s1.isEmpty()) {
                binding.tvText.setVisibility(View.GONE);
            }
            if (s2.isEmpty()) {
                binding.tvText2.setVisibility(View.GONE);
            }
            if (btn1.isEmpty()) {
                binding.btncountinue.setVisibility(View.GONE);
            }
            if (btn2.isEmpty()) {
                binding.btnCancel.setVisibility(View.GONE);
            }
            this.mBuilder.show();
        }
    }

    /* renamed from: lambda$showReliteDiscardPopup$1$com-example-shortvideod-popup-PopupBuilder */
    public  void cancelPopUp(View v) {
        this.mBuilder.dismiss();
    }

    /* renamed from: lambda$showReliteDiscardPopup$2$com-example-shortvideod-popup-PopupBuilder */
    public /* synthetic */ void continuewPopup(OnPopupClickListner onPopupClickListner, View v) {
        this.mBuilder.dismiss();
        onPopupClickListner.onClickCountinue();
    }
}
