#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstring>
#include <thread>
#include <atomic>
#include <cmath>
#include <vector>
#include <mutex>
#include <random>
#include <chrono>

#define LOG_TAG "AimLabNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// ESTRUTURAS DE DADOS
// ============================================================================

struct Vec3 {
    float x;
    float y;
    float z;
    
    Vec3(float _x = 0, float _y = 0, float _z = 0) : x(_x), y(_y), z(_z) {}
    Vec3 operator-(const Vec3& other) const { return Vec3(x - other.x, y - other.y, z - other.z); }
    float length() const { return std::sqrt(x*x + y*y + z*z); }
};

struct PlayerData {
    Vec3 position;
    float health;
    int id;
    bool isAlive;
    std::string name;
};

struct CameraState {
    Vec3 position;
    Vec3 forward;
    float fov;
};

// ============================================================================
// ESTADO GLOBAL
// ============================================================================

static std::atomic<bool> g_engineRunning{false};
static std::thread* g_scanThread = nullptr;
static std::atomic<int> g_detectedPlayers{0};
static std::vector<PlayerData> g_playerCache;
static std::mutex g_cacheMutex;

// ============================================================================
// MATEMÁTICA DE PROJEÇÃO
// ============================================================================

bool projectWorldToScreen(const CameraState& cam, const Vec3& worldPos, 
                          int screenWidth, int screenHeight, 
                          float& outX, float& outY) {
    Vec3 toObject = worldPos - cam.position;
    
    float dot = toObject.x * cam.forward.x + 
                toObject.y * cam.forward.y + 
                toObject.z * cam.forward.z;
    
    if (dot < 0.01f) {
        return false;
    }

    float distance = dot;
    float fovRad = cam.fov * (3.14159265f / 180.0f);
    float scale = screenHeight / (2.0f * std::tan(fovRad / 2.0f));

    outX = (screenWidth / 2.0f) + (toObject.x / distance) * scale;
    outY = (screenHeight / 2.0f) - (toObject.y / distance) * scale;

    if (outX < -100 || outX > screenWidth + 100 || outY < -100 || outY > screenHeight + 100) {
        return false;
    }

    return true;
}

// ============================================================================
// THREAD DE SCAN
// ============================================================================

void memoryScanThread(JNIEnv* env, jobject thiz) {
    LOGI("AimLab: Memory scanner thread started. PID: %d", getpid());
    
    const int MAX_ENTITIES = 8;
    float simulationTime = 0.0f;
    
    {
        std::lock_guard<std::mutex> lock(g_cacheMutex);
        g_playerCache.clear();
        for (int i = 0; i < MAX_ENTITIES; ++i) {
            PlayerData p;
            p.id = i + 1;
            p.isAlive = true;
            p.health = 100.0f;
            p.name = "ENEMY_" + std::to_string(i);
            g_playerCache.push_back(p);
        }
    }

    while (g_engineRunning) {
        simulationTime += 0.1f;

        {
            std::lock_guard<std::mutex> lock(g_cacheMutex);
            for (auto& player : g_playerCache) {
                float radius = 15.0f;
                float angle = simulationTime + (player.id * 0.8f);
                
                player.position.x = std::cos(angle) * radius;
                player.position.z = std::sin(angle) * radius;
                player.position.y = 1.0f;
                
                if (std::rand() % 100 < 5) {
                    player.health -= std::rand() % 20;
                    if (player.health <= 0) {
                        player.isAlive = false;
                        player.health = 0;
                    }
                }
            }
        }
        
        g_detectedPlayers = g_playerCache.size();
        
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }
    
    LOGI("AimLab: Memory scanner thread stopped.");
}

// ============================================================================
// INTERFACE JNI
// ============================================================================

