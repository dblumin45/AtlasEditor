package atlas;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.*;
import java.net.URL;
import java.util.*;

public class Controller implements Initializable{
    @FXML
    Label rootLabel;
    @FXML
    Button rootButton;
    @FXML
    TextField rootField;
    @FXML
    MenuItem deleteButton;
    @FXML
    MenuItem undoButton;
    @FXML
    MenuItem redoButton;
    @FXML
    MenuItem closeButton;
    @FXML
    MenuItem openButton;
    @FXML
    MenuItem newButton;
    @FXML
    MenuItem saveButton;
    @FXML
    MenuItem saveAsButton;
    @FXML
    TreeView<AtlasNode> treeView;
    @FXML
    ImageView imageView;
    @FXML
    VBox propertiesBox;
    @FXML
    Label frameLabel;
    private File root;
    private AtlasNode hoverNode;
    private ArrayList<TreeItem<AtlasNode>> draggedList;
    private ArrayList<TreeItem<AtlasNode>> clipboard;
    private TreeItem<AtlasNode> rootItem;
    private String filePath;
    private SpriteAnimation animation;
    private boolean isPlaying = false;
    private boolean isDragging = false;
    private boolean hasUnsavedChanges = false;
    private final boolean showRoot = true;
    private int cursor = 0;
    private final int ANIMATION_HIERARCHY = 2;
    private ArrayList<String> backupList;
    private ArrayList<ArrayList<Boolean>> expandedList;
    private ArrayList<String> toWrite;
    private ArrayList <String> animationNames = new ArrayList<>();
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Try to read config file for root directory
        try(BufferedReader reader = new BufferedReader(new FileReader("config.txt"))) {
            toWrite = new ArrayList<>();
            for(String line; (line=reader.readLine()) != null;) {
                if(line.contains("root=")){
                    root = new File(getValue(line, "="));
                } else {
                    toWrite.add(line);
                }
            }
        } catch (IOException ioe) {

        }

        // if root not set in config file, open directory chooser to pick root
        if(root == null) {
            changeRoot();
        }
        rootField.setText(root.getAbsolutePath());

        Main.stage.setOnCloseRequest(event -> {
            if(!exit()) {
                event.consume();
            }
        });

        // listener for updating root field
        rootField.setOnKeyPressed(event -> {
            if(event.getCode() == KeyCode.ENTER) {
                String tempUrl = rootField.getText();
                File exists = new File(tempUrl);
                if(exists.exists()) {
                    root = new File(tempUrl);
                    writeConfig();
                } else {
                    rootField.setText(root.getAbsolutePath());
                }
            }
        });

