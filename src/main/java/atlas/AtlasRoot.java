package atlas;

public class AtlasRoot extends AtlasNode {
    private int margin;
    private int extrudeBorders;
    private int innerPadding;
    public AtlasRoot(String name, int margin, int extrudeBorders, int innerPadding) {
        super(name, 0);
        this.margin = margin;
        this.extrudeBorders = extrudeBorders;
        this.innerPadding = innerPadding;
    }

    public AtlasRoot(AtlasRoot node) {
        this(node.getName(), node.getMargin(), node.getExtrudeBorders(), node.getInnerPadding());
    }

    @Override
    public String getSummary() {
        return getName();
    }

    public int getMargin() {
        return this.margin;
    }
    public int getExtrudeBorders() {
        return this.extrudeBorders;
    }
    public int getInnerPadding() {
        return this.innerPadding;
    }

    public void setMargin(int margin) {
        this.margin = margin;
    }
    public void setInnerPadding(int innerPadding) {
        this.innerPadding = innerPadding;
    }
    public void setExtrudeBorders(int extrudeBorders) {
        this.extrudeBorders = extrudeBorders;
    }
}
