package de.fatox.meta.api.dao;

/**
 * Created by Frotty on 26.06.2016.
 */
public class MetaData {
//    public static final String DATA_FOLDER_NAME = "./.meta/";
//    public static final String DATA_FILE_NAME = "metadata.json";
//
//    @Expose
//    private String[] lastProjects;
//    @Expose
//    private ExposedArray<MetaScreenData> screenData = new ExposedArray<>(4);
//    @Expose
//    private MetaAudioVideoData videoData = new MetaAudioVideoData();
//
//    public ExposedArray<String> lastProjectFiles = new ExposedArray<>();
//
//    @Inject
//    private Gson gson;
//
//    private FileHandle fileHandle;
//    private long lastMillis = TimeUtils.millis();
//
//    public MetaData() {
//        Meta.inject(this);
//    }
//
//    public void apply() {
//        if (videoData.isFullscreen()) {
//            if (!Gdx.graphics.isFullscreen()) {
//                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayModes()[videoData.getDisplayMode()]);
//            }
//        } else {
//            Gdx.graphics.setUndecorated(videoData.isBorderless());
//            Gdx.graphics.setWindowedMode(videoData.getWidth(), videoData.getHeight());
//        }
//        Gdx.graphics.setVSync(videoData.isVsyncEnabled());
//        write();
//    }
//
//    public void write() {
//        if (lastMillis + 500 < TimeUtils.millis()) {
//            fileHandle.writeBytes(gson.toJson(this).getBytes(), false);
//            lastMillis = TimeUtils.millis();
//        }
//    }
//
//    public void addLastProject(String s) {
//        if (!lastProjectFiles.contains(s, false)) {
//            lastProjectFiles.add(s);
//        }
//        lastProjects = lastProjectFiles.toArray(String.class);
//        write();
//    }
//
//    public void setFileHandle(FileHandle fileHandle) {
//        this.fileHandle = fileHandle;
//    }
//
//    public ExposedArray<String> getLastProjectFiles() {
//        if (lastProjectFiles.size == 0 && lastProjects != null && lastProjects.length > 0) {
//            lastProjectFiles.addAll(lastProjects);
//        }
//        return lastProjectFiles;
//    }
//
//    public MetaAudioVideoData getAudioVideoData() {
//        return videoData;
//    }
//
//    public void setMainWindowSize(int width, int height) {
//        videoData.setWidth(width);
//        videoData.setHeight(height);
//        write();
//    }

}
