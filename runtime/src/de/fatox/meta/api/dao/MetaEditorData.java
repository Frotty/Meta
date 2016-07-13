package de.fatox.meta.api.dao;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.TimeUtils;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import de.fatox.meta.Meta;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 26.06.2016.
 */
public class MetaEditorData {
    public static final String DATA_FILE_NAME = "metadata.json";

    public Array<String> lastProjectFiles = new Array<>();
    public Array<MetaWindowData> windowDatas = new Array<>();
    @Expose
    private String[] lastProjects;
    @Expose
    private MetaWindowData[] windowData;
    @Expose
    private int windowWidth = 1200;
    @Expose
    private int windowHeight = 700;
    @Inject
    private Gson gson;

    private FileHandle fileHandle;
    private long lastMillis = TimeUtils.millis();

    public MetaEditorData() {
        Meta.inject(this);
    }

    public void write() {
        if (lastMillis + 500 < TimeUtils.millis()) {
            fileHandle.writeBytes(gson.toJson(this).getBytes(), false);
            lastMillis = TimeUtils.millis();
        }
    }

    public void addLastProject(String s) {
        if (!lastProjectFiles.contains(s, false)) {
            lastProjectFiles.add(s);
        }
        lastProjects = lastProjectFiles.toArray(String.class);
        write();
    }

    public void setFileHandle(FileHandle fileHandle) {
        this.fileHandle = fileHandle;

        windowDatas.addAll(windowData);
    }

    public Array<String> getLastProjectFiles() {
        if (lastProjectFiles.size == 0) {
            lastProjectFiles.addAll(lastProjects);
        }
        return lastProjectFiles;
    }

    public int getMainWindowWidth() {
        return windowWidth;
    }

    public int getMainWindowHeight() {
        return windowHeight;
    }

    public void setMainWindowSize(int width, int height) {
        this.windowWidth = width;
        this.windowHeight = height;
        write();
    }

    public MetaWindowData getWindowData(Window window) {
        String name = window.getTitleLabel().getText().toString();
        for (MetaWindowData data : windowDatas) {
            if (data.name.equals(name)) {
                return data;
            }
        }
        MetaWindowData data = new MetaWindowData(window);
        windowDatas.add(data);
        windowData = windowDatas.toArray(MetaWindowData.class);
        write();
        return data;
    }
}
