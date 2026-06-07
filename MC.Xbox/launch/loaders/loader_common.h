#pragma once

#include <string>
#include <vector>

std::wstring fwd(const std::wstring& path);
std::wstring FirstArgValue(const std::vector<std::wstring>& args, const std::wstring& name);
std::wstring MavenPath(
    const std::wstring& group,
    const std::wstring& artifact,
    const std::wstring& version,
    const std::wstring& classifier = L"",
    const std::wstring& extension = L"jar");

enum class LoaderId {
    Unknown,
    Fabric,
    NeoForge,
    Forge,
};

LoaderId ParseLoaderId(const std::wstring& loader);
bool IsLoader(const std::wstring& loader, LoaderId id);
