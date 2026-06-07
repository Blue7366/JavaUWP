#pragma once

#include <string>
#include <vector>

#include "loader.h"

void FabricFinalizeVersionInfo(
    MinecraftVersionInfo& info,
    const LaunchTarget& target,
    const std::wstring& packageDir,
    const LaunchTarget& defaultTarget);
void FabricCollectExtraClasspathJars(const LoaderPreLaunchContext& ctx, std::vector<std::wstring>& jars);
void FabricAddJvmOptions(const LoaderJvmContext& ctx, std::vector<std::string>& vmOptions);
