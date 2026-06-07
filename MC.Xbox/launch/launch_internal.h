#pragma once

#include <jni.h>
#include <string>
#include <vector>

bool LaunchInvokeJavaMain(JNIEnv* env, const std::wstring& className, const std::vector<std::string>& args);
void LaunchSetJavaSystemProperty(JNIEnv* env, const std::wstring& key, const std::wstring& value);
