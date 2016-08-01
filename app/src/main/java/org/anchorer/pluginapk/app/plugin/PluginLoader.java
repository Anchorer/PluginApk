package org.anchorer.pluginapk.app.plugin;

import android.util.Log;

import org.anchorer.pluginapk.app.TestApplication;
import org.anchorer.pluginapk.app.common.CommonUtils;
import org.anchorer.pluginapk.app.common.Const;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class PluginLoader {

	private static final int LOAD_PLUGIN_APK_STATE_INIT = 0;
	private static final int LOAD_PLUGIN_APK_STATE_LOADED = 2;
	private int mLoadPluginApkState = LOAD_PLUGIN_APK_STATE_INIT;

	private DexClassLoader mDexClassLoader;

	public void loadPluginApk(String pluginName) {
		DexClassLoader classLoader = createDexClassLoader(pluginName);
		if (classLoader != null) {
			inject(classLoader);
		}
		mLoadPluginApkState = LOAD_PLUGIN_APK_STATE_LOADED;
	}

	public Object newInstance(String className) {
		if (mDexClassLoader == null) {
			return null;
		}
		
		try {
			Class<?> clazz = mDexClassLoader.loadClass(className);
			Object instance = clazz.newInstance();
			return instance;
		} catch (Exception e) {
			Log.e(Const.LOG, "newInstance className = " + className + " failed" + " exception = " + e.getMessage());
		}
		
		return null;
	}

	private String getPlguinApkDirectory() {
		File pluginPath = TestApplication.getInstance().getDir("apk", 0);
		return pluginPath.getAbsolutePath() + "/";
	}

	/**
	 * 将插件APK保存至SD卡
	 * @param pluginName	插件APK的名称
     */
	private boolean savePluginApkToStorage(String pluginName) {
		String pluginApkPath = this.getPlguinApkDirectory() + pluginName;

		File plugApkFile = new File(pluginApkPath);
		if (plugApkFile.exists()) {
			try {
				plugApkFile.delete();
			} catch (Throwable e) {}
		}
		
		BufferedInputStream inStream = null;
		BufferedOutputStream outStream = null;

		try {
			InputStream stream = TestApplication.getInstance().getAssets().open("plugin/" + pluginName);
			inStream = new BufferedInputStream(stream);
			outStream = new BufferedOutputStream(new FileOutputStream(pluginApkPath));
			
			final int BUF_SIZE = 4096;
			byte[] buf = new byte[BUF_SIZE];
			while(true) {
				int readCount = inStream.read(buf, 0, BUF_SIZE);
				if (readCount == -1) {
					break;
				}
				outStream.write(buf,0, readCount);
			}
		} catch(Exception e) {
			return false;
		} finally {
			if (inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {}
				inStream = null;
			}
			
			if (outStream != null) {
				try {
					outStream.close();
				} catch (IOException e) {}
				outStream = null;
			}
		}
		return true;
	}

	/**
	 * Create Class Loader
	 * @param pluginName	插件APK名称
     */
	private DexClassLoader createDexClassLoader(String pluginName) {
		boolean saved = savePluginApkToStorage(pluginName);
		if (!saved) {
			return null;
		}

		DexClassLoader classLoader = null;
		try {
			String apkPath = getPlguinApkDirectory() + pluginName;
			File dexOutputDir = TestApplication.getInstance().getDir("dex", 0);
			String dexOutputDirPath = dexOutputDir.getAbsolutePath();
			Log.d(Const.LOG, " apkPath = " + apkPath + " dexOutputPath = " + dexOutputDirPath);
			
	        ClassLoader cl = TestApplication.getInstance().getClassLoader();
			classLoader = new DexClassLoader(apkPath, dexOutputDirPath, null, cl);
		} catch(Throwable e) {}
		return classLoader;
	}

	public boolean inject(DexClassLoader dexClassLoader) {
	    try {
	        Class.forName("dalvik.system.BaseDexClassLoader");
	    } catch (ClassNotFoundException e) {}

		PathClassLoader pathClassLoader = (PathClassLoader)TestApplication.getInstance().getClassLoader();
		Log.d(Const.LOG, "path class loader class = " + PathClassLoader.class.toString());
		try {
			Object dexElements = CommonUtils.combineArray(getDexElements(getPathList(pathClassLoader)), getDexElements(getPathList(dexClassLoader)));
			Object pathList = getPathList(pathClassLoader);
			setField(pathList, pathList.getClass(), "dexElements", dexElements);
			Log.d(Const.LOG, "inject pahtClassLoader = " + pathClassLoader);
			mDexClassLoader = dexClassLoader;
			return true;
		} catch (Throwable e) {
			return false;
		}
	}

	/**
	 * The BaseDexClassLoader definition
	 *  https://android.googlesource.com/platform/libcore-snapshot/+/ics-mr1/dalvik/src/main/java/dalvik/system/BaseDexClassLoader.java
	 * @param pathClassLoader
	 * @return
	 * @throws Exception
	 */
	private Object getPathList(Object pathClassLoader) throws Exception {
		Field pathField = BaseDexClassLoader.class.getDeclaredField("pathList");
		pathField.setAccessible(true);
		Object pathListObj = pathField.get(pathClassLoader);
		Log.d(Const.LOG, " pathListObj = " + pathListObj);
		return pathListObj;
	}
	
	private Object getDexElements(Object pathListObj) throws Exception {
		Log.d(Const.LOG, "getDexElements pathListObj = " + pathListObj);

		Field dexElementsField = pathListObj.getClass().getDeclaredField("dexElements");
		dexElementsField.setAccessible(true);
		Object dexElements = dexElementsField.get(pathListObj);
		
		Log.d(Const.LOG, " dexElements = " + dexElements);
		return dexElements;
	}
	
	private void setField(Object obj, Class<? extends Object> clazz, String filedName, Object value) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		Field field = clazz.getDeclaredField(filedName);
		field.setAccessible(true);
		field.set(obj, value);
	}

	/**
	 * 检查插件APK是否已经load成功
     */
	public static int getLoadPluginApkStateLoaded() {
		return LOAD_PLUGIN_APK_STATE_LOADED;
	}
}
