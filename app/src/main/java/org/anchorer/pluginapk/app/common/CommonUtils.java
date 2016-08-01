package org.anchorer.pluginapk.app.common;

import android.util.Log;

import java.lang.reflect.Array;

/**
 * 公共工具类
 * Created by Anchorer on 16/8/1.
 */
public class CommonUtils {

    /**
     * 拼接两个数组
     * @param array1    数组1
     * @param array2    数组2
     */
    public static Object combineArray(Object array1, Object array2) {
        Log.d(Const.LOG, " array 1 = " + array1 + " array2 = " + array2);

        Class<?> clazz = Array.get(array1, 0).getClass();

        int size1 = Array.getLength(array1);
        int size2 = Array.getLength(array2);

        Object newArrayObj = Array.newInstance(clazz, size1 + size2);

        int index = 0;
        for(int i = 0; i < size1; i++) {
            Object element = Array.get(array1, i);
            Array.set(newArrayObj, index, element);
            Log.d(Const.LOG, "element 1 = " + element);
            index++;
        }

        for(int j = 0; j < size2; j++) {
            Object element = Array.get(array2, j);
            Log.d(Const.LOG, "element 2 = " + element);
            Array.set(newArrayObj, index, element);
            index++;
        }

        Log.d(Const.LOG, "combineArray result array = " + newArrayObj);
        return newArrayObj;
    }

}
