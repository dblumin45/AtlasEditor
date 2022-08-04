package atlas;

import java.util.ArrayList;

public class AtlasAnimation extends AtlasNode {

    private int fps;
    private boolean flipHorizontal;
    private boolean flipVertical;
    private String playback;
    public AtlasAnimation(String name, ArrayList<AtlasNode> childList, int fps, boolean flipHorizontal, boolean flipVertical, String playback) {
        super(name, childList, 2);
        this.fps = fps;
        this.flipHorizontal = flipHorizontal;
        this.flipVertical = flipVertical;
        this.playback = playback;
    }

    public AtlasAnimation(AtlasAnimation node) {
        this(node.getName(), node.getChildList(), node.getFps(), node.isFlipHorizontal(), node.isFlipVertical(), node.getPlayback());
    }

    public int getFps() {
        return this.fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    @Override
    public int getChildCount() {
        int count = 0;

        for(AtlasNode node : getChildList()) {
            Class c = node.getClass();
            if (c == AtlasImage.class) {
                count++;
            } else if (c == AtlasBreakpoint.class) {
                count += node.getChildCount();
            }
        }
        return count;
    }

    @Override
    public String getSummary() {
        return "Animation name: " + getName() + "\nPlayback rate: " + this.fps + "fps"
                + "\nFrames: " + getChildCount();
    }

    public String getPlayback() {
        return this.playback;
    }
    public boolean isFlipHorizontal() {
        return this.flipHorizontal;
    }
    public boolean isFlipVertical() {
        return this.flipVertical;
    }

    public void setPlayback(String playback) {
        this.playback = playback;
    }
    public void setFlipHorizontal(boolean flipHorizontal) {
        this.flipHorizontal = flipHorizontal;
    }
    public void setFlipVertical(boolean flipVertical) {
        this.flipVertical = flipVertical;
    }
}
