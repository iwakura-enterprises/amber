package enterprises.iwakura.amber;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

/**
 * Custom class loader for loading classes from specified dependencies and the caller's location. This class loader allows immediate
 * access to all downloaded/loaded dependencies without restarting the JVM in order to load Class-Path resources.
 */
public class AmberClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Constructs a new AmberClassLoader with the specified dependencies, parent class loader, and caller class.
     *
     * @param dependencies the list of dependency paths to include in the class loader
     * @param parent       the parent class loader
     * @param caller       the caller class whose code source location will be included
     */
    public AmberClassLoader(List<Path> dependencies, ClassLoader parent, Class<?> caller) {
        super(constructUrls(dependencies, caller), parent);
    }

    /**
     * Constructs an array of URLs from the given list of paths and the caller's location.
     *
     * @param pathList the list of paths to convert to URLs
     * @param caller   the caller class whose code source location will be included
     *
     * @return an array of URLs
     */
    private static URL[] constructUrls(List<Path> pathList, Class<?> caller) {
        URL[] urls = new URL[pathList.size() + 1];
        for (int i = 0; i < pathList.size(); i++) {
            Path path = pathList.get(i);
            // FIXME: In rare cases there can be null path. Find why
            if (path != null) {
                try {
                    urls[i] = path.toUri().toURL();
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed to convert path " + path + " into URL", e);
                }
            } else {
                System.err.println("Null path passed into AmberClassLoader");
            }
        }
        urls[pathList.size()] = caller.getProtectionDomain().getCodeSource().getLocation();
        return urls;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> clazz = findLoadedClass(name);
            if (clazz == null) {
                // Only delegate java.* and javax.* to parent first
                if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("sun.") || name.startsWith("jdk.")) {
                    clazz = getParent().loadClass(name);
                } else {
                    try {
                        clazz = findClass(name); // try own URLs first
                    } catch (ClassNotFoundException e) {
                        clazz = getParent().loadClass(name); // fallback to parent
                    }
                }
            }
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }
    }
}
