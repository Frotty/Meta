package de.fatox.meta;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import de.fatox.meta.injection.Feather;

public class Meta extends ApplicationAdapter {
	private static Feather featherInstance;

	public static final void inject(Object object) {
		if(featherInstance == null) {
			featherInstance = Feather.with(new MetaModule());
		}
		featherInstance.injectFields(object);
	}

	@Override
	public void create () {
		//TODO
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}
}
