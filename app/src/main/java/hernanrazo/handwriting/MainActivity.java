package hernanrazo.handwriting;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import hernanrazo.handwriting.models.Classifier;
import hernanrazo.handwriting.models.Classification;
import hernanrazo.handwriting.models.TensorFlowClassifier;
import hernanrazo.handwriting.views.DrawModel;
import hernanrazo.handwriting.views.DrawView;

public class MainActivity extends Activity implements View.OnClickListener, View.OnTouchListener {

    private static final int PIXEL_WIDTH = 28;

    private Button clear;
    private Button classify;
    private TextView resText;
    private List<Classifier> mClassifiers = new ArrayList<>();
    private DrawModel drawModel;
    private DrawView drawView;
    private PointF mTmpPoint = new PointF();
    private float mLastX;
    private float mLastY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawView = findViewById(R.id.drawView);
        drawModel = new DrawModel(PIXEL_WIDTH, PIXEL_WIDTH);
        drawView.setModel(drawModel);
        drawView.setOnTouchListener(this);

        clear = findViewById(R.id.clear);
        clear.setOnClickListener(this);

        classify = findViewById(R.id.classify);
        classify.setOnClickListener(this);

        resText = findViewById(R.id.textView);

        loadModel();
    }

    @Override
    protected void onResume() {
        drawView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        drawView.onPause();
        super.onPause();
    }
    //TODO: remove keras model and associated code????
    private void loadModel() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mClassifiers.add(TensorFlowClassifier.create(getAssets(), "TensorFlow",
                                    "opt_mnist_convnet-tf.pb", "labels.txt", PIXEL_WIDTH,
                                    "input", "output", true));

                    mClassifiers.add(TensorFlowClassifier.create(getAssets(), "Keras",
                                    "opt_mnist_convnet-keras.pb", "labels.txt", PIXEL_WIDTH,
                                    "conv2d_1_input", "dense_2/Softmax", false));
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing classifiers!", e);
                }
            }
        }).start();
    }

    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.clear) {

            drawModel.clear();
            drawView.reset();
            drawView.invalidate();
            resText.setText("");

        } else if (view.getId() == R.id.classify) {

            float pixels[] = drawView.getPixelData();

            String text = "";

            for (Classifier classifier : mClassifiers) {

                final Classification res = classifier.recognize(pixels);

                if (res.getLabel() == null) {
                    text += classifier.name() + ": ?\n";
                } else {

                    text += String.format("%s: %s, %f\n", classifier.name(), res.getLabel(), res.getConf());
                }
            }
            resText.setText(text);
        }
    }

    @Override

    public boolean onTouch(View v, MotionEvent event) {

        int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN) {

            processTouchDown(event);
            return true;

        } else if (action == MotionEvent.ACTION_MOVE) {
            processTouchMove(event);
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            processTouchUp();
            return true;
        }
        return false;
    }

    private void processTouchDown(MotionEvent event) {

        mLastX = event.getX();
        mLastY = event.getY();

        drawView.calcPos(mLastX, mLastY, mTmpPoint);

        float lastConvX = mTmpPoint.x;
        float lastConvY = mTmpPoint.y;

        drawModel.startLine(lastConvX, lastConvY);
    }

    private void processTouchMove(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        drawView.calcPos(x, y, mTmpPoint);

        float newConvX = mTmpPoint.x;
        float newConvY = mTmpPoint.y;

        drawModel.addLineElem(newConvX, newConvY);

        mLastX = x;
        mLastY = y;

        drawView.invalidate();
    }

    private void processTouchUp() {
        drawModel.endLine();
    }
}