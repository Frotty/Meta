package de.fatox.meta.ui.components;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisImage;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTextButton;
import de.fatox.meta.Meta;
import de.fatox.meta.api.AssetProvider;
import de.fatox.meta.api.dao.RenderBufferData;
import de.fatox.meta.api.graphics.MetaGLShader;
import de.fatox.meta.injection.Inject;
import de.fatox.meta.shader.MetaShaderLibrary;
import de.fatox.meta.util.GoldenRatio;

import static de.fatox.meta.api.dao.RenderBufferData.IN.FULLSCREEN;
import static de.fatox.meta.api.dao.RenderBufferData.IN.GEOMETRY;

/**
 * Created by Frotty on 04.06.2016.
 */
public class MetaPassButton extends Button {
    private final MetaLabel nameLabel;
    @Inject
    private AssetProvider assetProvider;
    @Inject
    private MetaShaderLibrary shaderLibrary;

    private VisSelectBox<RenderBufferData.IN> inSelect = new VisSelectBox<>();
    private VisSelectBox<MetaGLShader> shaderSelect = new VisSelectBox<>();
    private VisCheckBox depthCheckBox = new VisCheckBox("depth", false);

    public MetaPassButton(String text) {
        this(text, 12);
    }

    public MetaPassButton(String text, int size) {
        super(VisUI.getSkin().get(VisTextButton.VisTextButtonStyle.class));
        Meta.inject(this);
        setColor(Color.GRAY);
        pad(GoldenRatio.C * 10, GoldenRatio.A * 20, GoldenRatio.C * 10, GoldenRatio.A * 20);
        inSelect.setItems(GEOMETRY, FULLSCREEN);
        shaderSelect.setItems(shaderLibrary.getActiveShaders());

        nameLabel = new MetaLabel(text, size, Color.WHITE);
        nameLabel.setAlignment(Align.center);
        top();
        clear();
        add(nameLabel).colspan(2).center().growX();
        row().padTop(2);

        VisImage codeImage = new VisImage(assetProvider.getDrawable("ui/appbar.page.code.png"));
        codeImage.setScaling(Scaling.fit);
        add(codeImage).size(24).padRight(2).left();
        add(shaderSelect).growX();
        row().padTop(2);

        VisImage boxImage = new VisImage(assetProvider.getDrawable("ui/appbar.box.png"));
        boxImage.setScaling(Scaling.fit);
        add(boxImage).size(24).padRight(2).left();
        add(inSelect).growX();
        row().padTop(2);

        VisImage dImage = new VisImage(assetProvider.getDrawable("ui/appbar.grade.d.png"));
        dImage.setScaling(Scaling.fit);
        add(dImage).size(24).padRight(2).left();
        add(depthCheckBox).growX();
        row().padTop(2);
        pack();
    }


    public void setText(String text) {
        nameLabel.setText(text);
    }

    public CharSequence getText() {
        return nameLabel.getText();
    }


}
