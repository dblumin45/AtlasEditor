package atlas;

import javafx.scene.control.TreeItem;

import java.util.ArrayList;

public abstract class AtlasNode {
    private String name;
    private ArrayList<AtlasNode> childList;
    private Class[] childTypes;
    private int hierarchy;

    public AtlasNode(String name, ArrayList<AtlasNode> childList, int hierarchy) {
        this.name = name;
        this.hierarchy = hierarchy;
        if (childList == null) {
            childList = new ArrayList<>();
        }
        this.childList = childList;

        // Assign valid child types based on this Class
        Class c = this.getClass();
        if (c == AtlasAnimation.class) {
            childTypes = new Class[] {
                AtlasBreakpoint.class,
                AtlasImage.class
            };
        } else if (c == AtlasBreakpoint.class) {
            childTypes = new Class[] {
                    AtlasImage.class
            };
        } else if (c == AtlasGroup.class) {
            childTypes = new Class[] {
                    AtlasAnimation.class,
                    AtlasImage.class
            };
        }
    }

    public AtlasNode(String name, int hierarchy) {
        this.hierarchy = hierarchy;
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract String getSummary();

    public ArrayList<AtlasNode> getChildList() {
        return this.childList;
    }

    public Class[] getChildTypes() {
        return this.childTypes;
    }

    public int getChildCount() {
        if(this.childList == null) {
            return 0;
        } else {
            return this.childList.size();
        }
    }

    public boolean isValidChild(Class child) {
        for(int i = 0; i < this.childList.size(); i++) {
            if(child == this.childTypes[i]) {
                return true;
            }
        }
        return false;
    }

    public void addChild(AtlasNode child) {
        if (this.childList != null) {
            this.childList.add(child);
        }
    }

    public void addChild(AtlasNode child, int index) {
        if (this.childList != null) {
            this.childList.add(index, child);
        }
    }

    public void removeChild(AtlasNode child) {
        if (this.childList != null) {
            this.childList.remove(child);
        }
    }

    public void setChildList(ArrayList<AtlasNode> list) {
        this.childList = list;
    }

    public int getHierarchy() {
        return this.hierarchy;
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
