#pragma once

#include <string>

std::wstring ProfileExportsDir(const std::wstring& runtimeRoot);
std::wstring DefaultProfileExportPath(const std::wstring& runtimeRoot, const std::wstring& profileId);

bool ExportProfileMrpack(
    const std::wstring& runtimeRoot,
    const std::wstring& profileId,
    const std::wstring& outputPath,
    std::wstring& error);

bool InstallModpackFromFile(
    const std::wstring& mrpackPath,
    const std::wstring& runtimeRoot,
    const std::wstring& profileId,
    std::wstring& error);
