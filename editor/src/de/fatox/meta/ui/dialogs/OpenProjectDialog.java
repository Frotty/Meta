package de.fatox.meta.ui.dialogs;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.Tooltip;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;
import com.kotcrab.vis.ui.widget.file.FileTypeFilter;
import de.fatox.meta.api.lang.LanguageBundle;
import de.fatox.meta.ide.ProjectManager;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.injection.Named;
import de.fatox.meta.ui.components.MetaClickListener;
import de.fatox.meta.ui.windows.MetaDialog;
import de.fatox.meta.ui.components.MetaTextButton;
import de.fatox.meta.util.StringUtil;

/**
 * Created by Frotty on 05.06.2016.
 */
public class OpenProjectDialog extends MetaDialog {
    @Inject
    private LanguageBundle languageBundle;
    @Inject
    @Named("open")
    private FileChooser fileChooser;
    @Inject
    private ProjectManager projectManager;

    private VisLabel folderLabel;
    private MetaTextButton folderButton;
    private FileHandle rootfile;

    public OpenProjectDialog() {
        super("Open Project", "Cancel", "Open");

        createFolderButton();

        VisTable visTable = new VisTable();
        visTable.defaults().pad(4);
        visTable.add(folderLabel).growX();
        visTable.add(folderButton).growX();
        visTable.row();
        contentTable.add(visTable).top().growX();
        rightButton.setDisabled(true);
    }


    private void createFolderButton() {
        folderLabel = new VisLabel(languageBundle.get("newproj_dia_proj_root"));
        folderButton = new MetaTextButton(languageBundle.get("newproj_dia_select_project"));
        folderButton.addListener(new MetaClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                fileChooser.setSelectionMode(FileChooser.SelectionMode.FILES);
                FileTypeFilter fileTypeFilter = new FileTypeFilter(false);
                fileTypeFilter.addRule("Meta Project File", "json");
                fileChooser.setFileTypeFilter(fileTypeFilter);
                fileChooser.fadeIn();
                getStage().setKeyboardFocus(fileChooser);
                fileChooser.setListener(new FileChooserAdapter() {
                    @Override
                    public void selected(Array<FileHandle> file) {
                        if (file.size == 1) {
                            rootfile = file.get(0);
                            folderButton.setText(StringUtil.truncate(file.get(0).pathWithoutExtension(), 30));
                            if(projectManager.verifyProjectFile(rootfile)) {
                                rightButton.setDisabled(false);
                            }
                        }
                        fileChooser.fadeOut();
                    }
                });
                getStage().addActor(fileChooser);
                fileChooser.fadeIn();
            }
        });
        new Tooltip.Builder(languageBundle.get("newproj_dia_tooltip_location")).target(folderLabel).build();
    }

    @Override
    public void onResult(Object object) {
        boolean b = (boolean) object;
        if(b) {
            projectManager.loadProject(rootfile);
        }
        close();
    }
}
