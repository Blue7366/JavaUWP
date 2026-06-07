#include "loader_common.h"

#include <algorithm>

std::wstring fwd(const std::wstring& path) {
    std::wstring r = path;
    for (auto& c : r) {
        if (c == L'\\') c = L'/';
    }
    return r;
}

LoaderId ParseLoaderId(const std::wstring& loader) {
    if (_wcsicmp(loader.c_str(), L"fabric") == 0) return LoaderId::Fabric;
    if (_wcsicmp(loader.c_str(), L"neoforge") == 0) return LoaderId::NeoForge;
    if (_wcsicmp(loader.c_str(), L"forge") == 0) return LoaderId::Forge;
    return LoaderId::Unknown;
}

bool IsLoader(const std::wstring& loader, LoaderId id) {
    return ParseLoaderId(loader) == id;
}

std::wstring FirstArgValue(const std::vector<std::wstring>& args, const std::wstring& name) {
    for (size_t i = 0; i + 1 < args.size(); ++i) {
        if (args[i] == name) return args[i + 1];
    }
    return {};
}

std::wstring MavenPath(
    const std::wstring& group,
    const std::wstring& artifact,
    const std::wstring& version,
    const std::wstring& classifier,
    const std::wstring& extension) {
    std::wstring groupPath = group;
    std::replace(groupPath.begin(), groupPath.end(), L'.', L'\\');
    return groupPath + L"\\" + artifact + L"\\" + version + L"\\" +
        artifact + L"-" + version + (classifier.empty() ? L"" : (L"-" + classifier)) + L"." + extension;
}
