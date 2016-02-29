package de.fatox.meta;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import de.fatox.meta.injection.Feather;
import de.fatox.meta.injection.Inject;

public class Meta extends Game {
	private static Feather featherInstance;

	public static final void inject(Object object) {
		if(featherInstance == null) {
			featherInstance = Feather.with(new MetaModule());
		}
		featherInstance.injectFields(object);
	}

	@Inject
	private Screen firstScreen;

	@Override
	public void create () {
		inject(this);
		setScreen(firstScreen);
	}
}
