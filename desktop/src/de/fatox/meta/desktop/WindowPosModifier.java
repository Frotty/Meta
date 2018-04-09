package de.fatox.meta.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import de.fatox.meta.api.PosModifier;
import org.lwjgl.glfw.GLFW;

public class WindowPosModifier implements PosModifier {

    @Override
    public void modify(int x, int y) {
        try {
            Field field = ClassReflection.getDeclaredField(Lwjgl3Application.class, "currentWindow");
            field.setAccessible(true);
            Lwjgl3Window result = (Lwjgl3Window) field.get(Gdx.app);
            GLFW.glfwSetWindowPos(result.getWindowHandle(), x, y);
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getX() {
        try {
            Field field = ClassReflection.getDeclaredField(Lwjgl3Application.class, "currentWindow");
            field.setAccessible(true);
            Lwjgl3Window result = (Lwjgl3Window) field.get(Gdx.app);
            return result.getPositionX();
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
        return 2;
    }

    @Override
    public int getY() {
        try {
            Field field = ClassReflection.getDeclaredField(Lwjgl3Application.class, "currentWindow");
            field.setAccessible(true);
            Lwjgl3Window result = (Lwjgl3Window) field.get(Gdx.app);
            return result.getPositionY();
        } catch (ReflectionException e) {
            e.printStackTrace();
        }
        return 12;
    }
}
