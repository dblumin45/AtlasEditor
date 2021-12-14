package atlas;


public class AtlasImage extends AtlasNode {
    private int frame;
    private String trimMode;
    public AtlasImage(String name, int frame, String trimMode) {
        super(name, 4);
        this.frame = frame;
        this.trimMode = trimMode;
    }

    public AtlasImage(AtlasImage node) {
        this(node.getName(), node.getFrame(), node.getTrimMode());
    }

    @Override
    public String getSummary() {
        return " Frame: " + this.frame + " Image Location: " + getName();
    }

    public int getFrame() {
        return this.frame;
    }

    public void setFrame(int frame) {
        this.frame = frame;
    }

    public String getTrimMode() {
        return this.trimMode;
    }

    public void setTrimMode(String trimMode) {
        this.trimMode = trimMode;
    }
}
