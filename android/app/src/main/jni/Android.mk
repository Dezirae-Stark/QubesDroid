LOCAL_PATH := $(call my-dir)

# QubesDroid Crypto Library
include $(CLEAR_VARS)

LOCAL_MODULE := qubesdroid-crypto

# Source directory (relative to QubesDroid root)
CRYPTO_SRC := $(LOCAL_PATH)/../../../../../src/Crypto
COMMON_SRC := $(LOCAL_PATH)/../../../../../src/Common

# Crypto source files
LOCAL_SRC_FILES := \
    qubesdroid_crypto.c \
    $(CRYPTO_SRC)/cpu.c \
    $(CRYPTO_SRC)/chacha20poly1305.c \
    $(CRYPTO_SRC)/chacha256.c \
    $(CRYPTO_SRC)/poly1305.c \
    $(CRYPTO_SRC)/chachaRng.c \
    $(CRYPTO_SRC)/Sha2.c \
    $(CRYPTO_SRC)/blake2s.c \
    $(CRYPTO_SRC)/Argon2/src/argon2.c \
    $(CRYPTO_SRC)/Argon2/src/core.c \
    $(CRYPTO_SRC)/Argon2/src/ref.c \
    $(CRYPTO_SRC)/Argon2/src/blake2/blake2b.c

# Include paths
LOCAL_C_INCLUDES := \
    $(CRYPTO_SRC) \
    $(COMMON_SRC) \
    $(CRYPTO_SRC)/Argon2/include \
    $(CRYPTO_SRC)/Argon2/src \
    $(CRYPTO_SRC)/Argon2/src/blake2

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