extern "C" {

    JNIEXPORT void JNICALL
    Java_com_kindo_aimlab_ESPOverlay_nativeStart(JNIEnv* env, jobject thiz) {
        if (g_engineRunning) {
            LOGI("AimLab: Engine already running.");
            return;
        }
        
        LOGI("AimLab: Starting native injection engine...");
        g_engineRunning = true;
        
        g_scanThread = new std::thread([env, thiz]() {
            JavaVM* vm = nullptr;
            env->GetJavaVM(&vm);
            JNIEnv* newEnv = nullptr;
            
            if (vm->AttachCurrentThread(&newEnv, nullptr) != JNI_OK) {
                LOGE("AimLab: Failed to attach thread to JVM.");
                return;
            }
            
            memoryScanThread(newEnv, thiz);
            
            vm->DetachCurrentThread();
        });
    }

    JNIEXPORT void JNICALL
    Java_com_kindo_aimlab_ESPOverlay_nativeStop(JNIEnv* env, jobject thiz) {
        if (!g_engineRunning) return;
        
        LOGI("AimLab: Stopping native injection engine...");
        g_engineRunning = false;
        
        if (g_scanThread) {
            g_scanThread->join();
            delete g_scanThread;
            g_scanThread = nullptr;
        }
        
        {
            std::lock_guard<std::mutex> lock(g_cacheMutex);
            g_playerCache.clear();
        }
    }

    JNIEXPORT jstring JNICALL
    Java_com_kindo_aimlab_ESPOverlay_nativeGetStatus(JNIEnv* env, jobject thiz) {
        std::string status = "Status: " + (g_engineRunning ? "ACTIVE" : "INACTIVE") + 
                             " | Detected: " + std::to_string(g_detectedPlayers.load());
        return env->NewStringUTF(status.c_str());
    }
    
    JNIEXPORT jobjectArray JNICALL
    Java_com_kindo_aimlab_ESPOverlay_nativeGetProjectedPlayers(JNIEnv* env, jobject thiz, 
                                                               jint screenWidth, jint screenHeight) {
        if (!g_engineRunning) {
            return env->NewObjectArray(0, env->FindClass("org/json/JSONObject"), nullptr);
        }

        jclass jsonClass = env->FindClass("org/json/JSONObject");
        if (!jsonClass) {
            LOGE("AimLab: Could not find JSONObject class.");
            return nullptr;
        }

        jmethodID jsonInit = env->GetMethodID(jsonClass, "<init>", "()V");
        jmethodID putFloat = env->GetMethodID(jsonClass, "put", "(Ljava/lang/String;F)Lorg/json/JSONObject;");
        jmethodID putInt = env->GetMethodID(jsonClass, "put", "(Ljava/lang/String;I)Lorg/json/JSONObject;");
        jmethodID putString = env->GetMethodID(jsonClass, "put", "(Ljava/lang/String;Ljava/lang/String;)Lorg/json/JSONObject;");

        CameraState cam;
        cam.position = Vec3(0, 1.7f, 0);
        cam.forward = Vec3(0, 0, 1);
        cam.fov = 90.0f;

        int count = 0;
        {
            std::lock_guard<std::mutex> lock(g_cacheMutex);
            count = g_playerCache.size();
        }

        jobjectArray resultArray = env->NewObjectArray(count, jsonClass, nullptr);
        if (!resultArray) return nullptr;

        int index = 0;
        
        std::lock_guard<std::mutex> lock(g_cacheMutex);
        
        for (const auto& player : g_playerCache) {
            if (!player.isAlive) continue;

            float sx, sy;
            if (projectWorldToScreen(cam, player.position, screenWidth, screenHeight, sx, sy)) {
                jobject playerObj = env->NewObject(jsonClass, jsonInit);
                if (!playerObj) continue;

                env->CallVoidMethod(playerObj, putFloat, env->NewStringUTF("x"), sx);
                env->CallVoidMethod(playerObj, putFloat, env->NewStringUTF("y"), sy);
                env->CallVoidMethod(playerObj, putInt, env->NewStringUTF("id"), player.id);
                env->CallVoidMethod(playerObj, putInt, env->NewStringUTF("hp"), (jint)player.health);
                env->CallVoidMethod(playerObj, putString, env->NewStringUTF("name"), env->NewStringUTF(player.name.c_str()));
                
                env->SetObjectArrayElement(resultArray, index++, playerObj);
                env->DeleteLocalRef(playerObj);
            }
        }

        return resultArray;
    }
    
    JNIEXPORT void JNICALL
    Java_com_kindo_aimlab_ESPOverlay_nativeForceUpdate(JNIEnv* env, jobject thiz) {
        LOGI("AimLab: Force update requested.");
    }
}