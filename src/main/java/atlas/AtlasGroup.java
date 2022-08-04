package atlas;

import java.util.ArrayList;

public class AtlasGroup extends AtlasNode {
    public AtlasGroup(String name, ArrayList<AtlasNode> childList) {
        super(name, childList, 1);
    }

    public AtlasGroup(AtlasGroup node) {
        this(node.getName(), node.getChildList());
    }

    @Override
    public String getSummary() {
        return "Group name: " + getName() + "\nAnimations and loose images: " + getChildCount();
    }
}
