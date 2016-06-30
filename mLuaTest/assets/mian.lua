local context = ...

import("ReflectRunnable", "m.mlua.ReflectHelper$ReflectRunnable")

local function main()
  print("hello world from mLua", 123, true)
  print("current context: ", context)
  local packageName = context:getPackageName()
  print("packageName: ", packageName)
  
  local luaCode = {
    run = function(arg)
      print("luaCode:run(), input: ", arg)
      return "yoyoyo"
    end
  }
  local proxy = createProxy("ReflectRunnable", luaCode)
  local res = proxy:run(packageName)
  print("luaCode.run(), output: ", res)
  
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
