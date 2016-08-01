package org.anchorer.pluginapk.app.plugin;

import android.util.Log;

import org.anchorer.pluginapk.app.TestApplication;
import org.anchorer.pluginapk.app.common.Const;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import dalvik.system.DexClassLoader;

public class PluginLoader {

	private DexClassLoader mDexClassLoader;

	/**
	 * 加载插件APK
	 * @param pluginName	插件APK的名称
     */
	public void loadPluginApk(String pluginName) {
		mDexClassLoader = createDexClassLoader(pluginName);
	}

	/**
	 * 加载指定名称的类
	 * @param className	类名(包含包名)
     */
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
}
