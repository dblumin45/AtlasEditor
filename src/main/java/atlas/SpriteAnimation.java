package atlas;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.util.ArrayList;

public class SpriteAnimation extends Transition {

    private final ImageView imageView;
    private final Label frameLabel;
    private ArrayList<Image> imageList;
    private final int count;
    private int lastIndex;

    SpriteAnimation(ImageView imageView, ArrayList<Image> imageList, Duration duration, Label frameLabel) {
        this.imageView = imageView;
        this.imageList = imageList;
        this.count = imageList.size();
        this.frameLabel = frameLabel;
        setInterpolator(Interpolator.LINEAR);
        setCycleDuration(duration);
        setCycleCount(Animation.INDEFINITE);
    }

    @Override
    protected void interpolate(double v) {
        final int index = Math.min((int) Math.floor(v * count), count - 1);
        if (index != lastIndex) {
            imageView.setImage(imageList.get(index));
            frameLabel.setText("Frame: " + (lastIndex + 1));
            lastIndex = index;
        }
    }
}
