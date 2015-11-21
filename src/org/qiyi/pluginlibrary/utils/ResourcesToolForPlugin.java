package org.qiyi.pluginlibrary.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;

public class ResourcesToolForPlugin {
	static final String DRAWABLE = "drawable";
	static final String STRING = "string";
	static final String STYLE = "style";
	static final String LAYOUT = "layout";
	static final String ID = "id";
	static final String COLOR = "color";
	static final String RAW = "raw";
	static final String ANIM = "anim";
	static final String ATTR = "attr";

	String mPackageName;
	Resources mResources;
//	private static Object sInitLock = new Object();

	public ResourcesToolForPlugin(Context appContext) {
//		synchronized (sInitLock) {
			if (mResources == null && TextUtils.isEmpty(mPackageName)) {
				mPackageName = appContext.getPackageName();
				mResources = appContext.getResources();
			}
//		}
	}

    public ResourcesToolForPlugin(Resources resource, String packageName) {
        if (resource != null && !TextUtils.isEmpty(packageName)) {
            mPackageName = packageName;
            mResources = resource;
        }
    }

	/**
	 * 获取主包资源id
	 * 
	 * @param sourceName
	 * @param sourceType
	 * @return
	 */
	private int getResourceId(String sourceName, String sourceType) {
		if (mResources == null || TextUtils.isEmpty(sourceName)) {
			return -1;
		} else {
			return mResources.getIdentifier(sourceName, sourceType, mPackageName);
		}

	}

	public int getResourceIdForString(String sourceName) {
		if (TextUtils.isEmpty(sourceName)) {
			sourceName = "emptey_string_res";
		}

		if (isForceGetResourceByR) {
			Integer id = stringIds.get(sourceName);
			if (null != id) {
				return id;
			}
		}

		int id = getResourceId(sourceName, STRING);
		if (id < 0) {
			id = getResourceId("emptey_string_res", STRING);
		}

		return id;
	}

	public int getResourceIdForID(String sourceName) {

		if (isForceGetResourceByR) {
			Integer id = idIds.get(sourceName);
			if (null != id) {
				return id;
			}
		}

		return getResourceId(sourceName, ID);
	}

	public int getResourceIdForLayout(String sourceName) {

		if (isForceGetResourceByR) {
			Integer id = layoutIds.get(sourceName);
			if (null != id) {
				return id;
			}
		}

		return getResourceId(sourceName, LAYOUT);
	}

	public int getResourceIdForDrawable(String sourceName) {
		if (TextUtils.isEmpty(sourceName)) {
			sourceName = "default_empty_drawable_transparent";// 默认一个透明图片资源
		}

		if (isForceGetResourceByR) {
			Integer id = drawableIds.get(sourceName);
			if (null != id) {
				return id;
			}
		}

		return getResourceId(sourceName, DRAWABLE);
	}

	public int getResourceIdForStyle(String sourceName) {
		return getResourceId(sourceName, STYLE);
	}

	public int getResourceIdForColor(String sourceName) {

		if (isForceGetResourceByR) {
			Integer id = colorIds.get(sourceName);
			if (null != id) {
				return id;
			}
		}

		return getResourceId(sourceName, COLOR);
	}

	public int getResourceIdForRaw(String sourceName) {
		return getResourceId(sourceName, RAW);
	}

	public int getResourceForAnim(String sourceName) {
		return getResourceId(sourceName, ANIM);
	}

	public int getResourceForAttr(String sourceName) {
		return getResourceId(sourceName, ATTR);
	}

	// ////////////////////////////////////////////////////////////////////
	// ///// 针对 反射和R获取资源的动态切换需求
	// ////////////////////////////////////////////////////////////////////

	private static boolean isForceGetResourceByR = false;
	private static Map<String, Integer> stringIds = new HashMap<String, Integer>(32);
	private static Map<String, Integer> drawableIds = new HashMap<String, Integer>(32);
	private static Map<String, Integer> colorIds = new HashMap<String, Integer>(32);
	private static Map<String, Integer> layoutIds = new HashMap<String, Integer>(16);
	private static Map<String, Integer> idIds = new HashMap<String, Integer>(64);
	private static Map<String, Integer> animIds = new HashMap<String, Integer>(64);

	public void clear() {
		drawableIds.clear();
		stringIds.clear();
		colorIds.clear();
		layoutIds.clear();
		idIds.clear();
		animIds.clear();
	}

	/**
	 * 初始资源池
	 */
	public void initResourcePool(Context ctx) {
		String packageName = ctx.getPackageName();
		String rName = packageName + ".R";

		Class<?> cls = null;
		try {
			cls = Class.forName(rName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		}

		Class<?>[] cs = cls.getDeclaredClasses();

		for (Class<?> c : cs) {
			if (c.getName().contains("$string")) {
				optResourcePool(c, stringIds);
			} else if (c.getName().contains("$drawable")) {
				optResourcePool(c, drawableIds);
			} else if (c.getName().contains("$color")) {
				optResourcePool(c, colorIds);
			} else if (c.getName().contains("$layout")) {
				optResourcePool(c, layoutIds);
			} else if (c.getName().contains("$id")) {
				optResourcePool(c, idIds);
			} else if (c.getName().contains("$anim")) {
				optResourcePool(c, animIds);
			} else {
				continue;
			}
		}
	}

	private void optResourcePool(Class<?> obj, Map<String, Integer> container) {
		container.clear();

		Field[] fields = obj.getDeclaredFields();
		if (null != fields) {
			try {
				for (Field f : fields) {
					f.setAccessible(true);
					if (f.getModifiers() == 25) {// 获取 public static final 修饰的字段
						container.put(f.getName(), f.getInt(obj));
					}
				}

			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/*
		 * JAVA 反射机制中，Field的getModifiers()方法返回int类型值表示该字段的修饰符。
		 * 其中，该修饰符是java.lang.reflect.Modifier的静态属性。
		 * 
		 * 对应表如下：
		 * 
		 * PUBLIC : 1 PRIVATE : 2 PROTECTED : 4 STATIC : 8 FINAL : 16
		 * SYNCHRONIZED : 32 VOLATILE : 64 TRANSIENT : 128 NATIVE : 256
		 * INTERFACE : 512 ABSTRACT : 1024 STRICT : 2048
		 * 
		 * 当一个方法或者字段被多个修饰符修饰的时候， getModifiers()返回的值等会各个修饰符累加的和
		 */
	}

	public boolean isForceGetResourceByR() {
		return isForceGetResourceByR;
	}

	/**
	 * 设置是否强制要求使用 R.type.name 方式获取资源id，如果设置成true，那么务必调用 初始化资源池的方法。
	 * 
	 * @see #initResourcePool()
	 * @param isForceGetResourceByR
	 */
	public void setForceGetResourceByR(boolean isForceGetResourceByR) {
	}
}
