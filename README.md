# 介绍

我此前做过一个基于luajava的、名为mLua的lua解析器。虽然多数情况下它能正常工作，但美中不足的是在lua中创建的java线程，并不能真的并行执行，因此mLua实际上是一个单线程的工具。而且mLua基于JNI代码，时下安卓设备CPU类型越来越多，为了兼容不同的硬件，软件需要为不同的CPU编译不同的so库，导致软件的体积十分臃肿。

为了解决上述的问题，我转而求助于另外一个lua解析器luaj。它的好处很明显：纯java、真正的多线程。纯java意味着不需要为不同CPU编译不同的库，而且代码阅读、调试和异常捕获都十分方便。不过luaj要更好地沟通java和lua两端的代码，还需要一个jse的库，而这个库依旧达不到我要的效果，故而我放弃了jse库，重新实现了mLua库，并将项目重命名为**mLua2**。

# 特点描述

和mLua类似，mLua2依旧使用内置全局函数的方式来提供支持，但是由于luaj的扩展性很好，改良后的mLua2允许lua端直接操作java对象，所以原来提供对象操作的全局函数都被删除，使得代码看上去会自然很多。

虽然luaj本身会将java数组当作userdata推到lua端，所以不会出现luajava“不区分byte数组和string”的问题。但同时lua端的代码对数组对象的操作也十分麻烦。mLua2对此作了改良，允许lua端代码使用“[下标]”的方式直接读写java数组的元素，也可以通过“length”获取java数组的长度。其使用方式与java代码相同。

mLua区分byte数组和string。在mLua中，java的byte数组对lua端而言，只是一个普通的userdata。

和mLua一样，将lua端的number传递给java后，会依照byte - short - int - long - float - double链条来尝试解释。

mLua2的所有操作也都基于MLua实例完成。

# java端方法描述

mLua2的java端方法集中在MLua中：

方法名称                     | 方法解释
--------------------------- | -------------
< init >(String)            | 构造函数，其中的参数表示解析器启动后首先执行的lua脚本
setLoadFromFile(String)     | 设置从文件系统中加载lua脚本。此方法应当在start前调用，并且与<br/>setLoadFromAssets相斥
setLoadFromAssets(Context)  | 设置从assets目录中加载lua脚本，此方法应当在start前调用，并且与<br/>setLoadFromFile相斥
start()                     | 启动lua解析器
start(Object)               | 传递参数并启动lua解析器

由于lua中调用java api变得十分方便，因此mLua2不再提供往解析器中推入自定义全局函数的方法。

# lua端函数描述

在mLua2下，lua原来的require、print函数已经被改写。

## require

如果lua脚本存放在文件系统中，require必须使用设置在java端的basedir为根目录的相对路径引用其他lua脚本；如果lua脚本存放在assets目录中，则项目的assets目录就是require加载脚本的根目录：

``` lua
require "dir1/dir2/script1"
require "script2"
```

## print

支持输出一个或多个对象，但是不能将string与java对象作拼接：

``` lua
-- 正确的做法 --
print("hello mLua")
print("context: ", getContext())
print("string " .. 111)

-- 错误的做法 --
print("context: " .. getContext())
```

通过逗号分隔的对象会在java端以tab号分隔显示

## 操作java对象

mLua2提供了如下的lua内置函数：

函数名称                                       | 函数解释
--------------------------------------------- | ----------
import(className)                             | 向ReflectHelper类缓存中导入一个类，此函数将返回一个<br/>string，用于后续代码从缓存中重新获取导入的类实例
import(name, className)                       | 向ReflectHelper类缓存中导入一个类，并将此缓存的key<br/>设置为指定名称
new(className, ...)                           | 构造一个java实例，参数className是import函数的返回值，<br/>后续参数为java构造方法的输入参数
createProxy(proxyTable, ...)                  | 构造一个java接口代理。参数proxyTable是一个lua的table，<br/>其中的key必须与java接口类的方法名称相同，key对应的<br/>value是一个lua的function，function的参数列表和返回值也<br/>必须与java接口相同。proxyTable后的参数是被实现的接口<br/>列表名称，皆为string，由import函数返回。此函数将返回一<br/>个java接口代理实例，可将此实例传回java端并进行操作，<br/>当实例中的接口函数被调用时，mLua会调用proxyTable中的<br/>对应funtion代码完成操作

# 例子

## java端代码

``` java
public class MainActivity extends Activity {
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 构造一个解析器实例，设置main.lua为入口代码
		MLua mLua = new MLua("mian.lua");
		// 设置lua代码存放位置
		mLua.setLoadFromAssets(this);
		// 启动解析器
		mLua.start(getApplication());
	}
}
```

## lua端代码

``` lua
-- 读取启动参数 --
local context = ...

-- 导入ReflectHelper.ReflectRunnable类，并命名为ReflectRunnable --
import("ReflectRunnable", "m.mlua.ReflectHelper$ReflectRunnable")

local function main()
  -- 演示print --
  print("hello world from mLua", 123, true)
  print("current context: ", context)
  local packageName = context:getPackageName()
  print("packageName: ", packageName)
  
  -- 演示java接口代理 --
  local luaCode = {
    run = function(arg)
      print("luaCode:run(), input: ", arg)
      return "yoyoyo"
    end
  }
  local proxy = createProxy("ReflectRunnable", luaCode)
  local res = proxy:run(packageName)
  print("luaCode.run(), output: ", res)
  
  -- 演示java数组操作 --
  local bArray = new("[B", 16)
  for i = 0, (bArray.length - 1) do
    bArray[i] = i + 1
  end
  
  local bArray2 = new("[B", bArray.length)
  local System = getClass("System")
  System:arraycopy(bArray, 0, bArray2, 0, 16)
  
  for i = 0, (bArray2.length - 1) do
    print("bArray2[" .. i .. "]: ", bArray2[i])
  end
  
  -- 演示多线程 --
  local luaRunnable = {
    run = function()
      local Thread = getClass("Thread")
      local curThread = Thread:currentThread()
      for i = 0, 5 do
        local threadName = curThread:getName()
        print("thread: ", threadName)
        Thread:sleep(1000)
      end
    end
  }
  local runnable = createProxy("Runnable", luaRunnable)
  local thread1 = new("Thread", runnable)
  local thread2 = new("Thread", runnable)
  local thread3 = new("Thread", runnable)
  thread1:start()
  thread2:start()
  thread3:start()
end

main()
```
