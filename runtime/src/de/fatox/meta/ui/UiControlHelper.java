package de.fatox.meta.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import de.fatox.meta.injection.Singleton;

@Singleton
public class UiControlHelper {
    private Widget selectedWidget = null;



    public Widget getSelectedWidget() {
        return selectedWidget;
    }

    public void setSelectedWidget(Widget selectedWidget) {
        this.selectedWidget = selectedWidget;
    }
}
