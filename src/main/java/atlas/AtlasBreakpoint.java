package atlas;

import java.util.ArrayList;

public class AtlasBreakpoint extends AtlasNode {

    public AtlasBreakpoint(String name, ArrayList<AtlasNode> childList) {
        super(name, childList, 3);
    }

    public AtlasBreakpoint(AtlasBreakpoint node) {
        this(node.getName(), node.getChildList());
    }

    @Override
    public String getSummary() {
        return "Breakpoint: " + getName() + "\nFrames: " + getChildCount();

    }
}
