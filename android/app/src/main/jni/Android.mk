LOCAL_PATH := $(call my-dir)

# QubesDroid Crypto Library
include $(CLEAR_VARS)

LOCAL_MODULE := qubesdroid-crypto

# Source directory (relative to QubesDroid root)
CRYPTO_SRC := $(LOCAL_PATH)/../../../../../src/Crypto

# Crypto source files
LOCAL_SRC_FILES := \
    qubesdroid_crypto.c \
    $(CRYPTO_SRC)/chacha20poly1305.c \
    $(CRYPTO_SRC)/chacha256.c \
    $(CRYPTO_SRC)/poly1305.c \
    $(CRYPTO_SRC)/chachaRng.c \
    $(CRYPTO_SRC)/Sha2.c \
    $(CRYPTO_SRC)/blake2s.c \
    $(CRYPTO_SRC)/Argon2/argon2.c \
    $(CRYPTO_SRC)/Argon2/core.c \
    $(CRYPTO_SRC)/Argon2/encoding.c \
    $(CRYPTO_SRC)/Argon2/opt.c \
    $(CRYPTO_SRC)/Argon2/thread.c \
    $(CRYPTO_SRC)/Argon2/blake2/blake2b.c

# Include paths
LOCAL_C_INCLUDES := \
    $(CRYPTO_SRC) \
    $(CRYPTO_SRC)/Argon2 \
    $(CRYPTO_SRC)/Argon2/blake2

# Compiler flags
LOCAL_CFLAGS := \
    -O3 \
    -ffast-math \
    -DANDROID \
    -DPQ_CRYPTO \
    -DARGON2_NO_THREADS=0 \
    -Wall \
    -Wno-unused-parameter

# ARM-specific optimizations
ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
    LOCAL_CFLAGS += -march=armv8-a+crypto -mtune=cortex-a72
    LOCAL_ARM_NEON := true
endif

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_CFLAGS += -march=armv7-a -mfpu=neon -mfloat-abi=softfp
    LOCAL_ARM_NEON := true
endif

# Link libraries
LOCAL_LDLIBS := -llog -landroid

include $(BUILD_SHARED_LIBRARY)
