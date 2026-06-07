#pragma once

#include <string>

bool CreateCrashReportZip(const std::wstring& runtimeRoot, const std::wstring& reason);
void ArchivePreviousCrashIfNeeded(const std::wstring& runtimeRoot);

bool RewriteZipTextEntry(
    const std::wstring& zipPath,
    const char* entryName,
    const std::wstring& replacementText,
    const std::wstring& backupPath);
