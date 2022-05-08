package FileBrowser;

import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import java.nio.file.*;
import java.util.Objects;

public class FileTreeItem extends TreeItem<String> {
    public static Image folderCollapseImage = new Image(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("folder_closed.png")));
    public static Image folderOpenImage = new Image(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("folder_open.png")));
    public static Image fileImage = new Image(Objects.requireNonNull(ClassLoader.getSystemResourceAsStream("file_generic.png")));

    private final String fullPath;
    private final boolean isDirectory;
    private final Path myPath;
    private boolean loaded = false;

    public String getFullPath() { return fullPath; }

    public boolean isDirectory() { return isDirectory; }

    @SuppressWarnings({"unchecked", "rawtypes", "all"})
    public FileTreeItem(Path file, boolean root) {
        super(file.toFile().getAbsolutePath());

        myPath = file;

        this.fullPath = file.toString();
        if (!fullPath.endsWith(File.separator)) {
            String value = file.toString();
            int index = value.lastIndexOf(File.separator);
            if (index > 0) this.setValue(value.substring(index + 1));
            else this.setValue(value);
        }

        if (this.isDirectory = Files.isDirectory(file)) {
            this.setGraphic(new ImageView(folderCollapseImage));

            this.addEventHandler(TreeItem.branchExpandedEvent(), (EventHandler) event -> {
                FileTreeItem source = (FileTreeItem) event.getSource();
                if (source.isExpanded()) {
                    ((ImageView) source.getGraphic()).setImage(folderOpenImage);

                    for (TreeItem x : this.getChildren()) {
                        FileTreeItem temp = (FileTreeItem) x;
                        if (temp.isDirectory && !temp.loaded && temp.myPath.toFile().list() != null) for (String y : temp.myPath.toFile().list()) {
                            x.getChildren().add(new FileTreeItem(Paths.get(this.fullPath + "/" + y), false));
                            temp.loaded = true;
                        }
                    }
                }
            });

            this.addEventHandler(TreeItem.branchCollapsedEvent(), (EventHandler) e -> {
                FileTreeItem source = (FileTreeItem)e.getSource();
                if(!source.isExpanded()) ((ImageView) source.getGraphic()).setImage(folderCollapseImage);
            });

            if (root) {
                for (String x : file.toFile().list()) this.getChildren().add(new FileTreeItem(Paths.get(this.fullPath + "/" + x), false)); //TODO: add hidden files to folder
                loaded = true;
            }
        } else this.setGraphic(new ImageView(fileImage)); //TODO: set images for different filetypes
    }
}