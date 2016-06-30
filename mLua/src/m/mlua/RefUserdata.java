package m.mlua;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

public class RefUserdata extends LuaUserdata {
	private boolean classType;
	private boolean arrayType;

	public RefUserdata(Object instance) {
		super(instance);
		classType = (instance instanceof Class);
		arrayType = instance.getClass().isArray();
	}

	public LuaValue get(LuaValue key) {
		if (classType) {
			return getClassMember(key);
		} else if (arrayType) {
			return getArrayMember(key);
		} else {
			return getMember(key);
		}
	}
	
	public void set(LuaValue key, LuaValue value) {
		if (classType) {
			setClassMember(key, value);
		} else if (arrayType) {
			setArrayMember(key, value);
		} else {
			setMember(key, value);
		}
	}
	
	private LuaValue getClassMember(LuaValue key) {
		String clzName = null;
		try {
			clzName = ReflectHelper.getName((Class<?>) m_instance);
			return MLuaLib.toLuaValue(ReflectHelper.getStaticField(clzName, key.checkjstring()));
		} catch (NoSuchFieldException e) {
			final String fclzName = clzName;
			final String mthName = key.checkjstring();
			return new VarArgFunction() {
				public Varargs onInvoke(Varargs args) {
					try {
						if (args.narg() <= 1) {
							return MLuaLib.toLuaValue(ReflectHelper.invokeStaticMethod(fclzName, mthName));
						} else {
							Object[] objs = new Object[args.narg() - 1];
							for (int i = 0; i < objs.length; i++) {
								objs[i] = MLuaLib.toJavaValue(args.arg(i + 2));
							}
							return MLuaLib.toLuaValue(ReflectHelper.invokeStaticMethod(fclzName, mthName, objs));
						}
					} catch (Throwable t) {
						throw new LuaError(t);
					}
				}
			};
		} catch (Throwable t) {
			throw new LuaError(t);
		}
	}

	private LuaValue getArrayMember(LuaValue key) {
		try {
			Object jkey = MLuaLib.toJavaValue(key);
			if ("length".equals(jkey)) {
				return valueOf((Integer) ReflectHelper.getInstanceField(m_instance, "length"));
			} else {
				return MLuaLib.toLuaValue(ReflectHelper.getInstanceField(m_instance, "[" + jkey + "]"));
			}
		} catch (Throwable t) {
			throw new LuaError(t);
		}
	}

	private LuaValue getMember(LuaValue key) {
		try {
			return MLuaLib.toLuaValue(ReflectHelper.getInstanceField(m_instance, key.checkjstring()));
		} catch (NoSuchFieldException e) {
			final String mthName = key.checkjstring();
			return new VarArgFunction() {
				public Varargs onInvoke(Varargs args) {
					try {
						if (args.narg() <= 1) {
							return MLuaLib.toLuaValue(ReflectHelper.invokeInstanceMethod(m_instance, mthName));
						} else {
							Object[] objs = new Object[args.narg() - 1];
							for (int i = 0; i < objs.length; i++) {
								objs[i] = MLuaLib.toJavaValue(args.arg(i + 2));
							}
							return MLuaLib.toLuaValue(ReflectHelper.invokeInstanceMethod(m_instance, mthName, objs));
						}
					} catch (Throwable t) {
						throw new LuaError(t);
					}
				}
			};
		} catch (Throwable t) {
			throw new LuaError(t);
		}
	}

	private void setClassMember(LuaValue key, LuaValue value) {
		try {
			String clzName = ReflectHelper.getName((Class<?>) m_instance);
			ReflectHelper.setStaticField(clzName, key.checkjstring(), MLuaLib.toJavaValue(value));
		} catch (Throwable t) {
			throw new LuaError(t);
		}
	}

	private void setArrayMember(LuaValue key, LuaValue value) {
		try {
			Object index = MLuaLib.toJavaValue(key);
			ReflectHelper.setInstanceField(m_instance, "[" + index + "]", MLuaLib.toJavaValue(value));
		} catch (Throwable t) {
			throw new LuaError(t);
		}
	}

	private void setMember(LuaValue key, LuaValue value) {
		try {
			ReflectHelper.setInstanceField(m_instance, key.checkjstring(), MLuaLib.toJavaValue(value));
		} catch (Throwable t) {
			throw new LuaError(t);
		}
	}
	
}
