package com.example.shortvideod.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import com.daasuu.gpuv.egl.filter.GlFilter;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.example.segmentedprogressbar.ProgressBarListener;
import com.example.segmentedprogressbar.SegmentedProgressBar;
import com.example.shortvideod.R;
import com.example.shortvideod.adapter.FilterRecordAdapter;
import com.example.shortvideod.databinding.ActivityRecorderBinding;
import com.example.shortvideod.design.Song;
import com.example.shortvideod.design.Sticker;
import com.example.shortvideod.design.StickerView;
import com.example.shortvideod.filter.VideoFilter;
import com.example.shortvideod.filters.ExposureFilter;
import com.example.shortvideod.filters.HazeFilter;
import com.example.shortvideod.filters.MonochromeFilter;
import com.example.shortvideod.filters.PixelatedFilter;
import com.example.shortvideod.filters.SolarizeFilter;
import com.example.shortvideod.popup.PopupBuilder;
import com.example.shortvideod.util.AnimationUtil;
import com.example.shortvideod.util.Const;
import com.example.shortvideod.util.IntentUtil;
import com.example.shortvideod.util.TempUtil;
import com.example.shortvideod.util.TextFormatUtil;
import com.example.shortvideod.util.VideoUtil;
import com.example.shortvideod.workers.FileDownloadWorker;
import com.example.shortvideod.workers.MergeVideosWorker2;
import com.example.shortvideod.workers.VideoSpeedWorker2;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.slider.Slider;
import com.kaopiz.kprogresshud.KProgressHUD;
import com.munon.turboimageview.TurboImageView;
import com.otaliastudios.cameraview.BitmapCallback;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.controls.Flash;
import com.otaliastudios.cameraview.controls.Mode;
import com.otaliastudios.cameraview.filter.Filters;
import com.otaliastudios.cameraview.filters.BrightnessFilter;
import com.otaliastudios.cameraview.filters.GammaFilter;
import com.otaliastudios.cameraview.filters.SharpnessFilter;
import info.hoang8f.android.segmented.SegmentedGroup;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class RecorderActivity extends BaseActivity {
    public static final String EXTRA_AUDIO = "audio";
    public static final String EXTRA_SONG = "song";
    private static final String TAG = "RecorderActivity";
    private GlFilter GFilter = new GlFilter();
    ActivityRecorderBinding binding;
    private GlFilter filter = new GlFilter();
    StickerView mCurrentView;
    /* access modifiers changed from: private */
    public final Handler mHandler = new Handler(Looper.getMainLooper());
    /* access modifiers changed from: private */
    public MediaPlayer mMediaPlayer;
    /* access modifiers changed from: private */
    public RecorderActivityViewModel mModel;
    /* access modifiers changed from: private */
    public KProgressHUD mProgress;
    /* access modifiers changed from: private */
    public YoYo.YoYoString mPulse;
    /* access modifiers changed from: private */
    public final Runnable mStopper = new Runnable() {
        @Override
        public void run() {
            stopRecording();
        }
    };
    /* access modifiers changed from: private */
    public ArrayList<View> mViews = new ArrayList<>();
    File merged;
    File outputPathOverlay;
    int timeInSeconds = 0;

    public static Bitmap getBitmapFromView(View view) {
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(returnedBitmap);
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            bgDrawable.draw(canvas);
        } else {
            canvas.drawColor(-1);
        }
        view.draw(canvas);
        return returnedBitmap;
    }

    public static File getDirPathWithFolder(Context context) {
        File mediaStorageDir = new File(context.getFilesDir().getAbsolutePath(), Const.TEMP_FOLDER_NAME);
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }
        return mediaStorageDir;
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "Received request: " + requestCode + ", result: " + resultCode + ".");
        if (requestCode == 100 && resultCode == -1 && data != null) {
            submitUpload(data.getData());
        } else if (requestCode == 60605 && resultCode == -1 && data != null) {
            setupSong((Song) data.getParcelableExtra("song"), (Uri) data.getParcelableExtra("audio"));
        } else if (requestCode == 60607 && resultCode == -1 && data != null) {
            Sticker stickerDummy = (Sticker) data.getParcelableExtra(StickerPickerActivity.EXTRA_STICKER);
            Log.d(TAG, "onActivityResult: " + stickerDummy.getImage());
            downloadSticker(stickerDummy);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /* access modifiers changed from: protected */
    public void onDestroy() {
        super.onDestroy();
        MediaPlayer mediaPlayer = this.mMediaPlayer;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                this.mMediaPlayer.stop();
            }
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
        }
        for (RecordSegment segment : this.mModel.segments) {
            File file = new File(segment.file);
            if (!file.delete()) {
                Log.v(TAG, "Could not delete record segment file: " + file);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    private void addSticker(File file) {
        ((TurboImageView) findViewById(R.id.stickerTurbo)).addObject((Context) this, BitmapFactory.decodeFile(file.getAbsolutePath()));
        findViewById(R.id.remove).setVisibility(View.VISIBLE);
    }

    /* renamed from: com.example.shortvideod.activity.RecorderActivity$5 */
    static /* synthetic */ class C05025 {
        static final /* synthetic */ int[] $SwitchMap$com$example$shortvideod$filter$VideoFilter;

        static {
            int[] iArr = new int[VideoFilter.values().length];
            $SwitchMap$com$example$shortvideod$filter$VideoFilter = iArr;
            try {
                iArr[VideoFilter.BRIGHTNESS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.EXPOSURE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.GAMMA.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.GRAYSCALE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.HAZE.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.INVERT.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.MONOCHROME.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.PIXELATED.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.POSTERIZE.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.SEPIA.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.SHARP.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.SOLARIZE.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$example$shortvideod$filter$VideoFilter[VideoFilter.VIGNETTE.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
        }
    }

    /* access modifiers changed from: private */
    public void applyPreviewFilter(VideoFilter filter2) {
        switch (C05025.$SwitchMap$com$example$shortvideod$filter$VideoFilter[filter2.ordinal()]) {
            case 1:
                BrightnessFilter glf = (BrightnessFilter) Filters.BRIGHTNESS.newInstance();
                glf.setBrightness(1.2f);
                this.binding.camera.setFilter(glf);
                return;
            case 2:
                this.binding.camera.setFilter(new ExposureFilter());
                return;
            case 3:
                GammaFilter glf2 = (GammaFilter) Filters.GAMMA.newInstance();
                glf2.setGamma(2.0f);
                this.binding.camera.setFilter(glf2);
                return;
            case 4:
                this.binding.camera.setFilter(Filters.GRAYSCALE.newInstance());
                return;
            case 5:
                HazeFilter glf3 = new HazeFilter();
                glf3.setSlope(-0.5f);
                this.binding.camera.setFilter(glf3);
                return;
            case 6:
                this.binding.camera.setFilter(Filters.INVERT_COLORS.newInstance());
                return;
            case 7:
                this.binding.camera.setFilter(new MonochromeFilter());
                return;
            case 8:
                PixelatedFilter glf4 = new PixelatedFilter();
                glf4.setPixel(5.0f);
                this.binding.camera.setFilter(glf4);
                return;
            case 9:
                this.binding.camera.setFilter(Filters.POSTERIZE.newInstance());
                return;
            case 10:
                this.binding.camera.setFilter(Filters.SEPIA.newInstance());
                return;
            case 11:
                SharpnessFilter glf5 = (SharpnessFilter) Filters.SHARPNESS.newInstance();
                glf5.setSharpness(0.25f);
                this.binding.camera.setFilter(glf5);
                return;
            case 12:
                this.binding.camera.setFilter(new SolarizeFilter());
                return;
            case 13:
                this.binding.camera.setFilter(Filters.VIGNETTE.newInstance());
                return;
            default:
                this.binding.camera.setFilter(Filters.NONE.newInstance());
                return;
        }
    }

    private void applyVideoSpeed(File file, float speed, long duration) {
        File output = TempUtil.createNewFile((Context) this, ".mp4");
        this.mProgress = KProgressHUD.create(this).setStyle(KProgressHUD.Style.SPIN_INDETERMINATE).setLabel(getString(R.string.progress_title)).setCancellable(false).setCancellable(false).show();
        OneTimeWorkRequest request = (OneTimeWorkRequest) ((OneTimeWorkRequest.Builder) new OneTimeWorkRequest.Builder(VideoSpeedWorker2.class).setInputData(new Data.Builder().putString("input", file.getAbsolutePath()).putString("output", output.getAbsolutePath()).putFloat(VideoSpeedWorker2.KEY_SPEED, speed).build())).build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue((WorkRequest) request);
        wm.getWorkInfoByIdLiveData(request.getId()).observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        confirmVideoSpeed(output,duration, workInfo);
                    }
                });
   //     wm.getWorkInfoByIdLiveData(request.getId()).observe(this, new RecorderActivity$$ExternalSyntheticLambda13(this, output, duration));
    }

    /* mo4610xc7d17bb9 renamed from: lambda$applyVideoSpeed$0$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void confirmVideoSpeed (File output, long duration, WorkInfo info2) {
        if (info2.getState() == WorkInfo.State.CANCELLED || info2.getState() == WorkInfo.State.FAILED || info2.getState() == WorkInfo.State.SUCCEEDED) {
            this.mProgress.dismiss();
        }
        if (info2.getState() == WorkInfo.State.SUCCEEDED) {
            RecordSegment segment = new RecordSegment();
            segment.file = output.getAbsolutePath();
            segment.duration = duration;
            this.mModel.segments.add(segment);
        }
    }

    @AfterPermissionGranted(200)
    private void chooseVideoForUpload() {
        IntentUtil.startChooser((Activity) this, 100, MimeTypes.VIDEO_MP4);
    }

    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.binding = (ActivityRecorderBinding) DataBindingUtil.setContentView(this, R.layout.activity_recorder);
        this.mModel = (RecorderActivityViewModel) new ViewModelProvider(this).get(RecorderActivityViewModel.class);
        Song songDummy = (Song) getIntent().getParcelableExtra("song");
        Uri audio = (Uri) getIntent().getParcelableExtra("audio");
        if (audio != null) {
            setupSong(songDummy, audio);
        }
        this.binding.camera.setLifecycleOwner(this);
        this.binding.camera.setMode(Mode.VIDEO);
        this.binding.close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeVideo(view);
            }
        });
        this.binding.done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoDone(view);
            }
        });
        this.binding.flip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flipVideo(view);
            }
        });
        this.binding.flash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flashVideo(view);
            }
        });
        this.binding.viewsticker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                viewVideoSticker(view);
            }
        });
        SegmentedGroup speeds = (SegmentedGroup) findViewById(R.id.speeds);
        View speed = findViewById(R.id.speed);
        speed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoSpeed(speeds,view);
            }
        });
        speed.setVisibility(Build.VERSION.SDK_INT >= 23 ? View.VISIBLE : View.GONE);
        RadioButton speed05x = (RadioButton) findViewById(R.id.speed05x);
        RadioButton speed075x = (RadioButton) findViewById(R.id.speed075x);
        RadioButton speed1x = (RadioButton) findViewById(R.id.speed1x);
        RadioButton speed15x = (RadioButton) findViewById(R.id.speed15x);
        RadioButton speed2x = (RadioButton) findViewById(R.id.speed2x);
        speed05x.setChecked(this.mModel.speed == 0.5f);
        speed075x.setChecked(this.mModel.speed == 0.75f);
        speed1x.setChecked(this.mModel.speed == 1.0f);
        speed15x.setChecked(this.mModel.speed == 1.5f);
        speed2x.setChecked(this.mModel.speed == 2.0f);
     //   RadioGroup recorderActivity$$ExternalSyntheticLambda9;
        RadioButton radioButton = speed15x;
        RadioButton radioButton2 = speed1x;
        RecorderActivity$$ExternalSyntheticLambda9 recorderActivity$$ExternalSyntheticLambda9 =
                new RecorderActivity$$ExternalSyntheticLambda9(this, speed05x, speed075x, speed1x, speed15x, speed2x);
        speeds.setOnCheckedChangeListener(recorderActivity$$ExternalSyntheticLambda9);
        this.binding.filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFilter(view);
            }
        });
        TurboImageView stickers = (TurboImageView) findViewById(R.id.stickerTurbo);
        this.binding.camera.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, @SuppressLint("ClickableViewAccessibility") MotionEvent motionEvent) {
                return   stickers.onTouchEvent(motionEvent);
            }
        });
        View remove = findViewById(R.id.remove);
        remove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            //    stickers, remove 7
            }
        });
        findViewById(R.id.sticker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stickerOnClick(view);
            }
        });
        View sticker = findViewById(R.id.sticker);
        sticker.setVisibility(getResources().getBoolean(R.bool.stickers_enabled) ? View.VISIBLE : View.GONE);
        View sheet = findViewById(R.id.timer_sheet);
        BottomSheetBehavior<View> bsb = BottomSheetBehavior.from(sheet);
     //   ((ImageView) sheet.findViewById(R.id.btnClose)).setOnClickListener(new RecorderActivity$$ExternalSyntheticLambda6(bsb));

        ((ImageView) sheet.findViewById(R.id.btnClose)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    //    ((ImageView) sheet.findViewById(R.id.btnDone)).setOnClickListener(new RecorderActivity$$ExternalSyntheticLambda2(this, bsb));
        ((ImageView) sheet.findViewById(R.id.btnDone)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBtnDoneClick(bsb,view);
            }
        });
       // this.binding.timer.setOnClickListener(new RecorderActivity$$ExternalSyntheticLambda3(this, bsb));
        this.binding.timer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onTimerBsb(bsb,view);
            }
        });
        final TextView maximum = (TextView) findViewById(R.id.maximum);
        TurboImageView turboImageView = stickers;
        //findViewById(R.id.sound).setOnClickListener(new RecorderActivity$$ExternalSyntheticLambda20(this));
        findViewById(R.id.sound).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mo4618x3e883540(view);
            }
        });
        final Slider selection = (Slider) findViewById(R.id.selection);
        View view = remove;
        long duration =  10000;
        //selection.setLabelFormatter(RecorderActivity$$ExternalSyntheticLambda16.INSTANCE);
        //selection.setLabelFormatter(TextFormatUtil.toMMSS(duration));
        View upload = findViewById(R.id.upload);
        View view2 = sticker;
       // upload.setOnClickListener(new RecorderActivity$$ExternalSyntheticLambda21(this));
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mo4619xc59e70c2(view);
            }
        });
        bsb.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            public void onSlide(View v, float offset) {
            }

            public void onStateChanged(View v, int state) {
                if (state == 3) {
                    long max = TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(Const.MAX_DURATION - RecorderActivity.this.mModel.recorded()));
                    selection.setValue(0.0f);
                    selection.setValueTo((float) max);
                    selection.setValue((float) max);
                    maximum.setText(TextFormatUtil.toMMSS(max));
                }
            }
        });
        final SegmentedProgressBar segments = (SegmentedProgressBar) findViewById(R.id.segments);
        Slider slider = selection;
        View view3 = upload;
        segments.enableAutoProgressView(Const.MAX_DURATION);
        segments.setDividerColor(ViewCompat.MEASURED_STATE_MASK);
        segments.setDividerEnabled(true);
        segments.setDividerWidth(2.0f);
       // segments.setListener(RecorderActivity$$ExternalSyntheticLambda14.INSTANCE);
        segments.setListener(new ProgressBarListener() {
            @Override
            public void TimeinMill(long l) {
                lambda$onCreate$18(l);
            }
        });
        segments.setShader(new int[]{ContextCompat.getColor(this, R.color.pink_main), ContextCompat.getColor(this, R.color.pink)});
        this.binding.camera.addCameraListener(new CameraListener() {
            public void onPictureTaken(PictureResult result) {
                super.onPictureTaken(result);
                result.toBitmap(new BitmapCallback() {
                    @Override
                    public void onBitmapReady(@Nullable Bitmap bitmap) {
                       // getFilter(RecorderActivity);
                    }
                });
            }

            /* renamed from: lambda$onPictureTaken$1$com-example-shortvideod-activity-RecorderActivity$2 */
            public /* synthetic */ void mo4633x7607c230(Bitmap bitmap) {
                if (bitmap != null) {
                    FilterRecordAdapter adapter = new FilterRecordAdapter(RecorderActivity.this);
                    adapter.setListener(new FilterRecordAdapter.OnFilterSelectListener() {
                        @Override
                        public void onSelectFilter(VideoFilter filter) {
                         //   getFilter((View)filter);
                        }
                    });
                   // adapter.setListener(new RecorderActivity$2$$ExternalSyntheticLambda0(RecorderActivity.this));
                    RecorderActivity.this.binding.filters.setAdapter(adapter);
                    RecorderActivity.this.binding.filters.setVisibility(View.VISIBLE);
                }
                RecorderActivity.this.mProgress.dismiss();
            }

            public void onVideoRecordingEnd() {
                Log.v(RecorderActivity.TAG, "Video recording has ended.");
                segments.pause();
                segments.addDivider();
                RecorderActivity.this.mHandler.removeCallbacks(RecorderActivity.this.mStopper);
                RecorderActivity.this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        onBtnDoneClick(bsb,view);
                    }
                },500);
                if (RecorderActivity.this.mMediaPlayer != null) {
                    RecorderActivity.this.mMediaPlayer.pause();
                }
                RecorderActivity.this.mPulse.stop();
                RecorderActivity.this.binding.record.setSelected(false);
                RecorderActivity.this.toggleVisibility(true);
            }

            /* renamed from: lambda$onVideoRecordingEnd$2$com-example-shortvideod-activity-RecorderActivity$2 */
            public /* synthetic */ void mo4634x92e52515() {
                RecorderActivity.this.processCurrentRecording();
            }

            public void onVideoRecordingStart() {
                Log.v(RecorderActivity.TAG, "Video recording has started.");
                segments.resume();
                if (RecorderActivity.this.mMediaPlayer != null) {
                    if (Build.VERSION.SDK_INT >= 23) {
                        float speed = 1.0f;
                        if (RecorderActivity.this.mModel.speed == 0.5f) {
                            speed = 2.0f;
                        } else if (RecorderActivity.this.mModel.speed == 0.75f) {
                            speed = 1.5f;
                        } else if (RecorderActivity.this.mModel.speed == 1.5f) {
                            speed = 0.75f;
                        } else if (RecorderActivity.this.mModel.speed == 2.0f) {
                            speed = 0.5f;
                        }
                        PlaybackParams params = new PlaybackParams();
                        params.setSpeed(speed);
                        RecorderActivity.this.mMediaPlayer.setPlaybackParams(params);
                    }
                    RecorderActivity.this.mMediaPlayer.start();
                }
                YoYo.YoYoString unused = RecorderActivity.this.mPulse = YoYo.with(Techniques.Pulse).repeat(-1).playOn(RecorderActivity.this.binding.record);
                RecorderActivity.this.binding.record.setSelected(true);
                RecorderActivity.this.toggleVisibility(false);
            }
        });
       // this.binding.record.setOnClickListener(new RecorderActivity$$ExternalSyntheticLambda4(this, speeds));
        this.binding.record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordVideo(speeds,view);
            }
        });
    }

    /* renamed from: lambda$onCreate$1$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void closeVideo(View view) {
        confirmClose();
    }

    /* renamed from: lambda$onCreate$2$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void videoDone(View view) {
        if (this.binding.camera.isTakingVideo()) {
            Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT).show();
        } else if (this.mModel.segments.isEmpty()) {
            Toast.makeText(this, R.string.recorder_error_no_clips, Toast.LENGTH_SHORT).show();
        } else {
            commitRecordings(this.mModel.segments, this.mModel.audio);
        }
    }

    /* renamed from: lambda$onCreate$3$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void flipVideo(View view) {
        if (this.binding.camera.isTakingVideo()) {
            Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT).show();
        } else {
            this.binding.camera.toggleFacing();
        }
    }

    /* renamed from: lambda$onCreate$4$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void flashVideo(View view) {
        if (this.binding.camera.isTakingVideo()) {
            Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT).show();
        } else {
            this.binding.camera.setFlash(this.binding.camera.getFlash() == Flash.OFF ? Flash.TORCH : Flash.OFF);
        }
    }

    /* renamed from: lambda$onCreate$5$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void viewVideoSticker(View view) {
        StickerView stickerView = this.mCurrentView;
        if (stickerView != null) {
            stickerView.setInEdit(false);
        }
    }

    /* renamed from: lambda$onCreate$6$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void videoSpeed(SegmentedGroup speeds, View view) {
        int i = 0;
        if (this.binding.camera.isTakingVideo()) {
            Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT).show();
            return;
        }
        if (speeds.getVisibility() == View.VISIBLE) {
            i = View.GONE;
        }
        speeds.setVisibility(i);
    }

    /* renamed from: lambda$onCreate$7$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void OnCheckChanged(RadioButton speed05x, RadioButton speed075x, RadioButton speed1x, RadioButton speed15x, RadioButton speed2x, RadioGroup group, int checked) {
        float factor = 1.0f;
        if (checked != R.id.speed05x) {
            speed05x.setChecked(false);
        } else {
            factor = 0.5f;
        }
        if (checked != R.id.speed075x) {
            speed075x.setChecked(false);
        } else {
            factor = 0.75f;
        }
        if (checked != R.id.speed1x) {
            speed1x.setChecked(false);
        }
        if (checked != R.id.speed15x) {
            speed15x.setChecked(false);
        } else {
            factor = 1.5f;
        }
        if (checked != R.id.speed2x) {
            speed2x.setChecked(false);
        } else {
            factor = 2.0f;
        }
        this.mModel.speed = factor;
    }

    /* mo4627x4a0f4a7c renamed from: lambda$onCreate$8$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void getFilter(View view) {
        if (this.binding.camera.isTakingVideo()) {
            Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT).show();
        } else if (this.binding.filters.getVisibility() == View.VISIBLE) {
            this.binding.filters.setAdapter((RecyclerView.Adapter) null);
            this.binding.filters.setVisibility(View.GONE);
        } else {
            if (this.binding.filters.getVisibility() == View.GONE) {
                this.binding.filters.setVisibility(View.VISIBLE);
            }
            this.mProgress = KProgressHUD.create(this).setStyle(KProgressHUD.Style.SPIN_INDETERMINATE).setLabel(getString(R.string.progress_title)).setCancellable(false).show();
            this.binding.camera.takePictureSnapshot();
        }
    }

    static /* synthetic */ void lambda$onCreate$10(TurboImageView stickers, View remove, View v) {
        stickers.removeSelectedObject();
        if (stickers.getObjectCount() <= 0) {
            remove.setVisibility(View.GONE);
        }
    }

    /* renamed from: lambda$onCreate$11$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void stickerOnClick(View v) {
        startActivityForResult(new Intent(this, StickerPickerActivity.class), Const.REQUEST_CODE_PICK_STICKER);
    }

    /* renamed from: lambda$onCreate$13$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void onBtnDoneClick(BottomSheetBehavior bsb, View view) {
        bsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
        startTimer();
    }

    /* renamed from: lambda$onCreate$14$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void onTimerBsb(BottomSheetBehavior bsb, View view) {
        if (this.binding.camera.isTakingVideo()) {
            Toast.makeText(this, R.string.recorder_error_in_progress, Toast.LENGTH_SHORT).show();
        } else {
            bsb.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    /* renamed from: lambda$onCreate$15$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void mo4618x3e883540(View view) {
        if (this.mModel.segments.isEmpty()) {
            startActivityForResult(new Intent(this, SongPickerActivity.class), 60605);
        } else if (this.mModel.audio == null) {
            Toast.makeText(this, R.string.message_song_select, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.message_song_change, Toast.LENGTH_SHORT).show();
        }
    }

    /* renamed from: lambda$onCreate$17$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void mo4619xc59e70c2(View view) {
        String[] permissions = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};
        if (EasyPermissions.hasPermissions(this, permissions)) {
            chooseVideoForUpload();
        } else {
            EasyPermissions.requestPermissions((Activity) this, getString(R.string.permission_rationale_upload), 200, permissions);
        }
    }

    static /* synthetic */ void lambda$onCreate$18(long l) {
    }

    /* mo4620x4cb4ac44 renamed from: lambda$onCreate$19$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void recordVideo(SegmentedGroup speeds, View view) {
        this.binding.filters.setVisibility(View.GONE);
        if (this.binding.camera.isTakingVideo()) {
            stopRecording();
            return;
        }
        this.binding.filters.setVisibility(View.GONE);
        speeds.setVisibility(View.GONE);
        startRecording();
    }

    private void commitRecordings(List<RecordSegment> segments, Uri audio) {
        this.timeInSeconds = 0;
        this.mProgress = KProgressHUD.create(this).setStyle(KProgressHUD.Style.SPIN_INDETERMINATE).setLabel(getString(R.string.progress_title)).setCancellable(false).show();
        List<String> videos = new ArrayList<>();
        for (RecordSegment segment : segments) {
            videos.add(segment.file);
        }
        this.merged = TempUtil.createNewFile((Context) this, ".mp4");
        Log.d(TAG, "commitRecordings: first merged" + this.merged.getAbsolutePath());
        OneTimeWorkRequest request = (OneTimeWorkRequest) ((OneTimeWorkRequest.Builder) new OneTimeWorkRequest.Builder(MergeVideosWorker2.class).setInputData(new Data.Builder().putStringArray(MergeVideosWorker2.KEY_INPUTS, (String[]) videos.toArray(new String[0])).putString("output", this.merged.getAbsolutePath()).build())).build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue((WorkRequest) request);
      //  wm.getWorkInfoByIdLiveData(request.getId()).observe(this, new RecorderActivity$$ExternalSyntheticLambda10(this, audio));
        wm.getWorkInfoByIdLiveData(request.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                mo4611x773b251c(audio,workInfo);
            }
        });
        Log.d(TAG, "commitRecordings: ");
    }

    /* renamed from: lambda$commitRecordings$20$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void mo4611x773b251c(Uri audio, WorkInfo info2) {
        if (info2.getState() == WorkInfo.State.CANCELLED || info2.getState() == WorkInfo.State.FAILED || info2.getState() == WorkInfo.State.SUCCEEDED) {
            Log.d(TAG, "commitRecordings: ended");
            this.mProgress.dismiss();
        }
        if (info2.getState() == WorkInfo.State.SUCCEEDED) {
            Log.d(TAG, "commitRecordings: success");
            if (audio != null) {
                proceedToVolume(this.merged, new File(audio.getPath()));
            } else {
                proceedToFilter(this.merged);
            }
        }
    }

    public String savePhoto(Bitmap bmp) {
        File imageFileName = new File(getFilesDir(), "tempOverlay.png");
        try {
            FileOutputStream out = new FileOutputStream(imageFileName);
            try {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
                out.flush();
                out.close();
                return imageFileName.toString();
            } catch (Exception e2) {
                FileOutputStream fileOutputStream = out;
                e2.printStackTrace();
                return "";
            }
        } catch (Exception e3) {
            e3.printStackTrace();
            return "";
        }
    }

    private File initVideoFileTemp() {
        File dir = getDirPathWithFolder(this);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.exists()) {
            dir = getCacheDir();
        }
        return new File(dir, "tempVideo.mp4");
    }

    private File initVideoFile() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), getString(R.string.app_name));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        if (!dir.exists()) {
            dir = getCacheDir();
        }
        return new File(dir, String.format("buzzy.mp4", new Object[]{Long.valueOf(System.currentTimeMillis())}));
    }

    public void onBackPressed() {
        confirmClose();
    }

    private void confirmClose() {
      //  new PopupBuilder(this).showReliteDiscardPopup("Discard Entire video ?", "If you go back now, you will lose all the clips added to your video", "Discard Video", "Cancel", new RecorderActivity$$ExternalSyntheticLambda15(this));
        new PopupBuilder(this).showReliteDiscardPopup("Discard Entire video ?", "If you go back now, you will lose all the clips added to your video",
                "Discard Video", "Cancel",
                new PopupBuilder.OnPopupClickListner() {
                    @Override
                    public void onClickCountinue() {
                        mo4612xafa347de();
                    }
                });
    }

    /* renamed from: lambda$confirmClose$21$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void mo4612xafa347de() {
        finish();
    }

    private void downloadSticker(Sticker stickerDummy) {
        File stickers = new File(getFilesDir(), "stickers");
        if (!stickers.exists() && !stickers.mkdirs()) {
            Log.w(TAG, "Could not create directory at " + stickers);
        }
        String extension = stickerDummy.getImage().substring(stickerDummy.getImage().lastIndexOf(".") + 1);
        File image = new File(stickers, stickerDummy.getId() + extension);
        if (image.exists()) {
            addSticker(image);
            return;
        }
        KProgressHUD progress = KProgressHUD.create(this).setStyle(KProgressHUD.Style.SPIN_INDETERMINATE).setLabel(getString(R.string.progress_title)).setCancellable(false).show();
        WorkRequest request = ((OneTimeWorkRequest.Builder) new OneTimeWorkRequest.Builder(FileDownloadWorker.class).setInputData(new Data.Builder().putString("input", stickerDummy.getImage()).putString("output", image.getAbsolutePath()).build())).build();
        WorkManager wm = WorkManager.getInstance(this);
        wm.enqueue(request);
        wm.getWorkInfoByIdLiveData(request.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                getWork( progress, image,workInfo);
            }
        });
    }

    /* mo4613x136ab59c renamed from: lambda$downloadSticker$22$com-example-shortvideod-activity-RecorderActivity */
    public /* synthetic */ void getWork(KProgressHUD progress, File image, WorkInfo info2) {
        if (info2.getState() == WorkInfo.State.CANCELLED || info2.getState() == WorkInfo.State.FAILED || info2.getState() == WorkInfo.State.SUCCEEDED) {
            progress.dismiss();
        }
        if (info2.getState() == WorkInfo.State.SUCCEEDED) {
            addSticker(image);
        }
    }

    private void proceedToFilter(File video) {
        Log.d(TAG, "Proceeding to filter screen with " + video);
        Intent intent = new Intent(this, FilterActivity.class);
        intent.putExtra("song", this.mModel.song);
        intent.putExtra("video", video.getAbsolutePath());
        Log.d(TAG, "proceedToFilter: " + video.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    private void proceedToVolume(File video, File audio) {
        Log.v(TAG, "Proceeding to volume screen with " + video + "; " + audio);
        Intent intent = new Intent(this, VolumeActivity.class);
        intent.putExtra("song", this.mModel.song);
        intent.putExtra("video", video.getAbsolutePath());
        intent.putExtra("audio", audio.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    /* access modifiers changed from: private */
    public void processCurrentRecording() {
        if (this.mModel.video != null) {
            long duration = VideoUtil.getDuration(this, Uri.fromFile(this.mModel.video));
            if (this.mModel.speed != 1.0f) {
                applyVideoSpeed(this.mModel.video, this.mModel.speed, duration);
            } else {
                RecordSegment segment = new RecordSegment();
                segment.file = this.mModel.video.getAbsolutePath();
                segment.duration = duration;
                this.mModel.segments.add(segment);
            }
        }
        this.mModel.video = null;
    }

    private void setupSong(Song songDummy, Uri file) {
        Log.d(TAG, "setupSong:  file" + file.toString());
        MediaPlayer create = MediaPlayer.create(this, file);
        this.mMediaPlayer = create;
       // create.setOnCompletionListener(new RecorderActivity$$ExternalSyntheticLambda0(this));
        create.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                onCompletion(mediaPlayer);
            }
        });
        TextView sound = (TextView) findViewById(R.id.sound);
        if (songDummy != null) {
            sound.setText(songDummy.getTitle());
            this.mModel.song = songDummy.getId();
        } else {
            sound.setText(getString(R.string.audio_from_clip));
        }
        this.mModel.audio = file;
    }

    public Runnable onCompletion(MediaPlayer mp) {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer = null;
        }
        return null;
    }

    /* access modifiers changed from: private */
    public void startRecording() {
        Log.d(TAG, "startRecording: ");
        this.binding.filters.setVisibility(View.GONE);
        long recorded = this.mModel.recorded();
        if (recorded >= Const.MAX_DURATION) {
            Toast.makeText(this, R.string.recorder_error_maxed_out, Toast.LENGTH_SHORT).show();
            return;
        }
        this.mModel.video = TempUtil.createNewFile((Context) this, ".mp4");
        this.binding.camera.takeVideoSnapshot(this.mModel.video, (int) (Const.MAX_DURATION - recorded));
    }

    private void startTimer() {
        View countdown = findViewById(R.id.countdown);
        TextView count = (TextView) findViewById(R.id.count);
        count.setText((CharSequence) null);
        final TextView textView = count;
        final View view = countdown;
        final long value = (long) ((Slider) findViewById(R.id.selection)).getValue();
        CountDownTimer timer = new CountDownTimer(3000, 1000) {
            public void onTick(long remaining) {
              //  RecorderActivity.this.mHandler.post(new RecorderActivity$3$$ExternalSyntheticLambda1(textView, remaining));
            }

            public void onFinish() {
                RecorderActivity.this.mHandler.post( onCompletion(mMediaPlayer));
                RecorderActivity.this.startRecording();
                RecorderActivity.this.mHandler.postDelayed(RecorderActivity.this.mStopper, value);
            }
        };
        countdown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startTimer();
            }
        });
        countdown.setVisibility(View.VISIBLE);
        timer.start();
    }

    static /* synthetic */ void lambda$startTimer$24(CountDownTimer timer, View countdown, View v) {
        timer.cancel();
        countdown.setVisibility(View.GONE);
    }

    /* access modifiers changed from: private */
    public void stopRecording() {
        Log.d(TAG, "stopRecording: ");
        this.binding.camera.stopVideo();
    }

    private void submitUpload(Uri uri) {
        File copy = TempUtil.createCopy(this, uri, ".mp4");
        //Here ..

        Intent intent = new Intent(this, TrimmerActivity.class);
        if (this.mModel.audio != null) {
            intent.putExtra("audio", this.mModel.audio.getPath());
        }
        intent.putExtra("song", this.mModel.song);
        intent.putExtra("video", copy.getAbsolutePath());
        startActivity(intent);
        finish();
    }

    /* access modifiers changed from: private */
    public void toggleVisibility(boolean show) {
        if (getResources().getBoolean(R.bool.clutter_free_recording_enabled)) {
            AnimationUtil.toggleVisibilityToTop(findViewById(R.id.top), show);
            AnimationUtil.toggleVisibilityToLeft(findViewById(R.id.right), show);
            AnimationUtil.toggleVisibilityToBottom(findViewById(R.id.upload), show);
            AnimationUtil.toggleVisibilityToBottom(findViewById(R.id.done), show);
        }
    }

    private void addStickerView(File file) {
        final StickerView stickerView = new StickerView(this);
        stickerView.setBitmap(BitmapFactory.decodeFile(file.getAbsolutePath()));
        stickerView.setOperationListener(new StickerView.OperationListener() {
            public void onDeleteClick() {
                RecorderActivity.this.mViews.remove(stickerView);
                RecorderActivity.this.binding.viewsticker.removeView(stickerView);
            }

            public void onEdit(StickerView stickerView) {
                RecorderActivity.this.mCurrentView.setInEdit(false);
                RecorderActivity.this.mCurrentView = stickerView;
                RecorderActivity.this.mCurrentView.setInEdit(true);
            }

            public void onTop(StickerView stickerView) {
                int position = RecorderActivity.this.mViews.indexOf(stickerView);
                if (position != RecorderActivity.this.mViews.size() - 1) {
                    RecorderActivity.this.mViews.add(RecorderActivity.this.mViews.size(), (StickerView) RecorderActivity.this.mViews.remove(position));
                }
            }
        });
        this.binding.viewsticker.addView(stickerView, new RelativeLayout.LayoutParams(-1, -1));
        this.mViews.add(stickerView);
        setCurrentEdit(stickerView);
    }

    private void setCurrentEdit(StickerView stickerView) {
        StickerView stickerView2 = this.mCurrentView;
        if (stickerView2 != null) {
            stickerView2.setInEdit(false);
        }
        this.mCurrentView = stickerView;
        stickerView.setInEdit(true);
    }

    public static class RecorderActivityViewModel extends ViewModel {
        public Uri audio;
        public List<RecordSegment> segments = new ArrayList();
        public String song = "";
        public float speed = 1.0f;
        public File video;

        public long recorded() {
            long recorded = 0;
            for (RecordSegment segment : this.segments) {
                recorded += segment.duration;
            }
            return recorded;
        }
    }

    private static class RecordSegment {
        public long duration;
        public String file;

        private RecordSegment() {
        }
    }


    public final /* synthetic */ class RecorderActivity$$ExternalSyntheticLambda9 implements RadioGroup.OnCheckedChangeListener {
        public final /* synthetic */ RecorderActivity f$0;
        public final /* synthetic */ RadioButton f$1;
        public final /* synthetic */ RadioButton f$2;
        public final /* synthetic */ RadioButton f$3;
        public final /* synthetic */ RadioButton f$4;
        public final /* synthetic */ RadioButton f$5;

        public /* synthetic */ RecorderActivity$$ExternalSyntheticLambda9(RecorderActivity recorderActivity, RadioButton radioButton, RadioButton radioButton2, RadioButton radioButton3, RadioButton radioButton4, RadioButton radioButton5) {
            this.f$0 = recorderActivity;
            this.f$1 = radioButton;
            this.f$2 = radioButton2;
            this.f$3 = radioButton3;
            this.f$4 = radioButton4;
            this.f$5 = radioButton5;
        }

        public final void onCheckedChanged(RadioGroup radioGroup, int i) {
            OnCheckChanged(this.f$1, this.f$2, this.f$3, this.f$4, this.f$5, radioGroup, i);
        }
    }


}