        // undo and redo options disabled until changes are made
        undoButton.setDisable(true);
        redoButton.setDisable(true);
    }

    private void loadAtlas(String newPath) {
        File atlas;
        animationNames = new ArrayList<>();
        // if passed in a path, try to load atlas from that
        if(newPath != null) {
            atlas = new File(newPath);
        } else { // otherwise, open filechooser with .atlas extension filter, starting in root directory
            FileChooser atlasChooser = new FileChooser();
            atlasChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("ATLAS (*.atlas)", "*.atlas"));
            atlasChooser.setInitialDirectory(root);
            atlas = atlasChooser.showOpenDialog(null);
            if(atlas != null) {
                filePath = atlas.getAbsolutePath();
            }
        }

        // atlas file exists
        if(atlas != null) {
            // set title based on loaded file, with symbol indicating unsaved changes
            if(hasUnsavedChanges) {
                Main.setTitle(filePath + " *");
            } else {
                Main.setTitle(filePath);
            }
            // read in atlas file
            try(BufferedReader reader = new BufferedReader(new FileReader(atlas))) {
                ArrayList<AtlasNode> groupList = new ArrayList<>();
                ArrayList<AtlasNode> animationList = new ArrayList<>();
                ArrayList<AtlasNode> imageList = new ArrayList<>();
                boolean inAnimation = false, inImage = false, inGroup = false, inFrame = false, inBreakpoint = false, flipHorizontal = false;
                String groupName = "", breakName = "", id = "", playback = "", imageID = "";

                int imageCount = 0, margin = 0, extrudeBorders = 0, innerPadding = 0, fps = 0;

                // parse atlas, constructing list of nodes
                for(String line; (line = reader.readLine()) != null;) {
                    // remove comments, leave '#' delimeted keywords
                    line = line.trim();
                    if (line.length() > 0 && line.charAt(0) != '#') {
                        line = line.replaceAll("#.*", "").trim();
                    }

                    if (line.length() > 0) {
                        // group keyword; start of animation group
                        if (line.contains("#group:")) {
                            // create new group for any animations that come within this group
                            inGroup = true;
                            groupName = getValue(line);
                            groupList.add(new AtlasGroup(groupName, new ArrayList<>()));
                            // sub keyword; start of animation breakpoint
                        } else if (line.contains("#sub:")) {
                            // add breakpoint to image list; will be used to group subsequent images
                            inBreakpoint = true;
                            breakName = getValue(line);
                            imageList.add(new AtlasBreakpoint(breakName, new ArrayList<>()));
                            // frame keyword; should probably factor out
                        } else if (line.contains("#frame:")) {
                            inFrame = true;
                        }
                        else if (line.contains("#end")) {
                            if (inFrame) {
                                inFrame = false;
                            } else if (inBreakpoint) {
                                inBreakpoint = false;
                            } else if (inGroup) {
                                inGroup = false;
                            }
                        } else if(line.contains("margin:")) {
                            margin = Integer.parseInt(getValue(line).trim());
                        } else if(line.contains("extrude_borders:")) {
                            extrudeBorders = Integer.parseInt(getValue(line).trim());
                        } else if(line.contains("inner_padding:")) {
                            innerPadding = Integer.parseInt(getValue(line).trim());
                        } else if (line.contains("animations {") ) { // start new animation, reset default params
                            inAnimation = true;
                            flipHorizontal = false;
                            playback = "";
                            fps = 0;
                            imageCount = 0;
                        } else if (inAnimation) {
                            if (line.contains("id:")) {
                                id = getValue(line).replace('"', ' ').trim();
                            } else if(line.contains("playback:")) {
                                playback = getValue(line).trim();
                            } else if(line.contains("flip_horizontal:")) {
                                flipHorizontal = Integer.parseInt(getValue(line).trim()) == 1;
                            } else if(line.contains("flip_vertical:")) { // flip_vertical is last param of animation,
                                // create new animation with read params and all encapsulated images
                                AtlasNode animation = new AtlasAnimation(
                                        id, imageList, fps, flipHorizontal, Integer.parseInt(getValue(line).trim()) == 1, playback
                                );

                                animationNames.add(id);
                                // reset imageList
                                imageList = new ArrayList<>();

                                // add animation to encapsulating group (if there is one)
                                if (inGroup) {
                                    AtlasGroup group = (AtlasGroup) groupList.get(groupList.size() - 1);
                                    group.addChild(animation);
                                } else { // otherwise, add animation to raw animation list
                                    animationList.add(animation);
                                }
                            } else if(line.contains("}")) { // close animation or image, based on current layer
                                if (inImage) {
                                    inImage = false;
                                } else {
                                    inAnimation = false;
                                }
                            } else if(line.contains("fps:")) {
                                fps = Integer.parseInt(getValue(line).replace('"', ' ').trim());

                            } else if(line.contains("images {")) { // start new image
                                inImage = true;

                            } else if(line.contains("image:")) {
                                imageID = getValue(line).replace('"', ' ').trim();

                            } else if(line.contains("sprite_trim_mode:")) { // last param of image,
                                // create new image
                                AtlasImage image = new AtlasImage(imageID, ++imageCount, getValue(line).trim());

                                // add image to current breakpoint (if present)
                                if(inBreakpoint) {
                                    AtlasBreakpoint breakPoint = (AtlasBreakpoint) imageList.get(imageList.size()-1);
                                    breakPoint.addChild(image);
                                } else { // or raw imagelist
                                    imageList.add(image);
                                }
                            }
                        }
                    }
                }

                // populate tree view with generated node hierarchy
                rootItem = new TreeItem<>(new AtlasRoot("Atlas", margin, extrudeBorders, innerPadding));
                treeView.setRoot(rootItem);
                addAllToTree(rootItem, animationList);
                addAllToTree(rootItem, imageList);

                for(AtlasNode group : groupList) {
                    recursiveAddToTree(rootItem, group);
                }

            } catch (IOException ioe) {
                System.err.println(ioe.getMessage());
            }
        }
    }

    private void buildTree() {
        // need to access cells for TreeItem events
        treeView.setCellFactory(tv -> {
            TreeCell<AtlasNode> cell = new TreeCell<>() {
                @Override
                protected void updateItem(AtlasNode node, boolean b) {
                    super.updateItem(node, b);
                    if(node == null) {
                        setText("");
                    } else {
                        setText(node.toString());
                    }
                }
            };

            // mouse will automatically be moved by a pixel when it is released after dragging
            cell.setOnMouseMoved(event -> {
                hoverNode = cell.getItem();
                TreeItem<AtlasNode> hoverItem = cell.getTreeItem();
                if(draggedList != null) {
                    if(hoverNode != null) {
                        flagChange();
                        // remove item from itemList and parent node's childList
                        removeItems(draggedList);
                        addItems(draggedList, hoverItem);
                    }

                    draggedList = null;
                    updateTree();
                }
            });
            return cell;
        });
        // construct tree
        treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        treeView.getSelectionModel().selectedItemProperty().addListener( (observable, oldValue, newValue) -> {
            if(newValue != null && newValue != oldValue) {
                itemSelected(newValue);
            }
        });
        treeView.setShowRoot(showRoot);
        if(rootItem != null) {
            rootItem.setExpanded(true);

        }
    }

    private String getValue(String line, String delim) {
        return line.split(delim)[1].trim();
    }

    private String getValue(String line) {
        return getValue(line, ":");
    }

    private HBox newBox() {
        HBox box = new HBox();
        box.setSpacing(20);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    // called anytime a change to the underlying atlas structure is made
    private void flagChange() {
        hasUnsavedChanges = true;
        Main.setTitle(filePath + " *");
        backupFile();
    }

    // toggle tree expansion to show changes in underlying structure
    private void updateTree() {
        treeView.setShowRoot(!showRoot);
        treeView.setShowRoot(showRoot);

    }

    // return relative filepath from root
    private String removeRoot(File root, File file) {
        return file.getAbsolutePath().substring(root.getAbsolutePath().length());
    }

    // get all nested images from node
    private void recursiveGetImages(ArrayList<Image> imageList, AtlasNode node) {
        ArrayList<AtlasNode> childList = node.getChildlist();
        if(childList != null) {
            for(AtlasNode child : childList) {
                recursiveGetImages(imageList, child);
            }
        } else if (node.getClass() == AtlasAnimation.class) {
            imageList.add(new Image("file:///" + root.getAbsolutePath() + node.getName()));
        }
    }

    // disable menu item
    private void hideOption(MenuItem menuItem) {
        menuItem.setDisable(true);
    }

    // handle key events:
    private void keyHandler(TreeItem<AtlasNode> item) {
        AtlasNode node = item.getValue();
        // key pressed while TreeView has focus:
        treeView.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case SPACE: // toggle play of animation (if selected)
                    // toggle play status
                    isPlaying = !isPlaying;
                    // if now playing,
                    if (isPlaying) {
                        // Can only play AtlasAnimations and Breakpoints
                        isPlaying = false;
                        if(node.getClass() == AtlasAnimation.class || node.getClass() == AtlasBreakpoint.class) {
                            // get all image children
                            ArrayList<Image> imageList = new ArrayList<>();
                            recursiveGetImages(imageList, node);
                            int fps;

                            // don't need to play if animation only has one image
                            if(imageList.size() > 1) {
                                // inherit parent fps for breakpoints
                                if (node.getClass() == AtlasBreakpoint.class) {
                                    AtlasAnimation parent = (AtlasAnimation) item.getParent().getValue();
                                    fps = parent.getFps();
                                } else {
                                    fps = ((AtlasAnimation) node).getFps();
                                }

                                // how long it takes for a single loop of animation
                                Duration duration = Duration.seconds(imageList.size() / (double) fps);

                                // create and play animation
                                animation = new SpriteAnimation(imageView, imageList, duration, frameLabel);
                                animation.play();
                                isPlaying = true;
                            }
                        }
                    } else {
                        // stop animation if already playing
                        animation.stop();
                    }
                    break;
                case DELETE: // delete selected node(s) (and children)
                    flagChange();
                    removeItems(treeView.getSelectionModel().getSelectedItems());
                    break;
                case C: // copy selected nodes
                    if(event.isControlDown()) {
                        copyItems();
                    }
                    break;
                case X: // cut selected nodes
                    if(event.isControlDown()) {
                        flagChange();
                        cutItems();

                    }
                    break;
                case V: // paste copied nodes
                    if(event.isControlDown()) {
                        flagChange();
                        pasteItems(item);

                    }
                    break;
            }
        });
    }

    // mouse event handler
    private void mouseEvents() {
        // start dragging selection; store selection list
        treeView.setOnDragDetected(event -> {
            draggedList = new ArrayList<>();
            for(TreeItem i : treeView.getSelectionModel().getSelectedItems()) {
                TreeItem<AtlasNode> parent = i.getParent();

                if(parent == null || !treeView.getSelectionModel().getSelectedItems().contains(parent)) {
                    draggedList.add(i);
                }
            }
            isDragging = true;
        });

        // release mouse if dragging; force mouse movement of a pixel to trigger event on current node
        treeView.setOnMouseReleased(event -> {
            if(isDragging) {
                isDragging = false;
                Robot robot = new Robot();
                robot.mouseMove(event.getScreenX() + 1, event.getScreenY());
            }
        });
    }

    // When new tree item is selected:
    private void itemSelected(TreeItem<AtlasNode> item) {
        // Get AtlasNode associated with this item
        AtlasNode node = item.getValue();

        // update view, context menu, and event handlers
        keyHandler(item);
        initializeContextMenu(item);
        mouseEvents();
        updateProperties(node);
    }

    private void updateProperties(AtlasNode node) {
        // remove all properties of old selection, update properties of new selection
        ObservableList list = propertiesBox.getChildren();
        list.clear();

        AtlasNode image = node;
        while(image.getChildlist() != null && image.getChildlist().size() > 0) {
            image = image.getChildlist().get(0);
        }
        String url = root.getAbsolutePath() + image.getName();
        imageView.setImage(new Image("file:///" + url));

        // if selected node is an Image:
        if(node.getClass() == AtlasImage.class) {
            // expose frame field
            frameLabel.setText("Frame: " + ((AtlasImage) node).getFrame());

            // set image by filechooser or text field
            Button folderButton = new Button("Open");
            TextField urlField = new TextField(node.getName());
            urlField.setPrefWidth(300);

            // open button:
            folderButton.setOnAction((event) -> {
                // open filechooser, filtered to .png and .jpg
                FileChooser chooser = new FileChooser();
                chooser.setInitialDirectory(new File(root.getAbsolutePath() + node.getName()).getParentFile());
                chooser.getExtensionFilters().add( new FileChooser.ExtensionFilter("IMAGES (*.png, *.jpg)", "*.png", "*.jpg") );
                File file = chooser.showOpenDialog(null);

                // if file was picked,
                if (file != null) {
                    // update selected node and update view
                    flagChange();
                    String newUrl = removeRoot(root, file);
                    node.setName(newUrl.replace("\\", "/"));
                    imageView.setImage(new Image("file:///" + file.getAbsolutePath()));
                    urlField.setText(node.getName());
                    updateTree();
                }
            });

            // image path text field:
            urlField.setOnKeyPressed(event -> {
                    // on enter,
                    if(event.getCode() == KeyCode.ENTER) {
                        String tempUrl = root.getAbsolutePath() + urlField.getText();
                        File exists = new File(tempUrl);
                        // if url field resolves to a valid image file,
                        if(exists.exists()) {
                            // update node and view
                            flagChange();
                            node.setName(urlField.getText().replace("\\", "/"));
                            imageView.setImage(new Image("file:///" + tempUrl));
                            updateTree();

                        } else { // otherwise, reset url field
                            urlField.setText(node.getName());
                            Alert urlAlert = new Alert(Alert.AlertType.ERROR);
                            urlAlert.setContentText("No file with url: " + tempUrl);
                            urlAlert.setHeaderText(null);
                            urlAlert.setTitle(null);
                            urlAlert.showAndWait();
                        }
                    }
                });
            // organize fields
            HBox box = newBox();
            box.getChildren().addAll(
                    new Label("Url:"),
                    urlField,
                    folderButton
            );
            list.add(box);
            box = newBox();
            ComboBox<String> trimModeBox = new ComboBox<>();
            trimModeBox.setValue(((AtlasImage)node).getTrimMode());
            trimModeBox.getItems().addAll(
                    "SPRITE_TRIM_MODE_OFF",
                    "SPRITE_TRIM_MODE_4",
                    "SPRITE_TRIM_MODE_5",
                    "SPRITE_TRIM_MODE_6",
                    "SPRITE_TRIM_MODE_7",
                    "SPRITE_TRIM_MODE_8"
            );
            trimModeBox.valueProperty().addListener(
                    (ObservableValue<? extends String> ov, String old_val, String new_val) -> {
                        flagChange();
                        ((AtlasImage)node).setTrimMode(new_val);

                    });
            box.getChildren().addAll(
                    new Label("Sprite Trim Mode:"),
                    trimModeBox
            );
            list.add(box);
        } else {
            if(!isPlaying) {
                frameLabel.setText("");
            }
            HBox box = newBox();
            if(node.getClass() != AtlasRoot.class) {
                // everything but AtlasRoot has a name field (Image's name field is dependent on path)
                TextField nameField = new TextField(node.getName());

                // name field can be updated freely
                nameField.setOnKeyPressed(event -> {
                    if(event.getCode() == KeyCode.ENTER) {
                        String name = nameField.getText();
                        if (node.getClass() != AtlasAnimation.class) {
                            flagChange();
                            node.setName(name);
                        } else {
                            if (!animationNames.contains(name)) {
                                animationNames.remove(node.getName());
                                node.setName(name);
                                animationNames.add(name);
                                flagChange();
                            } else {
                                nameField.setText(node.getName());
                                Alert nameAlert = new Alert(Alert.AlertType.ERROR);
                                nameAlert.setContentText("Animation with name '" + name + "' already exists. Animation names must be unique!");
                                nameAlert.setHeaderText(null);
                                nameAlert.setTitle(null);
                                nameAlert.showAndWait();
                            }
                        }

                        updateTree();
                    }
                });
                box.getChildren().addAll(
                        new Label("Name:"),
                        nameField
                );
                list.add(box);
                box = newBox();
            }

            if (node.getClass() == AtlasAnimation.class) { // for animations:
                TextField fpsField = new TextField(((AtlasAnimation)node).getFps() + "");
                fpsField.setOnKeyPressed(event -> {
                    if(event.getCode() == KeyCode.ENTER) { // on updating fps field,
                        try {
                            // try to update fps field by parsing int
                            flagChange();
                            int fps = Integer.parseInt(fpsField.getText());
                            ((AtlasAnimation) node).setFps(fps);
                            updateTree();

                        } catch (NumberFormatException nfe) { // invalid characters,
                            // reset fps field
                            fpsField.setText(((AtlasAnimation) node).getFps()+"");
                        }

                    }
                });
                box.getChildren().addAll(
                        new Label("fps:"),
                        fpsField
                );
                list.add(box);
                box = newBox();

                // checkboxes for flip horizontal and vertical:
                CheckBox flipHorizontalBox = new CheckBox();
                flipHorizontalBox.setSelected(((AtlasAnimation)node).isFlipHorizontal());
                flipHorizontalBox.selectedProperty().addListener(
                        (ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
                            flagChange();
                            ((AtlasAnimation)node).setFlipHorizontal(new_val);

                        });
                box.getChildren().addAll(
                        new Label("Flip Horizontal: "),
                        flipHorizontalBox
                );
                list.add(box);
                box = newBox();
                CheckBox flipVerticalBox = new CheckBox();
                box.getChildren().addAll(
                        new Label("Flip Vertical: "),
                        flipVerticalBox
                );
                flipVerticalBox.setSelected(((AtlasAnimation)node).isFlipVertical());
                flipVerticalBox.selectedProperty().addListener(
                        (ObservableValue<? extends Boolean> ov, Boolean old_val, Boolean new_val) -> {
                            flagChange();
                            ((AtlasAnimation)node).setFlipVertical(new_val);

                        });
                list.add(box);
                box = newBox();

                // combo box for playback mode
                ComboBox<String> playbackBox = new ComboBox<>();
                playbackBox.setValue(((AtlasAnimation)node).getPlayback());
                playbackBox.getItems().addAll(
                        "PLAYBACK_NONE",
                        "PLAYBACK_ONCE_FORWARD",
                        "PLAYBACK_ONCE_BACKWARD",
                        "PLAYBACK_ONCE_PINGPONG",
                        "PLAYBACK_LOOP_FORWARD",
                        "PLAYBACK_LOOP_BACKWARD",
                        "PLAYBACK_LOOP_PINGPONG"
                );
                playbackBox.valueProperty().addListener(
                        (ObservableValue<? extends String> ov, String old_val, String new_val) -> {
                            flagChange();
                            ((AtlasAnimation)node).setPlayback(new_val);

                        });
                box.getChildren().addAll(
                        new Label("Playback Mode:"),
                        playbackBox
                );
            } else if(node.getClass() == AtlasRoot.class) { // Atlas Root:
                // margin field should be integer
                TextField marginField = new TextField(((AtlasRoot)node).getMargin() + "");
                marginField.setOnKeyPressed(event -> {
                    if(event.getCode() == KeyCode.ENTER) {
                        try {
                            int margin = Integer.parseInt(marginField.getText());
                            flagChange();
                            ((AtlasRoot) node).setMargin(margin);
                            updateTree();

                        } catch (NumberFormatException nfe) {
                            marginField.setText(((AtlasRoot) node).getMargin()+"");
                        }

                    }
                });
                box.getChildren().addAll(
                        new Label("Margin:"),
                        marginField
                );
                if(!list.contains(box)) {
                    list.add(box);
                }

                // extrude borders field should be integer
                box = newBox();
                TextField extrudeBordersField = new TextField(((AtlasRoot)node).getExtrudeBorders() + "");
                extrudeBordersField.setOnKeyPressed(event -> {
                    if(event.getCode() == KeyCode.ENTER) {
                        try {
                            int extrudeBorders = Integer.parseInt(extrudeBordersField.getText());
                            flagChange();
                            ((AtlasRoot) node).setExtrudeBorders(extrudeBorders);
                            updateTree();

                        } catch (NumberFormatException nfe) {
                            extrudeBordersField.setText(((AtlasRoot) node).getExtrudeBorders()+"");
                        }
                    }
                });
                box.getChildren().addAll(
                        new Label("Extrude Borders:"),
                        extrudeBordersField
                );
                list.add(box);
                box = newBox();

                // inner padding field should be int
                TextField innerPaddingField = new TextField(((AtlasRoot)node).getInnerPadding() + "");
                innerPaddingField.setOnKeyPressed(event -> {
                    if(event.getCode() == KeyCode.ENTER) {
                        try {
                            int innerPadding = Integer.parseInt(innerPaddingField.getText());
                            flagChange();
                            ((AtlasRoot) node).setInnerPadding(innerPadding);
                            updateTree();

                        } catch (NumberFormatException nfe) {
                            innerPaddingField.setText(((AtlasRoot) node).getInnerPadding()+"");
                        }

                    }
                });
                box.getChildren().addAll(
                        new Label("Inner Padding:"),
                        innerPaddingField
                );

            }
            list.add(box);
        }
    }

    // check for clashes in animation name
    private boolean isUniqueAnimation(String name, TreeItem<AtlasNode> item) {
        /*
        boolean unique = true;
        if(item.getValue().getHierarchy() == 2 && item.getValue().getName().equals(name)) {
            unique = false;
        } else if(item.getValue().getHierarchy() < 2) {
            for (TreeItem<AtlasNode> child : item.getChildren()) {
                unique = isUniqueAnimation(name, child);
                if(!unique) {
                    break;
                }
            }
        }
        return unique;
        */
         return !animationNames.contains(name);
    }

    // set up context menu based on selected node type
    private void initializeContextMenu(TreeItem<AtlasNode> item) {
        AtlasNode node = item.getValue();
        // right click menu
        ContextMenu contextMenu = new ContextMenu();
        MenuItem addImages = new MenuItem("Add Images");
        MenuItem encapsulateBreakpoint = new MenuItem("Encapsulate In Breakpoint");
        boolean valid = true;
        TreeItem<AtlasNode> tempParent = null;
        for(TreeItem<AtlasNode> selectedItem : treeView.getSelectionModel().getSelectedItems()) {
            if(tempParent == null) {
                tempParent = selectedItem.getParent();
            }
            if(tempParent == null || tempParent.getValue().getClass() != AtlasAnimation.class || selectedItem.getParent() != tempParent) {
                valid = false;
                break;
            }
        }
        if(!valid) {
            hideOption(encapsulateBreakpoint);
        }
        MenuItem createGroup = new MenuItem("Create Group");
        if(node.getHierarchy() > 1) {
            hideOption(createGroup);
        }
        MenuItem addAnimation = new MenuItem("Add Animation");
        if(node.getHierarchy() > 2) {
            hideOption(addAnimation);
        }
        MenuItem createBreakpoint = new MenuItem("Create Breakpoint");
        if(node.getHierarchy() < 2) {
            hideOption(createBreakpoint);
        }
        MenuItem removeItem = new MenuItem("Remove Item");
        MenuItem copyItem = new MenuItem("Copy");
        MenuItem cutItem = new MenuItem("Cut");
        MenuItem pasteItem = new MenuItem("Paste");
        if(clipboard == null) {
            hideOption(pasteItem);
        }
        contextMenu.getItems().addAll(
                addImages, createGroup, addAnimation, createBreakpoint,
                encapsulateBreakpoint, removeItem, copyItem, cutItem, pasteItem
        );

        // add selected images to current node
        addImages.setOnAction(event -> {
            // open file chooser filtered to images
            FileChooser chooser = new FileChooser();
            chooser.setInitialDirectory(root);
            chooser.getExtensionFilters().add( new FileChooser.ExtensionFilter("IMAGES (*.png, *.jpg)", "*.png", "*.jpg") );

            // allow selection of multiple files
            List<File> fileList = chooser.showOpenMultipleDialog(null);
            if(fileList != null && fileList.size() > 0) {

                // add all chosen files to the parent node (what was clicked on)
                TreeItem<AtlasNode> parentItem = item;
                flagChange();
                for(File file : fileList) {
                    AtlasImage image = new AtlasImage(file.getAbsolutePath().substring(root.getAbsolutePath().length()).replace("\\", "/"), 0, "SPRITE_TRIM_MODE_OFF");
                    AtlasNode newNode = node;
                    if(node.getClass() == AtlasImage.class) {
                        // insert into parent list, ahead of this element
                        parentItem = item.getParent();
                        newNode = parentItem.getValue();
                        int index = parentItem.getChildren().indexOf(item);
                        image.setFrame(index + 1);
                        // update frame count of all following frames
                        newNode.addChild(image, index);
                        parentItem.getChildren().add(index, new TreeItem<>(image));

                    } else {
                        // insert at end of this list
                        image.setFrame(newNode.getChildCount() + 1);
                        newNode.getChildlist().add(image);
                        parentItem.getChildren().add(new TreeItem<>(image));
                    }
                }
                int count = 1;
                for(AtlasNode newNode : parentItem.getValue().getChildlist()) {
                    try {
                        AtlasImage image = (AtlasImage) newNode;
                        image.setFrame(count++);
                    } catch (ClassCastException cce) {
                        // only need to update frames for images
                    }
                }
            }
            updateProperties(node);
        });

        // create new group
        createGroup.setOnAction(event -> {
            TextInputDialog textInputDialog = new TextInputDialog("Enter group name:");
            textInputDialog.setTitle("New group");
            textInputDialog.setHeaderText("Create new group?");
            Optional<String> result = textInputDialog.showAndWait();
            if(result.isPresent()) {
                flagChange();
                AtlasGroup newGroup = new AtlasGroup(result.get(), new ArrayList<>());
                rootItem.getValue().addChild(newGroup);
                rootItem.getChildren().add(new TreeItem<>(newGroup));
                updateTree();
            }
        });

        // create new animation
        addAnimation.setOnAction(event -> {
            TextInputDialog textInputDialog = new TextInputDialog("Enter animation name:");
            textInputDialog.setTitle("New animation");
            textInputDialog.setHeaderText("Create new animation?");
            Optional<String> result = null;
            do {
                if(result != null) {
                    textInputDialog.setContentText("Animation name '" + result.get() + "' is already taken. Animation names must be unique!");
                }
                result = textInputDialog.showAndWait();
            } while(result.isPresent() && !isUniqueAnimation(result.get(), rootItem));
            if(result.isPresent()) {
                // check for duplicate animation names
                String animationName = result.get();
                flagChange();
                AtlasAnimation newAnimation = new AtlasAnimation(animationName, new ArrayList<>(), 60, false, false, "PLAYBACK_ONCE_FORWARD");
                animationNames.add(animationName);
                TreeItem<AtlasNode> parent = item;
                while(parent.getValue().getHierarchy() > 1) {
                    parent = parent.getParent();
                }
                if(parent == null) {
                    parent = rootItem;
                }
                int index = parent.getChildren().indexOf(item);
                if(index == -1) {
                    index = parent.getChildren().size();
                }
                parent.getValue().addChild(newAnimation, index);
                parent.getChildren().add(index, new TreeItem<>(newAnimation));
                updateTree();
            }

        });

        // create new breakpoint
        createBreakpoint.setOnAction(event -> {
            TextInputDialog textInputDialog = new TextInputDialog("Enter breakpoint name:");
            textInputDialog.setTitle("New breakpoint");
            textInputDialog.setHeaderText("Create new breakpoint?");
            Optional<String> result = textInputDialog.showAndWait();
            if(result.isPresent()) {
                flagChange();
                AtlasBreakpoint newBreakpoint = new AtlasBreakpoint(result.get(), new ArrayList<>());
                TreeItem<AtlasNode> parent = item;
                while(parent.getValue().getClass() != AtlasAnimation.class) {
                    parent = parent.getParent();
                }
                int index = parent.getChildren().indexOf(item);
                if (index == -1) {
                    index = parent.getChildren().size();
                }
                parent.getValue().addChild(newBreakpoint, index);
                parent.getChildren().add(index, new TreeItem<>(newBreakpoint));
                updateTree();
            }
        });

        // encapsulate selected images in a new breakpoint
        encapsulateBreakpoint.setOnAction(event -> {
            TextInputDialog textInputDialog = new TextInputDialog("Enter breakpoint name:");
            textInputDialog.setTitle("New breakpoint");
            textInputDialog.setHeaderText("Create new breakpoint?");
            Optional<String> result = textInputDialog.showAndWait();
            if(result.isPresent()) {
                flagChange();
                ArrayList<TreeItem<AtlasNode>> selectedList = new ArrayList<>();
                for(TreeItem<AtlasNode> selectedItem : treeView.getSelectionModel().getSelectedItems()) {
                    selectedList.add(selectedItem);
                }
                TreeItem<AtlasNode> parent = selectedList.get(0).getParent();
                AtlasNode newBreakpoint = new AtlasBreakpoint(result.get(), new ArrayList<>());
                TreeItem<AtlasNode> breakpointItem = new TreeItem<>(newBreakpoint);
                for(TreeItem<AtlasNode> selectedItem : selectedList) {
                    breakpointItem.getChildren().add(new TreeItem<>(selectedItem.getValue()));
                    newBreakpoint.addChild(selectedItem.getValue());
                }
                int index = parent.getChildren().indexOf(selectedList.get(0));
                parent.getChildren().add(index, breakpointItem);
                parent.getValue().addChild(newBreakpoint, index);
                removeItems(selectedList);
                updateTree();
            }
        });
        // delete all selected items
        removeItem.setOnAction(event -> {
            flagChange();
            removeItems(treeView.getSelectionModel().getSelectedItems());

        });
        copyItem.setOnAction(event -> {
            copyItems();

        });
        cutItem.setOnAction(event -> {
            flagChange();
            cutItems();

        });
        pasteItem.setOnAction(event -> {
            flagChange();
            pasteItems(item);

        });
        treeView.setContextMenu(contextMenu);
    }

    private void cutItems() {
        copyItems(true);
        removeItems();
        updateTree();
    }

    private String validAnimationName(String name) {
        // animation name is taken
        if (animationNames.contains(name)) {
            // if not already a copy, append copy to end of the name

            // contains 'copy'
            int index = name.indexOf("copy");
            if (index > -1) {
                // increment number at the end of copy
                String baseName = name.substring(0, index + 4);
                int copyNumber = Integer.parseInt(name.substring(index + 4));
                name = validAnimationName(baseName + (copyNumber + 1) );

            } else { // first copy
                name = validAnimationName(name + "copy0");
            }
        }
        return name;
    }

    // create and return copy of node and its children
    private AtlasNode deepCopy(AtlasNode node) {
        ArrayList<AtlasNode> childList = new ArrayList<>();
        AtlasNode copy;
        ArrayList<AtlasNode> children = node.getChildlist();

        // if node has children, add copies of each to new childList
        if (children != null) {
            for(AtlasNode child : children) {
                childList.add(deepCopy(child));
            }
        }

        // copy node to matching type based on hierarchy
        switch (node.getHierarchy()) {
            case 0:
                copy = new AtlasRoot((AtlasRoot) node);
                break;

            case 1:
                copy = new AtlasGroup((AtlasGroup) node);
                break;

            case 2:
                copy = new AtlasAnimation((AtlasAnimation) node);
                // animation names must be unique
                copy.setName(validAnimationName(copy.getName()));
                break;

            case 3:
                copy = new AtlasBreakpoint((AtlasBreakpoint) node);
                break;

            default:
                copy = new AtlasImage((AtlasImage) node);
                break;
        }

        // assign new childList to copy
        if (copy.getChildlist() != null) {
            copy.setChildList(childList);
        }
        return copy;
    }

    private void copyItems() {
        copyItems(false);
    }
    // copy selected items, if they are not children of something already in the clipboard
    private void copyItems(boolean isCut) {
        clipboard = new ArrayList<>();
        for(TreeItem<AtlasNode> i : treeView.getSelectionModel().getSelectedItems()) {
            TreeItem<AtlasNode> parent = i.getParent();
            if(parent == null || !treeView.getSelectionModel().getSelectedItems().contains(parent)) {
                AtlasNode node = i.getValue();
                if (isCut) {
                    if (node.getClass() == AtlasAnimation.class) {
                        String name = node.getName();
                        if(animationNames.contains(name)) {
                            animationNames.remove(name);
                        }
                    }
                }

                clipboard.add(new TreeItem<>(deepCopy(node)));
            }
        }
        for(TreeItem<AtlasNode> item : clipboard) {
            if(item.getValue().getChildlist() != null) {
                for(AtlasNode node : item.getValue().getChildlist()) {
                    recursiveAddToTree(item, node);
                }
            }
        }

    }

    private void copyItems(ArrayList<TreeItem<AtlasNode>> items) {
        clipboard = new ArrayList<>();
        for(TreeItem<AtlasNode> i : items) {
            TreeItem<AtlasNode> parent = i.getParent();
            if(parent == null || !items.contains(parent)) {
                clipboard.add(new TreeItem<>(deepCopy(i.getValue())));
            }
        }
        for(TreeItem<AtlasNode> item : clipboard) {
            if(item.getValue().getChildlist() != null) {
                for(AtlasNode node : item.getValue().getChildlist()) {
                    recursiveAddToTree(item, node);
                }
            }
        }
    }

    private void pasteItems(TreeItem item) {
        if(clipboard != null && clipboard.size() > 0) {
            addItems(clipboard, item);
        }
        updateTree();
        copyItems( new ArrayList<>(clipboard) );
    }

    @FXML
    public void removeItems() {
        removeItems(treeView.getSelectionModel().getSelectedItems());
    }

    // remove all items from passed in list
    public void removeItems(ArrayList<TreeItem<AtlasNode>> itemList) {

        // remove any redundant references to children of a parent already in the list
        ArrayList<TreeItem<AtlasNode>> cleanedList = new ArrayList<>();
        for(TreeItem<AtlasNode> item : itemList) {
            TreeItem<AtlasNode> parent = item.getParent();
            if(parent == null || !itemList.contains(parent)) {
                cleanedList.add(item);
            }
        }
        itemList = cleanedList;

        // remove selected nodes
        ArrayList<TreeItem<AtlasNode>> changedList = new ArrayList<>();
        for (TreeItem<AtlasNode> listItem : itemList) {
            AtlasNode listNode = listItem.getValue();
            if (listNode.getClass() == AtlasAnimation.class) {
                animationNames.remove(listNode.getName());
            }
            TreeItem parentItem = listItem.getParent();
            if(parentItem != null) {
                AtlasNode parentNode = (AtlasNode) parentItem.getValue();
                parentNode.removeChild(listNode);
                parentItem.getChildren().remove(listItem);
                if(listNode.getClass() == AtlasImage.class && !changedList.contains(parentItem)) {
                    if(parentItem.getValue().getClass() == AtlasBreakpoint.class) {
                        changedList.add(parentItem.getParent());
                    } else {
                        changedList.add(parentItem);
                    }
                }
            }
        }
        updateFrames(changedList);
        updateTree();
    }


    private void addItems(ArrayList<TreeItem<AtlasNode>> clipboard, TreeItem<AtlasNode> hoverItem) {
        // add item to appropriate position in childList and itemList
        ArrayList<TreeItem<AtlasNode>> changedList = new ArrayList<>();
        AtlasNode node = hoverItem.getValue();
        for(TreeItem<AtlasNode> item : clipboard) {

            AtlasNode draggedNode = item.getValue();
            if (draggedNode.getClass() == AtlasAnimation.class) {
                String nodeName = draggedNode.getName();
                if (!animationNames.contains(nodeName)) {
                    animationNames.add(nodeName);
                }
            }

            // get initial index of item
            // same level; put dragged item into same list as hoveredNode
            TreeItem<AtlasNode> parent = hoverItem;
            if(draggedNode.getClass() == node.getClass()) {
                parent = hoverItem.getParent();
                // dragged is higher level than hovered; recursively back up through hovered parent until == level
            } else if(draggedNode.getHierarchy() < node.getHierarchy()) {
                while(parent != null && draggedNode.getHierarchy() <= parent.getValue().getHierarchy()) {
                    parent = parent.getParent();
                }
            }
            if(parent == null) {
                parent = rootItem;
            }
            int index = parent.getChildren().indexOf(hoverItem);
            if(index == -1) {
                index = parent.getChildren().size();
            }
            parent.getChildren().add(index, item);
            parent.getValue().addChild(draggedNode, index);
            if(draggedNode.getClass() == AtlasImage.class && !changedList.contains(parent)) {
                if(hoverNode.getClass() == AtlasBreakpoint.class) {
                    changedList.add(parent.getParent());
                } else {
                    changedList.add(parent);
                }
            }

        }
        updateFrames(changedList);
    }

    private void updateFrames(ArrayList<TreeItem<AtlasNode>> changedList) {
        // update frame numbers
        for(TreeItem<AtlasNode> parent : changedList) {
            if(parent.getValue().getClass() == AtlasBreakpoint.class) {
                continue;
            }
            int count = 1;
            for(TreeItem<AtlasNode> item : parent.getChildren()) {
                if(item.getValue().getClass() == AtlasImage.class) {
                    AtlasImage image = (AtlasImage) item.getValue();
                    image.setFrame(count++);
                } else if(item.getValue().getClass() == AtlasBreakpoint.class) {
                    for(AtlasNode childNode : item.getValue().getChildlist()) {
                        ((AtlasImage)childNode).setFrame(count++);
                    }
                }
            }
        }
    }

    // create a backup file of the atlas's current state, for use with undo/redo
    private void backupFile() {
        try {
            if(cursor < backupList.size()) {
                // remove all backups ahead of cursor (can't redo anymore)
                for(int i = cursor; i < backupList.size(); i++ ) {
                    File backup = new File(backupList.get(i));
                    if(backup != null && backup.exists()) {
                        backup.delete();
                    }
                    backupList.remove(i);
                }
            }

            File temp = File.createTempFile("backup", cursor++ + "");
            undoButton.setDisable(false);
            backupList.add(temp.getAbsolutePath());
            expandedList.add(toExpandedList(flattenTree()));
            saveAtlas(temp.getAbsolutePath(), false);
            temp.deleteOnExit();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void removeItems(ObservableList<TreeItem<AtlasNode>> list) {
        ArrayList<TreeItem<AtlasNode>> newList = new ArrayList<>(list);
        removeItems(newList);

    }

    private void recursiveAddToList(ArrayList<TreeItem<AtlasNode>> list, TreeItem<AtlasNode> item) {
        list.add(item);
        for(TreeItem<AtlasNode> child : item.getChildren()) {
            if(child.getChildren().size() > 0) {
                recursiveAddToList(list, child);
            }
        }
    }

    private ArrayList<TreeItem<AtlasNode>> flattenTree() {
        ArrayList<TreeItem<AtlasNode>> list = new ArrayList<>();
        recursiveAddToList(list, rootItem);
        return list;
    }

    // store state of which tree items are expanded
    private ArrayList<Boolean> toExpandedList(ArrayList<TreeItem<AtlasNode>> list) {
        ArrayList<Boolean> expandedList = new ArrayList<>();
        for(TreeItem<AtlasNode> item : list) {
            expandedList.add(item.isExpanded());
        }
        return expandedList;
    }

    // read state of which tree items are expanded
    private void expandFromList(ArrayList<Boolean> expandedList, ArrayList<TreeItem<AtlasNode>> list) {
        for(int i = 0; i < list.size(); i++) {
            list.get(i).setExpanded(expandedList.get(i));
        }
    }

    private void addAllToTree(TreeItem rootItem, ArrayList<AtlasNode> nodeList) {
        for(AtlasNode node : nodeList) {
            rootItem.getChildren().add(new TreeItem(node));
        }
    }

    private void recursiveAddToTree(TreeItem rootItem, AtlasNode node) {
        TreeItem<AtlasNode> item = new TreeItem<>(node);
        rootItem.getChildren().add(item);
        ArrayList<AtlasNode> childList = node.getChildlist();
        if(childList != null) {
            for(AtlasNode child : childList) {
                recursiveAddToTree(item, child);
            }
        }
    }

    // popup when user closes program with unsaved changes
    private boolean saveChangesPopup(String operation) {
        boolean shouldExit = true;

        // if there are unsaved changes,
        if(hasUnsavedChanges) {
            // confirmation popup
            ButtonType saveButton = new ButtonType("Save changes", ButtonBar.ButtonData.OK_DONE);
            ButtonType exitButton = new ButtonType("Discard changes", ButtonBar.ButtonData.NO);
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            Alert unsavedAlert = new Alert(Alert.AlertType.WARNING,
                    "There are unsaved changes. Would you like to save before " + operation + "?",
                    saveButton, exitButton, cancelButton
            );
            unsavedAlert.setTitle("Exit?");
            Optional<ButtonType> result = unsavedAlert.showAndWait();

            if(result.orElse(null) == saveButton) {
                shouldExit = saveAtlas(null, true);
            } else if(result.orElse(null) != exitButton) {
                shouldExit = false;
            }
        }
        return shouldExit;
    }

    @FXML
    public boolean exit() {
        // exit unless user has unsaved changes and hits "cancel" at the pop up
        boolean shouldExit = saveChangesPopup("exiting");

        if(shouldExit) {
            Platform.exit();
        }
        return shouldExit;
    }

    @FXML
    public void openAtlas() {
        if(saveChangesPopup("loading new atlas file")) {
            loadAtlas(null);
            buildTree();
        }
        backupList = new ArrayList<>();
        expandedList = new ArrayList<>();
        undoButton.setDisable(true);
        redoButton.setDisable(true);
        cursor = 0;

    }
    @FXML
    public void newAtlas() {
        if(saveChangesPopup("creating new atlas")) {
            rootItem = new TreeItem<>(new AtlasRoot("Atlas", 0, 2, 0));
            treeView.setRoot(rootItem);
            buildTree();
        }
        filePath = "untitled";
        Main.setTitle(filePath);
        backupList = new ArrayList<>();
        expandedList = new ArrayList<>();
        undoButton.setDisable(true);
        redoButton.setDisable(true);
        cursor = 0;
    }

    @FXML
    public void undo() {
        // rollback one version (if not at back)
        if(backupList.size() > 0 && cursor > 0) {
            if(cursor == backupList.size()) {
                flagChange();
                cursor--;
            }
            loadAtlas(backupList.get(--cursor));
            expandFromList(expandedList.get(cursor), flattenTree());
            if(cursor <= 0) {
                undoButton.setDisable(true);
            }
            if(cursor < backupList.size() - 1) {
                redoButton.setDisable(false);
            }
            buildTree();
        }
    }
    @FXML
    public void redo() {
        // fast forward one backup (if not at front)
        if(backupList.size() > 0 && cursor < backupList.size() - 1) {
            loadAtlas(backupList.get(++cursor));
            expandFromList(expandedList.get(cursor), flattenTree());
            if(cursor >= backupList.size() - 1) {
                redoButton.setDisable(true);
            }
            if(cursor > 0) {
                undoButton.setDisable(false);
            }
            buildTree();
        }
    }
    @FXML
    public void changeRoot() {
        File tempRoot = new DirectoryChooser().showDialog(null);
        if(tempRoot != null) {
            root = tempRoot;
        }
        if(root == null) {
            root = new File("/");
        } else {
            writeConfig();
        }
        rootField.setText(root.getAbsolutePath());
    }

    private void writeConfig() {
        try {
            File configFile = new File("config.txt");

            PrintWriter writer = new PrintWriter(configFile);

            writer.println("root="+root.getAbsolutePath());
            if(toWrite != null) {
                for(String line : toWrite) {
                    writer.println(line);
                }
            }
            writer.close();
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
        }
    }

    // print node data in format .atlas expects (based on node type)
    private void printNode(PrintWriter writer, AtlasNode node) {
        if(node.getClass() == AtlasGroup.class) {
            writer.println("#group: " + node.getName());
        } else if(node.getClass() == AtlasAnimation.class) {
            writer.println("animations {");
            writer.println("id: " + "\"" + node.getName() + "\"");
        } else if(node.getClass() == AtlasBreakpoint.class) {
            writer.println("#sub: " + node.getName());
        } else if(node.getClass() == AtlasImage.class) {
            writer.println("images {");
            writer.println("image: " + "\"" + node.getName() + "\"");
            writer.println("sprite_trim_mode: " + ((AtlasImage) node).getTrimMode());
            writer.println("}");
        }
        if(node.getChildlist() != null) {
            for(AtlasNode child : node.getChildlist()) {
                printNode(writer, child);
            }
        }

        if(node.getClass() == AtlasGroup.class || node.getClass() == AtlasBreakpoint.class) {
            writer.println("#end");
        } else if(node.getClass() == AtlasAnimation.class) {
            writer.println("playback: " + ((AtlasAnimation) node).getPlayback());
            writer.println("fps: " + ((AtlasAnimation) node).getFps());
            int flipHorizontal = 0;
            if(((AtlasAnimation) node).isFlipHorizontal()) {
                flipHorizontal = 1;
            }
            writer.println("flip_horizontal: " + flipHorizontal);
            int flipVertical = 0;
            if(((AtlasAnimation) node).isFlipVertical()) {
                flipVertical = 1;
            }
            writer.println("flip_vertical: " + flipVertical);
            writer.println("}");
        }
    }

    @FXML
    public void saveOverwrite() {
        if(!filePath.equals("untitled")) {
            saveAtlas(filePath, true);
        } else {
            saveAtlas(null, true);
        }
    }
    @FXML
    public void saveAs() {
        saveAtlas(null, true);
    }

    // save atlas file
    public boolean saveAtlas(String newPath, boolean properSave) {
        boolean saved = true;
        File saveFile;

        // use passed in file path
        if(newPath != null) {
            saveFile = new File(newPath);
        } else { // or open a filechooser
            FileChooser saveChooser = new FileChooser();
            saveChooser.setInitialDirectory(root);
            saveChooser.getExtensionFilters().add( new FileChooser.ExtensionFilter("ATLAS (*.atlas)", "*.atlas") );
            saveFile = saveChooser.showSaveDialog(null);
            if(saveFile != null) {
                filePath = saveFile.getAbsolutePath();
            }
        }

        // if valid save destination is picked and some information has been saved in the atlas,
        if(saveFile != null && treeView.getRoot() != null) {
            try {
                PrintWriter writer = new PrintWriter(saveFile);
                // write node by node to that file
                for(TreeItem<AtlasNode> item : rootItem.getChildren()) {
                    printNode(writer, item.getValue());
                }
                AtlasRoot rootNode = (AtlasRoot) rootItem.getValue();

                // add root node params at the end of the .atlas file
                writer.println("margin: " + rootNode.getMargin());
                writer.println("extrude_borders: " + rootNode.getExtrudeBorders());
                writer.println("inner_padding: " + rootNode.getInnerPadding());
                writer.close();
                if(properSave) {
                    hasUnsavedChanges = false;
                }
            } catch (Exception e) {
                //saved = false;
            }
        } else if(saveFile == null) {
            saved = false;
        }
        if(saved && properSave) {
            Main.setTitle(filePath);
        }
        return saved;
    }
}
