package org.anchorer.pluginapk.app.plugin;

import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;

import org.anchorer.pluginapk.app.TestApplication;
import org.anchorer.pluginapk.app.consts.Const;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class PluginLoader {

	private static final int LOAD_PLUGIN_APK_STATE_INIT = 0;
	private static final int LOAD_PLUGIN_APK_STATE_LOADING = 1;
	private static final int LOAD_PLUGIN_APK_STATE_LOADED = 2;
	private int mLoadPluginApkState = LOAD_PLUGIN_APK_STATE_INIT;

	private DexClassLoader mDexClassLoader;
	private String mPluginName;

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

	private boolean savePluginApkToStorage(String pluginName) {
		this.mPluginName = pluginName;
		String pluginApkPath = this.getPlguinApkDirectory() + pluginName;

		File plugApkFile = new File(pluginApkPath);
		if (plugApkFile.exists()) {
			try {
				plugApkFile.delete();
			}catch (Throwable e) {
				e.printStackTrace();
			}
		}
		
		BufferedInputStream inStream = null;
		BufferedOutputStream outStream = null;

		try {
			
			InputStream stream = TestApplication.getInstance().getAssets().open("plugin/" + pluginName);
			
			inStream = new BufferedInputStream(stream);
			
			outStream = new BufferedOutputStream(new FileOutputStream(pluginApkPath));
			
			Log.d(Const.LOG, " pluginFilePath = " + pluginApkPath);
			final int BUF_SIZE = 4096;
			byte[] buf = new byte[BUF_SIZE];
			while(true) {
				
				int readCount = inStream.read(buf, 0, BUF_SIZE);
				if(readCount == -1) {
					break;
				}
				outStream.write(buf,0, readCount);
				
			}
		
		}catch(Exception e) {
			return false;
			
		}finally {
			
			if(inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				inStream = null;
			}
			
			if(outStream != null) {
				try {
					outStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				outStream = null;
			}
		}
		
		Log.d(Const.LOG, "End savePluginApkToStorage ");
		return true;
	}

	private DexClassLoader createDexClassLoader(String pluginName) {
		
		boolean saved = savePluginApkToStorage(pluginName);
		if(!saved) {
			return null;
		}
		
		Log.d(Const.LOG, "createDexClassLoader pluginName = " + pluginName);
		
		DexClassLoader classLoader = null;
		
		try {
			
			String apkPath = this.getPlguinApkDirectory() + pluginName;
			
			File dexOutputDir = TestApplication.getInstance().getDir("dex", 0);
			
			String dexOutputDirPath = dexOutputDir.getAbsolutePath();
			
			Log.d(Const.LOG, " apkPath = " + apkPath + " dexOutputPath = " + dexOutputDirPath);
			
			ApplicationInfo ai = TestApplication.getInstance().getApplicationInfo();
	        String nativeLibraryDir = null;
	        if (Build.VERSION.SDK_INT > 8) {
	            nativeLibraryDir = ai.nativeLibraryDir;
	        } else {
	            nativeLibraryDir = "/data/data/" + ai.packageName + "/lib/";
	        }
	        
	        Log.d(Const.LOG, " native library path = " + nativeLibraryDir);
	        
	        ClassLoader cl = TestApplication.getInstance().getClassLoader();
	        
	        Log.d(Const.LOG, " get parent class loader = " + cl.getParent());
	        
			classLoader = new DexClassLoader(apkPath, dexOutputDirPath, nativeLibraryDir,
					cl);  
			
			Log.d(Const.LOG, "after new class loader classLoader = " + classLoader.toString());
		} catch(Throwable e) {}
		
		Log.d(Const.LOG, "End load apk");
		
		return classLoader;
		
	}

	public boolean inject(DexClassLoader dexClassLoader) {
		
	    boolean hasBaseDexClassLoader = true;  
	    try {  
	        Class.forName("dalvik.system.BaseDexClassLoader");
	    } catch (ClassNotFoundException e) {
	        hasBaseDexClassLoader = false;  
	    }  
	    
	    if (hasBaseDexClassLoader) {  
	    	
	        PathClassLoader pathClassLoader = (PathClassLoader)TestApplication.getInstance().getClassLoader();
	        Log.d(Const.LOG, "path class loader class = " + PathClassLoader.class.toString());
	        try {  
	        	
	            Object dexElements = combineArray(getDexElements(getPathList(pathClassLoader)), getDexElements(getPathList(dexClassLoader)));
	            Object pathList = getPathList(pathClassLoader);
	            setField(pathList, pathList.getClass(), "dexElements", dexElements);  
	            Log.d(Const.LOG, "inject pahtClassLoader = " + pathClassLoader);
	            mDexClassLoader = dexClassLoader;
	            return true;
	            
	        } catch (Throwable e) {
	            return false;
	        }  
	        
	    }else {
	    	
	    	return inject_2_x(dexClassLoader);
	    }
	    
	}

	private Object combineArray(Object array1, Object array2) {
		
		Log.d(Const.LOG, " array 1 = " + array1 + " array2 = " + array2);
		
		Class<?> clazz = Array.get(array1, 0).getClass();
		
		int size1 = Array.getLength(array1);
		int size2 = Array.getLength(array2);
		
		Object newArrayObj = Array.newInstance(clazz, size1+ size2);
		
		int index = 0;
		for(int i=0; i<size1; i++) {
			
			Object element = Array.get(array1, i);
			Array.set(newArrayObj, index, element);
			Log.d(Const.LOG, "element 1 = " + element);
			index++;
		}
		
		for(int j=0; j<size2; j++) {
			
			Object element = Array.get(array2, j);
			Log.d(Const.LOG, "element 2 = " + element);
			Array.set(newArrayObj, index, element);
			index++;
		}
		
		Log.d(Const.LOG, "combineArray result array = " + newArrayObj);
		return newArrayObj;
		
	}
	
	/** The BaseDexClassLoader definition
	 *  https://android.googlesource.com/platform/libcore-snapshot/+/ics-mr1/dalvik/src/main/java/dalvik/system/BaseDexClassLoader.java
	 * @param pathClassLoader
	 * @return
	 * @throws Exception
	 */
	private Object getPathList(Object pathClassLoader) throws Exception {
		
		// "mPaths"
			// DexPathList
		Field pathField = BaseDexClassLoader.class.getDeclaredField("pathList");
		pathField.setAccessible(true);
		Object pathListObj = pathField.get(pathClassLoader);
		Log.d(Const.LOG, " pathListObj = " + pathListObj);
		
		return pathListObj;
		
	}
	
	private Object getDexElements(Object pathListObj) throws Exception {
		
		Log.d(Const.LOG, "getDexElements pathListObj = " + pathListObj);
		
		 /** list of dex/resource (class path) elements */
	   // private final Element[] dexElements;
			
		Field dexElementsField = pathListObj.getClass().getDeclaredField("dexElements");
		dexElementsField.setAccessible(true);
		Object dexElements = dexElementsField.get(pathListObj);
		
		Log.d(Const.LOG, " dexElements = " + dexElements);
		return dexElements;
		
	}
	
	private void setField(Object obj, Class<? extends Object> clazz,
						  String filedName, Object value) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		
			Field field = clazz.getDeclaredField(filedName);
			field.setAccessible(true);
			field.set(obj, value);
	}
	
	/**
	 * for 2.x system pathClassLoader definition
	 * public class PathClassLoader extends ClassLoader {
    private final String path;
    private final String libPath;
    /*
     * Parallel arrays for jar/apk files.
     *
     * (could stuff these into an object and have a single array;
     * improves clarity but adds overhead)
     */
	/**
    private final String[] mPaths;
    private final File[] mFiles;
    private final ZipFile[] mZips;
    private final DexFile[] mDexs;
    */
    /**
     * Native library path.
     */
   /** private final List<String> libraryPathElements;
    * 
    *  Please ref https://android.googlesource.com/platform/libcore/+/android-2.3.6_r1/dalvik/src/main/java/dalvik/system/PathClassLoader.java
 * @throws NoSuchFieldException
 * @throws IllegalAccessException
 * @throws IllegalArgumentException
	 */
	 
	public boolean inject_2_x(DexClassLoader dexClassLoader) {
		
        PathClassLoader pathClassLoader = (PathClassLoader)TestApplication.getInstance().getClassLoader();
        Log.d(Const.LOG, "inject_2_x path class loader before changed " + pathClassLoader);
        try {  
        	
        	// mPaths
        	Object paths = combineArray(getPaths(pathClassLoader), getRawDexPathList(dexClassLoader));
        	setField(pathClassLoader, pathClassLoader.getClass(), "mPaths", paths);
        	
            // mFiles
            Object files = combineArray(getFiles(pathClassLoader), getFiles(dexClassLoader));
            setField(pathClassLoader, pathClassLoader.getClass(), "mFiles", files);
            
            // mZips
            Object zips = combineArray(getZipFiles(pathClassLoader), getZipFiles(dexClassLoader));
            setField(pathClassLoader, pathClassLoader.getClass(), "mZips", zips);
            
            // mDexs
            Object dexs = combineArray(getDexFiles(pathClassLoader), getDexFiles(dexClassLoader));
            setField(pathClassLoader, pathClassLoader.getClass(), "mDexs", dexs);
            
            Log.d(Const.LOG, "inject_2_x inject pahtClassLoader = " + pathClassLoader);
            mDexClassLoader = dexClassLoader;
            return true;
            
        } catch (Throwable e) {
            return false;
        }  
	    
	}

	private Object getRawDexPathList(Object dexClassLoader) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		
		Field rawDexPathField = dexClassLoader.getClass().getDeclaredField("mRawDexPath");
		rawDexPathField.setAccessible(true);
		String rawDexPath = (String) rawDexPathField.get(dexClassLoader);
		
		Log.d(Const.LOG, "getRawDexPathList rawDexPath = " + rawDexPath);
		
		String[] dexPathList = rawDexPath.split(":");
		Log.d(Const.LOG, "dexPath list = " + Arrays.toString(dexPathList));
		return dexPathList;
		
	}
	
	private Object getPaths(Object pathClassLoader) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		
		Field pathsField = pathClassLoader.getClass().getDeclaredField("mPaths");
		pathsField.setAccessible(true);
		Object paths = pathsField.get(pathClassLoader);
		return paths;
		
	}
	
	private Object getFiles(Object pathClassLoader) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		
		Field filesField = pathClassLoader.getClass().getDeclaredField("mFiles");
		filesField.setAccessible(true);
		Object files = filesField.get(pathClassLoader);
		return files;
	}
	
	private Object getZipFiles(Object pathClassLoader) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		
		Field zipFilesField = pathClassLoader.getClass().getDeclaredField("mZips");
		zipFilesField.setAccessible(true);
		Object zipFiles = zipFilesField.get(pathClassLoader);
		return zipFiles;
	}
	
	private Object getDexFiles(Object pathClassLoader) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
		
		Field dexFilesField = pathClassLoader.getClass().getDeclaredField("mDexs");
		dexFilesField.setAccessible(true);
		Object dexFiles = dexFilesField.get(pathClassLoader);
		return dexFiles;
	}
	
}
