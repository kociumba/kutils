---@meta

-- The Logger class allowing for integration with minecraft logging
---@class Logger
---@field info fun(message: string) # Log an informational message
---@field warn fun(message: string) # Log a warning message
---@field error fun(message: string) # Log an error message
---@field debug fun(message: string) # Log a debug message

-- Logger creation and usage,
---Creates a named minecraft logger for your script
---@param name string # The name of the logger
---@return Logger # The created logger you can use to log messages
function createLogger(name) end

-- JVM class importing,
---Imports a Java class from the game's runtime
---@param className string # Full class name including package
---@return any # The imported Java class
function requireJVM(className) end

-- HUD rendering,
---Registers a function to render HUD elements
---@param callback fun(drawContext: any, tickDelta: number) # Called every frame when script is enabled
function registerHudRenderer(callback) end

-- Script lifecycle,
---Registers a cleanup function that will be called when the script is disabled by the user
---@param callback function # Function to call on script disable
function onDisable(callback) end

---Checks if the script is currently enabled
---@return boolean # true if the script is enabled
function isEnabled() end

-- Thread safety,
---Executes the given function on Minecraft's main thread
---Use this when modifying game state that must be accessed from the main thread
---@param callback function # Function to execute on the main thread
function runOnMain(callback) end