package m.mlua;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

public class MLuaLib extends VarArgFunction {
	private static final int INIT                   = 0;
	private static final int IMPORT_CLASS           = 1;
	private static final int NEW_INSTANCE           = 2;
	private static final int GET_CLASS              = 3;
	private static final int CREATE_PROXY           = 4;
	private static final int PRINT                  = 5;
	
	private static final String[] NAMES = {
		"import",
		"new",
		"getClass",
		"createProxy",
		"print"
	};
	
	static final Object toJavaValue(LuaValue value) {
		if (value.isboolean()) {
			return value.checkboolean();
		} else if (value.isint()) {
			return toJavaNumber(value.checkint());
		} else if (value.islong()) {
			return toJavaNumber(value.checklong());
		} else if (value.isnumber()) {
			return toJavaNumber(value.checkdouble());
		} else if (value.isstring()) {
			return value.checkjstring();
		} else if (value.isnil()) {
			return null;
		} else if (value instanceof LuaUserdata) {
			LuaUserdata ji = (LuaUserdata) value;
			return ji.m_instance;
		} else {
			return value.checkuserdata();
		}
	}
	
	private static final Object toJavaNumber(double value) {
		Double d = Double.valueOf(value);
		if (value == 0) {
			return (byte) 0;
		} else if (value > Float.MAX_VALUE || value < Float.MIN_VALUE) {
			return d;
		} else if (value > Long.MAX_VALUE || value < Long.MIN_VALUE || ((long) value != value)) {
			return (float) value;
		} else if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
			return d.longValue();
		} else if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
			return d.intValue();
		} else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
			return d.shortValue();
		} else {
			return d.byteValue();
		}
	}
	
	static final LuaValue toLuaValue(Object value) {
		if (value == null) {
			return NIL;
		} else if (value instanceof Boolean) {
			return valueOf((Boolean) value);
		} else if (value instanceof Byte || value instanceof Short || value instanceof Character || value instanceof Integer) {
			return valueOf(Integer.valueOf(String.valueOf(value)));
		} else if (value instanceof Long || value instanceof Float || value instanceof Double) {
			return valueOf(Double.valueOf(String.valueOf(value)));
		} else if (value instanceof String) {
			return valueOf((String) value);
		} else {
			return new RefUserdata(value);
		}
	}
	
	MLuaLib() {
		
	}
	
	public Varargs invoke(Varargs args) {
		try {
			switch (opcode) {
				case INIT: return onInit(args);
				case IMPORT_CLASS: return onImport(args);
				case NEW_INSTANCE: return onNew(args);
				case GET_CLASS: return onGetClass(args);
				case CREATE_PROXY: return onCreateProxy(args);
				case PRINT: return onPrint(args);
			}
		} catch (Throwable t) {
			throw new LuaError(t);
		}
		throw new LuaError("not yet supported: " + this);
	}
	
	private Varargs onInit(Varargs args) throws Throwable {
		LuaValue env = args.arg(2);
		for (int i = 0; i < NAMES.length; i++) {
			MLuaLib f = new MLuaLib();
			f.opcode = IMPORT_CLASS + i;
			f.name = NAMES[i];
			env.set(f.name, f);
		}
		return NIL;
	}
	
	private Varargs onImport(Varargs args) throws Throwable {
		if (args.narg() == 1) {
			String className = args.checkjstring(1);
			return toLuaValue(ReflectHelper.importClass(className));
		} else {
			String name = args.checkjstring(1);
			String className = args.checkjstring(2);
			return toLuaValue(ReflectHelper.importClass(name, className));
		}
	}
	
	private Varargs onNew(Varargs args) throws Throwable {
		String className = args.checkjstring(1);
		if (args.narg() > 1) {
			Object[] params = new Object[args.narg() - 1];
			for (int i = 0; i < params.length; i++) {
				params[i] = toJavaValue(args.arg(i + 2));
			}
			return toLuaValue(ReflectHelper.newInstance(className, params));
		} else {
			return toLuaValue(ReflectHelper.newInstance(className));
		}
	}
	
	private Varargs onGetClass(Varargs args) throws Throwable {
		String name = args.checkjstring(1);
		return toLuaValue(ReflectHelper.getClass(name));
	}
	
	private Varargs onCreateProxy(Varargs args) throws Throwable {
		final LuaValue table = args.arg(args.narg());
		Class<?>[] proxyIntefaces = new Class<?>[args.narg() - 1];
		for (int i = 0; i < proxyIntefaces.length; i++) {
			String name = args.checkjstring(i + 1);
			proxyIntefaces[i] = ReflectHelper.getClass(name);
		}
		
		ClassLoader loader = getClass().getClassLoader();
		InvocationHandler handler = new InvocationHandler() {
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				LuaValue function = table.get(method.getName());
				Class<?> retType = method.getReturnType();
				if (retType.equals(Void.class) || retType.equals(void.class)) {
					function.call(toLuaValue(args));
					return NIL;
				} else {
					return function.call(toLuaValue(args));
				}
			}
		};
		
		return toLuaValue(Proxy.newProxyInstance(loader, proxyIntefaces, handler));
	}
	
	private Varargs onPrint(Varargs args) throws Throwable {
		StringBuffer sb = new StringBuffer();
		for (int i = 0, size = args.narg(); i < size; i++) {
			LuaValue arg = args.arg(i + 1);
			Object obj = toJavaValue(arg);
			if (obj == null) {
				sb.append("nil");
			} else if (obj.getClass().isArray()) {
				int len = Array.getLength(obj);
				sb.append('[');
				for (int index = 0; index < len; index++) {
					if (index > 0) {
						sb.append(',');
					}
					sb.append(Array.get(obj, index));
				}
				sb.append(']');
			} else {
				sb.append(obj);
			}
		}
		System.out.println(sb.toString());
		return NIL;
	}
	
}
