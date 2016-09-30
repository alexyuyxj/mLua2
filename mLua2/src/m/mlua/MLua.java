package m.mlua;

import android.content.Context;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.CoroutineLib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.ResourceFinder;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class MLua {
	private Globals globals;
	private String launcher;
	
	public MLua(String launcher) {
		this.launcher = launcher;
		globals = new Globals();
		globals.load(new BaseLib());
		globals.load(new PackageLib());
		globals.load(new Bit32Lib());
		globals.load(new TableLib());
		globals.load(new StringLib());
		globals.load(new CoroutineLib());
		globals.load(new MLuaLib());
		LoadState.install(globals);
		LuaC.install(globals);
	}
	
	public void setLoadFromFile(final String basedir) {
		globals.finder = new ResourceFinder() {
			public InputStream findResource(String path) {
				try {
					File file = new File(basedir, path);
					if (!file.exists() && !path.endsWith(".lua")) {
						file = new File(basedir, path + ".lua");
					}
					
					if (file.exists()) {
						return new FileInputStream(file);
					}
				} catch (Throwable t) {
					throw new LuaError(t);
				}
				return null;
			}
		};
	}

	int i = 0;
	public void setLoadFromAssets(Context context) {
		final Context app = context.getApplicationContext();
		globals.finder = new ResourceFinder() {
			public InputStream findResource(String path) {
				try {
					if (!path.endsWith(".lua")) {
						return app.getAssets().open(path + ".lua");
					} else {
						return app.getAssets().open(path);
					}
				} catch (Throwable t) {
					throw new LuaError(t);
				}
			}
		};
	}
	
	public void start(Object... args) {
		try {
			if (args == null) {
				globals.loadfile(launcher).call();
			} else {
				globals.loadfile(launcher).call(MLuaLib.toLuaValue(args));
			}
		} catch (Throwable t) {
			throw new LuaError(t);
		}
	}
	
}
