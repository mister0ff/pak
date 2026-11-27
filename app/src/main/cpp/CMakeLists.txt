# For more information on using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

# Declares and generates the import for the native library.
add_library(
        # Specifies the name of the library.
        aimlab
        # Sets the library as shared.
        SHARED
        # Provides a relative path to your source file(s).
        src/main/cpp/native.cpp)

# Finds and loads a library.
find_library(
        # Sets the name of the path variable.
        log-lib
        # Specifies the name of the NDK library that you want to load.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
target_link_libraries(
        # Specifies the target library.
        aimlab
        # Links the log library to the target library.
        ${log-lib})