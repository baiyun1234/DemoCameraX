package bai.bai.bai.democamerax;

import android.content.Intent;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

import androidx.camera.core.CameraX;
import androidx.camera.core.FlashMode;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class MainActivity extends AppCompatActivity {

    private String TAG = "baibai -> ";
    private TextureView mTextureView;
    private Preview mPreview;
    private ImageCapture mImageCapture;
    private ImageAnalysis mImageAnalysis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        //下面三个用例都是需要先定义一个配置，然后将其用于创建用例的实例

        //===============================================图像预览===================================================
        PreviewConfig config = new PreviewConfig.Builder()
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation())//设置此配置中图像的预期目标的旋期目标的旋转。三个都有
                .build();
        mPreview = new Preview(config);

        //===============================================图像拍摄===================================================
        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setLensFacing(CameraX.LensFacing.BACK)//根据镜头所面向的方向设置要配置的主相机。
                .setFlashMode(FlashMode.ON)//请求的闪光模式
                .setTargetAspectRatio(Rational.NEGATIVE_INFINITY)//设置此配置中图像的预期目标的纵横比
                .setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)//设置图像捕获模式: 要优化照片拍摄的延迟时间,设置为 MIN_LATENCY;要优化照片质量，请将其设置为 MAX_QUALITY。
                .build();
        mImageCapture = new ImageCapture(imageCaptureConfig);

        //================================================图像分析===================================================
        ImageAnalysisConfig analysisConfig = new ImageAnalysisConfig.Builder()
                //从该配置设置目标的分辨率。如果使用 setTargetResolution，可能会收到宽高比与其他用例不匹配的缓冲区。
                // 如果必须匹配宽高比，请检查两个用例返回的缓冲区的尺寸，然后剪裁或缩放其中一个以与另一个匹配
                .setTargetResolution(new Size(1280, 720))
                .build();
        mImageAnalysis = new ImageAnalysis(analysisConfig);

        //=========================================将用例绑定android生命周期===========================================
        // 用例使用以下代码绑定到Android生命周期。
        // 第一个参数：控制用例生命周期转换的lifecycleOwner。
        // 后面的参数：要绑定到生命周期的用例，可以是多个，用逗号隔开。
        //
        // 也可以自定义生命周期，新建一个实现 lifecycleOwner 接口的类，并实现其抽象方法
        //
        CameraX.bindToLifecycle(MainActivity.this, mPreview, mImageCapture, mImageAnalysis);

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_preview:
                preview();
                break;

            case R.id.btn_analysis:
                imageAnalysis();
                break;

            case R.id.btn_capture:
                imageCapture();
                break;
        }
    }

    private void initView() {
        mTextureView = findViewById(R.id.texture_view);//预览的view
    }

    /**
     * 图像预览
     */
    private void preview() {
        mPreview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    @Override
                    public void onUpdated(Preview.PreviewOutput previewOutput) {
                        mTextureView.setSurfaceTexture(previewOutput.getSurfaceTexture());
                        updateTransform();
                    }
                });
    }

    /**
     * 图片拍摄
     */
    private void imageCapture() {
        File files = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/baibaibai/");
        Log.d(TAG, "files = " + files.getAbsolutePath());
        if (!files.exists()) {
            Log.d(TAG, "文件夹不存在");
            files.mkdirs();
            return;
        }
        String str = System.currentTimeMillis() + "";
        File file = new File(files.getAbsoluteFile() + "/" + str + ".png");
        mImageCapture.takePicture(file, new ImageCapture.OnImageSavedListener() {
            @Override
            public void onImageSaved(File file) {
                Log.d(TAG, "onImageSaved -> file" + file.getAbsolutePath());
                //发广播刷新图库，不然只能在文件管理器找到，而图库不会自动更新
                MainActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + file.getAbsolutePath())));
            }

            @Override
            public void onError(ImageCapture.UseCaseError useCaseError, String message, Throwable cause) {
                Log.d(TAG, "onError -> useCaseError = " + useCaseError.name() + ", massage = " + message);
            }
        });
    }

    /**
     * 图像分析
     */
    private void imageAnalysis() {
        mImageAnalysis.setAnalyzer(new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(ImageProxy image, int rotationDegrees) {
//                Log.d(TAG, "analyze -> " + image.getImageInfo());
            }
        });
    }

    /**
     * 补偿设备方向的变化，以直立旋转显示我们的取景器
     */
    private void updateTransform() {
        Matrix matrix = new Matrix();
        Float centerX = mTextureView.getWidth() / 2f;
        Float centerY = mTextureView.getHeight() / 2f;

        int rotationDegrees = 0;
        switch (mTextureView.getDisplay().getRotation()) {
            case Surface.ROTATION_0:
                rotationDegrees = 0;
                break;
            case Surface.ROTATION_90:
                rotationDegrees = 270;
                break;
            case Surface.ROTATION_180:
                rotationDegrees = 180;
                break;
            case Surface.ROTATION_270:
                rotationDegrees = 90;
                break;
        }

        matrix.postRotate(rotationDegrees, centerX, centerY);
        mTextureView.setTransform(matrix);

    }


    /**
     * 待研究 自定义生命周期
     */
    class CustomLifecycle implements LifecycleOwner {

        private LifecycleRegistry mLifecycleRegistry;

        public CustomLifecycle() {
            mLifecycleRegistry = new LifecycleRegistry(this);
            mLifecycleRegistry.markState(Lifecycle.State.CREATED);
        }

        public void doOnResume() {
            Log.d(TAG, "doOnResume前");
            mLifecycleRegistry.markState(Lifecycle.State.RESUMED);
            Log.d(TAG, "doOnResume");
        }

        public void doOnStart() {
            mLifecycleRegistry.markState(Lifecycle.State.STARTED);
        }

        public void doOnReStart() {
            mLifecycleRegistry.markState(Lifecycle.State.RESUMED);
        }

        public void doOnDestory() {
            mLifecycleRegistry.markState(Lifecycle.State.DESTROYED);
        }

        @Override
        public Lifecycle getLifecycle() {
            return mLifecycleRegistry;
        }
    }


}
