package de.fatox.meta.api.dao;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.TimeUtils;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.kotcrab.vis.ui.widget.VisWindow;
import de.fatox.meta.Meta;
import de.fatox.meta.injection.Inject;

/**
 * Created by Frotty on 26.06.2016.
 */
public class MetaData {
    public static final String DATA_FILE_NAME = "metadata.json";

    @Expose
    private String[] lastProjects;
    @Expose
    private ExposedArray<MetaScreenData> screenData = new ExposedArray<>(4);
    @Expose
    private int windowWidth = 1200;
    @Expose
    private int windowHeight = 700;

    public ExposedArray<String> lastProjectFiles = new ExposedArray<>();
    public MetaScreenData currentScreenData;

    @Inject
    private Gson gson;

    private FileHandle fileHandle;
    private long lastMillis = TimeUtils.millis();

    public MetaData() {
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
    }

    public ExposedArray<String> getLastProjectFiles() {
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

    public MetaScreenData getScreenData(String screenName) {
        for (MetaScreenData data : screenData) {
            if (data.name.equals(screenName)) {
                currentScreenData = data;
                return data;
            }
        }
        MetaScreenData data = new MetaScreenData(screenName);
        screenData.add(data);
        currentScreenData = data;
        write();
        return data;
    }

    public boolean hasWindowData(VisWindow data) {
        for (MetaWindowData wdata : currentScreenData.windowData) {
            if (wdata.name.equals(data.getTitleLabel().getText().toString())) {
                return true;
            }
        }
        return false;
    }

    public MetaWindowData getWindowData(VisWindow data) {
        for (MetaWindowData wdata : currentScreenData.windowData) {
            if (wdata.name.equals(data.getTitleLabel().getText().toString())) {
                return wdata;
            }
        }
        MetaWindowData ndata = new MetaWindowData(data);
        currentScreenData.windowData.add(ndata);
        write();
        return ndata;
    }
}
