package whiterose.foundation;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Advanced HotSwapManager for 1.7.10.
 * Designed to work safely in large modpacks by isolating "scripts" or "addons".
 */
public class HotSwapManager {

    private static HotSwapManager instance;
    private CustomClassLoader currentLoader;
    private final List<Object> loadedModules = new ArrayList<Object>();
    private final File scriptDir;

    public HotSwapManager() {
        // Use a dedicated folder in the instance root
        this.scriptDir = new File(Minecraft.getMinecraft().mcDataDir, "hotscripts");
        if (!scriptDir.exists()) {
            scriptDir.mkdirs();
        }
    }

    /**
     * Call this in your @Mod's FMLInitializationEvent or FMLPostInitializationEvent.
     */
    public static void init() {
        instance = new HotSwapManager();
        instance.loadModules();

        // Register the keybinding for triggering reloads
        instance.registerKeybinding();
    }

    private void registerKeybinding() {
        ClientRegistry.registerKeyBinding(KeyHandler.reloadKey);
        FMLCommonHandler.instance().bus().register(new KeyHandler());
    }

    public static void reload() {
        if (instance != null) {
            instance.unloadModules();
            // Give the GC a tiny window to breathe
            instance.loadModules();
            log(EnumChatFormatting.GREEN + "HotSwap Reload Complete!");
        }
    }

    private void unloadModules() {
        log("Unregistering " + loadedModules.size() + " modules...");
        for (Object module : loadedModules) {
            try {
                // Critical: Remove listeners before discarding the classloader
                MinecraftForge.EVENT_BUS.unregister(module);
                FMLCommonHandler.instance().bus().unregister(module);

                // Call custom 'onUnload' via reflection if present
                try {
                    module.getClass().getMethod("onUnload").invoke(module);
                } catch (NoSuchMethodException ignored) {}

            } catch (Exception e) {
                log(EnumChatFormatting.RED + "Unload Error: " + e.getMessage());
            }
        }
        loadedModules.clear();

        if (currentLoader != null) {
            try {
                currentLoader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        currentLoader = null;

        // Vital for preventing Metaspace/PermGen leaks in large packs
        System.gc();
    }

    private void loadModules() {
        if (!scriptDir.exists()) return;

        try {
            File[] files = scriptDir.listFiles();
            if (files == null || files.length == 0) return;

            List<URL> urls = new ArrayList<URL>();
            urls.add(scriptDir.toURI().toURL());
            for (File f : files) {
                if (f.getName().endsWith(".jar")) urls.add(f.toURI().toURL());
            }

            currentLoader = new CustomClassLoader(urls.toArray(new URL[0]), HotSwapManager.class.getClassLoader());

            for (File file : files) {
                if (file.getName().endsWith(".class")) {
                    loadClassFile(file.getName().replace(".class", ""));
                } else if (file.getName().endsWith(".jar")) {
                    loadJarFile(file);
                }
            }
        } catch (Exception e) {
            log(EnumChatFormatting.RED + "Load Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadJarFile(File file) throws Exception {
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName().replace("/", ".").replace(".class", "");
                    // Filter: only load classes that look like modules
                    if (className.endsWith("Module") || className.contains("script")) {
                        loadClassFile(className);
                    }
                }
            }
        }
    }

    private void loadClassFile(String className) {
        try {
            Class<?> clazz = currentLoader.loadClass(className);

            // Safety Check: Don't load if it's already a system class
            if (clazz.getClassLoader() != currentLoader) return;

            Object moduleInstance = clazz.newInstance();

            // Re-register to buses
            MinecraftForge.EVENT_BUS.register(moduleInstance);
            FMLCommonHandler.instance().bus().register(moduleInstance);

            loadedModules.add(moduleInstance);
            log("Loaded: " + className);
        } catch (Exception e) {
            log(EnumChatFormatting.RED + "Failed to load " + className);
        }
    }

    private static void log(String msg) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(EnumChatFormatting.GOLD + "[HotSwap] " + EnumChatFormatting.WHITE + msg));
        } else {
            System.out.println("[HotSwap] " + msg);
        }
    }

    /**
     * Inner class to handle Key Input events separately.
     */
    public static class KeyHandler {
        public static final KeyBinding reloadKey = new KeyBinding("HotSwap Reload", Keyboard.KEY_F10, "Whiterose Foundation");

        @SubscribeEvent
        public void onKeyInput(InputEvent.KeyInputEvent event) {
            // Check for key press and that we aren't in a GUI (like chat/inventory)
            if (reloadKey.isPressed() && Minecraft.getMinecraft().currentScreen == null) {
                HotSwapManager.reload();
            }
        }
    }

    /**
     * Specialized ClassLoader to prevent leaking classes into the main app.
     */
    private static class CustomClassLoader extends URLClassLoader {
        public CustomClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    try {
                        // Priority: Check the 'hotscripts' folder first
                        c = findClass(name);
                    } catch (ClassNotFoundException e) {
                        // Fallback: Delegate to the main game ClassLoader
                        c = super.loadClass(name, resolve);
                    }
                }
                if (resolve) resolveClass(c);
                return c;
            }
        }
    }
}