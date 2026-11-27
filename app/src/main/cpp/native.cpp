#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstring>
#include <thread>
#include <atomic>

#define LOG_TAG "AimLabNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Estrutura para representar um jogador na memória
struct PlayerData {
    float x;
    float y;
    float z;
    bool isAlive;
    int health;
};

// Variável global para controle de thread de injeção
static std::atomic<bool> g_isRunning{false};

extern "C" {

    // Inicializa o motor de ESP nat