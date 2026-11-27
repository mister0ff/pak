cmake_minimum_required(VERSION 3.22.1)

project("AimLabNative")

# Define o nome da biblioteca
add_library(
        aimlab
        SHARED
        # Caminho relativo para o arquivo C++
        native.cpp)

# Encontra a biblioteca de log do Android
find_library(
        log-lib
        log)

# Liga a biblioteca de log ao seu projeto
target_link_libraries(
        aimlab
        ${log-lib})