package com.nabob.conch.tomcat.core.tomcat.i18n;

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * 国际化String管理 Tomcat 内部的国际化
 * <p>
 * 基于package路径进行操作，每个包都会有自己的独立的String管理
 * <p>
 * 缓存
 * <p>
 * 核心：
 * - ResourceBundle
 * - Locale
 *
 * @author Adam
 * @see java.util.ResourceBundle
 * @since 2023/12/5
 */
public class StringManager {

    /**
     * 缓存大小
     */
    private static int LOCALE_CACHE_SIZE = 10;

    private static final Map<String, Map<Locale, StringManager>> managers = new HashMap<>();

    private final ResourceBundle bundle;

    private final Locale locale;

    public Locale getLocale() {
        return locale;
    }

    private StringManager(String packageName, Locale locale) {
        String bundleName = packageName + ".LocalStrings";

        ResourceBundle resBnd = null;
        try {
            // The ROOT Locale uses English
            if (locale.getLanguage().equals(Locale.ENGLISH.getLanguage())) {
                locale = Locale.ROOT;
            }
            resBnd = ResourceBundle.getBundle(bundleName, locale);
        } catch (MissingResourceException ex) {
            // Try from the current loader
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                try {
                    resBnd = ResourceBundle.getBundle(bundleName, locale, cl);
                } catch (MissingResourceException ex2) {
                    // Ignore
                }
            }
        }

        bundle = resBnd;

        // Get the actual locale, which may be different from the requested one
        if (bundle != null) {
            Locale bundleLocale = bundle.getLocale();
            if (bundleLocale.equals(Locale.ROOT)) {
                this.locale = Locale.ENGLISH;
            } else {
                this.locale = bundleLocale;
            }
        } else {
            this.locale = null;
        }
    }

    public String getString(String key) {
        if (key == null) {
            String msg = "key may not have a null value";
            throw new IllegalArgumentException(msg);
        }

        String str = null;

        try {
            // Avoid NPE if bundle is null and treat it like an MRE
            if (bundle != null) {
                str = bundle.getString(key);
            }
        } catch (MissingResourceException mre) {
            str = null;
        }

        return str;
    }

    public String getString(final String key, final Object... args) {
        String value = getString(key);
        if (value == null) {
            value = key;
        }

        MessageFormat mf = new MessageFormat(value);
        mf.setLocale(locale);
        return mf.format(args, new StringBuffer(), null).toString();
    }

    public static final StringManager getManager(Class<?> clazz) {
        return getManager(clazz.getPackage().getName());
    }

    public static final StringManager getManager(String packageName) {
        return getManager(packageName, Locale.getDefault());
    }

    public static final synchronized StringManager getManager(String packageName, Locale locale) {

        Map<Locale, StringManager> map = managers.get(packageName);
        if (map == null) {
            /*
             * Don't want the HashMap size to exceed LOCALE_CACHE_SIZE. Expansion occurs when size() exceeds capacity.
             * Therefore keep size at or below capacity. removeEldestEntry() executes after insertion therefore the test
             * for removal needs to use one less than the maximum desired size. Note this is an LRU cache.
             */
            map = new LinkedHashMap<>(LOCALE_CACHE_SIZE, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<Locale, StringManager> eldest) {
                    if (size() > (LOCALE_CACHE_SIZE - 1)) {
                        return true;
                    }
                    return false;
                }
            };
            managers.put(packageName, map);
        }

        StringManager mgr = map.get(locale);
        if (mgr == null) {
            mgr = new StringManager(packageName, locale);
            map.put(locale, mgr);
        }
        return mgr;
    }

    public static StringManager getManager(String packageName, Enumeration<Locale> requestedLocales) {
        while (requestedLocales.hasMoreElements()) {
            Locale locale = requestedLocales.nextElement();
            StringManager result = getManager(packageName, locale);
            if (result.getLocale().equals(locale)) {
                return result;
            }
        }
        // Return the default
        return getManager(packageName);
    }
}
