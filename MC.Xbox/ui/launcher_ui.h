#pragma once

#include <initializer_list>
#include <string>

#include <windows.system.h>
#include <windows.ui.core.h>

#include "auth_ui_state.h"
#include "minecraft_auth.h"

class AuthScreenRenderer;

enum class MainMenuAction {
    Play,
    RepairDownloads,
    SignOut
};

bool CoreWindowAcceptsInput();
bool AnyVirtualKeyDown(
    ABI::Windows::UI::Core::ICoreWindow* window,
    std::initializer_list<ABI::Windows::System::VirtualKey> keys);

void ProcessAuthUiEvents();

void RenderAuth(AuthScreenRenderer* renderer, const AuthUiState& state);
void SleepWithAuthUi(AuthScreenRenderer* renderer, AuthUiState& state, int milliseconds);
void RenderPreparationProgress(
    AuthScreenRenderer* renderer,
    AuthUiState& state,
    const wchar_t* status,
    const wchar_t* detail,
    float progress);

MainMenuAction ShowMainMenu(
    ABI::Windows::UI::Core::ICoreWindow* window,
    const LaunchAuthConfig& authConfig,
    const std::wstring& runtimeRoot);

bool ResolveLaunchAuthConfig(
    ABI::Windows::UI::Core::ICoreWindow* window,
    LaunchAuthConfig& out);
