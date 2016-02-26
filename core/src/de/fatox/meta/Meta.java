package de.fatox.meta;

import com.badlogic.gdx.Game;
import de.fatox.meta.injection.Feather;

public class Meta extends Game {
	private static Feather featherInstance;

	public static final void inject(Object object) {
		if(featherInstance == null) {
			featherInstance = Feather.with(new MetaModule());
		}
		featherInstance.injectFields(object);
	}

	@Override
	public void create () {
		inject(this);
		System.out.println("done");
	}
}
