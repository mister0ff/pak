#include <jni.h>
#include <string>
#include <android/log.h>
#include <cstring>
#include <thread>
#include <atomic>
#include <cmath>
#include <vector>
#include <random>
#include <chrono>

#define LOG_TAG "AimLabNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// ============================================================================
// ESTRUTURAS DE DADOS
// Estas estruturas simulam o layout de memória de um engine de jogo (como o 
// usado no Free Fire). Em um cheat real, você usaria offsets específicos 
// (ex: 0x14C para posição, 0x2A para HP).
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
    Vec3 forward; // Direção em que o jogador está olhando
    float fov;    // Field of View
};

// ============================================================================
// ESTADO GLOBAL
// ============================================================================

static std::atomic<bool> g_engineRunning{false};
static std::thread* g_scanThread = nullptr;
static std::atomic<int> g_detectedPlayers{0};

// Cache de jogadores para evitar alocar memória a cada frame
static std::vector<PlayerData> g_playerCache;
static std::mutex g_cacheMutex;

// ============================================================================
// MATEMÁTICA DE PROJEÇÃO
// Converte coordenadas de mundo (3D) para coordenadas de tela (2D).
// Algoritmo simplificado de Projeção Perspectiva.
// ============================================================================

bool projectWorldToScreen(const CameraState& cam, const Vec3& worldPos, 
                          int screenWidth, int screenHeight, 
                          float& outX, float& outY) {
    
    // 1. Calcular vetor da câmera para o objeto
    Vec3 toObject = worldPos - cam.position;
    
    // 2. Verificar se o objeto está atrás da câmera
    // (Produto escalar com o vetor forward deve ser positivo)
    float dot = toObject.x * cam.forward.x + 
                toObject.y * cam.forward.y + 
                toObject.z * cam.forward.z;
    
    if (dot < 0.01f) {
        return false; // Objeto está atrás da câmera
    }

    // 3. Calcular a distância (profundidade)
    float distance = dot;

    // 4. Fator de escala baseado no FOV
    // tan(FOV / 2)
    float fovRad = cam.fov * (3.14159265f / 180.0f);
    float scale = screenHeight / (2.0f * std::tan(fovRad / 2.0f));

    // 5. Projeção
    // Dividimos pela distância para simular a perspectiva
    outX = (screenWidth / 2.0f) + (toObject.x / distance) * scale;
    outY = (screenHeight / 2.0f) - (toObject.y / distance) * scale; // Y invertido para tela

    // 6. Verificar limites da tela (com margem para jogadores "quase" fora)
    if (outX < -100 || outX > screenWidth + 100 || outY < -100 || outY > screenHeight + 100) {
        return false;
    }

    return true;
}

// ============================================================================
// THREAD DE SCAN (SIMULAÇÃO DE MEMÓRIA)
// Esta thread roda em background. Em um cenário real, ela faria:
// 1. mprotect(PROT_READ) na memória do processo do jogo.
// 2. Ler os offsets base (ex: GameWorld, LocalPlayer).
// 3. Iterar pela lista encadeada de entidades.
// 4. Preencher o g_playerCache.
// ============================================================================

