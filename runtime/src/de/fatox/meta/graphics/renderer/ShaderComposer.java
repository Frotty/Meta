package de.fatox.meta.graphics.renderer;

import com.badlogic.gdx.utils.Array;
import de.fatox.meta.Meta;

/**
 * Created by Frotty on 10.04.2017.
 */
public class ShaderComposer {
    private Array<ShaderComposition> compositions = new Array<>(2);

    public ShaderComposer() {
        Meta.inject(this);
    }

    public void addComposition(ShaderComposition composition) {
        compositions.add(composition);
    }

    public ShaderComposition getComposition(String compName) {
        for(ShaderComposition comp : compositions) {
            if(comp.data.name.equals(compName)) {
                return comp;
            }
        }
        return null;
    }
}