void memoryScanThread(JNIEnv* env, jobject thiz) {
    LOGI("AimLab: Memory scanner thread started. PID: %d", getpid());
    
    // Configurações da "Memória Simulada"
    const int MAX_ENTITIES = 8;
    float simulationTime = 0.0f;
    
    // Inicializa o cache
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
        simulationTime += 0.1f; // Incrementa tempo de simulação

        // --- LÓGICA DE "LEITURA" DE MEMÓRIA (SIMULADA) ---
        // A cada iteração, atualizamos as posições dos "jogadores"
        // simulando movimento no mundo 3D.
        {
            std::lock_guard<std::mutex> lock(g_cacheMutex);
            for (auto& player : g_playerCache) {
                // Movimento circular em torno do "local player" (0, 0, 0)
                float radius = 15.0f;
                float angle = simulationTime + (player.id * 0.8f);
                
                player.position.x = std::cos(angle) * radius;
                player.position.z = std::sin(angle) * radius;
                player.position.y = 1.0f; // Altura do chão
                
                // Simula dano aleatório
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
        
        // Loop de 60 FPS
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }
    
    LOGI("AimLab: Memory scanner thread stopped.");
}

// ============================================================================
// INTERFACE JNI
// Funções expostas para o Kotlin.
// ============================================================================

extern "C" {

    // Inicia o motor de injeção
    JNIEXPORT void JNICALL
    Java_com_kindo_aimlab_ESPOverlay_nativeStart(JNIEnv* env, jobject thiz) {
        if (g_engineRunning) {
            LOGI("AimLab: Engine already running.");
            return;
        }
        
        LOGI("AimLab: Starting native injection engine...");
        g_engineRunning = true;
        
        // Cria a thread de background
        g_scanThread = new std::thread([env, thiz]() {
            // Anexa a thread atual à JVM para permitir callbacks se necessário
            JavaVM* vm = nullptr;
            env->GetJavaVM(&vm);
            JNIEnv* newEnv = nullptr;
            
            if (vm->AttachCurrentThread(&newEnv, nullptr) != JNI_OK) {
                LOGE("AimLab: Failed to attach thread to JVM.");
                return;
            }
            
            // Executa o loop de scan
            memoryScanThread(newEnv, thiz);
            
            // Desanexa a thread
            vm->DetachCurrentThread();
        });
        
        // Não chamamos join para não bloquear a UI.
        // A thread é controlada pelo flag g_engineRunning.
    }

    // Para o motor
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
        
        // Limpa o cache
        {
            std::lock_guard<std::mutex> lock(g_cacheMutex);
            g_playerCache.clear();
        }
    }

    // Retorna o status atual em JSON (para debug/UI)
    JNIEXPORT jstring JNICALL
    Java_com_kindo_aimlab_ESPOverlay_nativeGetStatus(JNIEnv* env, jobject thiz) {
        std::string status = "Status: " + (g_engineRunning ? "ACTIVE" : "INACTIVE") + 
                             " | Detected: " + std::to_string(g_detectedPlayers.load());
        return env->NewStringUTF(status.c_str());
    }
    
    // Retorna a lista de jogadores projetados na tela.
    // Retorna um JSONArray de objetos {x, y, hp, id, name}
    JNIEXPORT jobjectArray JNICALL
    Java_com_kindo_aimlab_ESPOverlay_nativeGetProjectedPlayers(JNIEnv* env, jobject thiz, 
                                                               jint screenWidth, jint screenHeight) {
        if (!g_engineRunning) {
            return env->NewObjectArray(0, env->FindClass("[Ljava/lang/Object;"), nullptr);
        }

        // Obtem classes Java
        jclass jsonClass = env->FindClass("org/json/JSONObject");
        if (!jsonClass) {
            LOGE("AimLab: Could not find JSONObject class.");
            return nullptr;
        }

        jmethodID jsonInit = env->GetMethodID(jsonClass, "<init>", "()V");
        jmethodID putFloat = env->GetMethodID(jsonClass, "put", "(Ljava/lang/String;F)Lorg/json/JSONObject;");
        jmethodID putInt = env->GetMethodID(jsonClass, "put", "(Ljava/lang/String;I)Lorg/json/JSONObject;");
        jmethodID putString = env->GetMethodID(jsonClass, "put", "(Ljava/lang/String;Ljava/lang/String;)Lorg/json/JSONObject;");

        // Estado da Câmera (Simulado: Jogador local no 0,0,0 olhando para +Z)
        CameraState cam;
        cam.position = Vec3(0, 1.7f, 0);
        cam.forward = Vec3(0, 0, 1);
        cam.fov = 90.0f;

        jobjectArray resultArray = env->NewObjectArray(g_detectedPlayers, jsonClass, nullptr);
        if (!resultArray) return nullptr;

        int index = 0;
        
        // Lock do cache para leitura segura
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
    
    // Força uma atualização imediata (para testes)
    JNIEXPORT void JNICALL
    Java_com_kindo_aimlab_ESPOverlay_nativeForceUpdate(JNIEnv* env, jobject thiz) {
        // Em uma implementação real, isso dispararia um evento de atualização
        // no buffer de compartilhamento (SharedMemory) ou via callback.
        LOGI("AimLab: Force update requested.");
    }
}